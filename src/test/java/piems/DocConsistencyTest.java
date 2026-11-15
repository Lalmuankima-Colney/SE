package piems;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Automated documentation consistency and drift verification test (Decision D-21, D-34).
 *
 * <p>Enforces exactly three checks:
 * <ol>
 *   <li>FR test IDs in COVERAGE.md Section 5 exist in TEST_PLAN.md, and verified FRs appear in test sources.</li>
 *   <li>DECISIONS.md entries equal index rows and the count quoted in ITERATION_LOG.md.</li>
 *   <li>The test count quoted in README.md equals the Surefire total read from target/surefire-reports.</li>
 * </ol>
 * </p>
 */
class DocConsistencyTest {

    private static final int THIS_CLASS_TEST_COUNT = 3;

    @Test
    @DisplayName("D-21/D-34 Check 1: COVERAGE FR test IDs exist in TEST_PLAN and verified FRs have test in source")
    void coverageTestIdsMatchTestPlanAndSource() throws IOException {
        Set<String> planTestIds = extractTestPlanIds(Path.of("docs", "TEST_PLAN.md"));
        String testSourceCode = readAllTestSourceCode(Path.of("src", "test", "java"));
        List<CoverageRequirement> frRows = parseCoverageSection5(Path.of("docs", "COVERAGE.md"));

        assertThat(frRows).isNotEmpty();
        for (CoverageRequirement req : frRows) {
            for (String tcId : req.testIds()) {
                assertThat(planTestIds)
                        .as("Test ID %s in requirement %s must exist in TEST_PLAN.md", tcId, req.id())
                        .contains(tcId);
            }
            if (req.isVerified()) {
                boolean foundInSource = req.testIds().stream().anyMatch(testSourceCode::contains);
                assertThat(foundInSource)
                        .as("Verified requirement %s must have at least one test ID in test source", req.id())
                        .isTrue();
            }
        }
    }

    @Test
    @DisplayName("D-21/D-34 Check 2: DECISIONS entries equal index rows and count quoted in ITERATION_LOG")
    void decisionCountMatchesIndexAndLog() throws IOException {
        Path decisionsPath = Path.of("docs", "DECISIONS.md");
        List<String> decLines = Files.readAllLines(decisionsPath);

        long headingCount = decLines.stream()
                .filter(line -> line.matches("^###\\s+D-\\d+.*"))
                .count();

        long indexCount = decLines.stream()
                .filter(line -> line.matches("^\\|\\s*D-\\d+\\s*\\|.*"))
                .count();

        Path logPath = Path.of("docs", "ITERATION_LOG.md");
        int quotedLogCount = extractQuotedDecisionCount(logPath);

        assertThat(headingCount)
                .as("Heading count (%d) must equal index row count (%d)", headingCount, indexCount)
                .isEqualTo(indexCount);

        assertThat(headingCount)
                .as("Heading count (%d) must equal count quoted in ITERATION_LOG (%d)",
                        headingCount, quotedLogCount)
                .isEqualTo(quotedLogCount);
    }

    @Test
    @DisplayName("D-21/D-34 Check 3: Test count quoted in README matches Surefire execution output")
    void quotedTestCountMatchesSurefire() throws Exception {
        Path readmePath = Path.of("README.md");
        int quotedCount = extractQuotedReadmeTestCount(readmePath);

        Path surefireDir = Path.of("target", "surefire-reports");
        assertThat(Files.isDirectory(surefireDir))
                .as("Surefire reports directory must exist: %s", surefireDir)
                .isTrue();

        int surefireTotal = calculateSurefireTestTotal(surefireDir);
        assertThat(quotedCount)
                .as("Quoted test count in README (%d) must match Surefire total (%d)",
                        quotedCount, surefireTotal)
                .isEqualTo(surefireTotal);
    }

    private List<CoverageRequirement> parseCoverageSection5(Path coveragePath) throws IOException {
        List<CoverageRequirement> rows = new ArrayList<>();
        List<String> lines = Files.readAllLines(coveragePath);
        boolean inSection5 = false;
        Pattern frPattern = Pattern.compile(
                "^\\|\\s*(FR-\\d+)[^|]*\\|[^|]*\\|[^|]*\\|\\s*([^|]+)\\|\\s*([^|]+)\\|");
        for (String line : lines) {
            if (line.contains("## 5. Requirement traceability matrix")) {
                inSection5 = true;
                continue;
            }
            if (inSection5 && line.startsWith("## ")) {
                break;
            }
            if (inSection5) {
                Matcher m = frPattern.matcher(line);
                if (m.find()) {
                    String reqId = m.group(1).trim();
                    List<String> testIds = extractTestIds(m.group(2));
                    String status = m.group(3).trim();
                    boolean verified = status.startsWith("Pass") || status.contains("Verified");
                    rows.add(new CoverageRequirement(reqId, testIds, verified));
                }
            }
        }
        return rows;
    }

    private Set<String> extractTestPlanIds(Path planPath) throws IOException {
        String content = Files.readString(planPath);
        Matcher matcher = Pattern.compile("TC-[A-Z0-9]+-\\d+").matcher(content);
        Set<String> ids = new HashSet<>();
        while (matcher.find()) {
            ids.add(matcher.group());
        }
        return ids;
    }

    private String readAllTestSourceCode(Path testDir) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (Stream<Path> stream = Files.walk(testDir)) {
            List<Path> javaFiles = stream.filter(p -> p.toString().endsWith(".java")).toList();
            for (Path file : javaFiles) {
                sb.append(Files.readString(file)).append('\n');
            }
        }
        return sb.toString();
    }

    private List<String> extractTestIds(String text) {
        Matcher matcher = Pattern.compile("TC-[A-Z0-9]+-\\d+").matcher(text);
        List<String> ids = new ArrayList<>();
        while (matcher.find()) {
            ids.add(matcher.group());
        }
        return ids;
    }

    private int extractQuotedDecisionCount(Path logPath) throws IOException {
        String content = Files.readString(logPath);
        Matcher matcher = Pattern.compile("Total decisions:\\s*(\\d+)").matcher(content);
        int count = -1;
        while (matcher.find()) {
            count = Integer.parseInt(matcher.group(1));
        }
        if (count != -1) {
            return count;
        }
        throw new IllegalStateException("Could not find 'Total decisions: <N>' in " + logPath);
    }

    private int extractQuotedReadmeTestCount(Path readmePath) throws IOException {
        String content = Files.readString(readmePath);
        Matcher matcher = Pattern.compile("(\\d+)\\s+tests passed").matcher(content);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        throw new IllegalStateException("Could not find '<N> tests passed' in " + readmePath);
    }

    private int calculateSurefireTestTotal(Path surefireDir) throws Exception {
        int total = 0;
        try (Stream<Path> stream = Files.list(surefireDir)) {
            List<Path> xmlFiles = stream
                    .filter(p -> p.getFileName().toString().startsWith("TEST-")
                            && p.getFileName().toString().endsWith(".xml"))
                    .toList();
            for (Path xmlFile : xmlFiles) {
                if (xmlFile.getFileName().toString().contains("DocConsistencyTest")) {
                    continue; // Skip on-disk report as it reflects a previous run while this class executes
                }
                total += parseXmlTestCount(xmlFile);
            }
            total += THIS_CLASS_TEST_COUNT;
        }
        return total;
    }

    private int parseXmlTestCount(Path xmlFile) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document doc = dbf.newDocumentBuilder().parse(xmlFile.toFile());
        Element root = doc.getDocumentElement();
        return Integer.parseInt(root.getAttribute("tests"));
    }

    private record CoverageRequirement(String id, List<String> testIds, boolean isVerified) {
    }
}
