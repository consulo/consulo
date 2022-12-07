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

package consulo.ide.impl.idea.codeInsight.intention.impl.config;

import consulo.annotation.DeprecationInfo;
import consulo.codeEditor.Editor;
import consulo.language.editor.intention.IntentionActionDelegate;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.action.ShortcutProvider;
import consulo.ui.ex.action.ShortcutSet;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Deprecated
@DeprecationInfo("We don't use beans")
public class IntentionActionWrapper implements IntentionAction, ShortcutProvider, IntentionActionDelegate {
  private static final Logger LOG = Logger.getInstance(IntentionActionWrapper.class);

  @Nonnull
  @Override
  public IntentionAction getDelegate() {
    throw new UnsupportedOperationException();
  }

  @Nls
  @Nonnull
  @Override
  public String getText() {
    throw new UnsupportedOperationException();
  }

  @Nls
  @Nonnull
  @Override
  public String getFamilyName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public ShortcutSet getShortcut() {
    throw new UnsupportedOperationException();
  }
}
