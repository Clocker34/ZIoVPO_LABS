package ru.rkjrth.adboard.service;

import org.springframework.stereotype.Service;
import ru.mfa.signature.SigningService;
import ru.rkjrth.adboard.binary.BinaryEndianWriter;
import ru.rkjrth.adboard.binary.BinaryExportType;
import ru.rkjrth.adboard.binary.HexBinaryCodec;
import ru.rkjrth.adboard.binary.MultipartMixedBodyBuilder;
import ru.rkjrth.adboard.entity.MalwareSignature;
import ru.rkjrth.adboard.entity.SignatureStatus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

/**
 * Builds manifest.bin and data.bin and multipart packages per multipart.md.
 */
@Service
public class SignatureBinaryExportService {

    public static final int FORMAT_VERSION = 1;
    public static final String MANIFEST_MAGIC = "MF-Парамонов";
    public static final String DATA_MAGIC = "DB-Парамонов";

    private static final long SINCE_UNUSED = -1L;

    private final SigningService signingService;

    public SignatureBinaryExportService(SigningService signingService) {
        this.signingService = signingService;
    }

    public record BinaryParts(byte[] manifestBin, byte[] dataBin) {
    }

    public BinaryParts buildFull(List<MalwareSignature> records) throws Exception {
        List<MalwareSignature> sorted = sortedCopy(records);
        return buildPackage(BinaryExportType.FULL, SINCE_UNUSED, sorted);
    }

    public BinaryParts buildIncrement(List<MalwareSignature> records, long sinceEpochMillis) throws Exception {
        List<MalwareSignature> sorted = sortedCopy(records);
        return buildPackage(BinaryExportType.INCREMENT, sinceEpochMillis, sorted);
    }

    public BinaryParts buildByIds(List<MalwareSignature> records) throws Exception {
        List<MalwareSignature> sorted = sortedCopy(records);
        return buildPackage(BinaryExportType.BY_IDS, SINCE_UNUSED, sorted);
    }

    public MultipartMixedBodyBuilder.MultipartBody buildMultipartFull(List<MalwareSignature> records) throws Exception {
        BinaryParts p = buildFull(records);
        return MultipartMixedBodyBuilder.build(p.manifestBin(), p.dataBin());
    }

    public MultipartMixedBodyBuilder.MultipartBody buildMultipartIncrement(List<MalwareSignature> records,
                                                                           long sinceEpochMillis) throws Exception {
        BinaryParts p = buildIncrement(records, sinceEpochMillis);
        return MultipartMixedBodyBuilder.build(p.manifestBin(), p.dataBin());
    }

    public MultipartMixedBodyBuilder.MultipartBody buildMultipartByIds(List<MalwareSignature> records)
            throws Exception {
        BinaryParts p = buildByIds(records);
        return MultipartMixedBodyBuilder.build(p.manifestBin(), p.dataBin());
    }

    private List<MalwareSignature> sortedCopy(List<MalwareSignature> records) {
        List<MalwareSignature> copy = new ArrayList<>(records);
        copy.sort(Comparator.comparing(MalwareSignature::getId));
        return copy;
    }

    private BinaryParts buildPackage(int exportType, long sinceEpochMillis, List<MalwareSignature> sorted)
            throws Exception {
        long generatedAt = System.currentTimeMillis();

        List<byte[]> dataRecords = new ArrayList<>(sorted.size());
        for (MalwareSignature sig : sorted) {
            dataRecords.add(encodeDataRecord(sig));
        }

        long running = 0;
        List<Long> offsets = new ArrayList<>(sorted.size());
        List<Long> lengths = new ArrayList<>(sorted.size());
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        for (byte[] rec : dataRecords) {
            offsets.add(running);
            lengths.add((long) rec.length);
            payload.write(rec);
            running += rec.length;
        }

        ByteArrayOutputStream dataFile = new ByteArrayOutputStream();
        BinaryEndianWriter.writeMagicField(dataFile, DATA_MAGIC);
        BinaryEndianWriter.writeU16BE(dataFile, FORMAT_VERSION);
        BinaryEndianWriter.writeU32BE(dataFile, sorted.size());
        dataFile.write(payload.toByteArray());
        byte[] dataBin = dataFile.toByteArray();

        byte[] dataSha256 = sha256(dataBin);

        ByteArrayOutputStream unsignedManifest = new ByteArrayOutputStream();
        BinaryEndianWriter.writeMagicField(unsignedManifest, MANIFEST_MAGIC);
        BinaryEndianWriter.writeU16BE(unsignedManifest, FORMAT_VERSION);
        BinaryEndianWriter.writeU8(unsignedManifest, exportType);
        BinaryEndianWriter.writeI64BE(unsignedManifest, generatedAt);
        BinaryEndianWriter.writeI64BE(unsignedManifest, sinceEpochMillis);
        BinaryEndianWriter.writeU32BE(unsignedManifest, sorted.size());
        unsignedManifest.write(dataSha256);

        for (int i = 0; i < sorted.size(); i++) {
            MalwareSignature sig = sorted.get(i);
            BinaryEndianWriter.writeUuidBE(unsignedManifest, sig.getId());
            BinaryEndianWriter.writeU8(unsignedManifest, statusCode(sig.getStatus()));
            BinaryEndianWriter.writeI64BE(unsignedManifest, sig.getUpdatedAt().toEpochMilli());
            BinaryEndianWriter.writeU64BE(unsignedManifest, offsets.get(i));
            BinaryEndianWriter.writeU64BE(unsignedManifest, lengths.get(i));
            byte[] recordSig = decodeRecordSignature(sig);
            BinaryEndianWriter.writeU32BE(unsignedManifest, recordSig.length);
            unsignedManifest.write(recordSig);
        }

        byte[] unsignedBytes = unsignedManifest.toByteArray();
        byte[] manifestSig = signingService.signBytes(unsignedBytes);

        ByteArrayOutputStream manifestOut = new ByteArrayOutputStream();
        manifestOut.write(unsignedBytes);
        BinaryEndianWriter.writeU32BE(manifestOut, manifestSig.length);
        manifestOut.write(manifestSig);

        return new BinaryParts(manifestOut.toByteArray(), dataBin);
    }

    private static byte[] encodeDataRecord(MalwareSignature sig) throws IOException {
        ByteArrayOutputStream r = new ByteArrayOutputStream();
        BinaryEndianWriter.writeUtf8WithU32Length(r, sig.getThreatName());
        BinaryEndianWriter.writeBytesWithU32Length(r, HexBinaryCodec.decodeHex(sig.getFirstBytesHex()));
        BinaryEndianWriter.writeBytesWithU32Length(r, HexBinaryCodec.decodeHex(sig.getRemainderHashHex()));
        BinaryEndianWriter.writeI64BE(r, sig.getRemainderLength());
        BinaryEndianWriter.writeUtf8WithU32Length(r, sig.getFileType());
        BinaryEndianWriter.writeI64BE(r, sig.getOffsetStart());
        BinaryEndianWriter.writeI64BE(r, sig.getOffsetEnd());
        return r.toByteArray();
    }

    private static int statusCode(SignatureStatus status) {
        return switch (status) {
            case ACTUAL -> 1;
            case DELETED -> 2;
        };
    }

    private static byte[] decodeRecordSignature(MalwareSignature sig) {
        String b64 = sig.getDigitalSignatureBase64();
        if (b64 == null || b64.isBlank()) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(b64.getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] sha256(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(data);
    }
}
