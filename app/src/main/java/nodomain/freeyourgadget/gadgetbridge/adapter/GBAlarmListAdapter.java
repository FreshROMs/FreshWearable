/*  Copyright (C) 2015-2024 Andreas Shimokawa, Arjan Schrijver, Carsten
    Pfeiffer, Daniele Gobbetti

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import androidx.cardview.widget.CardView;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.activities.ConfigureAlarms;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.Alarm;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

/**
 * Adapter for displaying GBAlarm instances.
 */
public class GBAlarmListAdapter extends RecyclerView.Adapter<GBAlarmListAdapter.ViewHolder> {

    private final Context mContext;
    private ArrayList<Alarm> alarmList;

    public GBAlarmListAdapter(Context context) {
        this.mContext = context;
    }

    public void setAlarmList(List<Alarm> alarms) {
        this.alarmList = new ArrayList<>(alarms);
    }

    public ArrayList<Alarm> getAlarmList() {
        return alarmList;
    }

    private void updateInDB(Alarm alarm) {
        DBHelper.store(alarm);
    }

    @NonNull
    @Override
    public GBAlarmListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alarm, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {

        final Alarm alarm = alarmList.get(position);

        holder.alarmDayMonday.setChecked(alarm.getRepetition(Alarm.ALARM_MON));
        holder.alarmDayTuesday.setChecked(alarm.getRepetition(Alarm.ALARM_TUE));
        holder.alarmDayWednesday.setChecked(alarm.getRepetition(Alarm.ALARM_WED));
        holder.alarmDayThursday.setChecked(alarm.getRepetition(Alarm.ALARM_THU));
        holder.alarmDayFriday.setChecked(alarm.getRepetition(Alarm.ALARM_FRI));
        holder.alarmDaySaturday.setChecked(alarm.getRepetition(Alarm.ALARM_SAT));
        holder.alarmDaySunday.setChecked(alarm.getRepetition(Alarm.ALARM_SUN));
        holder.container.setAlpha(alarm.getUnused() ? 0.5f : 1.0f);
        holder.isEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    alarm.setUnused(false);
                    holder.container.setAlpha(1.0f);
                }
                alarm.setEnabled(isChecked);
                updateInDB(alarm);
            }
        });

        holder.container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ConfigureAlarms) mContext).configureAlarm(alarm);
            }
        });
        holder.container.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                alarm.setUnused(!alarm.getUnused());
                holder.container.setAlpha(alarm.getUnused() ? 0.5f : 1.0f);
                holder.isEnabled.setChecked(false); // This falls through to the onCheckChanged function
                updateInDB(alarm);
                return true;
            }
        });

        holder.alarmTime.setText(DateTimeUtils.formatTime(alarm.getHour(), alarm.getMinute()));
        holder.isEnabled.setChecked(alarm.getEnabled());
        if (alarm.getSmartWakeup()) {
            holder.isSmartWakeup.setVisibility(TextView.VISIBLE);
        } else {
            holder.isSmartWakeup.setVisibility(TextView.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return alarmList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        CardView container;

        TextView alarmTime;
        SwitchCompat isEnabled;
        TextView isSmartWakeup;

        CheckedTextView alarmDayMonday;
        CheckedTextView alarmDayTuesday;
        CheckedTextView alarmDayWednesday;
        CheckedTextView alarmDayThursday;
        CheckedTextView alarmDayFriday;
        CheckedTextView alarmDaySaturday;
        CheckedTextView alarmDaySunday;

        ViewHolder(View view) {
            super(view);

            container = view.findViewById(R.id.card_view);

            alarmTime = view.findViewById(R.id.alarm_item_time);
            isEnabled = view.findViewById(R.id.alarm_item_toggle);
            isSmartWakeup = view.findViewById(R.id.alarm_smart_wakeup);

            alarmDayMonday = view.findViewById(R.id.alarm_item_monday);
            alarmDayTuesday = view.findViewById(R.id.alarm_item_tuesday);
            alarmDayWednesday = view.findViewById(R.id.alarm_item_wednesday);
            alarmDayThursday = view.findViewById(R.id.alarm_item_thursday);
            alarmDayFriday = view.findViewById(R.id.alarm_item_friday);
            alarmDaySaturday = view.findViewById(R.id.alarm_item_saturday);
            alarmDaySunday = view.findViewById(R.id.alarm_item_sunday);
        }
    }

}
