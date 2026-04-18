package ru.rkjrth.adboard.dto.license;

import java.util.UUID;

public class CreateLicenseRequest {

    private UUID productId;
    private UUID typeId;
    /** Пользователь, на которого выписана лицензия. */
    private Long userId;
    private Long ownerId;
    private String description;
    /** Если null — берётся разумный дефолт (например 1). */
    private Integer deviceCount;

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public UUID getTypeId() {
        return typeId;
    }

    public void setTypeId(UUID typeId) {
        this.typeId = typeId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDeviceCount() {
        return deviceCount;
    }

    public void setDeviceCount(Integer deviceCount) {
        this.deviceCount = deviceCount;
    }
}
