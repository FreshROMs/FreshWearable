package xyz.tenseventyseven.fresh.wearable.activities.devicesettings.alarms;

import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.picker.widget.SeslTimePicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.Alarm;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.common.AbstractNoActionBarActivity;
import xyz.tenseventyseven.fresh.databinding.WearActivityAlarmDetailBinding;

public class AlarmDetailActivity extends AbstractNoActionBarActivity {
    private WearActivityAlarmDetailBinding binding;
    private GBDevice device;
    private Alarm alarm;

    private int hour = -1;
    private int minute = -1;
    private int repetitionMask = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = WearActivityAlarmDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        device = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        if (device == null) {
            Toast.makeText(this, "Device not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        alarm = (Alarm) getIntent().getSerializableExtra(Alarm.EXTRA_ALARM);
        if (alarm == null) {
            Toast.makeText(this, "Alarm not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupTimePicker();
        setupButtons();
        setupAlarm();
    }

    private void setupTimePicker() {
        float size = 48.0f;
        binding.timePicker.setNumberPickerTextSize(SeslTimePicker.PICKER_HOUR, size);
        binding.timePicker.setNumberPickerTextSize(SeslTimePicker.PICKER_MINUTE, size);
        binding.timePicker.setNumberPickerTextSize(SeslTimePicker.PICKER_DIVIDER, size);
        binding.timePicker.setNumberPickerTextSize(SeslTimePicker.PICKER_AMPM, size * 0.55f);

        // Make typeface of family "sec" at 600 weight
        Typeface typeface;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            typeface = new Typeface.Builder("sec")
                    .setFallback("sec")
                    .setWeight(600)
                    .build();
        } else {
            typeface = Typeface.create("sec-roboto-light", Typeface.BOLD);
        }

        binding.timePicker.setNumberPickerTextTypeface(SeslTimePicker.PICKER_HOUR, typeface);
        binding.timePicker.setNumberPickerTextTypeface(SeslTimePicker.PICKER_MINUTE, typeface);
        binding.timePicker.setNumberPickerTextTypeface(SeslTimePicker.PICKER_AMPM, typeface);
    }

    private void setupButtons() {
        binding.btnCancel.setOnClickListener(v -> {
            finish();
        });
        binding.btnSave.setOnClickListener(v -> {
            saveAlarm();
            finish();
        });
    }

    private void saveAlarm() {
        alarm.setHour(hour);
        alarm.setMinute(minute);
        alarm.setRepetition(repetitionMask);

        DBHelper.store(device, alarm);
    }

    private void setupAlarm() {
        hour = alarm.getHour();
        minute = alarm.getMinute();
        repetitionMask = alarm.getRepetition();

        binding.timePicker.setHour(hour);
        binding.timePicker.setMinute(minute);
        binding.timePicker.setOnTimeChangedListener((view, hourOfDay, minute) -> {
            hour = hourOfDay;
            this.minute = minute;
            updateRepetitionText();
        });

        setupDayToggle(binding.daySunday, Alarm.ALARM_SUN);
        setupDayToggle(binding.dayMonday, Alarm.ALARM_MON);
        setupDayToggle(binding.dayTuesday, Alarm.ALARM_TUE);
        setupDayToggle(binding.dayWednesday, Alarm.ALARM_WED);
        setupDayToggle(binding.dayThursday, Alarm.ALARM_THU);
        setupDayToggle(binding.dayFriday, Alarm.ALARM_FRI);
        setupDayToggle(binding.daySaturday, Alarm.ALARM_SAT);
        updateRepetitionText();
    }

    private void setupDayToggle(ToggleButton button, byte day) {
        button.setChecked(alarm.getRepetition(day));
        button.setOnCheckedChangeListener((v, checked) -> {
            if (checked) {
                repetitionMask |= day;
            } else {
                repetitionMask &= ~day;
            }
            updateRepetitionText();
        });
    }

    private void updateRepetitionText() {
        String text;
        if (repetitionMask == 0) {
            Date alarmDate = getAlarmDate();
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d", Locale.US);
            text = dateFormat.format(alarmDate);
        } else if (repetitionMask == 0b01111111) {
            text = getString(R.string.wear_device_alarms_repeat_text_daily);
        } else {
            List<String> days = new ArrayList<>();
            if ((repetitionMask & Alarm.ALARM_SUN) > 0) {
                days.add(getString(R.string.alarm_sun_short));
            }

            if ((repetitionMask & Alarm.ALARM_MON) > 0) {
                days.add(getString(R.string.alarm_mon_short));
            }

            if ((repetitionMask & Alarm.ALARM_TUE) > 0) {
                days.add(getString(R.string.alarm_tue_short));
            }

            if ((repetitionMask & Alarm.ALARM_WED) > 0) {
                days.add(getString(R.string.alarm_wed_short));
            }

            if ((repetitionMask & Alarm.ALARM_THU) > 0) {
                days.add(getString(R.string.alarm_thu_short));
            }

            if ((repetitionMask & Alarm.ALARM_FRI) > 0) {
                days.add(getString(R.string.alarm_fri_short));
            }

            if ((repetitionMask & Alarm.ALARM_SAT) > 0) {
                days.add(getString(R.string.alarm_sat_short));
            }

            text = getString(R.string.wear_device_alarms_repeat_text, String.join(", ", days));
        }

        binding.nextAlarmDate.setText(text);
    }

    @NonNull
    private Date getAlarmDate() {
        Calendar calendar = Calendar.getInstance();
        Calendar now = Calendar.getInstance();

        // Set the alarm time
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If alarm time has already passed today, set it for tomorrow
        if (calendar.before(now)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        Date alarmDate = calendar.getTime();
        return alarmDate;
    }
}