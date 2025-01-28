/*  Copyright (C) 2024 Arjan Schrijver

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
package xyz.tenseventyseven.fresh.wearable.activities.onboarding;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.util.PermissionsUtils;
import xyz.tenseventyseven.fresh.wearable.activities.OnboardingActivity;

public class PermissionsFragment extends Fragment {
    private static final Logger LOG = LoggerFactory.getLogger(PermissionsFragment.class);

    private RecyclerView permissionsListView;
    private PermissionAdapter permissionAdapter;
    private List<String> requestingPermissions = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_onboarding_permissions, container, false);

//        if (((AppCompatActivity)getActivity()).getSupportActionBar().isShowing()) {
//            // Hide title when the Action Bar is visible (i.e. when not in the first run flow)
//            view.findViewById(R.id.onboarding_permissions_title).setVisibility(View.GONE);
//        }

        // Initialize RecyclerView and data
        permissionsListView = view.findViewById(R.id.permissions_list);

        // Set up RecyclerView
        permissionAdapter = new PermissionAdapter(PermissionsUtils.getRequiredPermissionsList(requireActivity()), requireContext());
        permissionsListView.setLayoutManager(new LinearLayoutManager(requireContext()));
        permissionsListView.setAdapter(permissionAdapter);


        final AppCompatButton nextButton = view.findViewById(R.id.permisisons_continue);
        nextButton.setOnClickListener(v -> {
            List<PermissionsUtils.PermissionDetails> wantedPermissions = PermissionsUtils.getRequiredPermissionsList(requireActivity());
            requestingPermissions = new ArrayList<>();
            for (PermissionsUtils.PermissionDetails wantedPermission : wantedPermissions) {
                requestingPermissions.add(wantedPermission.getPermission());
            }
            requestAllPermissions();

            if (PermissionsUtils.checkAllPermissions(requireActivity())) {
                // Move to the next fragment from the viewpager
                OnboardingActivity activity = (OnboardingActivity) requireActivity();
                activity.nextFragment();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        permissionAdapter.notifyDataSetChanged();
        if (!requestingPermissions.isEmpty()) {
            requestAllPermissions();
        }
    }

    public void requestAllPermissions() {
        if (!requestingPermissions.isEmpty()) {
            Iterator<String> it = requestingPermissions.iterator();
            while (it.hasNext()) {
                String currentPermission = it.next();
                if (PermissionsUtils.specialPermissions.contains(currentPermission)) {
                    it.remove();
                    if (!PermissionsUtils.checkPermission(requireActivity(), currentPermission)) {
                        PermissionsUtils.requestPermission(requireActivity(), currentPermission);
                        return;
                    }
                }
            }
            String[] combinedPermissions = requestingPermissions.toArray(new String[0]);
            requestingPermissions.clear();
            ActivityCompat.requestPermissions(requireActivity(), combinedPermissions, 0);
        } else {
            // Move to the next fragment from the viewpager
            OnboardingActivity activity = (OnboardingActivity) requireActivity();
            activity.nextFragment();
        }
    }

    private static class PermissionHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView summaryTextView;
        ImageView checkmarkImageView;
        ImageView permissionIcon;

        public PermissionHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.permission_title);
            summaryTextView = itemView.findViewById(R.id.permission_summary);
            permissionIcon = itemView.findViewById(R.id.permission_icon);
            checkmarkImageView = itemView.findViewById(R.id.permission_check);
        }
    }

    private class PermissionAdapter extends RecyclerView.Adapter<PermissionHolder> {
        private List<PermissionsUtils.PermissionDetails> permissionList;
        private Context context;

        public PermissionAdapter(List<PermissionsUtils.PermissionDetails> permissionList, Context context) {
            this.permissionList = permissionList;
            this.context = context;
        }

        @NonNull
        @Override
        public PermissionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_welcome_permission_row, parent, false);
            return new PermissionHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull PermissionHolder holder, int position) {
            PermissionsUtils.PermissionDetails permissionData = permissionList.get(position);
            holder.titleTextView.setText(permissionData.getTitle());
            holder.summaryTextView.setText(permissionData.getSummary());
            holder.permissionIcon.setImageResource(permissionData.getIcon());
            if (PermissionsUtils.checkPermission(requireContext(), permissionData.getPermission())) {
                holder.checkmarkImageView.setVisibility(View.VISIBLE);
            } else {
                holder.checkmarkImageView.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return permissionList.size();
        }
    }
}
