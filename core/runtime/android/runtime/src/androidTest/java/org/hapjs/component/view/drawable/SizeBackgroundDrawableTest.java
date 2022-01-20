/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view.drawable;

import static junit.framework.Assert.assertEquals;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SizeBackgroundDrawableTest {

    private static final int PX = SizeBackgroundDrawable.Position.UNIT_PX;
    private static final int PERCENT = SizeBackgroundDrawable.Position.UNIT_PERCENT;
    private static final int OFFSET = SizeBackgroundDrawable.Position.UNIT_PERCENT_OFFSET;

    private static final String[] SAMPLES = {
            "xpx", "20px", "x%", "30%", "left", "right", "top", "bottom", "center", "invalid"
    };

    private SizeBackgroundDrawable.Position mPosition;

    private int length = SAMPLES.length;

    @Before
    public void setUp() {

        mPosition = new SizeBackgroundDrawable.Position();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSpecifiedStr() {

        String target = "50.9%";
        mPosition.parsePosition(target);
        assertValid(target, (float) 50.9, 0, PERCENT, 50, 0, PERCENT);
        mPosition.setDefault();

        target = "50%%";
        mPosition.parsePosition(target);
        assertDefault(target);
        mPosition.setDefault();

        target = "50%90%";
        mPosition.parsePosition(target);
        assertDefault(target);
        mPosition.setDefault();

        target = "100pxpxpx";
        mPosition.parsePosition(target);
        assertDefault(target);
        mPosition.setDefault();

        target = "50px50px";
        mPosition.parsePosition(target);
        assertDefault(target);
        mPosition.setDefault();
    }

    @Test
    public void testStrParseOneParams() {

        for (int i = 0; i < length; i++) {
            String target = SAMPLES[i];
            mPosition.parsePosition(target);
            switch (target) {
                case "20px":
                    assertValid(target, 20, 0, PX, 50, 0, PERCENT);
                    break;
                case "30%":
                    assertValid(target, 30, 0, PERCENT, 50, 0, PERCENT);
                    break;
                case "left":
                    assertValid(target, 0, 0, PX, 50, 0, PERCENT);
                    break;
                case "right":
                    assertValid(target, 100, 0, PERCENT, 50, 0, PERCENT);
                    break;
                case "top":
                    assertValid(target, 50, 0, PERCENT, 0, 0, PX);
                    break;
                case "bottom":
                    assertValid(target, 50, 0, PERCENT, 100, 0, PERCENT);
                    break;
                case "center":
                    assertValid(target, 50, 0, PERCENT, 50, 0, PERCENT);
                    break;
                default:
                    assertDefault(target);
            }
            mPosition.setDefault();
        }
    }

    @Test
    public void testStrParseTwoParams() {

        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++) {
                String target = SAMPLES[i] + " " + SAMPLES[j];
                mPosition.parsePosition(target);
                switch (target) {
                    case "20px 20px":
                        assertValid(target, 20, 0, PX, 20, 0, PX);
                        break;
                    case "20px 30%":
                        assertValid(target, 20, 0, PX, 30, 0, PERCENT);
                        break;
                    case "20px top":
                        assertValid(target, 20, 0, PX, 0, 0, PX);
                        break;
                    case "20px bottom":
                        assertValid(target, 20, 0, PX, 100, 0, PERCENT);
                        break;
                    case "20px center":
                        assertValid(target, 20, 0, PX, 50, 0, PERCENT);
                        break;

                    case "30% 20px":
                        assertValid(target, 30, 0, PERCENT, 20, 0, PX);
                        break;
                    case "30% 30%":
                        assertValid(target, 30, 0, PERCENT, 30, 0, PERCENT);
                        break;
                    case "30% top":
                        assertValid(target, 30, 0, PERCENT, 0, 0, PX);
                        break;
                    case "30% bottom":
                        assertValid(target, 30, 0, PERCENT, 100, 0, PERCENT);
                        break;
                    case "30% center":
                        assertValid(target, 30, 0, PERCENT, 50, 0, PERCENT);
                        break;

                    case "left 20px":
                        assertValid(target, 0, 0, PX, 20, 0, PX);
                        break;
                    case "left 30%":
                        assertValid(target, 0, 0, PX, 30, 0, PERCENT);
                        break;
                    case "left top":
                        assertValid(target, 0, 0, PX, 0, 0, PX);
                        break;
                    case "left bottom":
                        assertValid(target, 0, 0, PX, 100, 0, PERCENT);
                        break;
                    case "left center":
                        assertValid(target, 0, 0, PX, 50, 0, PERCENT);
                        break;

                    case "right 20px":
                        assertValid(target, 100, 0, PERCENT, 20, 0, PX);
                        break;
                    case "right 30%":
                        assertValid(target, 100, 0, PERCENT, 30, 0, PERCENT);
                        break;
                    case "right top":
                        assertValid(target, 100, 0, PERCENT, 0, 0, PX);
                        break;
                    case "right bottom":
                        assertValid(target, 100, 0, PERCENT, 100, 0, PERCENT);
                        break;
                    case "right center":
                        assertValid(target, 100, 0, PERCENT, 50, 0, PERCENT);
                        break;

                    case "top left":
                        assertValid(target, 0, 0, PX, 0, 0, PX);
                        break;
                    case "top right":
                        assertValid(target, 100, 0, PERCENT, 0, 0, PX);
                        break;
                    case "top center":
                        assertValid(target, 50, 0, PERCENT, 0, 0, PX);
                        break;

                    case "bottom left":
                        assertValid(target, 0, 0, PX, 100, 0, PERCENT);
                        break;
                    case "bottom right":
                        assertValid(target, 100, 0, PERCENT, 100, 0, PERCENT);
                        break;
                    case "bottom center":
                        assertValid(target, 50, 0, PERCENT, 100, 0, PERCENT);
                        break;

                    case "center 20px":
                        assertValid(target, 50, 0, PERCENT, 20, 0, PX);
                        break;
                    case "center 30%":
                        assertValid(target, 50, 0, PERCENT, 30, 0, PERCENT);
                        break;
                    case "center left":
                        assertValid(target, 0, 0, PX, 50, 0, PERCENT);
                        break;
                    case "center right":
                        assertValid(target, 100, 0, PERCENT, 50, 0, PERCENT);
                        break;
                    case "center top":
                        assertValid(target, 50, 0, PERCENT, 0, 0, PX);
                        break;
                    case "center bottom":
                        assertValid(target, 50, 0, PERCENT, 100, 0, PERCENT);
                        break;
                    case "center center":
                        assertValid(target, 50, 0, PERCENT, 50, 0, PERCENT);
                        break;

                    default:
                        assertDefault(target);
                }
                mPosition.setDefault();
            }
        }
    }

    @Test
    public void testStrParseThreeParams() {

        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++) {
                for (int k = 0; k < length; k++) {
                    String target = SAMPLES[i] + " " + SAMPLES[j] + " " + SAMPLES[k];
                    mPosition.parsePosition(target);
                    switch (target) {
                        case "left 20px top":
                            assertValid(target, 20, 0, PX, 0, 0, PX);
                            break;
                        case "left 20px bottom":
                            assertValid(target, 20, 0, PX, 100, 0, PERCENT);
                            break;
                        case "left 20px center":
                            assertValid(target, 20, 0, PX, 50, 0, PERCENT);
                            break;

                        case "left 30% top":
                            assertValid(target, 30, 0, PERCENT, 0, 0, PX);
                            break;
                        case "left 30% bottom":
                            assertValid(target, 30, 0, PERCENT, 100, 0, PERCENT);
                            break;
                        case "left 30% center":
                            assertValid(target, 30, 0, PERCENT, 50, 0, PERCENT);
                            break;

                        case "left top 20px":
                            assertValid(target, 0, 0, PX, 20, 0, PX);
                            break;
                        case "left top 30%":
                            assertValid(target, 0, 0, PX, 30, 0, PERCENT);
                            break;
                        case "left bottom 20px":
                            assertValid(target, 0, 0, PX, 100, -20, OFFSET);
                            break;
                        case "left bottom 30%":
                            assertValid(target, 0, 0, PX, 70, 0, PERCENT);
                            break;

                        case "right 20px top":
                            assertValid(target, 100, -20, OFFSET, 0, 0, PX);
                            break;
                        case "right 20px bottom":
                            assertValid(target, 100, -20, OFFSET, 100, 0, PERCENT);
                            break;
                        case "right 20px center":
                            assertValid(target, 100, -20, OFFSET, 50, 0, PERCENT);
                            break;

                        case "right 30% top":
                            assertValid(target, 70, 0, PERCENT, 0, 0, PX);
                            break;
                        case "right 30% bottom":
                            assertValid(target, 70, 0, PERCENT, 100, 0, PERCENT);
                            break;
                        case "right 30% center":
                            assertValid(target, 70, 0, PERCENT, 50, 0, PERCENT);
                            break;

                        case "right top 20px":
                            assertValid(target, 100, 0, PERCENT, 20, 0, PX);
                            break;
                        case "right top 30%":
                            assertValid(target, 100, 0, PERCENT, 30, 0, PERCENT);
                            break;
                        case "right bottom 20px":
                            assertValid(target, 100, 0, PERCENT, 100, -20, OFFSET);
                            break;
                        case "right bottom 30%":
                            assertValid(target, 100, 0, PERCENT, 70, 0, PERCENT);
                            break;

                        case "top 20px left":
                            assertValid(target, 0, 0, PX, 20, 0, PX);
                            break;
                        case "top 20px right":
                            assertValid(target, 100, 0, PERCENT, 20, 0, PX);
                            break;
                        case "top 20px center":
                            assertValid(target, 50, 0, PERCENT, 20, 0, PX);
                            break;

                        case "top 30% left":
                            assertValid(target, 0, 0, PX, 30, 0, PERCENT);
                            break;
                        case "top 30% right":
                            assertValid(target, 100, 0, PERCENT, 30, 0, PERCENT);
                            break;
                        case "top 30% center":
                            assertValid(target, 50, 0, PERCENT, 30, 0, PERCENT);
                            break;

                        case "top left 20px":
                            assertValid(target, 20, 0, PX, 0, 0, PX);
                            break;
                        case "top left 30%":
                            assertValid(target, 30, 0, PERCENT, 0, 0, PX);
                            break;
                        case "top right 20px":
                            assertValid(target, 100, -20, OFFSET, 0, 0, PX);
                            break;
                        case "top right 30%":
                            assertValid(target, 70, 0, PERCENT, 0, 0, PX);
                            break;

                        case "bottom 20px left":
                            assertValid(target, 0, 0, PX, 100, -20, OFFSET);
                            break;
                        case "bottom 20px right":
                            assertValid(target, 100, 0, PERCENT, 100, -20, OFFSET);
                            break;
                        case "bottom 20px center":
                            assertValid(target, 50, 0, PERCENT, 100, -20, OFFSET);
                            break;

                        case "bottom 30% left":
                            assertValid(target, 0, 0, PX, 70, 0, PERCENT);
                            break;
                        case "bottom 30% right":
                            assertValid(target, 100, 0, PERCENT, 70, 0, PERCENT);
                            break;
                        case "bottom 30% center":
                            assertValid(target, 50, 0, PERCENT, 70, 0, PERCENT);
                            break;

                        case "bottom left 20px":
                            assertValid(target, 20, 0, PX, 100, 0, PERCENT);
                            break;
                        case "bottom left 30%":
                            assertValid(target, 30, 0, PERCENT, 100, 0, PERCENT);
                            break;
                        case "bottom right 20px":
                            assertValid(target, 100, -20, OFFSET, 100, 0, PERCENT);
                            break;
                        case "bottom right 30%":
                            assertValid(target, 70, 0, PERCENT, 100, 0, PERCENT);
                            break;

                        case "center left 20px":
                            assertValid(target, 20, 0, PX, 50, 0, PERCENT);
                            break;
                        case "center left 30%":
                            assertValid(target, 30, 0, PERCENT, 50, 0, PERCENT);
                            break;
                        case "center right 20px":
                            assertValid(target, 100, -20, OFFSET, 50, 0, PERCENT);
                            break;
                        case "center right 30%":
                            assertValid(target, 70, 0, PERCENT, 50, 0, PERCENT);
                            break;
                        case "center top 20px":
                            assertValid(target, 50, 0, PERCENT, 20, 0, PX);
                            break;
                        case "center top 30%":
                            assertValid(target, 50, 0, PERCENT, 30, 0, PERCENT);
                            break;
                        case "center bottom 20px":
                            assertValid(target, 50, 0, PERCENT, 100, -20, OFFSET);
                            break;
                        case "center bottom 30%":
                            assertValid(target, 50, 0, PERCENT, 70, 0, PERCENT);
                            break;

                        default:
                            assertDefault(target);
                    }
                    mPosition.setDefault();
                }
            }
        }
    }

    @Test
    public void testStrParseFourParams() {

        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++) {
                for (int k = 0; k < length; k++) {
                    for (int m = 0; m < length; m++) {
                        String target =
                                SAMPLES[i] + " " + SAMPLES[j] + " " + SAMPLES[k] + " " + SAMPLES[m];
                        mPosition.parsePosition(target);
                        switch (target) {
                            case "left 20px top 20px":
                                assertValid(target, 20, 0, PX, 20, 0, PX);
                                break;
                            case "left 20px top 30%":
                                assertValid(target, 20, 0, PX, 30, 0, PERCENT);
                                break;

                            case "left 20px bottom 20px":
                                assertValid(target, 20, 0, PX, 100, -20, OFFSET);
                                break;
                            case "left 20px bottom 30%":
                                assertValid(target, 20, 0, PX, 70, 0, PERCENT);
                                break;

                            case "left 30% top 20px":
                                assertValid(target, 30, 0, PERCENT, 20, 0, PX);
                                break;
                            case "left 30% top 30%":
                                assertValid(target, 30, 0, PERCENT, 30, 0, PERCENT);
                                break;

                            case "left 30% bottom 20px":
                                assertValid(target, 30, 0, PERCENT, 100, -20, OFFSET);
                                break;
                            case "left 30% bottom 30%":
                                assertValid(target, 30, 0, PERCENT, 70, 0, PERCENT);
                                break;

                            case "right 20px top 20px":
                                assertValid(target, 100, -20, OFFSET, 20, 0, PX);
                                break;
                            case "right 20px top 30%":
                                assertValid(target, 100, -20, OFFSET, 30, 0, PERCENT);
                                break;

                            case "right 20px bottom 20px":
                                assertValid(target, 100, -20, OFFSET, 100, -20, OFFSET);
                                break;
                            case "right 20px bottom 30%":
                                assertValid(target, 100, -20, OFFSET, 70, 0, PERCENT);
                                break;

                            case "right 30% top 20px":
                                assertValid(target, 70, 0, PERCENT, 20, 0, PX);
                                break;
                            case "right 30% top 30%":
                                assertValid(target, 70, 0, PERCENT, 30, 0, PERCENT);
                                break;

                            case "right 30% bottom 20px":
                                assertValid(target, 70, 0, PERCENT, 100, -20, OFFSET);
                                break;
                            case "right 30% bottom 30%":
                                assertValid(target, 70, 0, PERCENT, 70, 0, PERCENT);
                                break;

                            case "top 20px left 20px":
                                assertValid(target, 20, 0, PX, 20, 0, PX);
                                break;
                            case "top 30% left 20px":
                                assertValid(target, 20, 0, PX, 30, 0, PERCENT);
                                break;

                            case "bottom 20px left 20px":
                                assertValid(target, 20, 0, PX, 100, -20, OFFSET);
                                break;
                            case "bottom 30% left 20px":
                                assertValid(target, 20, 0, PX, 70, 0, PERCENT);
                                break;

                            case "top 20px left 30%":
                                assertValid(target, 30, 0, PERCENT, 20, 0, PX);
                                break;
                            case "top 30% left 30%":
                                assertValid(target, 30, 0, PERCENT, 30, 0, PERCENT);
                                break;

                            case "bottom 20px left 30%":
                                assertValid(target, 30, 0, PERCENT, 100, -20, OFFSET);
                                break;
                            case "bottom 30% left 30%":
                                assertValid(target, 30, 0, PERCENT, 70, 0, PERCENT);
                                break;

                            case "top 20px right 20px":
                                assertValid(target, 100, -20, OFFSET, 20, 0, PX);
                                break;
                            case "top 30% right 20px":
                                assertValid(target, 100, -20, OFFSET, 30, 0, PERCENT);
                                break;

                            case "bottom 20px right 20px":
                                assertValid(target, 100, -20, OFFSET, 100, -20, OFFSET);
                                break;
                            case "bottom 30% right 20px":
                                assertValid(target, 100, -20, OFFSET, 70, 0, PERCENT);
                                break;

                            case "top 20px right 30%":
                                assertValid(target, 70, 0, PERCENT, 20, 0, PX);
                                break;
                            case "top 30% right 30%":
                                assertValid(target, 70, 0, PERCENT, 30, 0, PERCENT);
                                break;

                            case "bottom 20px right 30%":
                                assertValid(target, 70, 0, PERCENT, 100, -20, OFFSET);
                                break;
                            case "bottom 30% right 30%":
                                assertValid(target, 70, 0, PERCENT, 70, 0, PERCENT);
                                break;

                            default:
                                assertDefault(target);
                        }
                        mPosition.setDefault();
                    }
                }
            }
        }
    }

    private void assertDefault(String target) {
        assertValid(target, 0, 0, PX, 0, 0, PX);
    }

    private void assertValid(
            String target, float x, float offsetX, int unitX, float y, float offsetY, int unitY) {

        assertEquals(target, x, mPosition.getParseX());
        assertEquals(target, offsetX, mPosition.getOffsetX());
        assertEquals(target, unitX, mPosition.getXUnit());
        assertEquals(target, y, mPosition.getParseY());
        assertEquals(target, offsetY, mPosition.getOffsetY());
        assertEquals(target, unitY, mPosition.getYUnit());
    }
}
