/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.util.ArrayMap;
import java.lang.ref.WeakReference;
import java.util.Map;
import org.hapjs.component.constants.Attributes;

/**
 * 动画监听器工厂类，保存多个监听器的公有信息并产生监听器实例 实例由中转监听器和动画总时长构造
 */
public class AnimatorListenerBridge {
    public static final String ATTR_ANIMATION_NAME = "animationName";
    public static final String ATTR_ELAPSED_TIME = "elapsedTime";
    private AnimatorEventListener mAnimatorEventListener;

    public AnimatorListenerBridge(AnimatorEventListener animatorEventListener) {
        mAnimatorEventListener = animatorEventListener;
    }

    public AnimatorListenerAdapter createAnimatorListener(final String animatorName) {
        return new AnimatorListener(animatorName, mAnimatorEventListener);
    }

    /**
     * 前端和Native端传播动画事件的监听器，仅起到中继作用
     */
    public interface AnimatorEventListener {

        /**
         * 将事件联同参数传递到前端处理
         *
         * @param animator   动画
         * @param eventName  事件名
         * @param params     事件参数，一般可以在前端通过 e.args 直接获取
         * @param attributes 事件属性，前端不可以直接获取到
         */
        void onAnimatorEvent(
                Animator animator,
                String eventName,
                Map<String, Object> params,
                Map<String, Object> attributes);

        /**
         * 仅对在监听事件集合中注册的事件进行中转
         *
         * @param eventName 事件名，包括 animationStart 等
         * @return <tt>true</tt>，如果监听事件集合此时并不包含该事件
         */
        boolean registerEvent(String eventName);

        /**
         * 取消注册监听某单个事件
         *
         * @param eventName 事件名，包括 animationStart 等
         * @return <tt>true</tt>，如果监听事件集合包含该事件
         */
        boolean unregisterEvent(String eventName);

        /**
         * 取消注册监听的所有事件
         */
        void unregisterAllEvents();
    }

    private static class AnimatorListener extends AnimatorListenerAdapter {
        private int mRepeatCount = 0;
        private boolean mIsCancelled = false;
        private long mStartTime = 0;
        private Map<String, Object> mEventAttributes = new ArrayMap<>();
        private WeakReference<AnimatorEventListener> mWeakReference;

        public AnimatorListener(String animatorName, AnimatorEventListener listener) {
            mEventAttributes.put(ATTR_ANIMATION_NAME, animatorName);
            mWeakReference = new WeakReference<>(listener);
        }

        @Override
        public void onAnimationStart(Animator animation) {
            super.onAnimationStart(animation);
            mStartTime = System.currentTimeMillis();
            AnimatorEventListener listener = mWeakReference.get();
            if (listener != null) {
                mEventAttributes.put(ATTR_ELAPSED_TIME, 0f);
                listener.onAnimatorEvent(
                        animation, Attributes.Event.ANIMATION_START, mEventAttributes, null);
            }
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            AnimatorEventListener listener = mWeakReference.get();
            if (listener != null) {
                float endTime =
                        mIsCancelled
                                ? (System.currentTimeMillis() - mStartTime) / 1000f
                                : animation.getDuration() * (mRepeatCount + 1) / 1000f;
                mEventAttributes.put(ATTR_ELAPSED_TIME, endTime);
                listener.onAnimatorEvent(animation, Attributes.Event.ANIMATION_END,
                        mEventAttributes, null);
            }
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
            super.onAnimationRepeat(animation);
            AnimatorEventListener listener = mWeakReference.get();
            mRepeatCount += 1;
            if (listener != null) {
                float repeatTime = animation.getDuration() * mRepeatCount / 1000f;
                mEventAttributes.put(ATTR_ELAPSED_TIME, repeatTime);
                listener.onAnimatorEvent(
                        animation, Attributes.Event.ANIMATION_ITERATION, mEventAttributes, null);
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            super.onAnimationCancel(animation);
            mIsCancelled = true;
        }
    }
}
