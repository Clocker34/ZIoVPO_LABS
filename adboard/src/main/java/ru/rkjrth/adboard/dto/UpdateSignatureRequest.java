package ru.rkjrth.adboard.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class UpdateSignatureRequest {

    @NotBlank(message = "threatName must not be empty")
    private String threatName;

    @NotBlank(message = "firstBytesHex must not be empty")
    @Pattern(regexp = "^[0-9a-fA-F]+$", message = "firstBytesHex must contain only hex characters")
    private String firstBytesHex;

    @NotBlank(message = "remainderHashHex must not be empty")
    @Pattern(regexp = "^[0-9a-fA-F]+$", message = "remainderHashHex must contain only hex characters")
    private String remainderHashHex;

    @Min(value = 0, message = "remainderLength must be >= 0")
    private long remainderLength;

    @NotBlank(message = "fileType must not be empty")
    private String fileType;

    @Min(value = 0, message = "offsetStart must be >= 0")
    private long offsetStart;

    private long offsetEnd;

    public String getThreatName() {
        return threatName;
    }

    public void setThreatName(String threatName) {
        this.threatName = threatName;
    }

    public String getFirstBytesHex() {
        return firstBytesHex;
    }

    public void setFirstBytesHex(String firstBytesHex) {
        this.firstBytesHex = firstBytesHex;
    }

    public String getRemainderHashHex() {
        return remainderHashHex;
    }

    public void setRemainderHashHex(String remainderHashHex) {
        this.remainderHashHex = remainderHashHex;
    }

    public long getRemainderLength() {
        return remainderLength;
    }

    public void setRemainderLength(long remainderLength) {
        this.remainderLength = remainderLength;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public long getOffsetStart() {
        return offsetStart;
    }

    public void setOffsetStart(long offsetStart) {
        this.offsetStart = offsetStart;
    }

    public long getOffsetEnd() {
        return offsetEnd;
    }

    public void setOffsetEnd(long offsetEnd) {
        this.offsetEnd = offsetEnd;
    }
}
