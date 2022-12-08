/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.input;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaNode;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hapjs.bridge.annotation.TypeAnnotation;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.executors.AbsTask;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.SwipeObserver;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.utils.YogaUtil;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.render.Page;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.R;
import org.hapjs.widgets.input.phone.PhoneManager;
import org.hapjs.widgets.text.TypefaceBuilder;
import org.hapjs.widgets.view.text.FlexEditText;
import org.hapjs.widgets.view.text.TextSpan;

@WidgetAnnotation(
        name = Edit.WIDGET_NAME,
        types = {
                @TypeAnnotation(name = Edit.TYPE_TEXT, isDefault = true),
                @TypeAnnotation(name = Edit.TYPE_DATE),
                @TypeAnnotation(name = Edit.TYPE_TIME),
                @TypeAnnotation(name = Edit.TYPE_EMAIL),
                @TypeAnnotation(name = Edit.TYPE_NUMBER),
                @TypeAnnotation(name = Edit.TYPE_PASSWORD),
                @TypeAnnotation(name = Edit.TYPE_TELEPHONE)
        },
        methods = {
                Component.METHOD_FOCUS,
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Edit.METHOD_SELECT,
                Edit.METHOD_SET_SELECTION_RANGE,
                Edit.METHOD_GET_SELECTION_RANGE,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class Edit extends Component<TextView> implements SwipeObserver {
    protected static final String WIDGET_NAME = "input";
    protected static final String METHOD_SELECT = "select";
    protected static final String METHOD_SET_SELECTION_RANGE = "setSelectionRange";
    protected static final String METHOD_GET_SELECTION_RANGE = "getSelectionRange";
    protected static final String TYPE_TEXT = "text";
    protected static final String TYPE_DATE = "date";
    protected static final String TYPE_TIME = "time";
    protected static final String TYPE_EMAIL = "email";
    protected static final String TYPE_NUMBER = "number";
    protected static final String TYPE_PASSWORD = "password";
    protected static final String TYPE_TELEPHONE = "tel";
    protected static final String DEFAULT_FONT_SIZE = "37.5px";
    protected static final String DEFAULT_COLOR = "#de000000";
    protected static final String DEFAULT_PLACEHOLDER_COLOR = "#61000000";
    protected static final String DEFAULT_WIDTH = "150px";
    private static final String TAG = "Edit";
    // style
    private static final String KEY_TEXT = "text";
    private static final String KEY_CHANGE_EVENT_STATE = "change_event_state";
    private static final String MAX_LENGTH = "maxlength";
    private static final String ATTR_ENTER_KEY_TYPE = "enterkeytype";
    private static final String ATTR_AUTO_COMPLETE = "autocomplete";
    private static final String ATTR_CARET_COLOR = "caretColor";
    private static final String EVENT_ENTER_KEY_CLICK = "enterkeyclick";
    private static final String EVENT_SELECTION_CHANGE = "selectionchange";
    private String mInputType = Attributes.InputType.TEXT;
    private int mEditorAction = EditorInfo.IME_ACTION_UNSPECIFIED;
    private EditTextWatcher mTextWatcher;
    private TextView.OnEditorActionListener mEditorActionListener;
    private FlexEditText.SelectionChangeListener mSelectionChangeListener;
    private boolean mNeedToggleFocus = false;
    private boolean mIsChangeEventRegistered = false;

    private TextWatcherRunnable mTextWatcherRunnable = new TextWatcherRunnable();

    private TextSpan mTextSpan = new TextSpan();

    private View.OnFocusChangeListener mFocusChangeListener;
    private PhoneAdapter mPhoneAdapter;
    private ToggleFocusRunnable mToggleFocus = new ToggleFocusRunnable();

    public Edit(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected TextView createViewImpl() {
        FlexEditText editText = new FlexEditText(mContext,isEnableTalkBack());
        editText.setComponent(this);

        initDefaultView(editText);

        setInputType(mInputType, editText);
        toggleFocus();

        return editText;
    }

    protected void initDefaultView(TextView view) {
        Page page = initFontLevel();
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, Attributes.getFontSize(mHapEngine, page, DEFAULT_FONT_SIZE, this));
        view.setTextColor(ColorUtil.getColor(DEFAULT_COLOR));
        view.setHintTextColor(ColorUtil.getColor(DEFAULT_PLACEHOLDER_COLOR));
        view.setBackground(null);
        view.setSingleLine();
        view.setGravity(Gravity.CENTER_VERTICAL);

        int minWidth = Attributes.getInt(mHapEngine, DEFAULT_WIDTH, this);
        view.setMinWidth(minWidth);
        view.setMinimumWidth(minWidth);
        setTextWatcher(view);
    }

    void setTextWatcher(TextView view) {
        if (mTextWatcher == null) {
            mTextWatcher = new EditTextWatcher(this);
        }
        view.removeTextChangedListener(mTextWatcher);
        view.addTextChangedListener(mTextWatcher);
    }

    public boolean isChangeEventRegistered() {
        return mIsChangeEventRegistered
                || mEventDomData.contains(Attributes.Event.CHANGE)
                || (getBoundRecyclerItem() != null
                && getBoundRecyclerItem().getDomEvents() != null
                && getBoundRecyclerItem().getDomEvents().contains(Attributes.Event.CHANGE));
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case ATTR_ENTER_KEY_TYPE:
                String enterKeyType = Attributes.getString(attribute);
                setEnterKeyType(enterKeyType);
                return true;
            case Attributes.Style.TYPE:
                String type = Attributes.getString(attribute);
                setInputType(type);
                return true;
            case Attributes.Style.PLACEHOLDER:
                String placeholder = Attributes.getString(attribute, "");
                setPlaceholder(placeholder);
                return true;
            case Attributes.Style.PLACEHOLDER_COLOR:
                String placeholderColorStr =
                        Attributes.getString(attribute, getDefaultPlaceholderColor());
                setPlaceholderColor(placeholderColorStr);
                return true;
            case Attributes.Style.COLOR:
                String colorStr = Attributes.getString(attribute, getDefaultColor());
                setColor(colorStr);
                return true;
            case Attributes.Style.FONT_SIZE:
                int defaultFontSize = Attributes.getFontSize(mHapEngine, getPage(), getDefaultFontSize(), this);
                int fontSize = Attributes.getFontSize(mHapEngine, getPage(), attribute, defaultFontSize, this);
                setFontSize(fontSize);
                return true;
            case Attributes.Style.LINE_HEIGHT:
                if (mAutoLineHeight) {
                    int lineHeight = Attributes.getFontSize(mHapEngine, getPage(), attribute, -1, this);
                    setLineHeight(lineHeight);
                } else {
                    int lineHeight = Attributes.getInt(mHapEngine, attribute, -1, this);
                    setLineHeight(lineHeight);
                }
                return true;
            case Attributes.Style.AUTO_LINE_HEIGHT:
                boolean autoLineHeight = Attributes.getBoolean(attribute, false);
                mAutoLineHeight = autoLineHeight;
                return true;
            case Attributes.Style.FONT_STYLE:
                String fontStyleStr = Attributes.getString(attribute, "normal");
                setFontStyle(fontStyleStr);
                return true;
            case Attributes.Style.FONT_WEIGHT:
                String fontWeightStr = Attributes.getString(attribute, "normal");
                setFontWeight(fontWeightStr);
                return true;
            case Attributes.Style.TEXT_ALIGN:
                String textAlignStr = Attributes.getString(attribute, "left");
                setTextAlign(textAlignStr);
                return true;
            case Attributes.Style.VALUE:
            case Attributes.Style.CONTENT:
                String text = Attributes.getString(attribute, "");
                setText(text);
                return true;
            case MAX_LENGTH:
                int maxLength = Attributes.getInt(mHapEngine, attribute, -1);
                setMaxLength(maxLength);
                return true;
            case ATTR_AUTO_COMPLETE:
                String autoCompleteStr =
                        Attributes.getString(attribute, Attributes.AutoComplete.ON);
                setAutoComplete(autoCompleteStr);
                return true;
            case ATTR_CARET_COLOR:
                String caretColorStr = Attributes.getString(attribute, getDefaultColor());
                setCaretColor(caretColorStr);
                return true;
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        if (Attributes.Event.CHANGE.equals(event)) {
            mIsChangeEventRegistered = true;
            return true;
        } else if (EVENT_ENTER_KEY_CLICK.equals(event)) {
            if (mEditorActionListener == null) {
                mEditorActionListener =
                        new TextView.OnEditorActionListener() {
                            @Override
                            public boolean onEditorAction(TextView v, int actionId,
                                                          KeyEvent event) {
                                Map<String, Object> params = new HashMap<>(1);
                                params.put("value", v.getText().toString());
                                mCallback.onJsEventCallback(
                                        getPageId(), mRef, EVENT_ENTER_KEY_CLICK, Edit.this, params,
                                        null);
                                boolean consumed = true;
                                if (actionId == EditorInfo.IME_ACTION_NEXT
                                        || actionId == EditorInfo.IME_ACTION_PREVIOUS
                                        || actionId == EditorInfo.IME_ACTION_DONE) {
                                    consumed = false;
                                }
                                return consumed;
                            }
                        };
            }
            mHost.setOnEditorActionListener(mEditorActionListener);
        } else if (EVENT_SELECTION_CHANGE.equals(event)) {
            if (mSelectionChangeListener == null) {
                mSelectionChangeListener =
                        new FlexEditText.SelectionChangeListener() {
                            @Override
                            public void onSelectionChange(int selStart, int selEnd) {
                                mCallback.onJsEventCallback(
                                        getPageId(), mRef, EVENT_SELECTION_CHANGE, Edit.this, null,
                                        null);
                            }
                        };
            }
            ((FlexEditText) mHost).setOnSelectionChangeListener(mSelectionChangeListener);
        }

        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        if (Attributes.Event.CHANGE.equals(event)) {
            mIsChangeEventRegistered = false;
            return true;
        } else if (EVENT_ENTER_KEY_CLICK.equals(event)) {
            mHost.setOnEditorActionListener(null);
        } else if (EVENT_SELECTION_CHANGE.equals(event)) {
            ((FlexEditText) mHost).setOnSelectionChangeListener(null);
        }

        return super.removeEvent(event);
    }

    protected int getDefaultVerticalGravity() {
        if (mHost.getLineCount() == 1) {
            return Gravity.CENTER_VERTICAL;
        } else {
            return Gravity.TOP;
        }
    }

    public void setInputType(String inputType) {
        if (TextUtils.isEmpty(inputType)) {
            return;
        }
        mInputType = inputType;

        if (mHost == null) {
            return;
        }

        setInputType(inputType, mHost);
    }

    private void setInputType(String inputType, TextView editText) {
        if (Attributes.InputType.DATE.equals(inputType)) {
            editText.setInputType(
                    InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE);
        } else if (Attributes.InputType.TIME.equals(inputType)) {
            editText.setInputType(
                    InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        } else if (Attributes.InputType.EMAIL.equals(inputType)) {
            editText.setInputType(
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        } else if (Attributes.InputType.NUMBER.equals(inputType)) {
            editText.setKeyListener(DigitsKeyListener.getInstance("0123456789.+-eE"));
        } else if (Attributes.InputType.PASSWORD.equals(inputType)) {
            editText.setInputType(
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else if (Attributes.InputType.TEXT.equals(inputType)) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT);
        } else if (Attributes.InputType.TELEPHONE.equals(inputType)) {
            editText.setKeyListener(DigitsKeyListener.getInstance("0123456789+"));
        }
        setSupportAutoComplete(Attributes.InputType.TELEPHONE.equals(inputType));
    }

    private void setEnterKeyType(String type) {
        if (mHost == null) {
            return;
        }
        switch (type) {
            case EnterKeyType.DEFAULT:
                mEditorAction = EditorInfo.IME_ACTION_UNSPECIFIED;
                break;
            case EnterKeyType.SEND:
                mEditorAction = EditorInfo.IME_ACTION_SEND;
                break;
            case EnterKeyType.SEARCH:
                mEditorAction = EditorInfo.IME_ACTION_SEARCH;
                break;
            case EnterKeyType.NEXT:
                mEditorAction = EditorInfo.IME_ACTION_NEXT;
                break;
            case EnterKeyType.GO:
                mEditorAction = EditorInfo.IME_ACTION_GO;
                break;
            case EnterKeyType.DONE:
                mEditorAction = EditorInfo.IME_ACTION_DONE;
                break;
            default:
                mEditorAction = EditorInfo.IME_ACTION_UNSPECIFIED;
                break;
        }
        mHost.setImeOptions(mEditorAction);
    }

    private void setSupportAutoComplete(boolean supported) {
        if (supported && mFocusChangeListener == null) {
            mFocusChangeListener = new FocusChangeListener(this);
            addOnFocusChangeListener(mFocusChangeListener);
        } else if (!supported && mFocusChangeListener != null) {
            removeOnFocusChangeListener(mFocusChangeListener);
            mFocusChangeListener = null;
        }
        if (!supported && mHost instanceof FlexEditText) {
            ((FlexEditText) mHost).setAdapter(null);
        }
    }

    public void setPlaceholder(String placeholder) {
        if (placeholder == null || mHost == null) {
            return;
        }

        mHost.setHint(placeholder);
    }

    public void setPlaceholderColor(String placeholderColorStr) {
        if (TextUtils.isEmpty(placeholderColorStr) || mHost == null) {
            return;
        }

        int placeholderColor = ColorUtil.getColor(placeholderColorStr);
        mHost.setHintTextColor(placeholderColor);
    }

    public void setColor(String colorStr) {
        if (TextUtils.isEmpty(colorStr) || mHost == null) {
            return;
        }

        int color = ColorUtil.getColor(colorStr);
        mHost.setTextColor(color);
    }

    public void setFontSize(int fontSize) {
        if (fontSize <= 0 || mHost == null) {
            return;
        }

        mHost.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize);
    }

    public void setLineHeight(int lineHeight) {
        if (lineHeight <= 0 || mHost == null) {
            return;
        }

        mTextSpan.setLineHeight(lineHeight);
        updateSpannable(mHost.getText().toString());
    }

    public void setFontStyle(String fontStyleStr) {
        if (TextUtils.isEmpty(fontStyleStr) || mHost == null) {
            return;
        }

        int fontStyle = Typeface.NORMAL;
        if ("italic".equals(fontStyleStr)) {
            fontStyle = Typeface.ITALIC;
        }

        Typeface typeface = mHost.getTypeface();
        if (typeface == null) {
            typeface = Typeface.DEFAULT;
        }
        if (fontStyle != typeface.getStyle()) {
            mHost.setTypeface(typeface, fontStyle);
        }
    }

    public void setFontWeight(String fontWeightStr) {
        if (TextUtils.isEmpty(fontWeightStr) || mHost == null) {
            return;
        }

        int fontWeight = TypefaceBuilder.parseFontWeight(fontWeightStr);

        Typeface typeface = mHost.getTypeface();
        if (typeface == null) {
            typeface = Typeface.DEFAULT;
        }
        if (fontWeight != typeface.getStyle()) {
            mHost.setTypeface(typeface, fontWeight);
        }
    }

    public void setTextAlign(String textAlignStr) {
        if (TextUtils.isEmpty(textAlignStr) || mHost == null) {
            return;
        }

        int alignment = Gravity.LEFT;
        if ("center".equals(textAlignStr)) {
            alignment = Gravity.CENTER_HORIZONTAL;
        } else if ("right".equals(textAlignStr)) {
            alignment = Gravity.RIGHT;
        }

        mHost.setGravity(alignment | getDefaultVerticalGravity());
    }

    public void setText(String text) {
        forceUpdateSpannable(text);
    }

    public void forceUpdateSpannable(String text) {
        mTextSpan.setDirty(true);
        updateSpannable(text);
    }

    public void updateSpannable(String text) {
        if (!mTextSpan.isDirty()) {
            return;
        }
        mTextSpan.setDirty(false);

        if (TextUtils.isEmpty(text)) {
            mHost.setText("");
            return;
        }

        int selection = text.length();
        if (text.equals(mTextWatcherRunnable.mText)) {
            selection = mHost.getSelectionStart();
        }

        Spannable spannable = mTextSpan.createSpanned(text);
        mHost.setText(spannable);

        if (mHost instanceof EditText) {
            int length = mHost.getText().length();
            if (selection > length) {
                selection = length;
            }
            ((EditText) mHost).setSelection(selection);
        }
    }

    public Spannable generateSpannable(String text) {
        if (TextUtils.isEmpty(text)) {
            return null;
        }
        return mTextSpan.createSpanned(text);
    }

    public void setMaxLength(int length) {
        if (mHost == null) {
            return;
        }

        InputFilter[] orig = mHost.getFilters();
        int origLength = Integer.MIN_VALUE;
        int origLengthIndex = -1;
        for (int i = 0; i < orig.length; i++) {
            if (orig[i] instanceof InputFilter.LengthFilter) {
                if (Build.VERSION.SDK_INT >= 21) {
                    origLength = ((InputFilter.LengthFilter) orig[i]).getMax();
                }
                origLengthIndex = i;
                break;
            }
        }

        if (length == origLength) {
            return;
        }

        if (length < 0) {
            if (origLengthIndex >= 0) {
                InputFilter[] newFilters = new InputFilter[orig.length - 1];
                for (int i = 0; i < newFilters.length; i++) {
                    if (i < origLengthIndex) {
                        newFilters[i] = orig[i];
                    } else {
                        newFilters[i] = orig[i + 1];
                    }
                }
                mHost.setFilters(newFilters);
            }

            return;
        }

        if (origLengthIndex >= 0) {
            orig[origLengthIndex] = new InputFilter.LengthFilter(length);
        } else {
            InputFilter[] newFilters = new InputFilter[orig.length + 1];
            System.arraycopy(orig, 0, newFilters, 0, orig.length);
            newFilters[orig.length] = new InputFilter.LengthFilter(length);
            mHost.setFilters(newFilters);
        }
    }

    public void setAutoComplete(String autoCompleteStr) {
        if (mHost == null) {
            return;
        }
        boolean autoCompleted = Attributes.AutoComplete.ON.equals(autoCompleteStr);
        if (mHost instanceof FlexEditText) {
            ((FlexEditText) mHost).setAutoCompleted(autoCompleted);
        }
    }

    private void setCaretColor(String caretColorStr) {
        if (mHost == null) {
            return;
        }
        if (TextUtils.equals(caretColorStr, "auto")) {
            caretColorStr = ColorUtil.getColorStr(mHost.getCurrentTextColor());
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Get the drawable and set a color filter
                mHost
                        .getTextCursorDrawable()
                        .setColorFilter(ColorUtil.getColor(caretColorStr), PorterDuff.Mode.SRC_IN);
                return;
            }
            // Get the cursor resource id
            Field field = TextView.class.getDeclaredField("mCursorDrawableRes");
            field.setAccessible(true);
            int drawableResId = field.getInt(mHost);

            // Get the editor
            field = TextView.class.getDeclaredField("mEditor");
            field.setAccessible(true);
            Object editor = field.get(mHost);

            // Get the drawable and set a color filter
            Drawable drawable = ContextCompat.getDrawable(mHost.getContext(), drawableResId);
            if (drawable == null) {
                return;
            }
            drawable.setColorFilter(ColorUtil.getColor(caretColorStr), PorterDuff.Mode.SRC_IN);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Set the drawables
                field = editor.getClass().getDeclaredField("mDrawableForCursor");
                field.setAccessible(true);
                field.set(editor, drawable);
            } else {
                Drawable[] drawables = {drawable, drawable};

                // Set the drawables
                field = editor.getClass().getDeclaredField("mCursorDrawable");
                field.setAccessible(true);
                field.set(editor, drawables);
            }
        } catch (Exception e) {
            Log.e("Edit", "error on set caret color", e);
        }
    }

    @Override
    protected void onSaveInstanceState(Map<String, Object> outState) {
        super.onSaveInstanceState(outState);
        if (mHost == null) {
            return;
        }
        outState.put(KEY_CHANGE_EVENT_STATE, mIsChangeEventRegistered);
        outState.put(KEY_TEXT, mHost.getText());
    }

    @Override
    protected void onRestoreInstanceState(Map<String, Object> savedState) {
        super.onRestoreInstanceState(savedState);
        if (savedState == null || mHost == null) {
            return;
        }
        if (savedState.get(KEY_CHANGE_EVENT_STATE) != null) {
            mIsChangeEventRegistered = (boolean) savedState.get(KEY_CHANGE_EVENT_STATE);
        }
        if (savedState.containsKey(KEY_TEXT)) {
            mHost.setText((CharSequence) savedState.get(KEY_TEXT));
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mHost != null) {
            Handler handler = mHost.getHandler();
            if (handler != null) {
                handler.removeCallbacks(mTextWatcherRunnable);
                handler.removeCallbacks(mToggleFocus);
            }
            mHost.removeTextChangedListener(mTextWatcher);
        }
        mTextWatcher = null;
    }

    protected String getDefaultFontSize() {
        return DEFAULT_FONT_SIZE;
    }

    protected String getDefaultColor() {
        return DEFAULT_COLOR;
    }

    protected String getDefaultPlaceholderColor() {
        return DEFAULT_PLACEHOLDER_COLOR;
    }

    @Override
    public void invokeMethod(String methodName, Map<String, Object> args) {
        super.invokeMethod(methodName, args);

        if (!(mHost instanceof EditText)) {
            return;
        }

        EditText editText = (EditText) mHost;
        if (METHOD_SELECT.equals(methodName)) {
            focus(true);
            editText.selectAll();
        } else if (METHOD_SET_SELECTION_RANGE.equals(methodName)) {
            if (args.containsKey("start") && args.containsKey("end")) {
                int start = (int) args.get("start");
                if (start > editText.length()) {
                    start = editText.length();
                }

                int end = (int) args.get("end");
                if (end < 0 || end > editText.length()) {
                    end = editText.length();
                }

                if (start < 0 || start > end) {
                    start = end;
                }

                focus(true);
                editText.setSelection(start, end);
            }
        } else if (METHOD_GET_SELECTION_RANGE.equals(methodName)) {
            if (args.containsKey("callback")) {
                String callbackId = (String) args.get("callback");
                mCallback.onJsMethodCallback(
                        getPageId(), callbackId, editText.getSelectionStart(),
                        editText.getSelectionEnd());
            }
        }
    }

    private void toggleFocus() {
        if (mNeedToggleFocus) {
            View rootView = getRootComponent().getHostView();
            if (rootView != null) {
                Handler handler = rootView.getHandler();
                if (handler != null) {
                    handler.removeCallbacks(mToggleFocus);
                    handler.postDelayed(mToggleFocus, 50);
                } else {
                    Log.w(TAG, "toggleFocus: handler is null");
                }
            } else {
                Log.w(TAG, "toggleFocus: rootView is null");
            }
            mNeedToggleFocus = false;
        }
    }

    @Override
    public void focus(boolean focus) {
        // TODO: move it to super.focus(boolean focus) when stable.
        mToggleFocus.setFocus(focus);
        if (mHost != null) {
            mToggleFocus.run();
        } else {
            mNeedToggleFocus = true;
        }
    }

    @Override
    public void onHostViewAttached(ViewGroup parent) {
        YogaNode node = YogaUtil.getYogaNode(mHost);
        if (node != null && mHost.getLayoutParams() != null) {
            YogaNode parentNode = node.getParent();
            if (parentNode != null) {
                if (parentNode.getFlexDirection() == YogaFlexDirection.ROW
                        || parentNode.getAlignItems() != YogaAlign.STRETCH) {
                    float width = mHost.getLayoutParams().width;
                    width = width >= 0 ? width : FloatUtil.UNDEFINED;
                    node.setWidth(width);
                }

                if (parentNode.getFlexDirection() == YogaFlexDirection.COLUMN
                        || parentNode.getAlignItems() != YogaAlign.STRETCH) {
                    float height = mHost.getLayoutParams().height;
                    height = height >= 0 ? height : FloatUtil.UNDEFINED;
                    node.setHeight(height);
                }
            } else {
                Log.e(TAG, "onHostViewAttached: parentNode is null");
            }
        }
        super.onHostViewAttached(parent);
    }

    private void showPhoneNumberPrompt(List<String> numbers) {
        if (mHost == null || !(mHost instanceof FlexEditText)) {
            return;
        }
        if (numbers == null || numbers.isEmpty()) {
            return;
        }
        RuntimeLogManager.getDefault().logPhonePromptStart(numbers.size());
        FlexEditText view = (FlexEditText) mHost;
        if (mPhoneAdapter == null) {
            mPhoneAdapter = new PhoneAdapter(view.getContext(), numbers);
            view.getDropDownBackground().setAlpha(200);
            view.setOnItemClickListener(
                    new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position,
                                                long id) {
                            RuntimeLogManager.getDefault()
                                    .logPhonePromptClick(mPhoneAdapter.getItem(position));
                        }
                    });
        } else {
            mPhoneAdapter.refresh(numbers);
        }
        if (view.getAdapter() == null) {
            view.setAdapter(mPhoneAdapter);
        }
        view.showDropDown();
    }

    private interface EnterKeyType {
        String DEFAULT = "default";
        String SEND = "send";
        String SEARCH = "search";
        String NEXT = "next";
        String GO = "go";
        String DONE = "done";
    }

    private static class EditTextWatcher implements TextWatcher {
        public WeakReference<Edit> mRef;
        private String mOldText = "";

        public EditTextWatcher(Edit edit) {
            mRef = new WeakReference<>(edit);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            final Edit edit = mRef.get();
            if (edit == null) {
                return;
            }
            if (mOldText != null && mOldText.equals(s.toString())) {
                return;
            }
            mOldText = s.toString();

            edit.changeAttrDomData(Attributes.Style.VALUE, mOldText);

            if (edit.isChangeEventRegistered()) {
                Handler handler = edit.mHost.getHandler();
                edit.mTextWatcherRunnable.mText = mOldText;
                if (handler != null) {
                    handler.removeCallbacks(edit.mTextWatcherRunnable);
                    handler.postDelayed(edit.mTextWatcherRunnable, 16);
                } else {
                    edit.mTextWatcherRunnable.run();
                }
            }
        }
    }

    private static class FocusChangeListener implements View.OnFocusChangeListener {

        private WeakReference<Edit> editRef;

        public FocusChangeListener(Edit edit) {
            this.editRef = new WeakReference<>(edit);
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (v.getWindowVisibility() != View.VISIBLE
                    || !(v instanceof FlexEditText)
                    || !((FlexEditText) v).isAutoCompleted()) {
                return;
            }
            TextView textView = (TextView) v;
            if (hasFocus) {
                Executors.io()
                        .execute(
                                new AbsTask<List<String>>() {
                                    @Override
                                    protected List<String> doInBackground() {
                                        return PhoneManager.get().getSavedNumbers();
                                    }

                                    @Override
                                    protected void onPostExecute(List<String> numbers) {
                                        if (editRef.get() != null) {
                                            editRef.get().showPhoneNumberPrompt(numbers);
                                        }
                                    }
                                });
            } else {
                final String input = textView.getText().toString().trim();
                Executors.io()
                        .execute(
                                () -> {
                                    PhoneManager.get().saveNumber(input);
                                    RuntimeLogManager.getDefault().logPhonePromptEnd(input);
                                });
            }
        }
    }

    private static class PhoneAdapter extends BaseAdapter implements Filterable {
        private Context mContext;
        private List<String> mValues;
        private ArrayList<String> mOriginalValues;
        private ArrayFilter mFilter;

        public PhoneAdapter(Context context, List<String> list) {
            this.mContext = context.getApplicationContext();
            this.mValues = list;
        }

        public void refresh(List<String> values) {
            if (mOriginalValues != null) {
                mOriginalValues.clear();
                mOriginalValues.addAll(values);
            }
            mValues.clear();
            mValues.addAll(values);
            notifyDataSetChanged();
        }

        public void remove(String value) {
            if (mOriginalValues != null) {
                mOriginalValues.remove(value);
            }
            mValues.remove(value);
            notifyDataSetChanged();
            PhoneManager.get().deleteNumber(value);
            RuntimeLogManager.getDefault().logPhonePromptDelete(value);
        }

        @Override
        public int getCount() {
            return mValues.size();
        }

        @Override
        public String getItem(int position) {
            return mValues.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            Holder holder = null;
            if (convertView == null) {
                convertView =
                        LayoutInflater.from(mContext)
                                .inflate(R.layout.item_phone_list, parent, false);
                holder = new Holder();
                holder.phoneText = convertView.findViewById(R.id.phone_text);
                holder.deleteBtn = convertView.findViewById(R.id.delete_btn);
                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
            }
            final String number = mValues.get(position);
            holder.phoneText.setText(number);
            holder.deleteBtn.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            remove(mValues.get(position));
                        }
                    });
            return convertView;
        }

        @Override
        public Filter getFilter() {
            if (mFilter == null) {
                mFilter = new ArrayFilter();
            }
            return mFilter;
        }

        private class Holder {
            TextView phoneText;
            ImageView deleteBtn;
        }

        private class ArrayFilter extends Filter {
            @Override
            protected FilterResults performFiltering(CharSequence prefix) {
                if (mOriginalValues == null) {
                    mOriginalValues = new ArrayList<>(mValues);
                }
                final FilterResults results = new FilterResults();
                if (TextUtils.isEmpty(prefix)) {
                    results.values = new ArrayList<>(mOriginalValues);
                    results.count = mOriginalValues.size();
                } else {
                    final String prefixString = prefix.toString().toLowerCase();
                    final int count = mOriginalValues.size();
                    final ArrayList<String> newValues = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        final String value = mOriginalValues.get(i);
                        if (value.startsWith(prefixString)) {
                            newValues.add(value);
                        }
                    }
                    results.values = newValues;
                    results.count = newValues.size();
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mValues = (List<String>) results.values;
                if (results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        }
    }

    private class ToggleFocusRunnable implements Runnable {

        private boolean mFocused = false;

        private void setFocus(boolean focused) {
            mFocused = focused;
        }

        @Override
        public void run() {
            if (mHost == null) {
                return;
            }

            if (mFocused) {
                mHost.setFocusable(true);
                mHost.setFocusableInTouchMode(true);
                mHost.requestFocus();
            } else {
                mHost.clearFocus();
            }

            InputMethodManager imm =
                    (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (mFocused) {
                imm.showSoftInput(mHost, InputMethodManager.SHOW_IMPLICIT);
            } else {
                imm.hideSoftInputFromWindow(mHost.getWindowToken(), 0);
            }
        }
    }

    private class TextWatcherRunnable implements Runnable {

        public String mText = "";

        @Override
        public void run() {
            Map<String, Object> params = new HashMap<>();
            params.put("text", mText); // deprecated in platformVersion 101, use value instead
            params.put("value", mText);
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("value", mText);
            mCallback.onJsEventCallback(
                    getPageId(), mRef, Attributes.Event.CHANGE, Edit.this, params, attrs);
        }
    }
}
