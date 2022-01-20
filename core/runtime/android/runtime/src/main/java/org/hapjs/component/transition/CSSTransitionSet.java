/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.transition;

import android.os.Build;
import android.text.TextUtils;
import android.transition.ChangeTransform;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Interpolator;
import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hapjs.component.animation.AnimationParser;
import org.hapjs.component.animation.EaseInterpolator;
import org.hapjs.component.animation.TimingFactory;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.transition.utils.TransitionUtils;

public class CSSTransitionSet extends TransitionSet {
    private static final String TAG = "TransitionSet";

    private Map<Integer, Parameter> mTargetParameter;
    private Map<Integer, Map<String, Transition>> mTargetTransition;
    private Map<Integer, Map<String, Transition>> mDelayedTransition;
    private boolean isPreDrawListenerAdded = false;

    public CSSTransitionSet() {
        mTargetParameter = new ArrayMap<>();
    }

    public boolean setProperties(View target, String propertyStr) {
        if (target == null) {
            return false;
        }
        Parameter parameter = getParameter(target);
        if (TextUtils.equals(parameter.mProperties, propertyStr)) {
            return false;
        } else if (parameter.isPropertiesParsed) {
            removeTransition(target);
        }

        parameter.mProperties = propertyStr;
        parameter.isPropertiesParsed = false;
        return true;
    }

    public void setDurations(View target, String durationStr) {
        if (target == null) {
            return;
        }
        Parameter parameter = getParameter(target);
        if (TextUtils.equals(parameter.mDurations, durationStr)) {
            return;
        }
        parameter.mDurations = durationStr;
    }

    public void setDelays(View target, String delayStr) {
        if (target == null) {
            return;
        }
        Parameter parameter = getParameter(target);
        if (TextUtils.equals(parameter.mDelays, delayStr)) {
            return;
        }
        parameter.mDelays = delayStr;
    }

    public void setTimingFunctions(View target, String timingFuncStr) {
        if (target == null) {
            return;
        }
        Parameter parameter = getParameter(target);
        if (TextUtils.equals(parameter.mTimingFunctions, timingFuncStr)) {
            return;
        }
        parameter.mTimingFunctions = timingFuncStr;
    }

    public boolean isTransitionProperty(View target, String property) {
        if (mTargetParameter.isEmpty() || target == null) {
            return false;
        }
        // 为适应子简写属性和其子属性的匹配，如 paddingLeft 和 borderLeftColor 等
        property = property.replaceAll("(Left|Top|Right|Bottom)", "");

        Parameter parameter = getParameter(target);
        return parameter.mProperties != null
                && (parameter.mProperties.contains(property)
                || parameter.mProperties.contains(Attributes.Style.ALL)
                || parameter.mProperties.contains(Attributes.Style.NONE));
    }

    public void prepareProperty(String property, View target, ViewGroup sceneRootView) {
        if (property == null || target == null || sceneRootView == null) {
            return;
        }
        switch (property) {
            case Attributes.Style.WIDTH:
            case Attributes.Style.HEIGHT:
            case Attributes.Style.BACKGROUND_COLOR:
            case Attributes.Style.BACKGROUND:
            case Attributes.Style.OPACITY:
            case Attributes.Style.VISIBILITY:
            case Attributes.Style.TRANSFORM:
            case Attributes.Style.TRANSFORM_ORIGIN:
            case Attributes.Style.PADDING:
            case Attributes.Style.PADDING_LEFT:
            case Attributes.Style.PADDING_TOP:
            case Attributes.Style.PADDING_RIGHT:
            case Attributes.Style.PADDING_BOTTOM:
            case Attributes.Style.BORDER_WIDTH:
            case Attributes.Style.BORDER_LEFT_WIDTH:
            case Attributes.Style.BORDER_TOP_WIDTH:
            case Attributes.Style.BORDER_RIGHT_WIDTH:
            case Attributes.Style.BORDER_BOTTOM_WIDTH:
            case Attributes.Style.BORDER_COLOR:
            case Attributes.Style.BORDER_LEFT_COLOR:
            case Attributes.Style.BORDER_TOP_COLOR:
            case Attributes.Style.BORDER_RIGHT_COLOR:
            case Attributes.Style.BORDER_BOTTOM_COLOR:
            case Attributes.Style.MARGIN:
            case Attributes.Style.MARGIN_LEFT:
            case Attributes.Style.MARGIN_TOP:
            case Attributes.Style.MARGIN_RIGHT:
            case Attributes.Style.MARGIN_BOTTOM:
            case Attributes.Style.LEFT:
            case Attributes.Style.TOP:
            case Attributes.Style.RIGHT:
            case Attributes.Style.BOTTOM:
            case Attributes.Style.FLEX:
            case Attributes.Style.FLEX_GROW:
            case Attributes.Style.FLEX_SHRINK:
            case Attributes.Style.FLEX_BASIS:
                parseTransition(target, sceneRootView);
                TransitionManager.beginDelayedTransition(sceneRootView, this);
                if (!isPreDrawListenerAdded && sceneRootView.isLaidOut()) {
                    ViewTreeObserver viewTreeObserver = sceneRootView.getViewTreeObserver();
                    viewTreeObserver.addOnPreDrawListener(
                            new ViewTreeObserver.OnPreDrawListener() {
                                @Override
                                public boolean onPreDraw() {
                                    if (viewTreeObserver.isAlive()) {
                                        viewTreeObserver.removeOnPreDrawListener(this);
                                    } else {
                                        sceneRootView.getViewTreeObserver()
                                                .removeOnPreDrawListener(this);
                                    }
                                    isPreDrawListenerAdded = false;
                                    // 取消下一帧绘制
                                    return false;
                                }
                            });
                    isPreDrawListenerAdded = true;
                }
                break;
            case Attributes.Style.BACKGROUND_POSITION:
                // 背景位置没准备好，延迟执行
                parseTransition(target, sceneRootView);
                break;
            default:
                break;
        }
    }

    private void parseTransition(@NonNull View target, ViewGroup sceneRootView) {
        Parameter parameter = getParameter(target);
        if (parameter.mProperties == null || parameter.isPropertiesParsed) {
            return;
        }

        String[] properties = TransitionUtils.parseString(parameter.mProperties);
        parameter.isPropertiesParsed = true;

        String[] durationArray = getDurationArray(parameter);
        String[] functionArray = getFunctionArray(parameter);
        String[] delayArray = getDelayArray(parameter);

        if (mTargetTransition == null) {
            mTargetTransition = new ArrayMap<>();
        }
        Map<String, Transition> propTransition = new LinkedHashMap<>(properties.length);
        mTargetTransition.put(target.hashCode(), propTransition);
        int index = 0;
        int duration;
        int delay;
        Interpolator interpolator;
        for (String property : properties) {
            duration =
                    index < durationArray.length
                            ? AnimationParser.getTime(durationArray[index])
                            : AnimationParser.getTime(durationArray[index % durationArray.length]);
            interpolator =
                    index < functionArray.length
                            ? TimingFactory.getTiming(functionArray[index])
                            : new EaseInterpolator();
            delay = index < delayArray.length ? AnimationParser.getTime(delayArray[index]) : 0;

            Transition transition = null;
            switch (property) {
                case Attributes.Style.WIDTH:
                    transition = new SizeTransition(true);
                    propTransition.put(property, transition);
                    break;
                case Attributes.Style.HEIGHT:
                    transition = new SizeTransition(false);
                    propTransition.put(property, transition);
                    break;
                case Attributes.Style.BACKGROUND_COLOR:
                    transition = new BgColorTransition();
                    propTransition.put(property, transition);
                    break;
                case Attributes.Style.BACKGROUND_POSITION:
                    transition = addBgPositionTransition(target);
                    break;
                case Attributes.Style.BACKGROUND:
                    addBgTransition(target, propTransition, interpolator, duration, delay);
                    break;
                case Attributes.Style.OPACITY:
                    transition = new OpacityTransition();
                    propTransition.put(property, transition);
                    break;
                case Attributes.Style.VISIBILITY:
                    addVisibilityTransition(propTransition, duration, delay);
                    break;
                case Attributes.Style.BORDER_COLOR:
                    addBdColorTransition(propTransition, interpolator, duration, delay);
                    break;
                case Attributes.Style.BORDER_LEFT_COLOR:
                case Attributes.Style.BORDER_TOP_COLOR:
                case Attributes.Style.BORDER_RIGHT_COLOR:
                case Attributes.Style.BORDER_BOTTOM_COLOR:
                    transition = new BorderColorTransition(property);
                    propTransition.put(property, transition);
                    break;
                case Attributes.Style.BORDER_WIDTH:
                    addBdWidthTransition(propTransition, interpolator, duration, delay);
                    break;
                case Attributes.Style.BORDER_LEFT_WIDTH:
                case Attributes.Style.BORDER_TOP_WIDTH:
                case Attributes.Style.BORDER_RIGHT_WIDTH:
                case Attributes.Style.BORDER_BOTTOM_WIDTH:
                    transition = new BorderWidthTransition(property);
                    propTransition.put(property, transition);
                    break;
                case Attributes.Style.PADDING:
                    addPaddingTransition(propTransition, interpolator, duration, delay);
                    break;
                case Attributes.Style.PADDING_LEFT:
                case Attributes.Style.PADDING_TOP:
                case Attributes.Style.PADDING_RIGHT:
                case Attributes.Style.PADDING_BOTTOM:
                    transition = new PaddingTransition(property);
                    propTransition.put(property, transition);
                    break;
                case Attributes.Style.MARGIN:
                    addMarginTransition(propTransition, interpolator, duration, delay);
                    break;
                case Attributes.Style.MARGIN_LEFT:
                case Attributes.Style.MARGIN_TOP:
                case Attributes.Style.MARGIN_RIGHT:
                case Attributes.Style.MARGIN_BOTTOM:
                    transition = new MarginTransition(property);
                    propTransition.put(property, transition);
                    break;
                case Attributes.Style.LEFT:
                case Attributes.Style.TOP:
                case Attributes.Style.RIGHT:
                case Attributes.Style.BOTTOM:
                    transition = new PositionTransition(property);
                    propTransition.put(property, transition);
                    break;
                case Attributes.Style.FLEX:
                    addFlexTransition(propTransition, interpolator, duration, delay);
                    break;
                case Attributes.Style.FLEX_GROW:
                case Attributes.Style.FLEX_SHRINK:
                case Attributes.Style.FLEX_BASIS:
                    transition = new FlexTransition(property);
                    propTransition.put(property, transition);
                    break;
                case Attributes.Style.TRANSFORM_ORIGIN:
                    transition = new TransformOriginTransition();
                    propTransition.put(property, transition);
                    break;
                case Attributes.Style.TRANSFORM:
                    transition = addTransformTransition(propTransition);
                    break;
                case Attributes.Style.ALL:
                    addAllTransition(target, propTransition, interpolator, duration, delay);
                    break;
                case Attributes.Style.NONE:
                    noneTransition(target, propTransition);
                    return;
                default:
                    break;
            }
            if (transition != null) {
                transition.setDuration(duration).setInterpolator(interpolator).setStartDelay(delay);
            }
            index++;
        }

        for (Transition transition : propTransition.values()) {
            addTransition(transition);
            transition.addTarget(target);
        }
    }

    private Parameter getParameter(@NonNull View target) {
        Parameter parameter = mTargetParameter.get(target.hashCode());
        if (parameter == null) {
            parameter = new Parameter();
            mTargetParameter.put(target.hashCode(), parameter);
        }
        return parameter;
    }

    public void beginDelayedTransition(View target, ViewGroup sceneRoot) {
        if (mDelayedTransition == null || target == null || sceneRoot == null) {
            return;
        }
        Map<String, Transition> propertyTransition = mDelayedTransition.get(target.hashCode());
        if (propertyTransition == null) {
            return;
        }
        for (Transition delayedTransition : propertyTransition.values()) {
            TransitionManager.beginDelayedTransition(sceneRoot, delayedTransition);
        }
    }

    @NonNull
    private Map<String, Transition> getDelayedTransitionMap(@NonNull View target) {
        if (mDelayedTransition == null) {
            mDelayedTransition = new ArrayMap<>(1);
        }
        int targetCode = target.hashCode();
        Map<String, Transition> propertyTransition = mDelayedTransition.get(targetCode);
        if (propertyTransition == null) {
            propertyTransition = new ArrayMap<>(1);
            mDelayedTransition.put(targetCode, propertyTransition);
        }
        return propertyTransition;
    }

    private Transition addBgPositionTransition(@NonNull View target) {
        Map<String, Transition> propertyTransition = getDelayedTransitionMap(target);
        Transition bgPositionTransition = new BgPositionTransition().addTarget(target);
        propertyTransition.put(Attributes.Style.BACKGROUND_POSITION, bgPositionTransition);
        return bgPositionTransition;
    }

    private void addBgTransition(
            @NonNull View target,
            @NonNull Map<String, Transition> propTransition,
            Interpolator interpolator,
            int duration,
            int delay) {
        // 属性集合缩写，参考标准：https://developer.mozilla.org/en-US/docs/Web/CSS/background
        // todo: 对于快应用，支持 background-color, background-size (待补充), background-position
        Map<String, Transition> propertyTransition = getDelayedTransitionMap(target);
        Transition bgPositionTransition =
                new BgPositionTransition()
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay)
                        .addTarget(target);
        propertyTransition.put(Attributes.Style.BACKGROUND_POSITION, bgPositionTransition);
        Transition bgColorTransition =
                new BgColorTransition()
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        propTransition.put(Attributes.Style.BACKGROUND_COLOR, bgColorTransition);
    }

    private void addVisibilityTransition(
            @NonNull Map<String, Transition> propTransition, int duration, int delay) {
        Transition visibility =
                new Fade().setInterpolator(null).setDuration(0).setStartDelay(delay + duration);
        propTransition.put(Attributes.Style.VISIBILITY, visibility);
    }

    private void addBdColorTransition(
            @NonNull Map<String, Transition> propTransition,
            Interpolator interpolator,
            int duration,
            int delay) {
        Transition leftColor =
                new BorderColorTransition(Attributes.Style.BORDER_LEFT_COLOR)
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        Transition topColor =
                new BorderColorTransition(Attributes.Style.BORDER_TOP_COLOR)
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        Transition rightColor =
                new BorderColorTransition(Attributes.Style.BORDER_RIGHT_COLOR)
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        Transition bottomColor =
                new BorderColorTransition(Attributes.Style.BORDER_BOTTOM_COLOR)
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        propTransition.put(Attributes.Style.BORDER_LEFT_COLOR, leftColor);
        propTransition.put(Attributes.Style.BORDER_TOP_COLOR, topColor);
        propTransition.put(Attributes.Style.BORDER_RIGHT_COLOR, rightColor);
        propTransition.put(Attributes.Style.BORDER_BOTTOM_COLOR, bottomColor);
    }

    private void addBdWidthTransition(
            @NonNull Map<String, Transition> propTransition,
            Interpolator interpolator,
            int duration,
            int delay) {
        Transition leftWidth =
                new BorderWidthTransition(Attributes.Style.BORDER_LEFT_WIDTH)
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        Transition topWidth =
                new BorderWidthTransition(Attributes.Style.BORDER_TOP_WIDTH)
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        Transition rightWidth =
                new BorderWidthTransition(Attributes.Style.BORDER_RIGHT_WIDTH)
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        Transition bottomWidth =
                new BorderWidthTransition(Attributes.Style.BORDER_BOTTOM_WIDTH)
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        propTransition.put(Attributes.Style.BORDER_LEFT_WIDTH, leftWidth);
        propTransition.put(Attributes.Style.BORDER_TOP_WIDTH, topWidth);
        propTransition.put(Attributes.Style.BORDER_RIGHT_WIDTH, rightWidth);
        propTransition.put(Attributes.Style.BORDER_BOTTOM_WIDTH, bottomWidth);
    }

    private void addPaddingTransition(
            @NonNull Map<String, Transition> propTransition,
            Interpolator interpolator,
            int duration,
            int delay) {
        Transition paddingLeft =
                new PaddingTransition(Attributes.Style.PADDING_LEFT)
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        Transition paddingTop =
                new PaddingTransition(Attributes.Style.PADDING_TOP)
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        Transition paddingRight =
                new PaddingTransition(Attributes.Style.PADDING_RIGHT)
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        Transition paddingBottom =
                new PaddingTransition(Attributes.Style.PADDING_BOTTOM)
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        propTransition.put(Attributes.Style.PADDING_LEFT, paddingLeft);
        propTransition.put(Attributes.Style.PADDING_TOP, paddingTop);
        propTransition.put(Attributes.Style.PADDING_RIGHT, paddingRight);
        propTransition.put(Attributes.Style.PADDING_BOTTOM, paddingBottom);
    }

    private void addMarginTransition(
            @NonNull Map<String, Transition> propTransition,
            Interpolator interpolator,
            int duration,
            int delay) {
        Transition marginLeft =
                new MarginTransition(Attributes.Style.MARGIN_LEFT)
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        Transition marginTop =
                new MarginTransition(Attributes.Style.MARGIN_TOP)
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        Transition marginRight =
                new MarginTransition(Attributes.Style.MARGIN_RIGHT)
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        Transition marginBottom =
                new MarginTransition(Attributes.Style.MARGIN_BOTTOM)
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        propTransition.put(Attributes.Style.MARGIN_LEFT, marginLeft);
        propTransition.put(Attributes.Style.MARGIN_TOP, marginTop);
        propTransition.put(Attributes.Style.MARGIN_RIGHT, marginRight);
        propTransition.put(Attributes.Style.MARGIN_BOTTOM, marginBottom);
    }

    private void addPositionTransition(
            @NonNull Map<String, Transition> propTransition,
            Interpolator interpolator,
            int duration,
            int delay) {
        Transition left =
                new PositionTransition(Attributes.Style.LEFT)
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        Transition top =
                new PositionTransition(Attributes.Style.TOP)
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        Transition right =
                new PositionTransition(Attributes.Style.RIGHT)
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        Transition bottom =
                new PositionTransition(Attributes.Style.BOTTOM)
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        propTransition.put(Attributes.Style.LEFT, left);
        propTransition.put(Attributes.Style.TOP, top);
        propTransition.put(Attributes.Style.RIGHT, right);
        propTransition.put(Attributes.Style.BOTTOM, bottom);
    }

    private void addFlexTransition(
            @NonNull Map<String, Transition> propTransition,
            Interpolator interpolator,
            int duration,
            int delay) {
        Transition flexGrow =
                new FlexTransition(Attributes.Style.FLEX_GROW)
                        .setDuration(duration)
                        .setInterpolator(interpolator)
                        .setStartDelay(delay);
        Transition flexShrink =
                new FlexTransition(Attributes.Style.FLEX_SHRINK)
                        .setDuration(duration)
                        .setInterpolator(interpolator)
                        .setStartDelay(delay);
        Transition flexBasis =
                new FlexTransition(Attributes.Style.FLEX_BASIS)
                        .setDuration(duration)
                        .setInterpolator(interpolator)
                        .setStartDelay(delay);
        propTransition.put(Attributes.Style.FLEX_GROW, flexGrow);
        propTransition.put(Attributes.Style.FLEX_SHRINK, flexShrink);
        propTransition.put(Attributes.Style.FLEX_BASIS, flexBasis);
    }

    private Transition addTransformTransition(@NonNull Map<String, Transition> propTransition) {
        Transition transform = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            transform = new ChangeTransform();
            propTransition.put(Attributes.Style.TRANSFORM, transform);
        } else {
            Log.w(
                    TAG, "prepare: transform transition not supported for version "
                            + Build.VERSION.SDK_INT);
        }
        return transform;
    }

    private void addAllTransition(
            @NonNull View target,
            @NonNull Map<String, Transition> propTransition,
            Interpolator interpolator,
            int duration,
            int delay) {
        // todo: all 可以包含更多 transition
        Transition width =
                new SizeTransition(true)
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        Transition height =
                new SizeTransition(false)
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        Transition opacity =
                new OpacityTransition()
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        Transition visibility =
                new Fade().setInterpolator(null).setDuration(0).setStartDelay(delay + duration);
        Transition transformOrigin =
                new TransformOriginTransition()
                        .setInterpolator(interpolator)
                        .setDuration(duration)
                        .setStartDelay(delay);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Transition transform =
                    new ChangeTransform()
                            .setInterpolator(interpolator)
                            .setDuration(duration)
                            .setStartDelay(delay);
            propTransition.put(Attributes.Style.TRANSFORM, transform);
        } else {
            Log.w(
                    TAG,
                    "prepare: "
                            + Attributes.Style.TRANSFORM
                            + " transition not supported for version "
                            + Build.VERSION.SDK_INT);
        }
        propTransition.put(Attributes.Style.WIDTH, width);
        propTransition.put(Attributes.Style.HEIGHT, height);
        propTransition.put(Attributes.Style.OPACITY, opacity);
        propTransition.put(Attributes.Style.VISIBILITY, visibility);
        propTransition.put(Attributes.Style.TRANSFORM_ORIGIN, transformOrigin);
        addBgTransition(target, propTransition, interpolator, duration, delay);
        addFlexTransition(propTransition, interpolator, duration, delay);
        addBdColorTransition(propTransition, interpolator, duration, delay);
        addBdWidthTransition(propTransition, interpolator, duration, delay);
        addMarginTransition(propTransition, interpolator, duration, delay);
        addPositionTransition(propTransition, interpolator, duration, delay);
        addPaddingTransition(propTransition, interpolator, duration, delay);
    }

    private void noneTransition(
            @NonNull View target, @NonNull Map<String, Transition> propTransition) {
        propTransition.clear();
        removeTarget(target);
        removeTransition(target);
    }

    private String[] getDurationArray(@NonNull Parameter parameter) {
        if (parameter.mDurations == null) {
            return new String[] {"0s"};
        }
        return TransitionUtils.parseString(parameter.mDurations);
    }

    private String[] getFunctionArray(@NonNull Parameter parameter) {
        if (parameter.mTimingFunctions == null) {
            return new String[] {"ease"};
        }
        return TransitionUtils.parseString(parameter.mTimingFunctions);
    }

    private String[] getDelayArray(@NonNull Parameter parameter) {
        if (parameter.mDelays == null) {
            return new String[] {"0s"};
        }
        return TransitionUtils.parseString(parameter.mDelays);
    }

    private void removeTransition(@NonNull View target) {
        int targetCode = target.hashCode();
        if (mTargetTransition != null) {
            Map<String, Transition> transitionMap = mTargetTransition.get(targetCode);
            if (transitionMap == null) {
                return;
            }
            for (Transition transition : transitionMap.values()) {
                removeTransition(transition);
            }
            transitionMap.clear();
            mTargetTransition.remove(targetCode);
        }
        if (mDelayedTransition != null) {
            mDelayedTransition.remove(targetCode);
        }
    }

    public void destroy() {
        mTargetParameter.clear();
        mTargetParameter = null;
        if (mTargetTransition != null) {
            for (int targetCode : mTargetTransition.keySet()) {
                Map<String, Transition> transitionMap = mTargetTransition.get(targetCode);
                if (transitionMap != null) {
                    for (Transition transition : transitionMap.values()) {
                        removeTransition(transition);
                    }
                    transitionMap.clear();
                }
            }
            mTargetTransition.clear();
            mTargetTransition = null;
        }
        if (mDelayedTransition != null) {
            mDelayedTransition.clear();
            mDelayedTransition = null;
        }
    }

    static class Parameter {
        private String mProperties;
        private String mDurations;
        private String mTimingFunctions;
        private String mDelays;
        private boolean isPropertiesParsed = false;
    }
}
