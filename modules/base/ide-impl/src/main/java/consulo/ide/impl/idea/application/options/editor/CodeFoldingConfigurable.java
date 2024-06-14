/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.application.options.editor;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.language.editor.folding.CodeFoldingManager;
import consulo.application.ApplicationBundle;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.configurable.Configurable;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.disposer.Disposable;
import consulo.ide.impl.configurable.EditorGeneralConfigurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.platform.base.localize.ApplicationLocalize;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
@ExtensionImpl
public class CodeFoldingConfigurable extends SimpleConfigurableByProperties implements Configurable, ApplicationConfigurable {
  @Nonnull
  @Override
  public String getId() {
    return "editor.preferences.folding";
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.EDITOR_GROUP;
  }

  @Override
  @Nls
  public String getDisplayName() {
    return ApplicationBundle.message("group.code.folding");
  }

  @Override
  public String getHelpTopic() {
    return "reference.settingsdialog.IDE.editor.code.folding";
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  protected Component createLayout(PropertyBuilder propertyBuilder, @Nonnull Disposable uiDisposable) {
    VerticalLayout verticalLayout = VerticalLayout.create();

    CheckBox outlineBox = CheckBox.create(ApplicationLocalize.checkboxShowCodeFoldingOutline());
    verticalLayout.add(outlineBox);
    EditorSettingsExternalizable externalizable = EditorSettingsExternalizable.getInstance();
    propertyBuilder.add(outlineBox, externalizable::isFoldingOutlineShown, externalizable::setFoldingOutlineShown);
    return verticalLayout;
  }

  @Override
  protected void afterApply() {
    super.afterApply();

    final List<Pair<Editor, Project>> toUpdate = new ArrayList<Pair<Editor, Project>>();
    for (final Editor editor : EditorFactory.getInstance().getAllEditors()) {
      final Project project = editor.getProject();
      if (project != null && !project.isDefault()) {
        toUpdate.add(Pair.create(editor, project));
      }
    }

    ApplicationManager.getApplication().invokeLater(() -> {
      for (Pair<Editor, Project> each : toUpdate) {
        if (each.second == null || each.second.isDisposed()) {
          continue;
        }
        final CodeFoldingManager foldingManager = CodeFoldingManager.getInstance(each.second);
        if (foldingManager != null) {
          foldingManager.buildInitialFoldings(each.first);
        }
      }
      EditorGeneralConfigurable.reinitAllEditors();
    }, IdeaModalityState.nonModal());
  }
}
