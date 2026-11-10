package com.praksh.logsearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IndexerClient {

    private final Path binary;

    public IndexerClient(@Value("${logsearch.indexer-path}") String indexerPath) {
        this.binary = Path.of(indexerPath).toAbsolutePath().normalize();
        if (!Files.isExecutable(binary)) {
            throw new IllegalStateException(
                    "C++ indexer not found at " + binary + ". Build it first.");
        }
    }

    public void build(Path logFile, Path prefix) {
        run(List.of("build", logFile.toString(), prefix.toString()));
    }

    public IndexerResult search(Path logFile, Path prefix, int limit, List<String> terms) {
        List<String> args = new ArrayList<>(List.of(
                "search", logFile.toString(), prefix.toString()));
        args.addAll(terms);

        List<String> out = run(args);
        if (out.get(0).startsWith("TOTAL\t")) {
            throw new IndexerException("Unexpected output from indexer");
        }
        long total = Long.parseLong(out.get(0).substring("TOTAL\t".length()).trim());

        List<LogLine> lines = new ArrayList<>();
        for (int i = 1; i < out.size(); i++) {
            String row = out.get(i);
            int tab = row.indexOf('\t');
            lines.add(new LogLine(Long.parseLong(row.substring(0, tab)), row.substring(tab + 1)));
        }
        return new IndexerResult(total, lines);
    }

    private List<String> run(List<String> args) {
        List<String> command = new ArrayList<>();
        command.add(binary.toString());
        command.addAll(args);

        try {
            Process process = new ProcessBuilder(command).start();

            List<String> out = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.add(line);
                }
            }
            String err = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

            if (process.exitValue() != 0) {
                throw new IndexerException("Indexer failed: " + err.trim());
            }
            return out;
        } catch (IOException e) {
            throw new IndexerException("Could not run indexer: " + e.getMessage());
        };
    }
}
