package com.allinonemanager.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class LoginActivity extends Activity {
    private static final int MIN_PASSWORD_LENGTH = 4;

    private boolean createMode;
    private EditText txtPassword;
    private EditText txtConfirmPassword;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppLanguage.apply(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiKit.applyWindow(this);
        createMode = !AccessManager.hasPassword(this);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(UiKit.COLOR_BACKGROUND);
        LinearLayout root = vertical();
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(scroll);

        LinearLayout header = vertical();
        header.setGravity(Gravity.CENTER_HORIZONTAL);
        header.setPadding(dp(18), dp(18), dp(18), dp(18));
        header.setBackground(UiKit.headerBackground(this));

        TextView appName = new TextView(this);
        appName.setText(R.string.app_name);
        UiKit.styleHeaderTitle(appName);
        header.addView(appName);

        TextView title = new TextView(this);
        title.setText(createMode ? R.string.login_create_title : R.string.login_unlock_title);
        title.setTextColor(0xffffffff);
        title.setTextSize(15);
        title.setPadding(0, dp(8), 0, 0);
        header.addView(title);

        LinearLayout.LayoutParams headerParams = fullWidthParams();
        headerParams.setMargins(0, 0, 0, dp(12));
        root.addView(header, headerParams);

        txtPassword = input(R.string.hint_access_password);
        root.addView(txtPassword);

        if (createMode) {
            txtConfirmPassword = input(R.string.hint_confirm_password);
            root.addView(txtConfirmPassword);
        }

        Button access = UiKit.primaryTextButton(
                this,
                createMode ? R.string.action_save : R.string.action_unlock,
                R.drawable.ic_key_24);
        access.setOnClickListener(v -> submit());
        LinearLayout.LayoutParams buttonParams = fullWidthParams();
        buttonParams.setMargins(0, dp(8), 0, 0);
        root.addView(access, buttonParams);
    }

    private void submit() {
        String password = txtPassword.getText().toString();
        if (password.trim().isEmpty()) {
            toast(R.string.toast_password_required);
            txtPassword.requestFocus();
            return;
        }

        if (createMode) {
            createPassword(password);
        } else {
            unlock(password);
        }
    }

    private void createPassword(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            toast(R.string.toast_password_short);
            txtPassword.requestFocus();
            return;
        }

        String confirm = txtConfirmPassword == null ? "" : txtConfirmPassword.getText().toString();
        if (!password.equals(confirm)) {
            toast(R.string.toast_password_mismatch);
            txtConfirmPassword.requestFocus();
            return;
        }

        AccessManager.createPassword(this, password);
        toast(R.string.toast_access_created);
        openMain();
    }

    private void unlock(String password) {
        if (!AccessManager.unlock(this, password)) {
            toast(R.string.toast_login_failed);
            txtPassword.setText("");
            txtPassword.requestFocus();
            return;
        }

        openMain();
    }

    private void openMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private EditText input(int hintResId) {
        EditText editText = new EditText(this);
        editText.setHint(hintResId);
        editText.setSingleLine(true);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        UiKit.styleInput(editText, R.drawable.ic_key_24);
        LinearLayout.LayoutParams params = fullWidthParams();
        params.setMargins(0, dp(6), 0, dp(6));
        editText.setLayoutParams(params);
        return editText;
    }

    private LinearLayout.LayoutParams fullWidthParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private void toast(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return UiKit.dp(this, value);
    }
}
