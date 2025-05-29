// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.dumb.DumbAware;
import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.Editor;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.InlayProperties;
import consulo.codeEditor.impl.EditorScrollingPositionKeeper;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.util.DocumentUtil;
import consulo.language.editor.impl.highlight.EditorBoundHighlightingPass;
import consulo.language.editor.inlay.*;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SyntaxTraverser;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.SmartList;
import consulo.util.lang.TriConsumer;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public class DeclarativeInlayHintsPass extends EditorBoundHighlightingPass implements DumbAware {
    private final PsiElement rootElement;
    private final Editor editor;
    private final List<InlayProviderPassInfo> providerInfos;
    private final boolean isPreview;
    private final boolean isProviderDisabled;
    private PreprocessedInlayData preprocessedInlayData = PreprocessedInlayData.EMPTY;

    public DeclarativeInlayHintsPass(PsiElement rootElement,
                                     Editor editor,
                                     List<InlayProviderPassInfo> providerInfos,
                                     boolean isPreview) {
        this(rootElement, editor, providerInfos, isPreview, false);
    }

    public DeclarativeInlayHintsPass(PsiElement rootElement,
                                     Editor editor,
                                     List<InlayProviderPassInfo> providerInfos,
                                     boolean isPreview,
                                     boolean isProviderDisabled) {
        super(editor, rootElement.getContainingFile(), true);
        this.rootElement = rootElement;
        this.editor = editor;
        this.providerInfos = providerInfos;
        this.isPreview = isPreview;
        this.isProviderDisabled = isProviderDisabled;
    }

    @Override
    public void doCollectInformation(ProgressIndicator progress) {
        List<CollectionInfo<OwnBypassCollector>> ownCollectors = new ArrayList<>();
        List<CollectionInfo<SharedBypassCollector>> sharedCollectors = new ArrayList<>();
        List<InlayTreeSinkImpl> sinks = new ArrayList<>();
        for (InlayProviderPassInfo providerInfo : providerInfos) {
            InlayHintsProvider provider = providerInfo.getProvider();
            if (DumbService.isDumb(myProject) && !(provider instanceof DumbAware)) {
                continue;
            }
            InlayTreeSinkImpl sink = new InlayTreeSinkImpl(
                providerInfo.getProviderId(),
                providerInfo.getOptionToEnabled(),
                isPreview,
                isProviderDisabled,
                provider.getClass(),
                passSourceId
            );
            sinks.add(sink);
            InlayHintsCollector collector = provider.createCollector(myFile, editor);
            if (collector instanceof OwnBypassCollector) {
                ownCollectors.add(new CollectionInfo<>(sink, (OwnBypassCollector) collector));
            }
            else if (collector instanceof SharedBypassCollector) {
                sharedCollectors.add(new CollectionInfo<>(sink, (SharedBypassCollector) collector));
            }
        }
        for (CollectionInfo<OwnBypassCollector> info : ownCollectors) {
            info.collector.collectHintsForFile(myFile, info.sink);
        }
        for (PsiElement element : SyntaxTraverser.psiTraverser(rootElement)) {
            for (CollectionInfo<SharedBypassCollector> info : sharedCollectors) {
                info.collector.collectFromElement(element, info.sink);
            }
        }
        List<InlayData> allInlayData = new ArrayList<>();
        for (InlayTreeSinkImpl sink : sinks) {
            allInlayData.addAll(sink.finish());
        }
        preprocessedInlayData = preprocessCollectedInlayData(allInlayData, editor.getDocument());
    }

    @Override
    public void doApplyInformationToEditor() {
        EditorScrollingPositionKeeper positionKeeper = new EditorScrollingPositionKeeper(editor);
        positionKeeper.savePosition();
        applyInlayData(editor, myProject, preprocessedInlayData, passSourceId);
        positionKeeper.restorePosition(false);
    }

    private static class CollectionInfo<T> {
        final InlayTreeSinkImpl sink;
        final T collector;

        CollectionInfo(InlayTreeSinkImpl sink, T collector) {
            this.sink = sink;
            this.collector = collector;
        }
    }

    public static class AboveLineIndentedPositionDetail {
        private final int line;
        private final InlayData inlayData;

        public AboveLineIndentedPositionDetail(int line, InlayData inlayData) {
            this.line = line;
            this.inlayData = inlayData;
        }

        public InlayPosition.AboveLineIndentedPosition getAboveLineIndentedPosition() {
            if (inlayData.getPosition() instanceof InlayPosition.AboveLineIndentedPosition) {
                return (InlayPosition.AboveLineIndentedPosition) inlayData.getPosition();
            }
            else {
                throw new IllegalStateException("Expected AboveLineIndentedPosition, got " + inlayData.getPosition());
            }
        }

        public int getLine() {
            return line;
        }

        public InlayData getInlayData() {
            return inlayData;
        }
    }

    public static class PreprocessedInlayData {
        public final List<InlayData> inline;
        public final List<InlayData> endOfLine;
        public final List<AboveLineIndentedPositionDetail> aboveLine;
        public static final PreprocessedInlayData EMPTY =
            new PreprocessedInlayData(Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList());

        public PreprocessedInlayData(List<InlayData> inline,
                                     List<InlayData> endOfLine,
                                     List<AboveLineIndentedPositionDetail> aboveLine) {
            this.inline = inline;
            this.endOfLine = endOfLine;
            this.aboveLine = aboveLine;
        }
    }

    public static final String passSourceId = DeclarativeInlayHintsPass.class.getName();

    @RequiredUIAccess
    public static void applyInlayData(Editor editor,
                                      Project project,
                                      PreprocessedInlayData preprocessedInlayData,
                                      String sourceId) {
        var inlayModel = editor.getInlayModel();
        Document document = editor.getDocument();

        Int2ObjectOpenHashMap<SmartList<Inlay<? extends DeclarativeInlayRenderer>>> offsetToExistingInlineInlays =
            groupRelevantExistingInlays(
                sourceId,
                inlayModel.getInlineElementsInRange(0, document.getTextLength(), DeclarativeInlayRenderer.class),
                inlay -> inlay.getOffset()
            );

        Int2ObjectOpenHashMap<SmartList<Inlay<? extends DeclarativeInlayRenderer>>> offsetToExistingEolInlays =
            groupRelevantExistingInlays(
                sourceId,
                inlayModel.getAfterLineEndElementsInRange(0, document.getTextLength(), DeclarativeInlayRenderer.class),
                inlay -> inlay.getOffset()
            );

        var offsetToExistingBlockInlays =
            groupRelevantExistingInlays(
                sourceId,
                inlayModel.getBlockElementsInRange(0, document.getTextLength(), DeclarativeIndentedBlockInlayRenderer.class),
                inlay -> document.getLineNumber(inlay.getOffset())
            );

        var storage = InlayHintsUtils.getTextMetricStorage(editor);
        DeclarativeInlayUpdateListener publisher = project.getMessageBus().syncPublisher(DeclarativeInlayUpdateListener.TOPIC);

        for (InlayData inlayData : preprocessedInlayData.endOfLine) {
            if (!sourceId.equals(inlayData.getSourceId())) {
                throw new IllegalStateException("Inconsistent sourceId=" + sourceId + ", inlayData=" + inlayData);
            }
            InlayPosition.EndOfLinePosition position = (InlayPosition.EndOfLinePosition) inlayData.getPosition();
            int lineEndOffset = document.getLineEndOffset(position.getLine());
            boolean updated = tryUpdateInlayAndRemoveFromDeleteList(
                offsetToExistingEolInlays,
                inlayData,
                lineEndOffset,
                inlay -> inlay.getRenderer().getProviderId().equals(inlayData.getProviderId()),
                (inlay, oldModel, newData) -> publisher.afterModelUpdate(inlay, oldModel, List.of(newData))
            );
            if (!updated) {
                DeclarativeInlayRenderer renderer = new DeclarativeInlayRenderer(inlayData, storage, inlayData.getProviderId(), sourceId);
                InlayProperties properties = new InlayProperties()
                    .priority(position.getPriority())
                    .relatesToPrecedingText(true)
                    .disableSoftWrapping(false);
                var inlay = inlayModel.addAfterLineEndElement(lineEndOffset, properties, renderer);
                if (inlay != null) {
                    renderer.initInlay(inlay);
                }
            }
        }

        for (InlayData inlayData : preprocessedInlayData.inline) {
            if (!sourceId.equals(inlayData.getSourceId())) {
                throw new IllegalStateException("Inconsistent sourceId=" + sourceId + ", inlayData=" + inlayData);
            }
            InlayPosition.InlineInlayPosition position = (InlayPosition.InlineInlayPosition) inlayData.getPosition();
            boolean updated = tryUpdateInlayAndRemoveFromDeleteList(
                offsetToExistingInlineInlays,
                inlayData,
                position.getOffset(),
                inlay -> inlay.getRenderer().getProviderId().equals(inlayData.getProviderId()) &&
                    inlay.isRelatedToPrecedingText() == position.isRelatedToPrevious(),
                (inlay, oldModel, newModel) -> publisher.afterModelUpdate(inlay, oldModel, List.of(newModel))
            );
            if (!updated) {
                DeclarativeInlayRenderer renderer = new DeclarativeInlayRenderer(inlayData, storage, inlayData.getProviderId(), sourceId);
                var inlay = inlayModel.addInlineElement(position.getOffset(), position.isRelatedToPrevious(), position.getPriority(), renderer);
                if (inlay != null) {
                    renderer.initInlay(inlay);
                }
            }
        }

        forEachRun(preprocessedInlayData.aboveLine, (line, providerId, verticalPriority, inlayData) -> {
            for (InlayData data : inlayData) {
                if (!sourceId.equals(data.getSourceId())) {
                    throw new IllegalStateException("Inconsistent sourceId=" + sourceId + ", inlayData=" + data);
                }
            }
            boolean updated = tryUpdateInlayAndRemoveFromDeleteList(
                offsetToExistingBlockInlays,
                inlayData,
                line,
                inlay -> inlay.getRenderer().getProviderId().equals(providerId) &&
                    inlay.getProperties().getPriority() == verticalPriority,
                publisher::afterModelUpdate
            );

            if (!updated) {
                DeclarativeIndentedBlockInlayRenderer renderer = new DeclarativeIndentedBlockInlayRenderer(
                    inlayData, storage, providerId, sourceId,
                    DocumentUtil.getLineStartIndentedOffset(document, line)
                );
                int offset = inlayData.stream()
                    .mapToInt(d -> ((InlayPosition.AboveLineIndentedPosition) d.getPosition()).getOffset())
                    .min().orElseThrow();
                var inlay = inlayModel.addBlockElement(offset, false, true, verticalPriority, renderer);
                if (inlay != null) {
                    renderer.initInlay(inlay);
                }
            }
        });

        deleteNotPreservedInlays(offsetToExistingInlineInlays);
        deleteNotPreservedInlays(offsetToExistingEolInlays);
        deleteNotPreservedInlays(offsetToExistingBlockInlays);

        DeclarativeInlayHintsPassFactory.updateModificationStamp(editor, project);
    }

    @FunctionalInterface
    private interface RunAction {
        void apply(int line, String providerId, int verticalPriority, List<InlayData> inlayData);
    }

    private static void forEachRun(List<AboveLineIndentedPositionDetail> details, @RequiredUIAccess RunAction action) {
        if (details.isEmpty()) {
            return;
        }

        AboveLineIndentedPositionDetail initial = details.get(0);
        int line = initial.getLine();
        String providerId = initial.getInlayData().getProviderId();
        int verticalPriority = initial.getAboveLineIndentedPosition().getVerticalPriority();
        int runStart = 0;
        int runEnd = 1;

        for (int index = 1; index < details.size(); index++) {
            AboveLineIndentedPositionDetail item = details.get(index);
            boolean newRun = item.getLine() != line
                || !item.getInlayData().getProviderId().equals(providerId)
                || item.getAboveLineIndentedPosition().getVerticalPriority() != verticalPriority;
            if (newRun) {
                action.apply(
                    line,
                    providerId,
                    verticalPriority,
                    details.subList(runStart, index)
                        .stream()
                        .map(AboveLineIndentedPositionDetail::getInlayData)
                        .collect(Collectors.toList())
                );
                line = item.getLine();
                providerId = item.getInlayData().getProviderId();
                verticalPriority = item.getAboveLineIndentedPosition().getVerticalPriority();
                runStart = index;
            }
            runEnd = index + 1;
        }
        if (runStart < runEnd) {
            action.apply(
                line,
                providerId,
                verticalPriority,
                details.subList(runStart, runEnd)
                    .stream()
                    .map(AboveLineIndentedPositionDetail::getInlayData)
                    .collect(Collectors.toList())
            );
        }
    }

    private static void deleteNotPreservedInlays(Int2ObjectOpenHashMap<? extends SmartList<? extends Inlay<?>>> offsetToExistingInlays) {
        for (SmartList<? extends Inlay<?>> inlays : offsetToExistingInlays.values()) {
            for (Inlay<?> inlay : inlays) {
                Disposer.dispose(inlay);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @RequiredUIAccess
    private static <M> boolean tryUpdateInlayAndRemoveFromDeleteList(
        Int2ObjectOpenHashMap<? extends SmartList<? extends Inlay<? extends DeclarativeInlayRendererBase<M>>>> offsetToExistingInlays,
        M inlayData,
        int groupKey,
        @RequiredUIAccess Predicate<Inlay<? extends DeclarativeInlayRendererBase<M>>> require,
        @RequiredUIAccess TriConsumer<Inlay<? extends DeclarativeInlayRendererBase<M>>, List<InlayData>, M> afterModelUpdate
    ) {
        SmartList<? extends Inlay<? extends DeclarativeInlayRendererBase<M>>> inlays = offsetToExistingInlays.get(groupKey);
        if (inlays == null) {
            return false;
        }
        Iterator<? extends Inlay<? extends DeclarativeInlayRendererBase<M>>> iterator = inlays.iterator();
        while (iterator.hasNext()) {
            Inlay<? extends DeclarativeInlayRendererBase<M>> existingInlay = iterator.next();
            if (require.test(existingInlay)) {
                List<InlayData> oldInlayData = existingInlay.getRenderer().toInlayData(false);
                existingInlay.getRenderer().updateModel(inlayData);
                afterModelUpdate.accept(existingInlay, oldInlayData, inlayData);
                existingInlay.update();
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    @RequiredReadAction
    public static PreprocessedInlayData preprocessCollectedInlayData(List<InlayData> inlayData, Document document) {
        List<InlayData> inlineData = new ArrayList<>();
        List<InlayData> eolData = new ArrayList<>();
        List<AboveLineIndentedPositionDetail> aboveLineData = new ArrayList<>();
        for (InlayData data : inlayData) {
            if (data.getPosition() instanceof InlayPosition.AboveLineIndentedPosition) {
                int line = document.getLineNumber(((InlayPosition.AboveLineIndentedPosition) data.getPosition()).getOffset());
                aboveLineData.add(new AboveLineIndentedPositionDetail(line, data));
            }
            else if (data.getPosition() instanceof InlayPosition.EndOfLinePosition) {
                eolData.add(data);
            }
            else if (data.getPosition() instanceof InlayPosition.InlineInlayPosition) {
                inlineData.add(data);
            }
        }
        aboveLineData.sort(Comparator
            .comparingInt(AboveLineIndentedPositionDetail::getLine)
            .thenComparing(detail -> detail.getInlayData().getProviderId())
            .thenComparingInt(detail -> detail.getAboveLineIndentedPosition().getVerticalPriority())
            .thenComparing((d1, d2) -> Integer.compare(d2.getAboveLineIndentedPosition().getPriority(),
                d1.getAboveLineIndentedPosition().getPriority()))
        );
        return new PreprocessedInlayData(inlineData, eolData, aboveLineData);
    }

    private static <Rend extends DeclarativeInlayRendererBase<?>> Int2ObjectOpenHashMap<SmartList<Inlay<? extends Rend>>> groupRelevantExistingInlays(
        String sourceId,
        List<Inlay<? extends Rend>> inlays,
        ToIntFunction<Inlay<?>> groupKey
    ) {
        Int2ObjectOpenHashMap<SmartList<Inlay<? extends Rend>>> grouped = new Int2ObjectOpenHashMap<>();
        for (Inlay<? extends Rend> inlay : inlays) {
            if (!sourceId.equals(inlay.getRenderer().getSourceId())) {
                continue;
            }
            int key = groupKey.applyAsInt(inlay);
            grouped.computeIfAbsent(key, k -> new SmartList<>()).add(inlay);
        }
        return grouped;
    }
}
