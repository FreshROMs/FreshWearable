package xyz.tenseventyseven.fresh.health.data.models;

import androidx.core.content.ContextCompat;

import java.io.Serializable;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.WearableApplication;

public class NightSleepDataModel implements Serializable {
    public SleepState state;
    public int start;
    public int end;

    public boolean isAwake() {
        return state == SleepState.AWAKE;
    }

    public boolean isAsleep() {
        return state != SleepState.AWAKE;
    }

    public int getColor() {
        return state.getColor();
    }

    public enum SleepState {
        AWAKE,
        LIGHT,
        DEEP,
        REM;

        public static SleepState fromActivityKind(ActivityKind kind) {
            switch (kind) {
                case DEEP_SLEEP:
                    return DEEP;
                case LIGHT_SLEEP:
                    return LIGHT;
                case REM_SLEEP:
                    return REM;
                case AWAKE_SLEEP:
                default:
                    return AWAKE;
            }
        }

        public int getColor() {
            switch (this) {
                case DEEP:
                    return ContextCompat.getColor(WearableApplication.getContext(), R.color.health_sleep_deep);
                case LIGHT:
                    return ContextCompat.getColor(WearableApplication.getContext(), R.color.health_sleep_light);
                case REM:
                    return ContextCompat.getColor(WearableApplication.getContext(), R.color.health_sleep_rem);
                case AWAKE:
                default:
                    return ContextCompat.getColor(WearableApplication.getContext(), R.color.health_sleep_awake);
            }
        }
    }
}
