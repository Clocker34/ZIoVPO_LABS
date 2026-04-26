package ru.rkjrth.adboard.binary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Big-endian serialization for all multi-byte fields (see multipart.md §7).
 */
public final class BinaryEndianWriter {

    private BinaryEndianWriter() {
    }

    public static void writeU8(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
    }

    public static void writeU16BE(ByteArrayOutputStream out, int value) {
        out.write((value >>> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    public static void writeU32BE(ByteArrayOutputStream out, long value) {
        out.write((int) ((value >>> 24) & 0xFF));
        out.write((int) ((value >>> 16) & 0xFF));
        out.write((int) ((value >>> 8) & 0xFF));
        out.write((int) (value & 0xFF));
    }

    public static void writeI64BE(ByteArrayOutputStream out, long value) {
        writeU32BE(out, value >>> 32);
        writeU32BE(out, value & 0xFFFFFFFFL);
    }

    public static void writeU64BE(ByteArrayOutputStream out, long value) {
        writeI64BE(out, value);
    }

    public static void writeUuidBE(ByteArrayOutputStream out, UUID id) {
        writeI64BE(out, id.getMostSignificantBits());
        writeI64BE(out, id.getLeastSignificantBits());
    }

    /** UTF-8 string prefixed with uint32 length (BigEndian). */
    public static void writeUtf8WithU32Length(ByteArrayOutputStream out, String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeU32BE(out, bytes.length);
        out.write(bytes);
    }

    public static void writeBytesWithU32Length(ByteArrayOutputStream out, byte[] data) throws IOException {
        writeU32BE(out, data.length);
        out.write(data);
    }

    /**
     * Magic field: uint16 length + UTF-8 bytes (prefix MF-/DB- plus label per spec).
     */
    public static void writeMagicField(ByteArrayOutputStream out, String ascii) throws IOException {
        byte[] bytes = ascii.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 0xFFFF) {
            throw new IllegalArgumentException("magic too long");
        }
        writeU16BE(out, bytes.length);
        out.write(bytes);
    }
}
