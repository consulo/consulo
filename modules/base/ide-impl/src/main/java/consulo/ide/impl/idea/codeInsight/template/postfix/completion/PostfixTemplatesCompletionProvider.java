// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.template.postfix.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.application.util.matcher.PrefixMatcher;
import consulo.language.editor.template.CustomTemplateCallback;
import consulo.ide.impl.idea.codeInsight.template.impl.LiveTemplateCompletionContributor;
import consulo.language.editor.postfixTemplate.PostfixTemplatesSettings;
import consulo.ide.impl.idea.codeInsight.template.postfix.templates.PostfixLiveTemplate;
import consulo.codeEditor.Editor;
import consulo.application.progress.ProgressManager;
import consulo.language.pattern.StandardPatterns;
import consulo.language.psi.PsiFile;
import consulo.language.util.ProcessingContext;
import consulo.language.editor.completion.CompletionProvider;
import jakarta.annotation.Nonnull;

import static consulo.ide.impl.idea.codeInsight.template.postfix.completion.PostfixTemplateCompletionContributor.getPostfixLiveTemplate;

class PostfixTemplatesCompletionProvider implements CompletionProvider {
  @Override
  public void addCompletions(@Nonnull CompletionParameters parameters, @Nonnull ProcessingContext context, @Nonnull CompletionResultSet result) {
    Editor editor = parameters.getEditor();
    if (!isCompletionEnabled(parameters) || LiveTemplateCompletionContributor.shouldShowAllTemplates() || editor.getCaretModel().getCaretCount() != 1) {
      /*
        disabled or covered with {@link consulo.ide.impl.idea.codeInsight.template.impl.LiveTemplateCompletionContributor}
       */
      return;
    }

    PsiFile originalFile = parameters.getOriginalFile();
    PostfixLiveTemplate postfixLiveTemplate = getPostfixLiveTemplate(originalFile, editor);
    if (postfixLiveTemplate != null) {
      postfixLiveTemplate.addCompletions(parameters, result.withPrefixMatcher(new MyPrefixMatcher(result.getPrefixMatcher().getPrefix())));
      String possibleKey = postfixLiveTemplate.computeTemplateKeyWithoutContextChecking(new CustomTemplateCallback(editor, originalFile));
      if (possibleKey != null) {
        result = result.withPrefixMatcher(possibleKey);
        result.restartCompletionOnPrefixChange(StandardPatterns.string().oneOf(postfixLiveTemplate.getAllTemplateKeys(originalFile, parameters.getOffset())));
      }
    }
  }

  private static boolean isCompletionEnabled(@Nonnull CompletionParameters parameters) {
    ProgressManager.checkCanceled();
    if (!parameters.isAutoPopup()) {
      return false;
    }

    PostfixTemplatesSettings settings = PostfixTemplatesSettings.getInstance();
    if (!settings.isPostfixTemplatesEnabled() || !settings.isTemplatesCompletionEnabled()) {
      return false;
    }

    return true;
  }

  private static class MyPrefixMatcher extends PrefixMatcher {
    protected MyPrefixMatcher(String prefix) {
      super(prefix);
    }

    @Override
    public boolean prefixMatches(@Nonnull String name) {
      return name.equalsIgnoreCase(myPrefix);
    }

    @Nonnull
    @Override
    public PrefixMatcher cloneWithPrefix(@Nonnull String prefix) {
      return new MyPrefixMatcher(prefix);
    }
  }
}
