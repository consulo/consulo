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

package consulo.ide.impl.idea.codeInsight.template.macro;

import consulo.application.impl.internal.IdeaModalityState;
import consulo.language.editor.WriteCommandAction;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.language.editor.internal.PsiUtilBase;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.completion.lookup.event.LookupAdapter;
import consulo.language.editor.completion.lookup.event.LookupEvent;
import consulo.language.editor.template.*;
import consulo.language.editor.template.macro.Macro;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;

import javax.annotation.Nonnull;

public abstract class BaseCompleteMacro extends Macro {
  private final String myName;

  protected BaseCompleteMacro(String name) {
    myName = name;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public String getPresentableName() {
    return myName + "()";
  }

  @Override
  @Nonnull
  public String getDefaultValue() {
    return "a";
  }

  @Override
  public final Result calculateResult(@Nonnull Expression[] params, final ExpressionContext context) {
    return new InvokeActionResult(new Runnable() {
      @Override
      public void run() {
        invokeCompletion(context);
      }
    });
  }

  private void invokeCompletion(final ExpressionContext context) {
    final Project project = context.getProject();
    final Editor editor = context.getEditor();

    final PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        if (project.isDisposed() || editor.isDisposed() || psiFile == null || !psiFile.isValid()) return;

        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
          @Override
          public void run() {
            invokeCompletionHandler(project, editor);
            Lookup lookup = LookupManager.getInstance(project).getActiveLookup();

            if (lookup != null) {
              lookup.addLookupListener(new MyLookupListener(context));
            }
            else {
              considerNextTab(editor);
            }
          }
        }, "", null);
      }
    };
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      runnable.run();
    }
    else {
      ApplicationManager.getApplication().invokeLater(runnable);
    }
  }

  private static void considerNextTab(Editor editor) {
    TemplateState templateState = TemplateManager.getInstance(editor.getProject()).getTemplateState(editor);
    if (templateState != null) {
      TextRange range = templateState.getCurrentVariableRange();
      if (range != null && range.getLength() > 0) {
        int caret = editor.getCaretModel().getOffset();
        if (caret == range.getEndOffset()) {
          templateState.nextTab();
        }
        else if (caret > range.getEndOffset()) {
          templateState.cancelTemplate();
        }
      }
    }
  }

  protected abstract void invokeCompletionHandler(Project project, Editor editor);

  private static class MyLookupListener extends LookupAdapter {
    private final ExpressionContext myContext;

    public MyLookupListener(@Nonnull ExpressionContext context) {
      myContext = context;
    }

    @Override
    public void itemSelected(LookupEvent event) {
      LookupElement item = event.getItem();
      if (item == null) return;

      char c = event.getCompletionChar();
      if (!LookupEvent.isSpecialCompletionChar(c)) {
        return;
      }

      for (TemplateCompletionProcessor processor : TemplateCompletionProcessor.EP_NAME.getExtensionList()) {
        if (!processor.nextTabOnItemSelected(myContext, item)) {
          return;
        }
      }

      final Project project = myContext.getProject();
      if (project == null) {
        return;
      }

      Runnable runnable = new Runnable() {
        @Override
        public void run() {
          new WriteCommandAction(project) {
            @Override
            protected void run(consulo.application.Result result) throws Throwable {
              Editor editor = myContext.getEditor();
              if (editor != null) {
                considerNextTab(editor);
              }
            }
          }.execute();
        }
      };
      if (ApplicationManager.getApplication().isUnitTestMode()) {
        runnable.run();
      }
      else {
        ApplicationManager.getApplication().invokeLater(runnable, IdeaModalityState.current(), project.getDisposed());
      }

    }
  }
}
