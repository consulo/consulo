// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.language.editor.inlay.InlayPresentation;
import consulo.language.editor.inlay.InlayPresentationFactory;
import consulo.language.editor.inlay.RootInlayPresentation;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.SyntaxTraverser;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.dataholder.Key;

import java.awt.*;
import java.util.function.Supplier;

public class InlayHintsUtils {
    private InlayHintsUtils() {
    }

    public static void fireContentChanged(InlayPresentation presentation) {
        presentation.fireContentChanged(new Rectangle(presentation.getWidth(), presentation.getHeight()));
    }

    public static void fireUpdateEvent(InlayPresentation presentation, Dimension previousDimension) {
        Dimension current = new Dimension(presentation.getWidth(), presentation.getHeight());
        if (!current.equals(previousDimension)) {
            presentation.fireSizeChanged(previousDimension, current);
        }
        presentation.fireContentChanged(new Rectangle(presentation.getWidth(), presentation.getHeight()));
    }

    public static Dimension dimension(InlayPresentation presentation) {
        return new Dimension(presentation.getWidth(), presentation.getHeight());
    }

    private static <Content> boolean updateIfSame(RootInlayPresentation<Content> oldRoot,
                                                  RootInlayPresentation<?> newRoot,
                                                  Editor editor,
                                                  InlayPresentationFactory factory) {
        if (!oldRoot.getKey().equals(newRoot.getKey())) {
            return false;
        }
        //noinspection unchecked
        return oldRoot.update((Content) newRoot.getContent(), editor, factory);
    }

    @RequiredReadAction
    public static TextRange getTextRangeWithoutLeadingCommentsAndWhitespaces(PsiElement element) {
        PsiElement start = element;
        for (PsiElement child : SyntaxTraverser.psiApi().children(element)) {
            if (!(child instanceof PsiComment) && !(child instanceof PsiWhiteSpace)) {
                start = child;
                break;
            }
        }
        return TextRange.create(start.getTextRange().getStartOffset(),
            element.getTextRange().getEndOffset());
    }

    @RequiredReadAction
    public static boolean isFirstInLine(PsiElement element) {
        PsiElement prev = PsiTreeUtil.prevLeaf(element, true);
        if (prev == null) {
            return true;
        }
        while (prev instanceof PsiWhiteSpace) {
            String text = prev.getText();
            if (text.contains("\n") || prev.getTextRange().getStartOffset() == 0) {
                return true;
            }
            prev = PsiTreeUtil.prevLeaf(prev, true);
        }
        return false;
    }

    private static final Key<InlayTextMetricsStorage> TEXT_METRICS_STORAGE =
        Key.create("InlayTextMetricsStorage");

    public static InlayTextMetricsStorage getTextMetricStorage(Editor editor) {
        InlayTextMetricsStorage storage = editor.getUserData(TEXT_METRICS_STORAGE);
        if (storage == null) {
            storage = new InlayTextMetricsStorage(editor);
            editor.putUserData(TEXT_METRICS_STORAGE, storage);
        }
        return storage;
    }

    /**
     * Runs the given computation under a read action if one is not already held.
     * <p>
     * For {@link consulo.language.editor.codeVision.DaemonBoundCodeVisionProvider#computeForEditor},
     * this is always called under {@link RequiredReadAction}, so the computation is invoked directly.
     * This method exists as a utility for non-daemon-bound providers that need to acquire read access
     * themselves before processing PSI.
     */
    public static <T> T computeCodeVisionUnderReadAction(Supplier<T> computable) {
        Application app = Application.get();
        if (app.isReadAccessAllowed()) {
            return computable.get();
        }
        return app.runReadAction(computable);
    }
}
