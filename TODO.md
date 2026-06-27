# TODO

## Initial Java Straight Conversion

- No open initial straight-conversion TODO items in this file.

## Full Straight Conversion Roadmap

### 1. CLI Parity

- No open CLI parity items in this section.

### 2. Markdown Parsing Parity

- No open bounded Markdown parsing parity items in this section.
- Future optional work: replace the lightweight scanner with a full Markdown
  AST parser if exact `remark-parse` / `remark-gfm` compatibility becomes a
  hard requirement.

### 3. Workbook Model Parity

- No open bounded workbook model parity items in this section.
- Future optional work: add native Excel formula, chart, and shape
  reconstruction if Java runtime scope expands beyond preserving xlsx2md
  metadata as workbook text.

### 4. XLSX Package Parity

- No open XLSX package parity items in this section.

### 5. Upstream Fixture Parity

- No open upstream fixture parity items in this section.

### 6. Semantic Roundtrip And Diff Checks

- No open semantic roundtrip and diff check items in this section.

### 7. Documentation And Release Readiness

- Keep `README.md`, `docs/upstream-cli-mapping.md`,
  `docs/upstream-class-mapping.md`, and `docs/upstream-test-mapping.md` in sync
  with each completed parity slice.
- Run `mvn test`, `mvn package`, `java -jar ... --help`, `java -jar ...
  --version`, and representative fixture conversions before declaring full
  straight conversion complete.

## Completed Initial Slices

- Added Java parity tests from small upstream `tests/fixtures/from-xlsx2md`
  table, hyperlink, and merge samples.
- Strengthened Markdown table and cell handling for escaped pipes, empty cells,
  `<br>` line breaks, and xlsx2md-derived cell text.
- Ported upstream hyperlink handling for external links and internal
  `Sheet!A1`-style locations.
- Ported upstream merge marker handling for `[←M←]` and `[↑M↑]`, including
  `mergeCells` worksheet output.
- Improved XLSX writer parity for worksheet hyperlink relationships and merge
  ranges.
- Ported upstream image asset collection and drawing package generation for
  local PNG/JPEG/GIF-style image references.
- Ported initial rich text and inline style handling for bold, italic, strike,
  underline, and `<br>` line breaks.
- Ported upstream column hints and initial xlsx2md fixture compatibility
  behavior.
- Added parity tests against selected `miku-md2xlsx` fixtures.
- Recorded Java-side Maven plugin support as out of initial scope; revisit as a
  separated `miku-md2xlsx-java-maven` repository only after runtime parity
  stabilizes.

## Completed Full Conversion Slices

- Compared Java `--help` output against upstream `scripts/lib/cli-support.mjs`
  usage text and kept the content equivalent except for Java command examples.
- Added CLI tests for missing `--out`, missing input, unknown option, invalid
  `--sheet-mode`, invalid `--sheet-heading-depth`, invalid `--table-style`,
  missing option value, unexpected positional arguments, zero-argument help
  behavior, and runtime I/O errors.
- Confirmed exit code behavior for success, validation errors, and runtime I/O
  errors.
- Aligned `--version` with upstream `package.json` version `0.5.0`.
- Ported an initial block-level behavior slice from `markdown-blocks.ts`,
  including h1 title rows, heading-mode split at depth 1, blank row insertion
  before headings, list markers and nested list depth, and blockquote rows.
- Ported an initial inline extraction slice for nested inline styles, escaped
  Markdown characters, hard breaks, HTML `<br>`, underline tags, mixed link
  text, and inline image refs / alt text.
- Aligned mixed inline link extraction with upstream `markdown-text.ts` /
  `markdown-rich-text.ts`: non-cell hyperlinks remain visible as Markdown text
  such as `[label](url)`, while single-link cells still become Excel
  hyperlinks.
- Aligned inline code extraction with upstream `remark-parse` behavior so
  Markdown markers inside backticks remain literal text instead of becoming
  rich text styles.
- Expanded inline code extraction for multi-backtick code spans and
  remark-style code span whitespace normalization.
- Aligned table compatibility repair for double escaped pipe cells such as
  `a \\| b`, matching upstream `markdown-table-compat.ts` behavior.
- Aligned HTML entity text decoding with upstream `remark-parse` text-node
  behavior while preserving encoded tags such as `&lt;ins&gt;` as literal text.
- Hardened the lightweight Markdown scanner for representative `remark-gfm`
  single-tilde strikethrough behavior.
- Aligned image text extraction for empty image URLs so `![alt]()` contributes
  `alt` to workbook text like upstream `markdown-text.ts`.
- Aligned link text extraction for empty link URLs so `[label]()` contributes
  `label` to workbook text like upstream `markdown-text.ts`.
- Aligned link text extraction for `[url](url)` so mixed workbook text uses
  the URL label without Markdown link syntax like upstream `markdown-text.ts`.
- Aligned inline markup extraction inside link labels, including mixed text
  links, with upstream recursive `extractText` behavior.
- Hardened the lightweight Markdown scanner for `remark-parse` standard setext
  headings and tilde fenced code blocks.
- Hardened the lightweight Markdown scanner for `remark-parse` ATX heading
  closing sequences such as `### Heading ###`.
- Hardened the lightweight Markdown scanner for `remark-parse` indented code
  blocks and variable-length fenced code blocks.
- Hardened the lightweight Markdown scanner for spaced thematic breaks and
  ordered list markers that use `)`.
- Hardened the lightweight Markdown scanner for representative `remark-gfm`
  autolink cells, including angle-bracket URLs, bare scheme URLs, `www.`
  literals, and email addresses.
- Hardened the lightweight Markdown scanner for representative `remark-gfm`
  task list items by removing checkbox markers from list item text.
- Hardened the lightweight Markdown scanner for `remark-parse` blockquote lazy
  continuation lines.
- Hardened the lightweight Markdown scanner for `remark-parse` list lazy
  continuation lines.
- Hardened the lightweight Markdown scanner for `remark-parse` link definition
  blocks so definitions do not become workbook rows.
- Aligned reference link text extraction for `[label][id]` and `[label][]`
  with upstream recursive `extractText` behavior.
- Aligned shortcut reference link text extraction when a matching link
  definition exists, while leaving unknown bracketed text literal.
- Hardened representative `remark-parse` HTML block handling so block HTML is
  preserved as raw text rather than inline rich text.
- Aligned initial `sheet-builder.ts` behavior for `single` and `heading`
  modes, duplicate sheet names, illegal sheet name characters, title handling,
  preface rows, and `--sheet-heading-depth 1/2`.
- Ported additional `types.ts` / `workbook-model.ts` parity for
  caller-supplied `imageAssets`, `tableStyle: plain` body-cell roles, and
  xlsx2md-style internal hyperlink targets such as `Other!A1` resolving to
  `'Sheet Other'!A1` after heading-mode sheet splitting.
- Confirmed Java model/API coverage for upstream `types.ts` and `core.ts`,
  including image assets, image refs, rich text runs, hyperlinks, column hints,
  row kinds, cell style roles, and `markdownToXlsxModel` /
  `workbookModelToXlsx` / `md2xlsx`.
- Ported `xlsx-xml.ts` XML text sanitization, escaping, inline text
  `xml:space` behavior, and column naming coverage beyond `Z`.
- Ported `xlsx-styles.ts` style indexes and style sheet shape for normal,
  heading1-6, table header, table cell, code, and separator styles.
- Covered `xlsx-rich-text.ts` output behavior for rich text run boundaries,
  per-run inline text XML, and fallback to plain inline text when run text does
  not match the cell value.
- Covered `xlsx-hyperlinks.ts` relationship behavior for internal links,
  external links, and external hyperlink `rId` offsets when a worksheet also
  has drawing relationships.
- Covered `xlsx-merge.ts` merge range calculation for horizontal, vertical,
  and 2x2 merge marker patterns, including hiding merge marker cell text in
  worksheet output.
- Ported `zip-io.ts` deterministic ZIP behavior for stable entry timestamps,
  stable entry order, stable repeated output bytes, and deterministic image
  content type default ordering.
- Completed `xlsx-worksheet.ts` coverage for dimensions, column hints, empty
  column hints, inline string cells, hyperlinks, merges, and drawing
  relationships.
- Ported `xlsx-drawing*.ts`, `xlsx-media.ts`, `xlsx-sheet-drawings.ts`,
  `xlsx-image-preview.ts`, and `image-size.ts` coverage for media parts,
  drawing relationships, 3-column anchors, image-size-derived preview rows,
  and reserved blank preview rows.
- Added a Java-side semantic XLSX inspection helper comparable to upstream
  `tests/helpers/xlsx.js`, with focused regression coverage for workbook XML,
  worksheet values, relationships, merge refs, hyperlinks, drawing anchors,
  media entries, and content type defaults.
- Added a practical Java equivalent of upstream `semantic-roundtrip.mjs`
  coverage by converting representative upstream fixtures to XLSX packages and
  asserting semantic tokens from generated workbook XML values.
- Added a GitHub Release asset workflow for `v*` tags/manual dispatch,
  producing executable jar and sources jar assets with Java 8 runtime
  verification. The local dist zip is built by Maven but is not uploaded to
  GitHub Releases.
- Confirmed Maven plugin support remains deferred to a possible separate
  repository after runtime parity work is complete.
- Ran the release-readiness command set for the current slice: `mvn test`,
  `mvn package`, executable jar `--help`, executable jar `--version`, and a
  representative fixture conversion.
- Added tracked Java fixture resources and tests for the upstream smoke fixture
  group: `xlsx2md-basic-sample01`, `table-basic-sample01`,
  `table-basic-sample02`, `hyperlink-basic-sample01`,
  `merge-pattern-sample01`, and `image-basic-sample01` with required image
  assets.
- Added tracked Java fixture resources and representative tests for rich text
  and escaping fixtures: `rich-text-github-sample01`,
  `rich-markdown-escape-sample01`, and `rich-usecase-sample01`.
- Added tracked Java fixture resources and representative tests for narrative,
  display format, formula, cross-sheet formula, named range, chart, edge, and
  dense table fixtures.
- Added tracked Java fixture resources and representative tests for
  `image-basic-sample02`, `shape-basic-sample01`,
  `shape-block-arrow-sample01`, `shape-callout-sample01`, and
  `shape-flowchart-sample01`, including required image/SVG assets.
- Added tracked Java fixture resources and representative tests for
  `grid-layout-sample-01`, `formula-shared-sample01`,
  `formula-spill-sample01`, and `chart-mixed-sample01`, recording unsupported
  formula/chart reconstruction semantics explicitly.
- Added package-level regression coverage that unsupported formula, chart, and
  shape metadata remains text-only and does not create native Excel formula,
  chart, or shape drawing package parts.
- Refactored core responsibilities without behavior changes: extracted
  Markdown row parsing, heading-mode sheet splitting, internal hyperlink
  normalization, XLSX image size reading, and reusable Markdown regex patterns
  from the larger builder/helper classes.
- Followed upstream Node `miku-md2xlsx` `0.6.5` package-layer changes by
  vendoring `miku-ms-office-core-java` `0.5.1` for shared ZIP/OPC/XML helpers
  and updating deterministic ZIP entry-order coverage to the shared core order.
