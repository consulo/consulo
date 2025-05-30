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

package consulo.language.editor.navigation;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nullable;

/**
 * @author yole
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface GotoDeclarationHandler {
  ExtensionPointName<GotoDeclarationHandler> EP_NAME = ExtensionPointName.create(GotoDeclarationHandler.class);

  /**
   * Provides an array of target declarations for given {@code sourceElement}.
   *
   *
   * @param sourceElement input psiElement
   * @param offset offset in the file
   *@param editor  @return all target declarations as an array of  {@code PsiElement} or null if none was found
   */
  @Nullable
  PsiElement[] getGotoDeclarationTargets(PsiElement sourceElement, int offset, Editor editor);

  /**
   * Provides the custom action text
   * @return the custom text or null to use the default text
   * @param context the action data context
   */
  @Nullable
  default String getActionText(DataContext context) {
    return null;
  }
}
