/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import java.io.File;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.Callback;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.bridge.NativeInterface;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.common.utils.MediaUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class implement FeatureExtension for sharing data with other applications. currently {@link
 * Intent#ACTION_SEND} is used to share the data, and the data should be encapsulated as a JSON
 * string in {@link Request}.
 *
 * <table>
 *     <tr>
 *         <th>Action name</th> <th>Action description</th> <th>Invocation mode</th>
 *         <th>Request parameter name</th> <th>Request parameter description</th>
 *         <th>Response parameter name</th> <th>Response parameter description</th>
 *     </tr>
 *
 *     <tr>
 *         <td>send</td> <td>share data with {@link Intent#ACTION_SEND}</td> <td>CALLBACK</td>
 *         <td>type</td> <td>the type of content be to shared</td>
 *         <td>code</td> <td>error code if failed</td>
 *     </tr>
 *
 *     <tr>
 *         <td></td> <td></td> <td></td> <td>data</td> <td>the content to be shared</td> <td>message</td>
 *     <td>error message if failed</td></tr>
 * </table>
 *
 * @hide
 */
@FeatureExtensionAnnotation(
        name = Share.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = Share.ACTION_SHARE, mode = FeatureExtension.Mode.ASYNC)})
public class Share extends FeatureExtension {

    protected static final String FEATURE_NAME = "system.share";
    /**
     * Action supported by this feature.
     */
    protected static final String ACTION_SHARE = "share";
    /**
     * The name in JSON string to specify the type of data to be shared.
     */
    protected static final String PARAM_TYPE = "type";
    /**
     * The name in JSON string to specify the data to be shared.
     */
    protected static final String PARAM_DATA = "data";
    private static final String TAG = "HybridShare";
    private static final int REQUEST_CODE_BASE = getRequestBaseCode();
    private static final int REQUEST_SHARE = REQUEST_CODE_BASE + 1;

    /**
     * Invoke the share action.
     *
     * @param request invocation request with action of send, and the rawParams in the request should
     *                be populated with a JSON string to specify the type and data as described in {@link
     *                Intent#ACTION_SEND}.
     * @return invocation response. null if success or error message if failed.
     */
    @Override
    public Response invokeInner(Request request) throws JSONException {
        share(request);
        return null;
    }

    private void share(final Request request) throws JSONException {
        final NativeInterface nativeInterface = request.getNativeInterface();
        Activity activity = nativeInterface.getActivity();
        final Callback cb = request.getCallback();
        Intent intent = getShareIntent(request);
        if (intent == null) {
            cb.callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "invalid intent"));
            return;
        }

        try {
            activity.startActivityForResult(intent, REQUEST_SHARE);
            LifecycleListener l =
                    new LifecycleListener() {
                        @Override
                        public void onActivityResult(int requestCode, int resultCode, Intent data) {
                            if (REQUEST_SHARE == requestCode) {
                                nativeInterface.removeLifecycleListener(this);
                                Response response;
                                if (resultCode == Activity.RESULT_OK) {
                                    response = Response.SUCCESS;
                                } else if (resultCode == Activity.RESULT_CANCELED) {
                                    response = Response.CANCEL;
                                } else {
                                    response = Response.ERROR;
                                }
                                cb.callback(response);
                            }
                        }
                    };
            nativeInterface.addLifecycleListener(l);
        } catch (ActivityNotFoundException e) {
            Response response = getExceptionResponse(request, e);
            cb.callback(response);
        }
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    private Intent getShareIntent(Request request) throws JSONException {
        String rawParams = request.getRawParams();
        JSONObject params = new JSONObject(rawParams);
        String type = params.getString(PARAM_TYPE);
        String data = params.getString(PARAM_DATA);

        Intent intent = new Intent();
        intent.setType(type);
        intent.setAction(Intent.ACTION_SEND);
        if (type.startsWith("text/")) {
            intent.putExtra(Intent.EXTRA_TEXT, data);
            if ("text/html".equals(type)) {
                intent.putExtra(Intent.EXTRA_HTML_TEXT, data);
            }
        } else {
            File file = request.getApplicationContext().getUnderlyingFile(data);
            Uri uri;
            if (file != null) {
                ApplicationContext appContext = request.getApplicationContext();
                uri =
                        MediaUtils.getMediaContentUri(
                                appContext.getContext(), appContext.getPackage(), type,
                                Uri.fromFile(file));
            } else {
                uri = request.getApplicationContext().getUnderlyingUri(data);
            }
            if (uri == null) {
                return null;
            }
            intent.putExtra(Intent.EXTRA_STREAM, uri);
        }
        String title = request.getNativeInterface().getActivity().getString(R.string.share_title);
        return Intent.createChooser(intent, title);
    }
}
