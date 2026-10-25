// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.diff.DiffContext;
import consulo.diff.DiffExtension;
import consulo.diff.DiffTool;
import consulo.diff.DiffToolType;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.FrameDiffTool;
import consulo.diff.combined.CombinedBlockId;
import consulo.diff.combined.CombinedBlockProducer;
import consulo.diff.combined.CombinedDiffComponentProcessor;
import consulo.diff.combined.CombinedDiffKeys;
import consulo.diff.impl.internal.DiffRequestProcessor;
import consulo.diff.impl.internal.ErrorDiffTool;
import consulo.diff.internal.DiffImplUtil;
import consulo.diff.internal.DiffManagerEx;
import consulo.diff.internal.DiffSettingsHolder;
import consulo.diff.internal.DiffUserDataKeysEx;
import consulo.diff.request.DiffRequest;
import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditorState;
import consulo.fileEditor.FileEditorStateLevel;
import consulo.fileEditor.text.TextEditorProvider;
import consulo.fileEditor.text.TextEditorState;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CombinedDiffComponentProcessorImpl implements CombinedDiffComponentProcessor {
    private static final Logger LOG = Logger.getInstance(CombinedDiffComponentProcessor.class);

    private final CombinedDiffModel myModel;

    private final CombinedDiffMainUI myMainUi;

    private @Nullable CombinedDiffViewer myCombinedViewer;

    private boolean myDisposed;

    @RequiredUIAccess
    public CombinedDiffComponentProcessorImpl(CombinedDiffModel model, AnAction goToChangeAction) {
        myModel = model;

        Disposer.register(this, model.getDisposable());
        model.addListener(new ModelListener(), this);

        myMainUi = new CombinedDiffMainUI(model, goToChangeAction);
        Disposer.register(this, myMainUi);
        model.getContext().putUserData(CombinedDiffViewerKeys.COMBINED_DIFF_MAIN_UI, myMainUi);

        myCombinedViewer = createCombinedViewer(true);

        model.cleanBlocks();
    }

    public CombinedDiffModel getModel() {
        return myModel;
    }

    @Override
    public boolean isDisposed() {
        return myDisposed;
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }

    @Override
    public @Nullable Component getPreferredFocusedComponent() {
        return myMainUi.getPreferredFocusedComponent();
    }

    @Override
    public Component getComponent() {
        return myMainUi.getComponent();
    }

    @Override
    public List<? extends Editor> getEmbeddedEditors() {
        CombinedDiffViewer viewer = myCombinedViewer;
        return viewer != null ? viewer.getEditors() : List.of();
    }

    @Override
    public DiffContext getContext() {
        return myModel.getContext();
    }

    @Override
    public List<CombinedBlockProducer> getBlocks() {
        return myModel.getRequests();
    }

    @RequiredUIAccess
    @Override
    public void setBlocks(List<CombinedBlockProducer> requests) {
        myModel.setBlocks(requests);
    }

    @Override
    public void cleanBlocks() {
        myModel.cleanBlocks();
    }

    @Override
    public @Nullable CombinedBlockProducer getCurrentBlock() {
        CombinedDiffViewer viewer = myCombinedViewer;
        if (viewer == null) {
            return null;
        }
        return myModel.getBlock(viewer.getCurrentBlockId());
    }

    @RequiredUIAccess
    @Override
    public void fireProcessorActivated() {
    }

    @Override
    public FileEditorState getState(FileEditorStateLevel level) {
        CombinedDiffViewer viewer = myCombinedViewer;
        if (viewer == null) {
            return FileEditorState.INSTANCE;
        }

        TextEditorProvider textEditorProvider = TextEditorProvider.getInstance();
        Set<CombinedBlockId> blockIds = new LinkedHashSet<>();
        for (CombinedBlockProducer request : myModel.getRequests()) {
            blockIds.add(request.getId());
        }
        List<TextEditorState> editorStates = new ArrayList<>();
        FrameDiffTool.DiffViewer currentViewer = viewer.getCurrentDiffViewer();
        if (currentViewer != null) {
            for (Editor editor : CombinedDiffViewer.getEditors(currentViewer)) {
                editorStates.add(textEditorProvider.getStateImpl(null, editor, level));
            }
        }
        return new CombinedDiffEditorState(
            blockIds,
            // FULL editor states are requested for actions restoring editor state after close, not by navigation.
            // We only want to restore the exact block selection when restoring for navigation or undo.
            level != FileEditorStateLevel.FULL ? viewer.getCurrentBlockId() : null,
            editorStates
        );
    }

    @RequiredUIAccess
    @Override
    public void setState(FileEditorState state) {
        if (!(state instanceof CombinedDiffEditorState combinedState)) {
            return;
        }

        CombinedDiffViewer viewer = myCombinedViewer;
        if (viewer == null) {
            return;
        }
        Set<CombinedBlockId> blockIds = new LinkedHashSet<>();
        for (CombinedBlockProducer request : myModel.getRequests()) {
            blockIds.add(request.getId());
        }
        if (!blockIds.equals(combinedState.getCurrentBlockIds())) {
            return;
        }

        CombinedBlockId activeBlockId = combinedState.getActiveBlockId();
        TextEditorProvider textEditorProvider = TextEditorProvider.getInstance();
        FrameDiffTool.DiffViewer activeViewer = activeBlockId != null ? viewer.getDiffViewerForId(activeBlockId) : null;
        if (activeViewer != null) {
            List<? extends Editor> editors = CombinedDiffViewer.getEditors(activeViewer);
            List<TextEditorState> states = combinedState.getActiveEditorStates();
            for (int i = 0; i < Math.min(editors.size(), states.size()); i++) {
                textEditorProvider.setStateImpl(null, editors.get(i), states.get(i));
            }
        }

        if (activeBlockId != null) {
            viewer.selectDiffBlock(activeBlockId, true);
            viewer.scrollToCaret();
        }
    }

    @RequiredUIAccess
    private @Nullable CombinedDiffViewer createCombinedViewer(boolean initialFocusRequest) {
        DiffContext context = myModel.getContext();
        if (!DiffImplUtil.isUserDataFlagSet(CombinedDiffKeys.COMBINED_DIFF_VIEWER_INITIAL_FOCUS_REQUEST, context)) {
            context.putUserData(CombinedDiffKeys.COMBINED_DIFF_VIEWER_INITIAL_FOCUS_REQUEST, initialFocusRequest);
        }
        List<CombinedBlockProducer> blocks = myModel.getRequests();
        CombinedBlockId blockToSelect = context.getUserData(CombinedDiffKeys.COMBINED_DIFF_SCROLL_TO_BLOCK);
        if (blocks.isEmpty()) {
            return null;
        }

        List<CombinedBlockId> ids = new ArrayList<>();
        for (CombinedBlockProducer block : blocks) {
            ids.add(block.getId());
        }
        BlockState blockState = new BlockState(ids, blockToSelect != null ? blockToSelect : blocks.get(0).getId());

        CombinedDiffViewer viewer = new CombinedDiffViewer(context, new MyBlockListener(), blockState, myMainUi.getUiState());
        Disposer.register(this, viewer);
        context.putUserData(CombinedDiffViewerKeys.COMBINED_DIFF_VIEWER_KEY, viewer);
        myMainUi.setContent(viewer);
        return viewer;
    }

    private class ModelListener implements CombinedDiffModelListener {
        @RequiredUIAccess
        @Override
        public void onModelReset() {
            CombinedDiffViewer viewer = myCombinedViewer;
            if (viewer != null) {
                Disposer.dispose(viewer);
            }
            myModel.getContext().putUserData(CombinedDiffViewerKeys.COMBINED_DIFF_VIEWER_KEY, null);

            myCombinedViewer = createCombinedViewer(false);
        }

        @RequiredUIAccess
        @Override
        public void onRequestsLoaded(CombinedBlockId blockId, DiffRequest request) {
            CombinedDiffViewer viewer = myCombinedViewer;
            if (viewer == null) {
                return;
            }
            CombinedDiffBlockContent newContent = buildBlockContent(myMainUi, myModel.getContext(), request, blockId);
            if (newContent != null) {
                viewer.updateBlockContent(newContent);
                request.onAssigned(true);
            }

            viewer.contentChanged();
        }

        @RequiredUIAccess
        @Override
        public void onRequestContentsUnloaded(Map<CombinedBlockId, DiffRequest> requests) {
            CombinedDiffViewer viewer = myCombinedViewer;
            if (viewer == null) {
                return;
            }

            for (Map.Entry<CombinedBlockId, DiffRequest> entry : requests.entrySet()) {
                viewer.replaceBlockWithPlaceholder(entry.getKey());
                entry.getValue().onAssigned(false);
            }
        }
    }

    private class MyBlockListener implements BlockListener {
        @RequiredUIAccess
        @Override
        public void blocksHidden(Collection<CombinedBlockId> blockIds) {
            myModel.unloadRequestContents(blockIds);
        }

        @RequiredUIAccess
        @Override
        public void blocksVisible(Collection<CombinedBlockId> blockIds) {
            myModel.loadRequestContents(blockIds);
        }
    }

    @RequiredUIAccess
    private static @Nullable CombinedDiffBlockContent buildBlockContent(
        CombinedDiffMainUI mainUi,
        DiffContext context,
        DiffRequest request,
        CombinedBlockId blockId
    ) {
        DiffSettingsHolder.DiffSettings diffSettings = DiffSettingsHolder.DiffSettings.getSettings(context.getUserData(DiffUserDataKeys.PLACE));
        List<DiffTool> diffTools = DiffManagerEx.getInstanceEx().getDiffTools();
        request.putUserData(DiffUserDataKeys.ALIGNED_TWO_SIDED_DIFF, true);
        DiffUserDataKeysEx.ScrollToPolicy scrollToChange = context.getUserData(DiffUserDataKeysEx.SCROLL_TO_CHANGE);
        if (scrollToChange != null) {
            request.putUserData(DiffUserDataKeysEx.SCROLL_TO_CHANGE, scrollToChange);
        }

        List<FrameDiffTool> orderedDiffTools = getOrderedDiffTools(diffSettings, diffTools, mainUi.isUnified());

        List<FrameDiffTool> fittedTools = new ArrayList<>();
        for (FrameDiffTool tool : orderedDiffTools) {
            try {
                if (tool.canShow(context, request)) {
                    fittedTools.add(tool);
                }
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }
        fittedTools = DiffImplUtil.filterSuppressedTools(fittedTools);
        FrameDiffTool diffTool = fittedTools.isEmpty() ? ErrorDiffTool.INSTANCE : fittedTools.get(0);

        FrameDiffTool.DiffViewer childViewer = diffTool.createComponent(context, request);

        notifyDiffViewerCreated(childViewer, context, request);

        return new CombinedDiffBlockContent(childViewer, blockId);
    }

    private static List<FrameDiffTool> getOrderedDiffTools(
        DiffSettingsHolder.DiffSettings diffSettings,
        List<DiffTool> diffTools,
        boolean isUnifiedView
    ) {
        List<FrameDiffTool> frameTools = new ArrayList<>();
        for (DiffTool tool : DiffRequestProcessor.getToolOrderFromSettings(diffSettings, diffTools)) {
            if (tool instanceof FrameDiffTool frameDiffTool) {
                frameTools.add(frameDiffTool);
            }
        }
        List<FrameDiffTool> sorted = new ArrayList<>();
        for (FrameDiffTool tool : frameTools) {
            boolean isUnifiedTool = tool.getToolType() == DiffToolType.UNIFIED;
            if (isUnifiedView == isUnifiedTool) {
                sorted.add(tool);
            }
        }
        for (FrameDiffTool tool : frameTools) {
            if (!sorted.contains(tool)) {
                sorted.add(tool);
            }
        }
        return sorted;
    }

    private static void notifyDiffViewerCreated(FrameDiffTool.DiffViewer viewer, DiffContext context, DiffRequest request) {
        Application.get().getExtensionPoint(DiffExtension.class).forEach(extension -> {
            try {
                extension.onViewerCreated(viewer, context, request);
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        });
    }
}
