# Remaining Migration Items

## Completed

- Repository conventions: `.gitignore`, `.mvn/jvm.config`, `workplace/.gitkeep`.
- Maven single-module Java runtime skeleton.
- CLI entrypoint with upstream option names.
- Core API names corresponding to upstream `core.ts`.
- Basic Markdown block conversion to workbook model.
- Basic XLSX zip package generation.
- Focused JUnit tests for core and CLI smoke behavior.
- Small upstream fixture parity tests for table, hyperlink, and merge samples.
- Escaped pipe and `<br>` handling in Markdown table cells.
- External and internal hyperlink worksheet output.
- Merge marker handling for `[←M←]` and `[↑M↑]` with `mergeCells` output.
- Local image asset collection and initial drawing/media package generation.
- Initial rich text runs for bold, italic, strike, underline, and line breaks.
- Column hints for worksheet `<cols>` output.
- Selected upstream fixture parity tests.
- Maven plugin support kept out of initial scope.
- CLI parity checks for generated help text, validation errors, exit codes, and
  version output.
- Initial block-level Markdown parity for title rows, heading splits, blank
  rows before headings, nested list rows, and blockquotes.
- Initial inline Markdown parity for nested styles, escaped characters, hard
  breaks, underline tags, mixed link text, and inline image refs.
- Mixed inline link extraction aligned with upstream behavior: links embedded
  in surrounding text stay visible as Markdown text, while single-link cells
  remain Excel hyperlinks.
- Inline code extraction aligned so Markdown markers inside backticks remain
  literal text instead of becoming rich text styles.
- Inline code extraction expanded for multi-backtick code spans and
  remark-style code span whitespace normalization.
- Table compatibility repair for double escaped pipe cells such as `a \\| b`,
  matching upstream `markdown-table-compat.ts` behavior.
- Scanner hardening for setext headings and tilde fenced code blocks.
- Scanner hardening for `remark-parse` ATX heading closing sequences such as
  `### Heading ###`.
- Scanner hardening for `remark-parse` indented code blocks and
  variable-length fenced code blocks.
- Scanner hardening for spaced thematic breaks and ordered list markers that
  use `)`.
- Scanner hardening for `remark-parse` list lazy continuation lines.
- Scanner hardening for `remark-parse` link definition blocks so definitions
  do not become workbook rows.
- Scanner hardening for representative `remark-parse` HTML blocks so block
  HTML remains raw text rather than inline rich text.
- Scanner hardening for representative `remark-gfm` autolink cells, including
  angle-bracket URLs, bare scheme URLs, `www.` literals, and email addresses.
- Scanner hardening for representative `remark-gfm` task list items by
  removing checkbox markers from list item text.
- Scanner hardening for `remark-parse` blockquote lazy continuation lines.
- HTML entity text decoding for representative `remark-parse` text nodes while
  keeping encoded tags such as `&lt;ins&gt;` as literal text.
- Representative `remark-gfm` single-tilde strikethrough handling.
- Image text extraction for empty image URLs, matching upstream
  `markdown-text.ts` fallback to alt text.
- Link text extraction for empty link URLs, matching upstream
  `markdown-text.ts` fallback to label text.
- Link text extraction when label equals URL, matching upstream
  `markdown-text.ts` output without Markdown link syntax.
- Reference link label extraction, matching upstream recursive `extractText`
  behavior for reference link children.
- Shortcut reference link label extraction when a matching definition exists.
- Inline markup extraction inside link labels, including mixed text links,
  matching upstream recursive `extractText` behavior for link children.
- Initial sheet-builder parity for single/heading modes, title handling,
  preface rows, duplicate names, and sanitized sheet names.
- Additional workbook model parity for caller-supplied image assets, plain
  table style body cells, and xlsx2md-style internal hyperlink target
  normalization after heading-mode sheet splitting.
- Java model/API coverage for upstream `types.ts` and `core.ts`, including
  image assets, image refs, rich text runs, hyperlinks, column hints, row
  kinds, cell style roles, and the three core API methods.
- XML text sanitization, escaping, inline text `xml:space` behavior, and
  column naming behavior from upstream `xlsx-xml.ts`.
- Upstream `xlsx-styles.ts` style indexes and style sheet shape for normal,
  heading1-6, table header, table cell, code, and separator styles.
- Upstream `xlsx-rich-text.ts` run-boundary output, per-run inline text XML,
  and fallback behavior when rich text runs do not match cell values.
- Upstream `xlsx-hyperlinks.ts` relationship behavior for internal links,
  external links, and external hyperlink relationship IDs when drawings are
  present.
- Upstream `xlsx-merge.ts` merge range calculation for horizontal, vertical,
  and 2x2 merge marker patterns.
- Deterministic ZIP output behavior corresponding to upstream
  `miku-ms-office-core` package helpers, including fixed entry timestamps,
  stable entry order, and deterministic image content type default ordering.
- Upstream `xlsx-worksheet.ts` worksheet output behavior for dimensions,
  column hints including empty hints, inline string cells, hyperlinks, merges,
  and drawing relationships.
- Upstream drawing/media behavior for media parts, drawing relationships,
  3-column anchors, image-size-derived preview rows, and reserved blank preview
  rows.
- Tracked Java fixture resources and tests for the upstream smoke fixture
  group, including `image-basic-sample01` image assets.
- Tracked Java fixture resources and representative tests for upstream rich
  text and escaping fixtures.
- Tracked Java fixture resources and representative tests for narrative,
  display format, formula, cross-sheet formula, named range, chart, edge, and
  dense table fixtures.
- Tracked Java fixture resources and representative tests for
  `image-basic-sample02` and shape fixture text/assets.
- Tracked Java fixture resources and representative tests for grid and
  unsupported formula/chart semantic fixtures.
- Package-level regression coverage that unsupported formula, chart, and shape
  metadata remains text-only and does not create native formula/chart/shape
  package parts.
- Java-side semantic XLSX inspection helper comparable to upstream
  `tests/helpers/xlsx.js`, with coverage for workbook XML, worksheet values,
  relationships, merge refs, hyperlinks, drawing anchors, media entries, and
  content type defaults.
- Practical Java equivalent of upstream `semantic-roundtrip.mjs` coverage for
  representative fixtures by asserting semantic tokens from generated workbook
  XML values.
- GitHub Release asset workflow for executable jar and sources jar assets with
  Java 8 runtime verification. The local dist zip remains a Maven package
  output and is not attached to GitHub Releases.
- Maven plugin support remains deferred to a possible separate repository after
  runtime parity work is complete.
- Current slice verified with `mvn test`, `mvn package`, jar `--help`, jar
  `--version`, and representative fixture conversion.

## Pending

- Optional full Markdown AST compatibility with upstream `remark-parse` and
  `remark-gfm` if exact parser parity becomes a hard requirement.
- Optional semantic parity for unsupported formula/chart/shape
  reconstruction if Java runtime scope is expanded beyond text-preserving
  xlsx2md metadata conversion.

The ordered roadmap is tracked in `TODO.md` under
`Full Straight Conversion Roadmap`.

## Latest Verification

Run:

```sh
mvn test
mvn package
java -jar target/miku-md2xlsx-java-0.6.5.jar --help
java -jar target/miku-md2xlsx-java-0.6.5.jar --version
java -jar target/miku-md2xlsx-java-0.6.5.jar src/test/resources/fixtures/from-xlsx2md/xlsx2md-basic-sample01.md --out target/verification-basic.xlsx --sheet-mode heading --sheet-heading-depth 2
```
