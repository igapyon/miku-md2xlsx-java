# miku-md2xlsx-java

Java straight-conversion runtime and CLI for
[miku-md2xlsx](https://github.com/igapyon/miku-md2xlsx).

This repository currently contains the initial Java runtime skeleton. It can
read Markdown from disk and write a basic `.xlsx` workbook. The first conversion
slice covers headings, paragraphs, lists, fenced code blocks, horizontal rules,
and GitHub-style Markdown tables. Advanced upstream behavior such as embedded
images, rich text runs, hyperlinks, merge markers, column hints, and semantic
roundtrip parity is tracked as follow-up work.

## Usage

```sh
mvn package
java -jar target/miku-md2xlsx-java-0.1.0.1.jar README.md --out README.xlsx
```

Options:

- `--out <file>`: output `.xlsx` path
- `--sheet-mode <mode>`: `single` or `heading`
- `--sheet-heading-depth <n>`: `1` or `2`
- `--title <value>`: workbook title or first sheet name
- `--table-style <mode>`: `plain` or `bordered`
- `--no-header-row`: do not style the first Markdown table row as a header
- `--help`: show help
- `--version`: show version

## Development

```sh
mvn test
mvn package
```

This repository follows the miku-soft Java straight-conversion conventions:

- Java source and target compatibility are fixed to `1.8`.
- Maven is the build tool.
- JUnit Jupiter is the test framework.
- `workplace/` is local scratch space. Only `workplace/.gitkeep` is tracked.
- `.mvn/jvm.config` is tracked for repository-local Maven JVM settings.

See `docs/` for upstream snapshot, mapping, and migration status documents.

