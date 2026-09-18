// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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

import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.uast.util.ClassSet;
import org.jspecify.annotations.Nullable;

/**
 * The main entry point to uast-conversions.
 * <p>
 * In the most cases you could use {@link #toUElement(PsiElement)} or {@link #toUElementOfExpectedTypes(PsiElement, Class[])}
 * instead of asking {@link UastLanguagePlugin#byLanguage(Language)} directly.
 */
public final class UastFacade {
    public static final Class<? extends UElement>[] DEFAULT_TYPES_LIST = defaultTypesList();

    public static final Class<? extends UExpression>[] DEFAULT_EXPRESSION_TYPES_LIST = defaultExpressionTypesList();

    private UastFacade() {
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends UElement>[] defaultTypesList() {
        return new Class[]{UElement.class};
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends UExpression>[] defaultExpressionTypesList() {
        return new Class[]{UExpression.class};
    }

    private static @Nullable UElement convertElementWithParent(PsiElement element, @Nullable Class<? extends UElement> requiredType) {
        if (element instanceof PsiWhiteSpace) {
            return null;
        }
        UastLanguagePlugin plugin = UastLanguagePlugin.byLanguage(element.getLanguage());
        return plugin == null ? null : plugin.convertElementWithParent(element, requiredType);
    }

    /**
     * Converts the element to UAST.
     */
    public static @Nullable UElement toUElement(@Nullable PsiElement element) {
        return element == null ? null : convertElementWithParent(element, null);
    }

    /**
     * Converts the element to an UAST element of the given type. Returns null if the PSI element type does not correspond
     * to the given UAST element type.
     */
    @SuppressWarnings("unchecked")
    public static <T extends UElement> @Nullable T toUElement(@Nullable PsiElement element, Class<? extends T> cls) {
        return element == null ? null : (T) convertElementWithParent(element, cls);
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public static <T extends UElement> @Nullable T toUElementOfExpectedTypes(@Nullable PsiElement element, Class<? extends T>... classes) {
        if (element == null) {
            return null;
        }
        if (classes.length == 0) {
            return (T) convertElementWithParent(element, UElement.class);
        }
        else if (classes.length == 1) {
            return (T) convertElementWithParent(element, classes[0]);
        }
        else {
            UastLanguagePlugin plugin = UastLanguagePlugin.byLanguage(element.getLanguage());
            return plugin == null ? null : plugin.convertElementWithParent(element, classes);
        }
    }

    /**
     * Finds an UAST element of a given type at the given {@code offset} in the specified file. Returns null if there is no UAST
     * element of the given type at the given offset.
     */
    @SuppressWarnings("unchecked")
    public static <T extends UElement> @Nullable T findUElementAt(PsiFile file, int offset, Class<? extends T> cls) {
        PsiElement element = file.findElementAt(offset);
        if (element == null) {
            return null;
        }
        UElement uElement = toUElement(element);
        if (uElement == null) {
            return null;
        }
        return (T) UElementUtil.withContainingElements(uElement).filter(cls::isInstance).findFirst().orElse(null);
    }

    /**
     * Finds an UAST element of the given type among the parents of the given PSI element.
     */
    @SuppressWarnings("unchecked")
    public static <T extends UElement> @Nullable T getUastParentOfType(@Nullable PsiElement element,
                                                                        Class<? extends T> cls,
                                                                        boolean strict) {
        if (element == null) {
            return null;
        }
        UElement firstUElement = getFirstUElement(element, strict);
        if (firstUElement == null) {
            return null;
        }

        return (T) UElementUtil.withContainingElements(firstUElement).filter(cls::isInstance).findFirst().orElse(null);
    }

    /**
     * Finds an UAST element of the given type among the parents of the given PSI element.
     */
    public static <T extends UElement> @Nullable T getUastParentOfType(@Nullable PsiElement element, Class<? extends T> cls) {
        return getUastParentOfType(element, cls, false);
    }

    /**
     * Finds an UAST element of any given type among the parents of the given PSI element.
     */
    public static @Nullable UElement getUastParentOfTypes(@Nullable PsiElement element,
                                                          Class<? extends UElement>[] classes,
                                                          boolean strict) {
        if (element == null) {
            return null;
        }
        UElement firstUElement = getFirstUElement(element, strict);
        if (firstUElement == null) {
            return null;
        }

        return UElementUtil.withContainingElements(firstUElement).filter(uElement -> {
            for (Class<? extends UElement> cls : classes) {
                if (cls.isInstance(uElement)) {
                    return true;
                }
            }
            return false;
        }).findFirst().orElse(null);
    }

    /**
     * Finds an UAST element of any given type among the parents of the given PSI element.
     */
    public static @Nullable UElement getUastParentOfTypes(@Nullable PsiElement element, Class<? extends UElement>[] classes) {
        return getUastParentOfTypes(element, classes, false);
    }

    /**
     * @return types of possible source PSI elements of {@code language}, which instances in principle
     * can be converted to at least one of the specified {@code uastTypes}
     * (or to {@link UElement} if no type was specified)
     * @see UastLanguagePlugin#getPossiblePsiSourceTypes(Class[])
     */
    @SafeVarargs
    public static ClassSet<PsiElement> getPossiblePsiSourceTypes(Language language, Class<? extends UElement>... uastTypes) {
        UastLanguagePlugin plugin = UastLanguagePlugin.byLanguage(language);
        return plugin == null ? ClassSet.emptyClassSet() : plugin.getPossiblePsiSourceTypes(uastTypes);
    }

    public static @Nullable UElement getFirstUElement(PsiElement psiElement, boolean strict) {
        PsiElement startingElement = strict ? psiElement.getParent() : psiElement;
        for (PsiElement current = startingElement; current != null; current = current.getParent()) {
            UElement uElement = toUElement(current);
            if (uElement != null) {
                return uElement;
            }
        }
        return null;
    }

    public static @Nullable UElement getFirstUElement(PsiElement psiElement) {
        return getFirstUElement(psiElement, false);
    }
}
