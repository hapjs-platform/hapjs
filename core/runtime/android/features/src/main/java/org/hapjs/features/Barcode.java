/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.bridge.NativeInterface;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.features.barcode.CaptureActivity;
import org.hapjs.features.barcode.Intents;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class implement FeatureExtension for web page to operate barcode
 *
 * <table>
 *     <tr>
 *         <th>Action name</th> <th>Action description</th> <th>Invocation mode</th>
 *         <th>Request parameter name</th> <th>Request parameter description</th>
 *         <th>Response parameter name</th> <th>Response parameter description</th>
 *     </tr>
 *
 *     <tr>
 *         <td>scan</td> <td>Scan barcode to retrieve info</td> <td>CALLBACK</td>
 *         <td></td> <td></td>
 *         <td>code</td> <td>error code if failed</td>
 *     </tr>
 *
 *     <tr><td></td> <td></td> <td></td> <td></td> <td></td> <td>result</td>
 *     <td>the text info in barcode</td></tr>
 * </table>
 *
 * @hide
 */
@FeatureExtensionAnnotation(
        name = Barcode.FEATURE_NAME,
        actions = {
                @ActionAnnotation(
                        name = Barcode.ACTION_SCAN,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {Manifest.permission.CAMERA})
        })
public class Barcode extends FeatureExtension {
    protected static final String FEATURE_NAME = "system.barcode";
    protected static final String ACTION_SCAN = "scan";
    protected static final String PARAM_SCAN_TYPE = "scanType";
    private static final String TAG = "Barcode";
    private static final String INTENT_EXTRA_TYPE = Intents.Scan.RESULT_TYPE;
    private static final String INTENT_EXTRA_RESULT = Intents.Scan.RESULT;
    private static final String KEY_TYPE = "type";
    private static final String KEY_RESULT = "result";

    private static final int REQUEST_CODE_BASE = getRequestBaseCode();
    private static final int REQUEST_SCAN_BARCODE = REQUEST_CODE_BASE + 1;

    @Override
    public Response invokeInner(final Request request) {
        final NativeInterface nativeInterface = request.getNativeInterface();
        Activity activity = nativeInterface.getActivity();
        Intent intent = new Intent();
        try {
            JSONObject params = request.getJSONParams();
            if (params != null) {
                intent.putExtra(PARAM_SCAN_TYPE, params.optInt(PARAM_SCAN_TYPE));
            }
        } catch (JSONException e) {
            Log.e(TAG, "get params fail", e);
        }
        intent.setClass(activity, CaptureActivity.class);

        LifecycleListener l =
                new LifecycleListener() {
                    @Override
                    public void onActivityResult(int requestCode, int resultCode, Intent data) {
                        if (requestCode == REQUEST_SCAN_BARCODE) {
                            nativeInterface.removeLifecycleListener(this);

                            Response response;
                            if (resultCode == Activity.RESULT_OK) {
                                response = new Response(makeResult(data));
                            } else if (resultCode == Activity.RESULT_CANCELED) {
                                response = Response.CANCEL;
                            } else {
                                response = Response.ERROR;
                            }

                            request.getCallback().callback(response);
                        }
                    }
                };
        nativeInterface.addLifecycleListener(l);

        activity.startActivityForResult(intent, REQUEST_SCAN_BARCODE);

        return Response.SUCCESS;
    }

    private JSONObject makeResult(Intent data) {
        if (data == null) {
            return null;
        }

        int type = data.getIntExtra(INTENT_EXTRA_TYPE, -1);
        String result = data.getStringExtra(INTENT_EXTRA_RESULT);

        JSONObject fullResult = new JSONObject();
        try {
            fullResult.put(KEY_TYPE, type);
            fullResult.put(KEY_RESULT, result);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return fullResult;
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }
}
