package nodomain.freeyourgadget.gadgetbridge.database;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;

public class DatabaseExportWorker extends Worker {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseExportWorker.class);

    public static final String ACTION_DATABASE_EXPORT_SUCCESS = "nodomain.freeyourgadget.gadgetbridge.action.DATABASE_EXPORT_SUCCESS";
    public static final String ACTION_DATABASE_EXPORT_FAIL = "nodomain.freeyourgadget.gadgetbridge.action.DATABASE_EXPORT_FAIL";

    private final Context mContext;

    public DatabaseExportWorker(@NonNull final Context context,
                                @NonNull final WorkerParameters workerParams) {
        super(context, workerParams);
        this.mContext = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            final DBHelper helper = new DBHelper(mContext);
            final String dst = GBApplication.getPrefs().getString(GBPrefs.AUTO_EXPORT_LOCATION, null);
            if (dst == null) {
                LOG.warn("Unable to export DB, export location not set");
                broadcastSuccess(false);
                return Result.failure();
            }

            final Uri dstUri = Uri.parse(dst);
            try (OutputStream out = mContext.getContentResolver().openOutputStream(dstUri)) {
                helper.exportDB(dbHandler, out);
                GBApplication.app().setLastAutoExportTimestamp(System.currentTimeMillis());
            }
        } catch (final Exception e) {
            GB.updateExportFailedNotification(mContext.getString(R.string.notif_export_failed_title), mContext);
            LOG.error("Exception while exporting DB", e);
            broadcastSuccess(false);
            return Result.failure();
        }

        LOG.info("DB export completed");

        broadcastSuccess(true);

        return Result.success();
    }

    @NonNull
    @Override
    public ForegroundInfo getForegroundInfo() {
        return super.getForegroundInfo();
    }

    private void broadcastSuccess(final boolean success) {
        if (!GBApplication.getPrefs().getBoolean("intent_api_broadcast_export", false)) {
            return;
        }

        LOG.info("Broadcasting database export success={}", success);

        final String action = success ? ACTION_DATABASE_EXPORT_SUCCESS : ACTION_DATABASE_EXPORT_FAIL;
        final Intent exportedNotifyIntent = new Intent(action);
        mContext.sendBroadcast(exportedNotifyIntent);
    }
}
