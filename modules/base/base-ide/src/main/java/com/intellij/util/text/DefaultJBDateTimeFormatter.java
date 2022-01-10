// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.text;

import javax.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
public class DefaultJBDateTimeFormatter extends JBDateTimeFormatter {

  @Override
  protected boolean isPrettyFormattingSupported() {
    return DateTimeFormatManager.getInstance().isPrettyFormattingAllowed();
  }

  @Override
  @Nonnull
  public String formatTime(long time) {
    return DateFormatUtil.formatTime(time);
  }

  @Override
  @Nonnull
  public String formatTimeWithSeconds(long time) {
    return DateFormatUtil.formatTimeWithSeconds(time);
  }

  @Override
  @Nonnull
  public String formatDate(long time) {
    return DateFormatUtil.formatDate(time);
  }

  @Override
  @Nonnull
  public String formatPrettyDateTime(long time) {
    if (isPrettyFormattingSupported()) {
      return DateFormatUtil.formatPrettyDateTime(time);
    }
    return formatDateTime(time);
  }

  @Override
  @Nonnull
  public String formatPrettyDate(long time) {
    if (isPrettyFormattingSupported()) {
      return DateFormatUtil.formatPrettyDate(time);
    }
    return formatDate(time);
  }
}
