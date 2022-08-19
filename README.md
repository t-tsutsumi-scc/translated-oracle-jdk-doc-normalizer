# Translated Oracle JDK Documentation Normalizer

Normalizer for using [translated Oracle JDK documentation archive](https://www.oracle.com/jp/java/technologies/documentation.html) in IntelliJ IDEA.

## Usage

```
Usage: <main class> [OPTIONS] <inputFile>
Normalize the translated Oracle JDK documentation archive for use in IntelliJ IDEA.
      <inputFile>   The input file i.e. ZIP archive of JDK document.
  -h, --help        Show this help message and exit.
  -o, --output=<outputFile>
                    The output file.
      --proportional-font=<proportionalFont>
                    Ex: Meiryo, "Hiragino Kaku Gothic ProN". Use the default browser font if empty.
      --monospaced-font=<monospacedFont>
                    Ex: "DejaVu Sans Mono", Consolas. Use the default browser font if empty.
```

### Gradle

```bash
./gradlew run --args="--proportional-font= jdk-17-docs-ja.zip"
```

### Docker

```bash
docker run --rm -v $PWD:/docs \
           ghcr.io/t-tsutsumi-scc/translated-oracle-jdk-doc-normalizer:main \
           -o /docs/17-mydoc.zip \
           --proportional-font='Meiryo, "Hiragino Kaku Gothic ProN"' \
           --monospaced-font='Consolas' \
           /docs/17.zip
```
