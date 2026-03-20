// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util;

/**
 * @author Konstantin Bulenkov
 */
class DefaultJBDateTimeFormatter extends JBDateTimeFormatter {
    @Override
    protected boolean isPrettyFormattingSupported() {
        return DateTimeFormatManager.getInstance().isPrettyFormattingAllowed();
    }

    @Override
    
    public String formatTime(long time) {
        return DateFormatUtil.formatTime(time);
    }

    @Override
    
    public String formatTimeWithSeconds(long time) {
        return DateFormatUtil.formatTimeWithSeconds(time);
    }

    @Override
    
    public String formatDate(long time) {
        return DateFormatUtil.formatDate(time);
    }

    @Override
    
    public String formatPrettyDateTime(long time) {
        if (isPrettyFormattingSupported()) {
            return DateFormatUtil.formatPrettyDateTime(time);
        }
        return formatDateTime(time);
    }

    @Override
    
    public String formatPrettyDate(long time) {
        if (isPrettyFormattingSupported()) {
            return DateFormatUtil.formatPrettyDate(time);
        }
        return formatDate(time);
    }
}
