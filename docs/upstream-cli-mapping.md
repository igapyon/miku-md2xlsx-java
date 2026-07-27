# Upstream CLI Mapping

| Upstream CLI option | Java CLI option | Status |
| --- | --- | --- |
| `<input.md>` | `<input.md>` | Ported |
| `--out <file>` | `--out <file>` | Ported |
| `--template <file>` | `--template <file>` | Ported |
| `--input-dialect <name>` | `--input-dialect <name>` | Ported |
| `--sheet-mode <mode>` | `--sheet-mode <mode>` | Ported |
| `--sheet-heading-depth <n>` | `--sheet-heading-depth <n>` | Ported |
| `--title <value>` | `--title <value>` | Ported |
| `--table-style <mode>` | `--table-style <mode>` | Ported |
| `--no-header-row` | `--no-header-row` | Ported |
| `--help` | `--help` | Ported |
| `--version` | `--version` | Ported |

Java command form:

```sh
java -jar miku-md2xlsx-java-0.10.0.jar <input.md> --out <output.xlsx> [options]
```

Known differences:

- The Release Asset command is the primary public command. Source-tree
  `target/` paths are limited to development documentation.
- Help resolves the actual executable JAR filename, including a dot-suffixed
  Release Asset name.
- Zero arguments print help and exit with code 0. Invalid usage exits with
  code 2, while input/output or conversion failures exit with code 1.
- Successful conversion writes only the requested `.xlsx` and is silent on
  stdout. Help and version use stdout; diagnostics use stderr. No `--summary`
  or other machine-readable terminal output is currently provided.
- Missing parent directories for `--out` are created, and existing output
  files are overwritten.
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
- Template mode reuses formatting-related parts but intentionally replaces
  template workbook content.
- `miku-xlsx2md` input dialect is early access and restores emitted sheet names
  and table anchors; it cannot be combined with generic sheet split options.
