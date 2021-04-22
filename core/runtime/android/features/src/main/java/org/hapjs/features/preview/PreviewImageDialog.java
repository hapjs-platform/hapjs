/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.preview;

import android.Manifest;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.facebook.binaryresource.FileBinaryResource;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.common.util.UriUtil;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.imagepipeline.image.ImageInfo;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.relex.photodraweeview.OnPhotoTapListener;
import me.relex.photodraweeview.PhotoDraweeView;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.permission.HapPermissionManager;
import org.hapjs.bridge.permission.PermissionCallback;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.common.utils.LogUtils;
import org.hapjs.common.utils.MediaUtils;
import org.hapjs.common.utils.ThemeUtils;
import org.hapjs.features.R;
import org.hapjs.runtime.DefaultHybridDialogProviderImpl;
import org.hapjs.runtime.HybridDialog;
import org.hapjs.runtime.HybridDialogProvider;
import org.hapjs.runtime.ProviderManager;

public class PreviewImageDialog extends Dialog {

    private static final String TAG = "PreviewImageDialog";

    private static final int BACKGROUND = 0x00000000;
    private static final String MIME_PREFFIX_IMAGE = "image";
    private static final String MIME_PREFFIX_VIDEO = "video";
    private static final float DEFAULT_MAX_SCALE = 4.0f;
    private static final float DEFAULT_MID_SCALE = 2.0f;
    private static final float DEFAULT_MIN_SCALE = 1.0f;
    private static final long ZOOM_DURATION = 200L;
    private static final int ITEM_SAVE_PICTURES = 0;
    private static final int ITEM_SHARE_PICTURES = 1;
    private static final int ITEM_CANCEL = 2;
    private static final String PREFIX_DEFAULT_NAME = "quick_";

    private PreviewViewPager mViewPager;
    private TextView mTvIndicator;
    private ImageViewPagerAdapter mAdapter;
    private Context mContext;
    private ApplicationContext mAppContext;
    private int mCurrentIndex = 0;
    private List<String> mImageViewUriLists = new ArrayList<>();

    private Request mRequest;
    private ViewPager.OnPageChangeListener mPageChangeListener =
            new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset,
                                           int positionOffsetPixels) {
                }

                @Override
                public void onPageSelected(int position) {
                    updateIndicator(position + 1, mAdapter.getCount());
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                }
            };

    public PreviewImageDialog(Context context) {
        this(context, 0);
    }

    public PreviewImageDialog(Context context, int theme) {
        super(context, theme);
        this.mContext = context;
        applyCompat();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setDecorViewUiFlag();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_preview);
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(BACKGROUND));
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
        }

        mViewPager = findViewById(R.id.preview_vp_image);
        mTvIndicator = findViewById(R.id.preview_tv_indicator);

        mAdapter = new ImageViewPagerAdapter();
        mViewPager.setAdapter(mAdapter);
        mViewPager.addOnPageChangeListener(mPageChangeListener);
        mViewPager.setPageMargin(
                mContext.getResources().getDimensionPixelSize(R.dimen.viewpager_page_margin));

        mViewPager.setCurrentItem(mCurrentIndex);
        mAdapter.notifyDataSetChanged();
        setOnDismissListener(
                new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        restoreDecorViewUiFlag();
                    }
                });
    }

    private void applyCompat() {
        if (Build.VERSION.SDK_INT < 19) {
            return;
        }
        if (getWindow() != null) {
            getWindow()
                    .setFlags(
                            WindowManager.LayoutParams.FLAG_FULLSCREEN,
                            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            Log.w(TAG, "applyCompat: getWindow() is null");
        }
    }

    private void setDecorViewUiFlag() {
        if (this.getWindow() != null) {
            View decorView = this.getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    decorView.getSystemUiVisibility()
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void restoreDecorViewUiFlag() {
        if (this.getWindow() != null) {
            View decorView = this.getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    decorView.getSystemUiVisibility()
                            & ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            & ~View.SYSTEM_UI_FLAG_FULLSCREEN
                            & ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void show() {
        if (getWindow() != null) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            if (getWindow().getDecorView() != null) {
                getWindow()
                        .getDecorView()
                        .setSystemUiVisibility(
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            }
        }
        super.show();
    }

    public void setParams(ApplicationContext applicationContext, int current, List<String> uris) {
        this.mAppContext = applicationContext;
        this.mCurrentIndex = current;
        this.mImageViewUriLists.addAll(uris);
        if (null != mViewPager) {
            mViewPager.setCurrentItem(mCurrentIndex);
            mAdapter.notifyDataSetChanged();
        }
    }

    public void setRequest(Request request) {
        mRequest = request;
    }

    private void updateIndicator(int current, int size) {
        mTvIndicator.setText(current + "/" + size);
    }

    private class ImageViewPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return mImageViewUriLists.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_preview_photo, null);
            final PhotoDraweeView imageView = view.findViewById(R.id.preview_item_pre_photo_view);
            final Uri uri = getUri(mImageViewUriLists.get(position));

            PipelineDraweeControllerBuilder controller =
                    Fresco.newDraweeControllerBuilder()
                            .setUri(uri)
                            .setOldController(imageView.getController())
                            .setControllerListener(
                                    new BaseControllerListener<ImageInfo>() {
                                        @Override
                                        public void onFinalImageSet(
                                                String id, ImageInfo imageInfo,
                                                Animatable animatable) {
                                            super.onFinalImageSet(id, imageInfo, animatable);
                                            if (imageInfo == null || imageView == null) {
                                                return;
                                            }
                                            imageView.update(imageInfo.getWidth(),
                                                    imageInfo.getHeight());
                                        }

                                        @Override
                                        public void onFailure(String id, Throwable throwable) {
                                            super.onFailure(id, throwable);
                                        }

                                        @Override
                                        public void onIntermediateImageFailed(String id,
                                                                              Throwable throwable) {
                                            super.onIntermediateImageFailed(id, throwable);
                                        }
                                    });
            imageView.setController(controller.build());
            imageView.setMaximumScale(DEFAULT_MAX_SCALE);
            imageView.setMediumScale(DEFAULT_MID_SCALE);
            imageView.setMinimumScale(DEFAULT_MIN_SCALE);
            imageView.setZoomTransitionDuration(ZOOM_DURATION);
            imageView.setOnPhotoTapListener(
                    new OnPhotoTapListener() {
                        @Override
                        public void onPhotoTap(View view, float x, float y) {
                            PreviewImageDialog.this.dismiss();
                        }
                    });
            imageView.setOnLongClickListener(
                    new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            try {
                                FileBinaryResource resource;
                                if (uri.getScheme() != null
                                        && UriUtil.LOCAL_FILE_SCHEME.equals(uri.getScheme())) {
                                    resource = FileBinaryResource
                                            .createOrNull(new File(new URI(uri.toString())));
                                } else {
                                    resource =
                                            (FileBinaryResource)
                                                    Fresco.getImagePipelineFactory()
                                                            .getMainFileCache()
                                                            .getResource(new SimpleCacheKey(
                                                                    mImageViewUriLists
                                                                            .get(position)));
                                }
                                if (null != resource) {
                                    longClick(uri, resource);
                                    return true;
                                }
                            } catch (URISyntaxException e) {
                                Log.e(TAG, "URISyntaxException: ", e);
                            }
                            return false;
                        }
                    });

            container.addView(view);
            return view;
        }

        private Uri getUri(String path) {
            if (path.toLowerCase().startsWith("https") || path.toLowerCase().startsWith("http")) {
                return Uri.parse(path);

            } else if (path.toLowerCase().startsWith("/")) {
                return new Uri.Builder().path(path).scheme(UriUtil.LOCAL_FILE_SCHEME).build();

            } else {
                Uri uri = mAppContext.getUnderlyingUri(path);
                if (uri != null) {
                    return uri;
                } else {
                    return Uri.parse(path);
                }
            }
        }

        private void longClick(final Uri uri, final FileBinaryResource resource) {
            HybridDialogProvider provider = getQuickAppDialogProvider();

            HybridDialog dialog =
                    provider.createAlertDialog(mContext, ThemeUtils.getAlertDialogTheme());
            dialog.setItems(
                    R.array.item_preview,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (i == ITEM_SAVE_PICTURES) {
                                saveToPhotoAlbum(uri, resource);
                            } else if (i == ITEM_SHARE_PICTURES) {
                                share(uri, resource);
                            } else {
                                dialogInterface.dismiss();
                            }
                        }
                    });
            dialog.setCancelable(true);
            dialog.show();
        }

        private HybridDialogProvider getQuickAppDialogProvider() {
            HybridDialogProvider provider =
                    ProviderManager.getDefault().getProvider(HybridDialogProvider.NAME);
            if (provider == null) {
                provider = new DefaultHybridDialogProviderImpl();
            }
            return provider;
        }

        private void saveToPhotoAlbum(Uri uri, FileBinaryResource resource) {
            HapPermissionManager.getDefault()
                    .requestPermissions(
                            mRequest.getView().getHybridManager(),
                            new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            new PermissionCallback() {
                                @Override
                                public void onPermissionAccept() {
                                    boolean success = realSavePhoto(uri, resource);
                                    Executors.ui()
                                            .execute(
                                                    () -> {
                                                        if (success) {
                                                            Toast.makeText(
                                                                    mContext,
                                                                    R.string.preview_save_successfully,
                                                                    Toast.LENGTH_SHORT)
                                                                    .show();
                                                        } else {
                                                            Toast.makeText(
                                                                    mContext,
                                                                    R.string.preview_save_failed,
                                                                    Toast.LENGTH_SHORT)
                                                                    .show();
                                                        }
                                                    });
                                }

                                @Override
                                public void onPermissionReject(int reason, boolean dontDisturb) {
                                    Executors.ui()
                                            .execute(
                                                    () ->
                                                            Toast.makeText(
                                                                    mContext,
                                                                    R.string.preview_save_failed,
                                                                    Toast.LENGTH_SHORT)
                                                                    .show());
                                    Log.d(TAG, "onPermissionReject: " + reason);
                                }
                            });
        }

        private boolean realSavePhoto(Uri uri, FileBinaryResource resource) {
            String fileName = getFileName(uri, resource);
            if (TextUtils.isEmpty(fileName)) {
                return false;
            }
            String mimeType = URLConnection.guessContentTypeFromName(fileName);
            if (TextUtils.isEmpty(mimeType) || !mimeType.startsWith(MIME_PREFFIX_IMAGE)) {
                return false;
            }

            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                File dir = getSaveDir(mAppContext.getContext().getPackageName(), mimeType);
                if (!dir.exists() && !FileUtils.mkdirs(dir)) {
                    return false;
                }

                File originalFile = resource.getFile();
                File targetFile = new File(dir, fileName);
                if (FileUtils.copyFile(originalFile, targetFile)) {
                    notifyMediaScanner(mContext, targetFile);
                    return true;
                }
            } else {
                String relativeFolderName =
                        File.separator + mAppContext.getContext().getPackageName();
                ContentResolver resolver = mAppContext.getContext().getContentResolver();
                Uri saveContentUri =
                        getSaveContentUri(resolver, fileName, relativeFolderName, mimeType);
                if (saveContentUri == null) {
                    return false;
                }
                try (OutputStream output = resolver.openOutputStream(saveContentUri);
                        InputStream input = resource.openStream()) {
                    if (input == null || output == null) {
                        return false;
                    } else {
                        byte[] buffer = new byte[4 * 1024];
                        for (int length; (length = input.read(buffer)) != -1; ) {
                            output.write(buffer, 0, length);
                        }
                        return true;
                    }
                } catch (Exception e) {
                    return false;
                }
            }
            return false;
        }

        private void share(Uri uri, FileBinaryResource resource) {
            File originalFile = resource.getFile();
            String fileName = getFileName(uri, resource);
            if (!TextUtils.isEmpty(fileName)) {
                File targetFile = new File(mAppContext.getMassDir(), fileName);
                if (FileUtils.copyFile(originalFile, targetFile)) {
                    Uri imageUri = FileProvider.getUriForFile(mContext,
                            mAppContext.getContext().getPackageName() + ".file", targetFile);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        imageUri =  MediaUtils.getMediaContentUri(mContext,
                                mAppContext.getContext().getPackageName(), "image/*", imageUri);
                    }

                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    shareIntent.setType("image/*");
                    mContext.startActivity(
                            Intent.createChooser(shareIntent,
                                    mContext.getString(R.string.preview_share_to)));
                } else {
                    Toast.makeText(mContext, R.string.preview_share_failed, Toast.LENGTH_SHORT)
                            .show();
                }
            } else {
                Toast.makeText(mContext, R.string.preview_share_failed, Toast.LENGTH_SHORT).show();
            }
        }

        private String getFileName(Uri uri, FileBinaryResource resource) {
            BufferedInputStream bufferedInputStream = null;
            try {
                bufferedInputStream = new BufferedInputStream(resource.openStream());
                String fileType;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    fileType = URLConnection.guessContentTypeFromStream(bufferedInputStream);
                } else {
                    fileType = guessContentTypeFromStream(bufferedInputStream);
                }
                if (!TextUtils.isEmpty(fileType)) {
                    String extension = fileType.substring(fileType.lastIndexOf("/") + 1);
                    File file = new File(uri.getPath());
                    if (null != file) {
                        String fileName = file.getName();
                        if (checkValidate(fileName)) {
                            return fileName;
                        }
                    }
                    return PREFIX_DEFAULT_NAME + System.currentTimeMillis() + "." + extension;
                } else {
                    Log.e(TAG, "file type is empty.");
                }
            } catch (IOException e) {
                Log.e(TAG, "io exception.");
            } finally {
                if (null != bufferedInputStream) {
                    try {
                        bufferedInputStream.close();
                    } catch (IOException e) {
                        Log.e(TAG, "close io exception.");
                    }
                }
            }
            return null;
        }

        private File getSaveDir(String pkgName, String mimeType) {
            String dirType = Environment.DIRECTORY_DOCUMENTS;
            if (!TextUtils.isEmpty(mimeType)) {
                if (mimeType.startsWith(MIME_PREFFIX_IMAGE)) {
                    dirType = Environment.DIRECTORY_PICTURES;
                } else if (mimeType.startsWith(MIME_PREFFIX_VIDEO)) {
                    dirType = Environment.DIRECTORY_MOVIES;
                }
            }
            File parentDir = Environment.getExternalStoragePublicDirectory(dirType);
            return new File(parentDir, pkgName);
        }

        private void notifyMediaScanner(Context context, File file) {
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(file));
            context.sendBroadcast(intent);
        }

        private boolean checkValidate(String fileName) {
            if (!TextUtils.isEmpty(fileName)) {
                int dotIndex = fileName.lastIndexOf(".");
                if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                    String reg = ".+(.jpeg|.jpg|.png|.gif|.webp)$";
                    Matcher matcher =
                            Pattern.compile(reg, Pattern.CASE_INSENSITIVE)
                                    .matcher(fileName.substring(dotIndex));
                    return matcher.find();
                }
            }
            return false;
        }

        private String guessContentTypeFromStream(InputStream is) throws IOException {
            // If we can't read ahead safely, just give up on guessing
            if (!is.markSupported()) {
                return null;
            }

            is.mark(16);
            int c1 = is.read();
            int c2 = is.read();
            int c3 = is.read();
            int c4 = is.read();
            int c5 = is.read();
            int c6 = is.read();
            int c7 = is.read();
            int c8 = is.read();
            int c9 = is.read();
            int c10 = is.read();
            int c11 = is.read();
            int c12 = is.read();
            int c13 = is.read();
            int c14 = is.read();
            int c15 = is.read();
            int c16 = is.read();
            is.reset();

            if (c1 == 'G' && c2 == 'I' && c3 == 'F' && c4 == '8') {
                return "image/gif";
            }

            if (c1 == '#' && c2 == 'd' && c3 == 'e' && c4 == 'f') {
                return "image/x-bitmap";
            }

            if (c1 == '!' && c2 == ' ' && c3 == 'X' && c4 == 'P' && c5 == 'M' && c6 == '2') {
                return "image/x-pixmap";
            }

            if (c1 == 137 && c2 == 80 && c3 == 78 && c4 == 71 && c5 == 13 && c6 == 10 && c7 == 26
                    && c8 == 10) {
                return "image/png";
            }

            if (c1 == 0xFF && c2 == 0xD8 && c3 == 0xFF) {
                if (c4 == 0xE0 || c4 == 0xEE) {
                    return "image/jpeg";
                }

                /**
                 * File format used by digital cameras to store images. Exif Format can be read by any
                 * application supporting JPEG. Exif Spec can be found at:
                 * http://www.pima.net/standards/it10/PIMA15740/Exif_2-1.PDF
                 */
                if ((c4 == 0xE1)
                        && (c7 == 'E' && c8 == 'x' && c9 == 'i' && c10 == 'f' && c11 == 0)) {
                    return "image/jpeg";
                }
            }
            return null;
        }

        @RequiresApi(api = Build.VERSION_CODES.Q)
        private Uri getSaveContentUri(
                ContentResolver resolver, String fileName, String relativeFolderName,
                String mimeType) {
            ContentValues values = new ContentValues();
            Uri external;
            if (mimeType.startsWith(MIME_PREFFIX_IMAGE)) {
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
                values.put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + relativeFolderName);
                external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if (mimeType.startsWith(MIME_PREFFIX_VIDEO)) {
                values.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Video.Media.MIME_TYPE, mimeType);
                values.put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_MOVIES + relativeFolderName);
                external = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            } else {
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
                values.put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOCUMENTS + relativeFolderName);
                external = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
            }

            if (external != null && values.size() != 0) {
                return resolver.insert(external, values);
            }
            return null;
        }
    }
}
