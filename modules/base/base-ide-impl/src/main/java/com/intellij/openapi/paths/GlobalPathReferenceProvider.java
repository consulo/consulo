/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.intellij.openapi.paths;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.util.io.URLUtil;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class GlobalPathReferenceProvider implements PathReferenceProvider {

  @NonNls private static final String[] PREFIXES = {"tel:", "sms:", "skype:", "data:", "xmpp:"};

  public static boolean startsWithAllowedPrefix(String s) {
    for (String prefix : PREFIXES) {
      if (s.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean createReferences(@Nonnull PsiElement psiElement, final @Nonnull List<PsiReference> references, final boolean soft) {
    final ElementManipulator<PsiElement> manipulator = ElementManipulators.getManipulator(psiElement);
    if (manipulator == null) {
      return false;
    }
    return createUrlReference(psiElement, manipulator.getRangeInElement(psiElement).substring(psiElement.getText()), manipulator.getRangeInElement(psiElement),
                              references);
  }

  public boolean createUrlReference(@Nonnull PsiElement psiElement, String url, TextRange rangeInElement, @Nonnull List<PsiReference> references) {
    if (isWebReferenceUrl(url)) {
      references.add(new WebReference(psiElement, rangeInElement, url));
      return true;
    }
    else if (url.contains("://") || url.startsWith("//") || startsWithAllowedPrefix(url)) {
      references.add(PsiReferenceBase.createSelfReference(psiElement, rangeInElement, psiElement));
      return true;
    }
    return false;
  }

  public static boolean isWebReferenceUrl(String url) {
    return url.startsWith("http://") || url.startsWith("https://") || url.startsWith("about:") || url.startsWith("mailto:");
  }

  @Override
  @Nullable
  public PathReference getPathReference(@Nonnull String path, @Nonnull final PsiElement element) {
    return URLUtil.containsScheme(path) ? new PathReference(path, PathReference.NULL_ICON) : null;
  }
}
