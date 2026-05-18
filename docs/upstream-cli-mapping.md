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
java -jar target/miku-md2xlsx-java-0.1.0.1.jar <input.md> --out <output.xlsx> [options]
```

Known differences:

- Local image embedding is not yet ported.
- Rich text, hyperlinks, merge markers, and detailed table styling are not yet
  at upstream parity.
- Output is a valid basic XLSX package, but byte-level parity with the Node
  runtime is not expected at this stage.

