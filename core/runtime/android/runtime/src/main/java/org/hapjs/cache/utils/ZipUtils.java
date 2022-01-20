/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hapjs.cache.utils;

import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.hapjs.common.utils.FileUtils;

/**
 * Assorted ZIP format helpers.
 *
 * <p>NOTE: Most helper methods operating on {@code ByteBuffer} instances expect that the byte order
 * of these buffers is little-endian.
 */
public abstract class ZipUtils {
    private static final String TAG = "ZipUtils";
    private static final int ZIP_EOCD_REC_MIN_SIZE = 22;
    private static final int ZIP_EOCD_REC_SIG = 0x06054b50;
    private static final int ZIP_EOCD_CENTRAL_DIR_SIZE_FIELD_OFFSET = 12;
    private static final int ZIP_EOCD_CENTRAL_DIR_OFFSET_FIELD_OFFSET = 16;
    private static final int ZIP_EOCD_COMMENT_LENGTH_FIELD_OFFSET = 20;
    private static final int ZIP64_EOCD_LOCATOR_SIZE = 20;
    private static final int ZIP64_EOCD_LOCATOR_SIG_REVERSE_BYTE_ORDER = 0x504b0607;
    private static final int UINT16_MAX_VALUE = 0xffff;

    private ZipUtils() {
    }

    /**
     * Returns the ZIP End of Central Directory record of the provided ZIP file.
     *
     * @return contents of the ZIP End of Central Directory record and the record's offset in the file
     * or {@code null} if the file does not contain the record.
     * @throws IOException if an I/O error occurs while reading the file.
     */
    static Pair<ByteBuffer, Long> findZipEndOfCentralDirectoryRecord(RandomAccessFile zip)
            throws IOException {
        // ZIP End of Central Directory (EOCD) record is located at the very end of the ZIP archive.
        // The record can be identified by its 4-byte signature/magic which is located at the very
        // beginning of the record. A complication is that the record is variable-length because of
        // the comment field.
        // The algorithm for locating the ZIP EOCD record is as follows. We search backwards from
        // end of the buffer for the EOCD record signature. Whenever we find a signature, we check
        // the candidate record's comment length is such that the remainder of the record takes up
        // exactly the remaining bytes in the buffer. The search is bounded because the maximum
        // size of the comment field is 65535 bytes because the field is an unsigned 16-bit number.

        long fileSize = zip.length();
        if (fileSize < ZIP_EOCD_REC_MIN_SIZE) {
            return null;
        }

        // Optimization: 99.99% of APKs have a zero-length comment field in the EoCD record and thus
        // the EoCD record offset is known in advance. Try that offset first to avoid unnecessarily
        // reading more data.
        Pair<ByteBuffer, Long> result = findZipEndOfCentralDirectoryRecord(zip, 0);
        if (result != null) {
            return result;
        }

        // EoCD does not start where we expected it to. Perhaps it contains a non-empty comment
        // field. Expand the search. The maximum size of the comment field in EoCD is 65535 because
        // the comment length field is an unsigned 16-bit number.
        return findZipEndOfCentralDirectoryRecord(zip, UINT16_MAX_VALUE);
    }

    /**
     * Returns the ZIP End of Central Directory record of the provided ZIP file.
     *
     * @param maxCommentSize maximum accepted size (in bytes) of EoCD comment field. The permitted
     *                       value is from 0 to 65535 inclusive. The smaller the value, the faster this method locates
     *                       the record, provided its comment field is no longer than this value.
     * @return contents of the ZIP End of Central Directory record and the record's offset in the file
     * or {@code null} if the file does not contain the record.
     * @throws IOException if an I/O error occurs while reading the file.
     */
    private static Pair<ByteBuffer, Long> findZipEndOfCentralDirectoryRecord(
            RandomAccessFile zip, int maxCommentSize) throws IOException {
        // ZIP End of Central Directory (EOCD) record is located at the very end of the ZIP archive.
        // The record can be identified by its 4-byte signature/magic which is located at the very
        // beginning of the record. A complication is that the record is variable-length because of
        // the comment field.
        // The algorithm for locating the ZIP EOCD record is as follows. We search backwards from
        // end of the buffer for the EOCD record signature. Whenever we find a signature, we check
        // the candidate record's comment length is such that the remainder of the record takes up
        // exactly the remaining bytes in the buffer. The search is bounded because the maximum
        // size of the comment field is 65535 bytes because the field is an unsigned 16-bit number.

        if ((maxCommentSize < 0) || (maxCommentSize > UINT16_MAX_VALUE)) {
            throw new IllegalArgumentException("maxCommentSize: " + maxCommentSize);
        }

        long fileSize = zip.length();
        if (fileSize < ZIP_EOCD_REC_MIN_SIZE) {
            // No space for EoCD record in the file.
            return null;
        }
        // Lower maxCommentSize if the file is too small.
        maxCommentSize = (int) Math.min(maxCommentSize, fileSize - ZIP_EOCD_REC_MIN_SIZE);

        ByteBuffer buf = ByteBuffer.allocate(ZIP_EOCD_REC_MIN_SIZE + maxCommentSize);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        long bufOffsetInFile = fileSize - buf.capacity();
        zip.seek(bufOffsetInFile);
        zip.readFully(buf.array(), buf.arrayOffset(), buf.capacity());
        int eocdOffsetInBuf = findZipEndOfCentralDirectoryRecord(buf);
        if (eocdOffsetInBuf == -1) {
            // No EoCD record found in the buffer
            return null;
        }
        // EoCD found
        buf.position(eocdOffsetInBuf);
        ByteBuffer eocd = buf.slice();
        eocd.order(ByteOrder.LITTLE_ENDIAN);
        return Pair.create(eocd, bufOffsetInFile + eocdOffsetInBuf);
    }

    /**
     * Returns the position at which ZIP End of Central Directory record starts in the provided buffer
     * or {@code -1} if the record is not present.
     *
     * <p>NOTE: Byte order of {@code zipContents} must be little-endian.
     */
    private static int findZipEndOfCentralDirectoryRecord(ByteBuffer zipContents) {
        assertByteOrderLittleEndian(zipContents);

        // ZIP End of Central Directory (EOCD) record is located at the very end of the ZIP archive.
        // The record can be identified by its 4-byte signature/magic which is located at the very
        // beginning of the record. A complication is that the record is variable-length because of
        // the comment field.
        // The algorithm for locating the ZIP EOCD record is as follows. We search backwards from
        // end of the buffer for the EOCD record signature. Whenever we find a signature, we check
        // the candidate record's comment length is such that the remainder of the record takes up
        // exactly the remaining bytes in the buffer. The search is bounded because the maximum
        // size of the comment field is 65535 bytes because the field is an unsigned 16-bit number.

        int archiveSize = zipContents.capacity();
        if (archiveSize < ZIP_EOCD_REC_MIN_SIZE) {
            return -1;
        }
        int maxCommentLength = Math.min(archiveSize - ZIP_EOCD_REC_MIN_SIZE, UINT16_MAX_VALUE);
        int eocdWithEmptyCommentStartPosition = archiveSize - ZIP_EOCD_REC_MIN_SIZE;
        for (int expectedCommentLength = 0;
                expectedCommentLength < maxCommentLength;
                expectedCommentLength++) {
            int eocdStartPos = eocdWithEmptyCommentStartPosition - expectedCommentLength;
            if (zipContents.getInt(eocdStartPos) == ZIP_EOCD_REC_SIG) {
                int actualCommentLength =
                        getUnsignedInt16(zipContents,
                                eocdStartPos + ZIP_EOCD_COMMENT_LENGTH_FIELD_OFFSET);
                if (actualCommentLength == expectedCommentLength) {
                    return eocdStartPos;
                }
            }
        }

        return -1;
    }

    /**
     * Returns {@code true} if the provided file contains a ZIP64 End of Central Directory Locator.
     *
     * @param zipEndOfCentralDirectoryPosition offset of the ZIP End of Central Directory record in
     *                                         the file.
     * @throws IOException if an I/O error occurs while reading the file.
     */
    public static final boolean isZip64EndOfCentralDirectoryLocatorPresent(
            RandomAccessFile zip, long zipEndOfCentralDirectoryPosition) throws IOException {

        // ZIP64 End of Central Directory Locator immediately precedes the ZIP End of Central
        // Directory Record.
        long locatorPosition = zipEndOfCentralDirectoryPosition - ZIP64_EOCD_LOCATOR_SIZE;
        if (locatorPosition < 0) {
            return false;
        }

        zip.seek(locatorPosition);
        // RandomAccessFile.readInt assumes big-endian byte order, but ZIP format uses
        // little-endian.
        return zip.readInt() == ZIP64_EOCD_LOCATOR_SIG_REVERSE_BYTE_ORDER;
    }

    /**
     * Returns the offset of the start of the ZIP Central Directory in the archive.
     *
     * <p>NOTE: Byte order of {@code zipEndOfCentralDirectory} must be little-endian.
     */
    public static long getZipEocdCentralDirectoryOffset(ByteBuffer zipEndOfCentralDirectory) {
        assertByteOrderLittleEndian(zipEndOfCentralDirectory);
        return getUnsignedInt32(
                zipEndOfCentralDirectory,
                zipEndOfCentralDirectory.position() + ZIP_EOCD_CENTRAL_DIR_OFFSET_FIELD_OFFSET);
    }

    /**
     * Sets the offset of the start of the ZIP Central Directory in the archive.
     *
     * <p>NOTE: Byte order of {@code zipEndOfCentralDirectory} must be little-endian.
     */
    public static void setZipEocdCentralDirectoryOffset(
            ByteBuffer zipEndOfCentralDirectory, long offset) {
        assertByteOrderLittleEndian(zipEndOfCentralDirectory);
        setUnsignedInt32(
                zipEndOfCentralDirectory,
                zipEndOfCentralDirectory.position() + ZIP_EOCD_CENTRAL_DIR_OFFSET_FIELD_OFFSET,
                offset);
    }

    /**
     * Returns the size (in bytes) of the ZIP Central Directory.
     *
     * <p>NOTE: Byte order of {@code zipEndOfCentralDirectory} must be little-endian.
     */
    public static long getZipEocdCentralDirectorySizeBytes(ByteBuffer zipEndOfCentralDirectory) {
        assertByteOrderLittleEndian(zipEndOfCentralDirectory);
        return getUnsignedInt32(
                zipEndOfCentralDirectory,
                zipEndOfCentralDirectory.position() + ZIP_EOCD_CENTRAL_DIR_SIZE_FIELD_OFFSET);
    }

    private static void assertByteOrderLittleEndian(ByteBuffer buffer) {
        if (buffer.order() != ByteOrder.LITTLE_ENDIAN) {
            throw new IllegalArgumentException("ByteBuffer byte order must be little endian");
        }
    }

    private static int getUnsignedInt16(ByteBuffer buffer, int offset) {
        return buffer.getShort(offset) & 0xffff;
    }

    private static long getUnsignedInt32(ByteBuffer buffer, int offset) {
        return buffer.getInt(offset) & 0xffffffffL;
    }

    private static void setUnsignedInt32(ByteBuffer buffer, int offset, long value) {
        if ((value < 0) || (value > 0xffffffffL)) {
            throw new IllegalArgumentException("uint32 value of out range: " + value);
        }
        buffer.putInt(buffer.position() + offset, (int) value);
    }

    public static boolean unzip(File zipFile, File targetDirectory) {
        Log.d(TAG,
                "unzip: " + zipFile.getAbsolutePath() + ", " + targetDirectory.getAbsolutePath());
        InputStream is;
        ZipInputStream zis = null;
        FileOutputStream fos = null;
        try {
            String filename;
            is = new FileInputStream(zipFile);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;

            while ((ze = zis.getNextEntry()) != null) {
                filename = ze.getName();
                Log.d(TAG, "unzip entry: " + filename);

                // fix ZipperDown issue
                if (!TextUtils.isEmpty(filename) && filename.contains("../")) {
                    continue;
                }

                File targetFile = new File(targetDirectory, filename);
                if (!targetFile.getCanonicalPath().startsWith(targetDirectory.getCanonicalPath())) {
                    Log.e(TAG, "seems a Zip Path Traversal attack! filename=" + filename);
                    return false;
                }

                if (ze.isDirectory()) {
                    targetFile.mkdirs();
                    continue;
                }

                File targetParentFile = targetFile.getParentFile();
                if (!targetParentFile.exists()) {
                    targetParentFile.mkdirs();
                }

                fos = new FileOutputStream(targetFile);

                while ((count = zis.read(buffer)) != -1) {
                    fos.write(buffer, 0, count);
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            Log.e(TAG, "IO exception.", e);
            return false;
        } finally {
            FileUtils.closeQuietly(fos);
            FileUtils.closeQuietly(zis);
        }

        return true;
    }

    public static long getUncompressedSize(File zipFile) {
        Log.d(TAG, "getUncompressedSize: " + zipFile.getAbsolutePath());
        InputStream is;
        ZipInputStream zis = null;
        long totalSize = 0;
        try {
            String filename;
            is = new FileInputStream(zipFile);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;

            while ((ze = zis.getNextEntry()) != null) {
                filename = ze.getName();
                Log.d(TAG, "unzip entry: " + filename);
                totalSize += ze.getSize();
                zis.closeEntry();
            }
        } catch (IOException e) {
            Log.e(TAG, "IO exception.", e);
        } finally {
            FileUtils.closeQuietly(zis);
        }
        return totalSize;
    }

    public static boolean retrive(File zipFile, String targetFileName, File destFile) {
        Log.d(TAG, "retrive: " + targetFileName);
        ZipInputStream zis = null;
        FileOutputStream fos = null;
        try {
            FileInputStream is = new FileInputStream(zipFile);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;

            while ((ze = zis.getNextEntry()) != null) {
                String filename = ze.getName();
                if (!ze.isDirectory() && filename.equals(targetFileName)) {
                    byte[] buffer = new byte[1024];
                    int count;
                    fos = new FileOutputStream(destFile);
                    while ((count = zis.read(buffer)) != -1) {
                        fos.write(buffer, 0, count);
                    }
                    return true;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "failed to retrive", e);
        } finally {
            FileUtils.closeQuietly(zis);
            FileUtils.closeQuietly(fos);
        }
        return false;
    }

    public static boolean zip(File src, File dst) {
        ZipOutputStream output = null;
        try {
            output = new ZipOutputStream(new FileOutputStream(dst));
            File root = src.isFile() ? src.getParentFile() : src;
            byte[] buffer = new byte[1024 * 4];
            if (zip(root, src, output, buffer)) {
                return true;
            }
        } catch (IOException e) {
            Log.e(TAG, "failed to zip: " + src.getAbsolutePath(), e);
        } finally {
            FileUtils.closeQuietly(output);
        }

        FileUtils.rmRF(dst);
        return false;
    }

    private static boolean zip(File root, File src, ZipOutputStream output, byte[] buffer)
            throws IOException {
        String rootPath = root.getAbsolutePath();
        String path = src.getAbsolutePath();
        if (!path.startsWith(rootPath)) {
            Log.e(TAG, "illegal paths: rootPath=" + rootPath + ", path=" + path);
            return false;
        }
        String relativePath = path.substring(rootPath.length());

        if (src.isFile()) {
            if (relativePath.startsWith(File.separator)) {
                relativePath = relativePath.substring(File.separator.length());
            }
            ZipEntry entry = new ZipEntry(relativePath);
            output.putNextEntry(entry);

            BufferedInputStream input = null;
            try {
                int bytesRead = -1;
                input = new BufferedInputStream(new FileInputStream(src));
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            } finally {
                FileUtils.closeQuietly(input);
            }
            return true;
        } else {
            if (!relativePath.isEmpty()) {
                ZipEntry entry = new ZipEntry(relativePath + File.separator);
                output.putNextEntry(entry);
            }

            boolean result = true;
            File[] files = src.listFiles();
            if (files != null) {
                for (File file : files) {
                    result = zip(root, file, output, buffer);
                    if (!result) {
                        Log.e(TAG, "failed to zip " + file.getAbsolutePath());
                        break;
                    }
                }
            }
            return result;
        }
    }
}
