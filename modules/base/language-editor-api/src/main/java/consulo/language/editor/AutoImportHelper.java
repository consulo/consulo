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
package consulo.language.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.keymap.util.KeymapUtil;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @see ReferenceImporter
 * @since 26-Jul-22
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface AutoImportHelper {
  static AutoImportHelper getInstance(@Nonnull Project project) {
    return project.getInstance(AutoImportHelper.class);
  }

  boolean canChangeFileSilently(@Nonnull PsiFile file);

  boolean mayAutoImportNow(@Nonnull PsiFile psiFile, boolean isInContent);

  void runOptimizeImports(@Nonnull Project project, @Nonnull PsiFile file, boolean withProgress);

  @Nonnull
  default String getImportMessage(final boolean multiple, @Nonnull String name) {
    final String messageKey = multiple ? "import.popup.multiple" : "import.popup.text";
    String hintText = DaemonBundle.message(messageKey, name);
    hintText += " " + KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS));
    return hintText;
  }
}
