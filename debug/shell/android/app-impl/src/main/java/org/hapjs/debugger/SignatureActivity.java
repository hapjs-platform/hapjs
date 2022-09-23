/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.hapjs.debugger.app.impl.R;
import org.hapjs.debugger.signutils.SignatureUtils;

import java.io.File;

public class SignatureActivity extends DraggableActivity {
    private static final String TAG = "SignatureActivity";

    private static final int REQUEST_CODE_PICK_APK = 0;
    private static final int REQUEST_CODE_PICK_INSTALLED_APP = 1;
    private static final int REQUEST_CODE_PICK_RPK = 2;
    private static final int REQUEST_CODE_PICK_PEM = 3;

    private static final int TYPE_APK = 0;
    private static final int TYPE_RPK = 1;
    private static final int TYPE_PEM = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.content, new GetSignMainFragment())
                .commit();
    }

    private static void displaySign(FragmentActivity activity, int type, String name, byte[] sign,
                                    Drawable icon, String smsHash) {
        new Handler(Looper.getMainLooper()).post(() ->
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content, new DisplaySignFragment(type, name, sign, icon, smsHash))
                        .addToBackStack(DisplaySignFragment.TAG)
                        .commit());
    }

    public static class DisplaySignFragment extends Fragment implements View.OnClickListener {
        private static final String TAG = "DisplaySignFragment";

        private int mType;
        private String mName;
        private byte[] mSign;
        private Drawable mIcon;
        private String mSmsHash;
        private String mSignSha256;
        private String mSignMd5;
        private String mSignStr;

        public DisplaySignFragment(int type, String name, byte[] sign, Drawable icon, String smsHash) {
            mType = type;
            mName = name;
            mSign = sign;
            mIcon = icon;
            mSmsHash = smsHash;

            mSignMd5 = SignatureUtils.getMd5(mSign);
            mSignSha256 = SignatureUtils.getSha256(mSign);
            mSignStr = Base64.encodeToString(mSign, Base64.NO_WRAP);
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View content = inflater.inflate(R.layout.display_sign_layout, container, false);

            if (mIcon != null) {
                ((ImageView) content.findViewById(R.id.icon)).setImageDrawable(mIcon);
            }
            content.findViewById(R.id.back).setOnClickListener(this);
            ((TextView) content.findViewById(R.id.app_label)).setText(mName);

            int titleResId = mType == TYPE_APK ? R.string.title_get_native_app_sign :
                    (mType == TYPE_RPK ? R.string.title_get_rpk_sign : R.string.title_get_pem_sign);

            ((TextView) content.findViewById(R.id.display_sign_title)).setText(getResources().getString(titleResId));
            ((TextView) content.findViewById(R.id.sign_md5)).setText(mSignMd5);
            ((TextView) content.findViewById(R.id.sign_sha256)).setText(mSignSha256);
            ((TextView) content.findViewById(R.id.sign)).setText(mSignStr);
            ((TextView) content.findViewById(R.id.sign)).setMovementMethod(ScrollingMovementMethod.getInstance());

            content.findViewById(R.id.copy_md5).setOnClickListener(this);
            content.findViewById(R.id.copy_sha256).setOnClickListener(this);
            content.findViewById(R.id.copy_sign).setOnClickListener(this);

            if (!TextUtils.isEmpty(mSmsHash)) {
                content.findViewById(R.id.sms_hash_layout).setVisibility(View.VISIBLE);
                ((TextView) content.findViewById(R.id.sms_hash)).setText(mSmsHash);
                content.findViewById(R.id.copy_sms_hash).setOnClickListener(this);
            }

            return content;
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.back) {
                getActivity().getSupportFragmentManager().popBackStack();
            } else if (id == R.id.copy_md5) {
                copy("md5", mSignMd5);
            } else if (id == R.id.copy_sha256) {
                copy("sha-256", mSignSha256);
            } else if (id == R.id.copy_sign) {
                copy("sign", mSignStr);
            } else if (id == R.id.copy_sms_hash) {
                copy("sms_hash", mSmsHash);
            }
        }

        private void copy(String label, String sign) {
            ClipboardManager cm = (ClipboardManager) getActivity().getSystemService(CLIPBOARD_SERVICE);
            ClipData data = new ClipData(label, new String[]{"text/plain"}, new ClipData.Item(sign));
            cm.setPrimaryClip(data);
            Toast.makeText(getActivity(), R.string.toast_copy_to_clipboard_succ, Toast.LENGTH_SHORT).show();
        }
    }

    public static class GetSignMainFragment extends Fragment {
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View content = inflater.inflate(R.layout.get_sign_main_layout, container, false);

            ViewPager2 pager = content.findViewById(R.id.pager);
            TabLayout tabLayout = content.findViewById(R.id.tab_layout);
            pager.setAdapter(new PagerAdapter(this));
            TabLayoutMediator mediator = new TabLayoutMediator(tabLayout, pager, (tab, position) -> {
                tab.setText(position == 0 ? R.string.tab_native_app
                        : (position == 1 ? R.string.tab_quick_app : R.string.tab_pem));
            });
            mediator.attach();

            return content;
        }

        private class PagerAdapter extends FragmentStateAdapter {

            public PagerAdapter(@NonNull Fragment fragment) {
                super(fragment);
            }

            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return position == 0 ? new GetNativeAppSignFragment()
                        : (position == 1 ? new GetQuickAppSignFragment() : new GetSignByPemFragment());
            }

            @Override
            public int getItemCount() {
                return 3;
            }
        }
    }

    public static class GetNativeAppSignFragment extends Fragment implements View.OnClickListener {
        private EditText mPkgEditText;

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View content = inflater.inflate(R.layout.get_native_app_sign_layout, container, false);

            TextView getSignBtn = content.findViewById(R.id.btn_get_sign);
            getSignBtn.setOnClickListener(this);
            content.findViewById(R.id.selected_install_app).setOnClickListener(this);
            content.findViewById(R.id.selected_apk).setOnClickListener(this);

            mPkgEditText = content.findViewById(R.id.pkg);
            View clearPkgBtn = content.findViewById(R.id.clear_pkg);
            clearPkgBtn.setOnClickListener(v -> mPkgEditText.setText(null));
            mPkgEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 0) {
                        getSignBtn.setEnabled(false);
                        clearPkgBtn.setVisibility(View.GONE);
                        setPaddingEnd(mPkgEditText, R.dimen.edit_text_hint_padding_end);
                    } else {
                        getSignBtn.setEnabled(true);
                        clearPkgBtn.setVisibility(View.VISIBLE);
                        setPaddingEnd(mPkgEditText, R.dimen.edit_text_user_input_padding_end);
                    }
                }
            });
            mPkgEditText.setOnFocusChangeListener((v, hasFocus) ->
                    clearPkgBtn.setVisibility(mPkgEditText.length() > 0 && hasFocus ? View.VISIBLE : View.GONE));

            return content;
        }

        private static void setPaddingEnd(View view, int paddingEndRes) {
            view.setPadding(view.getPaddingStart(), view.getPaddingTop(),
                    (int) view.getResources().getDimension(paddingEndRes), view.getPaddingBottom());
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.btn_get_sign) {
                getSignatureByPkgName(getActivity(), mPkgEditText.getText().toString());
            } else if (id == R.id.selected_apk) {
                pickApk();
            } else if (id == R.id.selected_install_app) {
                pickInstalledApp();
            }
        }

        private void pickApk() {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            getActivity().startActivityForResult(intent, REQUEST_CODE_PICK_APK);
        }

        private void pickInstalledApp() {
            Intent intent = new Intent(getActivity(), InstalledAppActivity.class);
            getActivity().startActivityForResult(intent, REQUEST_CODE_PICK_INSTALLED_APP);
        }
    }

    public static class GetQuickAppSignFragment extends Fragment {
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View content = inflater.inflate(R.layout.get_rpk_sign_layout, container, false);
            content.findViewById(R.id.selected_rpk).setOnClickListener(v -> pickRpk());
            return content;
        }

        private void pickRpk() {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            getActivity().startActivityForResult(intent, REQUEST_CODE_PICK_RPK);
        }
    }

    public static class GetSignByPemFragment extends Fragment {
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View content = inflater.inflate(R.layout.get_pem_sign_layout, container, false);
            content.findViewById(R.id.selected_pem).setOnClickListener(v -> pickPem());
            return content;
        }

        private void pickPem() {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            getActivity().startActivityForResult(intent, REQUEST_CODE_PICK_PEM);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_PICK_APK:
                if (resultCode == Activity.RESULT_OK) {
                    getSignatureFromApk(data.getData());
                }
                break;
            case REQUEST_CODE_PICK_INSTALLED_APP:
                if (resultCode == Activity.RESULT_OK) {
                    String pkg = data.getStringExtra(InstalledAppActivity.RESULT_PKG);
                    getSignatureByPkgName(this, pkg);
                    ;
                }
                break;
            case REQUEST_CODE_PICK_PEM:
                if (resultCode == Activity.RESULT_OK) {
                    getSignatureFromPem(data.getData());
                }
                break;
            case REQUEST_CODE_PICK_RPK:
                if (resultCode == Activity.RESULT_OK) {
                    getSignatureFromRpk(data.getData());
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private static void getSignatureByPkgName(FragmentActivity activity, String pkgName) {
        if (TextUtils.isEmpty(pkgName)) {
            Toast.makeText(activity, R.string.toast_pkg_name_null, Toast.LENGTH_SHORT).show();
            return;
        }

        PackageInfo info = SignatureUtils.getNativePackageInfo(activity, pkgName);
        getSignature(activity, info);
    }

    private void getSignatureFromApk(Uri uri) {
        PackageInfo info = SignatureUtils.getNativePackageInfo(this, uri);
        getSignature(this, info);
    }

    private void getSignatureFromPem(Uri uri) {
        String path = uri.getPath();
        if (path != null) {
            if (path.endsWith(".pem") || path.endsWith(".PEM")) {
                String name = path.substring(path.lastIndexOf("/") + 1);
                byte[] signature = SignatureUtils.getSignatureFromPem(this, uri);
                if (signature != null) {
                    displaySign(this, TYPE_PEM, name, signature, null, null);
                    return;
                }
            }
        }
        Toast.makeText(this, R.string.toast_invalid_pem, Toast.LENGTH_SHORT).show();
    }

    private void getSignatureFromRpk(Uri uri) {
        Pair<org.hapjs.debugger.pm.PackageInfo, File> pi = SignatureUtils.getHybridPackageInfo(this, uri);
        if (pi != null && pi.first != null) {
            String smsHash = SignatureUtils.getSMSHash(pi.first.getPackage(), pi.first.getSignature());
            displaySign(this, TYPE_RPK, pi.first.getName(), pi.first.getSignature(),
                    getIconDrawable(pi.second), smsHash);
        } else {
            Toast.makeText(this, R.string.toast_pkg_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    private Drawable getIconDrawable(File iconFile) {
        if (iconFile == null) {
            return null;
        }

        Bitmap bitmap = BitmapFactory.decodeFile(iconFile.getAbsolutePath());
        if (bitmap == null) {
            Log.e(TAG, "failed to decode icon");
            return null;
        }
        return new BitmapDrawable(bitmap);
    }

    private static void getSignature(FragmentActivity activity, PackageInfo info) {
        if (info != null) {
            byte[] signature = info.signatures[0].toByteArray();
            CharSequence label = activity.getPackageManager().getApplicationLabel(info.applicationInfo);
            Drawable icon = info.applicationInfo.loadIcon(activity.getPackageManager());
            String appName = TextUtils.isEmpty(label) ? info.packageName : label.toString();
            String smsHash = SignatureUtils.getSMSHash(info.packageName, signature);
            displaySign(activity, TYPE_APK, appName, signature, icon, smsHash);
        } else {
            Toast.makeText(activity, R.string.toast_pkg_not_found, Toast.LENGTH_SHORT).show();
        }
    }

}
