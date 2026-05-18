# Remaining Migration Items

## Completed

- Repository conventions: `.gitignore`, `.mvn/jvm.config`, `workplace/.gitkeep`.
- Maven single-module Java runtime skeleton.
- CLI entrypoint with upstream option names.
- Core API names corresponding to upstream `core.ts`.
- Basic Markdown block conversion to workbook model.
- Basic XLSX zip package generation.
- Focused JUnit tests for core and CLI smoke behavior.

## Pending

- Full Markdown AST compatibility with upstream `remark`/`remark-gfm` behavior.
- Image reference collection and Open XML drawing parts.
- Rich text runs and common inline style preservation.
- Hyperlink support.
- Merge marker support.
- Column hint support.
- Fixture parity against upstream `tests/fixtures/from-xlsx2md`.
- Release workflow setup after artifact naming and version policy stabilize.

## Latest Verification

Run:

```sh
mvn test
```

