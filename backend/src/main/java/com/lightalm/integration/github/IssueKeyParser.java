package com.lightalm.integration.github;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 커밋 메시지/PR 제목·본문에서 {PROJECT_KEY}-\d+(이슈) 또는 {PROJECT_KEY}-R\d+(요구사항) 패턴을 추출한다(§4.9).
 */
public final class IssueKeyParser {

    private IssueKeyParser() {
    }

    public record ParsedKey(String key, boolean requirement) {
    }

    public static Set<ParsedKey> parse(String projectKey, String... texts) {
        Pattern pattern = Pattern.compile(Pattern.quote(projectKey) + "-(R)?(\\d+)");
        Set<ParsedKey> found = new LinkedHashSet<>();
        for (String text : texts) {
            if (text == null || text.isBlank()) {
                continue;
            }
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                boolean requirement = matcher.group(1) != null;
                String key = projectKey + "-" + (requirement ? "R" : "") + matcher.group(2);
                found.add(new ParsedKey(key, requirement));
            }
        }
        return found;
    }
}
