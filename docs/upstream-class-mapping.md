# Upstream Class Mapping

| Upstream file | Java class / package | Status |
| --- | --- | --- |
| `src/ts/core.ts` | `jp.igapyon.mikumd2xlsx.core.MikuMd2xlsxCore` | API shape ported for `markdownToXlsxModel`, `workbookModelToXlsx`, and `md2xlsx` |
| `src/ts/types.ts` | `CellModel`, `HyperlinkModel`, `ImageAsset`, `ImageRefModel`, `RichTextRun`, `RowModel`, `SheetModel`, `WorkbookModel`, `Md2XlsxOptions` | Model fields ported, including `imageAssets`, `templateXlsx`, and `inputDialect` |
| `src/ts/workbook-model.ts`, `sheet-builder.ts` | `MarkdownWorkbookBuilder` | Initial single/heading sheet behavior, preface rows, title handling, sheet name handling, and xlsx2md-style internal hyperlink target normalization ported |
| `src/ts/markdown-text.ts`, `markdown-inline.ts`, `markdown-blocks.ts`, `markdown-table-compat.ts` | `MarkdownText`, `MarkdownWorkbookBuilder` | Text/table parsing with escaped pipe, line break, inline code, autolink, task-list, link/image fallback text, single-tilde strike, and HTML entity handling |
| `src/ts/xlsx-writer.ts` | `XlsxPackageBuilder`, `jp.igapyon.mikumsofficecore` | Basic workbook package ported; ZIP/OPC/XML package helpers follow upstream `miku-ms-office-core` 0.6.0 |
| `src/ts/xlsx-worksheet.ts` | `XlsxPackageBuilder#worksheetXml` | Dimensions, column hints, inline strings, hyperlinks, merges, and drawing relationships ported |
| `src/ts/xlsx-xml.ts` | `XmlUtils` | XML text sanitization, supplementary Unicode preservation, escaping, inline text XML, and column naming ported |
| `src/ts/xlsx-template.ts` | `XlsxTemplate`, `XlsxPackageBuilder` | Template styles, theme, worksheet settings, per-cell styles, and rightmost-sheet reuse ported |
| `src/ts/xlsx2md-dialect-parser.ts`, `xlsx2md-dialect.ts` | `Xlsx2mdDialect` | Early-access sheet marker, table anchor, validation, and overlay behavior ported |
| `src/ts/xlsx-styles.ts` | `XlsxPackageBuilder#stylesXml`, `XlsxPackageBuilder#styleIndex` | Style indexes and style sheet shape ported |
| `src/ts/xlsx-drawing*.ts`, `xlsx-media.ts`, `xlsx-sheet-drawings.ts`, `xlsx-image-preview.ts`, `image-size.ts` | `ImageRefModel`, `ImageAsset`, `XlsxPackageBuilder` | Media parts, drawing relationships, 3-column anchors, image-size-derived preview rows, and reserved blank preview rows ported |
| `src/ts/xlsx-hyperlinks.ts`, `markdown-links.ts` | `HyperlinkModel`, `MarkdownText`, `XlsxPackageBuilder` | External/internal hyperlink support and worksheet relationship IDs ported |
| `src/ts/xlsx-merge.ts` | `XlsxPackageBuilder` | Merge marker range calculation and covered-cell output ported |
| `src/ts/zip-io.ts`, `src/vendor/miku-ms-office-core-0.6.0.mjs` | `jp.igapyon.mikumsofficecore.ZipPackage` | Deterministic ZIP entry order and timestamps now come from shared Office core |
| `src/ts/markdown-rich-text.ts`, `xlsx-rich-text.ts` | `RichTextRun`, `MarkdownText`, `XlsxPackageBuilder` | Inline rich text parsing and XLSX run output ported |
| `src/ts/column-hints.ts` | `ColumnHints`, `SheetModel`, `XlsxPackageBuilder` | Initial column hint support ported |
| `scripts/lib/cli-support.mjs` | `jp.igapyon.mikumd2xlsx.cli` | Initial option contract ported |
