package xyz.tenseventyseven.fresh.health.activities.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import xyz.tenseventyseven.fresh.databinding.HealthFragmentProfileBinding;

public class ProfileFragment extends MainFragmentCommon {

    private HealthFragmentProfileBinding binding;

    @Override
    public String getTitle() {
        return "My page";
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = HealthFragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textMain;
        textView.setText("This is the profile fragment");
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}