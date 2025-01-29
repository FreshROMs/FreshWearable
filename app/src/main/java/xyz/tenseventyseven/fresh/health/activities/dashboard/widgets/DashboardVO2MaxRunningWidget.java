package xyz.tenseventyseven.fresh.health.activities.dashboard.widgets;

import android.os.Bundle;

import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.health.activities.dashboard.utils.AbstractDashboardVO2MaxWidget;
import xyz.tenseventyseven.fresh.health.activities.dashboard.HomeFragment;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Vo2MaxSample;

public class DashboardVO2MaxRunningWidget extends AbstractDashboardVO2MaxWidget {

    public DashboardVO2MaxRunningWidget() {
        super(R.string.vo2max_running, "vo2max", ir.alirezaivaz.tablericons.R.drawable.ic_run);
    }

    public static DashboardVO2MaxRunningWidget newInstance(final HomeFragment.DashboardData dashboardData) {
        final DashboardVO2MaxRunningWidget fragment = new DashboardVO2MaxRunningWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    public Vo2MaxSample.Type getVO2MaxType() {
        return Vo2MaxSample.Type.RUNNING;
    }

    public String getWidgetKey() {
        return "vo2max_running";
    }

    @Override
    protected boolean isSupportedBy(final GBDevice device) {
        return device.getDeviceCoordinator().supportsVO2MaxRunning();
    }
}
