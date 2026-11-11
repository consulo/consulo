/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.ui;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ui.UISettings;
import consulo.component.ComponentManager;
import consulo.configurable.search.BooleanOptionDescription;
import consulo.configurable.search.OptionsTopHitProvider;
import consulo.configurable.search.PublicMethodBasedOptionDescription;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.ui.impl.internal.notification.NotificationsConfigurationImpl;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * The appearance options no page binds - the others come from the generated options of the pages.
 *
 * @author Sergey.Malenkov
 */
@ExtensionImpl
public class AppearanceOptionsTopHitProvider extends OptionsTopHitProvider {
  public static final String ID = "appearance";

  private static final Collection<BooleanOptionDescription> ourOptions = List.of(
    new PublicMethodBasedOptionDescription<>(
      IdeLocalize.labelOptionUi(IdeLocalize.displayBalloonNotifications()).map(HTML_STRIP).get(),
      "reference.settings.ide.settings.notifications",
      NotificationsConfigurationImpl::getInstanceImpl,
      NotificationsConfigurationImpl::isShowBalloons,
      NotificationsConfigurationImpl::setShowBalloons
    ),
    // the main toolbar, the status bar and the navigation bar are switched by the toggle actions of View
    appearance(
      IdeLocalize.labelOptionWindow(IdeLocalize.checkboxUsePreviewWindow()),
      UISettings::getNavigateToPreview,
      UISettings::setNavigateToPreview
    )
  );

  @Override
  public Collection<BooleanOptionDescription> getOptions(@Nullable ComponentManager project) {
    return ourOptions;
  }

  @Override
  public String getId() {
    return ID;
  }

  static BooleanOptionDescription appearance(LocalizeValue option,
                                             Function<UISettings, Boolean> getter,
                                             BiConsumer<UISettings, Boolean> setter) {
    return uiOption(option, ID, getter, setter);
  }

  /**
   * An option of {@link UISettings} - the UI follows it once switched.
   */
  static BooleanOptionDescription uiOption(LocalizeValue option,
                                           String configurableId,
                                           Function<UISettings, Boolean> getter,
                                           BiConsumer<UISettings, Boolean> setter) {
    return new PublicMethodBasedOptionDescription<>(
      option.map(HTML_STRIP).get(),
      configurableId,
      UISettings::getInstance,
      getter,
      setter,
      () -> UISettings.getInstance().fireUISettingsChanged()
    );
  }
}
