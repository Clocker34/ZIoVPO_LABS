package ru.rkjrth.adboard.binary;

/**
 * exportType in manifest header (uint8).
 */
public final class BinaryExportType {
    public static final int FULL = 0;
    public static final int INCREMENT = 1;
    public static final int BY_IDS = 2;

    private BinaryExportType() {
    }
}
