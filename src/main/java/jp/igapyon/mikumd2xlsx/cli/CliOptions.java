package jp.igapyon.mikumd2xlsx.cli;

import jp.igapyon.mikumd2xlsx.core.Md2XlsxOptions;

class CliOptions {
    String inputPath;
    String outPath;
    String templatePath;
    boolean help;
    boolean version;
    boolean sheetModeSpecified;
    boolean sheetHeadingDepthSpecified;
    final Md2XlsxOptions convertOptions = new Md2XlsxOptions();

    static CliOptions parse(String[] args) {
        CliOptions options = new CliOptions();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--help".equals(arg)) {
                options.help = true;
                return options;
            } else if ("--version".equals(arg)) {
                options.version = true;
                return options;
            } else if ("--out".equals(arg)) {
                options.outPath = requireValue(args, ++i, "--out");
            } else if ("--template".equals(arg)) {
                options.templatePath = requireValue(args, ++i, "--template");
            } else if ("--input-dialect".equals(arg)) {
                options.convertOptions.setInputDialect(readInputDialect(requireValue(args, ++i, "--input-dialect")));
            } else if ("--sheet-mode".equals(arg)) {
                options.sheetModeSpecified = true;
                options.convertOptions.setSheetMode(readSheetMode(requireValue(args, ++i, "--sheet-mode")));
            } else if ("--sheet-heading-depth".equals(arg)) {
                options.sheetHeadingDepthSpecified = true;
                options.convertOptions.setSheetHeadingDepth(readSheetHeadingDepth(requireValue(args, ++i, "--sheet-heading-depth")));
            } else if ("--title".equals(arg)) {
                options.convertOptions.setTitle(requireValue(args, ++i, "--title"));
            } else if ("--table-style".equals(arg)) {
                options.convertOptions.setTableStyle(readTableStyle(requireValue(args, ++i, "--table-style")));
            } else if ("--no-header-row".equals(arg)) {
                options.convertOptions.setHeaderRow(false);
            } else if (arg.startsWith("--")) {
                throw new IllegalArgumentException("Unknown option: " + arg);
            } else if (options.inputPath == null) {
                options.inputPath = arg;
            } else {
                throw new IllegalArgumentException("Unexpected argument: " + arg);
            }
        }
        if ("miku-xlsx2md".equals(options.convertOptions.getInputDialect())
                && (options.sheetModeSpecified || options.sheetHeadingDepthSpecified)) {
            throw new IllegalArgumentException("--input-dialect miku-xlsx2md cannot be combined with --sheet-mode or --sheet-heading-depth.");
        }
        return options;
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length || args[index].startsWith("--")) {
            throw new IllegalArgumentException(option + " requires a value.");
        }
        return args[index];
    }

    private static String readSheetMode(String value) {
        if (!"single".equals(value) && !"heading".equals(value)) {
            throw new IllegalArgumentException("--sheet-mode must be single or heading.");
        }
        return value;
    }

    private static int readSheetHeadingDepth(String value) {
        if (!"1".equals(value) && !"2".equals(value)) {
            throw new IllegalArgumentException("--sheet-heading-depth must be 1 or 2.");
        }
        return Integer.parseInt(value);
    }

    private static String readTableStyle(String value) {
        if (!"plain".equals(value) && !"bordered".equals(value)) {
            throw new IllegalArgumentException("--table-style must be plain or bordered.");
        }
        return value;
    }

    private static String readInputDialect(String value) {
        if (!"markdown".equals(value) && !"miku-xlsx2md".equals(value)) {
            throw new IllegalArgumentException("--input-dialect must be markdown or miku-xlsx2md.");
        }
        return value;
    }
}
