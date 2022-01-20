/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css.media;

import org.junit.Assert;
import org.junit.Test;

public class CSSMediaParserTest {

    @Test
    public void media() {
        String mediaListText = "(height: 30)";
        MediaList mediaList = CSSMediaParser.parseMediaList(mediaListText);

        Assert.assertEquals(mediaList.getMediaQueries().length, 1);

        Assert.assertEquals(mediaList.getMediaQueries()[0].getMediaProperties().length, 1);
        MediaProperty property = mediaList.getMediaQueries()[0].getMediaProperties()[0];

        Assert.assertEquals(property.getType(), MediaProperty.HEIGHT);
        MediaPropertyFactory.IntCompareOperator compareOperator =
                (MediaPropertyFactory.IntCompareOperator) property.getCompareOperator();
        Assert.assertEquals(compareOperator.getOperatorType(), CompareOperator.MEDIA_OP_EQUAL);
        Assert.assertEquals(compareOperator.getCompareValue(), 30);

        property.setValue(29);
        Assert.assertFalse(property.getResult());
        property.setValue(30);
        Assert.assertTrue(property.getResult());
        property.setValue(31);
        Assert.assertFalse(property.getResult());

        mediaListText = "(max-height: 30)";
        mediaList = CSSMediaParser.parseMediaList(mediaListText);
        property = mediaList.getMediaQueries()[0].getMediaProperties()[0];
        property.setValue(29);
        Assert.assertTrue(property.getResult());
        property.setValue(30);
        Assert.assertTrue(property.getResult());
        property.setValue(31);
        Assert.assertFalse(property.getResult());

        mediaListText = "screen and (min-height: 400) and (max-height: 700) ";
        mediaList = CSSMediaParser.parseMediaList(mediaListText);

        MediaQuery mediaQuery1 = mediaList.getMediaQueries()[0];
        MediaProperty[] properties = mediaQuery1.getMediaProperties();
        Assert.assertEquals(properties.length, 2);
        properties[0].setValue(400);
        properties[1].setValue(400);
        Assert.assertTrue(mediaQuery1.getResult());
        properties[0].setValue(700);
        properties[1].setValue(700);
        Assert.assertTrue(mediaQuery1.getResult());
        properties[0].setValue(399);
        properties[1].setValue(399);
        Assert.assertFalse(mediaQuery1.getResult());
        properties[0].setValue(701);
        properties[1].setValue(701);
        Assert.assertFalse(mediaQuery1.getResult());

        mediaListText = "(400 <= height <= 700)";
        mediaList = CSSMediaParser.parseMediaList(mediaListText);

        mediaQuery1 = mediaList.getMediaQueries()[0];
        properties = mediaQuery1.getMediaProperties();
        Assert.assertEquals(properties.length, 1);
        properties[0].setValue(400);
        Assert.assertTrue(mediaQuery1.getResult());
        properties[0].setValue(700);
        Assert.assertTrue(mediaQuery1.getResult());
        properties[0].setValue(399);
        Assert.assertFalse(mediaQuery1.getResult());
        properties[0].setValue(701);
        Assert.assertFalse(mediaQuery1.getResult());
    }

    @Test
    public void media2() {
        String mediaListText =
                "(resolution: 360dpi) and (aspect-ratio: 720/1080) and (orientation: portrait)";
        MediaList mediaList = CSSMediaParser.parseMediaList(mediaListText);
        mediaList.updateMediaPropertyInfo(
                new MediaPropertyInfo() {
                    @Override
                    public int getHeight() {
                        return 1080;
                    }

                    @Override
                    public int getWidth() {
                        return 720;
                    }

                    @Override
                    public int getScreenHeight() {
                        return 0;
                    }

                    @Override
                    public int getScreenWidth() {
                        return 0;
                    }

                    @Override
                    public int getResolution() {
                        return 360;
                    }

                    @Override
                    public int getOrientation() {
                        return MediaProperty.ORIENTATION_PORTRAIT;
                    }

                    @Override
                    public int getPrefersColorScheme() {
                        return 0;
                    }
                });

        Assert.assertTrue(mediaList.getResult());
    }
}
