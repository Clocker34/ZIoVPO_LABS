package ru.rkjrth.adboard.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import ru.rkjrth.adboard.entity.SignatureStatus;

@JsonPropertyOrder(alphabetic = true)
public class SignaturePayload {

    private String fileType;
    private String firstBytesHex;
    private long offsetEnd;
    private long offsetStart;
    private String remainderHashHex;
    private long remainderLength;
    private SignatureStatus status;
    private String threatName;

    public SignaturePayload() {
    }

    public SignaturePayload(String threatName, String firstBytesHex, String remainderHashHex, long remainderLength, String fileType, long offsetStart, long offsetEnd, SignatureStatus status) {
        this.threatName = threatName;
        this.firstBytesHex = firstBytesHex;
        this.remainderHashHex = remainderHashHex;
        this.remainderLength = remainderLength;
        this.fileType = fileType;
        this.offsetStart = offsetStart;
        this.offsetEnd = offsetEnd;
        this.status = status;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFirstBytesHex() {
        return firstBytesHex;
    }

    public void setFirstBytesHex(String firstBytesHex) {
        this.firstBytesHex = firstBytesHex;
    }

    public long getOffsetEnd() {
        return offsetEnd;
    }

    public void setOffsetEnd(long offsetEnd) {
        this.offsetEnd = offsetEnd;
    }

    public long getOffsetStart() {
        return offsetStart;
    }

    public void setOffsetStart(long offsetStart) {
        this.offsetStart = offsetStart;
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

    public SignatureStatus getStatus() {
        return status;
    }

    public void setStatus(SignatureStatus status) {
        this.status = status;
    }

    public String getThreatName() {
        return threatName;
    }

    public void setThreatName(String threatName) {
        this.threatName = threatName;
    }
}
