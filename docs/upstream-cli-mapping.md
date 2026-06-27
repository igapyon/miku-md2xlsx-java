# Upstream CLI Mapping

| Upstream CLI option | Java CLI option | Status |
| --- | --- | --- |
| `<input.md>` | `<input.md>` | Ported |
| `--out <file>` | `--out <file>` | Ported |
| `--sheet-mode <mode>` | `--sheet-mode <mode>` | Ported |
| `--sheet-heading-depth <n>` | `--sheet-heading-depth <n>` | Ported |
| `--title <value>` | `--title <value>` | Ported |
| `--table-style <mode>` | `--table-style <mode>` | Ported |
| `--no-header-row` | `--no-header-row` | Ported |
| `--help` | `--help` | Ported |
| `--version` | `--version` | Ported |

Java command form:

```sh
java -jar target/miku-md2xlsx-java-0.6.5.jar <input.md> --out <output.xlsx> [options]
```

Known differences:

- Local image references are embedded when the referenced PNG/JPEG/GIF files
  are available relative to the input Markdown file.
- Remote image URLs are not downloaded.
- Markdown table cell values are written as strings. Numeric-looking and
  date-like Markdown text is not inferred as Excel numbers or dates.
- Java-side coverage includes representative upstream fixture semantics,
  table styling modes, rich text, hyperlinks, merge markers, drawings, image
  sizing, and generated workbook XML.
- Byte-level parity with the Node.js runtime is not a goal; representative
  tests cover semantic workbook output.
