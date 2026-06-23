package josp.choosphd.domain.auth;

import java.time.LocalDateTime;

public class ImportJobPO {
    private Long id;
    private String jobKey;
    private Integer sourceId;
    private String status;
    private Integer totalRows;
    private Integer processedRows;
    private Integer insertedRows;
    private Integer updatedRows;
    private Integer skippedRows;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getJobKey() { return jobKey; }
    public void setJobKey(String jobKey) { this.jobKey = jobKey; }
    public Integer getSourceId() { return sourceId; }
    public void setSourceId(Integer v) { this.sourceId = v; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTotalRows() { return totalRows; }
    public void setTotalRows(Integer v) { this.totalRows = v; }
    public Integer getProcessedRows() { return processedRows; }
    public void setProcessedRows(Integer v) { this.processedRows = v; }
    public Integer getInsertedRows() { return insertedRows; }
    public void setInsertedRows(Integer v) { this.insertedRows = v; }
    public Integer getUpdatedRows() { return updatedRows; }
    public void setUpdatedRows(Integer v) { this.updatedRows = v; }
    public Integer getSkippedRows() { return skippedRows; }
    public void setSkippedRows(Integer v) { this.skippedRows = v; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { this.errorMessage = v; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime v) { this.startedAt = v; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime v) { this.finishedAt = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer v) { this.deleted = v; }
}
