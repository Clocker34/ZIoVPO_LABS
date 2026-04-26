package ru.rkjrth.adboard.binary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Builds multipart/mixed body: manifest.bin first, then data.bin (multipart.md §3).
 */
public final class MultipartMixedBodyBuilder {

    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.US_ASCII);

    private MultipartMixedBodyBuilder() {
    }

    public record MultipartBody(String boundary, byte[] body) {
    }

    public static MultipartBody build(byte[] manifestBin, byte[] dataBin) throws IOException {
        String boundary = "----ParamonovBinarySig-" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream out = new ByteArrayOutputStream(manifestBin.length + dataBin.length + 256);

        writePart(out, boundary, "manifest.bin", manifestBin);
        writePart(out, boundary, "data.bin", dataBin);

        out.write(("--" + boundary + "--").getBytes(StandardCharsets.US_ASCII));
        out.write(CRLF);

        return new MultipartBody(boundary, out.toByteArray());
    }

    private static void writePart(ByteArrayOutputStream out, String boundary, String filename, byte[] body)
            throws IOException {
        out.write(("--" + boundary).getBytes(StandardCharsets.US_ASCII));
        out.write(CRLF);
        out.write(("Content-Disposition: attachment; filename=\"" + filename + "\"")
                .getBytes(StandardCharsets.US_ASCII));
        out.write(CRLF);
        out.write("Content-Type: application/octet-stream".getBytes(StandardCharsets.US_ASCII));
        out.write(CRLF);
        out.write(("Content-Length: " + body.length).getBytes(StandardCharsets.US_ASCII));
        out.write(CRLF);
        out.write(CRLF);
        out.write(body);
        out.write(CRLF);
    }
}
