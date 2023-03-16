/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import com.eclipsesource.v8.utils.typedarrays.ArrayBuffer;

import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.InstanceManager;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.features.nfc.base.BaseInstance;
import org.hapjs.render.jsruntime.serialize.JavaSerializeArray;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;
import org.hapjs.render.jsruntime.serialize.SerializeArray;
import org.hapjs.render.jsruntime.serialize.SerializeObject;

import java.util.Arrays;

public class NFCAdapterInstance extends BaseInstance {

    public static final String RESULT_TECHS = "techs";
    public static final String RESULT_MESSAGES = "messages";
    public static final String RESULT_ID = "id";
    public static final String RESULT_MESSAGES_RECORDS = "records";
    public static final String RESULT_MESSAGES_RECORD_ID = "id";
    public static final String RESULT_MESSAGES_RECORD_TYPE = "type";
    public static final String RESULT_MESSAGES_RECORD_PAYLOAD = "payload";
    public static final String RESULT_MESSAGES_RECORD_TNF = "tnf";

    private Activity mActivity;
    private HybridManager mHybridManager;
    private NfcAdapterLifecycleListener mAdapterLifecycleListener;
    private Tag mDiscoveredTag;
    private Request mDiscoveredRequest;
    private boolean mIsDispatchEnabled = false;

    public NFCAdapterInstance(Activity activity, HybridManager hybridManager, NfcAdapter nfcAdapter) {
        super(nfcAdapter);
        this.mActivity = activity;
        this.mHybridManager = hybridManager;
    }

    public void startDiscovery(Request request) {
        mAdapterLifecycleListener = new NfcAdapterLifecycleListener();
        mHybridManager.addLifecycleListener(mAdapterLifecycleListener);
        enableForegroundDispatch();
        request.getCallback().callback(Response.SUCCESS);
    }

    public void stopDiscovery(Request request) {
        disableForegroundDispatch();
        mHybridManager.removeLifecycleListener(mAdapterLifecycleListener);
        request.getCallback().callback(Response.SUCCESS);
    }

    public void onDiscovered(Request request) {
        mDiscoveredRequest = request;
    }

    public Response offDiscovered(Request request) {
        mDiscoveredRequest = null;
        return Response.SUCCESS;
    }

    public Response getTechInstance(String action, Request request) {
        if (null != mDiscoveredTag) {
            InstanceManager.IInstance instance = null;
            try {
                switch (action) {
                    case NFC.ACTION_GET_NDEF:
                        instance = new NdefInstance(mNfcAdapter, Ndef.get(mDiscoveredTag));
                        break;

                    case NFC.ACTION_GET_NFCA:
                        instance = new NfcAInstance(mNfcAdapter, NfcA.get(mDiscoveredTag));
                        break;

                    case NFC.ACTION_GET_NFCB:
                        instance = new NfcBInstance(mNfcAdapter, NfcB.get(mDiscoveredTag));
                        break;

                    case NFC.ACTION_GET_NFCF:
                        instance = new NfcFInstance(mNfcAdapter, NfcF.get(mDiscoveredTag));
                        break;

                    case NFC.ACTION_GET_NFCV:
                        instance = new NfcVInstance(mNfcAdapter, NfcV.get(mDiscoveredTag));
                        break;

                    case NFC.ACTION_GET_MIFARE_CLASSIC:
                        instance = new MifareClassicInstance(mNfcAdapter, MifareClassic.get(mDiscoveredTag));
                        break;

                    case NFC.ACTION_GET_MIFARE_ULTRALIGHT:
                        instance = new MifareUltralightInstance(mNfcAdapter, MifareUltralight.get(mDiscoveredTag));
                        break;

                    case NFC.ACTION_GET_ISO_DEP:
                        instance = new IsoDepInstance(mNfcAdapter, IsoDep.get(mDiscoveredTag));
                        break;
                    default:
                        Log.e(TAG, "unsupport tech.");
                        break;
                }
            } catch (NullPointerException e) {
                Log.e(TAG, "Null of tech.");
                instance = null;
            }
            if (null != instance) {
                HybridManager hybridManager = request.getView().getHybridManager();
                return new Response(InstanceManager.getInstance().createInstance(hybridManager, instance));
            } else {
                return null;
            }
        } else {
            Log.e(TAG, "null of nfc tag");
            return null;
        }
    }

    @Override
    public void release() {
        try {
            if (mIsDispatchEnabled) {
                disableForegroundDispatch();
            }
            mHybridManager.removeLifecycleListener(mAdapterLifecycleListener);
        } catch (Exception e) {
            Log.e(TAG, "unregister receiver error");
        }
    }

    private void enableForegroundDispatch() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent(mActivity, mActivity.getClass());
                    PendingIntent pendingIntent = PendingIntent.getActivity(mActivity, 0, intent, 0);
                    if (isEnabled()) {
                        mNfcAdapter.enableForegroundDispatch(mActivity, pendingIntent, null, null);
                        mIsDispatchEnabled = true;
                    }
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Failed to enableForegroundDispatch", e);
                } catch (UnsupportedOperationException e) {
                    Log.e(TAG, "Failed to enableForegroundDispatch", e);
                }
            }
        });
    }

    private void disableForegroundDispatch() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isEnabled()) {
                        mNfcAdapter.disableForegroundDispatch(mActivity);
                        mIsDispatchEnabled = false;
                    }
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Failed to disableForegroundDispatch", e);
                } catch (UnsupportedOperationException e) {
                    Log.e(TAG, "Failed to disableForegroundDispatch", e);
                }
            }
        });
    }

    private void onTagDiscovered(Intent intent) {
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction()) ||
                NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (null != tag) {
                mDiscoveredTag = tag;
            }

            if (null != mDiscoveredRequest) {
                SerializeObject resultObj = new JavaSerializeObject();
                if (null != mDiscoveredTag) {
                    SerializeArray arrayTech = getTechs(mDiscoveredTag.getTechList());
                    if (null != arrayTech) {
                        resultObj.put(RESULT_TECHS, arrayTech);
                    } else {
                        Log.e(TAG, "null of techs.");
                    }

                    byte[] id = mDiscoveredTag.getId();
                    Log.d(TAG, "id: " + Arrays.toString(id));
                    resultObj.put(RESULT_ID, new ArrayBuffer(id));
                } else {
                    Log.e(TAG, "null of discovered tag");
                }

                if (TextUtils.equals(intent.getAction(), NfcAdapter.ACTION_NDEF_DISCOVERED)) {
                    Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                    SerializeArray arrayMessage = getMessages(rawMessages);
                    if (null != arrayMessage) {
                        resultObj.put(RESULT_MESSAGES, arrayMessage);
                    }
                }

                mDiscoveredRequest.getCallback().callback(new Response(resultObj));
            } else {
                Log.e(TAG, "null of callback.");
            }
        }
    }

    private SerializeArray getTechs(String[] techList) {
        if (null != techList && techList.length > 0) {
            SerializeArray arrayTech = new JavaSerializeArray();
            for (String tech : techList) {
                String techStr = NFCUtils.getTechStr(tech);
                if (!TextUtils.isEmpty(techStr)) {
                    arrayTech.put(techStr);
                }
            }
            return arrayTech;
        } else {
            Log.e(TAG, "null of techs.");
            return null;
        }
    }

    private SerializeArray getMessages(Parcelable[] rawMessages) {
        NdefMessage[] ndefMessages = null;
        if (rawMessages != null && rawMessages.length > 0) {
            ndefMessages = new NdefMessage[rawMessages.length];
            for (int i = 0; i < rawMessages.length; i++) {
                ndefMessages[i] = (NdefMessage) rawMessages[i];
            }

            SerializeArray arrayMessage = new JavaSerializeArray();
            for (NdefMessage message : ndefMessages) {
                SerializeObject messasgeObj = new JavaSerializeObject();

                NdefRecord[] ndefRecords = message.getRecords();
                SerializeArray arrayRecord = new JavaSerializeArray();
                for (NdefRecord record : ndefRecords) {
                    SerializeObject recordObj = new JavaSerializeObject();
                    recordObj.put(RESULT_MESSAGES_RECORD_ID, new ArrayBuffer(record.getId()));
                    recordObj.put(RESULT_MESSAGES_RECORD_PAYLOAD, new ArrayBuffer(record.getPayload()));
                    recordObj.put(RESULT_MESSAGES_RECORD_TYPE, new ArrayBuffer(record.getType()));
                    recordObj.put(RESULT_MESSAGES_RECORD_TNF, record.getTnf());
                    arrayRecord.put(recordObj);
                }

                messasgeObj.put(RESULT_MESSAGES_RECORDS, arrayRecord);
                arrayMessage.put(messasgeObj);
            }
            return arrayMessage;
        } else {
            Log.e(TAG, "null of messages.");
            return null;
        }
    }

    private class NfcAdapterLifecycleListener extends LifecycleListener {

        @Override
        public void onNewIntent(Intent intent) {
            super.onNewIntent(intent);
            onTagDiscovered(intent);
        }

        @Override
        public void onResume() {
            super.onResume();
            enableForegroundDispatch();
        }

        @Override
        public void onPause() {
            super.onPause();
            disableForegroundDispatch();
        }
    }

    @Override
    public String getFeatureName() {
        return NFC.FEATURE_NAME;
    }
}
