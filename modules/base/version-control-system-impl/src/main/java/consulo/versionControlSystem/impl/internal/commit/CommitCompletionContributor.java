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
package consulo.versionControlSystem.impl.internal.commit;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.progress.ProgressManager;
import consulo.application.util.matcher.PlainPrefixMatcher;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.editor.completion.CamelHumpMatcher;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.PlainTextSymbolCompletionContributor;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.ui.awt.TextFieldWithAutoCompletionListProvider;
import consulo.language.plain.PlainTextLanguage;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeList;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFilePresentation;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl(id = "commitCompletion", order = "first, before liveTemplates")
public class CommitCompletionContributor extends CompletionContributor {

  @RequiredReadAction
  @Override
  public void fillCompletionVariants(CompletionParameters parameters, CompletionResultSet result) {
    PsiFile file = parameters.getOriginalFile();
    Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
    if (document != null) {
      DataContext dataContext = document.getUserData(CommitMessageImpl.DATA_CONTEXT_KEY);
      if (dataContext != null) {
        result.stopHere();
        if (parameters.getInvocationCount() > 0) {
          ChangeList[] lists = dataContext.getData(VcsDataKeys.CHANGE_LISTS);
          if (lists != null) {
            String prefix = TextFieldWithAutoCompletionListProvider.getCompletionPrefix(parameters);
            CompletionResultSet insensitive = result.caseInsensitive().withPrefixMatcher(new CamelHumpMatcher(prefix));
            CompletionResultSet prefixed = result.withPrefixMatcher(new PlainPrefixMatcher(prefix));
            for (ChangeList list : lists) {
              for (Change change : list.getChanges()) {
                ProgressManager.checkCanceled();

                VirtualFile virtualFile = change.getVirtualFile();
                if (virtualFile != null) {
                  LookupElementBuilder element = LookupElementBuilder.create(virtualFile.getName()).withIcon(VirtualFilePresentation.getIcon(virtualFile));
                  insensitive.addElement(element);
                  addLanguageSpecificElements(file.getProject(), parameters.getInvocationCount(), prefixed, virtualFile);
                }
              }
            }
          }
        }
      }
    }
  }

  @RequiredReadAction
  private static void addLanguageSpecificElements(Project project, int count, CompletionResultSet prefixed, VirtualFile vFile) {
    PsiFile psiFile = PsiManager.getInstance(project).findFile(vFile);
    if (psiFile == null) {
      return;
    }
    PlainTextSymbolCompletionContributor contributor = PlainTextSymbolCompletionContributor.forLanguage(psiFile.getLanguage());
    if (contributor == null) {
      return;
    }
    prefixed.addAllElements(contributor.getLookupElements(psiFile, count, prefixed.getPrefixMatcher().getPrefix()));
  }

  @Override
  public Language getLanguage() {
    return PlainTextLanguage.INSTANCE;
  }
}
