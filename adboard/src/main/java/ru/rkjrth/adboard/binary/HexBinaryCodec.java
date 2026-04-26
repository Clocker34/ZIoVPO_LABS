package ru.rkjrth.adboard.binary;

/**
 * Decodes domain hex strings (firstBytesHex, remainderHashHex) to raw bytes for data.bin.
 */
public final class HexBinaryCodec {

    private HexBinaryCodec() {
    }

    public static byte[] decodeHex(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("hex required");
        }
        String s = hex.trim();
        if (s.startsWith("0x") || s.startsWith("0X")) {
            s = s.substring(2);
        }
        if (s.isEmpty()) {
            return new byte[0];
        }
        if ((s.length() & 1) == 1) {
            throw new IllegalArgumentException("hex string must have even length");
        }
        int len = s.length() / 2;
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            int hi = Character.digit(s.charAt(i * 2), 16);
            int lo = Character.digit(s.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("invalid hex");
            }
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }
}
