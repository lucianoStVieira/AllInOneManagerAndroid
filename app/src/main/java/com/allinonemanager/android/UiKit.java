package com.allinonemanager.android;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

final class UiKit {
    static final int COLOR_BACKGROUND = Color.rgb(246, 248, 247);
    static final int COLOR_SURFACE = Color.WHITE;
    static final int COLOR_FIELD = Color.rgb(253, 254, 254);
    static final int COLOR_TEXT = Color.rgb(28, 39, 43);
    static final int COLOR_MUTED = Color.rgb(99, 114, 119);
    static final int COLOR_PRIMARY = Color.rgb(0, 121, 107);
    static final int COLOR_PRIMARY_DARK = Color.rgb(0, 89, 82);
    static final int COLOR_ACCENT = Color.rgb(245, 158, 11);
    static final int COLOR_BORDER = Color.rgb(218, 229, 226);
    static final int COLOR_SELECTED = Color.rgb(226, 247, 243);
    static final int COLOR_DANGER = Color.rgb(179, 38, 30);
    static final int COLOR_DANGER_SOFT = Color.rgb(255, 236, 233);
    static final int COLOR_RIPPLE = Color.argb(40, 0, 121, 107);

    private UiKit() {
    }

    static void applyWindow(Activity activity) {
        Window window = activity.getWindow();
        window.setStatusBarColor(COLOR_BACKGROUND);
        window.setNavigationBarColor(COLOR_BACKGROUND);

        int flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        window.getDecorView().setSystemUiVisibility(flags);
    }

    static Drawable headerBackground(Context context) {
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] { COLOR_PRIMARY_DARK, COLOR_PRIMARY });
        background.setCornerRadius(dp(context, 24));
        return background;
    }

    static Drawable panelBackground(Context context) {
        return roundedRect(context, COLOR_SURFACE, COLOR_BORDER, 1, 20);
    }

    static Drawable inputBackground(Context context) {
        return roundedRect(context, COLOR_FIELD, COLOR_BORDER, 1, 14);
    }

    static Drawable rowBackground(Context context, boolean selected) {
        return ripple(
                context,
                selected ? COLOR_SELECTED : COLOR_SURFACE,
                selected ? COLOR_PRIMARY : COLOR_BORDER,
                1,
                16,
                COLOR_RIPPLE);
    }

    static Drawable buttonBackground(Context context, int fillColor, int strokeColor, int rippleColor) {
        return ripple(context, fillColor, strokeColor, strokeColor == Color.TRANSPARENT ? 0 : 1, 14, rippleColor);
    }

    static void styleInput(EditText editText, int iconResId) {
        Context context = editText.getContext();
        editText.setTextColor(COLOR_TEXT);
        editText.setHintTextColor(COLOR_MUTED);
        editText.setTextSize(15);
        editText.setMinHeight(dp(context, 52));
        editText.setPadding(dp(context, 14), dp(context, 10), dp(context, 14), dp(context, 10));
        editText.setBackground(inputBackground(context));
        editText.setCompoundDrawablePadding(dp(context, 10));
        editText.setCompoundDrawablesRelative(icon(context, iconResId, COLOR_MUTED, 20), null, null, null);
    }

    static void styleSpinner(Spinner spinner) {
        Context context = spinner.getContext();
        spinner.setMinimumHeight(dp(context, 52));
        spinner.setPadding(dp(context, 10), 0, dp(context, 10), 0);
        spinner.setBackground(inputBackground(context));
    }

    static void styleSwitch(Switch switchView) {
        Context context = switchView.getContext();
        switchView.setTextColor(COLOR_TEXT);
        switchView.setTextSize(15);
        switchView.setGravity(Gravity.CENTER_VERTICAL);
        switchView.setMinHeight(dp(context, 48));
        switchView.setPadding(dp(context, 4), dp(context, 8), dp(context, 4), dp(context, 8));
    }

    static Button primaryButton(Context context, int textResId, int iconResId) {
        return iconButton(context, textResId, iconResId, COLOR_PRIMARY, Color.WHITE, Color.TRANSPARENT, 46, 20);
    }

    static Button neutralButton(Context context, int textResId, int iconResId) {
        return iconButton(context, textResId, iconResId, COLOR_SURFACE, COLOR_PRIMARY, COLOR_BORDER, 46, 20);
    }

    static Button dangerButton(Context context, int textResId, int iconResId) {
        return iconButton(context, textResId, iconResId, COLOR_DANGER_SOFT, COLOR_DANGER, COLOR_DANGER_SOFT, 46, 20);
    }

    static Button compactPrimaryButton(Context context, int textResId, int iconResId) {
        return iconButton(context, textResId, iconResId, COLOR_PRIMARY, Color.WHITE, Color.TRANSPARENT, 38, 18);
    }

    static Button compactNeutralButton(Context context, int textResId, int iconResId) {
        return iconButton(context, textResId, iconResId, COLOR_SURFACE, COLOR_PRIMARY, COLOR_BORDER, 38, 18);
    }

    static Button compactDangerButton(Context context, int textResId, int iconResId) {
        return iconButton(context, textResId, iconResId, COLOR_DANGER_SOFT, COLOR_DANGER, COLOR_DANGER_SOFT, 38, 18);
    }

    static Button primaryTextButton(Context context, int textResId, int iconResId) {
        return textButton(context, textResId, iconResId, COLOR_PRIMARY, Color.WHITE, Color.TRANSPARENT);
    }

    static Button neutralTextButton(Context context, int textResId, int iconResId) {
        return textButton(context, textResId, iconResId, COLOR_SURFACE, COLOR_PRIMARY, COLOR_BORDER);
    }

    static Button button(Context context, int textResId, int iconResId, int fillColor, int textColor, int strokeColor) {
        return iconButton(context, textResId, iconResId, fillColor, textColor, strokeColor, 46, 20);
    }

    private static Button iconButton(
            Context context,
            int textResId,
            int iconResId,
            int fillColor,
            int textColor,
            int strokeColor,
            int minHeightDp,
            int iconSizeDp) {
        Button button = new Button(context);
        button.setText("");
        button.setContentDescription(context.getString(textResId));
        styleButtonBase(button, fillColor, textColor, strokeColor, minHeightDp, 12, 1);
        button.setCompoundDrawablePadding(0);
        button.setCompoundDrawablesRelative(icon(context, iconResId, textColor, iconSizeDp), null, null, null);
        return button;
    }

    private static Button textButton(
            Context context,
            int textResId,
            int iconResId,
            int fillColor,
            int textColor,
            int strokeColor) {
        Button button = new Button(context);
        button.setText(textResId);
        styleButtonBase(button, fillColor, textColor, strokeColor, 46, 8, 2);
        button.setCompoundDrawablePadding(dp(context, 6));
        button.setCompoundDrawablesRelative(icon(context, iconResId, textColor, 16), null, null, null);
        return button;
    }

    private static void styleButtonBase(
            Button button,
            int fillColor,
            int textColor,
            int strokeColor,
            int minHeightDp,
            int horizontalPaddingDp,
            int maxLines) {
        Context context = button.getContext();
        button.setAllCaps(false);
        button.setTextColor(textColor);
        button.setTextSize(13);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(context, minHeightDp));
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinEms(0);
        button.setMaxLines(maxLines);
        button.setIncludeFontPadding(false);
        button.setPadding(dp(context, horizontalPaddingDp), 0, dp(context, horizontalPaddingDp), 0);
        button.setBackground(buttonBackground(context, fillColor, strokeColor, Color.argb(45, 0, 121, 107)));
    }

    static void styleHeaderTitle(TextView title) {
        title.setTextColor(Color.WHITE);
        title.setTextSize(21);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setIncludeFontPadding(false);
    }

    static void stylePageTitle(TextView title) {
        title.setTextColor(COLOR_TEXT);
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setIncludeFontPadding(false);
    }

    static void styleSectionTitle(TextView title, int iconResId) {
        Context context = title.getContext();
        title.setTextColor(COLOR_TEXT);
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(0, dp(context, 18), 0, dp(context, 8));
        title.setCompoundDrawablePadding(dp(context, 8));
        title.setCompoundDrawablesRelative(icon(context, iconResId, COLOR_PRIMARY, 20), null, null, null);
    }

    static void styleSelectedLabel(TextView label) {
        Context context = label.getContext();
        label.setTextColor(COLOR_MUTED);
        label.setTextSize(14);
        label.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
        label.setBackground(roundedRect(context, Color.rgb(238, 243, 241), COLOR_BORDER, 1, 14));
        label.setCompoundDrawablePadding(dp(context, 8));
        label.setCompoundDrawablesRelative(icon(context, R.drawable.ic_person_24, COLOR_PRIMARY, 18), null, null, null);
    }

    static void styleRow(TextView row, int iconResId, boolean selected) {
        Context context = row.getContext();
        row.setTextColor(COLOR_TEXT);
        row.setTextSize(15);
        row.setLineSpacing(dp(context, 2), 1f);
        row.setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12));
        row.setBackground(rowBackground(context, selected));
        row.setCompoundDrawablePadding(dp(context, 10));
        row.setCompoundDrawablesRelative(
                icon(context, iconResId, selected ? COLOR_PRIMARY : COLOR_MUTED, 20),
                null,
                null,
                null);
    }

    static void styleEmptyText(TextView view, int iconResId) {
        Context context = view.getContext();
        view.setTextColor(COLOR_MUTED);
        view.setTextSize(14);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12));
        view.setBackground(roundedRect(context, Color.rgb(241, 245, 244), COLOR_BORDER, 1, 14));
        view.setCompoundDrawablePadding(dp(context, 10));
        view.setCompoundDrawablesRelative(icon(context, iconResId, COLOR_MUTED, 18), null, null, null);
    }

    static void addBottomMargin(View view, int marginDp) {
        ViewGroup.LayoutParams current = view.getLayoutParams();
        ViewGroup.MarginLayoutParams params;
        if (current instanceof ViewGroup.MarginLayoutParams) {
            params = (ViewGroup.MarginLayoutParams) current;
        } else {
            params = new ViewGroup.MarginLayoutParams(current);
        }
        params.bottomMargin = dp(view.getContext(), marginDp);
        view.setLayoutParams(params);
    }

    static Drawable icon(Context context, int iconResId, int color, int sizeDp) {
        Drawable drawable = context.getDrawable(iconResId);
        if (drawable == null) {
            return null;
        }

        drawable = drawable.mutate();
        drawable.setTint(color);
        int size = dp(context, sizeDp);
        drawable.setBounds(0, 0, size, size);
        return drawable;
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static Drawable ripple(
            Context context,
            int fillColor,
            int strokeColor,
            int strokeWidthDp,
            int radiusDp,
            int rippleColor) {
        GradientDrawable content = roundedRect(context, fillColor, strokeColor, strokeWidthDp, radiusDp);
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, null);
    }

    private static GradientDrawable roundedRect(
            Context context,
            int fillColor,
            int strokeColor,
            int strokeWidthDp,
            int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(context, radiusDp));
        if (strokeWidthDp > 0 && strokeColor != Color.TRANSPARENT) {
            drawable.setStroke(dp(context, strokeWidthDp), strokeColor);
        }
        return drawable;
    }
}
