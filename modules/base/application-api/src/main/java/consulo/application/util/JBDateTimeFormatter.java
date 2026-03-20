// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util;

import java.util.Date;

/**
 * @author Konstantin Bulenkov
 */
public abstract class JBDateTimeFormatter {
    protected abstract boolean isPrettyFormattingSupported();

    
    public String formatTime(Date time) {
        return formatTime(time.getTime());
    }

    
    public abstract String formatTime(long time);

    
    public String formatTimeWithSeconds(Date time) {
        return formatTimeWithSeconds(time.getTime());
    }

    
    public abstract String formatTimeWithSeconds(long time);

    
    public String formatDate(Date time) {
        return formatDate(time.getTime());
    }

    
    public abstract String formatDate(long time);

    
    public String formatDateTime(Date date) {
        return formatDateTime(date.getTime());
    }

    
    public String formatDateTime(long time) {
        return DateFormatUtil.formatDateTime(time);
    }

    
    public String formatPrettyDateTime(Date date) {
        return formatPrettyDateTime(date.getTime());
    }

    
    public abstract String formatPrettyDateTime(long time);

    
    public String formatPrettyDate(Date date) {
        return formatPrettyDate(date.getTime());
    }

    
    public abstract String formatPrettyDate(long time);
}
