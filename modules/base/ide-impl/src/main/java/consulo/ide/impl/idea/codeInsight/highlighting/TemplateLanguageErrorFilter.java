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
package consulo.ide.impl.idea.codeInsight.highlighting;

import consulo.language.Language;
import consulo.language.editor.HighlightErrorFilter;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.util.dataholder.Key;
import consulo.language.ast.TokenSet;
import jakarta.annotation.Nonnull;

import java.util.*;

/**
 * @author Dennis.Ushakov
 */
public abstract class TemplateLanguageErrorFilter extends HighlightErrorFilter {
  @Nonnull
  private final TokenSet myTemplateExpressionStartTokens;
  @Nonnull
  private final Class myTemplateFileViewProviderClass;

  private final Set<Language> knownLanguageSet;
  
  private final static Key<Class> TEMPLATE_VIEW_PROVIDER_CLASS_KEY = Key.create("TEMPLATE_VIEW_PROVIDER_CLASS");

  protected TemplateLanguageErrorFilter(
    @Nonnull TokenSet templateExpressionStartTokens,
    @Nonnull Class templateFileViewProviderClass)
  {
    this(templateExpressionStartTokens, templateFileViewProviderClass, new String[0]);
  }

  protected TemplateLanguageErrorFilter(
    @Nonnull TokenSet templateExpressionStartTokens,
    @Nonnull Class templateFileViewProviderClass,
    @Nonnull String... knownSubLanguageNames)
  {
    myTemplateExpressionStartTokens = TokenSet.create(templateExpressionStartTokens.getTypes());
    myTemplateFileViewProviderClass = templateFileViewProviderClass;

    List<String> knownSubLanguageList = new ArrayList<String>(Arrays.asList(knownSubLanguageNames));
    knownSubLanguageList.add("JavaScript");
    knownSubLanguageList.add("CSS");
    knownLanguageSet = new HashSet<Language>();
    for (String name : knownSubLanguageList) {
      Language language = Language.findLanguageByID(name);
      if (language != null) {
        knownLanguageSet.add(language);
      }
    }
  }

  @Override
  public boolean shouldHighlightErrorElement(@Nonnull PsiErrorElement element) {
    if (isKnownSubLanguage(element.getParent().getLanguage())) {
      //
      // Immediately discard filters with non-matching template class if already known
      //
      Class templateClass = element.getUserData(TEMPLATE_VIEW_PROVIDER_CLASS_KEY);
      if (templateClass != null && (templateClass != myTemplateFileViewProviderClass)) return true;
      
      PsiFile psiFile = element.getContainingFile();
      int offset = element.getTextOffset();
      InjectedLanguageManager injectedLanguageManager = InjectedLanguageManager.getInstance(element.getProject());
      if (injectedLanguageManager.isInjectedFragment(psiFile)) {
        PsiElement host = injectedLanguageManager.getInjectionHost(element);
        if (host != null) {
          psiFile = host.getContainingFile();
          offset = injectedLanguageManager.injectedToHost(element, offset);
        }
      }
      FileViewProvider viewProvider = psiFile.getViewProvider();
      element.putUserData(TEMPLATE_VIEW_PROVIDER_CLASS_KEY, viewProvider.getClass());
      if (!(viewProvider.getClass() == myTemplateFileViewProviderClass)) {
        return true;
      }
      //
      // An error can occur at template element or before it. Check both.
      //
      if (shouldIgnoreErrorAt(viewProvider, offset) || shouldIgnoreErrorAt(viewProvider, offset + 1)) return false;
    }
    return true;
  }
  
  private boolean shouldIgnoreErrorAt(@Nonnull FileViewProvider viewProvider, int offset) {
    PsiElement element = viewProvider.findElementAt(offset, viewProvider.getBaseLanguage());
    if (element instanceof PsiWhiteSpace) element = element.getNextSibling();
    if (element != null && myTemplateExpressionStartTokens.contains(element.getNode().getElementType())) {
      return true;
    }
    return false;
  }
  
  protected boolean isKnownSubLanguage(@Nonnull Language language) {
    for (Language knownLanguage : knownLanguageSet) {
      if (language.is(knownLanguage)) {
        return true;
      }
    }
    return false;
  }
}
