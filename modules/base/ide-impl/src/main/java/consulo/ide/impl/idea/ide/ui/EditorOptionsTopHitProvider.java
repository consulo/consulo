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
import consulo.application.localize.ApplicationLocalize;
import consulo.application.ui.UISettings;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.component.ComponentManager;
import consulo.configurable.search.BooleanOptionDescription;
import consulo.configurable.search.OptionsTopHitProvider;
import consulo.configurable.search.PublicMethodBasedOptionDescription;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ProjectManager;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * The editor options no page binds - the others come from the generated options of the editor pages.
 *
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
public class EditorOptionsTopHitProvider extends OptionsTopHitProvider {
  static final String ID = "editor";

  private static final Collection<BooleanOptionDescription> ourOptions = List.of(
    editor(
      IdeLocalize.labelOptionMouse(
        Platform.current().os().isMac()
          ? ApplicationLocalize.checkboxEnableCtrlMousewheelChangesFontSizeMacos()
          : ApplicationLocalize.checkboxEnableCtrlMousewheelChangesFontSize()
      ),
      "editor.general",
      EditorSettingsExternalizable::isWheelFontChangeEnabled,
      EditorSettingsExternalizable::setWheelFontChangeEnabled
    ),
    editor(
      LocalizeValue.of(ApplicationLocalize.checkboxShowSoftwrapsOnlyForCaretLineActionText().get()),
      "editor.general",
      settings -> !settings.isAllSoftWrapsShown(),
      (settings, enabled) -> settings.setAllSoftwrapsShown(!enabled)
    ),
    editor(
      IdeLocalize.labelOptionAppearance(IdeLocalize.labelAppearanceShowLeadingWhitespaces()),
      "editor.preferences.appearance",
      EditorSettingsExternalizable::isLeadingWhitespacesShown,
      EditorSettingsExternalizable::setLeadingWhitespacesShown
    ),
    editor(
      IdeLocalize.labelOptionAppearance(IdeLocalize.labelAppearanceShowInnerWhitespaces()),
      "editor.preferences.appearance",
      EditorSettingsExternalizable::isInnerWhitespacesShown,
      EditorSettingsExternalizable::setInnerWhitespacesShown
    ),
    editor(
      IdeLocalize.labelOptionAppearance(IdeLocalize.labelAppearanceShowTrailingWhitespaces()),
      "editor.preferences.appearance",
      EditorSettingsExternalizable::isTrailingWhitespacesShown,
      EditorSettingsExternalizable::setTrailingWhitespacesShown
    ),
    // code folding rebuilds the foldings when applied, so its page gets no generated options
    editor(
      IdeLocalize.labelOptionAppearance(ApplicationLocalize.checkboxShowCodeFoldingOutline()),
      "editor.preferences.folding",
      EditorSettingsExternalizable::isFoldingOutlineShown,
      EditorSettingsExternalizable::setFoldingOutlineShown
    ),
    AppearanceOptionsTopHitProvider.uiOption(
      IdeLocalize.labelOptionTabs(ApplicationLocalize.radioCloseNonModifiedFilesFirst()),
      "editor.preferences.tabs",
      UISettings::getCloseNonModifiedFilesFirst,
      UISettings::setCloseNonModifiedFilesFirst
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

  /**
   * An option of the editor settings - the editors and the daemons follow it once switched.
   */
  static BooleanOptionDescription editor(LocalizeValue option,
                                         String configurableId,
                                         Function<EditorSettingsExternalizable, Boolean> getter,
                                         BiConsumer<EditorSettingsExternalizable, Boolean> setter) {
    return new PublicMethodBasedOptionDescription<>(
      option.map(HTML_STRIP).get(),
      configurableId,
      EditorSettingsExternalizable::getInstance,
      getter,
      setter,
      EditorOptionsTopHitProvider::editorSettingsChanged
    );
  }

  private static void editorSettingsChanged() {
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      DaemonCodeAnalyzer.getInstance(project).settingsChanged();
    }
    EditorFactory.getInstance().refreshAllEditors();
  }
}
