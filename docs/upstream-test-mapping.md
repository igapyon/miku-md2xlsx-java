# Upstream Test Mapping

| Upstream test intent | Java test | Status |
| --- | --- | --- |
| Core Markdown to workbook conversion | `MikuMd2xlsxCoreTest.convertsMarkdownToWorkbookModel` | Initial smoke coverage |
| Heading sheet split | `MikuMd2xlsxCoreTest.supportsHeadingSheetMode` | Initial smoke coverage |
| XLSX package generation | `MikuMd2xlsxCoreTest.writesXlsxZipEntries` | Initial zip-entry coverage |
| Small table fixture parity | `MikuMd2xlsxCoreTest.convertsSmallUpstreamTableFixture` | Initial fixture coverage |
| Upstream smoke fixture group | `MikuMd2xlsxCoreTest.convertsUpstreamSmokeFixtureGroup` | Covered |
| Image basic fixture with assets | `MikuMd2xlsxCoreTest.convertsUpstreamImageBasicFixtureWithAssets` | Covered |
| Image/chart coexistence fixture | `MikuMd2xlsxCoreTest.convertsUpstreamImageChartCoexistenceFixture` | Covered |
| Shape fixture text and assets | `MikuMd2xlsxCoreTest.convertsUpstreamShapeFixtureTextAndKeepsShapeAssets` | Representative coverage |
| Grid and unsupported semantic fixtures | `MikuMd2xlsxCoreTest.convertsUpstreamGridAndUnsupportedSemanticFixtures` | Representative coverage |
| Hyperlink fixture parity | `MikuMd2xlsxCoreTest.writesHyperlinkRelationshipsFromUpstreamFixture` | Initial fixture coverage |
| Hyperlink relationship IDs with drawings | `MikuMd2xlsxCoreTest.offsetsExternalHyperlinkRelationshipsWhenSheetHasDrawing` | Covered |
| Merge fixture parity | `MikuMd2xlsxCoreTest.writesMergeCellsFromUpstreamFixture` | Initial fixture coverage |
| Horizontal, vertical, and 2x2 merge range calculation | `MikuMd2xlsxCoreTest.writesHorizontalVerticalAndTwoByTwoMergeRangesLikeUpstream` | Covered |
| Escaped pipe and cell line break handling | `MikuMd2xlsxCoreTest.handlesEscapedPipesAndBreaksInTableCells` | Covered |
| Double escaped pipe table compatibility | `MikuMd2xlsxCoreTest.repairsDoubleEscapedPipeCellsLikeUpstreamTableCompat` | Covered |
| Local image embedding | `MikuMd2xlsxCoreTest.embedsLocalImageAssets` | Initial package part coverage |
| Image preview sizing and reserved rows | `MikuMd2xlsxCoreTest.embedsLocalImageAssets` | Covered |
| Semantic XLSX helper inspection for workbook XML, worksheet values, rels, merges, hyperlinks, drawings, media, and content types | `XlsxTestSupport`; `MikuMd2xlsxCoreTest.inspectsWorkbookSemanticsWithJavaXlsxHelper` | Covered |
| Practical semantic roundtrip smoke for representative fixture tokens | `MikuMd2xlsxCoreTest.keepsRepresentativeFixtureSemanticsInGeneratedXlsxPackage` | Covered |
| Caller-supplied `imageAssets` option | `MikuMd2xlsxCoreTest.acceptsImageAssetsOptionLikeUpstreamTypes` | Covered |
| Rich text inline style output | `MikuMd2xlsxCoreTest.writesRichTextRunsForInlineStyles` | Initial run coverage |
| Rich text run boundaries and fallback | `MikuMd2xlsxCoreTest.keepsRichTextRunBoundariesAndFallbackLikeUpstream` | Covered |
| Rich text fixture group | `MikuMd2xlsxCoreTest.convertsUpstreamRichTextFixtureGroup` | Covered |
| Markdown escaping fixture | `MikuMd2xlsxCoreTest.convertsUpstreamMarkdownEscapeFixture` | Representative coverage |
| Narrative, display format, and formula fixtures | `MikuMd2xlsxCoreTest.convertsUpstreamContentCompatibilityFixtures` | Covered |
| Cross-sheet formula, named range, and edge fixtures | `MikuMd2xlsxCoreTest.convertsUpstreamMultiSheetMetadataFixtures` | Representative coverage |
| Chart metadata and dense table fixtures | `MikuMd2xlsxCoreTest.convertsUpstreamChartAndDenseTableMetadataFixtures` | Representative coverage |
| Unsupported formula/chart/shape native reconstruction boundary | `MikuMd2xlsxCoreTest.keepsUnsupportedFormulaChartAndShapeMetadataAsTextOnly` | Covered |
| Column hint output | `MikuMd2xlsxCoreTest.computesColumnHintsForTextAndTableRows` | Covered |
| Empty column hints in worksheet output | `MikuMd2xlsxCoreTest.keepsWorksheetColumnsEmptyWhenColumnHintsAreEmptyLikeUpstream` | Covered |
| `tableStyle: plain` body-cell role | `MikuMd2xlsxCoreTest.appliesPlainTableStyleToBodyCells` | Covered |
| XML escaping and inline text `xml:space` behavior | `MikuMd2xlsxCoreTest.followsUpstreamXmlEscapingAndInlineTextRules` | Covered |
| Column names beyond `Z` | `MikuMd2xlsxCoreTest.writesColumnNamesBeyondZLikeUpstream` | Covered |
| Supplementary Unicode worksheet text | `MikuMd2xlsxCoreTest.preservesSupplementaryUnicodeCharactersInWorksheetText` | Covered |
| XLSX template styles, theme, namespaces, cell styles, and rightmost-sheet reuse | `MikuMd2xlsxCoreTest.reusesTemplateStylesThemeAndRightmostSheet` | Covered |
| miku-xlsx2md dialect sheet names and table anchors | `MikuMd2xlsxCoreTest.restoresXlsx2mdSheetNamesAndAnchoredTables` | Covered |
| malformed miku-xlsx2md structural markers | `MikuMd2xlsxCoreTest.rejectsMalformedXlsx2mdMarkers` | Covered |
| Style indexes and styles XML shape | `MikuMd2xlsxCoreTest.writesUpstreamStyleIndexesAndStyleSheetShape` | Covered |
| Deterministic ZIP bytes and entry order | `MikuMd2xlsxCoreTest.writesDeterministicZipBytesAndEntryOrder` | Covered |
| Heading split at depth 1 | `MikuMd2xlsxCoreTest.splitsHeadingModeAtDepthOneAndKeepsTitleRow` | Covered |
| Heading split at depth 2 with preface rows | `MikuMd2xlsxCoreTest.supportsHeadingSheetMode` | Covered |
| Sanitized and duplicate sheet names | `MikuMd2xlsxCoreTest.sanitizesAndDeduplicatesHeadingSheetNames` | Covered |
| Single mode title option | `MikuMd2xlsxCoreTest.usesTitleOptionForSingleSheetMode` | Covered |
| Heading mode title fallback without split | `MikuMd2xlsxCoreTest.usesTitleOptionWhenHeadingModeDoesNotSplit` | Covered |
| Blank row before heading | `MikuMd2xlsxCoreTest.insertsBlankBeforeHeadingAfterText` | Covered |
| List marker and nesting | `MikuMd2xlsxCoreTest.preservesListMarkersAndNestedDepth` | Covered |
| List lazy continuation | `MikuMd2xlsxCoreTest.parsesListLazyContinuationLikeRemarkParse` | Covered |
| Link definition blocks | `MikuMd2xlsxCoreTest.skipsLinkDefinitionsLikeRemarkParse` | Covered |
| GFM task list checkbox text | `MikuMd2xlsxCoreTest.stripsGfmTaskListCheckboxesLikeRemarkGfm` | Covered |
| Spaced thematic breaks | `MikuMd2xlsxCoreTest.parsesSpacedThematicBreaksLikeRemarkParse` | Covered |
| Blockquote row conversion | `MikuMd2xlsxCoreTest.convertsBlockquoteLinesToQuotedParagraph` | Covered |
| Blockquote lazy continuation | `MikuMd2xlsxCoreTest.parsesBlockquoteLazyContinuationLikeRemarkParse` | Covered |
| HTML block raw text | `MikuMd2xlsxCoreTest.keepsHtmlBlocksRawLikeRemarkParse` | Covered |
| Setext heading parsing | `MikuMd2xlsxCoreTest.parsesSetextHeadingsLikeRemarkParse` | Covered |
| ATX heading closing sequence parsing | `MikuMd2xlsxCoreTest.stripsAtxClosingHeadingSequenceLikeRemarkParse` | Covered |
| Tilde fenced code parsing | `MikuMd2xlsxCoreTest.parsesTildeFencedCodeLikeRemarkParse` | Covered |
| Variable-length fenced code parsing | `MikuMd2xlsxCoreTest.respectsFenceLengthLikeRemarkParse` | Covered |
| Indented code block parsing | `MikuMd2xlsxCoreTest.parsesIndentedCodeBlocksLikeRemarkParse` | Covered |
| Nested inline styles and escaped characters | `MikuMd2xlsxCoreTest.handlesNestedInlineStylesAndEscapes` | Covered |
| HTML entity text decoding | `MikuMd2xlsxCoreTest.decodesHtmlEntitiesLikeRemarkParseTextNodes` | Covered |
| GFM single-tilde strikethrough | `MikuMd2xlsxCoreTest.treatsSingleTildeAsGfmStrikethrough` | Covered |
| Inline code literal text | `MikuMd2xlsxCoreTest.keepsInlineCodeTextLiteralLikeRemarkParse` | Covered |
| Hard breaks and inline image refs | `MikuMd2xlsxCoreTest.handlesHardBreaksAndInlineImageRefs` | Covered |
| Image text extraction with empty URL | `MikuMd2xlsxCoreTest.usesAltTextForImagesWithoutUrlLikeUpstreamExtractText` | Covered |
| Link text extraction with empty URL | `MikuMd2xlsxCoreTest.usesLabelTextForLinksWithoutUrlLikeUpstreamExtractText` | Covered |
| Link text extraction when label equals URL | `MikuMd2xlsxCoreTest.usesLabelTextForLinksWhenLabelEqualsUrlLikeUpstreamExtractText` | Covered |
| Reference link label extraction | `MikuMd2xlsxCoreTest.extractsReferenceLinkLabelsLikeUpstreamExtractText` | Covered |
| Shortcut reference link label extraction | `MikuMd2xlsxCoreTest.extractsShortcutReferenceLinkLabelsWhenDefinitionExistsLikeRemarkParse` | Covered |
| Inline markup inside link labels | `MikuMd2xlsxCoreTest.extractsInlineMarkupInsideLinkLabelsLikeUpstreamExtractText` | Covered |
| Mixed link text without cell hyperlink | `MikuMd2xlsxCoreTest.keepsMixedLinkAsTextWithoutHyperlink` | Covered |
| GFM autolink cells | `MikuMd2xlsxCoreTest.treatsGfmAutolinkCellsAsExternalHyperlinks` | Covered |
| CLI `--version` | `MikuMd2xlsxCliTest.printsVersion` | Covered |
| CLI `--help` | `MikuMd2xlsxCliTest.printsHelp` | Covered |
| CLI zero-argument help | `MikuMd2xlsxCliTest.printsHelpWhenNoArguments` | Covered |
| CLI file conversion | `MikuMd2xlsxCliTest.convertsFile` | Covered |
| CLI validation errors | `MikuMd2xlsxCliTest` validation methods | Covered |
| CLI runtime I/O errors | `MikuMd2xlsxCliTest.reportsRuntimeIoErrors` | Covered |
| CLI input dialect validation and generic sheet-option exclusion | `MikuMd2xlsxCliTest.rejectsInvalidInputDialect`, `rejectsGenericSheetOptionsWithXlsx2mdDialect` | Covered |
| broader `tests/md2xlsx-from-xlsx2md-*.test.js` fixture parity | Not yet mapped | Pending |
| formula, image sizing, and full fixture parity | Not yet mapped | Pending |

Focused command:

```sh
mvn test
```
