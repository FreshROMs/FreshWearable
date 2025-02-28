package xyz.tenseventyseven.fresh.wearable.adapters;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckedTextView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.Alarm;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.wearable.activities.devicesettings.alarms.AlarmsActivity;
import xyz.tenseventyseven.fresh.wearable.adapters.base.SelectableListAdapter;

public class AlarmListAdapter extends SelectableListAdapter<Alarm, AlarmListAdapter.ViewHolder> {
    private GBDevice device;

    public interface AlarmListAdapterListener extends EditStateListener {
        void onLongClickAlarm(Alarm alarm);
    }

    public AlarmListAdapter(Context context) {
        super(context);
    }

    public AlarmListAdapter(Context context, GBDevice device) {
        super(context);
        this.device = device;
    }

    @Override
    protected void onItemUpdate(Alarm alarm) {
        DBHelper.store(alarm);
        Application.deviceService(device).onSetAlarms(getItems());
    }

    @Override
    protected void updateEditModeViews(ViewHolder holder, int position) {
        Alarm alarm = items.get(position);
        holder.selected.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        holder.enabled.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
        holder.selected.setChecked(selectedItems.contains(alarm));
    }

    @NonNull
    @Override
    public AlarmListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.wear_alarm_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        Alarm alarm = items.get(position);
        holder.alarm = alarm;
        holder.selected.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        holder.selected.setChecked(selectedItems.contains(alarm));
        holder.selected.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !selectedItems.contains(alarm)) {
                selectedItems.add(alarm);
            } else if (!isChecked) {
                selectedItems.remove(alarm);
            }
            notifySelectionChanged();
        });

        holder.enabled.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
        holder.enabled.setChecked(alarm.getEnabled());
        holder.enabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            alarm.setUnused(false);
            alarm.setEnabled(isChecked);
            onItemUpdate(alarm);
        });
        setupTime(holder, alarm);
        setupRepeatDays(holder, alarm);
        holder.item.setOnClickListener(v -> {
            if (isEditMode) {
                holder.selected.setChecked(!holder.selected.isChecked());
            } else {
                if (context instanceof AlarmsActivity) {
                    ((AlarmsActivity) context).onClickAlarm(alarm);
                }
            }
        });
        holder.item.setOnLongClickListener(v -> {
            if (isEditMode) {
                return true; // Do nothing
            }
            if (listener instanceof AlarmListAdapterListener) {
                ((AlarmListAdapterListener) listener).onLongClickAlarm(alarm);
            }
            return true;
        });
    }


    private void setupTime(ViewHolder holder, Alarm alarm) {
        int hour = alarm.getHour();
        int minute = alarm.getMinute();

        if (DateFormat.is24HourFormat(context)) {
            holder.time.setText(formatTime(hour, minute));
            return;
        }

        String suffix = hour < 12 ? "AM" : "PM";
        switch (hour) {
            case 0:
            case 12:
                hour = 12;
                break;
            default:
                hour = hour > 12 ? hour - 12 : hour;
        }

        holder.time.setText(formatTime(hour, minute));
        holder.unit.setText(suffix);
    }

    private static String formatTime(int hours, int minutes) {
        return String.format(Locale.US, "%01d", hours) + ":" + String.format(Locale.US, "%02d", minutes);
    }

    private void setupRepeatDays(ViewHolder holder, Alarm alarm) {
        if (alarm.isRepetitive()) {
            holder.dayOfWeek.setVisibility(View.VISIBLE);
            holder.dateOnce.setVisibility(View.GONE);
        } else {
            holder.dayOfWeek.setVisibility(View.GONE);
            holder.dateOnce.setVisibility(View.VISIBLE);
            setupOnceDate(holder, alarm);
            return;
        }

        holder.monday.setChecked(alarm.getRepetition(Alarm.ALARM_MON));
        holder.tuesday.setChecked(alarm.getRepetition(Alarm.ALARM_TUE));
        holder.wednesday.setChecked(alarm.getRepetition(Alarm.ALARM_WED));
        holder.thursday.setChecked(alarm.getRepetition(Alarm.ALARM_THU));
        holder.friday.setChecked(alarm.getRepetition(Alarm.ALARM_FRI));
        holder.saturday.setChecked(alarm.getRepetition(Alarm.ALARM_SAT));
        holder.sunday.setChecked(alarm.getRepetition(Alarm.ALARM_SUN));
    }

    private void setupOnceDate(ViewHolder holder, Alarm alarm) {
        // Alarm only gives hour and minute, so we can't simply just format it
        // Format needed is the date when the alarm will ring. e.g. Tue, Feb 25
        Calendar calendar = Calendar.getInstance();
        Calendar now = Calendar.getInstance();

        // Set the alarm time
        calendar.set(Calendar.HOUR_OF_DAY, alarm.getHour());
        calendar.set(Calendar.MINUTE, alarm.getMinute());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If alarm time has already passed today, set it for tomorrow
        if (calendar.before(now)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        Date alarmDate = calendar.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d", Locale.US);
        holder.dateOnce.setText(dateFormat.format(alarmDate));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final LinearLayout item;
        public final TextView time;
        public final TextView unit;
        public final CheckBox selected;
        public final SwitchCompat enabled;
        public Alarm alarm;

        public final LinearLayout dayOfWeek;
        public final AppCompatCheckedTextView monday;
        public final AppCompatCheckedTextView tuesday;
        public final AppCompatCheckedTextView wednesday;
        public final AppCompatCheckedTextView thursday;
        public final AppCompatCheckedTextView friday;
        public final AppCompatCheckedTextView saturday;
        public final AppCompatCheckedTextView sunday;
        public final TextView dateOnce;

        public ViewHolder(View itemView) {
            super(itemView);
            item = itemView.findViewById(R.id.alarm_item);
            time = itemView.findViewById(R.id.alarm_time);
            unit = itemView.findViewById(R.id.alarm_unit);
            selected = itemView.findViewById(R.id.alarm_selected);
            enabled = itemView.findViewById(R.id.alarm_switch);

            dayOfWeek = itemView.findViewById(R.id.alarm_day_of_week_repeat);
            monday = itemView.findViewById(R.id.alarm_repeat_monday);
            tuesday = itemView.findViewById(R.id.alarm_repeat_tuesday);
            wednesday = itemView.findViewById(R.id.alarm_repeat_wednesday);
            thursday = itemView.findViewById(R.id.alarm_repeat_thursday);
            friday = itemView.findViewById(R.id.alarm_repeat_friday);
            saturday = itemView.findViewById(R.id.alarm_repeat_saturday);
            sunday = itemView.findViewById(R.id.alarm_repeat_sunday);

            dateOnce = itemView.findViewById(R.id.alarm_day_once);
        }
    }
}
