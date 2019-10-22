// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.text;

import javax.annotation.Nonnull;

import java.util.Date;

/**
 * @author Konstantin Bulenkov
 */
public abstract class JBDateTimeFormatter {
  protected abstract boolean isPrettyFormattingSupported();

  @Nonnull
  public String formatTime(@Nonnull Date time) {
    return formatTime(time.getTime());
  }

  @Nonnull
  public abstract String formatTime(long time);

  @Nonnull
  public String formatTimeWithSeconds(@Nonnull Date time) {
    return formatTimeWithSeconds(time.getTime());
  }

  @Nonnull
  public abstract String formatTimeWithSeconds(long time);

  @Nonnull
  public String formatDate(@Nonnull Date time) {
    return formatDate(time.getTime());
  }

  @Nonnull
  public abstract String formatDate(long time);

  @Nonnull
  public String formatDateTime(Date date) {
    return formatDateTime(date.getTime());
  }

  @Nonnull
  public String formatDateTime(long time) {
    return DateFormatUtil.formatDateTime(time);
  }

  @Nonnull
  public String formatPrettyDateTime(@Nonnull Date date) {
    return formatPrettyDateTime(date.getTime());
  }

  @Nonnull
  public abstract String formatPrettyDateTime(long time);

  @Nonnull
  public String formatPrettyDate(@Nonnull Date date) {
    return formatPrettyDate(date.getTime());
  }

  @Nonnull
  public abstract String formatPrettyDate(long time);
}
