# Upstream Follow-up Log

## 2026-05-18

- Initialized Java straight-conversion repository from empty local target.
- Checked upstream `miku-md2xlsx` commit
  `4d4fbd12595cd543ec7140eda0d9eca7d4cb2bbe`.
- Checked same-layer sister `miku-md2docx-java` commit
  `730fc62cbe1cdade82f5af900f21211e96b8f7c6`.
- Established Maven/CLI/runtime skeleton and initial XLSX package builder.

Open parity follow-ups are tracked in `TODO.md` and
`docs/remaining-migration-items.md`.

## 2026-05-18 Follow-up

- Added small upstream fixture resources for table, hyperlink, and merge
  coverage.
- Added Java tests for fixture conversion, escaped table pipes, hyperlink
  relationships, and merge ranges.
- Ported initial external/internal hyperlink output and merge marker handling.
- Ported initial local image asset collection, media parts, drawing parts, and
  worksheet drawing relationships.
- Ported initial rich text run output for common Markdown inline styles.
- Ported column hints and recorded Maven plugin support as out of initial scope.

## 2026-05-18 CLI Parity

- Aligned Java runtime version with upstream `package.json` version `0.5.0`.
- Expanded Java `--help` to include upstream-equivalent usage notes.
- Added CLI tests for help, version, validation errors, and runtime I/O errors.

## 2026-05-18 Markdown Block Parity

- Ported initial block-level behavior for h1 title rows, heading-mode split at
  depth 1, blank rows before headings, list marker preservation, nested list
  indentation, and blockquote rows.
- Ported initial inline extraction behavior for nested inline styles, escaped
  Markdown characters, hard breaks, underline tags, mixed link text, and inline
  image refs / alt text.
- Aligned mixed inline link extraction with upstream behavior: only single-link
  cells become Excel hyperlinks, while links embedded in surrounding text stay
  visible as Markdown text such as `[label](url)`.
- Aligned inline code extraction with upstream behavior so Markdown markers
  inside backticks remain literal text instead of becoming rich text styles.
- Expanded inline code extraction for multi-backtick code spans and
  remark-style code span whitespace normalization.
- Aligned table compatibility repair for double escaped pipe cells such as
  `a \\| b`, matching upstream `markdown-table-compat.ts` behavior.
- Hardened the scanner for setext headings and tilde fenced code blocks.
- Hardened the scanner for ATX heading closing sequences such as
  `### Heading ###`.
- Hardened the scanner for indented code blocks and variable-length fenced code
  blocks, including four-backtick fences containing triple-backtick text.
- Hardened the scanner for spaced thematic breaks such as `- - -` and ordered
  list markers that use `)`.
- Hardened the scanner for representative `remark-gfm` autolink cells:
  angle-bracket URLs, bare scheme URLs, `www.` literals, and email addresses
  now become external hyperlinks when the whole cell is the autolink.
- Hardened the scanner for representative `remark-gfm` task list items so
  checkbox markers are removed from list item text before writing workbook
  cells.

## 2026-05-18 Workbook Model Parity

- Aligned initial sheet-builder behavior for preface rows in heading mode,
  title fallback, duplicate sheet names, sanitized sheet names, and heading
  depth 1/2 split behavior.
- Added caller-supplied `imageAssets` support to `Md2XlsxOptions`, matching the
  upstream `types.ts` option shape while keeping Java CLI file-based loading as
  an adapter concern.
- Aligned `tableStyle: plain` so Markdown table body cells use the `normal`
  style role while header cells still use `tableHeader` when header rows are
  enabled.
- Added xlsx2md-style internal hyperlink normalization after heading-mode sheet
  splitting, so targets like `Other!A1` resolve to `'Sheet Other'!A1` when the
  actual worksheet name has the `Sheet ` prefix.
- Confirmed Java model/API coverage for upstream `types.ts` and `core.ts`,
  including image assets, image refs, rich text runs, hyperlinks, sheet column
  hints, row kinds, cell style roles, and the three core API methods.

## 2026-05-18 XLSX XML Parity

- Ported upstream `xlsx-xml.ts` text sanitization so XML-invalid control
  characters are removed before worksheet strings are written.
- Aligned inline text output so `xml:space="preserve"` is emitted only for
  empty, leading/trailing whitespace, or newline/tab text, matching upstream
  `inlineTextXml`.
- Added Java regression coverage for XML escaping and column references beyond
  `Z`, including `AA` and `AB`.

## 2026-05-18 XLSX Style Parity

- Ported upstream `xlsx-styles.ts` style index mapping for `heading1-6`,
  `title`, `heading`, `tableHeader`, `tableCell`, `code`, `separator`, and
  normal cells.
- Replaced the simplified Java style sheet with the upstream 9-font,
  4-fill, 3-border, and 11-cell-format style sheet shape.
- Added Java regression coverage for worksheet style IDs and styles XML
  structure.

## 2026-05-18 XLSX Rich Text Parity

- Added Java regression coverage for upstream `xlsx-rich-text.ts` behavior:
  rich text runs are emitted only when run text concatenation matches the cell
  value.
- Confirmed per-run inline text XML is used, including `xml:space` preservation
  for whitespace-only runs.

## 2026-05-18 XLSX Hyperlink Parity

- Added Java regression coverage for upstream `xlsx-hyperlinks.ts` relationship
  ID behavior when worksheet drawings occupy `rId1` and external hyperlinks
  start at `rId2`.
- Confirmed existing internal hyperlink and external hyperlink fixture coverage
  remains compatible with the heading-mode sheet name normalization.

## 2026-05-18 XLSX Merge Parity

- Added direct Java regression coverage for upstream `xlsx-merge.ts` horizontal,
  vertical, and 2x2 merge range calculation.
- Confirmed merge marker cells are suppressed from worksheet visible text while
  `<mergeCell>` refs are emitted.

## 2026-05-18 ZIP Parity

- Aligned Java ZIP entries with upstream `zip-io.ts` deterministic behavior by
  fixing ZIP entry timestamps and keeping package entry insertion order stable.
- Made image content type defaults deterministic by sorting collected media
  extensions before writing `[Content_Types].xml`.
- Added Java regression coverage that repeated workbook generation produces
  identical bytes and the expected entry order.

## 2026-05-18 XLSX Worksheet Parity

- Aligned empty column hints with upstream `xlsx-worksheet.ts`: Java now emits
  an empty `<cols></cols>` instead of inventing a default column width when the
  model has no column hints.
- Recorded existing worksheet coverage for dimensions, inline strings,
  hyperlinks, merges, drawing relationships, and column hints.

## 2026-05-18 XLSX Drawing And Media Parity

- Ported image-size-derived preview row calculation for PNG/GIF/JPEG assets,
  including upstream min/max clamping behavior.
- Aligned drawing anchors with upstream 3-column preview width and reserved
  blank preview rows in worksheet output.
- Kept media extension/content type behavior and drawing relationships covered
  by existing image embedding tests.

## 2026-05-18 Upstream Fixture Parity

- Added tracked Java fixture resources for
  `xlsx2md-basic-sample01.md`, `table-basic-sample02.md`,
  `image-basic-sample01.md`, and the required `assets/image` PNG files.
- Added Java tests for the upstream smoke fixture group covering representative
  workbook text, adjacent table content, and image fixture media/drawing output.
- Added tracked Java fixture resources for
  `rich-text-github-sample01.md`, `rich-markdown-escape-sample01.md`, and
  `rich-usecase-sample01.md`.
- Added Java tests for rich text runs, underline/hyperlink use cases, and
  representative Markdown escaping fixture values.
- Aligned `rich-markdown-escape-sample01` HTML entity text handling with
  upstream-style decoded text values while preserving encoded tags as literal
  text.
- Added representative `remark-gfm` single-tilde strikethrough coverage for
  `~strike~` while leaving whitespace-separated `~ spaced` as literal text.
- Hardened blockquote parsing for `remark-parse` lazy continuation lines such
  as `> First` followed by `continued`.
- Hardened list parsing for `remark-parse` lazy continuation lines while
  preserving nested list rows as separate indented rows.
- Hardened scanner handling for `remark-parse` link definition blocks so
  `[id]: https://...` definitions are skipped rather than written as paragraph
  rows.
- Aligned reference link text extraction for `[label][id]` and `[label][]` so
  link reference labels contribute text without creating Excel hyperlinks.
- Aligned shortcut reference links such as `[label]` when a matching link
  definition exists, while leaving unknown bracketed text literal.
- Hardened representative HTML block parsing so block tags such as `<div>` are
  preserved as raw text and do not trigger inline rich-text handling.
- Aligned image text extraction for empty image URLs so `![alt]()` contributes
  `alt` to cell text, matching upstream `markdown-text.ts`.
- Aligned link text extraction for empty link URLs so `[label]()` contributes
  `label` without creating a hyperlink.
- Aligned link text extraction for `[url](url)` so mixed text gets the URL
  label without Markdown link syntax, matching upstream `markdown-text.ts`.
- Aligned inline markup extraction inside link labels so empty-URL links,
  whole-cell hyperlinks, and mixed text links use recursive
  `extractText`-style label text.
- Added tracked Java fixture resources and representative tests for
  `narrative-vs-table-sample01`, `display-format-sample01`,
  `formula-basic-sample01`, `formula-crosssheet-sample01`,
  `named-range-sample01`, `chart-basic-sample01`, `edge-empty-sample01`,
  `edge-weird-sheetname-sample01`, and `table-basic-sample13`.
- List-based chart and named-range metadata is currently preserved with Java's
  list marker text, such as `- Title: ...`; exact marker-free parity remains
  part of Markdown Parsing Parity.
- Added tracked Java fixture resources and representative tests for
  `image-basic-sample02`, `shape-basic-sample01`,
  `shape-block-arrow-sample01`, `shape-callout-sample01`, and
  `shape-flowchart-sample01`, including required image/SVG assets.
- Shape SVG assets are kept as fixture resources, but Java does not reconstruct
  Excel shape drawings from xlsx2md shape metadata; this remains accepted
  unsupported semantics unless a future scope adds SVG-to-drawing conversion.
- Added tracked Java fixture resources and representative tests for
  `grid-layout-sample-01`, `formula-shared-sample01`,
  `formula-spill-sample01`, and `chart-mixed-sample01`.
- Java preserves xlsx2md text for shared/spill formula and mixed chart fixtures
  but does not reconstruct native Excel shared formulas, spill formulas, or
  native chart objects. Treat this as accepted unsupported semantics unless a
  later scope explicitly adds formula/chart reconstruction.
- Added package-level regression coverage for the accepted unsupported
  semantics: formula/chart/shape metadata remains worksheet text and does not
  create native formula, chart, or shape drawing package parts.

## 2026-05-18 Semantic XLSX Inspection

- Added Java `XlsxTestSupport`, mirroring the role of upstream
  `tests/helpers/xlsx.js` for semantic inspection of generated XLSX packages.
- Added regression coverage that reads workbook sheet names, worksheet values,
  merge refs, worksheet hyperlinks, worksheet relationships, drawing anchors,
  drawing relationships, media entries, and content type defaults from a
  generated workbook.
- Added a practical Java equivalent of upstream `semantic-roundtrip.mjs`
  coverage by converting representative upstream fixtures to generated XLSX
  packages and asserting semantic tokens from workbook XML values.
- This Java smoke does not invoke sibling `miku-xlsx2md`; it validates the
  generated package semantics directly with `XlsxTestSupport`.

## 2026-05-18 Release Readiness

- Added `.github/workflows/release-cli-runtime.yml` for `v*` tags and manual
  dispatch.
- The workflow builds with Maven, checks tag-to-`pom.xml` version compatibility,
  prepares executable jar and sources jar release assets, verifies the
  executable jar with Java 8 using `--version`, and uploads those jar assets to
  GitHub Release. The local dist zip remains a Maven package output and is not
  attached to GitHub Releases.
- Confirmed Maven plugin support remains deferred to a possible separate
  repository after runtime parity work is complete.
- Verified the current release-readiness slice with `mvn package`, jar
  `--help`, jar `--version` returning `0.5.0`, and conversion of
  `xlsx2md-basic-sample01.md` to `target/verification-basic.xlsx`.

## 2026-06-28 Upstream 0.6.5 Package-Core Follow-Up

- Fetched upstream Node `miku-md2xlsx` and confirmed `origin/devel` advanced
  from `v0.5.0` to `v0.6.5`.
- Upstream package writing now uses vendored `miku-ms-office-core` `0.5.1`
  instead of the previous local `src/ts/zip-io.ts` writer.
- Updated Java runtime version to `0.6.5` and vendored the
  `miku-ms-office-core-java` `0.5.1` release jar under
  `vendor/miku-ms-office-core-java/`, following the `miku-xlsx2md-java`
  same-layer pattern.
- Replaced local ZIP writing and OPC relationship/content-type XML assembly in
  `XlsxPackageBuilder` with shared Office core helpers while keeping XLSX
  worksheet, drawing, style, merge, hyperlink, and rich-text semantics local.
- Updated deterministic ZIP entry-order coverage to the shared Office core's
  stable path ordering.
- Maven unpacks the vendored Office core jar during `generate-sources`, so
  release builds do not depend on Maven repository publication for the shared
  core.

## 2026-07-18 Upstream 0.7.0-0.9.0 Follow-Up

- Fetched upstream Node `miku-md2xlsx` and compared `v0.6.5..v0.9.0` at
  upstream commit `2d387ce`.
- `v0.7.0` added template-assisted XLSX generation and CLI `--template`.
  Java now carries `templateXlsx` through `Md2XlsxOptions` and `WorkbookModel`,
  and `XlsxTemplate` reuses styles, theme, worksheet settings, template cell
  styles, and the rightmost template sheet for additional generated sheets.
- `v0.8.0` preserved supplementary Unicode characters and retained template
  worksheet namespaces. Java now sanitizes XML by Unicode code point at the
  local XML boundary because the vendored Office core 0.5.1 Java helper drops
  surrogate pairs.
- `v0.9.0` added the early-access `miku-xlsx2md` input dialect. Java now ports
  `--input-dialect`, exact `Sheet:` names, `Table: N (A1-C4)` anchored table
  overlays, malformed-marker diagnostics, and exclusion with generic sheet
  split options.
- Updated the Java runtime version to `0.9.0`, CLI help, README, upstream class
  and test mappings, snapshot record, and focused regression coverage.
- Verification: `mvn test` and `mvn package` pass 84 tests; the packaged jar
  reports `0.9.0`, renders `--help`, converts the basic xlsx2md fixture with
  `--input-dialect miku-xlsx2md`, and converts README with `--template`.

## 2026-07-18 Upstream 0.9.5 Office Core Follow-Up

- Fetched upstream Node `miku-md2xlsx` and compared `v0.9.0..v0.9.5` at
  upstream commit `2b2e79d`.
- Confirmed Node `miku-md2xlsx` now vendors `miku-ms-office-core` `0.6.0` and
  retains its product-local `xlsx-xml.ts` helper for XLSX-specific XML text,
  rich-text, and column-name behavior.
- Updated the Java runtime version to `0.9.5` and replaced the vendored
  `miku-ms-office-core-java` `0.5.1` jar with the published `0.6.0` release
  jar, SHA-256
  `d25392727d9449e5001b9024b888f0ce09962c9fb977c18613731f37027b0a77`.
- Retained product-local `XmlUtils` to preserve direct correspondence with
  upstream `xlsx-xml.ts`; shared core continues to own ZIP/OPC plumbing and
  generic XML helpers.
- Expanded the supplementary Unicode regression to cover the XML 1.0 valid
  boundaries and rejection of isolated surrogates, `U+FFFE`, and `U+FFFF`.
- Verification: `mvn clean package` passes all 84 tests and creates the
  `0.9.5` runtime jar, sources jar, and distribution zip. The packaged jar
  reports `0.9.5`, renders `--help`, converts the representative xlsx2md
  fixture, and contains the `0.6.0` code-point-aware `XmlHelper` implementation.
