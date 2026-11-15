package piems;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies that the check-packages script enforces package dependency rules,
 * detects architectural cycles, and computes package instability metrics (D-05, NFR-07).
 *
 * <p>Traces to TC-NFR07-01 and TC-NFR05-02.</p>
 */
class PackageDependencyTest {

    private static final String SCRIPT_PATH = Path.of("scripts", "CheckPackages.java").toString();

    private record ProcessResult(int exitCode, List<String> outputLines) {
    }

    private ProcessResult runScript(String targetDir) throws Exception {
        String javaBin = ProcessHandle.current().info().command().orElse("java");
        ProcessBuilder pb = new ProcessBuilder(javaBin, SCRIPT_PATH, targetDir);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        int exitCode = process.waitFor();
        return new ProcessResult(exitCode, lines);
    }

    @Test
    @DisplayName("TC-NFR07-01 / D-05: check-packages exits 0 with no cycles or rule violations on src/main/java")
    void mainPackagesHaveZeroViolationsAndZeroCycles() throws Exception {
        ProcessResult result = runScript("src/main/java");

        assertThat(result.exitCode())
                .as("Production source packages must pass check-packages with exit code 0")
                .isZero();

        String combined = String.join("\n", result.outputLines());
        assertThat(combined)
                .contains("[PASS] Package Cycles: 0 cycles detected.")
                .contains("[PASS] Architectural Rules: 0 violations.")
                .contains("piems.domain")
                .contains("piems.repository");
    }

    @Test
    @DisplayName("TC-NFR07-01: check-packages exits 1 and reports deliberate violations in sample fixtures")
    void detectsDeliberateViolationsInSample() throws Exception {
        ProcessResult result = runScript("src/test/resources/package-violation");

        assertThat(result.exitCode())
                .as("Violation sample must cause check-packages to exit with non-zero code")
                .isEqualTo(1);

        String combined = String.join("\n", result.outputLines());
        assertThat(combined)
                .contains("[FAIL] Package Cycles Detected:")
                .contains("piems.report -> piems.service -> piems.report")
                .contains("[FAIL] Rule Violations Detected:")
                .contains("D-05 Domain Isolation: BadDomain.java:3")
                .contains("D-05 UI Layering: BadUi.java:3")
                .contains("D-05 SQL Encapsulation: BadService.java:3");
    }

    @Test
    @DisplayName("TC-NFR05-02 / D-44: check-packages forbids java.net and javax.net network imports")
    void checkPackagesForbidsNetworkImports(@TempDir Path tempDir) throws Exception {
        Path pkgDir = tempDir.resolve("piems").resolve("service");
        Files.createDirectories(pkgDir);
        Path badFile = pkgDir.resolve("NetworkService.java");
        Files.writeString(badFile, """
                package piems.service;
                import java.net.URL;
                import javax.net.SocketFactory;
                public class NetworkService {}
                """);

        ProcessResult result = runScript(tempDir.toString());
        assertThat(result.exitCode()).isEqualTo(1);
        String combined = String.join("\n", result.outputLines());
        assertThat(combined)
                .contains("[FAIL] Rule Violations Detected:")
                .contains("NFR-05 Network Isolation: NetworkService.java:2 network import 'java.net.URL'")
                .contains("NFR-05 Network Isolation: NetworkService.java:3 network import 'javax.net.SocketFactory'");
    }
}
