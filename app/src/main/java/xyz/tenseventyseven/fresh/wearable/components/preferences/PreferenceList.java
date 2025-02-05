package xyz.tenseventyseven.fresh.wearable.components.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.databinding.WearPreferenceListBinding;
import xyz.tenseventyseven.fresh.wearable.interfaces.DeviceSetting;

public class PreferenceList extends LinearLayout {
    private WearPreferenceListBinding binding;
    private GBDevice device;
    private List<DeviceSetting> settings;
    private PreferenceAdapter adapter;

    public PreferenceList(Context context) {
        super(context);
    }

    public PreferenceList(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PreferenceList(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PreferenceList(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setSettings(Context context, GBDevice device, List<DeviceSetting> settings) {
        this.device = device;
        this.settings = settings;
        init(context);
    }

    public void removeListener() {
        if (adapter == null) {
            return;
        }

        adapter.removeListener();
    }

    private void init(Context context) {
        binding = WearPreferenceListBinding.inflate(LayoutInflater.from(context), this, true);
        RecyclerView recyclerView = binding.preferenceListRecycler;
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        if (adapter != null) {
            adapter.removeListener();
        }

        adapter = new PreferenceAdapter(context, device, settings);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new PreferenceItemDecoration(context, settings));
    }
}
