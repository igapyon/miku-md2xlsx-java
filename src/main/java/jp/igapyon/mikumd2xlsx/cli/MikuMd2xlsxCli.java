package jp.igapyon.mikumd2xlsx.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import jp.igapyon.mikumd2xlsx.core.MikuMd2xlsxCore;

public class MikuMd2xlsxCli {
    public static void main(String[] args) {
        int exitCode = new MikuMd2xlsxCli().run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public int run(String[] args, PrintStream out, PrintStream err) {
        CliOptions options;
        try {
            options = CliOptions.parse(args);
        } catch (IllegalArgumentException ex) {
            err.println(ex.getMessage());
            return 2;
        }
        if (options.help || args.length == 0) {
            out.print(helpText());
            return 0;
        }
        if (options.version) {
            out.println(MikuMd2xlsxCore.VERSION);
            return 0;
        }
        if (options.inputPath == null) {
            err.println("Input Markdown file is required.");
            return 2;
        }
        if (options.outPath == null) {
            err.println("--out <file> is required.");
            return 2;
        }
        try {
            String markdown = new String(Files.readAllBytes(Paths.get(options.inputPath)), StandardCharsets.UTF_8);
            byte[] workbook = new MikuMd2xlsxCore().md2xlsx(markdown, options.convertOptions);
            Path outPath = Paths.get(options.outPath);
            Path parent = outPath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(outPath, workbook);
            return 0;
        } catch (IOException ex) {
            err.println(ex.getMessage());
            return 1;
        } catch (RuntimeException ex) {
            err.println(ex.getMessage());
            return 1;
        }
    }

    public static String helpText() {
        return "miku-md2xlsx " + MikuMd2xlsxCore.VERSION + "\n"
                + "\n"
                + "Usage:\n"
                + "  java -jar target/miku-md2xlsx-java-" + MikuMd2xlsxCore.VERSION + ".jar <input.md> --out <output.xlsx> [options]\n"
                + "  java -jar target/miku-md2xlsx-java-" + MikuMd2xlsxCore.VERSION + ".jar --help\n"
                + "  java -jar target/miku-md2xlsx-java-" + MikuMd2xlsxCore.VERSION + ".jar --version\n"
                + "\n"
                + "Arguments:\n"
                + "  <input.md>                Input Markdown file path\n"
                + "\n"
                + "Options:\n"
                + "  --out <file>              Output .xlsx path\n"
                + "  --sheet-mode <mode>       single or heading (default: single)\n"
                + "  --sheet-heading-depth <n> Heading depth for sheet splits: 1 or 2 (default: 1)\n"
                + "  --title <value>           Workbook title or first sheet name\n"
                + "  --table-style <mode>      plain or bordered (default: bordered)\n"
                + "  --no-header-row           Do not style first Markdown table row as a header\n"
                + "  --help                    Show this help\n"
                + "  --version                 Show version\n";
    }
}

