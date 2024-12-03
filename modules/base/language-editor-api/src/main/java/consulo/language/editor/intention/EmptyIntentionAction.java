/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.language.editor.intention;

import consulo.codeEditor.Editor;
import consulo.component.util.Iconable;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

/**
 * User: anna
 * Date: May 11, 2005
 */
public final class EmptyIntentionAction extends AbstractEmptyIntentionAction implements SyntheticIntentionAction, LowPriorityAction, Iconable {
  private final String myName;

  public EmptyIntentionAction(@Nonnull String name) {
    myName = name;
  }

  @Override
  @Nonnull
  public String getText() {
    return InspectionsBundle.message("inspection.options.action.text", myName);
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return true; //edit inspection settings is always enabled
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final EmptyIntentionAction that = (EmptyIntentionAction)o;

    return myName.equals(that.myName);
  }

  public int hashCode() {
    return myName.hashCode();
  }

  @Override
  public Image getIcon(@IconFlags int flags) {
    return Image.empty(Image.DEFAULT_ICON_SIZE);
  }
}
