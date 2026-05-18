# Upstream Test Mapping

| Upstream test intent | Java test | Status |
| --- | --- | --- |
| Core Markdown to workbook conversion | `MikuMd2xlsxCoreTest.convertsMarkdownToWorkbookModel` | Initial smoke coverage |
| Heading sheet split | `MikuMd2xlsxCoreTest.supportsHeadingSheetMode` | Initial smoke coverage |
| XLSX package generation | `MikuMd2xlsxCoreTest.writesXlsxZipEntries` | Initial zip-entry coverage |
| CLI `--version` | `MikuMd2xlsxCliTest.printsVersion` | Covered |
| CLI file conversion | `MikuMd2xlsxCliTest.convertsFile` | Covered |
| `tests/md2xlsx-from-xlsx2md-*.test.js` fixture parity | Not yet mapped | Pending |
| image, hyperlink, rich text, merge, formula fixture parity | Not yet mapped | Pending |

Focused command:

```sh
mvn test
```

