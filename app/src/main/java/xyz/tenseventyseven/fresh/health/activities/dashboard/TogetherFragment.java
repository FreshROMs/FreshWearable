package xyz.tenseventyseven.fresh.health.activities.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import xyz.tenseventyseven.fresh.databinding.HealthFragmentTogetherBinding;

public class TogetherFragment extends MainFragmentCommon {

    @Override
    public String getTitle() {
        return "Together";
    }

    private HealthFragmentTogetherBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = HealthFragmentTogetherBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textMain;
        textView.setText("This is the Together fragment");
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}