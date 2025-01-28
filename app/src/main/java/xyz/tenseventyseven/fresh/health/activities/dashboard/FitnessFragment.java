package xyz.tenseventyseven.fresh.health.activities.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import xyz.tenseventyseven.fresh.databinding.HealthFragmentFitnessBinding;

public class FitnessFragment extends MainFragmentCommon {

    private HealthFragmentFitnessBinding binding;

    @Override
    public String getTitle() {
        return "Fitness";
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = HealthFragmentFitnessBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textMain;
        textView.setText("This is the fitness fragment");
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}