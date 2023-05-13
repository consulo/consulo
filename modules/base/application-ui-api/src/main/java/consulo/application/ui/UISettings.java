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
package consulo.application.ui;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.ui.event.UISettingsListener;
import consulo.application.util.SystemInfo;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.util.SimpleModificationTracker;
import consulo.disposer.Disposable;
import consulo.ui.AntialiasingType;
import consulo.ui.ex.ColorBlindness;
import consulo.util.xml.serializer.XmlSerializerUtil;
import consulo.util.xml.serializer.annotation.Transient;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@State(name = "UISettings", storages = @Storage("ui.lnf.xml"))
public class UISettings extends SimpleModificationTracker implements PersistentStateComponent<UISettings> {
  private volatile static UISettings ourInstance;

  @Nonnull
  public static UISettings getInstance() {
    if (ourInstance != null) {
      return ourInstance;
    }
    UISettings settings = getInstanceOrNull();
    if (settings == null) {
      throw new UnsupportedOperationException("Application is not initialized. Please call getInstanceOrNull()");
    }
    return settings;
  }

  /**
   * Use this method if you are not sure whether the application is initialized.
   *
   * @return persisted UISettings instance or default values.
   */
  @Nonnull
  public static UISettings getShadowInstance() {
    UISettings settings = getInstanceOrNull();
    return settings == null ? new UISettings() : settings;
  }

  /**
   * @return null if application is not initialized
   */
  @Nullable
  public static UISettings getInstanceOrNull() {
    if (ourInstance != null) {
      return ourInstance;
    }
    Application application = ApplicationManager.getApplication();
    if (application == null) {
      return null;
    }
    UISettings settings = application.getInstance(UISettings.class);
    ourInstance = settings;
    return settings;
  }

  /**
   * Not tabbed pane.
   */
  public static final int ANIMATION_DURATION = 300; // Milliseconds

  public int RECENT_FILES_LIMIT = 50;
  public int CONSOLE_COMMAND_HISTORY_LIMIT = 300;
  public boolean OVERRIDE_CONSOLE_CYCLE_BUFFER_SIZE = false;
  public int CONSOLE_CYCLE_BUFFER_SIZE_KB = 1024;
  public int EDITOR_TAB_LIMIT = 10;
  public boolean REUSE_NOT_MODIFIED_TABS = false;
  public boolean ANIMATE_WINDOWS = true;
  public boolean SHOW_TOOL_WINDOW_NUMBERS = true;
  public boolean HIDE_TOOL_STRIPES = true;
  public boolean WIDESCREEN_SUPPORT = false;
  public boolean LEFT_HORIZONTAL_SPLIT = false;
  public boolean RIGHT_HORIZONTAL_SPLIT = false;
  public boolean SHOW_EDITOR_TOOLTIP = true;
  @Deprecated
  @DeprecationInfo("see StatusBarWidgetSettings")
  public boolean SHOW_MEMORY_INDICATOR = false;
  public boolean ALLOW_MERGE_BUTTONS = true;
  public boolean SHOW_MAIN_TOOLBAR = false;
  public boolean SHOW_STATUS_BAR = true;
  public boolean SHOW_NAVIGATION_BAR = true;
  public boolean ALWAYS_SHOW_WINDOW_BUTTONS = false;
  public boolean CYCLE_SCROLLING = true;
  public boolean SCROLL_TAB_LAYOUT_IN_EDITOR = true;
  public boolean HIDE_TABS_IF_NEED = true;
  public boolean SHOW_CLOSE_BUTTON = true;

  public static final int PLACEMENT_EDITOR_TAB_NONE = 0;
  public static final int PLACEMENT_EDITOR_TAB_TOP = 1;
  public static final int PLACEMENT_EDITOR_TAB_LEFT = 2;
  public static final int PLACEMENT_EDITOR_TAB_BOTTOM = 3;
  public static final int PLACEMENT_EDITOR_TAB_RIGHT = 4;

  @Deprecated
  @DeprecationInfo("Use #PLACEMENT_EDITOR_TAB_NONE")
  public static final int TABS_NONE = PLACEMENT_EDITOR_TAB_NONE;

  public int EDITOR_TAB_PLACEMENT = PLACEMENT_EDITOR_TAB_TOP;
  public boolean EDITOR_TABS_ALPHABETICAL_SORT = false;
  public boolean HIDE_KNOWN_EXTENSION_IN_TABS = false;
  public boolean SHOW_ICONS_IN_QUICK_NAVIGATION = true;
  public boolean CLOSE_NON_MODIFIED_FILES_FIRST = false;
  public boolean ACTIVATE_MRU_EDITOR_ON_CLOSE = false;
  public boolean ACTIVATE_RIGHT_EDITOR_ON_CLOSE = false;
  public boolean CLOSE_TAB_BUTTON_ON_THE_RIGHT = true;

  public AntialiasingType IDE_AA_TYPE = AntialiasingType.SUBPIXEL;
  public AntialiasingType EDITOR_AA_TYPE = AntialiasingType.SUBPIXEL;
  public ColorBlindness COLOR_BLINDNESS;
  public boolean MOVE_MOUSE_ON_DEFAULT_BUTTON = false;
  public boolean ENABLE_ALPHA_MODE = false;
  public int ALPHA_MODE_DELAY = 1500;
  public float ALPHA_MODE_RATIO = 0.5f;
  public int MAX_CLIPBOARD_CONTENTS = 5;
  public boolean SHOW_ICONS_IN_MENUS = true;
  public boolean DISABLE_MNEMONICS = SystemInfo.isMac; // IDEADEV-33409, should be disabled by default on MacOS
  public boolean DISABLE_MNEMONICS_IN_CONTROLS = false;
  public boolean USE_SMALL_LABELS_ON_TABS = SystemInfo.isMac;
  public boolean SORT_LOOKUP_ELEMENTS_LEXICOGRAPHICALLY = false;
  public int MAX_LOOKUP_WIDTH2 = 500;
  public int MAX_LOOKUP_LIST_HEIGHT = 11;
  public boolean HIDE_NAVIGATION_ON_FOCUS_LOSS = true;
  public boolean SHOW_MEMBERS_IN_NAVIGATION_BAR = true;
  public boolean DND_WITH_PRESSED_ALT_ONLY = false;
  public boolean DEFAULT_AUTOSCROLL_TO_SOURCE = false;
  public boolean COMPACT_TREE_INDENTS = false;
  public boolean SHOW_TREE_INDENT_GUIDES = false;
  @Transient
  public boolean PRESENTATION_MODE = false;
  public int PRESENTATION_MODE_FONT_SIZE = 24;
  public boolean MARK_MODIFIED_TABS_WITH_ASTERISK = false;
  public boolean SHOW_TABS_TOOLTIPS = true;
  public boolean SHOW_DIRECTORY_FOR_NON_UNIQUE_FILENAMES = true;
  public boolean NAVIGATE_TO_PREVIEW = false;
  public boolean SMOOTH_SCROLLING = true;
  public boolean PIN_FIND_IN_PATH_POPUP = false;

  public int RECENT_LOCATIONS_LIMIT = 25;

  public boolean CONTRAST_SCROLLBARS = false;

  public boolean FULL_PATHS_IN_WINDOW_HEADER;

  public boolean SHOW_BREAKPOINTS_OVER_LINE_NUMBERS = true;

  @Deprecated
  @DeprecationInfo("Use UISettingsListener.class")
  public void addUISettingsListener(UISettingsListener listener) {
    ApplicationManager.getApplication().getMessageBus().connect().subscribe(UISettingsListener.class, listener);
  }

  @Deprecated
  @DeprecationInfo("Use UISettingsListener#TOPIC")
  public void addUISettingsListener(@Nonnull final UISettingsListener listener, @Nonnull Disposable parentDisposable) {
    ApplicationManager.getApplication().getMessageBus().connect(parentDisposable).subscribe(UISettingsListener.class, listener);
  }

  /**
   * Notifies all registered listeners that UI settings has been changed.
   */
  public void fireUISettingsChanged() {
    incModificationCount();
    notifyDispatcher();
    ApplicationManager.getApplication().getMessageBus().syncPublisher(UISettingsListener.class).uiSettingsChanged(this);
  }

  protected void notifyDispatcher() {
  }

  @Override
  public UISettings getState() {
    return this;
  }

  @Override
  public void loadState(UISettings object) {
    XmlSerializerUtil.copyBean(object, this);

    // Check tab placement in editor
    if (EDITOR_TAB_PLACEMENT != PLACEMENT_EDITOR_TAB_NONE &&
        EDITOR_TAB_PLACEMENT != PLACEMENT_EDITOR_TAB_TOP &&
        EDITOR_TAB_PLACEMENT != PLACEMENT_EDITOR_TAB_LEFT &&
        EDITOR_TAB_PLACEMENT != PLACEMENT_EDITOR_TAB_BOTTOM &&
        EDITOR_TAB_PLACEMENT != PLACEMENT_EDITOR_TAB_RIGHT) {
      EDITOR_TAB_PLACEMENT = PLACEMENT_EDITOR_TAB_TOP;
    }

    // Check that alpha delay and ratio are valid
    if (ALPHA_MODE_DELAY < 0) {
      ALPHA_MODE_DELAY = 1500;
    }
    if (ALPHA_MODE_RATIO < 0.0f || ALPHA_MODE_RATIO > 1.0f) {
      ALPHA_MODE_RATIO = 0.5f;
    }

    if (MAX_CLIPBOARD_CONTENTS <= 0) {
      MAX_CLIPBOARD_CONTENTS = 5;
    }

    fireUISettingsChanged();
  }

  public int getEditorTabLimit() {
    return EDITOR_TAB_LIMIT;
  }

  public int getEditorTabPlacement() {
    return EDITOR_TAB_PLACEMENT;
  }

  public boolean getShowTabsTooltips() {
    return SHOW_TABS_TOOLTIPS;
  }

  public boolean getHideTabsIfNeed() {
    return HIDE_TABS_IF_NEED;
  }

  public boolean getMarkModifiedTabsWithAsterisk() {
    return MARK_MODIFIED_TABS_WITH_ASTERISK;
  }

  public boolean getActiveMruEditorOnClose() {
    return ACTIVATE_MRU_EDITOR_ON_CLOSE;
  }

  public boolean getActiveRigtEditorOnClose() {
    return ACTIVATE_RIGHT_EDITOR_ON_CLOSE;
  }

  public boolean getCloseNonModifiedFilesFirst() {
    return CLOSE_NON_MODIFIED_FILES_FIRST;
  }

  public boolean getPresentationMode() {
    return PRESENTATION_MODE;
  }

  public boolean getReuseNotModifiedTabs() {
    return REUSE_NOT_MODIFIED_TABS;
  }

  public void setReuseNotModifiedTabs(boolean reuseNotModifiedTabs) {
    REUSE_NOT_MODIFIED_TABS = reuseNotModifiedTabs;
  }

  public boolean getScrollTabLayoutInEditor() {
    return SCROLL_TAB_LAYOUT_IN_EDITOR;
  }

  public boolean getHideToolStripes() {
    return HIDE_TOOL_STRIPES;
  }

  public boolean getShowCloseButton() {
    return SHOW_CLOSE_BUTTON;
  }

  public boolean getShowMainToolbar() {
    return SHOW_MAIN_TOOLBAR;
  }

  public void setShowMainToolbar(boolean value) {
    SHOW_MAIN_TOOLBAR = value;
  }

  public boolean getShowMemoryIndicator() {
    return SHOW_MEMORY_INDICATOR;
  }

  public boolean getShowStatusBar() {
    return SHOW_STATUS_BAR;
  }

  public int getMaxLookupWidth() {
    return MAX_LOOKUP_WIDTH2;
  }

  public void setMaxLookupWidth(int width) {
    MAX_LOOKUP_WIDTH2 = width;
  }

  public void setMaxLookupListHeight(int height) {
    MAX_LOOKUP_LIST_HEIGHT = height;
  }

  public void setSortLookupElementsLexicographically(boolean value) {
    SORT_LOOKUP_ELEMENTS_LEXICOGRAPHICALLY = value;
  }

  public boolean getShowNavigationBar() {
    return SHOW_NAVIGATION_BAR;
  }

  public int getPresentationModeFontSize() {
    return PRESENTATION_MODE_FONT_SIZE;
  }

  public boolean getSortLookupElementsLexicographically() {
    return SORT_LOOKUP_ELEMENTS_LEXICOGRAPHICALLY;
  }

  public int getMaxLookupListHeight() {
    return MAX_LOOKUP_LIST_HEIGHT;
  }

  public void setHideToolStripes(boolean value) {
    HIDE_TOOL_STRIPES = value;
  }

  public boolean getWideScreenSupport() {
    return WIDESCREEN_SUPPORT;
  }

  public boolean getAnimateWindows() {
    return ANIMATE_WINDOWS;
  }

  public boolean getLeftHorizontalSplit() {
    return LEFT_HORIZONTAL_SPLIT;
  }

  public void setLeftHorizontalSplit(boolean value) {
    LEFT_HORIZONTAL_SPLIT = value;
  }

  public boolean getRightHorizontalSplit() {
    return RIGHT_HORIZONTAL_SPLIT;
  }

  public void setRightHorizontalSplit(boolean value) {
    RIGHT_HORIZONTAL_SPLIT = value;
  }

  public boolean getShowIconInQuickNavigation() {
    return SHOW_ICONS_IN_QUICK_NAVIGATION;
  }

  public boolean getDndWithPressedAltOnly() {
    return DND_WITH_PRESSED_ALT_ONLY;
  }

  public void setDisableMnemonics(boolean value) {
    DISABLE_MNEMONICS = value;
  }

  public void setDisableMnemonicsInControls(boolean value) {
    DISABLE_MNEMONICS_IN_CONTROLS = value;
  }

  public boolean getDisableMnemonics() {
    return DISABLE_MNEMONICS;
  }

  public boolean getDisableMnemonicsInControls() {
    return DISABLE_MNEMONICS_IN_CONTROLS;
  }

  public boolean getPinFindInPath() {
    return PIN_FIND_IN_PATH_POPUP;
  }

  public void setPinFindInPath(boolean value) {
    PIN_FIND_IN_PATH_POPUP = value;
  }

  public boolean getUseSmallLabelsOnTabs() {
    return USE_SMALL_LABELS_ON_TABS;
  }

  public AntialiasingType getIdeAAType() {
    return IDE_AA_TYPE;
  }

  public boolean getCloseTabButtonOnTheRight() {
    return CLOSE_TAB_BUTTON_ON_THE_RIGHT;
  }

  public boolean getCycleScrolling() {
    return CYCLE_SCROLLING;
  }

  public boolean getShowEditorToolTip() {
    return SHOW_EDITOR_TOOLTIP;
  }

  public void setShowEditorToolTip(boolean value) {
    SHOW_EDITOR_TOOLTIP = value;
  }

  public boolean getHideKnownExtensionInTabs() {
    return HIDE_KNOWN_EXTENSION_IN_TABS;
  }

  public boolean getOverrideConsoleCycleBufferSize() {
    return OVERRIDE_CONSOLE_CYCLE_BUFFER_SIZE;
  }

  public int getConsoleCycleBufferSizeKb() {
    return CONSOLE_CYCLE_BUFFER_SIZE_KB;
  }

  public boolean getShowDirectoryForNonUniqueFilenames() {
    return SHOW_DIRECTORY_FOR_NON_UNIQUE_FILENAMES;
  }

  public boolean getHideNavigationOnFocusLoss() {
    return HIDE_NAVIGATION_ON_FOCUS_LOSS;
  }

  public boolean getShowIconsInMenus() {
    return SHOW_ICONS_IN_MENUS;
  }

  public boolean getShowInplaceCommentsInternal() {
    return false;
  }

  public boolean getShowInplaceComments() {
    return false;
  }

  public int getRecentFilesLimit() {
    return RECENT_FILES_LIMIT;
  }

  public void setRecentFilesLimit(int limit) {
    RECENT_FILES_LIMIT = limit;
  }

  public int getRecentLocationsLimit() {
    return RECENT_LOCATIONS_LIMIT;
  }

  public boolean getShowMembersInNavigationBar() {
    return SHOW_MEMBERS_IN_NAVIGATION_BAR;
  }

  public boolean getCompactTreeIndents() {
    return COMPACT_TREE_INDENTS;
  }

  public boolean getShowTreeIndentGuides() {
    return SHOW_TREE_INDENT_GUIDES;
  }

  public boolean getAnimatedScrolling() {
    return !SystemInfo.isMac || !SystemInfo.isJetBrainsJvm;
  }

  public boolean getFullPathsInWindowHeader() {
    return FULL_PATHS_IN_WINDOW_HEADER;
  }

  public boolean getUseContrastScrollbars() {
    return false;
  }

  public int getAnimatedScrollingDuration() {
    if (SystemInfo.isWindows) return 200;
    if (SystemInfo.isMac) return 50;
    return 150;
  }

  public int getAnimatedScrollingCurvePoints() {
    if (SystemInfo.isWindows) return 1684366536;
    if (SystemInfo.isMac) return 845374563;
    return 729434056;
  }

  public boolean getShowBreakpointsOverLineNumbers() {
    return SHOW_BREAKPOINTS_OVER_LINE_NUMBERS;
  }

  public void setShowBreakpointsOverLineNumbers(boolean value) {
    SHOW_BREAKPOINTS_OVER_LINE_NUMBERS = value;
  }
}
