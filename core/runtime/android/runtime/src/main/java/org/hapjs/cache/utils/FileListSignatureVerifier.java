/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache.utils;

import android.util.Pair;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.hapjs.common.utils.FileUtils;

public class FileListSignatureVerifier {

    private static final CRC32 CRC32 = new CRC32();

    public static void verify(
            File apkFile, SignatureVerifier.SignatureInfo fileListSignature,
            Certificate certificate)
            throws SignatureVerifier.SignatureNotFoundException, SecurityException, IOException {
        ZipFile zipFile = new ZipFile(apkFile);

        ByteBuffer byteBuffer =
                SignatureVerifier.getLengthPrefixedSlice(fileListSignature.signatureBlock);

        // read digest list
        ByteBuffer digests = SignatureVerifier.getLengthPrefixedSlice(byteBuffer);

        // read signature
        ByteBuffer signature = SignatureVerifier.getLengthPrefixedSlice(byteBuffer);
        if (signature.remaining() < 8) {
            throw new SecurityException("Signature record too short");
        }
        int sigAlgorithm = signature.getInt();
        if (!SignatureVerifier.isSupportedSignatureAlgorithm(sigAlgorithm)) {
            throw new SecurityException("signature algorithm not supported:" + sigAlgorithm);
        }
        int bestSigAlgorithm = sigAlgorithm;
        byte[] bestSigAlgorithmSignatureBytes =
                SignatureVerifier.readLengthPrefixedByteArray(signature);

        // verify digest list
        Pair<String, ? extends AlgorithmParameterSpec> signatureAlgorithmParams =
                SignatureVerifier.getSignatureAlgorithmJcaSignatureAlgorithm(bestSigAlgorithm);
        String jcaSignatureAlgorithm = signatureAlgorithmParams.first;
        AlgorithmParameterSpec jcaSignatureAlgorithmParams = signatureAlgorithmParams.second;
        boolean sigVerified;
        try {
            PublicKey publicKey = certificate.getPublicKey();
            Signature sig = Signature.getInstance(jcaSignatureAlgorithm);
            sig.initVerify(publicKey);
            if (jcaSignatureAlgorithmParams != null) {
                sig.setParameter(jcaSignatureAlgorithmParams);
            }
            sig.update(digests);
            sigVerified = sig.verify(bestSigAlgorithmSignatureBytes);
        } catch (NoSuchAlgorithmException
                | InvalidKeyException
                | InvalidAlgorithmParameterException
                | SignatureException e) {
            throw new SecurityException("Failed to verify " + jcaSignatureAlgorithm + " signature",
                    e);
        }
        if (!sigVerified) {
            throw new SecurityException(jcaSignatureAlgorithm + " signature did not verify");
        }

        // parse digests
        Map<Integer, List<byte[]>> digestsMap = new HashMap<>();
        digests.position(0);
        int digestSigAlgorithm = digests.getInt();
        if (digestSigAlgorithm != bestSigAlgorithm) {
            throw new SecurityException("digestSigAlgorithm did not match the signature algorithm");
        }

        while (digests.hasRemaining()) {
            try {
                int key = digests.getInt();
                byte[] contentDigest = SignatureVerifier.readShortLengthPrefixedByteArray(digests);
                List<byte[]> digestList = digestsMap.get(key);
                if (digestList == null) {
                    digestList = new LinkedList<>();
                    digestsMap.put(key, digestList);
                }
                digestList.add(contentDigest);
            } catch (IOException | BufferUnderflowException e) {
                throw new SecurityException("Failed to parse digests record", e);
            }
        }
        // verify every entry
        try {
            verifyEveryEntry(digestsMap, zipFile, bestSigAlgorithm);
        } catch (DigestException e) {
            throw new SecurityException("Failed to verify digest(s) of contents", e);
        } finally {
            FileUtils.closeQuietly(zipFile);
        }
    }

    private static void verifyEveryEntry(
            Map<Integer, List<byte[]>> digestsMap, ZipFile zipFile, int bestSigAlgorithm)
            throws IOException, DigestException {
        int digestAlgorithm =
                SignatureVerifier.getSignatureAlgorithmContentDigestAlgorithm(bestSigAlgorithm);
        String jcaAlgorithmName =
                SignatureVerifier.getContentDigestAlgorithmJcaDigestAlgorithm(digestAlgorithm);
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(jcaAlgorithmName);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(jcaAlgorithmName + " digest not supported", e);
        }
        int digestOutputSizeBytes =
                SignatureVerifier.getContentDigestAlgorithmOutputSizeBytes(digestAlgorithm);
        byte[] actualContentDigest = new byte[digestOutputSizeBytes];
        Enumeration entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry ze = (ZipEntry) entries.nextElement();
            if (ze.isDirectory()) {
                continue;
            }

            final String name = ze.getName();

            List<byte[]> expectedContentDigestList = digestsMap.get(getFileNameHash(name));
            if (expectedContentDigestList == null || expectedContentDigestList.isEmpty()) {
                throw new SecurityException("Add new file:" + name);
            }
            md.reset();
            md.update(FileUtils.readStreamAsBytes(zipFile.getInputStream(ze), -1, true));
            int actualDigestSizeBytes = md.digest(actualContentDigest, 0, digestOutputSizeBytes);
            if (actualDigestSizeBytes != digestOutputSizeBytes) {
                throw new RuntimeException(
                        "Unexpected output size of " + md.getAlgorithm() + " digest: "
                                + actualDigestSizeBytes);
            }
            boolean verified = false;
            for (byte[] expectedContentDigest : expectedContentDigestList) {
                verified = MessageDigest.isEqual(expectedContentDigest, actualContentDigest);
                if (verified) {
                    break;
                }
            }
            if (!verified) {
                throw new SecurityException(
                        SignatureVerifier
                                .getContentDigestAlgorithmJcaDigestAlgorithm(digestAlgorithm)
                                + " digest of contents did not verify");
            }
        }
    }

    private static int getFileNameHash(String name) {
        try {
            CRC32.reset();
            CRC32.update(name.getBytes("UTF-8"));
            return (int) CRC32.getValue();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported utf-8 encode");
        }
    }
}
