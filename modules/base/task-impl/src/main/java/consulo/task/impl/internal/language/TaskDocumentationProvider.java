/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.task.impl.internal.language;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.documentation.ExternalDocumentationProvider;
import consulo.language.editor.documentation.UnrestrictedDocumentationProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.task.Comment;
import consulo.task.Task;
import consulo.util.lang.xml.XmlStringUtil;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author Dennis.Ushakov
 */
@ExtensionImpl
public class TaskDocumentationProvider implements UnrestrictedDocumentationProvider, ExternalDocumentationProvider {

  @Override
  public List<String> getUrlFor(PsiElement element, PsiElement originalElement) {
    if (element instanceof TaskPsiElement) {
      String url = ((TaskPsiElement)element).getTask().getIssueUrl();
      if (url != null) {
        return Collections.singletonList(url);
      }
    }
    return null;
  }

  @Override
  public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
    if (!(element instanceof TaskPsiElement)) {
      return null;
    }
    Task task = ((TaskPsiElement)element).getTask();
    StringBuilder builder = new StringBuilder();
    builder.append("<b>Summary:</b> ").append(task.getSummary()).append("<br>");
    builder.append("<b>Id:</b> ").append(task.getId()).append("<br>");
    if (task.getCreated() != null) {
      builder.append("<b>Created at:</b> ").append(task.getCreated()).append("<br>");
    }
    if (task.getUpdated() != null) {
      builder.append("<b>Updated at:</b> ").append(task.getUpdated()).append("<br>");
    }
    String description = task.getDescription();
    if (description != null) {
      builder.append("<b>Description:</b><br>").append(description);
    }
    for (Comment comment : task.getComments()) {
      comment.appendTo(builder);
    }
    return XmlStringUtil.wrapInHtml(builder);
  }

  @Override
  public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
    return object instanceof Task ? new TaskPsiElement(psiManager, (Task)object) : null;
  }


  @Override
  public String fetchExternalDocumentation(Project project, PsiElement element, List<String> docUrls) {
    return null;
  }

  @Override
  public boolean hasDocumentationFor(PsiElement element, PsiElement originalElement) {
    return element instanceof TaskPsiElement;
  }

  @Override
  public boolean canPromptToConfigureDocumentation(PsiElement element) {
    return false;
  }

  @Override
  public void promptToConfigureDocumentation(PsiElement element) {
  }
}
