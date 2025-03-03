package nodomain.freeyourgadget.gadgetbridge.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_NOTIFICATION_BODY;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.AppException;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;

public class DeviceCommunicationServiceTestCase extends TestBase {
    private static final java.lang.String TEST_DEVICE_ADDRESS = TestDeviceSupport.class.getName();

    /**
     * Factory that always returns the mockSupport instance
     */
    private class TestDeviceSupportFactory extends DeviceSupportFactory {
        TestDeviceSupportFactory(Context context) {
            super(context);
        }

        @Override
        public synchronized DeviceSupport createDeviceSupport(GBDevice device) throws AppException {
            return mockSupport;
        }
    }

    private TestDeviceService mDeviceService;
    @Mock
    private TestDeviceSupport realSupport;
    private TestDeviceSupport mockSupport;

    public DeviceCommunicationServiceTestCase() {
        super();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockSupport = null;
        realSupport = new TestDeviceSupport();
        realSupport.setContext(new GBDevice(TEST_DEVICE_ADDRESS, "Test Device", "Test Device Alias", "Test Folder", DeviceType.TEST), null, getContext());
        mockSupport = Mockito.spy(realSupport);
        DeviceCommunicationService.setDeviceSupportFactory(new TestDeviceSupportFactory(getContext()));

        mDeviceService = new TestDeviceService(getContext());
    }

    private GBDevice getDevice() {
        return realSupport.getDevice();
    }

    @Override
    public void tearDown() throws Exception {
        mDeviceService.stopService(mDeviceService.createIntent());
        super.tearDown();
    }

    @Test
    public void testNotConnected() {
        GBDevice device = getDevice();
        assertEquals(GBDevice.State.NOT_CONNECTED, device.getState());

        // verify that the events like onFindDevice do not reach the DeviceSupport instance,
        // because not connected
        InOrder inOrder = Mockito.inOrder(mockSupport);
        mDeviceService.onFindDevice(true);
        inOrder.verify(mockSupport, Mockito.times(0)).onFindDevice(true);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void ensureConnected() {
        // connection goes synchronously here
        mDeviceService.forDevice(getDevice()).connect();
        Mockito.verify(mockSupport, Mockito.times(1)).connect();
        assertTrue(getDevice().isInitialized());
    }

    @Ignore //FIXME, probably broken after adding multi-device support
    @Test
    public void testFindDevice() {
        ensureConnected();

        InOrder inOrder = Mockito.inOrder(mockSupport);
        mDeviceService.onFindDevice(true);
        mDeviceService.onFindDevice(false);
        inOrder.verify(mockSupport, Mockito.times(1)).onFindDevice(true);
        inOrder.verify(mockSupport, Mockito.times(1)).onFindDevice(false);
        inOrder.verifyNoMoreInteractions();
    }

    @Ignore //FIXME, probably broken after adding multi-device support
    @Test
    public void testTransliterationSupport() {
        SharedPreferences settings = Application.getPrefs().getPreferences();
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("transliteration", true);
        editor.commit();

        Intent intent = mDeviceService.createIntent().putExtra(EXTRA_NOTIFICATION_BODY, "Прõсто текčт");
        mDeviceService.invokeService(intent);
        String result = intent.getStringExtra(EXTRA_NOTIFICATION_BODY);

        assertEquals("Transliteration support fail!", "Prosto tekct", result);
    }

    @Test
    public void testRtlSupport() {
        SharedPreferences settings = Application.getPrefs().getPreferences();
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("transliteration", false);
        editor.putBoolean(GBPrefs.RTL_SUPPORT, true);
        editor.commit();

        Intent intent = mDeviceService.createIntent().putExtra(EXTRA_NOTIFICATION_BODY, "English and עברית");
        mDeviceService.invokeService(intent);
        String result = intent.getStringExtra(EXTRA_NOTIFICATION_BODY);

        assertEquals("Rtl support fail!", "תירבע English and", result);
    }
}
