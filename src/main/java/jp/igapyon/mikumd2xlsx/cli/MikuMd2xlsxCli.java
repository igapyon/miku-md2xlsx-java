package jp.igapyon.mikumd2xlsx.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import jp.igapyon.mikumd2xlsx.core.ImageAsset;
import jp.igapyon.mikumd2xlsx.core.Md2XlsxOptions;
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
            Path inputPath = Paths.get(options.inputPath);
            String markdown = new String(Files.readAllBytes(inputPath), StandardCharsets.UTF_8);
            options.convertOptions.setImageLoader(createImageLoader(inputPath));
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
                + "miku-md2xlsx converts a Markdown file into an Excel .xlsx workbook.\n"
                + "It is a local file converter: the input Markdown is read from disk and\n"
                + "the generated workbook is written to the --out path.\n"
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
                + "  --version                 Show version\n"
                + "\n"
                + "Examples:\n"
                + "  java -jar target/miku-md2xlsx-java-" + MikuMd2xlsxCore.VERSION + ".jar ./sample.md --out ./sample.xlsx\n"
                + "  java -jar target/miku-md2xlsx-java-" + MikuMd2xlsxCore.VERSION + ".jar ./sample.md --out ./sample.xlsx --sheet-mode heading\n"
                + "  java -jar target/miku-md2xlsx-java-" + MikuMd2xlsxCore.VERSION + ".jar ./book.md --out ./book.xlsx --sheet-mode heading --sheet-heading-depth 2\n"
                + "\n"
                + "Markdown handling notes:\n"
                + "  - Headings, paragraphs, lists, tables, code blocks, horizontal rules,\n"
                + "    links, common inline styles, merge markers, column width hints, and\n"
                + "    local PNG/JPEG/GIF image references are supported.\n"
                + "  - Table cell values are written as strings. Numeric-looking and date-like\n"
                + "    Markdown text is not inferred as Excel numbers or dates.\n"
                + "  - Relative local image references are embedded best-effort when the asset\n"
                + "    file exists next to the input Markdown. Missing, remote, and absolute\n"
                + "    image paths remain visible as text references.\n"
                + "  - miku-xlsx2md merge markers in table cells are treated as Excel merges:\n"
                + "    [←M←] extends a merge to the left, and [↑M↑] extends a merge upward.\n"
                + "  - A cell containing a single Markdown link is emitted as an Excel hyperlink\n"
                + "    when the target can be represented by Excel.\n"
                + "\n"
                + "Sheet mode notes:\n"
                + "  - single: create one worksheet from the whole Markdown document.\n"
                + "  - heading: split worksheets at headings matching --sheet-heading-depth.\n"
                + "  - Use --sheet-heading-depth 2 for miku-xlsx2md-style Markdown where # is\n"
                + "    the workbook title and ## headings are worksheet names.\n";
    }

    private Md2XlsxOptions.ImageLoader createImageLoader(final Path inputPath) {
        return new Md2XlsxOptions.ImageLoader() {
            @Override
            public ImageAsset load(String path) {
                if (path == null || path.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*") || path.startsWith("//")) {
                    return null;
                }
                try {
                    Path base = inputPath.toAbsolutePath().getParent();
                    Path resolved = base == null ? Paths.get(path) : base.resolve(path).normalize();
                    return new ImageAsset(path, Files.readAllBytes(resolved), contentTypeForPath(path));
                } catch (IOException ex) {
                    return null;
                }
            }
        };
    }

    private String contentTypeForPath(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        return "image/png";
    }
}
