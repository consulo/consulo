// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.psi;

import consulo.language.psi.path.PsiDynaReference;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class PsiReferenceUtil {
    private PsiReferenceUtil() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends PsiReference> @Nullable T findReferenceOfClass(PsiReference ref, Class<T> clazz) {
        if (clazz.isInstance(ref)) {
            return (T) ref;
        }
        if (ref instanceof PsiMultiReference multiReference) {
            for (PsiReference reference : multiReference.getReferences()) {
                if (clazz.isInstance(reference)) {
                    return (T) reference;
                }
            }
        }
        if (ref instanceof PsiDynaReference<?> dynaReference) {
            for (PsiReference reference : dynaReference.getReferences()) {
                if (clazz.isInstance(reference)) {
                    return (T) reference;
                }
            }
        }
        return null;
    }

    public static List<PsiReference> unwrapMultiReference(PsiReference maybeMultiReference) {
        if (maybeMultiReference instanceof PsiMultiReference multiReference) {
            return List.of(multiReference.getReferences());
        }
        return List.of(maybeMultiReference);
    }
}
