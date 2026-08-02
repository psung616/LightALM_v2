package com.lightalm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightalm.domain.GitLink;
import com.lightalm.domain.GitLinkSource;
import com.lightalm.domain.PrStatus;
import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.TargetType;
import com.lightalm.dto.CreateGitLinkRequest;
import com.lightalm.dto.GitLinkResponse;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.exception.ValidationException;
import com.lightalm.integration.github.GithubApiClient;
import com.lightalm.integration.github.GithubCommitInfo;
import com.lightalm.integration.github.GithubPullRequestInfo;
import com.lightalm.integration.github.GithubWebhookSignatureVerifier;
import com.lightalm.integration.github.IssueKeyParser;
import com.lightalm.repository.GitLinkRepository;
import com.lightalm.repository.IssueRepository;
import com.lightalm.repository.RequirementRepository;
import com.lightalm.security.UserPrincipal;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GitLinkService {

    private static final Logger log = LoggerFactory.getLogger(GitLinkService.class);

    private final GitLinkRepository gitLinkRepository;
    private final RequirementRepository requirementRepository;
    private final IssueRepository issueRepository;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;
    private final GithubApiClient githubApiClient;
    private final GithubWebhookSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<GitLinkResponse> list(Long projectId, TargetType targetType, Long targetId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        verifyTargetExists(projectId, targetType, targetId);
        return gitLinkRepository.findByTargetTypeAndTargetIdOrderByLinkedAtDesc(targetType, targetId).stream()
                .map(GitLinkResponse::from)
                .toList();
    }

    @Transactional
    public GitLinkResponse createManual(Long projectId, TargetType targetType, Long targetId, CreateGitLinkRequest request,
                                         UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        verifyTargetExists(projectId, targetType, targetId);
        Project project = projectService.getEntity(projectId);
        if (project.getGithubRepoOwner() == null || project.getGithubRepoName() == null) {
            throw new ValidationException("프로젝트에 GitHub 연동 정보(repoOwner/repoName)가 설정되어 있지 않습니다.");
        }

        GitLink link;
        if (request.getSource() == GitLinkSource.COMMIT) {
            if (request.getCommitSha() == null || request.getCommitSha().isBlank()) {
                throw new ValidationException("commitSha는 필수입니다.");
            }
            GithubCommitInfo commit = githubApiClient.getCommit(
                    project.getGithubRepoOwner(), project.getGithubRepoName(), request.getCommitSha(), project.getGithubAccessToken());
            link = GitLink.builder()
                    .project(project)
                    .targetType(targetType)
                    .targetId(targetId)
                    .source(GitLinkSource.COMMIT)
                    .commitSha(commit.sha())
                    .message(commit.message())
                    .authorLogin(commit.authorLogin())
                    .url(commit.htmlUrl())
                    .build();
        } else {
            if (request.getPrNumber() == null) {
                throw new ValidationException("prNumber는 필수입니다.");
            }
            GithubPullRequestInfo pr = githubApiClient.getPullRequest(
                    project.getGithubRepoOwner(), project.getGithubRepoName(), request.getPrNumber(), project.getGithubAccessToken());
            link = GitLink.builder()
                    .project(project)
                    .targetType(targetType)
                    .targetId(targetId)
                    .source(GitLinkSource.PULL_REQUEST)
                    .prNumber(pr.number())
                    .prStatus(resolvePrStatus(pr))
                    .message(pr.title())
                    .authorLogin(pr.authorLogin())
                    .url(pr.htmlUrl())
                    .build();
        }
        return GitLinkResponse.from(gitLinkRepository.save(link));
    }

    @Transactional
    public void delete(Long projectId, Long linkId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        GitLink link = gitLinkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("연결을 찾을 수 없습니다: " + linkId));
        if (!link.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("연결을 찾을 수 없습니다: " + linkId);
        }
        gitLinkRepository.delete(link);
    }

    public boolean verifyWebhookSignature(Long projectId, byte[] rawBody, String signatureHeader) {
        Project project = projectService.getEntity(projectId);
        return signatureVerifier.isValid(rawBody, signatureHeader, project.getGithubWebhookSecret());
    }

    @Transactional
    public void handlePushEvent(Long projectId, byte[] rawBody) {
        Project project = projectService.getEntity(projectId);
        JsonNode root = readTree(rawBody);
        JsonNode commits = root.path("commits");
        if (!commits.isArray()) {
            return;
        }
        for (JsonNode commitNode : commits) {
            String sha = commitNode.path("id").asText(null);
            String message = commitNode.path("message").asText(null);
            String url = commitNode.path("url").asText(null);
            String authorLogin = commitNode.path("author").path("username").asText(null);
            if (sha == null || message == null) {
                continue;
            }
            linkKeysToTarget(project, message, GitLinkSource.COMMIT, sha, null, message, authorLogin, url, null);
        }
    }

    @Transactional
    public void handlePullRequestEvent(Long projectId, byte[] rawBody) {
        Project project = projectService.getEntity(projectId);
        JsonNode root = readTree(rawBody);
        JsonNode pr = root.path("pull_request");
        if (pr.isMissingNode()) {
            return;
        }
        Integer number = pr.path("number").asInt();
        String title = pr.path("title").asText(null);
        String body = pr.path("body").asText(null);
        String url = pr.path("html_url").asText(null);
        String authorLogin = pr.path("user").path("login").asText(null);
        boolean merged = pr.path("merged").asBoolean(false);
        String state = pr.path("state").asText("open");
        PrStatus prStatus = merged ? PrStatus.MERGED : ("closed".equalsIgnoreCase(state) ? PrStatus.CLOSED : PrStatus.OPEN);

        String combinedText = (title != null ? title : "") + " " + (body != null ? body : "");
        linkKeysToTarget(project, combinedText, GitLinkSource.PULL_REQUEST, null, number, title, authorLogin, url, prStatus);
    }

    private void linkKeysToTarget(Project project, String text, GitLinkSource source, String commitSha, Integer prNumber,
                                   String message, String authorLogin, String url, PrStatus prStatus) {
        var parsedKeys = IssueKeyParser.parse(project.getProjectKey(), text);
        for (var parsed : parsedKeys) {
            TargetType targetType;
            Long targetId;
            if (parsed.requirement()) {
                var requirement = requirementRepository.findByReqKey(parsed.key()).orElse(null);
                if (requirement == null || !requirement.getProject().getId().equals(project.getId())) {
                    continue;
                }
                targetType = TargetType.REQUIREMENT;
                targetId = requirement.getId();
            } else {
                var issue = issueRepository.findByIssueKey(parsed.key()).orElse(null);
                if (issue == null || !issue.getProject().getId().equals(project.getId())) {
                    continue;
                }
                targetType = TargetType.ISSUE;
                targetId = issue.getId();
            }

            boolean alreadyLinked = source == GitLinkSource.COMMIT
                    ? gitLinkRepository.existsByTargetTypeAndTargetIdAndCommitSha(targetType, targetId, commitSha)
                    : gitLinkRepository.existsByTargetTypeAndTargetIdAndPrNumber(targetType, targetId, prNumber);
            if (alreadyLinked) {
                continue;
            }

            GitLink link = GitLink.builder()
                    .project(project)
                    .targetType(targetType)
                    .targetId(targetId)
                    .source(source)
                    .commitSha(commitSha)
                    .prNumber(prNumber)
                    .prStatus(prStatus)
                    .message(message)
                    .authorLogin(authorLogin)
                    .url(url)
                    .build();
            gitLinkRepository.save(link);
            log.info("GitHub {} 자동 연결 생성: project={} target={}:{} key={}", source, project.getProjectKey(), targetType, targetId, parsed.key());
        }
    }

    private PrStatus resolvePrStatus(GithubPullRequestInfo pr) {
        if (Boolean.TRUE.equals(pr.merged())) {
            return PrStatus.MERGED;
        }
        return "closed".equalsIgnoreCase(pr.state()) ? PrStatus.CLOSED : PrStatus.OPEN;
    }

    private JsonNode readTree(byte[] rawBody) {
        try {
            return objectMapper.readTree(new String(rawBody, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ValidationException("Webhook payload를 파싱할 수 없습니다.");
        }
    }

    private void verifyTargetExists(Long projectId, TargetType targetType, Long targetId) {
        if (targetType == TargetType.REQUIREMENT) {
            var requirement = requirementRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("요구사항을 찾을 수 없습니다: " + targetId));
            if (!requirement.getProject().getId().equals(projectId)) {
                throw new ResourceNotFoundException("요구사항을 찾을 수 없습니다: " + targetId);
            }
        } else {
            var issue = issueRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("이슈를 찾을 수 없습니다: " + targetId));
            if (!issue.getProject().getId().equals(projectId)) {
                throw new ResourceNotFoundException("이슈를 찾을 수 없습니다: " + targetId);
            }
        }
    }
}
