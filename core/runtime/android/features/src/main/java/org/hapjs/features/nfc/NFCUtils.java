/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.nfc;

import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.text.TextUtils;

public class NFCUtils {

    public static final String TECH_NFC_A = "NFC_A";

    public static final String TECH_NFC_B = "NFC_B";

    public static final String TECH_NFC_F = "NFC_F";

    public static final String TECH_NFC_V = "NFC_V";

    public static final String TECH_NDEF = "NDEF";

    public static final String TECH_ISO_DEP = "ISO_DEP";

    public static final String TECH_MIFARE_CLASSIC = "MIFARE_CLASSIC";

    public static final String TECH_MIFARE_ULTRALIGHT = "MIFARE_ULTRALIGHT";

    public static String getTechStr(String tech) {
        if (!TextUtils.isEmpty(tech)) {
            if (TextUtils.equals(tech, NfcA.class.getName())) {
                return TECH_NFC_A;

            } else if (TextUtils.equals(tech, NfcB.class.getName())) {
                return TECH_NFC_B;

            } else if (TextUtils.equals(tech, NfcF.class.getName())) {
                return TECH_NFC_F;

            } else if (TextUtils.equals(tech, NfcV.class.getName())) {
                return TECH_NFC_V;

            } else if (TextUtils.equals(tech, Ndef.class.getName())) {
                return TECH_NDEF;

            } else if (TextUtils.equals(tech, IsoDep.class.getName())) {
                return TECH_ISO_DEP;

            } else if (TextUtils.equals(tech, MifareClassic.class.getName())) {
                return TECH_MIFARE_CLASSIC;

            } else if (TextUtils.equals(tech, MifareUltralight.class.getName())) {
                return TECH_MIFARE_ULTRALIGHT;

            }
        }
        return "UNSUPPORT";
    }

}
