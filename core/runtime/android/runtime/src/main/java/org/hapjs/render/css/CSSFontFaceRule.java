/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css;

import org.json.JSONObject;

/**
 * ---------------- CSSFontFaceRule ------------- "@FONT-FACE": { "myfont": { "fontName": "myfont",
 * "fontSrc": [ "/common/css/BenMoJinSong.ttf" ] }, "icomoon": { "fontName": "icomoon", "fontSrc": [
 * "/common/css/icomoon.ttf" ] } } ----------------------------------------------
 *
 * <p>"#myId" : { "fontFamily": "myfont, icomoon" }
 *
 * <p>FontFace 记录着字体的详细路径信息
 */
class CSSFontFaceRule extends CSSRule {
    private final JSONObject mDeclaration;

    CSSFontFaceRule(JSONObject declaration) {
        mDeclaration = declaration;
    }

    @Override
    public int getType() {
        return CSSRule.FONT_FACE_RULE;
    }

    /**
     * @param fontName 字体名称, 如 myfont / icomoon
     * @return 字体详细信息, 包含 fontName, fontSrc
     */
    Object getFontFace(String fontName) {
        Object value = mDeclaration.opt(fontName);
        if (value != null) {
            return value;
        }
        return CSSProperty.UNDEFINED;
    }
}
