# Upstream Class Mapping

| Upstream file | Java class / package | Status |
| --- | --- | --- |
| `src/ts/core.ts` | `jp.igapyon.mikumd2xlsx.core.MikuMd2xlsxCore` | Initial API shape ported |
| `src/ts/types.ts` | `CellModel`, `RowModel`, `SheetModel`, `WorkbookModel`, `Md2XlsxOptions` | Initial model subset ported |
| `src/ts/workbook-model.ts` | `MarkdownWorkbookBuilder` | Initial single/heading sheet behavior ported |
| `src/ts/markdown-text.ts`, `markdown-inline.ts`, `markdown-blocks.ts` | `MarkdownText`, `MarkdownWorkbookBuilder` | Basic text/table parsing only |
| `src/ts/xlsx-writer.ts` | `XlsxPackageBuilder` | Basic workbook package ported |
| `src/ts/xlsx-worksheet.ts` | `XlsxPackageBuilder#worksheetXml` | Basic inline string cells ported |
| `src/ts/xlsx-styles.ts` | `XlsxPackageBuilder#stylesXml` | Initial styles only |
| `src/ts/xlsx-drawing*.ts`, `xlsx-media.ts`, `image-size.ts` | Not yet ported | Pending |
| `src/ts/xlsx-hyperlinks.ts`, `markdown-links.ts` | Not yet ported | Pending |
| `src/ts/xlsx-merge.ts` | Not yet ported | Pending |
| `scripts/lib/cli-support.mjs` | `jp.igapyon.mikumd2xlsx.cli` | Initial option contract ported |

