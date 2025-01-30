package xyz.tenseventyseven.fresh.health.activities.dashboard.widgets;

import android.os.Bundle;

import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.health.activities.dashboard.utils.AbstractDashboardVO2MaxWidget;
import xyz.tenseventyseven.fresh.health.activities.dashboard.HomeFragment;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Vo2MaxSample;

public class DashboardVO2MaxAnyWidget extends AbstractDashboardVO2MaxWidget {

    public DashboardVO2MaxAnyWidget() {
        super(R.string.menuitem_vo2_max, "vo2max", ir.alirezaivaz.tablericons.R.drawable.ic_stretching_2);
    }

    public static DashboardVO2MaxAnyWidget newInstance(final HomeFragment.DashboardData dashboardData) {
        final DashboardVO2MaxAnyWidget fragment = new DashboardVO2MaxAnyWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    public Vo2MaxSample.Type getVO2MaxType() {
        return Vo2MaxSample.Type.ANY;
    }

    public String getWidgetKey() {
        return "vo2max";
    }

    @Override
    protected boolean isSupportedBy(final GBDevice device) {
        return device.getDeviceCoordinator().supportsVO2Max();
    }
}
