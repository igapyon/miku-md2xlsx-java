package jp.igapyon.mikumd2xlsx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import jp.igapyon.mikumd2xlsx.cli.MikuMd2xlsxCli;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MikuMd2xlsxCliTest {
    @TempDir
    Path tempDir;

    @Test
    void printsVersion() {
        CliRun run = runCli("--version");
        assertEquals(0, run.exitCode);
        assertEquals("0.10.0\n", run.out);
        assertEquals("", run.err);
    }

    @Test
    void printsHelp() {
        CliRun run = runCli("--help");
        String help = run.out;
        assertEquals(0, run.exitCode);
        assertEquals("", run.err);
        assertTrue(help.contains("miku-md2xlsx 0.10.0"));
        assertTrue(help.contains("miku-md2xlsx converts a Markdown file into an Excel .xlsx workbook."));
        assertTrue(help.contains("Usage:"));
        assertTrue(help.contains("Inputs:"));
        assertTrue(help.contains("Outputs:"));
        assertTrue(help.contains("Generated artifacts:"));
        assertTrue(help.contains("Overwrite behavior:"));
        assertTrue(help.contains("Exit codes:"));
        assertTrue(help.contains("stdout  Help and version text."));
        assertTrue(help.contains("stderr  CLI usage errors and conversion or file-system failures."));
        assertTrue(help.contains("No --summary or other machine-readable terminal output is currently provided."));
        assertTrue(help.contains("no arguments prints this help and exits with code 0."));
        assertTrue(help.contains("java -jar miku-md2xlsx-java-0.10.0.jar"));
        assertTrue(!help.contains("java -jar target/"));
        assertTrue(help.contains("--out <file>"));
        assertTrue(help.contains("--version"));
        assertTrue(help.contains("Examples:"));
        assertTrue(help.contains("Markdown handling notes:"));
        assertTrue(help.contains("Table cell values are written as strings."));
        assertTrue(help.contains("Sheet mode notes:"));
        assertTrue(help.contains("Template mode notes:"));
        assertTrue(help.contains("miku-xlsx2md dialect notes:"));
        assertTrue(help.contains("--template <file>"));
        assertTrue(help.contains("--input-dialect <name>"));
    }

    @Test
    void printsHelpWhenNoArguments() {
        CliRun run = runCli();
        assertEquals(0, run.exitCode);
        assertTrue(run.out.contains("Usage:"));
        assertEquals("", run.err);
    }

    @Test
    void convertsFile() throws Exception {
        Path input = tempDir.resolve("sample.md");
        Path output = tempDir.resolve("sample.xlsx");
        Files.write(input, "# Title\n\n| A | B |\n|---|---|\n| 1 | 2 |\n".getBytes(StandardCharsets.UTF_8));
        CliRun run = runCli(input.toString(), "--out", output.toString());
        assertEquals(0, run.exitCode);
        assertEquals("", run.err);
        assertTrue(Files.size(output) > 0);
    }

    @Test
    void createsMissingOutputParentDirectories() throws Exception {
        Path input = tempDir.resolve("sample.md");
        Path output = tempDir.resolve("nested/output/sample.xlsx");
        Files.write(input, "# Title\n".getBytes(StandardCharsets.UTF_8));
        CliRun run = runCli(input.toString(), "--out", output.toString());
        assertEquals(0, run.exitCode);
        assertEquals("", run.out);
        assertEquals("", run.err);
        assertTrue(Files.size(output) > 0);
    }

    @Test
    void rejectsMissingOut() throws Exception {
        Path input = tempDir.resolve("sample.md");
        Files.write(input, "# Title\n".getBytes(StandardCharsets.UTF_8));
        CliRun run = runCli(input.toString());
        assertEquals(2, run.exitCode);
        assertEquals("--out <file> is required.\n", run.err);
        assertEquals("", run.out);
    }

    @Test
    void rejectsMissingInput() {
        CliRun run = runCli("--out", tempDir.resolve("out.xlsx").toString());
        assertEquals(2, run.exitCode);
        assertEquals("Input Markdown file is required.\n", run.err);
        assertEquals("", run.out);
    }

    @Test
    void rejectsUnknownOption() {
        CliRun run = runCli("--unknown");
        assertEquals(2, run.exitCode);
        assertEquals("Unknown option: --unknown\n", run.err);
        assertEquals("", run.out);
    }

    @Test
    void rejectsUnexpectedArgument() {
        CliRun run = runCli("one.md", "two.md", "--out", "out.xlsx");
        assertEquals(2, run.exitCode);
        assertEquals("Unexpected argument: two.md\n", run.err);
        assertEquals("", run.out);
    }

    @Test
    void rejectsMissingOptionValue() {
        CliRun run = runCli("sample.md", "--out");
        assertEquals(2, run.exitCode);
        assertEquals("--out requires a value.\n", run.err);
        assertEquals("", run.out);
    }

    @Test
    void rejectsInvalidSheetMode() {
        CliRun run = runCli("sample.md", "--out", "sample.xlsx", "--sheet-mode", "chapter");
        assertEquals(2, run.exitCode);
        assertEquals("--sheet-mode must be single or heading.\n", run.err);
        assertEquals("", run.out);
    }

    @Test
    void rejectsInvalidSheetHeadingDepth() {
        CliRun run = runCli("sample.md", "--out", "sample.xlsx", "--sheet-heading-depth", "3");
        assertEquals(2, run.exitCode);
        assertEquals("--sheet-heading-depth must be 1 or 2.\n", run.err);
        assertEquals("", run.out);
    }

    @Test
    void rejectsInvalidTableStyle() {
        CliRun run = runCli("sample.md", "--out", "sample.xlsx", "--table-style", "grid");
        assertEquals(2, run.exitCode);
        assertEquals("--table-style must be plain or bordered.\n", run.err);
        assertEquals("", run.out);
    }

    @Test
    void rejectsInvalidInputDialect() {
        CliRun run = runCli("sample.md", "--out", "sample.xlsx", "--input-dialect", "invalid");
        assertEquals(2, run.exitCode);
        assertEquals("--input-dialect must be markdown or miku-xlsx2md.\n", run.err);
    }

    @Test
    void rejectsGenericSheetOptionsWithXlsx2mdDialect() {
        CliRun run = runCli("sample.md", "--out", "sample.xlsx", "--input-dialect", "miku-xlsx2md",
                "--sheet-mode", "heading");
        assertEquals(2, run.exitCode);
        assertTrue(run.err.contains("cannot be combined with --sheet-mode or --sheet-heading-depth"));
    }

    @Test
    void reportsRuntimeIoErrors() {
        CliRun run = runCli(tempDir.resolve("missing.md").toString(), "--out", tempDir.resolve("out.xlsx").toString());
        assertEquals(1, run.exitCode);
        assertTrue(run.err.contains("missing.md"));
        assertEquals("", run.out);
    }

    private CliRun runCli(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exitCode = new MikuMd2xlsxCli().run(args, new PrintStream(out), new PrintStream(err));
        return new CliRun(
                exitCode,
                new String(out.toByteArray(), StandardCharsets.UTF_8),
                new String(err.toByteArray(), StandardCharsets.UTF_8));
    }

    private static final class CliRun {
        private final int exitCode;
        private final String out;
        private final String err;

        private CliRun(int exitCode, String out, String err) {
            this.exitCode = exitCode;
            this.out = out;
            this.err = err;
        }
    }
}
