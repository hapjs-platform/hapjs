/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.nfc;

import android.Manifest;
import android.app.Activity;
import android.nfc.NfcAdapter;
import android.util.Log;

import org.hapjs.bridge.CallbackHybridFeature;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.InstanceManager;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.features.nfc.base.BaseInstance;
import org.hapjs.features.nfc.base.BaseTagTechInstance;
import org.hapjs.features.nfc.base.BaseTechInstance;
import org.json.JSONException;

@FeatureExtensionAnnotation(
        name = NFC.FEATURE_NAME,
        residentType = FeatureExtensionAnnotation.ResidentType.FORBIDDEN,
        actions = {
                @ActionAnnotation(name = NFC.ACTION_GET_NFC_ADAPTER, mode = FeatureExtension.Mode.SYNC),
                // NFCAdapter
                @ActionAnnotation(name = NFC.ACTION_START_DISCOVERY, mode = FeatureExtension.Mode.ASYNC, permissions = {Manifest.permission.NFC}),
                @ActionAnnotation(name = NFC.ACTION_STOP_DISCOVERY, mode = FeatureExtension.Mode.ASYNC, permissions = {Manifest.permission.NFC}),
                @ActionAnnotation(name = NFC.ACTION_ON_DISCOVERED, mode = FeatureExtension.Mode.CALLBACK, normalize = FeatureExtension.Normalize.RAW),
                @ActionAnnotation(name = NFC.ACTION_OFF_DISCOVERED, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = NFC.ACTION_GET_NDEF, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = NFC.ACTION_GET_NFCA, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = NFC.ACTION_GET_NFCB, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = NFC.ACTION_GET_NFCF, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = NFC.ACTION_GET_NFCV, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = NFC.ACTION_GET_ISO_DEP, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = NFC.ACTION_GET_MIFARE_CLASSIC, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = NFC.ACTION_GET_MIFARE_ULTRALIGHT, mode = FeatureExtension.Mode.SYNC),
                // NfcA
                @ActionAnnotation(name = NFC.ACTION_CLOSE, mode = FeatureExtension.Mode.ASYNC, permissions = {Manifest.permission.NFC}),
                @ActionAnnotation(name = NFC.ACTION_CONNECT, mode = FeatureExtension.Mode.ASYNC, permissions = {Manifest.permission.NFC}),
                @ActionAnnotation(name = NFC.ACTION_GET_MAX_TRANSCEIVE_LENGTH, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = NFC.ACTION_GET_ATQA, mode = FeatureExtension.Mode.ASYNC, normalize = FeatureExtension.Normalize.RAW),
                @ActionAnnotation(name = NFC.ACTION_GET_SAK, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = NFC.ACTION_IS_CONNECTED, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = NFC.ACTION_SET_TIMEOUT, mode = FeatureExtension.Mode.ASYNC, permissions = {Manifest.permission.NFC}),
                @ActionAnnotation(name = NFC.ACTION_TRANSCEIVE, mode = FeatureExtension.Mode.ASYNC, normalize = FeatureExtension.Normalize.RAW, permissions = {Manifest.permission.NFC}),
                // Ndef
                @ActionAnnotation(name = NFC.ACTION_WRITE_NDEF_MESSAGE, mode = FeatureExtension.Mode.CALLBACK, permissions = {Manifest.permission.NFC}),
                // IsoDep
                @ActionAnnotation(name = NFC.ACTION_GET_HISTORICAL_BYTES, mode = FeatureExtension.Mode.ASYNC, normalize = FeatureExtension.Normalize.RAW),
        }
)
public class NFC extends CallbackHybridFeature {

    private static final String TAG = "NfcFeature";

    public static final String FEATURE_NAME = "system.nfc";

    // NFCAdapter
    public static final String ACTION_GET_NFC_ADAPTER = "getNFCAdapter";
    public static final String ACTION_START_DISCOVERY = "startDiscovery";
    public static final String ACTION_STOP_DISCOVERY = "stopDiscovery";
    public static final String ACTION_ON_DISCOVERED = "onDiscovered";
    public static final String ACTION_OFF_DISCOVERED = "offDiscovered";
    public static final String ACTION_GET_NDEF = "getNdef";
    public static final String ACTION_GET_NFCA = "getNfcA";
    public static final String ACTION_GET_NFCB = "getNfcB";
    public static final String ACTION_GET_NFCF = "getNfcF";
    public static final String ACTION_GET_NFCV = "getNfcV";
    public static final String ACTION_GET_ISO_DEP = "getIsoDep";
    public static final String ACTION_GET_MIFARE_CLASSIC = "getMifareClassic";
    public static final String ACTION_GET_MIFARE_ULTRALIGHT = "getMifareUltralight";

    // NfcA
    public static final String ACTION_CLOSE = "close";
    public static final String ACTION_CONNECT = "connect";
    public static final String ACTION_IS_CONNECTED = "isConnected";
    public static final String ACTION_GET_MAX_TRANSCEIVE_LENGTH = "getMaxTransceiveLength";
    public static final String ACTION_SET_TIMEOUT = "setTimeout";
    public static final String ACTION_TRANSCEIVE = "transceive";
    public static final String ACTION_GET_ATQA = "getAtqa";
    public static final String ACTION_GET_SAK = "getSak";

    // Ndef
    public static final String ACTION_WRITE_NDEF_MESSAGE = "writeNdefMessage";

    public static final String ACTION_GET_HISTORICAL_BYTES = "getHistoricalBytes";

    public static final String PARAM_URIS = "uris";
    public static final String PARAM_TEXTS = "texts";
    public static final String PARAM_RECORDS = "records";
    public static final String PARAM_RECORDS_TNF = "tnf";
    public static final String PARAM_RECORDS_ID = "id";
    public static final String PARAM_RECORDS_PAYLOAD = "payload";
    public static final String PARAM_RECORDS_TYPE = "type";
    public static final String PARAM_TIMEOUT = "timeout";
    public static final int DEFAULT_TIMEOUT = 3000;
    public static final String PARAM_TRANSCEIVE_DATA = "data";

    public static final String RESULT_MAX_TRANSCEIVE_LENGTH = "length";
    public static final String RESULT_HISTORICAL_BYTES = "histBytes";
    public static final String RESULT_TRANSCEIVE_DATA = "data";
    public static final String RESULT_ATQA = "atqa";
    public static final String RESULT_SAK = "sak";
    public static final String RESULT_IS_CONNECTED = "isConnected";

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        switch (action) {
            case ACTION_GET_NFC_ADAPTER:
                return getNFCAdapter(request);

            // NFCAdapter
            case ACTION_START_DISCOVERY:
                startDiscovery(request);
                break;
            case ACTION_STOP_DISCOVERY:
                stopDiscovery(request);
                break;
            case ACTION_ON_DISCOVERED:
                onDiscovered(request);
                break;
            case ACTION_OFF_DISCOVERED:
                return offDiscovered(request);
            case ACTION_GET_NDEF:
            case ACTION_GET_NFCA:
            case ACTION_GET_NFCB:
            case ACTION_GET_NFCF:
            case ACTION_GET_NFCV:
            case ACTION_GET_MIFARE_CLASSIC:
            case ACTION_GET_MIFARE_ULTRALIGHT:
            case ACTION_GET_ISO_DEP:
                return getTechInstance(action, request);

            case ACTION_CLOSE:
                close(request);
                break;
            case ACTION_CONNECT:
                connect(request);
                break;
            case ACTION_IS_CONNECTED:
                isConnected(request);
                break;
            case ACTION_GET_MAX_TRANSCEIVE_LENGTH:
                getMaxTransceiveLength(request);
                break;
            case ACTION_SET_TIMEOUT:
                setTimeout(request);
                break;
            case ACTION_TRANSCEIVE:
                transceive(request);
                break;
            case ACTION_GET_ATQA:
                getAtqa(request);
                break;
            case ACTION_GET_SAK:
                getSak(request);
                break;

            // Ndef
            case ACTION_WRITE_NDEF_MESSAGE:
                writeNdefMessage(request);
                break;

            // IsoDep
            case ACTION_GET_HISTORICAL_BYTES:
                getHistoricalBytes(request);
                break;
            default:
                Log.w(TAG, "unknown action");
                return Response.NO_ACTION;
        }
        return Response.SUCCESS;
    }

    protected Response getNFCAdapter(Request request) {
        HybridManager hybridManager = request.getView().getHybridManager();
        Activity activity = request.getNativeInterface().getActivity();
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(activity);
        NFCAdapterInstance instance = new NFCAdapterInstance(activity, hybridManager, adapter);
        return new Response(InstanceManager.getInstance().createInstance(hybridManager, instance));
    }

    protected void startDiscovery(Request request) {
        BaseInstance instance = getInstance(request);
        if (checkInstance(instance, request)) {
            if (instance instanceof NFCAdapterInstance) {
                ((NFCAdapterInstance) instance).startDiscovery(request);
            } else {
                request.getCallback().callback(new Response(NFCConstants.CODE_FUNCTION_NOT_SUPPORT, NFCConstants.DESC_FUNCTION_NOT_SUPPORT));
            }
        }
    }

    protected void stopDiscovery(Request request) {
        BaseInstance instance = getInstance(request);
        if (checkInstance(instance, request)) {
            if (instance instanceof NFCAdapterInstance) {
                ((NFCAdapterInstance) instance).stopDiscovery(request);
            } else {
                request.getCallback().callback(new Response(NFCConstants.CODE_FUNCTION_NOT_SUPPORT, NFCConstants.DESC_FUNCTION_NOT_SUPPORT));
            }
        }
    }

    protected void onDiscovered(Request request) {
        BaseInstance instance = getInstance(request);
        if (checkInstance(instance, request)) {
            if (instance instanceof NFCAdapterInstance) {
                ((NFCAdapterInstance) instance).onDiscovered(request);
            } else {
                request.getCallback().callback(new Response(NFCConstants.CODE_FUNCTION_NOT_SUPPORT, NFCConstants.DESC_FUNCTION_NOT_SUPPORT));
            }
        }
    }

    protected Response offDiscovered(Request request) {
        BaseInstance instance = getInstance(request);
        if (null != instance) {
            if (instance instanceof NFCAdapterInstance) {
                return ((NFCAdapterInstance) instance).offDiscovered(request);
            } else {
                return new Response(NFCConstants.CODE_FUNCTION_NOT_SUPPORT, NFCConstants.DESC_FUNCTION_NOT_SUPPORT);
            }
        } else {
            return new Response(NFCConstants.CODE_SERVICE_UNAVAILABLE, NFCConstants.DESC_SERVICE_UNAVAILABLE);
        }
    }

    protected Response getTechInstance(String action, Request request) {
        BaseInstance instance = getInstance(request);
        if (null != instance && instance instanceof NFCAdapterInstance) {
            return ((NFCAdapterInstance) instance).getTechInstance(action, request);
        } else {
            return null;
        }
    }

    protected BaseInstance getInstance(Request request) {
        return InstanceManager.getInstance().getInstance(request.getInstanceId());
    }

    protected boolean checkInstance(BaseInstance instance, Request request) {
        if (null == instance) {
            request.getCallback().callback(new Response(NFCConstants.CODE_SERVICE_UNAVAILABLE, NFCConstants.DESC_SERVICE_UNAVAILABLE));
            return false;
        } else if (!instance.isSupportNFC()) {
            Log.w(TAG, "unsupport nfc");
            request.getCallback().callback(new Response(NFCConstants.CODE_NOT_SUPPORT_NFC, NFCConstants.DESC_NOT_SUPPORT_NFC));
            return false;
        } else if (!instance.isEnabled()) {
            Log.w(TAG, "nfc is not enabled.");
            request.getCallback().callback(new Response(NFCConstants.CODE_NOT_OPEN_NFC, NFCConstants.DESC_NOT_OPEN_NFC));
            return false;
        }
        return true;
    }

    protected void close(Request request) {
        BaseInstance instance = getInstance(request);
        if (checkInstance(instance, request)) {
            if (instance instanceof BaseTechInstance) {
                ((BaseTechInstance) instance).close(request);
            } else {
                request.getCallback().callback(new Response(NFCConstants.CODE_FUNCTION_NOT_SUPPORT, NFCConstants.DESC_FUNCTION_NOT_SUPPORT));
            }
        }
    }

    protected void connect(Request request) {
        BaseInstance instance = getInstance(request);
        if (checkInstance(instance, request)) {
            if (instance instanceof BaseTechInstance) {
                ((BaseTechInstance) instance).connect(request);
            } else {
                request.getCallback().callback(new Response(NFCConstants.CODE_FUNCTION_NOT_SUPPORT, NFCConstants.DESC_FUNCTION_NOT_SUPPORT));
            }
        }
    }

    protected void isConnected(Request request) throws JSONException {
        BaseInstance instance = getInstance(request);
        if (checkInstance(instance, request)) {
            if (instance instanceof BaseTechInstance) {
                ((BaseTechInstance) instance).isConnected(request);
            } else {
                request.getCallback().callback(new Response(NFCConstants.CODE_FUNCTION_NOT_SUPPORT, NFCConstants.DESC_FUNCTION_NOT_SUPPORT));
            }
        }
    }

    protected void getMaxTransceiveLength(Request request) throws JSONException {
        BaseInstance instance = getInstance(request);
        if (checkInstance(instance, request)) {
            if (instance instanceof BaseTagTechInstance) {
                ((BaseTagTechInstance) instance).getMaxTransceiveLength(request);
            } else {
                request.getCallback().callback(new Response(NFCConstants.CODE_FUNCTION_NOT_SUPPORT, NFCConstants.DESC_FUNCTION_NOT_SUPPORT));
            }
        }
    }

    protected void setTimeout(Request request) throws JSONException {
        BaseInstance instance = getInstance(request);
        if (checkInstance(instance, request)) {
            if (instance instanceof BaseTagTechInstance
                    // NfcB and NfcV do not support setTimeout()
                    && !(instance instanceof NfcBInstance)
                    && !(instance instanceof NfcVInstance)) {
                ((BaseTagTechInstance) instance).setTimeout(request);
            } else {
                request.getCallback().callback(new Response(NFCConstants.CODE_FUNCTION_NOT_SUPPORT, NFCConstants.DESC_FUNCTION_NOT_SUPPORT));
            }
        }
    }

    protected void transceive(Request request) {
        BaseInstance instance = getInstance(request);
        if (checkInstance(instance, request)) {
            if (instance instanceof BaseTagTechInstance) {
                ((BaseTagTechInstance) instance).transceive(request);
            } else {
                request.getCallback().callback(new Response(NFCConstants.CODE_FUNCTION_NOT_SUPPORT, NFCConstants.DESC_FUNCTION_NOT_SUPPORT));
            }
        }
    }

    protected void getAtqa(Request request) {
        BaseInstance instance = getInstance(request);
        if (checkInstance(instance, request)) {
            if (instance instanceof NfcAInstance) {
                ((NfcAInstance) instance).getAtqa(request);
            } else {
                request.getCallback().callback(new Response(NFCConstants.CODE_FUNCTION_NOT_SUPPORT, NFCConstants.DESC_FUNCTION_NOT_SUPPORT));
            }
        }
    }

    protected void getSak(Request request) throws JSONException {
        BaseInstance instance = getInstance(request);
        if (checkInstance(instance, request)) {
            if (instance instanceof NfcAInstance) {
                ((NfcAInstance) instance).getSak(request);
            } else {
                request.getCallback().callback(new Response(NFCConstants.CODE_FUNCTION_NOT_SUPPORT, NFCConstants.DESC_FUNCTION_NOT_SUPPORT));
            }
        }
    }

    protected void writeNdefMessage(Request request) {
        BaseInstance instance = getInstance(request);
        if (checkInstance(instance, request)) {
            if (instance instanceof NdefInstance) {
                ((NdefInstance) instance).writeNdefMessage(request);
            } else {
                request.getCallback().callback(new Response(NFCConstants.CODE_FUNCTION_NOT_SUPPORT, NFCConstants.DESC_FUNCTION_NOT_SUPPORT));
            }
        }
    }

    protected void getHistoricalBytes(Request request) {
        BaseInstance instance = getInstance(request);
        if (checkInstance(instance, request)) {
            if (instance instanceof IsoDepInstance) {
                ((IsoDepInstance) instance).getHistoricalBytes(request);
            } else {
                request.getCallback().callback(new Response(NFCConstants.CODE_FUNCTION_NOT_SUPPORT, NFCConstants.DESC_FUNCTION_NOT_SUPPORT));
            }
        }
    }
}
