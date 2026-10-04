// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid.editor;

import consulo.ui.grid.ObjectFormatterConfig;
import org.jspecify.annotations.Nullable;

import java.sql.Time;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

public class TimeDelegate extends DateAndTimeFormatterDelegate<Time, OffsetTime> {
    private final @Nullable ObjectFormatterConfig myConfig;

    public TimeDelegate(@Nullable ObjectFormatterConfig config) {
        super(OffsetTime::from, a -> LocalTime.from(a).atOffset(DataGridFormattersUtilCore.getDefaultOffset()));
        myConfig = config;
    }

    @Override
    protected Time createFromTemporal(OffsetTime value) {
        return Time.valueOf(
            value.withOffsetSameInstant(DataGridFormattersUtilCore.getLocalTimeOffset(myConfig)).toLocalTime()
        );
    }

    @Override
    @SuppressWarnings("deprecation")
    protected OffsetTime toTemporalAccessor(Object value) {
        OffsetTime offsetTime;
        if (value instanceof Time time) {
            offsetTime = time.toLocalTime().atOffset(ZoneOffset.ofTotalSeconds(-time.getTimezoneOffset() * 60));
        }
        else if (value instanceof LocalTime localTime) {
            offsetTime = localTime.atOffset(ZoneOffset.UTC);
        }
        else {
            throw new IllegalArgumentException("Unsupported value type: " + value.getClass());
        }

        return offsetTime.withOffsetSameInstant(DataGridFormattersUtilCore.getZoneOffsetByEpochOrDefault(myConfig));
    }
}
