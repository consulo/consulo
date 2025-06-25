/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.application.util;

import consulo.application.CommonBundle;
import consulo.application.internal.dateTime.DateTimeFormatCache;
import consulo.util.lang.Clock;
import consulo.util.lang.SyncDateFormat;
import jakarta.annotation.Nonnull;

import java.util.Calendar;
import java.util.Date;

public class DateFormatUtil {
    public static final long SECOND = 1000;
    public static final long MINUTE = SECOND * 60;
    public static final long HOUR = MINUTE * 60;
    public static final long DAY = HOUR * 24;
    public static final long WEEK = DAY * 7;
    public static final long MONTH = DAY * 30;
    public static final long YEAR = DAY * 365;
    public static final long DAY_FACTOR = 24L * 60 * 60 * 1000;

    private static final long[] DENOMINATORS = {YEAR, MONTH, WEEK, DAY, HOUR, MINUTE};

    private enum Period {
        YEAR,
        MONTH,
        WEEK,
        DAY,
        HOUR,
        MINUTE
    }

    private static final Period[] PERIODS = {Period.YEAR, Period.MONTH, Period.WEEK, Period.DAY, Period.HOUR, Period.MINUTE};

    private DateFormatUtil() {
    }

    public static long getDifferenceInDays(@Nonnull Date startDate, @Nonnull Date endDate) {
        return (endDate.getTime() - startDate.getTime() + DAY_FACTOR - 1000) / DAY_FACTOR;
    }

    @Nonnull
    public static SyncDateFormat getDateFormat() {
        return DateTimeFormatCache.getInstance().DATE_FORMAT;
    }

    @Nonnull
    public static SyncDateFormat getTimeFormat() {
        return DateTimeFormatCache.getInstance().TIME_FORMAT;
    }

    @Nonnull
    public static SyncDateFormat getTimeWithSecondsFormat() {
        return DateTimeFormatCache.getInstance().TIME_WITH_SECONDS_FORMAT;
    }

    @Nonnull
    public static SyncDateFormat getDateTimeFormat() {
        return DateTimeFormatCache.getInstance().DATE_TIME_FORMAT;
    }

    @Nonnull
    public static SyncDateFormat getIso8601Format() {
        return DateTimeFormatCache.getInstance().ISO8601_FORMAT;
    }

    @Nonnull
    public static String formatTime(@Nonnull Date time) {
        return formatTime(time.getTime());
    }

    @Nonnull
    public static String formatTime(long time) {
        return getTimeFormat().format(time);
    }

    @Nonnull
    public static String formatTimeWithSeconds(@Nonnull Date time) {
        return formatTimeWithSeconds(time.getTime());
    }

    @Nonnull
    public static String formatTimeWithSeconds(long time) {
        return getTimeWithSecondsFormat().format(time);
    }

    @Nonnull
    public static String formatDate(@Nonnull Date time) {
        return formatDate(time.getTime());
    }

    @Nonnull
    public static String formatDate(long time) {
        return getDateFormat().format(time);
    }

    @Nonnull
    public static String formatPrettyDate(@Nonnull Date date) {
        return formatPrettyDate(date.getTime());
    }

    @Nonnull
    public static String formatPrettyDate(long time) {
        return doFormatPretty(time, false);
    }

    @Nonnull
    public static String formatDateTime(Date date) {
        return formatDateTime(date.getTime());
    }

    @Nonnull
    public static String formatDateTime(long time) {
        return getDateTimeFormat().format(time);
    }

    @Nonnull
    public static String formatPrettyDateTime(@Nonnull Date date) {
        return formatPrettyDateTime(date.getTime());
    }

    @Nonnull
    public static String formatPrettyDateTime(long time) {
        return doFormatPretty(time, true);
    }

    public static boolean isPrettyFormattingPossible(long time) {
        return doFormatPretty(time, true) != null;
    }

    @Nonnull
    private static String doFormatPretty(long time, boolean formatTime) {
        long currentTime = Clock.getTime();

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(currentTime);

        int currentYear = c.get(Calendar.YEAR);
        int currentDayOfYear = c.get(Calendar.DAY_OF_YEAR);

        c.setTimeInMillis(time);

        int year = c.get(Calendar.YEAR);
        int dayOfYear = c.get(Calendar.DAY_OF_YEAR);

        if (formatTime) {
            long delta = currentTime - time;
            if (delta <= HOUR && delta >= 0) {
                return CommonBundle.message("date.format.minutes.ago", (int) Math.rint(delta / (double) MINUTE));
            }
        }

        DateTimeFormatCache cache = DateTimeFormatCache.getInstance();
        boolean isToday = currentYear == year && currentDayOfYear == dayOfYear;
        if (isToday) {
            String result = CommonBundle.message("date.format.today");
            return formatTime ? result + " " + cache.TIME_FORMAT.format(time) : result;
        }

        boolean isYesterdayOnPreviousYear =
            (currentYear == year + 1) && currentDayOfYear == 1 && dayOfYear == c.getActualMaximum(Calendar.DAY_OF_YEAR);
        boolean isYesterday = isYesterdayOnPreviousYear || (currentYear == year && currentDayOfYear == dayOfYear + 1);

        if (isYesterday) {
            String result = CommonBundle.message("date.format.yesterday");
            return formatTime ? result + " " + cache.TIME_FORMAT.format(time) : result;
        }

        return formatTime ? cache.DATE_TIME_FORMAT.format(time) : cache.DATE_FORMAT.format(time);
    }

    @Nonnull
    public static String formatDuration(long delta) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < DENOMINATORS.length; i++) {
            long denominator = DENOMINATORS[i];
            int n = (int) (delta / denominator);
            if (n != 0) {
                buf.append(composeDurationMessage(PERIODS[i], n));
                buf.append(' ');
                delta = delta % denominator;
            }
        }

        if (buf.length() == 0) {
            return CommonBundle.message("date.format.less.than.a.minute");
        }
        return buf.toString().trim();
    }

    @SuppressWarnings("Duplicates")
    private static String composeDurationMessage(final Period period, final int n) {
        switch (period) {
            case DAY:
                return CommonBundle.message("date.format.n.days", n);
            case MINUTE:
                return CommonBundle.message("date.format.n.minutes", n);
            case HOUR:
                return CommonBundle.message("date.format.n.hours", n);
            case MONTH:
                return CommonBundle.message("date.format.n.months", n);
            case WEEK:
                return CommonBundle.message("date.format.n.weeks", n);
            default:
                return CommonBundle.message("date.format.n.years", n);
        }
    }

    @Nonnull
    public static String formatFrequency(long time) {
        return CommonBundle.message("date.frequency", formatBetweenDates(time, 0));
    }

    @Nonnull
    public static String formatBetweenDates(long d1, long d2) {
        long delta = Math.abs(d1 - d2);
        if (delta == 0) {
            return CommonBundle.message("date.format.right.now");
        }

        int n = -1;
        int i;
        for (i = 0; i < DENOMINATORS.length; i++) {
            long denominator = DENOMINATORS[i];
            if (delta >= denominator) {
                n = (int) (delta / denominator);
                break;
            }
        }

        if (d2 > d1) {
            if (n <= 0) {
                return CommonBundle.message("date.format.a.few.moments.ago");
            }
            else {
                return someTimeAgoMessage(PERIODS[i], n);
            }
        }
        else if (d2 < d1) {
            if (n <= 0) {
                return CommonBundle.message("date.format.in.a.few.moments");
            }
            else {
                return composeInSomeTimeMessage(PERIODS[i], n);
            }
        }

        return "";
    }

    @Nonnull
    public static String formatAboutDialogDate(@Nonnull Date date) {
        DateTimeFormatCache cache = DateTimeFormatCache.getInstance();
        return cache.ABOUT_DATE_FORMAT.format(date);
    }

    // helpers

    @SuppressWarnings("Duplicates")
    private static String someTimeAgoMessage(final Period period, final int n) {
        switch (period) {
            case DAY:
                return CommonBundle.message("date.format.n.days.ago", n);
            case MINUTE:
                return CommonBundle.message("date.format.n.minutes.ago", n);
            case HOUR:
                return CommonBundle.message("date.format.n.hours.ago", n);
            case MONTH:
                return CommonBundle.message("date.format.n.months.ago", n);
            case WEEK:
                return CommonBundle.message("date.format.n.weeks.ago", n);
            default:
                return CommonBundle.message("date.format.n.years.ago", n);
        }
    }

    @SuppressWarnings("Duplicates")
    private static String composeInSomeTimeMessage(final Period period, final int n) {
        switch (period) {
            case DAY:
                return CommonBundle.message("date.format.in.n.days", n);
            case MINUTE:
                return CommonBundle.message("date.format.in.n.minutes", n);
            case HOUR:
                return CommonBundle.message("date.format.in.n.hours", n);
            case MONTH:
                return CommonBundle.message("date.format.in.n.months", n);
            case WEEK:
                return CommonBundle.message("date.format.in.n.weeks", n);
            default:
                return CommonBundle.message("date.format.in.n.years", n);
        }
    }
}