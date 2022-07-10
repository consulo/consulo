// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util;

import javax.annotation.Nonnull;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Konstantin Bulenkov
 */
class CustomJBDateTimeFormatter extends JBDateTimeFormatter {
  @Nonnull
  private final DateFormat myDateFormat;
  @Nonnull
  private final DateFormat myDateTimeFormat;
  @Nonnull
  private final DateFormat myDateTimeSecondsFormat;

   CustomJBDateTimeFormatter(@Nonnull String pattern, boolean use24hour) {
    myDateFormat = new SimpleDateFormat(pattern);
    myDateTimeFormat = new SimpleDateFormat(pattern + ", " + (use24hour ? "HH:mm" : "h:mm a"));
    myDateTimeSecondsFormat = new SimpleDateFormat(pattern + ", " + (use24hour ? "HH:mm:ss" : "h:mm:ss a"));
  }

  @Nonnull
  protected DateFormat getFormat() {
    return myDateFormat;
  }

  @Nonnull
  protected DateFormat getDateTimeFormat() {
    return myDateTimeFormat;
  }

  @Nonnull
  protected DateFormat getDateTimeSecondsFormat() {
    return myDateTimeSecondsFormat;
  }

  @Override
  protected boolean isPrettyFormattingSupported() {
    return false;
  }

  @Nonnull
  @Override
  public String formatTime(long time) {
    return getDateTimeFormat().format(new Date(time));
  }

  @Nonnull
  @Override
  public String formatTimeWithSeconds(long time) {
    return getDateTimeSecondsFormat().format(time);
  }

  @Nonnull
  @Override
  public String formatDate(long time) {
    return getFormat().format(time);
  }

  @Nonnull
  @Override
  public String formatPrettyDateTime(long time) {
    if (DateTimeFormatManager.getInstance().isPrettyFormattingAllowed() && DateFormatUtil.isPrettyFormattingPossible(time)) {
      return DateFormatUtil.formatPrettyDateTime(time);
    }

    return formatTime(time);
  }

  @Nonnull
  @Override
  public String formatPrettyDate(long time) {
    if (DateTimeFormatManager.getInstance().isPrettyFormattingAllowed() && DateFormatUtil.isPrettyFormattingPossible(time)) {
      return DateFormatUtil.formatPrettyDate(time);
    }
    return formatDate(time);
  }

  @Nonnull
  @Override
  public String formatDateTime(Date date) {
    return formatTime(date);
  }

  @Nonnull
  @Override
  public String formatDateTime(long time) {
    return formatTime(time);
  }

  @Nonnull
  @Override
  public String formatPrettyDateTime(@Nonnull Date date) {
    return formatPrettyDateTime(date.getTime());
  }

  @Nonnull
  @Override
  public String formatPrettyDate(@Nonnull Date date) {
    return formatPrettyDate(date.getTime());
  }
}
