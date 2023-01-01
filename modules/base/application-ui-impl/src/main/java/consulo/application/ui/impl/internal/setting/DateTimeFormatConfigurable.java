/*
 * Copyright 2013-2022 consulo.io
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
package consulo.application.ui.impl.internal.setting;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.DateTimeFormatManager;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.StandardColors;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 10-Jul-22
 */
@ExtensionImpl
public class DateTimeFormatConfigurable extends SimpleConfigurableByProperties implements ApplicationConfigurable {
  private final Provider<DateTimeFormatManager> myDateTimeFormatManager;

  @Inject
  public DateTimeFormatConfigurable(Provider<DateTimeFormatManager> dateTimeFormatManager) {
    myDateTimeFormatManager = dateTimeFormatManager;
  }

  @Nonnull
  @Override
  public String getId() {
    return "ide.date.format";
  }

  /**
   * @see consulo.ide.impl.idea.ide.GeneralSettingsConfigurable
   */
  @Nullable
  @Override
  public String getParentId() {
    return "preferences.general";
  }

  @Nonnull
  @Override
  public String getDisplayName() {
    return "Date Formats";
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  protected Component createLayout(@Nonnull PropertyBuilder propertyBuilder, @Nonnull Disposable uiDisposable) {
    DateTimeFormatManager dateTimeFormatManager = myDateTimeFormatManager.get();

    VerticalLayout layout = VerticalLayout.create();
    CheckBox overrideFormatBox = CheckBox.create(LocalizeValue.localizeTODO("Override system date and time format"));
    layout.add(overrideFormatBox);
    propertyBuilder.add(overrideFormatBox, dateTimeFormatManager::isOverrideSystemDateFormat, dateTimeFormatManager::setOverrideSystemDateFormat);

    Label dateFormatLabel = Label.create(LocalizeValue.localizeTODO("Date format"));
    TextBox dateFormatBox = TextBox.create();
    dateFormatBox.setVisibleLength(16);
    propertyBuilder.add(dateFormatBox, dateTimeFormatManager::getDateFormatPattern, dateTimeFormatManager::setDateFormatPattern);
    CheckBox use24HoursBox = CheckBox.create(LocalizeValue.localizeTODO("Use 24-hour time"));
    propertyBuilder.add(use24HoursBox, dateTimeFormatManager::isUse24HourTime, dateTimeFormatManager::setUse24HourTime);
    Hyperlink formatLink = Hyperlink.create("(info)");
    formatLink.addHyperlinkListener(event -> Platform.current().openInBrowser("https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/text/SimpleDateFormat.html"));

    overrideFormatBox.addValueListener(event -> {
      dateFormatBox.setEnabled(event.getValue());
      dateFormatLabel.setEnabled(event.getValue());
      use24HoursBox.setEnabled(event.getValue());
      formatLink.setVisible(event.getValue());
    });

    VerticalLayout overrideLayout = VerticalLayout.create();
    overrideLayout.addBorder(BorderPosition.LEFT, BorderStyle.EMPTY, Image.DEFAULT_ICON_SIZE);

    overrideLayout.add(DockLayout.create().left(HorizontalLayout.create().add(dateFormatLabel).add(formatLink)).right(dateFormatBox));
    overrideLayout.add(use24HoursBox);

    layout.add(overrideLayout);
    
    CheckBox prettyPrintBox = CheckBox.create(LocalizeValue.localizeTODO("Use pretty formatting"));
    layout.add(prettyPrintBox);
    propertyBuilder.add(prettyPrintBox, dateTimeFormatManager::isPrettyFormattingAllowed, dateTimeFormatManager::setPrettyFormattingAllowed);

    HtmlLabel prettyPrintBoxComment = HtmlLabel.create(LocalizeValue.localizeTODO("Replace numeric date with <i>Today</i>, <i>Yesterday</i>, and <i>10 minutes ago</i>"));
    prettyPrintBoxComment.setForegroundColor(StandardColors.GRAY);
    HorizontalLayout prettyCommentLayout = HorizontalLayout.create();
    prettyCommentLayout.addBorder(BorderPosition.LEFT, BorderStyle.EMPTY, Image.DEFAULT_ICON_SIZE);
    prettyCommentLayout.add(prettyPrintBoxComment);
    layout.add(prettyCommentLayout);
    return layout;
  }
}
