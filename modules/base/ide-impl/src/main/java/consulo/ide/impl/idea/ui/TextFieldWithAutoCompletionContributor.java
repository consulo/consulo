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
package consulo.ide.impl.idea.ui;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressManager;
import consulo.application.util.matcher.PrefixMatcher;
import consulo.component.ProcessCanceledException;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.editor.completion.AutoCompletionPolicy;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.completion.lookup.PrioritizedLookupElement;
import consulo.language.plain.PlainTextLanguage;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ex.action.IdeActions;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Roman.Chernyatchik
 */
public class TextFieldWithAutoCompletionContributor<T> extends CompletionContributor {
  private static final Key<TextFieldWithAutoCompletionListProvider> KEY = Key.create("text field simple completion available");
  private static final Key<Boolean> AUTO_POPUP_KEY = Key.create("text Field simple completion auto-popup");

  public static <T> void installCompletion(Document document, Project project, @Nullable TextFieldWithAutoCompletionListProvider<T> consumer, boolean autoPopup) {
    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
    if (psiFile != null) {
      //noinspection unchecked
      psiFile.putUserData(KEY, consumer == null ? TextFieldWithAutoCompletion.EMPTY_COMPLETION : consumer);
      psiFile.putUserData(AUTO_POPUP_KEY, autoPopup);
    }
  }

  @RequiredReadAction
  @Override
  public void fillCompletionVariants(final CompletionParameters parameters, CompletionResultSet result) {
    PsiFile file = parameters.getOriginalFile();
    final TextFieldWithAutoCompletionListProvider<T> provider = file.getUserData(KEY);

    if (provider == null) {
      return;
    }
    String adv = provider.getAdvertisement();
    if (adv == null) {
      final String shortcut = getActionShortcut(IdeActions.ACTION_QUICK_JAVADOC);
      if (shortcut != null) {
        adv = provider.getQuickDocHotKeyAdvertisement(shortcut);
      }
    }
    if (adv != null) {
      result.addLookupAdvertisement(adv);
    }

    final String prefix = provider.getPrefix(parameters);
    if (prefix == null) {
      return;
    }
    if (parameters.getInvocationCount() == 0 && !file.getUserData(AUTO_POPUP_KEY)) {   // is autopopup
      return;
    }
    final PrefixMatcher prefixMatcher = provider.createPrefixMatcher(prefix);
    if (prefixMatcher != null) {
      result = result.withPrefixMatcher(prefixMatcher);
    }

    Collection<T> items = provider.getItems(prefix, true, parameters);
    addCompletionElements(result, provider, items, -10000);

    Future<Collection<T>> future = ApplicationManager.getApplication().executeOnPooledThread(new Callable<Collection<T>>() {
      @Override
      public Collection<T> call() {
        return provider.getItems(prefix, false, parameters);
      }
    });

    while (true) {
      try {
        Collection<T> tasks = future.get(100, TimeUnit.MILLISECONDS);
        if (tasks != null) {
          addCompletionElements(result, provider, tasks, 0);
          return;
        }
      }
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (Exception ignore) {

      }
      ProgressManager.checkCanceled();
    }
  }

  private static <T> void addCompletionElements(final CompletionResultSet result, final TextFieldWithAutoCompletionListProvider<T> listProvider, final Collection<T> items, final int index) {
    final AutoCompletionPolicy completionPolicy = ApplicationManager.getApplication().isUnitTestMode() ? AutoCompletionPolicy.ALWAYS_AUTOCOMPLETE : AutoCompletionPolicy.NEVER_AUTOCOMPLETE;
    int grouping = index;
    for (final T item : items) {
      LookupElementBuilder builder = listProvider.createLookupBuilder(item);

      result.addElement(PrioritizedLookupElement.withGrouping(builder.withAutoCompletionPolicy(completionPolicy), grouping--));
    }
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return PlainTextLanguage.INSTANCE;
  }
}
