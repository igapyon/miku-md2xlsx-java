# Upstream CLI Mapping

| Upstream CLI option | Java CLI option | Status |
| --- | --- | --- |
| `<input.md>` | `<input.md>` | Ported |
| `--out <file>` | `--out <file>` | Ported |
| `--sheet-mode <mode>` | `--sheet-mode <mode>` | Ported |
| `--sheet-heading-depth <n>` | `--sheet-heading-depth <n>` | Ported |
| `--title <value>` | `--title <value>` | Ported |
| `--table-style <mode>` | `--table-style <mode>` | Accepted; current Java styles are initial subset |
| `--no-header-row` | `--no-header-row` | Ported |
| `--help` | `--help` | Ported |
| `--version` | `--version` | Ported |

Java command form:

```sh
java -jar target/miku-md2xlsx-java-0.5.0.jar <input.md> --out <output.xlsx> [options]
```

Known differences:

- Local image embedding has initial support for local image references that can
  be read relative to the input Markdown file.
- Detailed table styling is not yet at upstream parity.
- Rich text has initial support for common Markdown inline styles, but broader
  upstream fixture parity is still pending.
- Hyperlink and merge marker support has initial Java coverage, but broader
  upstream fixture parity is still pending.
- Output is a valid basic XLSX package, but byte-level parity with the Node
  runtime is not expected at this stage.
