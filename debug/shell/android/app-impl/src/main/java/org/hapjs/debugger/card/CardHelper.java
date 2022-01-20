/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.card;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.hapjs.card.api.debug.CardDebugHost;
import org.hapjs.card.sdk.CardClient;

public class CardHelper {

    private static final String TAG = "CardHelper";

    private static final String CARD_PROCESS_NAME = ":card";

    public static boolean isCardProcess(Context context, String process) {
        return getCardProcessName(context).equals(process);
    }

    private static String getCardProcessName(Context context) {
        return context.getPackageName() + CARD_PROCESS_NAME;
    }

    public static void initCard(final Context context) {
        //init card sdk
        CardClient.initAsync(context, new CardClient.InitCallback() {
            @Override
            public void onInitSuccess(CardClient client) {
                CardClient.getInstance().setCardDebugHost(new CardDebugHost() {
                    @Override
                    public boolean launch(Context context, String url) {
                        Intent intent = new Intent(context, CardActivity.class);
                        intent.putExtra(CardActivity.EXTRA_CARD_URL, url);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                        return true;
                    }

                    @Override
                    public String getArchiveHost() {
                        return null;
                    }

                    @Override
                    public String getRuntimeHost() {
                        return null;
                    }
                });
            }

            @Override
            public void onInitFail() {
                Log.e(TAG, "The runtime framework not support card!");
            }
        });
    }
}
