package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi;

import android.graphics.Color;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetPart;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetPartSubtype;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetType;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiPreferences;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;

public class XiaomiWearableWidgetManager extends XiaomiWidgetManager {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiWearableWidgetManager.class);

    public XiaomiWearableWidgetManager(GBDevice device) {
        super(device);
    }

    private int getWidgetColor(final int widgetId) {
        switch (widgetId) {
            case 3089: // stats
            case 3105:
                return Color.parseColor("#f5ae15");
            case 3090: // steps
            case 3346:
            case 3362:
            case 3106:
                return Color.parseColor("#fcb317");
            case 3091: // calories
            case 3347:
            case 3107:
            case 3363:
                return Color.parseColor("#f86a11");
            case 3093: // moving
            case 3349:
            case 3109:
            case 3365:
                return Color.parseColor("#24b2f3");
            case 3092: // standing time
            case 3348:
            case 3108:
            case 3364:
                return Color.parseColor("#36cf6f");
            case 7183: // spo2 (oxygen)
            case 7441:
            case 7202:
            case 7458:
                return Color.parseColor("#fa224c");
            case 6161: // stress
            case 6417:
                return Color.parseColor("#06ddc7");
            case 8209: // sleep
            case 8241:
            case 8225:
            case 8481:
                return Color.parseColor("#6a5dfe");
            case 2065: // workout
            case 2321:
            case 2081:
            case 2337:
                return Color.parseColor("#ffe720");
            case 9249: // cycles
            case 9505:
                return Color.parseColor("#f5ae15");
            case 18465: // events
                return Color.parseColor("#297ee1");
            case 10257: // weather
            case 10513:
            case 10273:
            case 10529:
                return Color.parseColor("#5c9ecf");
            case 14353: // music
            case 14609:
            case 14369:
            case 14625:
                return Color.parseColor("#63d5b0");
            case 17: // battery
                return Color.parseColor("#32ef80");
            case 17525: // timer
            case 17442:
                return Color.parseColor("#2b93ff");
            case 4131: // heart rate
            case 4113:
            case 4369:
            case 4387:
                return Color.parseColor("#fa224c");
            default:
                return Application.getContext().getColor(dev.oneuiproject.oneui.design.R.color.oui_appinfolayout_button_bg_color);
        }
    }

    private int getWidgetIcon(final int widgetId) {
        switch (widgetId) {
            case 3089: // stats
            case 3105:
                return dev.oneuiproject.oneui.R.drawable.ic_oui_lightning;
            case 3090: // steps
            case 3346:
            case 3362:
            case 3106:
                return dev.oneuiproject.oneui.R.drawable.ic_oui_tag;
            case 3091: // calories
            case 3347:
            case 3107:
            case 3363:
                return dev.oneuiproject.oneui.R.drawable.ic_oui_food;
            case 3093: // moving
            case 3349:
            case 3109:
            case 3365:
                return dev.oneuiproject.oneui.R.drawable.ic_oui_accessibility_2;
            case 3092: // standing time
            case 3348:
            case 3108:
            case 3364:
                return dev.oneuiproject.oneui.R.drawable.ic_oui_contact;
            case 7183: // spo2 (oxygen)
            case 7441:
            case 7202:
            case 7458:
                return dev.oneuiproject.oneui.R.drawable.ic_oui_water_drop;
            case 6161: // stress
            case 6417:
                return dev.oneuiproject.oneui.R.drawable.ic_oui_emoji_category_angry;
            case 8209: // sleep
            case 8241:
            case 8225:
            case 8481:
                return dev.oneuiproject.oneui.R.drawable.ic_oui_bed;
            case 2065: // workout
            case 2321:
            case 2081:
            case 2337:
                return dev.oneuiproject.oneui.R.drawable.ic_oui_workout;
            case 9249: // cycles
            case 9505:
                return dev.oneuiproject.oneui.R.drawable.ic_oui_emoji;
            case 18465: // events
                return dev.oneuiproject.oneui.R.drawable.ic_oui_calendar;
            case 10257: // weather
            case 10513:
            case 10273:
            case 10529:
                return dev.oneuiproject.oneui.R.drawable.ic_oui_weather;
            case 14353: // music
            case 14609:
            case 14369:
            case 14625:
                return dev.oneuiproject.oneui.R.drawable.ic_oui_audio;
            case 17: // battery
                return dev.oneuiproject.oneui.R.drawable.ic_oui_battery;
            case 17525: // timer
            case 17442:
                return dev.oneuiproject.oneui.R.drawable.ic_oui_timer;
            case 4131: // heart rate
            case 4113:
            case 4369:
            case 4387:
                return dev.oneuiproject.oneui.R.drawable.ic_oui_health;
            default:
                return dev.oneuiproject.oneui.R.drawable.ic_oui_widget;
        }
    }

    @Override
    protected WidgetPart fromRawWidgetPart(final XiaomiProto.WidgetPart widgetPart, final Collection<WidgetPartSubtype> subtypes) {
        final WidgetType type = fromRawWidgetType(widgetPart.getType());

        if (type == null) {
            LOG.warn("Unknown widget type {}", widgetPart.getType());
            return null;
        }

        final String stringifiedId = String.valueOf(widgetPart.getId());
        final WidgetPart convertedPart = new WidgetPart(
                stringifiedId,
                Application.getContext().getString(R.string.widget_name_untitled, stringifiedId),
                type
        );

        if (!TextUtils.isEmpty(widgetPart.getTitle())) {
            convertedPart.setName(widgetPart.getTitle());
        } else {
            // some models do not provide the name of the widget in the screens list, resolve it here
            final XiaomiProto.WidgetPart resolvedPart = findRawPart(widgetPart.getType(), widgetPart.getId());
            if (resolvedPart != null) {
                convertedPart.setName(resolvedPart.getTitle());
            }
        }

        if (widgetPart.getFunction() == 16) {
            if (StringUtils.isBlank(convertedPart.getName())) {
                convertedPart.setName(Application.getContext().getString(R.string.menuitem_workout));
            }

            if (subtypes != null) {
                convertedPart.getSupportedSubtypes().addAll(subtypes);

                if (widgetPart.getSubType() != 0) {
                    final String widgetSubtype = String.valueOf(widgetPart.getSubType());

                    for (final WidgetPartSubtype availableSubtype : subtypes) {
                        if (availableSubtype.getId().equals(widgetSubtype)) {
                            convertedPart.setSubtype(availableSubtype);
                            break;
                        }
                    }
                }
            }
        }

        // Ensure name is always proper case
        convertedPart.setName(convertedPart.getName().toLowerCase().replaceFirst("^[a-z]", String.valueOf(Character.toUpperCase(convertedPart.getName().charAt(0)))));

        // Remove ID at the end if it exists (e.g. "Weather (10257)")
        if (convertedPart.getName().contains("(")) {
            convertedPart.setName(convertedPart.getName().replaceFirst("\\s*\\(\\d+\\)$", ""));
        }

        if ((widgetPart.getId() & 256) != 0) {
            convertedPart.setAlternate(true);
            convertedPart.setAlternateName(Application.getContext().getString(R.string.widget_name_colored_tile, convertedPart.getName()));
        }

        convertedPart.setIcon(getWidgetIcon(widgetPart.getId()));
        convertedPart.setColor(getWidgetColor(widgetPart.getId()));

        return convertedPart;
    }
}
