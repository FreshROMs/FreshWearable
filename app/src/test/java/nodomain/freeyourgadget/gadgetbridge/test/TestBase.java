package nodomain.freeyourgadget.gadgetbridge.test;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;

import ch.qos.logback.classic.util.ContextInitializer;
import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.AppEnvironment;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

import static org.junit.Assert.assertNotNull;
import static xyz.tenseventyseven.fresh.Logging.PROP_LOGFILES_DIR;

/**
 * Base class for all testcases in Gadgetbridge that are supposed to run locally
 * with robolectric.
 *
 * Important: To run them, create a run configuration and execute them in the Gadgetbridge/app/
 * directory.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 21)
public abstract class TestBase {
    protected static File logFilesDir;

    protected Application app = (Application) RuntimeEnvironment.application;
    protected DaoSession daoSession;
    protected DBHandler dbHandler;

    // Make sure logging is set up for all testcases, so that we can debug problems
    @BeforeClass
    public static void setupSuite() throws Exception {
        AppEnvironment.setupEnvironment(AppEnvironment.createLocalTestEnvironment());

        // print everything going to android.util.Log to System.out
        System.setProperty("robolectric.logging", "stdout");

        // properties might be preconfigured in build.gradle because of test ordering problems
        String logDir = System.getProperty(PROP_LOGFILES_DIR);
        if (logDir != null) {
            logFilesDir = new File(logDir);
        } else {
            logFilesDir = FileUtils.createTempDir("logfiles");
            System.setProperty(PROP_LOGFILES_DIR, logFilesDir.getAbsolutePath());
        }

        if (System.getProperty(ContextInitializer.CONFIG_FILE_PROPERTY) == null) {
            File workingDir = new File(System.getProperty("user.dir"));
            File configFile = new File(workingDir, "src/main/assets/logback.xml");
            System.out.println(configFile.getAbsolutePath());
            System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, configFile.getAbsolutePath());
        }
    }

    @Before
    public void setUp() throws Exception {
        app = (Application) RuntimeEnvironment.application;
        assertNotNull(app);
        assertNotNull(getContext());
        app.setupDatabase();
        dbHandler = Application.acquireDB();
        daoSession = dbHandler.getDaoSession();
        assertNotNull(daoSession);
    }

    @After
    public void tearDown() throws Exception {
        dbHandler.closeDb();
        Application.releaseDB();
    }

    protected GBDevice createDummyGDevice(String macAddress) {
        GBDevice dummyGBDevice = new GBDevice(macAddress, "Testie", "Tesie Alias", "Test Folder", DeviceType.TEST);
        dummyGBDevice.setFirmwareVersion("1.2.3");
        dummyGBDevice.setModel("4.0");
        return dummyGBDevice;
    }

    protected Context getContext() {
        return app;
    }
}
