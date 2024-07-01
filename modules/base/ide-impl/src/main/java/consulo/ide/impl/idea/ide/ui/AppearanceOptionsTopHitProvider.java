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
import consulo.localize.LocalizeValue;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.ui.ex.keymap.localize.KeyMapLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @author Sergey.Malenkov
 */
@ExtensionImpl
public class AppearanceOptionsTopHitProvider extends OptionsTopHitProvider {
  public static final String ID = "appearance";

  private static final Collection<BooleanOptionDescription> ourOptions = List.of(
    appearance(IdeLocalize.labelOptionUi(IdeLocalize.checkbooxCyclicScrollingInLists()), "CYCLE_SCROLLING"),
    appearance(IdeLocalize.labelOptionUi(IdeLocalize.checkboxShowIconsInQuickNavigation()), "SHOW_ICONS_IN_QUICK_NAVIGATION"),
    appearance(IdeLocalize.labelOptionUi(IdeLocalize.checkboxPositionCursorOnDefaultButton()), "MOVE_MOUSE_ON_DEFAULT_BUTTON"),
    appearance(IdeLocalize.labelOptionUi(IdeLocalize.ideHideNavigationOnFocusLossDescription()), "HIDE_NAVIGATION_ON_FOCUS_LOSS"),
    appearance(IdeLocalize.labelOptionUi(IdeLocalize.dndWithAltPressedOnly()), "DND_WITH_PRESSED_ALT_ONLY"),
    notifications(IdeLocalize.labelOptionUi(IdeLocalize.displayBalloonNotifications()), "SHOW_BALLOONS"),
    appearance(IdeLocalize.labelOptionWindow(IdeLocalize.checkboxAnimateWindows()), "ANIMATE_WINDOWS"),
    appearance(IdeLocalize.labelOptionWindow(IdeLocalize.checkboxShowMemoryIndicator()), "SHOW_MEMORY_INDICATOR"),
    appearance(IdeLocalize.labelOptionWindow(KeyMapLocalize.disableMnemonicInMenuCheckBox()), "DISABLE_MNEMONICS"),
    appearance(IdeLocalize.labelOptionWindow(KeyMapLocalize.disableMnemonicInControlsCheckBox()), "DISABLE_MNEMONICS_IN_CONTROLS"),
    appearance(IdeLocalize.labelOptionWindow(IdeLocalize.checkboxShowIconsInMenuItems()), "SHOW_ICONS_IN_MENUS"),
    appearance(IdeLocalize.labelOptionWindow(IdeLocalize.checkboxLeftToolwindowLayout()), "LEFT_HORIZONTAL_SPLIT"),
    appearance(IdeLocalize.labelOptionWindow(IdeLocalize.checkboxShowEditorPreviewPopup()), "SHOW_EDITOR_TOOLTIP"),
    appearance(IdeLocalize.labelOptionWindow(IdeLocalize.checkboxShowToolWindowNumbers()), "SHOW_TOOL_WINDOW_NUMBERS"),
    appearance(IdeLocalize.labelOptionWindow(IdeLocalize.allowMergingDialogButtons()), "ALLOW_MERGE_BUTTONS"),
    appearance(IdeLocalize.labelOptionWindow(IdeLocalize.smallLabelsInEditorTabs()), "USE_SMALL_LABELS_ON_TABS"),
    appearance(IdeLocalize.labelOptionWindow(IdeLocalize.checkboxWidescreenToolWindowLayout()), "WIDESCREEN_SUPPORT"),
    appearance(IdeLocalize.labelOptionWindow(IdeLocalize.checkboxRightToolwindowLayout()), "RIGHT_HORIZONTAL_SPLIT"),
    appearance(IdeLocalize.labelOptionWindow(IdeLocalize.checkboxUsePreviewWindow()), "NAVIGATE_TO_PREVIEW"),
    appearance(IdeLocalize.labelOptionWindow(IdeLocalize.optionHideToolWindowBars()), "HIDE_TOOL_STRIPES"),
    appearance(IdeLocalize.labelOptionView(IdeLocalize.showMainToolbar()), "SHOW_MAIN_TOOLBAR"),
    appearance(IdeLocalize.labelOptionView(IdeLocalize.showStatusBar()), "SHOW_STATUS_BAR"),
    appearance(IdeLocalize.labelOptionView(IdeLocalize.showNavigationBar()), "SHOW_NAVIGATION_BAR")
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

  static BooleanOptionDescription appearance(@Nonnull LocalizeValue option, String field) {
    return option(option, field, "preferences.lookFeel");
  }

  static BooleanOptionDescription option(@Nonnull LocalizeValue option, String field, String configurableId) {
    return new PublicFieldBasedOptionDescription(option.map(HTML_STRIP).get(), configurableId, field) {
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

  static BooleanOptionDescription notifications(LocalizeValue option, String field) {
    return new PublicFieldBasedOptionDescription(option.map(HTML_STRIP).get(), "reference.settings.ide.settings.notifications", field) {
      @Override
      public Object getInstance() {
        return NotificationsConfigurationImpl.getInstanceImpl();
      }
    };
  }

  @Deprecated
  static BooleanOptionDescription appearance(String option, String field) {
    return appearance(LocalizeValue.of(option), field);
  }

  @Deprecated
  static BooleanOptionDescription option(String option, String field, String configurableId) {
    return option(LocalizeValue.of(option), field, configurableId);
  }

  @Deprecated
  static BooleanOptionDescription notifications(String option, String field) {
    return notifications(LocalizeValue.of(option), field);
  }
}
