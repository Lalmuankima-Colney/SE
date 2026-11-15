package piems.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for CLI bootstrap and database URL extraction (Decision D-44, FR-18).
 *
 * <p>Traces to TC-FR18-07.</p>
 */
class MainTest {

    @Test
    @DisplayName("TC-FR18-07 / D-44: extractDbUrl auto-prepends jdbc:sqlite: prefix when missing")
    void extractDbUrlAutoPrependsJdbcSqlitePrefix() {
        assertThat(Main.extractDbUrl(new String[]{"custom.db"}))
                .isEqualTo("jdbc:sqlite:custom.db");
        assertThat(Main.extractDbUrl(new String[]{"data/finance.sqlite"}))
                .isEqualTo("jdbc:sqlite:data/finance.sqlite");
        assertThat(Main.extractDbUrl(new String[]{"--gui", "myledger.db"}))
                .isEqualTo("jdbc:sqlite:myledger.db");
        assertThat(Main.extractDbUrl(new String[]{"jdbc:sqlite:already_prefixed.db"}))
                .isEqualTo("jdbc:sqlite:already_prefixed.db");
        assertThat(Main.extractDbUrl(new String[]{}))
                .isEqualTo("jdbc:sqlite:piems.db");
    }
}
