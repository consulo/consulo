// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.codeInsight.codeVision;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.Editor;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.InlayModel;
import consulo.codeEditor.InlayProperties;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.ide.impl.codeInsight.codeVision.ui.model.CodeVisionListData;
import consulo.ide.impl.codeInsight.codeVision.ui.model.RangeCodeVisionModel;
import consulo.ide.impl.codeInsight.codeVision.ui.renderers.BlockCodeVisionInlayRenderer;
import consulo.ide.impl.codeInsight.codeVision.ui.renderers.CodeVisionInlayRendererBase;
import consulo.ide.impl.codeInsight.codeVision.ui.renderers.InlineCodeVisionInlayRenderer;
import consulo.language.editor.codeVision.CodeVisionAnchorKind;
import consulo.language.editor.codeVision.CodeVisionEntry;
import consulo.language.editor.codeVision.CodeVisionSettings;
import consulo.language.editor.codeVision.DaemonBoundCodeVisionProvider;
import consulo.language.editor.impl.highlight.EditorBoundHighlightingPass;
import consulo.language.psi.PsiFile;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.Pair;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Highlighting pass that collects code-vision lenses from all registered
 * {@link DaemonBoundCodeVisionProvider} extensions and renders them as inlays.
 * <p>
 * Position is resolved per-provider group:
 * <ol>
 *   <li>Per-group override from {@link CodeVisionSettings#getPositionForGroup}</li>
 *   <li>Provider's own {@link DaemonBoundCodeVisionProvider#getDefaultAnchor()}</li>
 *   <li>Global default from {@link CodeVisionSettings#getDefaultPosition()}</li>
 * </ol>
 * {@link CodeVisionAnchorKind#Top} → block inlay above the declaration line.<br>
 * {@link CodeVisionAnchorKind#Right} → after-line-end inlay appended to the line.
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
        CodeVisionSettings settings = CodeVisionSettings.getInstance();
        if (!settings.isCodeVisionEnabled()) return;

        for (DaemonBoundCodeVisionProvider provider : DaemonBoundCodeVisionProvider.EP_NAME.getExtensionList()) {
            if (progress.isCanceled()) break;
            // Sync with JB: filter by groupId, not individual provider id
            if (!settings.isProviderEnabled(provider.getGroupId())) continue;

            CodeVisionAnchorKind anchor = resolveAnchor(provider, settings);
            List<Pair<TextRange, CodeVisionEntry>> results = provider.computeForEditor(myEditor, myFile);
            for (Pair<TextRange, CodeVisionEntry> pair : results) {
                myLenses.add(new LensData(pair.getFirst(), pair.getSecond(), anchor));
            }
        }
    }

    @Override
    @RequiredUIAccess
    public void doApplyInformationToEditor() {
        InlayModel inlayModel = myEditor.getInlayModel();

        // Remove all existing code vision block inlays
        for (Inlay<?> inlay : inlayModel.getBlockElementsInRange(0,
            myEditor.getDocument().getTextLength(), BlockCodeVisionInlayRenderer.class)) {
            inlay.dispose();
        }

        // Remove all existing code vision after-line-end inlays
        for (Inlay<?> inlay : inlayModel.getAfterLineEndElementsInRange(0,
            myEditor.getDocument().getTextLength(), InlineCodeVisionInlayRenderer.class)) {
            inlay.dispose();
        }

        // Group entries by (startOffset, anchor)
        Map<OffsetAndAnchor, List<LensData>> byKey = new LinkedHashMap<>();
        for (LensData lens : myLenses) {
            OffsetAndAnchor key = new OffsetAndAnchor(lens.range().getStartOffset(), lens.anchor());
            byKey.computeIfAbsent(key, k -> new ArrayList<>()).add(lens);
        }

        Document document = myEditor.getDocument();

        for (Map.Entry<OffsetAndAnchor, List<LensData>> e : byKey.entrySet()) {
            OffsetAndAnchor key = e.getKey();
            List<LensData> group = e.getValue();
            int offset = key.offset();
            TextRange range = group.get(0).range();
            List<CodeVisionEntry> entries = new ArrayList<>();
            for (LensData ld : group) {
                entries.add(ld.entry());
            }

            if (key.anchor() == CodeVisionAnchorKind.Top) {
                addBlockInlay(inlayModel, offset, range, entries);
            }
            else {
                // Right (or any other — default Right behaviour like JB)
                int lineNumber = document.getLineNumber(offset);
                int lineEndOffset = document.getLineEndOffset(lineNumber);
                addAfterLineEndInlay(inlayModel, lineEndOffset, range, entries);
            }
        }

        CodeVisionPassFactory.updateModificationStamp(myEditor, myProject);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves the effective anchor for a provider, matching JB's priority chain:
     * per-group settings override → provider default → global default.
     */
    private static CodeVisionAnchorKind resolveAnchor(DaemonBoundCodeVisionProvider provider,
                                                       CodeVisionSettings settings) {
        // 1. Per-group position set by the user in Settings
        CodeVisionAnchorKind groupPos = settings.getPositionForGroup(provider.getGroupId());
        if (groupPos != null && groupPos != CodeVisionAnchorKind.Default) {
            return groupPos;
        }
        // 2. Provider's declared default
        CodeVisionAnchorKind providerDefault = provider.getDefaultAnchor();
        if (providerDefault != null && providerDefault != CodeVisionAnchorKind.Default) {
            return providerDefault;
        }
        // 3. Global default position from settings
        return settings.getDefaultPosition();
    }

    private void addBlockInlay(InlayModel inlayModel, int offset, TextRange range,
                                List<CodeVisionEntry> entries) {
        BlockCodeVisionInlayRenderer renderer = new BlockCodeVisionInlayRenderer();
        Inlay<BlockCodeVisionInlayRenderer> inlay = inlayModel.addBlockElement(
            offset,
            new InlayProperties().showAbove(true).relatesToPrecedingText(false),
            renderer
        );
        if (inlay != null) {
            initInlay(inlay, entries, range, CodeVisionAnchorKind.Top, renderer);
        }
    }

    private void addAfterLineEndInlay(InlayModel inlayModel, int lineEndOffset, TextRange range,
                                       List<CodeVisionEntry> entries) {
        InlineCodeVisionInlayRenderer renderer = new InlineCodeVisionInlayRenderer();
        Inlay<InlineCodeVisionInlayRenderer> inlay = inlayModel.addAfterLineEndElement(
            lineEndOffset,
            false,
            renderer
        );
        if (inlay != null) {
            initInlay(inlay, entries, range, CodeVisionAnchorKind.Right, renderer);
        }
    }

    private <T extends CodeVisionInlayRendererBase> void initInlay(Inlay<T> inlay,
                                                                     List<CodeVisionEntry> entries,
                                                                     TextRange range,
                                                                     CodeVisionAnchorKind anchor,
                                                                     T renderer) {
        Map<CodeVisionAnchorKind, List<CodeVisionEntry>> lensMap = Map.of(anchor, entries);
        RangeCodeVisionModel rangeModel = new RangeCodeVisionModel(
            myEditor.getProject(), myEditor, lensMap, range, ""
        );
        CodeVisionListData listData = new CodeVisionListData(rangeModel, inlay, entries, anchor);
        inlay.putUserData(CodeVisionListData.KEY, listData);
        renderer.initialize(inlay);
    }

    // -------------------------------------------------------------------------
    // Data records
    // -------------------------------------------------------------------------

    private record LensData(TextRange range, CodeVisionEntry entry, CodeVisionAnchorKind anchor) {
    }

    private record OffsetAndAnchor(int offset, CodeVisionAnchorKind anchor) {
    }
}
