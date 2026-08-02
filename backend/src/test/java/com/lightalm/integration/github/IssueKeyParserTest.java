package com.lightalm.integration.github;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class IssueKeyParserTest {

    @Test
    void parse_findsIssueKeyPattern() {
        Set<IssueKeyParser.ParsedKey> found = IssueKeyParser.parse("LALM", "LALM-101 fix login bug");

        assertThat(found).containsExactly(new IssueKeyParser.ParsedKey("LALM-101", false));
    }

    @Test
    void parse_findsRequirementKeyPattern() {
        Set<IssueKeyParser.ParsedKey> found = IssueKeyParser.parse("LALM", "implements LALM-R7 login form");

        assertThat(found).containsExactly(new IssueKeyParser.ParsedKey("LALM-R7", true));
    }

    @Test
    void parse_findsMultipleKeysAcrossSeveralTexts() {
        Set<IssueKeyParser.ParsedKey> found = IssueKeyParser.parse("LALM", "LALM-1 fix bug", "relates to LALM-R2 and LALM-3");

        assertThat(found).containsExactlyInAnyOrder(
                new IssueKeyParser.ParsedKey("LALM-1", false),
                new IssueKeyParser.ParsedKey("LALM-R2", true),
                new IssueKeyParser.ParsedKey("LALM-3", false));
    }

    @Test
    void parse_ignoresKeysFromOtherProjects() {
        Set<IssueKeyParser.ParsedKey> found = IssueKeyParser.parse("LALM", "OTHER-101 unrelated change");

        assertThat(found).isEmpty();
    }

    @Test
    void parse_ignoresNullOrBlankText() {
        Set<IssueKeyParser.ParsedKey> found = IssueKeyParser.parse("LALM", null, "", "  ");

        assertThat(found).isEmpty();
    }
}
