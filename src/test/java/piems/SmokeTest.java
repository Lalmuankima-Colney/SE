package piems;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Basic smoke test verifying the Maven build skeleton, testing framework,
 * and execution environment.
 *
 * <p>Traces to: TC-NFR06-01 (Portability and execution on JDK 17).</p>
 */
class SmokeTest {

    /**
     * Verifies that the test engine executes and basic assertions succeed.
     */
    @Test
    @DisplayName("TC-NFR06-01: Build and test skeleton executes successfully")
    void buildRuns() {
        assertTrue(true, "Maven skeleton, compiler, and JUnit 5 runner operate as expected");
    }
}
