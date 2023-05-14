// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.logging.Logger;
import consulo.util.xml.serializer.XmlSerializer;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Singleton;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * @author Konstantin Bulenkov
 */
@Singleton
@State(name = "DateTimeFormatter", storages = @Storage("ui-datetime.xml"))
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class DateTimeFormatManager implements PersistentStateComponent<Element> {
  private static final Logger LOG = Logger.getInstance(DateTimeFormatManager.class);

  @Nonnull
  public static DateTimeFormatManager getInstance() {
    return Application.get().getInstance(DateTimeFormatManager.class);
  }

  public static final String DEFAULT_DATE_FORMAT = "dd MMM yyyy";
  private boolean myPrettyFormattingAllowed = true;
  private String myPattern = DEFAULT_DATE_FORMAT;
  private boolean myOverrideSystemDateFormat = false;
  private boolean myUse24HourTime = true;

  @Nullable
  @Override
  public Element getState() {
    return XmlSerializer.serialize(this);
  }

  @Override
  public void loadState(@Nonnull Element state) {
    DateTimeFormatManager loaded = XmlSerializer.deserialize(state, DateTimeFormatManager.class);
    XmlSerializerUtil.copyBean(loaded, this);
  }

  public boolean isOverrideSystemDateFormat() {
    return myOverrideSystemDateFormat;
  }

  public void setOverrideSystemDateFormat(boolean overrideSystemDateFormat) {
    myOverrideSystemDateFormat = overrideSystemDateFormat;
  }

  public boolean isUse24HourTime() {
    return myUse24HourTime;
  }

  public void setUse24HourTime(boolean use24HourTime) {
    myUse24HourTime = use24HourTime;
  }

  public void setPrettyFormattingAllowed(boolean prettyFormattingAllowed) {
    myPrettyFormattingAllowed = prettyFormattingAllowed;
  }

  public boolean isPrettyFormattingAllowed() {
    return myPrettyFormattingAllowed;
  }

  @Nullable
  public DateFormat getDateFormat() {
    try {
      return new SimpleDateFormat(myPattern);
    }
    catch (IllegalArgumentException e) {
      LOG.warn(e);
    }
    return null;
  }

  @Nonnull
  public String getDateFormatPattern() {
    return myPattern;
  }

  public void setDateFormatPattern(@Nonnull String pattern) {
    try {
      new SimpleDateFormat(pattern);
      myPattern = pattern;
    }
    catch (Exception ignored) {
    }
  }
}
