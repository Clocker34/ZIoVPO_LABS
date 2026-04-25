package ru.rkjrth.adboard.dto.license;

import java.util.UUID;

public class CreateLicenseRequest {

    private UUID productId;
    private UUID typeId;
    /** Пользователь, на которого выписана лицензия. */
    private UUID userId;
    private UUID ownerId;
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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
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
