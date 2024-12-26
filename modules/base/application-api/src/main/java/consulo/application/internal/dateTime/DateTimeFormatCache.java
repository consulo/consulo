/*
 * Copyright 2013-2024 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.application.internal.dateTime;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.platform.Platform;
import consulo.util.lang.SyncDateFormat;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author VISTALL
 * @since 2024-12-26
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class DateTimeFormatCache {
    @Nonnull
    public static DateTimeFormatCache getInstance() {
        return Application.get().getInstance(DateTimeFormatCache.class);
    }

    // do not expose this constants - they are very likely to be changed in future
    public final SyncDateFormat DATE_FORMAT;
    public final SyncDateFormat TIME_FORMAT;
    public final SyncDateFormat TIME_WITH_SECONDS_FORMAT;
    public final SyncDateFormat DATE_TIME_FORMAT;
    public final SyncDateFormat ABOUT_DATE_FORMAT;
    public final SyncDateFormat ISO8601_FORMAT;

    @Inject
    public DateTimeFormatCache(Application application) {
        SyncDateFormat[] formats = getDateTimeFormats(application);
        DATE_FORMAT = formats[0];
        TIME_FORMAT = formats[1];
        TIME_WITH_SECONDS_FORMAT = formats[2];
        DATE_TIME_FORMAT = formats[3];

        ABOUT_DATE_FORMAT = new SyncDateFormat(DateFormat.getDateInstance(DateFormat.LONG, Locale.US));

        DateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        iso8601.setTimeZone(TimeZone.getTimeZone("UTC"));
        ISO8601_FORMAT = new SyncDateFormat(iso8601);
    }

    private SyncDateFormat[] getDateTimeFormats(Application application) {
        Platform platform = Platform.current();

        DateFormat[] res = application.getExtensionPoint(DateTimeFormatProvider.class).computeSafeIfAny(it -> it.getFormats(platform));

        if (res == null) {
            res = new DateFormat[4];

            res[0] = DateFormat.getDateInstance(DateFormat.SHORT);
            res[1] = DateFormat.getTimeInstance(DateFormat.SHORT);
            res[2] = DateFormat.getTimeInstance(DateFormat.MEDIUM);
            res[3] = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        }

        SyncDateFormat[] synced = new SyncDateFormat[4];
        for (int i = 0; i < res.length; i++) {
            synced[i] = new SyncDateFormat(res[i]);
        }
        return synced;
    }
}
