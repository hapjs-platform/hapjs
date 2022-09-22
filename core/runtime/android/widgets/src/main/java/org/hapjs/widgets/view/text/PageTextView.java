/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.widgets.view.text;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.net.Uri;
import android.text.Html;
import android.text.TextUtils;

import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.CloseableImage;

import org.hapjs.common.utils.BitmapUtils;
import org.hapjs.widgets.canvas.image.CanvasBitmap;
import org.hapjs.widgets.canvas.image.CanvasImageCache;

public class PageTextView extends androidx.appcompat.widget.AppCompatTextView {
    private PageSplitCallback pageSplitCallback;
    private String mOriginText;
    private Drawable mPlaceholder;
    private Html.ImageGetter mImageGetter;
    private String curPageResetText;

    public PageTextView(Context context) {
        super(context);
        mPlaceholder = new ColorDrawable(Color.BLACK);
        mPlaceholder.setBounds(0, 0, 400, 300);
        mImageCache = new CanvasImageCache();
    }

    public void setPageSplitCallback(PageSplitCallback pageSplitCallback) {
        this.pageSplitCallback = pageSplitCallback;
    }

    boolean havePic = false;
    boolean haveSplitContent = false;
    private int picCount = 0;
    private final CanvasImageCache mImageCache;

    public void setOriginText(String text, int parentHeight, int parentWidth) {
        mOriginText = text;
        havePic = false;
        haveSplitContent = false;
        if (mImageGetter == null) {
            mImageGetter = source -> {
                havePic = true;
                picCount++;
                LevelListDrawable d = new LevelListDrawable();
                d.addLevel(0, 0, mPlaceholder);
                d.setBounds(0, 0, mPlaceholder.getIntrinsicWidth(), mPlaceholder.getIntrinsicHeight());
                BitmapUtils.fetchBitmap(Uri.parse(source), new BitmapUtils.BitmapLoadCallback() {
                    @Override
                    public void onLoadSuccess(CloseableReference<CloseableImage> reference, Bitmap bitmap) {
                        CanvasBitmap canvasBitmap = new CanvasBitmap(reference);
                        mImageCache.put(source, canvasBitmap);
                        BitmapDrawable drawable = new BitmapDrawable(null, canvasBitmap.get());
                        d.addLevel(1, 1, drawable);
                        float ratioWidth = (float) parentWidth / (float) bitmap.getWidth();
                        float ratioHeight = (float) parentHeight / (float) bitmap.getHeight();
                        if (ratioWidth >= 1 && ratioHeight >= 1) {
                            d.setBounds((parentWidth - bitmap.getWidth()) / 2, 0, bitmap.getWidth(), bitmap.getHeight());
                        } else {
                            float ration = Math.min(ratioWidth, ratioHeight);
                            int picWidth = (int) (bitmap.getWidth() * ration);
                            int picHeight = (int) (bitmap.getHeight() * ration);
                            d.setBounds((parentWidth - picWidth) / 2, 0, picWidth, picHeight);
                        }
                        d.setLevel(1);
                        CharSequence charSequence = getText();
                        setText(charSequence);
                        picCount--;
                        post(() -> spiltPage(parentHeight));
                    }

                    @Override
                    public void onLoadFailure() {
                        picCount--;
                        post(() -> spiltPage(parentHeight));
                    }
                });

                return d;
            };
        }
        setText(Html.fromHtml(mOriginText, mImageGetter, null));
        post(() -> spiltPage(parentHeight));
    }

    public void showContent() {
        if (TextUtils.isEmpty(curPageResetText)) {
            return;
        }
        setText(Html.fromHtml(curPageResetText, mImageGetter, null));
    }

    private void spiltPage(int parentHeight) {
        //确保页面切割在图片加载完成之后
        if (picCount > 0 || haveSplitContent) {
            return;
        }
        haveSplitContent = true;
        int lineCount = getLineCount();

        for (int i = 0; i < lineCount; i++) {
            int contentHeight = getLayout().getLineBottom(i);
            if (contentHeight > parentHeight) {
                int subLineIndex = (i == 0 ? i : (i - 1));
                String lastLineContent = getText().subSequence(getLayout().getLineStart(subLineIndex), getLayout().getLineEnd(subLineIndex)).toString().trim();
                //拿最后一页中最后一行的文字去索引原文中的位置
                int index = mOriginText.indexOf(lastLineContent);
                boolean shouldAdd = (subLineIndex == 0);
                boolean findContentInFirstTime = true;
                while (index == -1 || TextUtils.isEmpty(lastLineContent)) {
                    findContentInFirstTime = false;
                    //如果索引不到，说明是空行或者无法识别，则寻找上一行或者下一行再去索引
                    if (shouldAdd) {
                        subLineIndex++;
                    } else {
                        subLineIndex--;
                    }

                    lastLineContent = getText().subSequence(getLayout().getLineStart(subLineIndex), getLayout().getLineEnd(subLineIndex)).toString().trim();
                    index = mOriginText.indexOf(lastLineContent);
                }

                String nextPageText;
                //结尾标签处理
                if (!findContentInFirstTime && shouldAdd) {
                    nextPageText = mOriginText.substring(index);
                    curPageResetText = mOriginText.substring(0, index);
                    if (curPageResetText.endsWith(">")) {
                        int tagIndex = curPageResetText.lastIndexOf("<");
                        String tag = curPageResetText.substring(tagIndex);
                        curPageResetText = curPageResetText.substring(0, tagIndex);
                        nextPageText = tag + nextPageText;
                    }
                } else {
                    nextPageText = mOriginText.substring(index + lastLineContent.length());
                    curPageResetText = mOriginText.substring(0, index + lastLineContent.length());
                    if (nextPageText.startsWith("</")) {
                        int tagIndex = nextPageText.indexOf(">") + 1;
                        String tag = nextPageText.substring(0, tagIndex);
                        nextPageText = nextPageText.substring(tagIndex);
                        curPageResetText += tag;
                    }
                }
                setText(Html.fromHtml(curPageResetText, mImageGetter, null));
                mOriginText = curPageResetText;
                if (pageSplitCallback != null) {
                    pageSplitCallback.nextContent(nextPageText);
                }
                break;
            }
            if (i == lineCount - 1 && pageSplitCallback != null) {
                pageSplitCallback.onSplitPageEnd();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mImageCache.clear();
    }

    public interface PageSplitCallback {
        void nextContent(String text);

        void onSplitPageEnd();
    }
}
