/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer

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
package xyz.tenseventyseven.fresh;

/**
 * Some more or less useful utility methods to aid local (non-device) testing.
 */
public class AppEnvironment {
// DO NOT USE A LOGGER HERE. Will break LoggingTest!
//    private static final Logger LOG = LoggerFactory.getLogger(GBEnvironment.class);

    private static AppEnvironment environment;
    private boolean localTest;
    private boolean deviceTest;

    public static AppEnvironment createLocalTestEnvironment() {
        AppEnvironment env = new AppEnvironment();
        env.localTest = true;
        return env;
    }

    static AppEnvironment createDeviceEnvironment() {
        return new AppEnvironment();
    }

    public final boolean isTest() {
        return localTest || deviceTest;
    }

    public boolean isLocalTest() {
        return localTest;
    }

    public static synchronized AppEnvironment env() {
        return environment;
    }

    static synchronized boolean isEnvironmentSetup() {
        return environment != null;
    }

    public synchronized static void setupEnvironment(AppEnvironment env) {
        environment = env;
    }
}
