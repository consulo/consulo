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
package com.intellij.codeInsight.template.postfix.templates;

import com.google.common.collect.Sets;
import com.intellij.codeInsight.template.CustomLiveTemplateBase;
import com.intellij.codeInsight.template.CustomTemplateCallback;
import com.intellij.codeInsight.template.impl.CustomLiveTemplateLookupElement;
import com.intellij.codeInsight.template.impl.TemplateSettings;
import com.intellij.codeInsight.template.postfix.completion.PostfixTemplateLookupElement;
import com.intellij.codeInsight.template.postfix.settings.PostfixTemplatesSettings;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.undo.UndoConstants;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.containers.ContainerUtil;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Set;

public class PostfixLiveTemplate extends CustomLiveTemplateBase {
  public static final String POSTFIX_TEMPLATE_ID = "POSTFIX_TEMPLATE_ID";
  private static final Logger LOG = Logger.getInstance(PostfixLiveTemplate.class);

  @Nonnull
  public Set<String> getAllTemplateKeys(PsiFile file, int offset) {
    Set<String> keys = Sets.newHashSet();
    Language language = PsiUtilCore.getLanguageAtOffset(file, offset);
    for (PostfixTemplateProvider provider : LanguagePostfixTemplate.LANG_EP.allForLanguage(language)) {
      keys.addAll(getKeys(provider));
    }
    return keys;
  }

  @Nullable
  private static String computeTemplateKeyWithoutContextChecking(@Nonnull PostfixTemplateProvider provider,
                                                                 @Nonnull CharSequence documentContent,
                                                                 int currentOffset) {
    int startOffset = currentOffset;
    if (documentContent.length() < startOffset) {
      return null;
    }

    while (startOffset > 0) {
      char currentChar = documentContent.charAt(startOffset - 1);
      if (!Character.isJavaIdentifierPart(currentChar)) {
        if (!provider.isTerminalSymbol(currentChar)) {
          return null;
        }
        startOffset--;
        break;
      }
      startOffset--;
    }
    return String.valueOf(documentContent.subSequence(startOffset, currentOffset));
  }

  @Nullable
  @Override
  public String computeTemplateKey(@Nonnull CustomTemplateCallback callback) {
    Editor editor = callback.getEditor();
    CharSequence charsSequence = editor.getDocument().getCharsSequence();
    int offset = editor.getCaretModel().getOffset();
    for (PostfixTemplateProvider provider : LanguagePostfixTemplate.LANG_EP.allForLanguage(getLanguage(callback))) {
      String key = computeTemplateKeyWithoutContextChecking(provider, charsSequence, offset);
      if (key != null && isApplicableTemplate(provider, key, callback.getFile(), editor)) {
        return key;
      }
    }
    return null;
  }

  @Nullable
  @Override
  public String computeTemplateKeyWithoutContextChecking(@Nonnull CustomTemplateCallback callback) {
    Editor editor = callback.getEditor();
    int currentOffset = editor.getCaretModel().getOffset();
    for (PostfixTemplateProvider provider : LanguagePostfixTemplate.LANG_EP.allForLanguage(getLanguage(callback))) {
      String key = computeTemplateKeyWithoutContextChecking(provider, editor.getDocument().getCharsSequence(), currentOffset);
      if (key != null) return key;
    }
    return null;
  }

  @Override
  public boolean supportsMultiCaret() {
    return false;
  }

  @Override
  public void expand(@Nonnull final String key, @Nonnull final CustomTemplateCallback callback) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    FeatureUsageTracker.getInstance().triggerFeatureUsed("editing.completion.postfix");

    Editor editor = callback.getEditor();
    for (PostfixTemplateProvider provider : LanguagePostfixTemplate.LANG_EP.allForLanguage(getLanguage(callback))) {
      PostfixTemplate postfixTemplate = getTemplate(provider, key);
      if (postfixTemplate != null) {
        final PsiFile file = callback.getContext().getContainingFile();
        if (isApplicableTemplate(provider, key, file, editor)) {
          int offset = deleteTemplateKey(file, editor, key);
          try {
            provider.preExpand(file, editor);
            PsiElement context = CustomTemplateCallback.getContext(file, positiveOffset(offset));
            expandTemplate(postfixTemplate, editor, context);
          }
          finally {
            provider.afterExpand(file, editor);
          }
        }
        // don't care about errors in multiCaret mode
        else if (editor.getCaretModel().getAllCarets().size() == 1) {
          LOG.error("Template not found by key: " + key);
        }
        return;
      }
    }

    // don't care about errors in multiCaret mode
    if (editor.getCaretModel().getAllCarets().size() == 1) {
      LOG.error("Template not found by key: " + key);
    }
  }

  @Override
  public boolean isApplicable(PsiFile file, int offset, boolean wrapping) {
    PostfixTemplatesSettings settings = PostfixTemplatesSettings.getInstance();
    if (wrapping || file == null || settings == null || !settings.isPostfixTemplatesEnabled()) {
      return false;
    }
    Language language = PsiUtilCore.getLanguageAtOffset(file, offset);
    for (PostfixTemplateProvider provider : LanguagePostfixTemplate.LANG_EP.allForLanguage(language)) {
      if (StringUtil.isNotEmpty(computeTemplateKeyWithoutContextChecking(provider, file.getText(), offset + 1))) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean supportsWrapping() {
    return false;
  }

  @Override
  public void wrap(@Nonnull String selection, @Nonnull CustomTemplateCallback callback) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public String getTitle() {
    return "Postfix";
  }

  @Override
  public char getShortcut() {
    PostfixTemplatesSettings settings = PostfixTemplatesSettings.getInstance();
    return settings != null ? (char)settings.getShortcut() : TemplateSettings.TAB_CHAR;
  }

  @Override
  public boolean hasCompletionItem(@Nonnull PsiFile file, int offset) {
    return true;
  }

  @Nonnull
  @Override
  public Collection<? extends CustomLiveTemplateLookupElement> getLookupElements(@Nonnull PsiFile file,
                                                                                 @Nonnull Editor editor,
                                                                                 int offset) {
    Collection<CustomLiveTemplateLookupElement> result = ContainerUtil.newHashSet();
    CustomTemplateCallback callback = new CustomTemplateCallback(editor, file);
    for (PostfixTemplateProvider provider : LanguagePostfixTemplate.LANG_EP.allForLanguage(getLanguage(callback))) {
      String key = computeTemplateKeyWithoutContextChecking(callback);
      if (key != null && editor.getCaretModel().getCaretCount() == 1) {
        Condition<PostfixTemplate> isApplicationTemplateFunction = createIsApplicationTemplateFunction(provider, key, file, editor);
        for (PostfixTemplate postfixTemplate : provider.getTemplates()) {
          if (isApplicationTemplateFunction.value(postfixTemplate)) {
            result.add(new PostfixTemplateLookupElement(this, postfixTemplate, postfixTemplate.getKey(), provider, false));
          }
        }
      }
    }

    return result;
  }

  private static void expandTemplate(@Nonnull final PostfixTemplate template,
                                     @Nonnull final Editor editor,
                                     @Nonnull final PsiElement context) {
    if (template.startInWriteAction()) {
      ApplicationManager.getApplication().runWriteAction(() -> CommandProcessor.getInstance()
              .executeCommand(context.getProject(), () -> template.expand(context, editor), "Expand postfix template", POSTFIX_TEMPLATE_ID));
    }
    else {
      template.expand(context, editor);
    }
  }


  private static int deleteTemplateKey(@Nonnull final PsiFile file, @Nonnull final Editor editor, @Nonnull final String key) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    final int currentOffset = editor.getCaretModel().getOffset();
    final int newOffset = currentOffset - key.length();
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        CommandProcessor.getInstance().runUndoTransparentAction(new Runnable() {
          public void run() {
            Document document = editor.getDocument();
            document.deleteString(newOffset, currentOffset);
            editor.getCaretModel().moveToOffset(newOffset);
            PsiDocumentManager.getInstance(file.getProject()).commitDocument(document);
          }
        });
      }
    });
    return newOffset;
  }

  private static Condition<PostfixTemplate> createIsApplicationTemplateFunction(@Nonnull final PostfixTemplateProvider provider,
                                                                                @Nonnull String key,
                                                                                @Nonnull PsiFile file,
                                                                                @Nonnull Editor editor) {
    int currentOffset = editor.getCaretModel().getOffset();
    final int newOffset = currentOffset - key.length();
    CharSequence fileContent = editor.getDocument().getCharsSequence();
    StringBuilder fileContentWithoutKey = new StringBuilder();
    fileContentWithoutKey.append(fileContent.subSequence(0, newOffset));
    fileContentWithoutKey.append(fileContent.subSequence(currentOffset, fileContent.length()));
    PsiFile copyFile = copyFile(file, fileContentWithoutKey);
    Document copyDocument = copyFile.getViewProvider().getDocument();
    if (copyDocument == null) {
      //noinspection unchecked
      return Condition.FALSE;
    }

    copyFile = provider.preCheck(copyFile, editor, newOffset);
    copyDocument = copyFile.getViewProvider().getDocument();
    if (copyDocument == null) {
      //noinspection unchecked
      return Condition.FALSE;
    }

    final PsiElement context = CustomTemplateCallback.getContext(copyFile, positiveOffset(newOffset));
    final Document finalCopyDocument = copyDocument;
    return new Condition<PostfixTemplate>() {
      @Override
      public boolean value(PostfixTemplate template) {
        return template != null && template.isEnabled(provider) && template.isApplicable(context, finalCopyDocument, newOffset);
      }
    };
  }

  @Nonnull
  public static PsiFile copyFile(@Nonnull PsiFile file, @Nonnull StringBuilder fileContentWithoutKey) {
    final PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(file.getProject());
    PsiFile copy = psiFileFactory.createFileFromText(file.getName(), file.getFileType(), fileContentWithoutKey);
    VirtualFile vFile = copy.getVirtualFile();
    if (vFile != null) {
      vFile.putUserData(UndoConstants.DONT_RECORD_UNDO, Boolean.TRUE);
    }
    return copy;
  }

  public static boolean isApplicableTemplate(@Nonnull PostfixTemplateProvider provider,
                                             @Nonnull String key,
                                             @Nonnull PsiFile file,
                                             @Nonnull Editor editor) {
    return createIsApplicationTemplateFunction(provider, key, file, editor).value(getTemplate(provider, key));
  }

  @Nonnull
  private static Set<String> getKeys(@Nonnull PostfixTemplateProvider provider) {
    Set<String> result = ContainerUtil.newHashSet();
    for (PostfixTemplate template : provider.getTemplates()) {
      result.add(template.getKey());
    }

    return result;
  }

  @Nullable
  private static PostfixTemplate getTemplate(@Nonnull PostfixTemplateProvider provider, @Nullable String key) {
    for (PostfixTemplate template : provider.getTemplates()) {
      if (template.getKey().equals(key)) {
        return template;
      }
    }
    return null;
  }

  private static Language getLanguage(@Nonnull CustomTemplateCallback callback) {
    return callback.getContext().getLanguage();
  }

  private static int positiveOffset(int offset) {
    return offset > 0 ? offset - 1 : offset;
  }
}
