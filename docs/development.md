# Development

## Checked References

- miku-soft developer skill: local installed skill checked on 2026-07-18 at
  commit `9b05e3b1214e657a378781f28f5de6261f5d3e16`.
- Main workflow: `references/30-java-straight-conversion-workflow.md`.
- Upstream Node repository: `https://github.com/igapyon/miku-md2xlsx`.
- Same-layer sister reference: `https://github.com/igapyon/miku-md2docx-java`.

## Sister Reference Decisions

`miku-md2docx-java` was used as the same-layer Java companion reference. The
following repository-shape decisions were adopted:

- single-module Maven runtime
- `jp.igapyon.miku...` base package
- thin CLI class with `run(String[], PrintStream, PrintStream)`
- root `pom.xml` with Java 1.8, JUnit Jupiter, source jar, shaded jar, and local dist zip
- `docs/` mapping documents and `TODO.md`
- `workplace/.gitkeep` as the only tracked `workplace/` file

The DOCX-specific package builder and image summary API were not copied because
this repository's upstream target is an XLSX generator with different Open XML
parts.

## Commands

```sh
mvn test
mvn package
java -jar target/miku-md2xlsx-java-0.9.0.jar README.md --out README.xlsx
```

## Release Assets

GitHub Release assets are produced by
`.github/workflows/release-cli-runtime.yml`.

The workflow runs for `v*` tags or manual `workflow_dispatch`, builds with
Maven, checks that the tag version matches `pom.xml` version or a dot-suffixed
variant, and uploads:

- `miku-md2xlsx-java-<version>.jar`
- `miku-md2xlsx-java-sources-<version>.jar`

The executable jar is verified with Java 8 using `--version` before upload.
The Maven package still builds the local dist zip, but the dist zip is not
attached to GitHub Releases.
