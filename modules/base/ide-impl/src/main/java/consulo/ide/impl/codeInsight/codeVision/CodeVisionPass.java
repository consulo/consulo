// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.codeInsight.codeVision;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.Editor;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.InlayModel;
import consulo.codeEditor.InlayProperties;
import consulo.document.util.TextRange;
import consulo.language.editor.codeVision.CodeVisionEntry;
import consulo.language.editor.codeVision.DaemonBoundCodeVisionProvider;
import consulo.language.editor.codeVision.TextCodeVisionEntry;
import consulo.language.editor.impl.highlight.EditorBoundHighlightingPass;
import consulo.language.psi.PsiFile;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Highlighting pass that collects code-vision lenses from all registered
 * {@link DaemonBoundCodeVisionProvider} extensions and renders them as block inlays
 * above the corresponding lines in the editor.
 */
public class CodeVisionPass extends EditorBoundHighlightingPass {
    private final List<LensData> myLenses = new ArrayList<>();

    public CodeVisionPass(PsiFile psiFile, Editor editor) {
        super(editor, psiFile, false);
    }

    @Override
    @RequiredReadAction
    public void doCollectInformation(ProgressIndicator progress) {
        myLenses.clear();
        List<DaemonBoundCodeVisionProvider> providers = DaemonBoundCodeVisionProvider.EP_NAME.getExtensionList();
        for (DaemonBoundCodeVisionProvider provider : providers) {
            if (progress.isCanceled()) break;
            List<Pair<TextRange, CodeVisionEntry>> results = provider.computeForEditor(myEditor, myFile);
            for (Pair<TextRange, CodeVisionEntry> pair : results) {
                myLenses.add(new LensData(provider, pair.getFirst(), pair.getSecond()));
            }
        }
    }

    @Override
    @RequiredUIAccess
    public void doApplyInformationToEditor() {
        InlayModel inlayModel = myEditor.getInlayModel();

        // Remove all existing code vision block inlays for this editor
        List<Inlay<? extends CodeVisionBlockInlayRenderer>> existing =
            inlayModel.getBlockElementsInRange(0, myEditor.getDocument().getTextLength(), CodeVisionBlockInlayRenderer.class);
        for (Inlay<?> inlay : existing) {
            inlay.dispose();
        }

        // Add new inlays
        for (LensData lens : myLenses) {
            String text = lens.entry() instanceof TextCodeVisionEntry textEntry
                ? textEntry.text
                : lens.entry().longPresentation;

            CodeVisionBlockInlayRenderer renderer =
                new CodeVisionBlockInlayRenderer(text, lens.provider(), lens.range(), lens.entry());

            int offset = lens.range().getStartOffset();
            inlayModel.addBlockElement(
                offset,
                new InlayProperties().showAbove(true).relatesToPrecedingText(false),
                renderer
            );
        }
    }

    private record LensData(DaemonBoundCodeVisionProvider provider, TextRange range, CodeVisionEntry entry) {
    }
}
