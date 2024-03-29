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
package consulo.language.editor.folding;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNamedElement;
import consulo.util.lang.Comparing;
import consulo.util.lang.reflect.ReflectionUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.StringTokenizer;

/**
 * @author Denis Zhdanov
 * @since 11/7/11 12:00 PM
 */
public abstract class AbstractElementSignatureProvider implements ElementSignatureProvider {

  protected static final char ELEMENTS_SEPARATOR = ';';
  protected static final String ELEMENT_TOKENS_SEPARATOR = "#";

  @Override
  @Nullable
  public PsiElement restoreBySignature(@Nonnull PsiFile file, @Nonnull String signature, @Nullable StringBuilder processingInfoStorage) {
    int semicolonIndex = signature.indexOf(ELEMENTS_SEPARATOR);
    PsiElement parent;

    if (semicolonIndex >= 0) {
      String parentSignature = signature.substring(semicolonIndex + 1);
      if (processingInfoStorage != null) {
        processingInfoStorage.append(String.format(
          "Provider '%s'. Restoring parent by signature '%s'...%n", getClass().getName(), parentSignature
        ));
      }
      parent = restoreBySignature(file, parentSignature, processingInfoStorage);
      if (processingInfoStorage != null) {
        processingInfoStorage.append(String.format("Restored parent by signature '%s': %s%n", parentSignature, parent));
      }
      if (parent == null) return null;
      signature = signature.substring(0, semicolonIndex);
    }
    else {
      parent = file;
    }

    StringTokenizer tokenizer = new StringTokenizer(signature, ELEMENT_TOKENS_SEPARATOR);
    String type = tokenizer.nextToken();
    if (processingInfoStorage != null) {
      processingInfoStorage.append(String.format(
        "Provider '%s'. Restoring target element by signature '%s'. Parent: %s, same as the given parent: %b%n",
        getClass().getName(), signature, parent, parent == file
      ));
    }
    return restoreBySignatureTokens(file, parent, type, tokenizer, processingInfoStorage);
  }

  @Nullable
  protected abstract PsiElement restoreBySignatureTokens(@Nonnull PsiFile file,
                                                         @Nonnull PsiElement parent,
                                                         @Nonnull String type,
                                                         @Nonnull StringTokenizer tokenizer,
                                                         @Nullable StringBuilder processingInfoStorage);

  protected static <T extends PsiNamedElement> int getChildIndex(T element, PsiElement parent, String name, Class<T> hisClass) {
    PsiElement[] children = parent.getChildren();
    int index = 0;

    for (PsiElement child : children) {
      if (ReflectionUtil.isAssignable(hisClass, child.getClass())) {
        T namedChild = hisClass.cast(child);
        final String childName = namedChild.getName();

        if (Comparing.equal(name, childName)) {
          if (namedChild.equals(element)) {
            return index;
          }
          index++;
        }
      }
    }

    return index;
  }

  @Nullable
  protected static <T extends PsiNamedElement> T restoreElementInternal(@Nonnull PsiElement parent,
                                                                        String name,
                                                                        int index,
                                                                        @Nonnull Class<T> hisClass)
  {
    PsiElement[] children = parent.getChildren();

    for (PsiElement child : children) {
      if (ReflectionUtil.isAssignable(hisClass, child.getClass())) {
        T namedChild = hisClass.cast(child);
        final String childName = namedChild.getName();

        if (Comparing.equal(name, childName)) {
          if (index == 0) {
            return namedChild;
          }
          index--;
        }
      }
    }

    return null;
  }
}
