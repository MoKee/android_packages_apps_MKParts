/*
 * Copyright (C) 2014-2019 The MoKee Open Source Project
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

package org.mokee.mkparts.push;

import org.json.JSONException;
import org.json.JSONObject;
import org.mokee.mkparts.R;

import android.app.Notification;
import android.app.Notification.BigTextStyle;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.mokee.utils.MoKeeUtils;
import android.net.Uri;
import android.os.Bundle
import android.util.Log;
import cn.jpush.android.api.JPushInterface;

import mokee.providers.MKSettings;

import com.mokee.os.Build;

public class PushingMessageReceiver extends BroadcastReceiver {

    public static final String TAG = PushingMessageReceiver.class.getSimpleName();

    public static final String MKPUSH_ALIAS = "pref_alias";
    public static final String MKPUSH_TAGS = "pref_tags";

    public static final int MSG_SET_ALIAS = 1001;
    public static final int MSG_SET_TAGS = 1002;

    private static final String ACTION_UPDATE_CHECK = "com.mokee.center.action.UPDATE_CHECK";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        Bundle bundle = intent.getExtras();
        Log.d(TAG, "[MyReceiver] onReceive - " + intent.getAction() + ", extras: " + printBundle(bundle));
        if (JPushInterface.ACTION_MESSAGE_RECEIVED.equals(intent.getAction())) {
            String message = bundle.getString(JPushInterface.EXTRA_MESSAGE);
            String customContentString = bundle.getString(JPushInterface.EXTRA_EXTRA);
            onMessage(ctx, message, customContentString);
            JPushInterface.reportNotificationOpened(ctx, bundle.getString(JPushInterface.EXTRA_MSG_ID));
        }
    }

    public void onMessage(Context ctx, String message, String customContentString) {
        if (customContentString != null & customContentString != "") {
            JSONObject customJson = null;
            try {
                customJson = new JSONObject(customContentString);
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
            String device = PushingUtils.getStringFromJson("device", customJson);
            String type = PushingUtils.getStringFromJson("type", customJson);
            String url = PushingUtils.getStringFromJson("url", customJson);
            String title = PushingUtils.getStringFromJson("title", customJson);
            String clipboard = PushingUtils.getStringFromJson("clipboard", customJson);
            int msg_id = PushingUtils.getIntFromJson("id", customJson);
            String currentDevice = Build.PRODUCT.toLowerCase();
            String currentVersion = Build.VERSION.toLowerCase();

            if (PushingUtils.allowPush(device, currentDevice, 1) && PushingUtils.allowPush(type, currentVersion, 0)
                    || device.equals("all") && type.equals("all")
                    || device.equals("all") && PushingUtils.allowPush(type, currentVersion, 0)
                    || PushingUtils.allowPush(device, currentDevice, 1) && type.equals("all")) {
                switch (msg_id) {
                    default:
                        if (MoKeeUtils.isSupportLanguage(true)
                                && MKSettings.System.getInt(ctx.getContentResolver(), MKSettings.System.RECEIVE_PUSH_NOTIFICATIONS, 1) == 1) {
                            promptUser(ctx, url, title, message, msg_id, R.drawable.ic_push_notify);
                            if (!customJson.isNull(clipboard)) {
                                ClipboardManager clipboardManager = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                                clipboardManager.setPrimaryClip(ClipData.newPlainText(null, clipboard));
                            }
                        }
                        break;
                }
            }
        }
    }

    private void promptUser(Context context, String url, String title, String message, int id, int icon) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        PendingIntent pendintIntent = PendingIntent.getActivity(context, 0, intent, 0);

        BigTextStyle noti = new Notification.BigTextStyle(new Notification.Builder(context)
                .setSmallIcon(icon).setAutoCancel(true).setTicker(title)
                .setContentIntent(pendintIntent).setWhen(0).setContentTitle(title)
                .setColor(context.getColor(com.android.internal.R.color.system_notification_accent_color))
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS)
                .setContentText(message)).bigText(message);

        nm.notify(id, noti.build());
    }

    // 打印所有的 intent extra 数据
    private static String printBundle(Bundle bundle) {
        StringBuilder sb = new StringBuilder();
        for (String key : bundle.keySet()) {
            if (key.equals(JPushInterface.EXTRA_NOTIFICATION_ID)) {
                sb.append("\nkey:" + key + ", value:" + bundle.getInt(key));
            } else if (key.equals(JPushInterface.EXTRA_CONNECTION_CHANGE)) {
                sb.append("\nkey:" + key + ", value:" + bundle.getBoolean(key));
            } 
            else {
                sb.append("\nkey:" + key + ", value:" + bundle.getString(key));
            }
        }
        return sb.toString();
    }
}