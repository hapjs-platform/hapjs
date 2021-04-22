/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.bridge.NativeInterface;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.bridge.permission.RuntimePermissionProvider;
import org.hapjs.common.executors.AbsTask;
import org.hapjs.common.executors.Executors;
import org.hapjs.runtime.ProviderManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class implement FeatureExtension for getting Contact data.
 *
 * <table>
 *     <tr>
 *         <th>Action name</th> <th>Action description</th> <th>Invocation mode</th>
 *         <th>Request param name</th> <th>Request param description</th>
 *         <th>Response param name</th> <th>Response param description</th>
 *     </tr>
 *     <tr>
 *         <td>pick</td> <td>show contact list and return the contact user picked</td> <td>ASYNC</td>
 *         <td>N/A</td> <td>N/A</td>
 *         <td>displayName</td> <td>The display name for the contact.</td>
 *     </tr>
 *     <tr>
 *         <td></td> <td></td> <td></td>
 *         <td></td> <td></td>
 *         <td>number</td> <td>the number for the contact</td>
 *     </tr>
 *
 * </table>
 */
@FeatureExtensionAnnotation(
        name = Contact.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = Contact.ACTION_PICK, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = Contact.ACTION_LIST,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {Manifest.permission.READ_CONTACTS})
        })
public class Contact extends FeatureExtension {
    protected static final String FEATURE_NAME = "system.contact";
    protected static final String ACTION_PICK = "pick";
    protected static final String ACTION_LIST = "list";
    private static final String TAG = "ContactFeature";
    private static final int REQUEST_CODE_BASE = getRequestBaseCode();
    private static final int REQUEST_CODE_PICK = REQUEST_CODE_BASE + 1;

    private static final String KEY_DISPLAY_NAME = "displayName";
    private static final String KEY_NUMBER = "number";

    private static final String[] PROJECTION_PHONE = {
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
    };
    private static final int COLUMN_INDEX_DISPLAY_NAME = 0;
    private static final int COLUMN_INDEX_NUMBER = 1;
    private static final String RESULT_CONTACT_LIST = "contactList";
    private static Boolean mIsTaskFinish = true; // 从数据库获取联系人列表的异步任务是否完成
    private static final Uri COMTACT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;

    private static JSONObject getContactFromPicker(
            ContentResolver resolver, Uri uri, Request request) {
        Cursor cursor = resolver.query(uri, PROJECTION_PHONE, null, null, null);

        JSONObject result = new JSONObject();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    result.put(KEY_DISPLAY_NAME, cursor.getString(COLUMN_INDEX_DISPLAY_NAME));
                    result.put(KEY_NUMBER,
                            normalizePhoneNumber(cursor.getString(COLUMN_INDEX_NUMBER)));
                }
            } catch (JSONException e) {
                Log.e(TAG, "parse contact error", e);
                request.getCallback().callback(Response.ERROR);
            } finally {
                cursor.close();
            }
        }

        return result;
    }

    private static JSONObject getContactList(ContentResolver resolver, Uri uri, Request request) {
        Cursor cursor = resolver.query(uri, PROJECTION_PHONE, null, null, null);
        JSONObject result = new JSONObject();
        if (cursor != null) {
            JSONArray jsonArray = new JSONArray();
            try {
                while (cursor.moveToNext()) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(KEY_DISPLAY_NAME, cursor.getString(COLUMN_INDEX_DISPLAY_NAME));
                    jsonObject.put(KEY_NUMBER,
                            normalizePhoneNumber(cursor.getString(COLUMN_INDEX_NUMBER)));
                    jsonArray.put(jsonObject);
                }
                result.put(RESULT_CONTACT_LIST, jsonArray);
            } catch (JSONException e) {
                Log.e(TAG, "parse contact error", e);
                request.getCallback().callback(Response.ERROR);
            } finally {
                cursor.close();
            }
        }
        return result;
    }

    /**
     * Normalize a phone number by removing the characters other than digits. If the given number has
     * keypad letters, the letters will be converted to digits first.
     *
     * @param phoneNumber the number to be normalized.
     * @return the normalized number.
     */
    private static String normalizePhoneNumber(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int len = phoneNumber.length();
        for (int i = 0; i < len; i++) {
            char c = phoneNumber.charAt(i);
            // Character.digit() supports ASCII and Unicode digits (fullwidth, Arabic-Indic, etc.)
            int digit = Character.digit(c, 10);
            if (digit != -1) {
                sb.append(digit);
            } else if (sb.length() == 0 && c == '+') {
                sb.append(c);
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                return normalizePhoneNumber(
                        android.telephony.PhoneNumberUtils
                                .convertKeypadLettersToDigits(phoneNumber));
            }
        }
        return sb.toString();
    }

    @Override
    public Response invokeInner(Request request) {
        if (TextUtils.equals(request.getAction(), ACTION_PICK)) {
            pickContact(request);
        } else if (TextUtils.equals(request.getAction(), ACTION_LIST)) {
            listContact(request);
        }
        return null;
    }

    private void pickContact(final Request request) {
        final NativeInterface nativeInterface = request.getNativeInterface();
        Activity activity = nativeInterface.getActivity();

        LifecycleListener l =
                new LifecycleListener() {
                    @Override
                    public void onActivityResult(int requestCode, int resultCode, Intent data) {
                        nativeInterface.removeLifecycleListener(this);

                        if (requestCode == REQUEST_CODE_PICK) {
                            if (resultCode == Activity.RESULT_OK) {
                                final ContentResolver resolver =
                                        nativeInterface.getActivity().getContentResolver();
                                Executors.io()
                                        .execute(
                                                new AbsTask<JSONObject>() {
                                                    @Override
                                                    protected JSONObject doInBackground() {
                                                        return getContactFromPicker(resolver,
                                                                data.getData(), request);
                                                    }

                                                    @Override
                                                    protected void onPostExecute(
                                                            JSONObject result) {
                                                        request.getCallback()
                                                                .callback(new Response(result));
                                                    }
                                                });
                            } else if (resultCode == Activity.RESULT_CANCELED) {
                                request.getCallback().callback(Response.CANCEL);
                            } else {
                                request.getCallback().callback(Response.ERROR);
                            }
                        }
                    }
                };
        nativeInterface.addLifecycleListener(l);

        Intent intent =
                new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        activity.startActivityForResult(intent, REQUEST_CODE_PICK);
    }

    private void listContact(final Request request) {
        final Activity activity = request.getNativeInterface().getActivity();
        String name = request.getApplicationContext().getName();
        final String message = activity.getString(R.string.get_contactlist_tip, name);
        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mIsTaskFinish) {
                            showPermissionDialog(activity, message, request);
                        } else {
                            Response response =
                                    new Response(
                                            Response.CODE_TOO_MANY_REQUEST,
                                            "Please wait last request finished.");
                            request.getCallback().callback(response);
                        }
                    }
                });
    }

    private void showPermissionDialog(Activity activity, String message, final Request request) {
        mIsTaskFinish = false;
        RuntimePermissionProvider mProvider =
                ProviderManager.getDefault().getProvider(RuntimePermissionProvider.NAME);
        Dialog dialog =
                mProvider.createPermissionDialog(
                        activity,
                        null,
                        request.getApplicationContext().getName(),
                        message,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == DialogInterface.BUTTON_POSITIVE) {
                                    getListFromContentProvider(request);
                                } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                                    mIsTaskFinish = true;
                                    request.getCallback().callback(Response.getUserDeniedResponse(false));
                                }
                            }
                        },
                        false);
        dialog.show();
    }

    private void getListFromContentProvider(final Request request) {
        final ContentResolver resolver =
                request.getNativeInterface().getActivity().getContentResolver();
        Executors.io()
                .execute(
                        new AbsTask<JSONObject>() {
                            @Override
                            protected JSONObject doInBackground() {
                                return getContactList(resolver, COMTACT_URI, request);
                            }

                            @Override
                            protected void onPostExecute(JSONObject result) {
                                mIsTaskFinish = true;
                                request.getCallback().callback(new Response(result));
                            }
                        });
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }
}
