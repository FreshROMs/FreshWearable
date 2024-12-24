package xyz.tenseventyseven.fresh.wearable.activities.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import xyz.tenseventyseven.fresh.wearable.R;
import xyz.tenseventyseven.fresh.wearable.WearableApplication;
import xyz.tenseventyseven.fresh.wearable.activities.OnboardingActivity;

public class IntroFragment extends Fragment {
    private static final Logger LOG = LoggerFactory.getLogger(IntroFragment.class);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        final View view = inflater.inflate(R.layout.fragment_onboarding_intro, container, false);
        final AppCompatButton nextButton = view.findViewById(R.id.setup_continue);
        nextButton.setOnClickListener(v -> {
            // Move to the next fragment from the viewpager
            OnboardingActivity activity = (OnboardingActivity) requireActivity();
            activity.nextFragment();
        });

        return view;
    }
}
