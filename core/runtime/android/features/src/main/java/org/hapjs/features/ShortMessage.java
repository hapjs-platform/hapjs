/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.bridge.permission.HapCustomPermissions;
import org.hapjs.bridge.permission.PermissionCallbackAdapter;
import org.hapjs.bridge.permission.SystemPermissionManager;
import org.hapjs.cache.CacheStorage;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = ShortMessage.FEATURE_NAME,
        actions = {
                @ActionAnnotation(
                        name = ShortMessage.ACTION_SEND,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {Manifest.permission.SEND_SMS}),
                @ActionAnnotation(name = ShortMessage.ACTION_READ_SAFELY, mode = FeatureExtension.Mode.ASYNC),
        })
public class ShortMessage extends FeatureExtension {
    public static final int NUM_HASHED_BYTES = 9;
    public static final int NUM_BASE64_CHAR = 11;
    protected static final String FEATURE_NAME = "system.sms";
    protected static final String ACTION_SEND = "send";
    protected static final String ACTION_READ_SAFELY = "readSafely";
    protected static final String PARAM_KEY_ADDRESS = "address";
    protected static final String PARAM_KEY_CONTENT = "content";
    protected static final String PARAM_TIMEOUT = "timeout";
    protected static final String RESULT_MESSAGE = "message";
    private static final String TAG = "ShortMessage";
    private static final String SENT_SMS_ACTION = "org.hapjs.intent.action.SEND_SMS";
    private static final int MESSAGE_MAX_LENGTH = 70;
    private static final String BROADCAST_TAG = "tag";
    private static final String BROADCAST_QUICKAPP_NAME = "quickAppName";
    private static final long DEFAULT_TIMEOUT = 60 * 1000L;
    private SendResultCallBack mCallBack;
    private int mIndex = 0;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    protected Response invokeInner(final Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_SEND.equals(action)) {
            send(request);
        } else if (ACTION_READ_SAFELY.equals(action)) {
            readSafely(request);
        }
        return null;
    }

    @Override
    public PermissionPromptStrategy getPermissionPromptStrategy(Request request) {
        String action = request.getAction();
        if (ACTION_SEND.equals(action)) {
            return PermissionPromptStrategy.EVERY_TIME;
        }
        return super.getPermissionPromptStrategy(request);
    }

    private void send(final Request request) {
        JSONObject params;
        try {
            params = new JSONObject(request.getRawParams());
        } catch (JSONException e) {
            request.getCallback().callback(Response.ERROR);
            return;
        }
        String address = params.optString(PARAM_KEY_ADDRESS);
        String content = params.optString(PARAM_KEY_CONTENT);
        if (TextUtils.isEmpty(address) || TextUtils.isEmpty(content)) {
            Response response =
                    new Response(Response.CODE_ILLEGAL_ARGUMENT, "address or content is null");
            request.getCallback().callback(response);
            return;
        }
        if (content.length() > MESSAGE_MAX_LENGTH) {
            Log.e(TAG, "The length of message is longer than 70");
            request
                    .getCallback()
                    .callback(
                            new Response(
                                    Response.CODE_ILLEGAL_ARGUMENT,
                                    "The length of message is longer than 70"));
            return;
        }

        sendSMS(request);
    }

    protected void readSafely(Request request) throws JSONException {
        JSONObject params = request.getJSONParams();
        long timeout =
                params != null ? params.optLong(PARAM_TIMEOUT, DEFAULT_TIMEOUT) : DEFAULT_TIMEOUT;
        if (timeout <= 0) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "timeout must >0"));
            return;
        }
        String appHash =
                getAppHash(
                        request.getNativeInterface().getActivity(),
                        request.getApplicationContext().getPackage());
        if (TextUtils.isEmpty(appHash)) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_GENERIC_ERROR, "Fail to get app hash"));
            return;
        }
        final SMSReader smsReader = new SMSReader(request, timeout, appHash);
        SystemPermissionManager.getDefault()
                .requestPermissions(
                        request.getView().getHybridManager(),
                        new String[] {Manifest.permission.READ_SMS,
                                Manifest.permission.RECEIVE_SMS},
                        new PermissionCallbackAdapter() {
                            @Override
                            public void onPermissionAccept(
                                    HybridManager hybridManager, String[] grantedPermissions,
                                    boolean userGranted) {
                                smsReader.read();
                            }

                            @Override
                            public void onPermissionReject(
                                    HybridManager hybridManager, String[] grantedPermissions) {
                                if (grantedPermissions != null
                                        && grantedPermissions.length > 0
                                        && Manifest.permission.READ_SMS
                                        .equals(grantedPermissions[0])) {
                                    smsReader.read();
                                }
                            }
                        },
                        PermissionPromptStrategy.EVERY_TIME);
    }

    private String getAppHash(Context context, String pkg) {
        String signature = CacheStorage.getInstance(context).getPackageSign(pkg);
        if (signature == null) {
            return null;
        }
        try {
            byte[] bytes = Base64.decode(signature, Base64.DEFAULT);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update((pkg + " ").getBytes(Charset.forName("UTF-8")));
            md.update(bytes);
            String base64Hash =
                    Base64.encodeToString(md.digest(), Base64.NO_PADDING | Base64.NO_WRAP);
            base64Hash = base64Hash.substring(0, NUM_BASE64_CHAR);
            return base64Hash;
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "getAppHash", e);
        }
        return null;
    }

    private void sendSMS(final Request request) {
        JSONObject params;
        try {
            params = new JSONObject(request.getRawParams());
        } catch (JSONException e) {
            request.getCallback().callback(Response.ERROR);
            return;
        }
        String address = params.optString(PARAM_KEY_ADDRESS);
        String content = params.optString(PARAM_KEY_CONTENT);

        Activity activity = request.getNativeInterface().getActivity();
        String name = request.getApplicationContext().getName();

        SmsManager smsManager = SmsManager.getDefault();

        Intent sentIntent = new Intent(SENT_SMS_ACTION);
        sentIntent.putExtra(BROADCAST_TAG, mIndex);
        sentIntent.putExtra(BROADCAST_QUICKAPP_NAME, name);
        sentIntent.setPackage(activity.getPackageName());
        PendingIntent sendPI =
                PendingIntent
                        .getBroadcast(activity, 0, sentIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (mCallBack == null) {
            mCallBack = new SendResultCallBack(activity);
        }

        try {
            smsManager.sendTextMessage(address, null, content, sendPI, null);
            mCallBack.addRequest(mIndex, request);
            mIndex++;
        } catch (Exception e) {
            Log.w(TAG, "exception while sending", e);
            request.getCallback().callback(Response.ERROR);
        }
    }

    @Override
    public void dispose(boolean force) {
        super.dispose(force);
        if (force) {
            if (mCallBack != null) {
                mCallBack.unregisterReceiver();
                mCallBack = null;
            }
        }
    }

    private static class SendResultCallBack {
        private BroadcastReceiver mSMSReceiver;
        private Activity mActivity;
        private Map<Integer, Request> mRequestMap = new HashMap<>();

        public SendResultCallBack(Activity activity) {
            mActivity = activity;
            mSMSReceiver =
                    new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            int tag = intent.getIntExtra(BROADCAST_TAG, -1);
                            Request request = mRequestMap.get(tag);
                            if (request != null) {
                                String name = intent.getStringExtra(BROADCAST_QUICKAPP_NAME);
                                if (!TextUtils.isEmpty(name)
                                        && name.equals(request.getApplicationContext().getName())) {
                                    mRequestMap.remove(tag);
                                    int code = getResultCode();
                                    if (code == Activity.RESULT_OK) {
                                        request.getCallback().callback(Response.SUCCESS);
                                    } else {
                                        request.getCallback().callback(Response.ERROR);
                                    }
                                }
                            }
                        }
                    };
            activity
                    .getApplicationContext()
                    .registerReceiver(
                            mSMSReceiver,
                            new IntentFilter(SENT_SMS_ACTION),
                            HapCustomPermissions.getHapPermissionReceiveBroadcast(activity),
                            null);
        }

        private void addRequest(int index, Request request) {
            mRequestMap.put(index, request);
        }

        private void unregisterReceiver() {
            mActivity.getApplicationContext().unregisterReceiver(mSMSReceiver);
        }
    }

    private static class SMSReceiver extends BroadcastReceiver {
        static final String SMS_RECEIVED_ACTION =
                android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION;
        private static final String FORMAT = "format";
        private static final String PDUS = "pdus";

        private SMSReader mSmsReader;

        public SMSReceiver(SMSReader smsReader) {
            this.mSmsReader = smsReader;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!SMS_RECEIVED_ACTION.equals(intent.getAction())) {
                return;
            }
            Bundle bundle = intent.getExtras();
            if (bundle == null) {
                Log.w(TAG, "onReceive: bundle is null");
                return;
            }
            Object[] pdus = (Object[]) bundle.get(PDUS);
            if (pdus != null) {
                String format = bundle.getString(FORMAT);
                SmsMessage[] msgs = new SmsMessage[pdus.length];
                for (int i = 0; i < msgs.length; i++) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                    } else {
                        msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    }
                    SmsMessage msg = msgs[i];
                    String message = msg.getMessageBody();
                    if (mSmsReader.verifyMessage(message)) {
                        mSmsReader.callbackSuccess(message);
                        abortBroadcast();
                        break;
                    }
                }
            }
        }
    }

    protected class SMSReader {
        private static final long MAX_TIME_READ = 5 * 60 * 1000L;
        private static final String URI_SMS_INBOX = "content://sms/inbox";
        private static final String COLUMN_BODY = "body";
        private static final String COLUMN_DATE = "date";

        private final Request mRequest;
        private final String mAppHash;
        private final long mTimeout;
        private final SMSReceiver mSmsReceiver;
        private final LifecycleListener mLifecycleListener;
        private final Runnable mTimeoutCallback =
                new Runnable() {
                    @Override
                    public void run() {
                        callback(new Response(Response.CODE_TIMEOUT, "timeout"));
                    }
                };

        public SMSReader(Request request, long timeout, String appHash) {
            this.mRequest = request;
            this.mTimeout = timeout;
            this.mAppHash = appHash;
            this.mSmsReceiver = new SMSReceiver(this);
            this.mLifecycleListener =
                    new LifecycleListener() {
                        @Override
                        public void onDestroy() {
                            cancel();
                            super.onDestroy();
                        }
                    };
            registerSMSReceiver(request.getNativeInterface().getActivity());
            request.getNativeInterface().addLifecycleListener(mLifecycleListener);
        }

        public void read() {
            mHandler.removeCallbacks(mTimeoutCallback);
            mHandler.postDelayed(mTimeoutCallback, mTimeout);
            readSMS(mRequest.getNativeInterface().getActivity());
        }

        void cancel() {
            mHandler.removeCallbacks(mTimeoutCallback);
            mRequest.getNativeInterface().removeLifecycleListener(mLifecycleListener);
            unregisterSMSReceiver(mRequest.getNativeInterface().getActivity());
        }

        private void readSMS(Context context) {
            Uri uri = Uri.parse(URI_SMS_INBOX);
            String[] projection = new String[] {COLUMN_BODY};
            String selection = COLUMN_BODY + " LIKE ? AND " + COLUMN_DATE + ">?";
            long startTime = System.currentTimeMillis() - MAX_TIME_READ;
            String[] selectionArgs = new String[] {"%" + mAppHash, String.valueOf(startTime)};
            String sortOrder = COLUMN_DATE + " DESC";
            Cursor cursor = null;
            try {
                cursor =
                        context
                                .getContentResolver()
                                .query(uri, projection, selection, selectionArgs, sortOrder);
                if (cursor != null) {
                    if (cursor.moveToNext()) {
                        String body = cursor.getString(0);
                        callbackSuccess(body);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Fail to read SMS", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        private void registerSMSReceiver(Context context) {
            IntentFilter filter = new IntentFilter();
            filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
            filter.addAction(SMSReceiver.SMS_RECEIVED_ACTION);
            context.getApplicationContext().registerReceiver(mSmsReceiver, filter);
        }

        private void unregisterSMSReceiver(Context context) {
            context.getApplicationContext().unregisterReceiver(mSmsReceiver);
        }

        void callback(Response response) {
            mRequest.getCallback().callback(response);
            cancel();
        }

        void callbackSuccess(String message) {
            JSONObject json = new JSONObject();
            try {
                json.put(RESULT_MESSAGE, message);
                callback(new Response(json));
            } catch (JSONException e) {
                callback(getExceptionResponse(mRequest, e));
            }
        }

        boolean verifyMessage(String message) {
            if (TextUtils.isEmpty(message)) {
                return false;
            }
            return message.contains(mAppHash);
        }
    }
}
