package app;

import org.jsoup.Jsoup;
import picocli.CommandLine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@CommandLine.Command(description = "Normalize a translated Oracle JDK documentation archive for use in IntelliJ IDEA.",
    sortOptions = false, usageHelpWidth = 100, usageHelpAutoWidth = true, abbreviateSynopsis = true,
    mixinStandardHelpOptions = true)
public class TranslatedOracleJdkDocNormalizer implements Runnable {

    private static final Pattern monospacedFontPattern = Pattern.compile("font-family: *'DejaVu Sans Mono', *monospace;");
    private static final List<Pattern> proportionalFontPatterns = List.of(
        Pattern.compile("font-family: *'DejaVu Sans', *Arial, *Helvetica, *sans-serif;"),
        Pattern.compile("font-family: *'DejaVu Serif', *Georgia, *\"Times New Roman\", *Times, *serif;"));

    @CommandLine.Parameters(index = "0", description = "The input file i.e. ZIP archive of JDK documentation.")
    private File inputFile;

    @CommandLine.Option(names = {"-o", "--output"}, order = 1,
        description = "The output file.")
    private Optional<File> outputFile;

    @CommandLine.Option(names = "--proportional-font", order = 2,
        description = "Ex: Meiryo, \"Hiragino Kaku Gothic ProN\". Use the default browser font if empty.")
    private Optional<String> proportionalFont;

    @CommandLine.Option(names = "--monospaced-font", order = 3,
        description = "Ex: \"DejaVu Sans Mono\", Consolas. Use the default browser font if empty.")
    private Optional<String> monospacedFont;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new TranslatedOracleJdkDocNormalizer()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        var outputFile = this.outputFile.orElseGet(() ->
            new File(inputFile.getAbsolutePath().replaceFirst("\\.zip$", "-normalized.zip")));
        try (var zipInput = new ZipFile(inputFile, StandardCharsets.UTF_8);
             var zipOutput = new ZipOutputStream(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
            normalize(zipInput, zipOutput);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void normalize(ZipFile zipInput, ZipOutputStream zipOutput) throws IOException {
        var processed = 0;

        for (var it = zipInput.entries().asIterator(); it.hasNext(); ) {
            var ze = it.next();
            zipOutput.putNextEntry(ze);

            if (ze.getName().endsWith(".html")) {
                try (var input = zipInput.getInputStream(ze)) {
                    var document = Jsoup.parse(input, null, "");

                    // Remove SiteCatalyst script
                    document.select("script[src=https://www.oracleimg.com/us/assets/metrics/ora_docs.js]").remove();

                    // Unwrap original text span
                    for (var mergedSpan : document.select("span[class=merged]")) {
                        mergedSpan.unwrap();
                    }

                    // Move translation notice to footer
                    var machineTranslationNotice = document.selectFirst("header ~ div[style=width:100%]");
                    var footerElement = document.selectFirst("footer[role=contentinfo]");
                    if (machineTranslationNotice != null && footerElement != null) {
                        footerElement.prependChild(machineTranslationNotice);
                    }

                    document.outputSettings().prettyPrint(false);
                    zipOutput.write(document.toString().getBytes(document.charset()));
                }
            } else if (ze.getName().endsWith(".css") && (proportionalFont.isPresent() || monospacedFont.isPresent())) {
                try (var input = new InputStreamReader(zipInput.getInputStream(ze), StandardCharsets.UTF_8)) {
                    var sw = new StringWriter();
                    input.transferTo(sw);
                    var css = sw.toString();
                    if (proportionalFont.isPresent()) {
                        for (var pattern : proportionalFontPatterns) {
                            css = pattern.matcher(css).replaceAll(proportionalFont.get().isEmpty()
                                ? "" : "font-family: %s;".formatted(proportionalFont.get()));
                        }
                    }
                    if (monospacedFont.isPresent()) {
                        css = monospacedFontPattern.matcher(css).replaceAll(monospacedFont.get().isEmpty()
                            ? "" : "font-family: %s;".formatted(monospacedFont.get()));
                    }
                    zipOutput.write(css.getBytes(StandardCharsets.UTF_8));
                }
            } else {
                try (InputStream input = zipInput.getInputStream(ze)) {
                    input.transferTo(zipOutput);
                }
            }

            if (++processed % 100 == 0) {
                System.out.print('.');
            }
        }
    }

}
