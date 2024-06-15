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
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.ide.impl.idea.ide.ui.search.BooleanOptionDescription;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.application.localize.ApplicationLocalize;
import consulo.platform.base.localize.IdeLocalize;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
public class EditorOptionsTopHitProvider extends OptionsTopHitProvider {
  static final String ID = "editor";

  private static final Collection<BooleanOptionDescription> ourOptions = List.of(
    editor(
      IdeLocalize.labelOptionMouse(ApplicationLocalize.checkboxHonorCamelhumpsWordsSettingsOnDoubleClick()),
      "IS_MOUSE_CLICK_SELECTION_HONORS_CAMEL_WORDS"
    ),
    editor(
      IdeLocalize.labelOptionMouse(
        Platform.current().os().isMac()
          ? ApplicationLocalize.checkboxEnableCtrlMousewheelChangesFontSizeMacos()
          : ApplicationLocalize.checkboxEnableCtrlMousewheelChangesFontSize()
      ),
      "IS_WHEEL_FONTCHANGE_ENABLED"
    ),
    editor(IdeLocalize.labelOptionMouse(ApplicationLocalize.checkboxEnableDragNDropFunctionalityInEditor()), "IS_DND_ENABLED"),
    new EditorOptionDescription(
      null,
      ApplicationLocalize.checkboxShowSoftwrapsOnlyForCaretLineActionText().map(HTML_STRIP).get(),
      "preferences.editor"
    ) {
      @Override
      public boolean isOptionEnabled() {
        return !EditorSettingsExternalizable.getInstance().isAllSoftWrapsShown();
      }

      @Override
      public void setOptionState(boolean enabled) {
        EditorSettingsExternalizable.getInstance().setAllSoftwrapsShown(!enabled);
        fireUpdated();
      }
    },
    editor(IdeLocalize.labelOptionVirtualSpace(ApplicationLocalize.checkboxAllowPlacementOfCaretAfterEndOfLine()), "IS_VIRTUAL_SPACE"),
    editor(IdeLocalize.labelOptionVirtualSpace(ApplicationLocalize.checkboxAllowPlacementOfCaretInsideTabs()), "IS_CARET_INSIDE_TABS"),
    editor(IdeLocalize.labelOptionVirtualSpace(ApplicationLocalize.checkboxShowVirtualSpaceAtFileBottom()), "ADDITIONAL_PAGE_AT_BOTTOM"),
    editorUI(IdeLocalize.labelOptionAppearance(IdeLocalize.checkboxUseAntialiasedFontInEditor()), "ANTIALIASING_IN_EDITOR"),
    editorApp(IdeLocalize.labelOptionAppearance(IdeLocalize.labelAppearanceCaretBlinking()), "IS_CARET_BLINKING"),
    editorApp(IdeLocalize.labelOptionAppearance(ApplicationLocalize.checkboxUseBlockCaret()), "IS_BLOCK_CURSOR"),
    editorApp(IdeLocalize.labelOptionAppearance(IdeLocalize.labelAppearanceShowRightMargin()), "IS_RIGHT_MARGIN_SHOWN"),
    editorCode(IdeLocalize.labelOptionAppearance(ApplicationLocalize.checkboxShowMethodSeparators()), "SHOW_METHOD_SEPARATORS"),
    editorApp(IdeLocalize.labelOptionAppearance(ApplicationLocalize.checkboxShowWhitespaces()), "IS_WHITESPACES_SHOWN"),
    editorApp(IdeLocalize.labelOptionAppearance(IdeLocalize.labelAppearanceShowLeadingWhitespaces()), "IS_LEADING_WHITESPACES_SHOWN"),
    editorApp(IdeLocalize.labelOptionAppearance(IdeLocalize.labelAppearanceShowInnerWhitespaces()), "IS_INNER_WHITESPACES_SHOWN"),
    editorApp(IdeLocalize.labelOptionAppearance(IdeLocalize.labelAppearanceShowTrailingWhitespaces()), "IS_TRAILING_WHITESPACES_SHOWN"),
    editorApp(IdeLocalize.labelOptionAppearance(IdeLocalize.labelAppearanceShowVerticalIndentGuides()), "IS_INDENT_GUIDES_SHOWN"),
    option(
      IdeLocalize.labelOptionAppearance(ApplicationLocalize.checkboxShowCodeFoldingOutline()),
      "IS_FOLDING_OUTLINE_SHOWN",
      "editor.preferences.folding"
    ),
    editorTabs(IdeLocalize.labelOptionTabs(ApplicationLocalize.checkboxEditorTabsInSingleRow()), "SCROLL_TAB_LAYOUT_IN_EDITOR"),
    editorTabs(IdeLocalize.labelOptionTabs(ApplicationLocalize.checkboxHideFileExtensionInEditorTabs()), "HIDE_KNOWN_EXTENSION_IN_TABS"),
    editorTabs(IdeLocalize.labelOptionTabs(IdeLocalize.labelTabsShowDirectoryForNonUniqueFilenames()), "SHOW_DIRECTORY_FOR_NON_UNIQUE_FILENAMES"),
    editorTabs(IdeLocalize.labelOptionTabs(ApplicationLocalize.checkboxEditorTabsShowCloseButton()), "SHOW_CLOSE_BUTTON"),
    editorTabs(IdeLocalize.labelOptionTabs(ApplicationLocalize.checkboxMarkModifiedTabsWithAsterisk()), "MARK_MODIFIED_TABS_WITH_ASTERISK"),
    editorTabs(IdeLocalize.labelOptionTabs(ApplicationLocalize.checkboxShowTabsTooltips()), "SHOW_TABS_TOOLTIPS"),
    editorTabs(IdeLocalize.labelOptionTabs(ApplicationLocalize.radioCloseNonModifiedFilesFirst()), "CLOSE_NON_MODIFIED_FILES_FIRST")
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

  static BooleanOptionDescription editor(LocalizeValue option, String field) {
    return option(option, field, "preferences.editor");
  }

  static BooleanOptionDescription editorTabs(LocalizeValue option, String field) {
    return AppearanceOptionsTopHitProvider.option(option, field, "editor.preferences.tabs");
  }

  static BooleanOptionDescription option(LocalizeValue option, String field, String configurableId) {
    return new EditorOptionDescription(field, option.map(HTML_STRIP).get(), configurableId);
  }

  static BooleanOptionDescription editorApp(LocalizeValue option, String field) {
    return option(option, field, "editor.preferences.appearance");
  }

  static BooleanOptionDescription editorUI(LocalizeValue option, String field) {
    return AppearanceOptionsTopHitProvider.option(option, field, "editor.preferences.appearance");
  }

  static BooleanOptionDescription editorCode(LocalizeValue option, String field) {
    return new DaemonCodeAnalyzerOptionDescription(field, option.map(HTML_STRIP).get(), "editor.preferences.appearance");
  }
}
