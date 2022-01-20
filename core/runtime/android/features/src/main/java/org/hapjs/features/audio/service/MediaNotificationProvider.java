/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.audio.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.widget.RemoteViews;

/*
 *供各厂商定制实现自己的音频播放通知栏
 */
public class MediaNotificationProvider {
    public static final String NAME = "media_notification";

    /*
     *播放状态发生变化需要更新通知栏界面时调用，用于传递记录定制通知栏UI需要的组件
     */
    public void initNotification(
            NotificationManager notificationManager,
            String pkg,
            AudioService service,
            MediaControllerCompat.TransportControls transportControls,
            RemoteViews remoteViews,
            int requestCode,
            PendingIntent stopIntent,
            PendingIntent pauseIntent,
            PendingIntent playIntent,
            PendingIntent previousIntent,
            PendingIntent nextIntent,
            String title,
            String artist,
            String cover) {
    }

    /*
     * 用户删除通知时是否停止播放，默认停止播放
     */
    public boolean isStopWhenRemoveNotification() {
        return true;
    }

    /*
     *创建通知，使用initNotification时传递的信息构建通知
     */
    public Notification buildNotification() {
        return null;
    }

    /*
     *定制通知显示UI时调用，使用initNotification时传递的信息定制显示界面
     * 备注：默认滑动删除，mStopIntent是关闭通知时响应，如果添加关闭按钮，可将mStopIntent作为关闭响应intent
     */
    public boolean configView(PlaybackStateCompat playbackState) {
        return false;
    }

    /*
     *显示通知时调用
     */
    public boolean show(Notification notification) {
        return false;
    }

    /*
     *通知隐藏时调用
     */
    public boolean hidden() {
        return false;
    }
}
