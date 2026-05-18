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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int exit = new MikuMd2xlsxCli().run(new String[] {"--version"}, new PrintStream(out), new PrintStream(new ByteArrayOutputStream()));
        assertEquals(0, exit);
        assertEquals("0.1.0.1\n", new String(out.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void convertsFile() throws Exception {
        Path input = tempDir.resolve("sample.md");
        Path output = tempDir.resolve("sample.xlsx");
        Files.write(input, "# Title\n\n| A | B |\n|---|---|\n| 1 | 2 |\n".getBytes(StandardCharsets.UTF_8));
        int exit = new MikuMd2xlsxCli().run(new String[] {input.toString(), "--out", output.toString()}, new PrintStream(new ByteArrayOutputStream()), new PrintStream(new ByteArrayOutputStream()));
        assertEquals(0, exit);
        assertTrue(Files.size(output) > 0);
    }
}

