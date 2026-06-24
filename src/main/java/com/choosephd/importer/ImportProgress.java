package com.choosephd.importer;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ImportProgress {

    private volatile String status = "IDLE";
    private volatile Instant startedAt;
    private volatile Instant finishedAt;
    private final AtomicInteger totalFiles = new AtomicInteger(0);
    private final AtomicInteger processedFiles = new AtomicInteger(0);
    private final AtomicLong totalRecords = new AtomicLong(0);
    private final AtomicLong successRecords = new AtomicLong(0);
    private final AtomicLong failedRecords = new AtomicLong(0);
    private volatile String currentFile;
    private volatile String message;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public AtomicInteger getTotalFiles() {
        return totalFiles;
    }

    public AtomicInteger getProcessedFiles() {
        return processedFiles;
    }

    public AtomicLong getTotalRecords() {
        return totalRecords;
    }

    public AtomicLong getSuccessRecords() {
        return successRecords;
    }

    public AtomicLong getFailedRecords() {
        return failedRecords;
    }

    public String getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(String currentFile) {
        this.currentFile = currentFile;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
