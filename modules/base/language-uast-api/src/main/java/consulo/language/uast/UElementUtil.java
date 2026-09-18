// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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

import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Top-level helpers of {@code UElement.kt} and the generic tree-walking helpers of {@code UastUtils.kt}
 * that reference only the core {@link UElement} vocabulary.
 */
public final class UElementUtil {
    public static final UElement[] EMPTY_ARRAY = new UElement[0];

    private UElementUtil() {
    }

    public static @Nullable PsiElement getSourcePsiElement(@Nullable UElement element) {
        return element == null ? null : element.getSourcePsi();
    }

    /**
     * Returns a stream including this element and its containing elements.
     */
    public static Stream<UElement> withContainingElements(UElement element) {
        return Stream.iterate(element, Objects::nonNull, UElement::getUastParent);
    }

    public static <T extends UElement> @Nullable T getParentOfType(UElement element, Class<? extends T> parentClass) {
        return getParentOfType(element, parentClass, true);
    }

    @SuppressWarnings("unchecked")
    public static <T extends UElement> @Nullable T getParentOfType(UElement element, Class<? extends T> parentClass, boolean strict) {
        UElement current = strict ? element.getUastParent() : element;
        while (current != null) {
            if (parentClass.isInstance(current)) {
                return (T) current;
            }
            current = current.getUastParent();
        }
        return null;
    }

    @SafeVarargs
    public static @Nullable UElement skipParentOfType(UElement element, boolean strict, Class<? extends UElement>... parentClasses) {
        UElement current = strict ? element.getUastParent() : element;
        while (current != null) {
            if (!isInstanceOfAny(current, parentClasses)) {
                return current;
            }
            current = current.getUastParent();
        }
        return null;
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T extends UElement> @Nullable T getParentOfType(
        UElement element,
        Class<? extends T> parentClass,
        boolean strict,
        Class<? extends UElement>... terminators
    ) {
        UElement current = strict ? element.getUastParent() : element;
        while (current != null) {
            if (parentClass.isInstance(current)) {
                return (T) current;
            }
            if (isInstanceOfAny(current, terminators)) {
                return null;
            }
            current = current.getUastParent();
        }
        return null;
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T extends UElement> @Nullable T getParentOfType(
        UElement element,
        boolean strict,
        Class<? extends T> firstParentClass,
        Class<? extends T>... parentClasses
    ) {
        UElement current = strict ? element.getUastParent() : element;
        while (current != null) {
            if (firstParentClass.isInstance(current)) {
                return (T) current;
            }
            if (isInstanceOfAny(current, parentClasses)) {
                return (T) current;
            }
            current = current.getUastParent();
        }
        return null;
    }

    public static @Nullable UVariable getContainingUVariable(UElement element) {
        return getParentOfType(element, UVariable.class);
    }

    public static boolean isUastChildOf(UElement element, @Nullable UElement probablyParent) {
        return isUastChildOf(element, probablyParent, false);
    }

    public static boolean isUastChildOf(UElement element, @Nullable UElement probablyParent, boolean strict) {
        if (probablyParent == null) {
            return false;
        }
        UElement current = strict ? element.getUastParent() : element;
        while (current != null) {
            if (current.equals(probablyParent)) {
                return true;
            }
            current = current.getUastParent();
        }
        return false;
    }

    private static boolean isInstanceOfAny(UElement element, Class<? extends UElement>[] classes) {
        for (Class<? extends UElement> clazz : classes) {
            if (clazz.isInstance(element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A helper function for getting the parent of a given {@link PsiElement} that could be considered as identifier.
     * Useful for working with gutter according to recommendations in {@code LineMarkerProvider}: line markers should be
     * anchored on leaf elements, so a provider is offered the identifier and asks for the declaration it names.
     */
    public static @Nullable UElement getUParentForIdentifier(PsiElement identifier) {
        UIdentifier uIdentifier = UastFacade.toUElement(identifier, UIdentifier.class);
        return uIdentifier == null ? null : uIdentifier.getUastParent();
    }
}
