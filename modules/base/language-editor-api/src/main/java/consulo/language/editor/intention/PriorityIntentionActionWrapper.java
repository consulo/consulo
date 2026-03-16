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

/**
 * @author Danila Ponomarenko
 */
@UsedInPlugin
public abstract class PriorityIntentionActionWrapper implements IntentionAction, IntentionActionDelegate {
  private final IntentionAction action;

  private PriorityIntentionActionWrapper(IntentionAction action) {
    this.action = action;
  }

  
  @Override
  public LocalizeValue getText() {
    return action.getText();
  }

  @Override
  public boolean isAvailable(Project project, Editor editor, PsiFile file) {
    return action.isAvailable(project, editor, file);
  }

  @Override
  public void invoke(Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    action.invoke(project, editor, file);
  }

  
  @Override
  public IntentionAction getDelegate() {
    return action;
  }

  @Override
  public boolean startInWriteAction() {
    return action.startInWriteAction();
  }

  private static class HighPriorityIntentionActionWrapper extends PriorityIntentionActionWrapper implements HighPriorityAction {
    protected HighPriorityIntentionActionWrapper(IntentionAction action) {
      super(action);
    }
  }

  private static class NormalPriorityIntentionActionWrapper extends PriorityIntentionActionWrapper {
    protected NormalPriorityIntentionActionWrapper(IntentionAction action) {
      super(action);
    }
  }

  private static class LowPriorityIntentionActionWrapper extends PriorityIntentionActionWrapper implements LowPriorityAction {
    protected LowPriorityIntentionActionWrapper(IntentionAction action) {
      super(action);
    }
  }

  
  public static IntentionAction highPriority(IntentionAction action) {
    return new HighPriorityIntentionActionWrapper(action);
  }

  
  public static IntentionAction normalPriority(IntentionAction action) {
    return new NormalPriorityIntentionActionWrapper(action);
  }

  
  public static IntentionAction lowPriority(IntentionAction action) {
    return new LowPriorityIntentionActionWrapper(action);
  }
}
