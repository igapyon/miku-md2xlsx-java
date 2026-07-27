# miku-md2xlsx-java

`miku-md2xlsx-java` converts Markdown files to Excel `.xlsx` workbooks from a
local Java command line.

This is the Java straight-conversion runtime for
[miku-md2xlsx](https://github.com/igapyon/miku-md2xlsx). It is intended for
local conversion, build automation, and Java-centered environments where a
single executable jar is easier to use than the Node.js CLI.

## What It Converts

The current Java runtime supports the following Markdown features:

- headings, paragraphs, lists, fenced code blocks, and horizontal rules
- GitHub-style Markdown tables
- local PNG/JPEG/GIF image references, resolved relative to the input Markdown
  file
- common inline styles: bold, italic, strike, underline, and line breaks
- external hyperlinks and internal `Sheet!A1`-style links
- miku-xlsx2md merge markers: `[←M←]` and `[↑M↑]`
- worksheet column width hints

The generated workbook is an Open XML `.xlsx` workbook. Java-side tests cover
representative upstream fixture semantics, image sizing, drawings, hyperlinks,
merges, styles, and generated workbook XML. Exact Markdown AST compatibility
with upstream `remark-parse` plus `remark-gfm` is optional future work rather
than a bounded migration item.
Generated XLSX package entries use ZIP DEFLATE compression.

## Requirements

- Java 8 or later runtime for running the jar
- Maven for building from source

The project source and target compatibility are fixed to Java 1.8.

## Quick Start

Download `miku-md2xlsx-java-0.10.0.jar` from the GitHub Release, then run:

```sh
java -jar miku-md2xlsx-java-0.10.0.jar README.md --out README.xlsx
```

## Command Form

```sh
java -jar miku-md2xlsx-java-0.10.0.jar <input.md> --out <output.xlsx> [options]
```

Examples:

```sh
java -jar miku-md2xlsx-java-0.10.0.jar sample.md --out sample.xlsx
java -jar miku-md2xlsx-java-0.10.0.jar sample.md --out sample.xlsx --template template.xlsx
java -jar miku-md2xlsx-java-0.10.0.jar book.md --out book.xlsx --sheet-mode heading
java -jar miku-md2xlsx-java-0.10.0.jar exported.md --out restored.xlsx --input-dialect miku-xlsx2md
```

The CLI reads Markdown as UTF-8, creates missing parent directories for
`--out`, and overwrites an existing output file. A normal conversion generates
only the requested `.xlsx` file.

`stdout` is used for `--help` and `--version`; a successful conversion is
otherwise silent. Usage errors and conversion failures are written to
`stderr`. The CLI does not currently provide `--summary` or other
machine-readable terminal output.

Exit codes are:

- `0`: conversion success, `--help`, `--version`, or zero-argument help
- `1`: input/output or conversion failure
- `2`: invalid CLI usage, including missing arguments or unknown options

## Options

- `--out <file>`: output `.xlsx` path
- `--template <file>`: use an existing `.xlsx` as the sheet-format source
- `--input-dialect <name>`: `markdown` or early-access `miku-xlsx2md`
- `--sheet-mode <mode>`: `single` or `heading`
- `--sheet-heading-depth <n>`: `1` or `2`
- `--title <value>`: sheet name in `single` mode, or the fallback sheet name
  before `heading` mode splits worksheets
- `--table-style <mode>`: `plain` or `bordered`
- `--no-header-row`: do not style the first Markdown table row as a header
- `--help`: show help
- `--version`: show version

## Sheet Modes

- `single`: create one worksheet from the whole Markdown file.
- `heading`: split worksheets at headings that match `--sheet-heading-depth`.

Use `--sheet-heading-depth 2` for miku-xlsx2md-style Markdown where the first
level heading is the workbook title and second level headings represent
worksheets.

## Template Mode

`--template` reuses template workbook styles, theme parts, and supported
worksheet settings. Generated sheets replace matching template sheets; extra
generated sheets reuse the rightmost template sheet as their formatting base.
Existing template values, formulas, charts, drawings, tables, pivot data, and
shared strings are not preserved as workbook content.

## miku-xlsx2md Dialect

`--input-dialect miku-xlsx2md` restores exact `## Sheet:` names and
`### Table: N (A1-C4)` table anchors emitted by miku-xlsx2md. This early-access
mode rejects malformed structural markers and cannot be combined with
`--sheet-mode` or `--sheet-heading-depth`.

## Notes

- Local images are embedded when the referenced files exist next to the input
  Markdown path or under a relative subdirectory.
- Remote image URLs are not downloaded.
- Markdown table cell values are written as strings. Numeric-looking and
  date-like Markdown text is not inferred as Excel numbers or dates.
- `--table-style plain` keeps table body cells visually plain while preserving
  header styling unless `--no-header-row` is also specified.
- Byte-level parity with the Node.js runtime is not a goal; representative
  tests cover semantic workbook output.

## Development

```sh
mvn test
mvn package
java -jar target/miku-md2xlsx-java-0.10.0.jar --help
```

Release assets are built by `.github/workflows/release-cli-runtime.yml` for
`v*` tags or manual workflow dispatch. The workflow uploads the executable jar,
and sources jar to the matching GitHub Release.

Repository conventions:

- Java source and target compatibility are fixed to `1.8`.
- Maven is the build tool.
- JUnit Jupiter is the test framework.
- `vendor/miku-ms-office-core-java/` contains the shared Office core release
  jar used for low-level ZIP/OPC/XML package helpers.
- `workplace/` is local scratch space. Only `workplace/.gitkeep` is tracked.
- `.mvn/jvm.config` is tracked for repository-local Maven JVM settings.

See `docs/` for upstream snapshot, mapping, and migration status documents.
