/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.lang.documentation;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.containers.ContainerUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CompositeDocumentationProvider extends DocumentationProviderEx implements ExternalDocumentationProvider, ExternalDocumentationHandler {

  private final List<DocumentationProvider> myProviders;

  public static DocumentationProvider wrapProviders(Collection<DocumentationProvider> providers) {
    ArrayList<DocumentationProvider> list = new ArrayList<DocumentationProvider>();
    for (DocumentationProvider provider : providers) {
      if (provider instanceof CompositeDocumentationProvider) {
        list.addAll(((CompositeDocumentationProvider)provider).getProviders());
      }
      else if (provider != null) {
        list.add(provider);
      }
    }
    // CompositeDocumentationProvider should be returned anyway because it
    // handles DocumentationProvider.EP as well as providers from the list
    return new CompositeDocumentationProvider(Collections.unmodifiableList(list));
  }

  private CompositeDocumentationProvider(List<DocumentationProvider> providers) {
    myProviders = providers;
  }

  @Nonnull
  public List<DocumentationProvider> getAllProviders() {
    return ContainerUtil.concat(getProviders(), EP_NAME.getExtensionList());
  }

  @Nonnull
  public List<DocumentationProvider> getProviders() {
    return myProviders;
  }

  @Override
  public boolean handleExternal(PsiElement element, PsiElement originalElement) {
    for (DocumentationProvider provider : getAllProviders()) {
      if (provider instanceof ExternalDocumentationHandler &&
          ((ExternalDocumentationHandler)provider).handleExternal(element, originalElement)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean handleExternalLink(PsiManager psiManager, String link, PsiElement context) {
    for (DocumentationProvider provider : getAllProviders()) {
      if (provider instanceof ExternalDocumentationHandler &&
          ((ExternalDocumentationHandler)provider).handleExternalLink(psiManager, link, context)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean canFetchDocumentationLink(String link) {
    for (DocumentationProvider provider : getAllProviders()) {
      if (provider instanceof ExternalDocumentationHandler && ((ExternalDocumentationHandler)provider).canFetchDocumentationLink(link)) {
        return true;
      }
    }

    return false;
  }

  @Nonnull
  @Override
  public String fetchExternalDocumentation(@Nonnull String link, @javax.annotation.Nullable PsiElement element) {
    for (DocumentationProvider provider : getAllProviders()) {
      if (provider instanceof ExternalDocumentationHandler && ((ExternalDocumentationHandler)provider).canFetchDocumentationLink(link)) {
        return ((ExternalDocumentationHandler)provider).fetchExternalDocumentation(link, element);
      }
    }

    throw new IllegalStateException("Unable to find a provider to fetch documentation link!");
  }

  @Override
  public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
    for (DocumentationProvider provider : getAllProviders()) {
      String result = provider.getQuickNavigateInfo(element, originalElement);
      if (result != null) return result;
    }
    return null;
  }

  @Override
  public List<String> getUrlFor(PsiElement element, PsiElement originalElement) {
    for (DocumentationProvider provider : getAllProviders()) {
      List<String> result = provider.getUrlFor(element, originalElement);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Override
  public String generateHoverDoc(@Nonnull PsiElement element, @Nullable PsiElement originalElement) {
    for (DocumentationProvider provider : getAllProviders()) {
      String result = provider.generateHoverDoc(element, originalElement);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Override
  public String generateDoc(PsiElement element, PsiElement originalElement) {
    for (DocumentationProvider provider : getAllProviders()) {
      String result = provider.generateDoc(element, originalElement);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Override
  public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
    for (DocumentationProvider provider : getAllProviders()) {
      PsiElement result = provider.getDocumentationElementForLookupItem(psiManager, object, element);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Override
  public PsiElement getDocumentationElementForLink(PsiManager psiManager, String link, PsiElement context) {
    for (DocumentationProvider provider : getAllProviders()) {
      PsiElement result = provider.getDocumentationElementForLink(psiManager, link, context);
      if (result != null) return result;
    }
    return null;
  }


  @javax.annotation.Nullable
  public CodeDocumentationProvider getFirstCodeDocumentationProvider() {
    for (DocumentationProvider provider : getAllProviders()) {
      if (provider instanceof CodeDocumentationProvider) {
        return (CodeDocumentationProvider)provider;
      }
    }
    return null;
  }

  @Override
  public String fetchExternalDocumentation(Project project, PsiElement element, List<String> docUrls) {
    for (DocumentationProvider provider : getAllProviders()) {
      if (provider instanceof ExternalDocumentationProvider) {
        final String doc = ((ExternalDocumentationProvider)provider).fetchExternalDocumentation(project, element, docUrls);
        if (doc != null) {
          return doc;
        }
      }
    }
    return null;
  }

  @Override
  public boolean hasDocumentationFor(PsiElement element, PsiElement originalElement) {
    for (DocumentationProvider provider : getAllProviders()) {
      if (provider instanceof ExternalDocumentationProvider) {
        if (((ExternalDocumentationProvider)provider).hasDocumentationFor(element, originalElement)) return true;
      }
      else {
        if (hasUrlsFor(provider, element, originalElement)) return true;
      }
    }
    return false;
  }

  @Override
  public boolean canPromptToConfigureDocumentation(PsiElement element) {
    for (DocumentationProvider provider : getAllProviders()) {
      if (provider instanceof ExternalDocumentationProvider &&
          ((ExternalDocumentationProvider)provider).canPromptToConfigureDocumentation(element)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void promptToConfigureDocumentation(PsiElement element) {
    for (DocumentationProvider provider : getAllProviders()) {
      if (provider instanceof ExternalDocumentationProvider &&
          ((ExternalDocumentationProvider)provider).canPromptToConfigureDocumentation(element)) {
        ((ExternalDocumentationProvider)provider).promptToConfigureDocumentation(element);
        break;
      }
    }
  }

  public static boolean hasUrlsFor(DocumentationProvider provider, PsiElement element, PsiElement originalElement) {
    final List<String> urls = provider.getUrlFor(element, originalElement);
    if (urls != null && !urls.isEmpty()) return true;
    return false;
  }

  @javax.annotation.Nullable
  @Override
  public PsiElement getCustomDocumentationElement(@Nonnull Editor editor,
                                                  @Nonnull PsiFile file,
                                                  @javax.annotation.Nullable PsiElement contextElement) {
    for (DocumentationProvider provider : getAllProviders()) {
      if (provider instanceof DocumentationProviderEx) {
        PsiElement element = ((DocumentationProviderEx)provider).getCustomDocumentationElement(editor, file, contextElement);
        if (element != null) {
          return element;
        }
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return getProviders().toString();
  }
}
