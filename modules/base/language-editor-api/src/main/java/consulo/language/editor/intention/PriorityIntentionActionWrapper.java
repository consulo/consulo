/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.annotation.UsedInPlugin;
import consulo.codeEditor.Editor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import jakarta.annotation.Nonnull;

/**
 * @author Danila Ponomarenko
 */
@UsedInPlugin
public abstract class PriorityIntentionActionWrapper implements IntentionAction, IntentionActionDelegate {
  private final IntentionAction action;

  private PriorityIntentionActionWrapper(@Nonnull IntentionAction action) {
    this.action = action;
  }

  @Nonnull
  @Override
  public LocalizeValue getText() {
    return action.getText();
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return action.isAvailable(project, editor, file);
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    action.invoke(project, editor, file);
  }

  @Nonnull
  @Override
  public IntentionAction getDelegate() {
    return action;
  }

  @Override
  public boolean startInWriteAction() {
    return action.startInWriteAction();
  }

  private static class HighPriorityIntentionActionWrapper extends PriorityIntentionActionWrapper implements HighPriorityAction {
    protected HighPriorityIntentionActionWrapper(@Nonnull IntentionAction action) {
      super(action);
    }
  }

  private static class NormalPriorityIntentionActionWrapper extends PriorityIntentionActionWrapper {
    protected NormalPriorityIntentionActionWrapper(@Nonnull IntentionAction action) {
      super(action);
    }
  }

  private static class LowPriorityIntentionActionWrapper extends PriorityIntentionActionWrapper implements LowPriorityAction {
    protected LowPriorityIntentionActionWrapper(@Nonnull IntentionAction action) {
      super(action);
    }
  }

  @Nonnull
  public static IntentionAction highPriority(@Nonnull IntentionAction action) {
    return new HighPriorityIntentionActionWrapper(action);
  }

  @Nonnull
  public static IntentionAction normalPriority(@Nonnull IntentionAction action) {
    return new NormalPriorityIntentionActionWrapper(action);
  }

  @Nonnull
  public static IntentionAction lowPriority(@Nonnull IntentionAction action) {
    return new LowPriorityIntentionActionWrapper(action);
  }
}
