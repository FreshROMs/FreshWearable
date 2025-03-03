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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.fetch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.GregorianCalendar;

import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;

/**
 * An operation that fetches statistics from /storage/statistics/ (hm_statis_data* files). We do not
 * know what these are or how to parse them, but syncing them helps free up memory.
 */
public class FetchStatisticsOperation extends AbstractRepeatingFetchOperation {
    private static final Logger LOG = LoggerFactory.getLogger(FetchStatisticsOperation.class);

    public FetchStatisticsOperation(final HuamiSupport support) {
        super(support, HuamiFetchDataType.STATISTICS);
    }

    @Override
    protected String taskDescription() {
        return getContext().getString(R.string.busy_task_fetch_statistics);
    }

    @Override
    protected boolean handleActivityData(final GregorianCalendar timestamp, final byte[] bytes) {
        LOG.debug("Ignoring {} bytes of statistics", bytes.length);

        timestamp.setTime(new Date());

        return true;
    }

    @Override
    protected String getLastSyncTimeKey() {
        return "lastStatisticsTimeMillis";
    }
}
