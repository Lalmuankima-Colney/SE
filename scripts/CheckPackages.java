import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Single-file architectural checker that verifies package dependencies, cycles,
 * instability metrics, and adherence to Decision D-05.
 */
public final class CheckPackages {

    private static final Pattern PKG_PATTERN = Pattern.compile("^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;");
    private static final Pattern IMP_PATTERN = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([a-zA-Z0-9_.]+)\\s*;");

    private CheckPackages() {
    }

    /**
     * CLI entry point. Exits with 0 on clean pass, 1 on any violation or cycle.
     *
     * @param args optional target directory to scan (defaults to src/main/java)
     */
    public static void main(String[] args) {
        Path targetDir = args.length > 0 ? Path.of(args[0]) : Path.of("src/main/java");
        int exitCode = run(targetDir, System.out);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /**
     * Executes package analysis and prints the report to the given output stream.
     *
     * @param targetDir directory to scan
     * @param out       destination print stream
     * @return 0 if all rules pass, 1 if violations or cycles are detected
     */
    public static int run(Path targetDir, PrintStream out) {
        try {
            AnalysisResult result = analyze(targetDir);
            printReport(result, out);
            return (result.violations().isEmpty() && result.cycles().isEmpty()) ? 0 : 1;
        } catch (IOException e) {
            out.println("[ERROR] Failed to scan packages: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Analyzes Java source files under the target directory.
     *
     * @param targetDir directory to scan
     * @return analysis results including metrics, cycles, and rule violations
     * @throws IOException if file reading fails
     */
    public static AnalysisResult analyze(Path targetDir) throws IOException {
        AnalysisData data = new AnalysisData();
        if (Files.exists(targetDir)) {
            try (Stream<Path> stream = Files.walk(targetDir)) {
                List<Path> javaFiles = stream.filter(p -> p.toString().endsWith(".java")).toList();
                for (Path file : javaFiles) {
                    scanFile(file, data);
                }
            }
        }
        computeCoupling(data);
        List<String> cycles = detectCycles(data.dependencies);
        List<String> violations = checkRules(data);
        return new AnalysisResult(targetDir, data.packages, data.ca, data.ce, data.dependencies, cycles, violations);
    }

    private static void scanFile(Path file, AnalysisData data) throws IOException {
        String currentPkg = null;
        int lineNum = 0;
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                Matcher pkgMatcher = PKG_PATTERN.matcher(line);
                if (pkgMatcher.find()) {
                    currentPkg = pkgMatcher.group(1);
                    data.packages.add(currentPkg);
                }
                Matcher impMatcher = IMP_PATTERN.matcher(line);
                if (impMatcher.find() && currentPkg != null) {
                    recordImport(file, lineNum, currentPkg, impMatcher.group(1), data);
                }
            }
        }
    }

    private static void recordImport(Path file, int line, String pkg, String imported, AnalysisData data) {
        data.rawImports.add(new ImportRecord(file, line, pkg, imported));
        String targetPkg = extractTargetPackage(imported);
        if (targetPkg != null && !targetPkg.equals(pkg)) {
            data.dependencies.computeIfAbsent(pkg, k -> new TreeSet<>()).add(targetPkg);
        }
    }

    private static String extractTargetPackage(String imported) {
        if (imported.startsWith("piems.")) {
            String[] parts = imported.split("\\.");
            if (parts.length >= 2) {
                return parts[0] + "." + parts[1];
            }
        }
        return null;
    }

    private static void computeCoupling(AnalysisData data) {
        for (String pkg : data.packages) {
            data.dependencies.putIfAbsent(pkg, new TreeSet<>());
        }
        for (String pkg : data.packages) {
            int ce = data.dependencies.getOrDefault(pkg, Collections.emptySet()).size();
            data.ce.put(pkg, ce);
            int ca = 0;
            for (Map.Entry<String, Set<String>> entry : data.dependencies.entrySet()) {
                if (!entry.getKey().equals(pkg) && entry.getValue().contains(pkg)) {
                    ca++;
                }
            }
            data.ca.put(pkg, ca);
        }
    }

    private static List<String> checkRules(AnalysisData data) {
        List<String> violations = new ArrayList<>();
        for (ImportRecord rec : data.rawImports) {
            checkDomainRule(rec, violations);
            checkUiRule(rec, violations);
            checkSqlRule(rec, violations);
            checkNetworkRule(rec, violations);
        }
        return violations;
    }

    private static void checkNetworkRule(ImportRecord rec, List<String> violations) {
        boolean isNetwork = rec.imported().startsWith("java.net.") || rec.imported().startsWith("javax.net.");
        if (isNetwork) {
            violations.add(String.format("NFR-05 Network Isolation: %s:%d network import '%s'",
                    rec.file().getFileName(), rec.line(), rec.imported()));
        }
    }

    private static void checkDomainRule(ImportRecord rec, List<String> violations) {
        if ("piems.domain".equals(rec.pkg()) && rec.imported().startsWith("piems.")
                && !rec.imported().startsWith("piems.domain.")) {
            violations.add(String.format("D-05 Domain Isolation: %s:%d domain imports '%s'",
                    rec.file().getFileName(), rec.line(), rec.imported()));
        }
    }

    private static void checkUiRule(ImportRecord rec, List<String> violations) {
        if ("piems.ui".equals(rec.pkg()) && rec.imported().startsWith("piems.repository")) {
            violations.add(String.format("D-05 UI Layering: %s:%d ui directly imports repository '%s'",
                    rec.file().getFileName(), rec.line(), rec.imported()));
        }
    }

    private static void checkSqlRule(ImportRecord rec, List<String> violations) {
        boolean isSql = rec.imported().startsWith("java.sql.") || rec.imported().startsWith("javax.sql.");
        if (isSql && !"piems.repository".equals(rec.pkg())) {
            violations.add(String.format("D-05 SQL Encapsulation: %s:%d SQL import outside repository in '%s'",
                    rec.file().getFileName(), rec.line(), rec.pkg()));
        }
    }

    private static List<String> detectCycles(Map<String, Set<String>> graph) {
        List<String> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        List<String> path = new ArrayList<>();
        for (String node : graph.keySet()) {
            dfsCycle(node, graph, visited, path, cycles);
        }
        return cycles;
    }

    private static void dfsCycle(String curr, Map<String, Set<String>> graph,
                                 Set<String> visited, List<String> path, List<String> cycles) {
        if (path.contains(curr)) {
            int idx = path.indexOf(curr);
            List<String> cyclePath = new ArrayList<>(path.subList(idx, path.size()));
            cyclePath.add(curr);
            cycles.add(String.join(" -> ", cyclePath));
            return;
        }
        if (visited.contains(curr)) {
            return;
        }
        visited.add(curr);
        path.add(curr);
        for (String next : graph.getOrDefault(curr, Collections.emptySet())) {
            dfsCycle(next, graph, visited, path, cycles);
        }
        path.remove(path.size() - 1);
    }

    private static void printReport(AnalysisResult res, PrintStream out) {
        out.println("================================================================================");
        out.println("PIEMS Package Dependency & Metrics Report (Decision D-05)");
        out.println("Target Directory: " + res.rootDir());
        out.println("================================================================================");
        out.printf("%-18s | %-4s | %-4s | %-15s | %-25s%n", "Package", "Ca", "Ce", "Instability (I)", "Depends On");
        out.println("-------------------+------+------+-----------------+----------------------------");
        for (String pkg : new TreeSet<>(res.packages())) {
            int ca = res.ca().getOrDefault(pkg, 0);
            int ce = res.ce().getOrDefault(pkg, 0);
            double inst = (ca + ce == 0) ? 0.0 : (double) ce / (ca + ce);
            String deps = String.join(", ", res.dependencies().getOrDefault(pkg, Collections.emptySet()));
            out.printf("%-18s | %-4d | %-4d | %-15.3f | %-25s%n", pkg, ca, ce, inst, deps.isEmpty() ? "-" : deps);
        }
        out.println("================================================================================");
        printViolationsAndCycles(res, out);
    }

    private static void printViolationsAndCycles(AnalysisResult res, PrintStream out) {
        if (res.cycles().isEmpty()) {
            out.println("[PASS] Package Cycles: 0 cycles detected.");
        } else {
            out.println("[FAIL] Package Cycles Detected:");
            res.cycles().forEach(c -> out.println("  - " + c));
        }
        if (res.violations().isEmpty()) {
            out.println("[PASS] Architectural Rules: 0 violations.");
        } else {
            out.println("[FAIL] Rule Violations Detected:");
            res.violations().forEach(v -> out.println("  - " + v));
        }
    }

    private static class AnalysisData {
        private final Set<String> packages = new TreeSet<>();
        private final Map<String, Integer> ca = new HashMap<>();
        private final Map<String, Integer> ce = new HashMap<>();
        private final Map<String, Set<String>> dependencies = new HashMap<>();
        private final List<ImportRecord> rawImports = new ArrayList<>();
    }

    private record ImportRecord(Path file, int line, String pkg, String imported) {
    }

    /**
     * Analysis result data transfer object.
     *
     * @param rootDir      scanned directory
     * @param packages     detected packages
     * @param ca           afferent couplings
     * @param ce           efferent couplings
     * @param dependencies dependency map
     * @param cycles       detected cycles
     * @param violations   rule violations
     */
    public record AnalysisResult(
            Path rootDir,
            Set<String> packages,
            Map<String, Integer> ca,
            Map<String, Integer> ce,
            Map<String, Set<String>> dependencies,
            List<String> cycles,
            List<String> violations
    ) {
    }
}
