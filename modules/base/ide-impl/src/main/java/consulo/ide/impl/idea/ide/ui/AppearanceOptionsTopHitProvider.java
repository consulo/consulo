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
    appearance("UI: " + message(IdeLocalize.checkbooxCyclicScrollingInLists()), "CYCLE_SCROLLING"),
    appearance("UI: " + message(IdeLocalize.checkboxShowIconsInQuickNavigation()), "SHOW_ICONS_IN_QUICK_NAVIGATION"),
    appearance("UI: " + message(IdeLocalize.checkboxPositionCursorOnDefaultButton()), "MOVE_MOUSE_ON_DEFAULT_BUTTON"),
    appearance("UI: Hide navigation popups on focus loss", "HIDE_NAVIGATION_ON_FOCUS_LOSS"),
    appearance("UI: Drag-n-Drop with ALT pressed only", "DND_WITH_PRESSED_ALT_ONLY"),
    notifications("UI: Display balloon notifications", "SHOW_BALLOONS"),
    appearance("Window: " + message(IdeLocalize.checkboxAnimateWindows()), "ANIMATE_WINDOWS"),
    appearance("Window: " + message(IdeLocalize.checkboxShowMemoryIndicator()), "SHOW_MEMORY_INDICATOR"),
    appearance("Window: " + message(KeyMapLocalize.disableMnemonicInMenuCheckBox()), "DISABLE_MNEMONICS"),
    appearance("Window: " + message(KeyMapLocalize.disableMnemonicInControlsCheckBox()), "DISABLE_MNEMONICS_IN_CONTROLS"),
    appearance("Window: " + message(IdeLocalize.checkboxShowIconsInMenuItems()), "SHOW_ICONS_IN_MENUS"),
    appearance("Window: " + message(IdeLocalize.checkboxLeftToolwindowLayout()), "LEFT_HORIZONTAL_SPLIT"),
    appearance("Window: " + message(IdeLocalize.checkboxShowEditorPreviewPopup()), "SHOW_EDITOR_TOOLTIP"),
    appearance("Window: " + message(IdeLocalize.checkboxShowToolWindowNumbers()), "SHOW_TOOL_WINDOW_NUMBERS"),
    appearance("Window: Allow merging buttons on dialogs", "ALLOW_MERGE_BUTTONS"),
    appearance("Window: Small labels in editor tabs", "USE_SMALL_LABELS_ON_TABS"),
    appearance("Window: " + message(IdeLocalize.checkboxWidescreenToolWindowLayout()), "WIDESCREEN_SUPPORT"),
    appearance("Window: " + message(IdeLocalize.checkboxRightToolwindowLayout()), "RIGHT_HORIZONTAL_SPLIT"),
    appearance("Window: " + message(IdeLocalize.checkboxUsePreviewWindow()), "NAVIGATE_TO_PREVIEW")
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
      appearance("Window: " + "Hide Tool Window Bars", "HIDE_TOOL_STRIPES"),
      appearance("View: Show Main Toolbar", "SHOW_MAIN_TOOLBAR"),
      appearance("View: Show Status Bar", "SHOW_STATUS_BAR"),
      appearance("View: Show Navigation Bar", "SHOW_NAVIGATION_BAR")
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
