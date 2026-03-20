// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Konstantin Bulenkov
 */
class CustomJBDateTimeFormatter extends JBDateTimeFormatter {
    
    private final DateFormat myDateFormat;
    
    private final DateFormat myDateTimeFormat;
    
    private final DateFormat myDateTimeSecondsFormat;

    CustomJBDateTimeFormatter(String pattern, boolean use24hour) {
        myDateFormat = new SimpleDateFormat(pattern);
        myDateTimeFormat = new SimpleDateFormat(pattern + ", " + (use24hour ? "HH:mm" : "h:mm a"));
        myDateTimeSecondsFormat = new SimpleDateFormat(pattern + ", " + (use24hour ? "HH:mm:ss" : "h:mm:ss a"));
    }

    
    protected DateFormat getFormat() {
        return myDateFormat;
    }

    
    protected DateFormat getDateTimeFormat() {
        return myDateTimeFormat;
    }

    
    protected DateFormat getDateTimeSecondsFormat() {
        return myDateTimeSecondsFormat;
    }

    @Override
    protected boolean isPrettyFormattingSupported() {
        return false;
    }

    
    @Override
    public String formatTime(long time) {
        return getDateTimeFormat().format(new Date(time));
    }

    
    @Override
    public String formatTimeWithSeconds(long time) {
        return getDateTimeSecondsFormat().format(time);
    }

    
    @Override
    public String formatDate(long time) {
        return getFormat().format(time);
    }

    
    @Override
    public String formatPrettyDateTime(long time) {
        if (DateTimeFormatManager.getInstance().isPrettyFormattingAllowed() && DateFormatUtil.isPrettyFormattingPossible(time)) {
            return DateFormatUtil.formatPrettyDateTime(time);
        }

        return formatTime(time);
    }

    
    @Override
    public String formatPrettyDate(long time) {
        if (DateTimeFormatManager.getInstance().isPrettyFormattingAllowed() && DateFormatUtil.isPrettyFormattingPossible(time)) {
            return DateFormatUtil.formatPrettyDate(time);
        }
        return formatDate(time);
    }

    
    @Override
    public String formatDateTime(Date date) {
        return formatTime(date);
    }

    
    @Override
    public String formatDateTime(long time) {
        return formatTime(time);
    }

    
    @Override
    public String formatPrettyDateTime(Date date) {
        return formatPrettyDateTime(date.getTime());
    }

    
    @Override
    public String formatPrettyDate(Date date) {
        return formatPrettyDate(date.getTime());
    }
}
