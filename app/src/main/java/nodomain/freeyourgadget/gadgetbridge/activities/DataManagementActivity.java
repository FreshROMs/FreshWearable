/*  Copyright (C) 2021-2024 Arjan Schrijver, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.NavUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.activities.files.FileManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.PeriodicExporter;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.ImportExportSharedPreferences;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import xyz.tenseventyseven.fresh.common.AbstractActionBarActivity;


public class DataManagementActivity extends AbstractActionBarActivity {
    private static final Logger LOG = LoggerFactory.getLogger(DataManagementActivity.class);
    private static SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_management);

        final ActivityResultLauncher<String> backupZipFileChooser = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/zip"),
                uri -> {
                    LOG.info("Got target backup file: {}", uri);
                    if (uri != null) {
                        final Intent startBackupIntent = new Intent(DataManagementActivity.this, BackupRestoreProgressActivity.class);
                        startBackupIntent.putExtra(BackupRestoreProgressActivity.EXTRA_URI, uri);
                        startBackupIntent.putExtra(BackupRestoreProgressActivity.EXTRA_ACTION, "export");
                        startActivity(startBackupIntent);
                    }
                }
        );

        final Button backupToZipButton = findViewById(R.id.backupToZipButton);
        backupToZipButton.setOnClickListener(v -> {
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
            final String defaultFilename = String.format(Locale.ROOT, "gadgetbridge_%s.zip", sdf.format(new Date()));
            backupZipFileChooser.launch(defaultFilename);
        });

        final ActivityResultLauncher<String[]> restoreFileChooser = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    LOG.info("Got restore file: {}", uri);

                    if (uri == null) {
                        return;
                    }

                    new AlertDialog.Builder(this)
                            .setCancelable(true)
                            .setIcon(R.drawable.ic_warning)
                            .setTitle(R.string.dbmanagementactivity_import_data_title)
                            .setMessage(R.string.dbmanagementactivity_overwrite_database_confirmation)
                            .setPositiveButton(R.string.dbmanagementactivity_overwrite, (dialog, which) -> {
                                // Disconnect from all devices right away
                                Application.deviceService().disconnect();

                                final Intent startBackupIntent = new Intent(DataManagementActivity.this, BackupRestoreProgressActivity.class);
                                startBackupIntent.putExtra(BackupRestoreProgressActivity.EXTRA_URI, uri);
                                startBackupIntent.putExtra(BackupRestoreProgressActivity.EXTRA_ACTION, "import");
                                startActivity(startBackupIntent);
                            })
                            .setNegativeButton(R.string.Cancel, (dialog, which) -> {
                            })
                            .show();
                }
        );

        final Button restoreFromZipButton = findViewById(R.id.restoreFromZipButton);
        restoreFromZipButton.setOnClickListener(v -> restoreFileChooser.launch(new String[]{"application/zip"}));

        TextView dbPath = findViewById(R.id.activity_data_management_path);
        dbPath.setText(getExternalPath());

        Button exportDBButton = findViewById(R.id.exportDataButton);
        exportDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportDB();
            }
        });
        Button importDBButton = findViewById(R.id.importDataButton);
        importDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importDB();
            }
        });

        Button showContentDataButton = findViewById(R.id.showContentDataButton);
        showContentDataButton.setOnClickListener(v -> {
            final Intent fileManagerIntent = new Intent(DataManagementActivity.this, FileManagerActivity.class);
            startActivity(fileManagerIntent);
        });

        int oldDBVisibility = hasOldActivityDatabase() ? View.VISIBLE : View.GONE;

        TextView deleteOldActivityTitle = findViewById(R.id.mergeOldActivityDataTitle);
        deleteOldActivityTitle.setVisibility(oldDBVisibility);

        Button deleteOldActivityDBButton = findViewById(R.id.deleteOldActivityDB);
        deleteOldActivityDBButton.setVisibility(oldDBVisibility);
        deleteOldActivityDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteOldActivityDbFile();
            }
        });

        Button deleteDBButton = findViewById(R.id.emptyDBButton);
        deleteDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteActivityDatabase();
            }
        });

        TextView dbPath2 = findViewById(R.id.activity_data_management_path2);
        dbPath2.setText(getExternalPath());

        Button cleanExportDirectoryButton = findViewById(R.id.cleanExportDirectoryButton);
        cleanExportDirectoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cleanExportDirectory();
            }
        });
        Application gbApp = Application.app();
        Prefs prefs = Application.getPrefs();
        boolean autoExportEnabled = prefs.getBoolean(GBPrefs.AUTO_EXPORT_ENABLED, false);
        int autoExportInterval = prefs.getInt(GBPrefs.AUTO_EXPORT_INTERVAL, 0);
        //returns an ugly content://...
        //String autoExportLocation = prefs.getString(GBPrefs.AUTO_EXPORT_LOCATION, "");

        int testExportVisibility = (autoExportInterval > 0 && autoExportEnabled) ? View.VISIBLE : View.GONE;
        boolean isExportEnabled = autoExportInterval > 0 && autoExportEnabled;
        TextView autoExportLocation_label = findViewById(R.id.autoExportLocation_label);
        autoExportLocation_label.setVisibility(testExportVisibility);

        TextView autoExportLocation_path = findViewById(R.id.autoExportLocation_path);
        autoExportLocation_path.setVisibility(testExportVisibility);
        autoExportLocation_path.setText(getAutoExportLocationUserString() + " (" + getAutoExportLocationPreferenceString() + ")" );

        TextView autoExportEnabled_label = findViewById(R.id.autoExportEnabled);
        if (isExportEnabled) {
            autoExportEnabled_label.setText(getString(R.string.activity_db_management_autoexport_enabled_yes));
        } else {
            autoExportEnabled_label.setText(getString(R.string.activity_db_management_autoexport_enabled_no));
        }

        TextView autoExportScheduled = findViewById(R.id.autoExportScheduled);
        autoExportScheduled.setVisibility(testExportVisibility);
        long setAutoExportScheduledTimestamp = gbApp.getAutoExportScheduledTimestamp();
        if (setAutoExportScheduledTimestamp > 0) {
            autoExportScheduled.setText(getString(R.string.activity_db_management_autoexport_scheduled_yes,
                    DateTimeUtils.formatDateTime(new Date(setAutoExportScheduledTimestamp))));
        } else {
            autoExportScheduled.setText(getResources().getString(R.string.activity_db_management_autoexport_scheduled_no));
        }

        TextView autoExport_lastTime_label = findViewById(R.id.autoExport_lastTime_label);
        long lastAutoExportTimestamp = gbApp.getLastAutoExportTimestamp();

        autoExport_lastTime_label.setVisibility(View.GONE);
        autoExport_lastTime_label.setText(getString(R.string.autoExport_lastTime_label,
                DateTimeUtils.formatDateTime(new Date(lastAutoExportTimestamp))));

        if (lastAutoExportTimestamp > 0) {
            autoExport_lastTime_label.setVisibility(testExportVisibility);
            autoExport_lastTime_label.setVisibility(testExportVisibility);
        }

        final Context context = getApplicationContext();
        Button testExportDBButton = findViewById(R.id.testExportDBButton);
        testExportDBButton.setVisibility(testExportVisibility);
        testExportDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PeriodicExporter.trigger();
                GB.toast(context,
                        context.getString(R.string.activity_DB_test_export_message),
                        Toast.LENGTH_SHORT, GB.INFO);
            }
        });

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    private String getAutoExportLocationPreferenceString() {
        String autoExportLocation = Application.getPrefs().getString(GBPrefs.AUTO_EXPORT_LOCATION, null);
        if (autoExportLocation == null) {
            return "";
        }
        return autoExportLocation;
    }

    private String getAutoExportLocationUri() {
        String autoExportLocation = getAutoExportLocationPreferenceString();
        if (autoExportLocation == null) {
            return "";
        }
        Uri uri = Uri.parse(autoExportLocation);
        try {

            return AndroidUtils.getFilePath(getApplicationContext(), uri);
        } catch (IllegalArgumentException e) {
            LOG.error("getFilePath did not work, trying to resolve content provider path");
            try {
                Cursor cursor = getContentResolver().query(
                        uri,
                        new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},
                        null, null, null, null
                );
                if (cursor != null && cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME));
                }
            } catch (Exception exception) {
                LOG.error("Error getting export path", exception);
            }
        }
        return "";
    }

    private String getAutoExportLocationUserString() {
        String location = getAutoExportLocationUri();
        if (location == "") {
            return getString(R.string.activity_db_management_autoexport_location);
        }
        return location;
    }

    private boolean hasOldActivityDatabase() {
        return new DBHelper(this).existsDB("ActivityDatabase");
    }

    private String getExternalPath() {
        try {
            return FileUtils.getExternalFilesDir().getAbsolutePath();
        } catch (Exception ex) {
            LOG.warn("Unable to get external files dir", ex);
        }
        return getString(R.string.dbmanagementactivvity_cannot_access_export_path);
    }

    private void exportShared() {
        try {
            File myPath = FileUtils.getExternalFilesDir();
            File myFile = new File(myPath, "Export_preference");
            ImportExportSharedPreferences.exportToFile(sharedPrefs, myFile);
        } catch (IOException ex) {
            GB.toast(this, getString(R.string.dbmanagementactivity_error_exporting_shared, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
        try (DBHandler lockHandler = Application.acquireDB()) {
            List<Device> activeDevices = DBHelper.getActiveDevices(lockHandler.getDaoSession());
            for (Device dbDevice : activeDevices) {
                SharedPreferences deviceSharedPrefs = Application.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                if (sharedPrefs != null) {
                    File myPath = FileUtils.getExternalFilesDir();
                    File myFile = new File(myPath, "Export_preference_" + FileUtils.makeValidFileName(dbDevice.getIdentifier()));
                    try {
                        ImportExportSharedPreferences.exportToFile(deviceSharedPrefs, myFile);
                    } catch (Exception ignore) {
                        // some devices no not have device specific preferences
                    }
                }
            }
        } catch (Exception e) {
            GB.toast("Error exporting device specific preferences", Toast.LENGTH_SHORT, GB.ERROR, e);
        }
    }

    private void importShared() {
        try {
            File myPath = FileUtils.getExternalFilesDir();
            File myFile = new File(myPath, "Export_preference");
            ImportExportSharedPreferences.importFromFile(sharedPrefs, myFile);
        } catch (Exception ex) {
            GB.toast(DataManagementActivity.this, getString(R.string.dbmanagementactivity_error_importing_db, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
        }

        try (DBHandler lockHandler = Application.acquireDB()) {
            List<Device> activeDevices = DBHelper.getActiveDevices(lockHandler.getDaoSession());
            for (Device dbDevice : activeDevices) {
                SharedPreferences deviceSharedPrefs = Application.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                if (sharedPrefs != null) {
                    File myPath = FileUtils.getExternalFilesDir();
                    File myFile = new File(myPath, "Export_preference_" + FileUtils.makeValidFileName(dbDevice.getIdentifier()));

                    if (!myFile.exists()) { //first try to use file in new format de_ad_be_af, if doesn't exist use old format de:at:be:af
                        myFile = new File(myPath, "Export_preference_" + dbDevice.getIdentifier());
                        LOG.info("Trying to import with older filename");
                    }else{
                        LOG.info("Trying to import with new filename");
                    }

                    try {
                        ImportExportSharedPreferences.importFromFile(deviceSharedPrefs, myFile);
                    } catch (Exception ignore) {
                        // some devices no not have device specific preferences
                    }
                }
            }
        } catch (Exception e) {
            GB.toast("Error importing device specific preferences", Toast.LENGTH_SHORT, GB.ERROR);
        }
    }

    private void exportDB() {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setIcon(R.drawable.ic_warning)
                .setTitle(R.string.dbmanagementactivity_export_data_title)
                .setMessage(R.string.dbmanagementactivity_export_confirmation)
                .setPositiveButton(R.string.activity_DB_ExportButton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try (DBHandler dbHandler = Application.acquireDB()) {
                            exportShared();
                            DBHelper helper = new DBHelper(DataManagementActivity.this);
                            File dir = FileUtils.getExternalFilesDir();
                            File destFile = helper.exportDB(dbHandler, dir);
                            GB.toast(DataManagementActivity.this, getString(R.string.dbmanagementactivity_exported_to, destFile.getAbsolutePath()), Toast.LENGTH_LONG, GB.INFO);
                        } catch (Exception ex) {
                            GB.toast(DataManagementActivity.this, getString(R.string.dbmanagementactivity_error_exporting_db, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
                        }
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void importDB() {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setIcon(R.drawable.ic_warning)
                .setTitle(R.string.dbmanagementactivity_import_data_title)
                .setMessage(R.string.dbmanagementactivity_overwrite_database_confirmation)
                .setPositiveButton(R.string.dbmanagementactivity_overwrite, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try (DBHandler dbHandler = Application.acquireDB()) {
                            DBHelper helper = new DBHelper(DataManagementActivity.this);
                            File dir = FileUtils.getExternalFilesDir();
                            SQLiteOpenHelper sqLiteOpenHelper = dbHandler.getHelper();
                            File sourceFile = new File(dir, sqLiteOpenHelper.getDatabaseName());
                            helper.importDB(dbHandler, sourceFile);
                            helper.validateDB(sqLiteOpenHelper);
                            GB.toast(DataManagementActivity.this, getString(R.string.dbmanagementactivity_import_successful), Toast.LENGTH_LONG, GB.INFO);
                        } catch (Exception ex) {
                            GB.toast(DataManagementActivity.this, getString(R.string.dbmanagementactivity_error_importing_db, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
                        }
                        importShared();
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void deleteActivityDatabase() {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setIcon(R.drawable.ic_warning)
                .setTitle(R.string.dbmanagementactivity_delete_activity_data_title)
                .setMessage(R.string.dbmanagementactivity_really_delete_entire_db)
                .setPositiveButton(R.string.Delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (Application.deleteActivityDatabase(DataManagementActivity.this)) {
                            GB.toast(DataManagementActivity.this, getString(R.string.dbmanagementactivity_database_successfully_deleted), Toast.LENGTH_SHORT, GB.INFO);
                        } else {
                            GB.toast(DataManagementActivity.this, getString(R.string.dbmanagementactivity_db_deletion_failed), Toast.LENGTH_SHORT, GB.INFO);
                        }
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void deleteOldActivityDbFile() {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.dbmanagementactivity_delete_old_activity_db)
                .setIcon(R.drawable.ic_warning)
                .setMessage(R.string.dbmanagementactivity_delete_old_activitydb_confirmation)
                .setPositiveButton(R.string.Delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (Application.deleteOldActivityDatabase(DataManagementActivity.this)) {
                    GB.toast(DataManagementActivity.this, getString(R.string.dbmanagementactivity_old_activity_db_successfully_deleted), Toast.LENGTH_SHORT, GB.INFO);
                } else {
                    GB.toast(DataManagementActivity.this, getString(R.string.dbmanagementactivity_old_activity_db_deletion_failed), Toast.LENGTH_SHORT, GB.INFO);
                }
            }
        });
        new AlertDialog.Builder(this).setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        new AlertDialog.Builder(this).show();
    }

    private void cleanExportDirectory() {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setIcon(R.drawable.ic_warning)
                .setTitle(R.string.activity_DB_clean_export_directory_warning_title)
                .setMessage(getString(R.string.activity_DB_clean_export_directory_warning_message))
                .setPositiveButton(R.string.Delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            File externalFilesDir = FileUtils.getExternalFilesDir();
                            String autoexportFile = getAutoExportLocationUri();
                            for (File file : externalFilesDir.listFiles()) {
                                if (file.isFile() &&
                                        (!FileUtils.getExtension(file.toString()).toLowerCase().equals("gpx")) && //keep GPX files
                                        (!file.toString().equals(autoexportFile)) // do not remove autoexport
                                ) {
                                    LOG.debug("Deleting file: " + file);
                                    try {
                                        file.delete();
                                    } catch (Exception exception) {
                                        LOG.error("Error erasing file: " + exception);
                                    }
                                }
                            }
                            GB.toast(getString(R.string.dbmanagementactivity_export_finished), Toast.LENGTH_SHORT, GB.INFO);
                        } catch (Exception ex) {
                            GB.toast(DataManagementActivity.this, getString(R.string.dbmanagementactivity_error_cleaning_export_directory, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
                        }
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
