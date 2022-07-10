// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util;

import javax.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
public class JBDateFormat {
  private static final JBDateTimeFormatter DEFAULT_FORMATTER = new DefaultJBDateTimeFormatter();

  @Nonnull
  public static JBDateTimeFormatter getFormatter() {
    DateTimeFormatManager formatManager = DateTimeFormatManager.getInstance();
    if (formatManager.isOverrideSystemDateFormat()) {
      return new CustomJBDateTimeFormatter(formatManager.getDateFormatPattern(), formatManager.isUse24HourTime());
    }

    return DEFAULT_FORMATTER;
  }
}
