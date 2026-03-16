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
package consulo.execution.debug.evaluation;

import consulo.annotation.access.RequiredReadAction;
import consulo.execution.debug.XDebuggerUtil;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import org.jspecify.annotations.Nullable;
import java.util.Collection;
import java.util.Collections;

public abstract class XDebuggerEditorsProviderBase extends XDebuggerEditorsProvider {
  @RequiredReadAction
  
  @Override
  public final Document createDocument(Project project, String text, @Nullable XSourcePosition sourcePosition, EvaluationMode mode) {
    PsiElement context = null;
    if (sourcePosition != null) {
      context = getContextElement(sourcePosition.getFile(), sourcePosition.getOffset(), project);
    }

    PsiFile codeFragment = createExpressionCodeFragment(project, text, context, true);
    Document document = PsiDocumentManager.getInstance(project).getDocument(codeFragment);
    assert document != null;
    return document;
  }

  
  @Override
  @RequiredReadAction
  public Document createDocument(Project project,
                                 XExpression expression,
                                 @Nullable XSourcePosition sourcePosition,
                                 EvaluationMode mode) {
    PsiElement context = null;
    if (sourcePosition != null) {
      context = getContextElement(sourcePosition.getFile(), sourcePosition.getOffset(), project);
    }
    return createDocument(project, expression, context, mode);
  }

  
  public Document createDocument(Project project,
                                 XExpression expression,
                                 @Nullable PsiElement context,
                                 EvaluationMode mode) {
    PsiFile codeFragment = createExpressionCodeFragment(project, expression, context, true);
    Document document = PsiDocumentManager.getInstance(project).getDocument(codeFragment);
    assert document != null;
    return document;
  }

  protected abstract PsiFile createExpressionCodeFragment(Project project, String text, @Nullable PsiElement context, boolean isPhysical);

  protected PsiFile createExpressionCodeFragment(Project project, XExpression expression, @Nullable PsiElement context, boolean isPhysical) {
    return createExpressionCodeFragment(project, expression.getExpression(), context, isPhysical);
  }

  
  public Collection<Language> getSupportedLanguages(@Nullable PsiElement context) {
    if (context != null) {
      return getSupportedLanguages(context.getProject(), null);
    }
    return Collections.emptyList();
  }

  @Nullable
  @RequiredReadAction
  protected PsiElement getContextElement(VirtualFile virtualFile, int offset, Project project) {
    return XDebuggerUtil.getInstance().findContextElement(virtualFile, offset, project);
  }
}