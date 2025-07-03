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
package consulo.language.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ChangeLocalityDetector {
  /**
   * @param changedElement
   * @return the psi element (ancestor of the changedElement) which should be re-highlighted, or null if unsure.
   * Examples:
   *  - in Java, when the statement has changed, re-highlight the enclosing code block only.
   *  - in (hypothetical) framework which stores its annotations in comments, e.g. "// @someAnnotation",
   *    when that special comment has changed, re-highlight the whole file.
   *
   * Note: for the performance sake, do not traverse PSI tree upwards here, since this method will be called for the
   *       changed element and all its parents anyway.
   *       So the parent check is enough, e.g: {@code changedElement.getParent() instanceof PsiCodeBlock}
   */
  @Nullable
  PsiElement getChangeHighlightingDirtyScopeFor(@Nonnull PsiElement changedElement);
}