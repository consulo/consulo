/*
 * Copyright 2013-2023 consulo.io
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
package consulo.language.editor.internal;

import consulo.language.Language;
import consulo.language.editor.documentation.CompositeDocumentationProvider;
import consulo.language.editor.documentation.DocumentationProvider;
import consulo.language.editor.documentation.LanguageDocumentationProvider;
import consulo.language.psi.*;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 15/01/2023
 */
public class DocumentationManagerHelper {
  public static final Key<String> SELECTED_QUICK_DOC_TEXT = Key.create("QUICK_DOC.SELECTED_TEXT");
  public static final Key<SmartPsiElementPointer<?>> ORIGINAL_ELEMENT_KEY = Key.create("Original element");

  public static final String JAVADOC_LOCATION_AND_SIZE = "javadoc.popup";
  public static final String NEW_JAVADOC_LOCATION_AND_SIZE = "javadoc.popup.new";

  @Nonnull
  public static DocumentationProvider getProviderFromElement(PsiElement element) {
    return getProviderFromElement(element, null);
  }

  public static void storeOriginalElement(Project project, PsiElement originalElement, PsiElement element) {
    if (element == null) return;
    try {
      element.putUserData(ORIGINAL_ELEMENT_KEY, SmartPointerManager.getInstance(project).createSmartPsiElementPointer(originalElement));
    }
    catch (RuntimeException ex) {
      // PsiPackage does not allow putUserData
    }
  }

  @Nonnull
  public static DocumentationProvider getProviderFromElement(@Nullable PsiElement element, @Nullable PsiElement originalElement) {
    if (element != null && !element.isValid()) {
      element = null;
    }
    if (originalElement != null && !originalElement.isValid()) {
      originalElement = null;
    }

    if (originalElement == null) {
      originalElement = getOriginalElement(element);
    }

    PsiFile containingFile = originalElement != null ? originalElement.getContainingFile() : element != null ? element.getContainingFile() : null;
    Set<DocumentationProvider> result = new LinkedHashSet<>();

    Language containingFileLanguage = containingFile != null ? containingFile.getLanguage() : null;
    DocumentationProvider originalProvider = containingFile != null ? LanguageDocumentationProvider.forLanguageComposite(containingFileLanguage) : null;

    Language elementLanguage = element != null ? element.getLanguage() : null;
    DocumentationProvider elementProvider = element == null || elementLanguage.is(containingFileLanguage) ? null : LanguageDocumentationProvider.forLanguageComposite(elementLanguage);

    ContainerUtil.addIfNotNull(result, elementProvider);
    ContainerUtil.addIfNotNull(result, originalProvider);

    if (containingFile != null) {
      Language baseLanguage = containingFile.getViewProvider().getBaseLanguage();
      if (!baseLanguage.is(containingFileLanguage)) {
        ContainerUtil.addIfNotNull(result, LanguageDocumentationProvider.forLanguageComposite(baseLanguage));
      }
    }
    else if (element instanceof PsiDirectory) {
      Set<Language> set = new HashSet<>();

      for (PsiFile file : ((PsiDirectory)element).getFiles()) {
        Language baseLanguage = file.getViewProvider().getBaseLanguage();
        if (!set.contains(baseLanguage)) {
          set.add(baseLanguage);
          ContainerUtil.addIfNotNull(result, LanguageDocumentationProvider.forLanguageComposite(baseLanguage));
        }
      }
    }
    return CompositeDocumentationProvider.wrapProviders(result);
  }

  @Nullable
  public static PsiElement getOriginalElement(PsiElement element) {
    SmartPsiElementPointer originalElementPointer = element != null ? element.getUserData(ORIGINAL_ELEMENT_KEY) : null;
    return originalElementPointer != null ? originalElementPointer.getElement() : null;
  }
}
