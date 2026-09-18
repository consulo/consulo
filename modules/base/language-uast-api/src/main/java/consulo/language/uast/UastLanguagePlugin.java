// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.uast;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.psi.PsiElement;
import consulo.language.uast.util.ClassSet;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Extension to provide UAST (Unified Abstract Syntax Tree) language support. UAST is an abstraction layer on PSI of different
 * languages. It provides a unified API for working with common language elements like classes and method declarations, literal values and
 * control flow operators.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface UastLanguagePlugin extends LanguageExtension {
    ExtensionPointCacheKey<UastLanguagePlugin, ByLanguageValue<UastLanguagePlugin>> KEY =
        ExtensionPointCacheKey.create("UastLanguagePlugin", LanguageOneToOne.build());

    static @Nullable UastLanguagePlugin byLanguage(Language language) {
        return Application.get().getExtensionPoint(UastLanguagePlugin.class).getOrBuildCache(KEY).get(language);
    }

    /**
     * Converts a PSI element, the parent of which already has an UAST representation, to UAST.
     *
     * @param element      the element to convert
     * @param parent       the parent as an UAST element, or null if the element is a file
     * @param requiredType the expected type of the result.
     * @return the converted element, or null if the element isn't supported or doesn't match the required result type.
     */
    @Nullable UElement convertElement(PsiElement element, @Nullable UElement parent, @Nullable Class<? extends UElement> requiredType);

    /**
     * Converts a PSI element, along with its chain of parents, to UAST.
     *
     * @param element      the element to convert
     * @param requiredType the expected type of the result.
     * @return the converted element, or null if the element isn't supported or doesn't match the required result type.
     */
    @Nullable UElement convertElementWithParent(PsiElement element, @Nullable Class<? extends UElement> requiredType);

    @SuppressWarnings("unchecked")
    default <T extends UElement> @Nullable T convertElementWithParent(PsiElement element, Class<? extends T>[] requiredTypes) {
        UElement result;
        if (requiredTypes.length == 0) {
            result = convertElementWithParent(element, (Class<? extends UElement>) null);
        }
        else if (requiredTypes.length == 1) {
            result = convertElementWithParent(element, requiredTypes[0]);
        }
        else {
            result = convertElementWithParent(element, (Class<? extends UElement>) null);
            if (result != null) {
                boolean matches = false;
                for (Class<? extends T> requiredType : requiredTypes) {
                    if (requiredType.isAssignableFrom(result.getClass())) {
                        matches = true;
                        break;
                    }
                }
                if (!matches) {
                    result = null;
                }
            }
        }
        return (T) result;
    }

    default <T extends UElement> List<T> convertToAlternatives(PsiElement element, Class<? extends T>[] requiredTypes) {
        T result = convertElementWithParent(element, requiredTypes);
        return result == null ? List.of() : List.of(result);
    }

    /**
     * Serves for optimization purposes. Helps to filter PSI elements which in principle
     * can be sources for UAST types of an interest.
     * <p>
     * Note: it is already used inside {@link UastLanguagePlugin} conversion methods implementations
     * for Java, Kotlin and Scala.
     *
     * @return types of possible source PSI elements, which instances in principle
     * can be converted to at least one of the specified {@code uastTypes}
     * (or to {@link UElement} if no type was specified)
     */
    @SuppressWarnings("unchecked")
    default ClassSet<PsiElement> getPossiblePsiSourceTypes(Class<? extends UElement>... uastTypes) {
        return ClassSet.classSetOf(PsiElement.class);
    }
}
