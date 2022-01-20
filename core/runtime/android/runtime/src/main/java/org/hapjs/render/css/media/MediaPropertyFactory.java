/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css.media;

public class MediaPropertyFactory {

    static MediaProperty createHeightProperty() {
        return new HeightProperty();
    }

    static MediaProperty createWidthProperty() {
        return new WidthProperty();
    }

    static MediaProperty createInvalidProperty() {
        return new InvalidProperty();
    }

    static MediaProperty createDeviceHeightProperty() {
        return new DeviceHeightProperty();
    }

    static MediaProperty createDeviceWidthProperty() {
        return new DeviceWidthProperty();
    }

    static MediaProperty createResolutionProperty() {
        return new ResolutionProperty();
    }

    static MediaProperty createAspectRatioProperty() {
        return new AspectRatioProperty();
    }

    static MediaProperty createOrientationProperty() {
        return new OrientationProperty();
    }

    static MediaProperty createPrefersColorSchemeProperty() {
        return new PrefersColorScheme();
    }

    static CompareOperator createIntCompareOperator(
            int compareTo1, int op1, int compareTo2, int op2) {
        return new TwoIntCompareOperator(compareTo1, op1, compareTo2, op2);
    }

    static CompareOperator createIntCompareOperator(int compareTo1, int op1) {
        return new IntCompareOperator(compareTo1, op1);
    }

    static CompareOperator createSimpleCompareOperator(Object compareTo1) {
        return new SimpleCompareOperator(compareTo1);
    }

    abstract static class NumberProperty extends MediaProperty {
    }

    static class HeightProperty extends NumberProperty {

        @Override
        void updateMediaPropertyInfo(MediaPropertyInfo info) {
            setValue(info.getViewPortHeight());
        }

        @Override
        int getType() {
            return MediaProperty.HEIGHT;
        }
    }

    static class WidthProperty extends NumberProperty {

        @Override
        void updateMediaPropertyInfo(MediaPropertyInfo info) {
            setValue(info.getViewPortWidth());
        }

        @Override
        int getType() {
            return MediaProperty.WIDTH;
        }
    }

    static class InvalidProperty extends MediaProperty {

        @Override
        void updateMediaPropertyInfo(MediaPropertyInfo info) {
            setValue(null);
        }

        @Override
        int getType() {
            return MediaProperty.INVALID_PROPERTY;
        }
    }

    static class DeviceHeightProperty extends NumberProperty {

        @Override
        void updateMediaPropertyInfo(MediaPropertyInfo info) {
            setValue(info.getScreenHeight());
        }

        @Override
        int getType() {
            return MediaProperty.DEVICE_HEIGHT;
        }
    }

    static class DeviceWidthProperty extends NumberProperty {

        @Override
        void updateMediaPropertyInfo(MediaPropertyInfo info) {
            setValue(info.getScreenWidth());
        }

        @Override
        int getType() {
            return MediaProperty.DEVICE_WIDTH;
        }
    }

    static class ResolutionProperty extends MediaProperty {

        @Override
        void updateMediaPropertyInfo(MediaPropertyInfo info) {
            setValue(info.getResolution());
        }

        @Override
        int getType() {
            return MediaProperty.RESOLUTION;
        }
    }

    static class AspectRatioProperty extends MediaProperty {

        @Override
        void updateMediaPropertyInfo(MediaPropertyInfo info) {
            setValue(100000 * info.getViewPortWidth() / info.getViewPortHeight());
        }

        @Override
        int getType() {
            return MediaProperty.ASPECT_RATIO;
        }
    }

    static class OrientationProperty extends MediaProperty {

        @Override
        void updateMediaPropertyInfo(MediaPropertyInfo info) {
            setValue(info.getOrientation());
        }

        @Override
        int getType() {
            return MediaProperty.ORIENTATION;
        }
    }

    static class PrefersColorScheme extends MediaProperty {

        @Override
        void updateMediaPropertyInfo(MediaPropertyInfo info) {
            setValue(info.getPrefersColorScheme());
        }

        @Override
        int getType() {
            return MediaProperty.PREFERS_COLOR_SCHEME;
        }
    }

    static class SimpleCompareOperator implements CompareOperator {
        private final Object compareTo;

        SimpleCompareOperator(Object compareTo) {
            this.compareTo = compareTo;
        }

        @Override
        public boolean compare(Object value) {
            return compareTo.equals(value);
        }
    }

    static class TwoIntCompareOperator implements CompareOperator {
        private IntCompareOperator intOp1;
        private IntCompareOperator intOp2;

        TwoIntCompareOperator(int compareTo1, int op1, int compareTo2, int op2) {
            intOp1 = new IntCompareOperator(compareTo1, op1);
            intOp2 = new IntCompareOperator(compareTo2, op2);
        }

        @Override
        public boolean compare(Object value) {
            return intOp1.compare(value) && intOp2.compare(value);
        }

        public IntCompareOperator getFirst() {
            return intOp1;
        }

        public IntCompareOperator getSecond() {
            return intOp2;
        }
    }

    static class IntCompareOperator implements CompareOperator {

        private final int compareTo;
        private final int op;

        IntCompareOperator(int compareTo, int op) {
            this.compareTo = compareTo;
            this.op = op;
        }

        boolean compare(int value) {
            switch (op) {
                case CompareOperator.MEDIA_OP_EQUAL:
                    return value == compareTo;
                case CompareOperator.MEDIA_OP_LESS:
                    return value < compareTo;
                case CompareOperator.MEDIA_OP_LESS_EQUAL:
                    return value <= compareTo;
                case CompareOperator.MEDIA_OP_MORE:
                    return value > compareTo;
                case CompareOperator.MEDIA_OP_MORE_EQUAL:
                    return value >= compareTo;
                default:
                    break;
            }
            throw new IllegalStateException();
        }

        @Override
        public boolean compare(Object value) {
            return compare((int) value);
        }

        int getCompareValue() {
            return compareTo;
        }

        int getOperatorType() {
            return op;
        }
    }
}
