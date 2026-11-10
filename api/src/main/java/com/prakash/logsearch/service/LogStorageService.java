package com.prakash.logsearch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;


@Service
public class LogStorageService {

    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,200}");

    private final Path dir;
    private final IndexerClient indexer;

    public LogStorageService(@Value("${logsearch.storage-dir}") String storageDir,
                             IndexerClient indexer) throws IOException {
        this.dir = Path.of(storageDir).toAbsolutePath().normalize();
        this.indexer = indexer;
        Files.createDirectories(dir);
    }

    public LogFileInfo save(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        String original = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        String safeName = original.replaceAll("[^A-Za-z0-9._-]", "_").replaceAll("\\.log$", "");
        String id = UUID.randomUUID().toString().substring(0, 8) + "-" + safeName;

        Path logFile = dir.resolve(id + ".log");
        file.transferTo(logFile);

        try {
            indexer.build(logFile, dir.resolve(id));
        } catch (RuntimeException e) {
            delete(id);
            throw e;
        }
        return new LogFileInfo(id, Files.size(logFile));
    }

}