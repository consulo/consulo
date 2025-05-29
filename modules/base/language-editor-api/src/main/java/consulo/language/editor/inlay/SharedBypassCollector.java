// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package consulo.language.editor.inlay;

import consulo.language.psi.PsiElement;

public interface SharedBypassCollector extends InlayHintsCollector {
    /**
     * Collects inlays for a given element.
     */
    void collectFromElement(PsiElement element, InlayTreeSink sink);

    /**
     * Version which is invoked from intention, and which is supposed to run faster.
     */
    default void collectFromElementForActions(PsiElement element, InlayTreeSink sink) {
        collectFromElement(element, sink);
    }
}