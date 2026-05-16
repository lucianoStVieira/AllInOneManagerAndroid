package com.allinonemanager.android;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class MainActivity extends Activity {
    private static final int REQUEST_POST_NOTIFICATIONS = 2001;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private DatabaseHelper db;
    private Long selectedClientId;

    private EditText txtFullName;
    private EditText txtPhone;
    private EditText txtEmail;
    private EditText txtDateOfBirth;
    private Spinner spinnerClientTimeZone;
    private EditText txtNotes;
    private EditText txtSearch;
    private EditText txtSessionDate;
    private EditText txtSessionTime;
    private EditText txtDurationMinutes;
    private EditText txtSessionNotes;
    private TextView selectedClientLabel;
    private LinearLayout clientsList;
    private LinearLayout sessionsList;
    private String languageSelectionAtCreate;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppLanguage.apply(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiKit.applyWindow(this);
        languageSelectionAtCreate = AppLanguage.selectedLanguageTag(this);
        db = new DatabaseHelper(this);
        requestNotificationPermission();
        buildUi();
        loadClients();
        ReminderScheduler.scheduleAll(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!AppLanguage.selectedLanguageTag(this).equals(languageSelectionAtCreate)) {
            recreate();
            return;
        }

        if (db != null && clientsList != null) {
            loadClients();
            loadSessionsForSelectedClient();
            ReminderScheduler.scheduleAll(this);
        }
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(UiKit.COLOR_BACKGROUND);
        LinearLayout root = vertical();
        root.setPadding(dp(16), dp(16), dp(16), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        LinearLayout header = horizontal();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(16), dp(14), dp(16));
        header.setBackground(UiKit.headerBackground(this));
        TextView title = new TextView(this);
        title.setText(R.string.main_title);
        UiKit.styleHeaderTitle(title);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button settings = UiKit.neutralButton(this, R.string.action_settings, R.drawable.ic_settings_24);
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        header.addView(settings);
        LinearLayout.LayoutParams headerParams = fullWidthParams();
        headerParams.setMargins(0, 0, 0, dp(8));
        root.addView(header, headerParams);

        addSectionTitle(root, R.string.section_client, R.drawable.ic_person_24);
        txtFullName = input(
                R.string.hint_full_name,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                R.drawable.ic_person_24);
        root.addView(txtFullName);
        txtPhone = input(R.string.hint_phone, InputType.TYPE_CLASS_PHONE, R.drawable.ic_phone_24);
        txtEmail = input(
                R.string.hint_email,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                R.drawable.ic_mail_24);
        root.addView(twoColumn(txtPhone, txtEmail));
        txtDateOfBirth = input(
                R.string.hint_date_of_birth,
                InputType.TYPE_CLASS_DATETIME,
                R.drawable.ic_calendar_24);
        txtDateOfBirth.setText(LocalDate.now().format(DATE_FORMAT));
        txtDateOfBirth.setOnClickListener(v -> showDatePicker(txtDateOfBirth));
        root.addView(txtDateOfBirth);

        addFieldLabel(root, R.string.hint_client_time_zone, R.drawable.ic_clock_24);
        spinnerClientTimeZone = timeZoneSpinner(TimeZoneSupport.DEFAULT_ZONE_ID);
        root.addView(spinnerClientTimeZone);

        txtNotes = input(
                R.string.hint_notes,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE,
                R.drawable.ic_notes_24);
        txtNotes.setMinLines(3);
        txtNotes.setGravity(Gravity.TOP | Gravity.START);
        root.addView(txtNotes);

        LinearLayout primaryClientButtons = horizontal();
        Button addClient = UiKit.primaryButton(this, R.string.action_add, R.drawable.ic_add_24);
        addClient.setOnClickListener(v -> addClient());
        Button updateClient = UiKit.neutralButton(this, R.string.action_update, R.drawable.ic_edit_24);
        updateClient.setOnClickListener(v -> updateClient());
        addWeighted(primaryClientButtons, addClient);
        addWeighted(primaryClientButtons, updateClient);
        root.addView(primaryClientButtons);

        LinearLayout secondaryClientButtons = horizontal();
        Button deleteClient = UiKit.dangerButton(this, R.string.action_delete, R.drawable.ic_delete_24);
        deleteClient.setOnClickListener(v -> deleteClient());
        Button clear = UiKit.neutralButton(this, R.string.action_clear, R.drawable.ic_clear_24);
        clear.setOnClickListener(v -> clearClientForm());
        addWeighted(secondaryClientButtons, deleteClient);
        addWeighted(secondaryClientButtons, clear);
        root.addView(secondaryClientButtons);

        addSectionTitle(root, R.string.section_clients, R.drawable.ic_search_24);
        txtSearch = input(R.string.hint_search_clients, InputType.TYPE_CLASS_TEXT, R.drawable.ic_search_24);
        txtSearch.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                loadClients();
            }
        });
        root.addView(txtSearch);
        clientsList = vertical();
        root.addView(clientsList);

        addSectionTitle(root, R.string.section_client_sessions, R.drawable.ic_calendar_24);
        selectedClientLabel = new TextView(this);
        selectedClientLabel.setText(R.string.text_select_client_schedule);
        UiKit.styleSelectedLabel(selectedClientLabel);
        root.addView(selectedClientLabel);

        txtSessionDate = input(R.string.hint_session_date, InputType.TYPE_CLASS_DATETIME, R.drawable.ic_calendar_24);
        txtSessionDate.setText(LocalDate.now().format(DATE_FORMAT));
        txtSessionDate.setOnClickListener(v -> showDatePicker(txtSessionDate));
        txtSessionTime = input(R.string.hint_session_time, InputType.TYPE_CLASS_DATETIME, R.drawable.ic_clock_24);
        txtSessionTime.setText("09:00");
        txtSessionTime.setOnClickListener(v -> showTimePicker(txtSessionTime));
        root.addView(twoColumn(txtSessionDate, txtSessionTime));

        txtDurationMinutes = input(R.string.hint_duration_minutes, InputType.TYPE_CLASS_NUMBER, R.drawable.ic_timer_24);
        txtDurationMinutes.setText("50");
        root.addView(txtDurationMinutes);
        txtSessionNotes = input(
                R.string.hint_session_notes,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE,
                R.drawable.ic_notes_24);
        txtSessionNotes.setMinLines(3);
        txtSessionNotes.setGravity(Gravity.TOP | Gravity.START);
        root.addView(txtSessionNotes);

        LinearLayout sessionButtons = horizontal();
        Button scheduleSession = UiKit.primaryButton(this, R.string.action_schedule, R.drawable.ic_calendar_24);
        scheduleSession.setOnClickListener(v -> scheduleSession());
        Button clearSession = UiKit.neutralButton(this, R.string.action_clear_notes, R.drawable.ic_clear_24);
        clearSession.setOnClickListener(v -> txtSessionNotes.setText(""));
        addWeighted(sessionButtons, scheduleSession);
        addWeighted(sessionButtons, clearSession);
        root.addView(sessionButtons);

        sessionsList = vertical();
        root.addView(sessionsList);
    }

    private void addClient() {
        String fullName = txtFullName.getText().toString().trim();
        if (fullName.isEmpty()) {
            toast(R.string.toast_full_name_required);
            txtFullName.requestFocus();
            return;
        }

        String dob;
        try {
            dob = parseDate(txtDateOfBirth).format(DATE_FORMAT);
        } catch (IllegalArgumentException ex) {
            toast(ex.getMessage());
            return;
        }

        long id = db.addClient(
                fullName,
                txtPhone.getText().toString(),
                txtEmail.getText().toString(),
                dob,
                selectedTimeZoneId(),
                txtNotes.getText().toString());
        clearClientForm();
        selectedClientId = id;
        loadClients();
        Client client = db.getClient(id);
        if (client != null) {
            selectClient(client);
        }
    }

    private void updateClient() {
        if (selectedClientId == null) {
            toast(R.string.toast_select_client_update);
            return;
        }

        String fullName = txtFullName.getText().toString().trim();
        if (fullName.isEmpty()) {
            toast(R.string.toast_full_name_required);
            txtFullName.requestFocus();
            return;
        }

        String dob;
        try {
            dob = parseDate(txtDateOfBirth).format(DATE_FORMAT);
        } catch (IllegalArgumentException ex) {
            toast(ex.getMessage());
            return;
        }

        db.updateClient(
                selectedClientId,
                fullName,
                txtPhone.getText().toString(),
                txtEmail.getText().toString(),
                dob,
                selectedTimeZoneId(),
                txtNotes.getText().toString());
        loadClients();
        loadSessionsForSelectedClient();
        toast(R.string.toast_client_updated);
    }

    private void deleteClient() {
        if (selectedClientId == null) {
            toast(R.string.toast_select_client_delete);
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_client_title)
                .setMessage(R.string.dialog_delete_client_message)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    db.deleteClient(selectedClientId);
                    clearClientForm();
                    loadClients();
                    loadSessionsForSelectedClient();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void clearClientForm() {
        selectedClientId = null;
        txtFullName.setText("");
        txtPhone.setText("");
        txtEmail.setText("");
        txtDateOfBirth.setText(LocalDate.now().format(DATE_FORMAT));
        setClientTimeZoneSelection(TimeZoneSupport.DEFAULT_ZONE_ID);
        txtNotes.setText("");
        txtSessionNotes.setText("");
        selectedClientLabel.setText(R.string.text_select_client_schedule);
        sessionsList.removeAllViews();
        txtFullName.requestFocus();
    }

    private void selectClient(Client client) {
        selectedClientId = client.id;
        txtFullName.setText(client.fullName);
        txtPhone.setText(client.phone);
        txtEmail.setText(client.email);
        txtDateOfBirth.setText(client.dateOfBirth == null || client.dateOfBirth.isEmpty()
                ? LocalDate.now().format(DATE_FORMAT)
                : client.dateOfBirth);
        setClientTimeZoneSelection(client.timeZoneId);
        txtNotes.setText(client.notes);
        selectedClientLabel.setText(getString(R.string.selected_client_format, client.fullName));
        loadClients();
        loadSessionsForSelectedClient();
    }

    private void loadClients() {
        if (clientsList == null) {
            return;
        }

        clientsList.removeAllViews();
        String search = txtSearch == null ? "" : txtSearch.getText().toString();
        List<Client> clients = db.getClients(search);
        if (clients.isEmpty()) {
            clientsList.addView(emptyText(R.string.empty_no_clients));
            return;
        }

        for (Client client : clients) {
            boolean selected = selectedClientId != null && selectedClientId == client.id;
            TextView row = rowText(client.fullName, clientSummary(client), R.drawable.ic_person_24, selected);
            row.setOnClickListener(v -> selectClient(client));
            clientsList.addView(row);
        }
    }

    private void scheduleSession() {
        if (selectedClientId == null) {
            toast(R.string.toast_select_client_schedule);
            return;
        }

        Instant sessionAt;
        int duration;
        try {
            LocalDate date = parseDate(txtSessionDate);
            LocalTime time = parseTime(txtSessionTime);
            sessionAt = ZonedDateTime.of(date, time, selectedClientZone()).toInstant();
            duration = parseDuration();
        } catch (IllegalArgumentException ex) {
            toast(ex.getMessage());
            return;
        }

        long sessionId = db.addSession(
                selectedClientId,
                sessionAt.toString(),
                duration,
                txtSessionNotes.getText().toString());
        txtSessionNotes.setText("");
        loadSessionsForSelectedClient();
        ReminderScheduler.scheduleSession(this, sessionId);
        toast(R.string.toast_session_scheduled);
    }

    private void loadSessionsForSelectedClient() {
        if (sessionsList == null) {
            return;
        }

        sessionsList.removeAllViews();
        if (selectedClientId == null) {
            sessionsList.addView(emptyText(R.string.empty_no_client_selected));
            return;
        }

        List<ClientSession> sessions = db.getSessionsForClient(selectedClientId);
        if (sessions.isEmpty()) {
            sessionsList.addView(emptyText(R.string.empty_no_sessions));
            return;
        }

        ZoneId clientZone = selectedClientZone();
        for (ClientSession session : sessions) {
            LinearLayout row = horizontal();
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView sessionText = rowText(
                    formatSessionTitle(session, clientZone),
                    formatSessionDetails(session, clientZone),
                    R.drawable.ic_calendar_24,
                    false);
            addWeighted(row, sessionText);
            Button delete = UiKit.dangerButton(this, R.string.action_delete, R.drawable.ic_delete_24);
            delete.setOnClickListener(v -> confirmDeleteSession(session));
            row.addView(delete);
            sessionsList.addView(row);
        }
    }

    private void confirmDeleteSession(ClientSession session) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_session_title)
                .setMessage(R.string.dialog_delete_session_message)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    if (selectedClientId != null) {
                        db.deleteSession(session.id, selectedClientId);
                        ReminderScheduler.cancelSession(this, session.id);
                        loadSessionsForSelectedClient();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private String clientSummary(Client client) {
        StringBuilder builder = new StringBuilder();
        if (client.phone != null && !client.phone.isEmpty()) {
            builder.append(client.phone);
        }
        if (client.email != null && !client.email.isEmpty()) {
            if (builder.length() > 0) {
                builder.append("  |  ");
            }
            builder.append(client.email);
        }
        if (client.dateOfBirth != null && !client.dateOfBirth.isEmpty()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(getString(R.string.summary_born_format, formatDateOnly(client.dateOfBirth)));
        }
        if (client.timeZoneId != null && !client.timeZoneId.isEmpty()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(getString(R.string.summary_time_zone_format, client.timeZoneId));
        }
        return builder.length() == 0 ? getString(R.string.summary_no_phone_email) : builder.toString();
    }

    private String formatSessionTitle(ClientSession session, ZoneId clientZone) {
        return getString(
                R.string.session_title_format,
                DISPLAY_FORMAT.format(session.sessionInstant().atZone(clientZone)),
                session.durationMinutes);
    }

    private String formatSessionDetails(ClientSession session, ZoneId clientZone) {
        StringBuilder builder = new StringBuilder();
        builder.append(getString(R.string.session_time_zone_format, clientZone.getId()));
        if (session.sessionNotes != null && !session.sessionNotes.trim().isEmpty()) {
            builder.append('\n').append(session.sessionNotes.trim());
        }
        return builder.toString();
    }

    private ZoneId selectedClientZone() {
        Client client = selectedClientId == null ? null : db.getClient(selectedClientId);
        return TimeZoneSupport.zoneId(client == null ? selectedTimeZoneId() : client.timeZoneId);
    }

    private String selectedTimeZoneId() {
        if (spinnerClientTimeZone == null || spinnerClientTimeZone.getSelectedItem() == null) {
            return TimeZoneSupport.DEFAULT_ZONE_ID;
        }

        Object selected = spinnerClientTimeZone.getSelectedItem();
        if (selected instanceof TimeZoneSupport.TimeZoneOption) {
            return ((TimeZoneSupport.TimeZoneOption) selected).zoneId();
        }

        return TimeZoneSupport.normalizeZoneId(selected.toString());
    }

    private void setClientTimeZoneSelection(String zoneId) {
        if (spinnerClientTimeZone != null) {
            spinnerClientTimeZone.setSelection(TimeZoneSupport.indexOf(zoneId));
        }
    }

    private Spinner timeZoneSpinner(String selectedZoneId) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<TimeZoneSupport.TimeZoneOption> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                TimeZoneSupport.availableZoneOptions());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(TimeZoneSupport.indexOf(selectedZoneId));
        spinner.setPrompt(getString(R.string.hint_client_time_zone));
        UiKit.styleSpinner(spinner);
        LinearLayout.LayoutParams params = fullWidthParams();
        params.setMargins(0, dp(6), 0, dp(6));
        spinner.setLayoutParams(params);
        return spinner;
    }

    private static String formatDateOnly(String isoDate) {
        try {
            return DateTimeFormatter.ofPattern("dd/MM/yyyy").format(LocalDate.parse(isoDate));
        } catch (RuntimeException ignored) {
            return isoDate;
        }
    }

    private LocalDate parseDate(EditText editText) {
        String text = editText.getText().toString().trim();
        try {
            return LocalDate.parse(text, DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(getString(R.string.error_date_format));
        }
    }

    private LocalTime parseTime(EditText editText) {
        String text = editText.getText().toString().trim();
        try {
            return LocalTime.parse(text, TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(getString(R.string.error_time_format));
        }
    }

    private int parseDuration() {
        try {
            int value = Integer.parseInt(txtDurationMinutes.getText().toString().trim());
            if (value < 15 || value > 240) {
                throw new IllegalArgumentException(getString(R.string.error_duration_range));
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(getString(R.string.error_duration_number));
        }
    }

    private void showDatePicker(EditText target) {
        LocalDate initial;
        try {
            initial = LocalDate.parse(target.getText().toString().trim(), DATE_FORMAT);
        } catch (RuntimeException ignored) {
            initial = LocalDate.now();
        }

        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) ->
                        target.setText(LocalDate.of(year, month + 1, dayOfMonth).format(DATE_FORMAT)),
                initial.getYear(),
                initial.getMonthValue() - 1,
                initial.getDayOfMonth()).show();
    }

    private void showTimePicker(EditText target) {
        LocalTime initial;
        try {
            initial = LocalTime.parse(target.getText().toString().trim(), TIME_FORMAT);
        } catch (RuntimeException ignored) {
            initial = LocalTime.of(9, 0);
        }

        new TimePickerDialog(
                this,
                (view, hourOfDay, minute) ->
                        target.setText(String.format("%02d:%02d", hourOfDay, minute)),
                initial.getHour(),
                initial.getMinute(),
                true).show();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS }, REQUEST_POST_NOTIFICATIONS);
        }
    }

    private EditText input(int hintResId, int inputType, int iconResId) {
        EditText editText = new EditText(this);
        editText.setHint(hintResId);
        editText.setSingleLine((inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
        editText.setInputType(inputType);
        UiKit.styleInput(editText, iconResId);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(6), 0, dp(6));
        editText.setLayoutParams(params);
        return editText;
    }

    private LinearLayout twoColumn(View left, View right) {
        LinearLayout row = horizontal();
        addWeighted(row, left);
        addWeighted(row, right);
        return row;
    }

    private void addWeighted(LinearLayout row, View view) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f);
        params.setMargins(dp(3), dp(6), dp(3), dp(6));
        row.addView(view, params);
    }

    private LinearLayout.LayoutParams fullWidthParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void addSectionTitle(LinearLayout root, int textResId, int iconResId) {
        TextView title = new TextView(this);
        title.setText(textResId);
        UiKit.styleSectionTitle(title, iconResId);
        root.addView(title);
    }

    private void addFieldLabel(LinearLayout root, int textResId, int iconResId) {
        TextView label = new TextView(this);
        label.setText(textResId);
        label.setTextColor(UiKit.COLOR_MUTED);
        label.setTextSize(13);
        label.setGravity(Gravity.CENTER_VERTICAL);
        label.setPadding(dp(4), dp(10), dp(4), 0);
        label.setCompoundDrawablePadding(dp(8));
        label.setCompoundDrawablesRelative(
                UiKit.icon(this, iconResId, UiKit.COLOR_MUTED, 18),
                null,
                null,
                null);
        root.addView(label, fullWidthParams());
    }

    private TextView rowText(String title, String details, int iconResId, boolean selected) {
        TextView row = new TextView(this);
        row.setText(rowContent(title, details));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(4), 0, dp(4));
        row.setLayoutParams(params);
        UiKit.styleRow(row, iconResId, selected);
        return row;
    }

    private SpannableString rowContent(String title, String details) {
        String text = details == null || details.isEmpty() ? title : title + "\n" + details;
        SpannableString content = new SpannableString(text);
        content.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (text.length() > title.length()) {
            content.setSpan(
                    new ForegroundColorSpan(UiKit.COLOR_MUTED),
                    title.length() + 1,
                    text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return content;
    }

    private TextView emptyText(int textResId) {
        TextView view = new TextView(this);
        view.setText(textResId);
        LinearLayout.LayoutParams params = fullWidthParams();
        params.setMargins(0, dp(4), 0, dp(4));
        view.setLayoutParams(params);
        UiKit.styleEmptyText(view, R.drawable.ic_notes_24);
        return view;
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void toast(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return UiKit.dp(this, value);
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}
