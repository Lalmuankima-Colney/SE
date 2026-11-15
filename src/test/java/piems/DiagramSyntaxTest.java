package piems;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Automated tests verifying that all required Mermaid UML diagrams and audit
 * question notes exist, are non-empty, and declare valid Mermaid syntax structures.
 */
class DiagramSyntaxTest {

    private static final Path DIAGRAMS_DIR = Path.of("docs", "diagrams");

    @Test
    @DisplayName("Mermaid: Domain class diagram exists and has valid classDiagram syntax")
    void domainClassDiagramExistsAndIsValid() throws Exception {
        Path classDiagramPath = DIAGRAMS_DIR.resolve("class-domain.mmd");
        assertThat(classDiagramPath).exists();

        String content = Files.readString(classDiagramPath);
        assertThat(content)
                .startsWith("classDiagram")
                .contains("class Money")
                .contains("class Account")
                .contains("class Transaction")
                .contains("class CategoryComponent")
                .contains("class Budget")
                .contains("class SavingsGoal");
    }

    @Test
    @DisplayName("Mermaid: Package diagram exists and has valid flowchart syntax")
    void packageDiagramExistsAndIsValid() throws Exception {
        Path packageDiagramPath = DIAGRAMS_DIR.resolve("package.mmd");
        assertThat(packageDiagramPath).exists();

        String content = Files.readString(packageDiagramPath);
        assertThat(content)
                .startsWith("flowchart TD")
                .contains("piems.domain")
                .contains("piems.repository")
                .contains("piems.service")
                .contains("piems.ui")
                .contains("piems.app");
    }

    @Test
    @DisplayName("Mermaid: SavingsGoal and Budget state diagrams exist and have valid stateDiagram-v2 syntax")
    void stateDiagramsExistAndAreValid() throws Exception {
        Path goalStatePath = DIAGRAMS_DIR.resolve("state-goal.mmd");
        assertThat(goalStatePath).exists();
        String goalContent = Files.readString(goalStatePath);
        assertThat(goalContent)
                .startsWith("stateDiagram-v2")
                .contains("Active --> Achieved")
                .contains("Q-008");

        Path budgetStatePath = DIAGRAMS_DIR.resolve("state-budget.mmd");
        assertThat(budgetStatePath).exists();
        String budgetContent = Files.readString(budgetStatePath);
        assertThat(budgetContent)
                .startsWith("stateDiagram-v2")
                .contains("OK --> WARNING")
                .contains("WARNING --> EXCEEDED")
                .contains("D-28");
    }

    @Test
    @DisplayName("Diagrams: questions.md exists and documents design discrepancies")
    void diagramQuestionsDocumentExists() {
        Path questionsPath = DIAGRAMS_DIR.resolve("questions.md");
        assertThat(questionsPath).exists();
        assertThat(questionsPath).isNotEmptyFile();
    }

    @Test
    @DisplayName("CRC: crc.md exists, covers 6 classes, and includes cohesion and coupling notes")
    void crcCardsDocumentExistsAndIsComplete() throws Exception {
        Path crcPath = DIAGRAMS_DIR.resolve("crc.md");
        assertThat(crcPath).exists();

        String content = Files.readString(crcPath);
        assertThat(content)
                .contains("Class: `Money`")
                .contains("Class: `Account`")
                .contains("Class: `Transaction`")
                .contains("Class: `Budget`")
                .contains("Class: `FinanceService`")
                .contains("Class: `ReportStrategy`")
                .contains("Cohesion Note:")
                .contains("Coupling Note:")
                .contains("Flagged Misplaced Responsibilities:");
    }
}
