/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi;

import android.graphics.Color;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import xyz.tenseventyseven.fresh.WearableApplication;
import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetLayout;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetManager;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetPart;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetPartSubtype;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetScreen;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetType;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiPreferences;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class XiaomiWidgetManager implements WidgetManager {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiWidgetManager.class);

    private final GBDevice device;

    public XiaomiWidgetManager(final GBDevice device) {
        this.device = device;
    }

    @Override
    public List<WidgetLayout> getSupportedWidgetLayouts() {
        final List<WidgetLayout> layouts = new ArrayList<>();

        final XiaomiProto.WidgetScreens widgetScreens = getRawWidgetScreens();
        if (!widgetScreens.hasWidgetsCapabilities() || !widgetScreens.getWidgetsCapabilities().hasSupportedLayoutStyles()) {
            return Collections.emptyList();
        }

        final int supportedBitmap = getRawWidgetScreens().getWidgetsCapabilities().getSupportedLayoutStyles();

        // highest known layout style is 0x4000 (1 << 14)
        for (int i = 0; i < 15; i++) {
            final int layoutStyleId = 1 << i;
            if ((supportedBitmap & layoutStyleId) != 0) {
                layouts.add(fromRawLayout(layoutStyleId));
            }
        }

        return layouts;
    }

    private static Collection<WidgetPartSubtype> convertWorkoutTypesToPartSubtypes(final Collection<XiaomiWorkoutType> workoutTypes) {
        final List<WidgetPartSubtype> subtypes = new ArrayList<>(workoutTypes.size());

        // convert workout types to subtypes
        for (final XiaomiWorkoutType workoutType : workoutTypes) {
            subtypes.add(new WidgetPartSubtype(
                    String.valueOf(workoutType.getCode()),
                    workoutType.getName()
            ));
        }

        // sort by name before returning
        Collections.sort(subtypes, (it, other) -> it.getName().compareToIgnoreCase(other.getName()));

        return subtypes;
    }

    @Override
    public List<WidgetPart> getSupportedWidgetParts(final WidgetType targetWidgetType) {
        final List<WidgetPart> parts = new LinkedList<>();

        final XiaomiProto.WidgetParts rawWidgetParts = getRawWidgetParts();

        final Set<String> seenNames = new HashSet<>();
        final Set<String> duplicatedNames = new HashSet<>();

        // get supported workout types and convert to subtypes for workout widgets
        final Collection<WidgetPartSubtype> subtypes = convertWorkoutTypesToPartSubtypes(XiaomiWorkoutType.getWorkoutTypesSupportedByDevice(getDevice()));

        for (final XiaomiProto.WidgetPart widgetPart : rawWidgetParts.getWidgetPartList()) {
            final WidgetPart convertedPart = fromRawWidgetPart(widgetPart, subtypes);

            if (convertedPart == null) {
                continue;
            }

            if (!convertedPart.getType().equals(targetWidgetType)) {
                continue;
            }

            final String convertedPartName = convertedPart.getName();
            if (seenNames.contains(convertedPartName)) {
                duplicatedNames.add(convertedPartName);
            } else {
                seenNames.add(convertedPartName);
            }

            parts.add(convertedPart);
            seenNames.add(convertedPart.getName());
        }

        // Ensure that all names are unique
        for (final WidgetPart part : parts) {
            if (duplicatedNames.contains(part.getFullName())) {
                part.setName(part.getName());
            }
        }

        Collections.sort(parts, (it, other) -> it.getName().compareToIgnoreCase(other.getName()));
        return parts;
    }

    private WidgetPart fromRawWidgetPart(final XiaomiProto.WidgetPart widgetPart, final Collection<WidgetPartSubtype> subtypes) {
        final WidgetType type = fromRawWidgetType(widgetPart.getType());

        if (type == null) {
            LOG.warn("Unknown widget type {}", widgetPart.getType());
            return null;
        }

        final String stringifiedId = String.valueOf(widgetPart.getId());
        final WidgetPart convertedPart = new WidgetPart(
                stringifiedId,
                WearableApplication.getContext().getString(R.string.widget_name_untitled, stringifiedId),
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
                convertedPart.setName(WearableApplication.getContext().getString(R.string.menuitem_workout));
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
            convertedPart.setAlternateName(WearableApplication.getContext().getString(R.string.widget_name_colored_tile, convertedPart.getName()));
        }

        convertedPart.setIcon(getWidgetDrawableId(widgetPart.getId()));
        convertedPart.setColor(getWidgetColorId(widgetPart.getId()));

        return convertedPart;
    }

    private int getWidgetColorId(final int widgetId) {
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
                return WearableApplication.getContext().getColor(dev.oneuiproject.oneui.design.R.color.oui_appinfolayout_button_bg_color);
        }
    }

    private int getWidgetDrawableId(final int widgetId) {
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
    public List<WidgetScreen> getWidgetScreens() {

        final XiaomiProto.WidgetScreens rawWidgetScreens = getRawWidgetScreens();

        final List<WidgetScreen> convertedScreens = new ArrayList<>(rawWidgetScreens.getWidgetScreenCount());
        final Collection<WidgetPartSubtype> workoutTypes = convertWorkoutTypesToPartSubtypes(XiaomiWorkoutType.getWorkoutTypesSupportedByDevice(getDevice()));

        for (final XiaomiProto.WidgetScreen rawScreen : rawWidgetScreens.getWidgetScreenList()) {
            final WidgetLayout layout = fromRawLayout(rawScreen.getLayout());

            final List<WidgetPart> convertedParts = new ArrayList<>(rawScreen.getWidgetPartCount());

            for (final XiaomiProto.WidgetPart rawPart : rawScreen.getWidgetPartList()) {
                final WidgetPart convertedPart = fromRawWidgetPart(rawPart, workoutTypes);

                if (convertedPart == null) {
                    LOG.warn("Widget cannot be converted, result was null for following raw widget: {}", rawPart);
                    continue;
                }

                convertedParts.add(convertedPart);
            }

            convertedScreens.add(new WidgetScreen(
                    String.valueOf(rawScreen.getId()),
                    layout,
                    convertedParts
            ));
        }

        return convertedScreens;
    }

    @Override
    public GBDevice getDevice() {
        return device;
    }

    @Override
    public int getMinScreens() {
        return getRawWidgetScreens().getWidgetsCapabilities().getMinWidgets();
    }

    @Override
    public int getMaxScreens() {
        return getRawWidgetScreens().getWidgetsCapabilities().getMaxWidgets();
    }

    @Override
    public void saveScreen(final WidgetScreen widgetScreen) {
        final XiaomiProto.WidgetScreens rawWidgetScreens = getRawWidgetScreens();

        LOG.debug("Saving widget screen {}", widgetScreen.getId());

        final int layoutNum = toRawLayout(widgetScreen.getLayout());
        if (layoutNum == -1) {
            return;
        }

        XiaomiProto.WidgetScreen.Builder rawScreen = null;
        if (widgetScreen.getId() == null) {
            // new screen
            rawScreen = XiaomiProto.WidgetScreen.newBuilder()
                    .setId(rawWidgetScreens.getWidgetScreenCount() + 1); // ids start at 1
        } else {
            for (final XiaomiProto.WidgetScreen screen : rawWidgetScreens.getWidgetScreenList()) {
                LOG.debug("Checking screen {} against {}", screen.getId(), widgetScreen.getId());
                if (String.valueOf(screen.getId()).equals(String.valueOf(widgetScreen.getId()))) {
                    rawScreen = XiaomiProto.WidgetScreen.newBuilder(screen);
                    break;
                }
            }

            if (rawScreen == null) {
                LOG.warn("Failed to find original screen for {}", widgetScreen.getId());
                rawScreen = XiaomiProto.WidgetScreen.newBuilder()
                        .setId(rawWidgetScreens.getWidgetScreenCount() + 1);
            }
        }

        rawScreen.setLayout(layoutNum);
        rawScreen.clearWidgetPart();

        final Collection<XiaomiWorkoutType> workoutTypes = XiaomiWorkoutType.getWorkoutTypesSupportedByDevice(getDevice());

        for (final WidgetPart newPart : widgetScreen.getParts()) {
            // Find the existing raw part
            final XiaomiProto.WidgetPart knownRawPart = findRawPart(
                    toRawWidgetType(newPart.getType()),
                    Integer.parseInt(Objects.requireNonNull(newPart.getId()))
            );

            final XiaomiProto.WidgetPart.Builder newRawPartBuilder = XiaomiProto.WidgetPart.newBuilder(knownRawPart);

            // TODO only support subtypes on widget with type 16
            if (newPart.getSubtype() != null) {
                try {
                    final int rawSubtype = Integer.parseInt(newPart.getSubtype().getId());

                    // Get the workout type as subtype
                    for (final XiaomiWorkoutType workoutType : workoutTypes) {
                        if (rawSubtype == workoutType.getCode()) {
                            newRawPartBuilder.setSubType(workoutType.getCode());
                            break;
                        }
                    }
                } catch (final NumberFormatException ex) {
                    LOG.error("Failed to convert workout type {} to a number, defaulting to 1", newPart.getSubtype());
                    newRawPartBuilder.setSubType(1);
                }
            }

            rawScreen.addWidgetPart(newRawPartBuilder);
        }

        final XiaomiProto.WidgetScreens.Builder builder = XiaomiProto.WidgetScreens.newBuilder(rawWidgetScreens);
        if (rawScreen.getId() == rawWidgetScreens.getWidgetScreenCount() + 1) {
            // Append at the end
            builder.addWidgetScreen(rawScreen);
        } else {
            // Replace existing
            builder.clearWidgetScreen();

            for (final XiaomiProto.WidgetScreen screen : rawWidgetScreens.getWidgetScreenList()) {
                if (screen.getId() == rawScreen.getId()) {
                    builder.addWidgetScreen(rawScreen);
                } else {
                    builder.addWidgetScreen(screen);
                }
            }
        }

        builder.setIsFullList(1);

        getPrefs().getPreferences().edit()
                .putString(XiaomiPreferences.PREF_WIDGET_SCREENS, GB.hexdump(builder.build().toByteArray()))
                .apply();
    }

    @Override
    public void deleteScreen(final WidgetScreen widgetScreen) {
        if (widgetScreen.getId() == null) {
            LOG.warn("Can't delete screen without id");
            return;
        }

        final XiaomiProto.WidgetScreens rawWidgetScreens = getRawWidgetScreens();

        final XiaomiProto.WidgetScreens.Builder builder = XiaomiProto.WidgetScreens.newBuilder(rawWidgetScreens)
                .clearWidgetScreen();

        int i = 1;
        for (final XiaomiProto.WidgetScreen screen : rawWidgetScreens.getWidgetScreenList()) {
            if (String.valueOf(screen.getId()).equals(widgetScreen.getId())) {
                continue;
            }

            // Ensure the IDs stay sequential and start at 1
            builder.addWidgetScreen(
                    XiaomiProto.WidgetScreen.newBuilder()
                            .mergeFrom(screen)
                            .setId(i++)
                            .build()
            );
        }

        getPrefs().getPreferences().edit()
                .putString(XiaomiPreferences.PREF_WIDGET_SCREENS, GB.hexdump(builder.build().toByteArray()))
                .apply();
    }

    @Override
    public void sendToDevice() {
        WearableApplication.deviceService(getDevice()).onSendConfiguration(DeviceSettingsPreferenceConst.PREF_WIDGETS);
    }

    private Prefs getPrefs() {
        return new Prefs(WearableApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
    }

    @Nullable
    private WidgetType fromRawWidgetType(final int rawType) {
        switch (rawType) {
            case 1:
                return WidgetType.SMALL;
            case 2:
                return WidgetType.WIDE;
            case 3:
                return WidgetType.TALL;
            case 4:
                return WidgetType.LARGE;
            case 5:
                return WidgetType.PORTRAIT_LARGE;
            default:
                LOG.warn("Unknown widget type {}", rawType);
                return null;
        }
    }

    private int toRawWidgetType(final WidgetType widgetType) {
        switch (widgetType) {
            case SMALL:
                return 1;
            case WIDE:
                return 2;
            case TALL:
                return 3;
            case LARGE:
                return 4;
            case PORTRAIT_LARGE:
                return 5;
            default:
                throw new IllegalArgumentException("Unknown widget type " + widgetType);
        }
    }

    @Nullable
    private WidgetLayout fromRawLayout(final int rawLayout) {
        switch (rawLayout) {
            case 1: // 2x2, top 2x small, bottom 2x small
                return WidgetLayout.TOP_2_BOT_2;
            case 2: // 2x2, top wide, bottom 2x small
                return WidgetLayout.TOP_1_BOT_2;
            case 4: // 2x2, top 2x small, bottom wide
                return WidgetLayout.TOP_2_BOT_1;
            case 128: // 2x2, full screen
                return WidgetLayout.TWO_BY_TWO_SINGLE;
            case 256: // 1x2, top small, bottom small
                return WidgetLayout.TWO;
            case 512: // 1x2, full screen
                return WidgetLayout.ONE_BY_TWO_SINGLE;
            case 1024: // 2x3, top 2x small, bottom 2x2 square
                return WidgetLayout.TOP_2_BOT_2X2;
            case 2048: // 2x3, top 2x2 square, bottom 2x small
                return WidgetLayout.TOP_2X2_BOT_2;
            case 4096: // 2x3, top wide, bottom 2x2 small
                return WidgetLayout.TOP_1_BOT_2X2;
            case 8192: // 2x3, top 2x2 small, bottom wide
                return WidgetLayout.TOP_2X2_BOT_1;
            case 16384: // 2x3, full screen
                return WidgetLayout.TWO_BY_THREE_SINGLE;
            case 8: // 2x2, left tall, right 2x small
            case 16: // 2x2, left 2x small, right tall
            case 32: // 2x2, top wide, bottom wide
            case 64: // 2x2, left tall, right tall
            default:
                LOG.warn("Unknown widget screens layout {}", rawLayout);
                return null;
        }
    }

    private int toRawLayout(final WidgetLayout layout) {
        if (layout == null) {
            return -1;
        }

        switch (layout) {
            case TOP_2_BOT_2:
                return 1;
            case TOP_1_BOT_2:
                return 2;
            case TOP_2_BOT_1:
                return 4;
            case TWO_BY_TWO_SINGLE:
                return 128;
            case TWO:
                return 256;
            case ONE_BY_TWO_SINGLE:
                return 512;
            case TOP_2_BOT_2X2:
                return 1024;
            case TOP_2X2_BOT_2:
                return 2048;
            case TOP_1_BOT_2X2:
                return 4096;
            case TOP_2X2_BOT_1:
                return 8192;
            case TWO_BY_THREE_SINGLE:
                return 16384;
            default:
                LOG.warn("Widget layout {} cannot be converted to raw variant", layout);
                return -1;
        }
    }

    @Nullable
    private XiaomiProto.WidgetPart findRawPart(final int type, final int id) {
        final XiaomiProto.WidgetParts rawWidgetParts = getRawWidgetParts();

        for (final XiaomiProto.WidgetPart rawPart : rawWidgetParts.getWidgetPartList()) {
            if (rawPart.getType() == type && rawPart.getId() == id) {
                return rawPart;
            }
        }

        return null;
    }

    private XiaomiProto.WidgetScreens getRawWidgetScreens() {
        final String hex = getPrefs().getString(XiaomiPreferences.PREF_WIDGET_SCREENS, null);
        if (hex == null) {
            LOG.warn("raw widget screens hex is null");
            return XiaomiProto.WidgetScreens.newBuilder().build();
        }

        try {
            return XiaomiProto.WidgetScreens.parseFrom(GB.hexStringToByteArray(hex));
        } catch (final InvalidProtocolBufferException e) {
            LOG.warn("failed to parse raw widget screns hex");
            return XiaomiProto.WidgetScreens.newBuilder().build();
        }
    }

    private XiaomiProto.WidgetParts getRawWidgetParts() {
        final String hex = getPrefs().getString(XiaomiPreferences.PREF_WIDGET_PARTS, null);
        if (hex == null) {
            LOG.warn("raw widget parts hex is null");
            return XiaomiProto.WidgetParts.newBuilder().build();
        }

        try {
            return XiaomiProto.WidgetParts.parseFrom(GB.hexStringToByteArray(hex));
        } catch (final InvalidProtocolBufferException e) {
            LOG.warn("failed to parse raw widget parts hex");
            return XiaomiProto.WidgetParts.newBuilder().build();
        }
    }
}
