// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.folding.impl;

import consulo.language.editor.folding.ElementSignatureProvider;
import consulo.language.editor.folding.FoldingBuilder;
import consulo.language.editor.folding.FoldingDescriptor;
import consulo.logging.Logger;
import consulo.application.dumb.IndexNotReadyException;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class FoldingPolicy {
    private static final Logger LOG = Logger.getInstance(FoldingPolicy.class);

    private static final GenericElementSignatureProvider GENERIC_PROVIDER = new GenericElementSignatureProvider();

    private FoldingPolicy() {
    }

    static boolean isCollapsedByDefault(@Nonnull FoldingDescriptor foldingDescriptor, @Nonnull FoldingBuilder foldingBuilder) {
        try {
            return foldingBuilder.isCollapsedByDefault(foldingDescriptor);
        }
        catch (IndexNotReadyException e) {
            LOG.error(e);
            return false;
        }
    }

    @Nullable
    public static String getSignature(@Nonnull PsiElement element) {
        for (ElementSignatureProvider provider : ElementSignatureProvider.EP_NAME.getExtensionList()) {
            String signature = provider.getSignature(element);
            if (signature != null) {
                return signature;
            }
        }
        return GENERIC_PROVIDER.getSignature(element);
    }

    @Nullable
    public static PsiElement restoreBySignature(@Nonnull PsiFile file, @Nonnull String signature) {
        return restoreBySignature(file, signature, null);
    }

    /**
     * Tries to restore target PSI element from the given file by the given signature.
     *
     * @param file                  target PSI file
     * @param signature             target element's signature
     * @param processingInfoStorage buffer used for tracing 'restore element' processing (if necessary)
     * @return PSI element from the given PSI file that corresponds to the given signature (if found)
     * {@code null} otherwise
     */
    @Nullable
    public static PsiElement restoreBySignature(
        @Nonnull PsiFile file,
        @Nonnull String signature,
        @Nullable StringBuilder processingInfoStorage
    ) {
        for (ElementSignatureProvider provider : ElementSignatureProvider.EP_NAME.getExtensionList()) {
            PsiElement result = provider.restoreBySignature(file, signature, processingInfoStorage);
            if (result != null) {
                return result;
            }
        }
        return GENERIC_PROVIDER.restoreBySignature(file, signature, processingInfoStorage);
    }
}
