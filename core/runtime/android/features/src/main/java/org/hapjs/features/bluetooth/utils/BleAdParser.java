/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.bluetooth.utils;

import android.util.Pair;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BleAdParser {

    public static final int EBLE_FLAGS =
            0x01; // «Flags»  Bluetooth Core Specification
    public static final int EBLE_16BitUUIDInc =
            0x02; // «Incomplete List of 16-bit Service Class UUIDs»  Bluetooth Core Specification
    public static final int EBLE_16BitUUIDCom =
            0x03; // «Complete List of 16-bit Service Class UUIDs»  Bluetooth Core Specification
    public static final int EBLE_32BitUUIDInc =
            0x04; // «Incomplete List of 32-bit Service Class UUIDs»  Bluetooth Core Specification
    public static final int EBLE_32BitUUIDCom =
            0x05; // «Complete List of 32-bit Service Class UUIDs»  Bluetooth Core Specification
    public static final int EBLE_128BitUUIDInc =
            0x06; // «Incomplete List of 128-bit Service Class UUIDs»  Bluetooth Core Specification
    public static final int EBLE_128BitUUIDCom =
            0x07; // «Complete List of 128-bit Service Class UUIDs»  Bluetooth Core Specification
    public static final int EBLE_SHORTNAME =
            0x08; // «Shortened Local Name»  Bluetooth Core Specification
    public static final int EBLE_COMPLETENAME =
            0x09; // «Complete Local Name»  Bluetooth Core Specification
    public static final int EBLE_16BitSERVICEDATA =
            0X16; // «Service Data»,«Service Data - 16-bit UUID»  Bluetooth Core Specification
    public static final int EBLE_32BitSERVICEDATA =
            0X20; // «Service Data - 32-bit UUID»  Bluetooth Core Specification
    public static final int EBLE_128BitSERVICEDATA =
            0X21; // «Service Data - 128-bit UUID»  Bluetooth Core Specification
    public static final int EBLE_TXPOWERLEVEL =
            0x0A; // «Tx Power Level»  Bluetooth Core Specification
    public static final int EBLE_MANDATA =
            0xFF; // «Manufacturer Specific Data»  Bluetooth Core Specification

    private static final String UUIDDigitPattern = "%08x-0000-1000-8000-00805f9b34fb";
    private static final String UUIDStrPattern = "00000000";
    private static final String UUIDBasePattern = "-0000-1000-8000-00805f9b34fb";
    private static final int LENGHT_32_UUID = 8;

    public static String parse16BitUUID(ByteBuffer data) {
        return String.format(UUIDDigitPattern, data.getShort()).toUpperCase();
    }

    public static String parse32BitUUID(ByteBuffer data) {
        return String.format(UUIDDigitPattern, data.getInt()).toUpperCase();
    }

    public static String parse128BitUUID(ByteBuffer data) {
        long lsb = data.getLong();
        long msb = data.getLong();
        return new UUID(msb, lsb).toString().toUpperCase();
    }

    public static byte[] getBytes(ByteBuffer data, int length) {
        if (length <= 0) {
            return new byte[0];
        }
        byte[] result = new byte[Math.min(length, data.remaining())];
        data.get(result);
        return result;
    }

    public static List<BleAdData> parseRecord(byte[] scanRecord) {
        List<BleAdData> ret = new ArrayList<>();
        ByteBuffer byteBuffer = ByteBuffer.wrap(scanRecord).order(ByteOrder.LITTLE_ENDIAN);
        while (byteBuffer.remaining() > 2) {
            int length = byteBuffer.get() & 0xff;
            if (length == 0) {
                break;
            }
            int type = byteBuffer.get() & 0xff;
            length--;
            switch (type) {
                case EBLE_16BitUUIDInc:
                case EBLE_16BitUUIDCom:
                    while (length >= 2) {
                        BleAdData data = new BleAdData(type);
                        data.setData(parse16BitUUID(byteBuffer));
                        ret.add(data);
                        length -= 2;
                    }
                    break;
                case EBLE_32BitUUIDInc:
                case EBLE_32BitUUIDCom:
                    while (length >= 4) {
                        BleAdData data = new BleAdData(type);
                        data.setData(parse32BitUUID(byteBuffer));
                        ret.add(data);
                        length -= 4;
                    }
                    break;
                case EBLE_128BitUUIDInc:
                case EBLE_128BitUUIDCom:
                    while (length >= 16) {
                        BleAdData data = new BleAdData(type);
                        data.setData(parse128BitUUID(byteBuffer));
                        ret.add(data);
                        length -= 16;
                    }
                    break;
                case EBLE_SHORTNAME:
                case EBLE_COMPLETENAME:
                    BleAdData nameData = new BleAdData(type);
                    nameData.setData(
                            new String(getBytes(byteBuffer, length), StandardCharsets.UTF_8));
                    length = 0;
                    ret.add(nameData);
                    break;
                case EBLE_16BitSERVICEDATA:
                    BleAdData serviceData16 = new BleAdData(type);
                    length -= 2;
                    Pair<String, byte[]> data16 =
                            new Pair<>(parse16BitUUID(byteBuffer), getBytes(byteBuffer, length));
                    serviceData16.setData(data16);
                    ret.add(serviceData16);
                    break;
                case EBLE_32BitSERVICEDATA:
                    BleAdData serviceData32 = new BleAdData(type);
                    length -= 4;
                    Pair<String, byte[]> data32 =
                            new Pair<>(parse32BitUUID(byteBuffer), getBytes(byteBuffer, length));
                    serviceData32.setData(data32);
                    ret.add(serviceData32);
                    break;
                case EBLE_128BitSERVICEDATA:
                    BleAdData serviceData128 = new BleAdData(type);
                    length -= 16;
                    Pair<String, byte[]> data128 =
                            new Pair<>(parse128BitUUID(byteBuffer), getBytes(byteBuffer, length));
                    serviceData128.setData(data128);
                    ret.add(serviceData128);
                    break;
                case EBLE_MANDATA:
                    BleAdData defaultData = new BleAdData(type);
                    defaultData.setData(getBytes(byteBuffer, length));
                    length = 0;
                    ret.add(defaultData);
                    break;
                default:
                    // ignore other data
                    break;
            }
            if (length > 0) {
                byteBuffer
                        .position(byteBuffer.position() + Math.min(length, byteBuffer.remaining()));
            }
        }

        return ret;
    }

    public static UUID string2UUID(String uuid) throws IllegalArgumentException {
        if (uuid.length() <= LENGHT_32_UUID) {
            return UUID
                    .fromString(UUIDStrPattern.substring(uuid.length()) + uuid + UUIDBasePattern);
        }
        return UUID.fromString(uuid);
    }

    public static class BleAdData {
        private int mType;
        private Object mData;

        public BleAdData(int type) {
            mType = type;
        }

        public int getType() {
            return mType;
        }

        public void setType(int type) {
            mType = type;
        }

        public <T> T getData() {
            return (T) mData;
        }

        public void setData(Object data) {
            this.mData = data;
        }
    }
}
