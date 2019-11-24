/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.codeInsight.template.postfix.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.template.CustomLiveTemplate;
import com.intellij.codeInsight.template.CustomTemplateCallback;
import com.intellij.codeInsight.template.impl.LiveTemplateCompletionContributor;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.postfix.settings.PostfixTemplatesSettings;
import com.intellij.codeInsight.template.postfix.templates.PostfixLiveTemplate;
import com.intellij.openapi.editor.Editor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.annotation.access.RequiredReadAction;
import consulo.codeInsight.completion.CompletionProvider;

public class PostfixTemplateCompletionContributor extends CompletionContributor {
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

  public PostfixTemplateCompletionContributor() {
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider() {
      @RequiredReadAction
      @Override
      public void addCompletions(@Nonnull CompletionParameters parameters, ProcessingContext context, @Nonnull CompletionResultSet result) {
        Editor editor = parameters.getEditor();
        if (!isCompletionEnabled(parameters) || LiveTemplateCompletionContributor.shouldShowAllTemplates() ||
            editor.getCaretModel().getCaretCount() != 1) {
          /**
           * disabled or covered with {@link com.intellij.codeInsight.template.impl.LiveTemplateCompletionContributor}
           */
          return;
        }

        PsiFile originalFile = parameters.getOriginalFile();
        PostfixLiveTemplate postfixLiveTemplate = PostfixTemplateCompletionContributor.getPostfixLiveTemplate(originalFile, editor);
        if (postfixLiveTemplate != null) {
          postfixLiveTemplate.addCompletions(parameters, result.withPrefixMatcher(new MyPrefixMatcher(result.getPrefixMatcher().getPrefix())));
          String possibleKey = postfixLiveTemplate.computeTemplateKeyWithoutContextChecking(new CustomTemplateCallback(editor, originalFile));
          if (possibleKey != null) {
            result = result.withPrefixMatcher(possibleKey);
            result.restartCompletionOnPrefixChange(
                    StandardPatterns.string().oneOf(postfixLiveTemplate.getAllTemplateKeys(originalFile, parameters.getOffset())));
          }
        }
      }
    });
  }

  private static boolean isCompletionEnabled(@Nonnull CompletionParameters parameters) {
    if (!parameters.isAutoPopup()) {
      return false;
    }

    PostfixTemplatesSettings settings = PostfixTemplatesSettings.getInstance();
    return settings.isPostfixTemplatesEnabled() && settings.isTemplatesCompletionEnabled();
  }

  @Nullable
  public static PostfixLiveTemplate getPostfixLiveTemplate(@Nonnull PsiFile file, @Nonnull Editor editor) {
    PostfixLiveTemplate postfixLiveTemplate = CustomLiveTemplate.EP_NAME.findExtension(PostfixLiveTemplate.class);
    return postfixLiveTemplate != null && TemplateManagerImpl.isApplicable(postfixLiveTemplate, editor, file) ? postfixLiveTemplate : null;
  }
}
