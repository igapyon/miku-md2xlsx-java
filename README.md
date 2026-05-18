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

The generated workbook is a basic `.xlsx` file. Java-side tests cover
representative upstream fixture semantics, image sizing, drawings, hyperlinks,
merges, styles, and generated workbook XML. Exact Markdown AST compatibility
with upstream `remark-parse` plus `remark-gfm` remains a migration follow-up
item.

## Requirements

- Java runtime for running the jar
- Maven for building from source

The project source and target compatibility are fixed to Java 1.8.

## Quick Start

```sh
mvn package
java -jar target/miku-md2xlsx-java-0.5.0.jar README.md --out README.xlsx
```

## Command Form

```sh
java -jar target/miku-md2xlsx-java-0.5.0.jar <input.md> --out <output.xlsx> [options]
```

Examples:

```sh
java -jar target/miku-md2xlsx-java-0.5.0.jar sample.md --out sample.xlsx
java -jar target/miku-md2xlsx-java-0.5.0.jar book.md --out book.xlsx --sheet-mode heading
java -jar target/miku-md2xlsx-java-0.5.0.jar book.md --out book.xlsx --sheet-mode heading --sheet-heading-depth 2
```

## Options

- `--out <file>`: output `.xlsx` path
- `--sheet-mode <mode>`: `single` or `heading`
- `--sheet-heading-depth <n>`: `1` or `2`
- `--title <value>`: workbook title or first sheet name
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

## Notes

- Local images are embedded when the referenced files exist next to the input
  Markdown path or under a relative subdirectory.
- Remote image URLs are not downloaded.
- Markdown table cell values are written as strings. Numeric-looking and
  date-like Markdown text is not inferred as Excel numbers or dates.
- `--table-style plain` keeps table body cells visually plain while preserving
  header styling unless `--no-header-row` is also specified.
- Byte-level parity with the Node.js runtime is not expected at this stage.

## Development

```sh
mvn test
mvn package
```

Release assets are built by `.github/workflows/release-cli-runtime.yml` for
`v*` tags or manual workflow dispatch. The workflow uploads the executable jar,
sources jar, and dist zip to the matching GitHub Release.

Repository conventions:

- Java source and target compatibility are fixed to `1.8`.
- Maven is the build tool.
- JUnit Jupiter is the test framework.
- `workplace/` is local scratch space. Only `workplace/.gitkeep` is tracked.
- `.mvn/jvm.config` is tracked for repository-local Maven JVM settings.

See `docs/` for upstream snapshot, mapping, and migration status documents.
