/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.intellij.codeInsight.hint;

import com.intellij.codeInsight.CodeInsightActionHandler;
import consulo.codeInsight.TargetElementUtil;
import com.intellij.lang.ExpressionTypeProvider;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageExpressionTypes;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pass;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.IntroduceTargetChooser;
import com.intellij.util.Function;
import com.intellij.util.ObjectUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import javax.annotation.Nonnull;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.Map;
import java.util.Set;

public class ShowExpressionTypeHandler implements CodeInsightActionHandler {

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @RequiredUIAccess
  @Override
  public void invoke(@Nonnull final Project project, @Nonnull final Editor editor, @Nonnull PsiFile file) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    PsiDocumentManager.getInstance(project).commitAllDocuments();

    PsiElement elementAt = file.findElementAt(
            TargetElementUtil.adjustOffset(file, editor.getDocument(), editor.getCaretModel().getOffset()));
    if (elementAt == null) return;

    Language language = elementAt.getLanguage();
    final Set<ExpressionTypeProvider> handlers = getHandlers(project, language, file.getViewProvider().getBaseLanguage());
    if (handlers.isEmpty()) return;

    boolean exactRange = false;
    TextRange range = EditorUtil.getSelectionInAnyMode(editor);
    final Map<PsiElement, ExpressionTypeProvider> map = ContainerUtil.newLinkedHashMap();
    for (ExpressionTypeProvider handler : handlers) {
      for (PsiElement element : ((ExpressionTypeProvider<? extends PsiElement>)handler).getExpressionsAt(elementAt)) {
        TextRange r = element.getTextRange();
        if (exactRange && !r.equals(range) || !r.contains(range)) continue;
        if (!exactRange) exactRange = r.equals(range);
        map.put(element, handler);
      }
    }
    Pass<PsiElement> callback = new Pass<PsiElement>() {
      @Override
      public void pass(@Nonnull PsiElement expression) {
        //noinspection unchecked
        ExpressionTypeProvider<PsiElement> provider = ObjectUtil.assertNotNull(map.get(expression));
        final String informationHint = provider.getInformationHint(expression);
        TextRange range = expression.getTextRange();
        editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset());
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            HintManager.getInstance().showInformationHint(editor, informationHint);
          }
        });
      }
    };
    if (map.isEmpty()) {
      ApplicationManager.getApplication().invokeLater(new Runnable() {
        @Override
        public void run() {
          String errorHint = ObjectUtil.assertNotNull(ContainerUtil.getFirstItem(handlers)).getErrorHint();
          HintManager.getInstance().showErrorHint(editor, errorHint);
        }
      });
    }
    else if (map.size() == 1) {
      callback.pass(ObjectUtil.assertNotNull(ContainerUtil.getFirstItem(map.keySet())));
    }
    else {
      IntroduceTargetChooser.showChooser(
              editor, ContainerUtil.newArrayList(map.keySet()), callback,
              new Function<PsiElement, String>() {
                @Override
                public String fun(@Nonnull PsiElement expression) {
                  return expression.getText();
                }
              }
      );
    }
  }

  @Nonnull
  public static Set<ExpressionTypeProvider> getHandlers(final Project project, Language... languages) {
    return JBIterable.of(languages).flatten(new Function<Language, Iterable<ExpressionTypeProvider>>() {
      @Override
      public Iterable<ExpressionTypeProvider> fun(Language language) {
        return DumbService.getInstance(project).filterByDumbAwareness(LanguageExpressionTypes.INSTANCE.allForLanguage(language));
      }
    }).addAllTo(ContainerUtil.<ExpressionTypeProvider>newLinkedHashSet());
  }

}

