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
import consulo.ide.impl.idea.ide.ui.search.BooleanOptionDescription;
import consulo.ide.impl.idea.notification.impl.NotificationsConfigurationImpl;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.platform.base.localize.IdeLocalize;
import consulo.platform.base.localize.KeyMapLocalize;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

/**
 * @author Sergey.Malenkov
 */
@ExtensionImpl
public class AppearanceOptionsTopHitProvider extends OptionsTopHitProvider {
  public static final String ID = "appearance";

  private static final Collection<BooleanOptionDescription> ourOptions = ContainerUtil.immutableList(
    appearance(message(IdeLocalize.labelOptionUi(IdeLocalize.checkbooxCyclicScrollingInLists())), "CYCLE_SCROLLING"),
    appearance(message(IdeLocalize.labelOptionUi(IdeLocalize.checkboxShowIconsInQuickNavigation())), "SHOW_ICONS_IN_QUICK_NAVIGATION"),
    appearance(message(IdeLocalize.labelOptionUi(IdeLocalize.checkboxPositionCursorOnDefaultButton())), "MOVE_MOUSE_ON_DEFAULT_BUTTON"),
    appearance(message(IdeLocalize.labelOptionUi(IdeLocalize.ideHideNavigationOnFocusLossDescription())), "HIDE_NAVIGATION_ON_FOCUS_LOSS"),
    appearance(message(IdeLocalize.labelOptionUi(IdeLocalize.dndWithAltPressedOnly())), "DND_WITH_PRESSED_ALT_ONLY"),
    notifications(message(IdeLocalize.labelOptionUi(IdeLocalize.displayBalloonNotifications())), "SHOW_BALLOONS"),
    appearance(message(IdeLocalize.labelOptionWindow(IdeLocalize.checkboxAnimateWindows())), "ANIMATE_WINDOWS"),
    appearance(message(IdeLocalize.labelOptionWindow(IdeLocalize.checkboxShowMemoryIndicator())), "SHOW_MEMORY_INDICATOR"),
    appearance(message(IdeLocalize.labelOptionWindow(KeyMapLocalize.disableMnemonicInMenuCheckBox())), "DISABLE_MNEMONICS"),
    appearance(message(IdeLocalize.labelOptionWindow(KeyMapLocalize.disableMnemonicInControlsCheckBox())), "DISABLE_MNEMONICS_IN_CONTROLS"),
    appearance(message(IdeLocalize.labelOptionWindow(IdeLocalize.checkboxShowIconsInMenuItems())), "SHOW_ICONS_IN_MENUS"),
    appearance(message(IdeLocalize.labelOptionWindow(IdeLocalize.checkboxLeftToolwindowLayout())), "LEFT_HORIZONTAL_SPLIT"),
    appearance(message(IdeLocalize.labelOptionWindow(IdeLocalize.checkboxShowEditorPreviewPopup())), "SHOW_EDITOR_TOOLTIP"),
    appearance(message(IdeLocalize.labelOptionWindow(IdeLocalize.checkboxShowToolWindowNumbers())), "SHOW_TOOL_WINDOW_NUMBERS"),
    appearance(message(IdeLocalize.labelOptionWindow(IdeLocalize.allowMergingDialogButtons())), "ALLOW_MERGE_BUTTONS"),
    appearance(message(IdeLocalize.labelOptionWindow(IdeLocalize.smallLabelsInEditorTabs())), "USE_SMALL_LABELS_ON_TABS"),
    appearance(message(IdeLocalize.labelOptionWindow(IdeLocalize.checkboxWidescreenToolWindowLayout())), "WIDESCREEN_SUPPORT"),
    appearance(message(IdeLocalize.labelOptionWindow(IdeLocalize.checkboxRightToolwindowLayout())), "RIGHT_HORIZONTAL_SPLIT"),
    appearance(message(IdeLocalize.labelOptionWindow(IdeLocalize.checkboxUsePreviewWindow())), "NAVIGATE_TO_PREVIEW")
  );

  @Nonnull
  @Override
  public Collection<BooleanOptionDescription> getOptions(@Nullable Project project) {
    return ourOptions;
  }

  @Override
  public String getId() {
    return ID;
  }

  static BooleanOptionDescription appearance(String option, String field) {
    return option(option, field, "preferences.lookFeel");
  }

  static BooleanOptionDescription option(String option, String field, String configurableId) {
    return new PublicFieldBasedOptionDescription(option, configurableId, field) {
      @Override
      public Object getInstance() {
        return UISettings.getInstance();
      }

      @Override
      protected void fireUpdated() {
        UISettings.getInstance().fireUISettingsChanged();
      }
    };
  }

  static BooleanOptionDescription notifications(String option, String field) {
    return new PublicFieldBasedOptionDescription(option, "reference.settings.ide.settings.notifications", field) {
      @Override
      public Object getInstance() {
        return NotificationsConfigurationImpl.getInstanceImpl();
      }
    };
  }

  public static class Ex extends OptionsTopHitProvider implements CoveredByToggleActions {
    private static final Collection<BooleanOptionDescription> ourOptions = ContainerUtil.immutableList(
      appearance(message(IdeLocalize.labelOptionWindow(IdeLocalize.optionHideToolWindowBars())), "HIDE_TOOL_STRIPES"),
      appearance(message(IdeLocalize.labelOptionView(IdeLocalize.showMainToolbar())), "SHOW_MAIN_TOOLBAR"),
      appearance(message(IdeLocalize.labelOptionView(IdeLocalize.showStatusBar())), "SHOW_STATUS_BAR"),
      appearance(message(IdeLocalize.labelOptionView(IdeLocalize.showNavigationBar())), "SHOW_NAVIGATION_BAR")
    );

    @Nonnull
    @Override
    public Collection<BooleanOptionDescription> getOptions(@Nullable Project project) {
      return ourOptions;
    }

    @Override
    public String getId() {
      return ID;
    }
  }
}
