> 상위 문서: [SPEC.md](SPEC.md)

## 7. 외부 연동 상세 (GitHub / Jenkins)

### 7.1 GitHub 연동 흐름
1. 프로젝트 설정 화면에서 `repoOwner`, `repoName`, Personal Access Token, Webhook Secret 입력
2. 백엔드는 GitHub REST API(`https://api.github.com`)를 `RestClient`로 호출하여 커밋/PR 메타데이터 조회
3. 사용자가 수동으로 커밋 SHA/PR 번호를 입력해 연결하거나, GitHub Webhook이 push/PR 이벤트를 `/api/webhooks/github/{projectId}`로 전송하면 커밋 메시지에서 이슈/요구사항 키 패턴을 파싱해 자동 연결
4. 사용자는 GitHub 저장소 Settings → Webhooks에서 Payload URL을 `https://{서버주소}/api/webhooks/github/{projectId}`로, Secret을 프로젝트 설정과 동일하게 등록해야 한다(README에 안내 문구 포함)

### 7.2 Jenkins 연동 흐름
1. 프로젝트 설정 화면에서 `baseUrl`, `jobName`, API 사용자/토큰 입력
2. "빌드 트리거" 버튼 클릭 시 백엔드가 Jenkins API(`POST {baseUrl}/job/{jobName}/build`, Basic Auth)를 호출
3. Jenkins Job의 Post-build Action에서 빌드 종료 시 아래 형태의 JSON을 `/api/webhooks/jenkins/{projectId}`로 POST하도록 구성(HTTP Request Plugin 또는 curl 스크립트 사용):
```json
{
  "targetType": "ISSUE",
  "targetId": 10,
  "jobName": "light-alm-backend",
  "buildNumber": 42,
  "status": "SUCCESS",
  "buildUrl": "https://jenkins.example.com/job/light-alm-backend/42/",
  "triggeredBy": "jenkins-user"
}
```
4. 헤더 `X-Jenkins-Token`으로 프로젝트별 시크릿을 검증한 뒤 `jenkins_builds` 테이블에 upsert

### 7.3 재시도/에러 처리
- 외부 API 호출 실패(네트워크 오류, 401 등)는 사용자에게 명확한 에러 메시지로 변환하여 반환(`GITHUB_API_ERROR`, `JENKINS_API_ERROR` 등 에러 코드 사용)
- 별도의 재시도 큐/비동기 처리는 Light 버전 스코프 밖(동기 호출 + 실패 시 에러 응답으로 충분)

### 7.4 보안 참고사항
- GitHub PAT, Jenkins API 토큰은 MVP 단계에서 DB 평문 저장을 허용하되, 코드 주석과 README에 "프로덕션 전환 시 Jasypt 등으로 암호화 필요"를 TODO로 명시한다.
- API 응답(GET 프로젝트 상세 등)에서 토큰 필드는 마스킹(`****`) 처리하여 반환한다.
