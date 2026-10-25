// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.impl.internal.combined.search.CombinedDiffSearchContext;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.ScrollingModelEx;
import consulo.codeEditor.action.EditorActionUtil;
import consulo.codeEditor.event.FocusChangeListener;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.diff.DiffContext;
import consulo.diff.DiffDataKeys;
import consulo.diff.EditorDiffViewer;
import consulo.diff.FrameDiffTool;
import consulo.diff.PrevNextDifferenceIterable;
import consulo.diff.combined.CombinedBlockId;
import consulo.diff.combined.CombinedDiffKeys;
import consulo.diff.impl.internal.DiffViewerEx;
import consulo.diff.impl.internal.TextDiffSettingsHolder;
import consulo.diff.impl.internal.fragment.UnifiedDiffViewer;
import consulo.diff.impl.internal.util.UnifiedDiffUtil;
import consulo.diff.impl.internal.viewer.DiffViewerBase;
import consulo.diff.impl.internal.viewer.FoldingModelSupport;
import consulo.diff.impl.internal.viewer.TextDiffViewerUtil;
import consulo.diff.impl.internal.viewer.simple.SimpleDiffViewer;
import consulo.diff.request.ContentDiffRequest;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Point2D;
import consulo.ui.Rectangle2D;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.internal.ScrollableLayoutInternal;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.ScrollableLayoutOptions;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class CombinedDiffViewer implements CombinedDiffNavigation, CombinedDiffCaretNavigation, UiDataProvider, Disposable {
    public enum ScrollPolicy {
        SCROLL_TO_BLOCK,
        SCROLL_TO_CARET
    }

    private static final int VISIBLE_BLOCKS_UPDATE_DELAY_MS = 100;
    private static final long VIEWPORT_ANCHOR_TIMEOUT_MS = 1000;
    private static final int CARET_SCROLL_MARGIN = 20;

    private final DiffContext myContext;
    private final BlockState myBlockState;
    private final CombinedDiffUIState myViewState;
    private final Project myProject;

    private final Map<CombinedBlockId, FrameDiffTool.DiffViewer> myDiffViewers = new HashMap<>();
    private final Map<CombinedBlockId, CombinedCollapsibleDiffBlock<?>> myDiffBlocks = new HashMap<>();

    private final BitSet myCollapsedDiffBlocks;

    private final CombinedDiffBlocksPanel myBlocksPanel;
    private final ScrollableLayoutInternal myScrollPane;

    private final List<BlockListener> myBlockListeners = new CopyOnWriteArrayList<>();

    private final CombinedDiffPrevNextDifferenceIterable myCurrentDiffIterable = new CombinedDiffPrevNextDifferenceIterable();

    private final List<AnAction> myToolbarActions;

    private int myVisibleBlocksUpdateGeneration;
    private @Nullable ViewportAnchor myViewportAnchor;
    private @Nullable CombinedBlockId myPendingScrollBlock;
    private @Nullable CombinedBlockId myStickyHeaderBlock;
    private boolean myDisposed;

    @RequiredUIAccess
    public CombinedDiffViewer(DiffContext context, BlockListener blockListener, BlockState blockState, CombinedDiffUIState viewState) {
        myContext = context;
        myBlockState = blockState;
        myViewState = viewState;
        myProject = Objects.requireNonNull(context.getProject()); // CombinedDiffContext expected

        myCollapsedDiffBlocks = new BitSet(blockState.getBlocksCount());

        myBlocksPanel = new CombinedDiffBlocksPanel(blockState);

        ScrollableLayoutOptions options = ScrollableLayoutOptions.builder()
            .horizontalScrollPolicy(ScrollableLayoutOptions.ScrollPolicy.NEVER)
            .verticalScrollPolicy(ScrollableLayoutOptions.ScrollPolicy.IF_NEEDED)
            .build();
        myScrollPane = ScrollableLayoutInternal.of(ScrollableLayout.create(myBlocksPanel.getComponent(), options));
        myScrollPane.putUserData(UiDataProvider.KEY, this);
        myBlocksPanel.install(myScrollPane);
        myScrollPane.addViewportListener(event -> onViewportChanged());

        myBlockState.addListener(this::changeSelection, this);
        myBlockListeners.add(blockListener);

        myToolbarActions = createToolbarActions();
    }

    @RequiredUIAccess
    void updateBlockContent(CombinedDiffBlockContent newContent) {
        CombinedBlockId blockId = newContent.getBlockId();
        CombinedCollapsibleDiffBlock<?> newDiffBlock = createDiffBlock(newContent, isCollapsed(blockId));

        newDiffBlock.updateBlockContent(newContent);
        FrameDiffTool.DiffViewer newViewer = newContent.getViewer();
        configureEditorForCombinedDiff(newViewer);
        setupEditorsScrollingListener(newViewer, newDiffBlock);
        installCombinedDiffViewer(newViewer, newDiffBlock);

        runPreservingViewportContent(() -> {
            disposeDiffBlockIfPresent(blockId);
            registerNewDiffBlock(blockId, newDiffBlock, newViewer);

            myBlocksPanel.setContent(blockId, newDiffBlock.getComponent());
            newViewer.init();
            trackEditors(newViewer);

            updateDiffInfo(myBlockState.getCurrentBlock());
            if (!(newViewer instanceof CombinedDiffLoadingBlock)) {
                Boolean requestFocus = myContext.getUserData(CombinedDiffKeys.COMBINED_DIFF_VIEWER_INITIAL_FOCUS_REQUEST);
                myContext.putUserData(CombinedDiffKeys.COMBINED_DIFF_VIEWER_INITIAL_FOCUS_REQUEST, null);
                if (Boolean.TRUE.equals(requestFocus) && myBlockState.getCurrentBlock().equals(blockId)) {
                    requestFocusInDiffViewer(blockId, null);
                }
            }
        });

        if (myBlockState.getCurrentBlock().equals(blockId)) {
            if (!(newContent.getViewer() instanceof CombinedDiffLoadingBlock)) {
                selectDiffBlock(blockId, true);
                scrollToFirstChange(blockId, false, ScrollPolicy.SCROLL_TO_CARET);
            }
        }
    }

    @RequiredUIAccess
    void replaceBlockWithPlaceholder(CombinedBlockId blockId) {
        runPreservingViewportContent(() -> {
            int height = myBlocksPanel.getHeightForBlock(blockId);
            myBlocksPanel.setPlaceholder(blockId, height);
            disposeDiffBlockIfPresent(blockId);
        });
    }

    @RequiredUIAccess
    private CombinedCollapsibleDiffBlock<?> createDiffBlock(CombinedDiffBlockContent content, boolean isCollapsed) {
        CombinedSimpleDiffBlockFactory diffBlockFactory = new CombinedSimpleDiffBlockFactory();
        CombinedCollapsibleDiffBlock<?> newDiffBlock = diffBlockFactory.createBlock(myProject, content, isCollapsed);
        newDiffBlock.addListener(new MyCombinedBlockListener(), this);
        return newDiffBlock;
    }

    @RequiredUIAccess
    private void registerNewDiffBlock(CombinedBlockId blockId, CombinedCollapsibleDiffBlock<?> newBlock, FrameDiffTool.DiffViewer newViewer) {
        Disposer.register(newBlock, () -> {
            myDiffBlocks.remove(blockId);
            FrameDiffTool.DiffViewer viewer = myDiffViewers.remove(blockId);
            if (viewer != null) {
                untrackEditors(viewer);
                Disposer.dispose(viewer);
            }
        });
        Disposer.register(this, newBlock);

        myDiffBlocks.put(blockId, newBlock);
        if (newBlock.getId().equals(myBlockState.getCurrentBlock())) {
            //initial selection of current block
            changeSelection(myBlockState.getCurrentBlock(), newBlock.getId());
        }
        myDiffViewers.put(blockId, newViewer);
    }

    @RequiredUIAccess
    private void disposeDiffBlockIfPresent(CombinedBlockId blockId) {
        CombinedCollapsibleDiffBlock<?> oldDiffBlock = myDiffBlocks.get(blockId);
        if (oldDiffBlock != null) {
            if (blockId.equals(myStickyHeaderBlock)) {
                setStickyHeader(null);
            }
            Disposer.dispose(oldDiffBlock);
        }
    }

    public Component getComponent() {
        return myScrollPane;
    }

    public @Nullable Component getPreferredFocusedComponent() {
        FrameDiffTool.DiffViewer viewer = getCurrentDiffViewer();
        return viewer != null ? viewer.getPreferredFocusedComponent() : null;
    }

    @RequiredUIAccess
    public FrameDiffTool.ToolbarComponents init() {
        FrameDiffTool.ToolbarComponents components = new FrameDiffTool.ToolbarComponents();
        components.toolbarActions = myToolbarActions;
        return components;
    }

    @RequiredUIAccess
    public void rediff() {
        for (FrameDiffTool.DiffViewer viewer : myDiffViewers.values()) {
            if (viewer instanceof DiffViewerBase viewerBase) {
                viewerBase.rediff();
            }
        }
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }

    public PrevNextDifferenceIterable getCurrentDiffIterable() {
        return myCurrentDiffIterable;
    }

    @Override
    public void uiDataSnapshot(DataSink sink) {
        FrameDiffTool.DiffViewer diffViewer = getCurrentDiffViewer();
        sink.set(Project.KEY, myProject);
        sink.set(DiffDataKeys.PREV_NEXT_DIFFERENCE_ITERABLE, myCurrentDiffIterable);
        if (diffViewer instanceof DiffViewerEx viewerEx) {
            sink.lazy(DiffDataKeys.NAVIGATABLE, viewerEx::getNavigatable);
        }
        sink.set(DiffDataKeys.DIFF_VIEWER, diffViewer);
        sink.set(CombinedDiffViewerKeys.COMBINED_DIFF_VIEWER, this);
        sink.set(DiffDataKeys.CURRENT_EDITOR, getCurrentEditor(diffViewer));
    }

    public CombinedDiffMainUI getMainUI() {
        return Objects.requireNonNull(myContext.getUserData(CombinedDiffViewerKeys.COMBINED_DIFF_MAIN_UI));
    }

    @RequiredUIAccess
    private void onViewportChanged() {
        if (myDisposed) {
            return;
        }

        applyViewportAnchor();
        applyPendingScroll();
        updateLastBlockHeight();
        updateStickyHeader();

        int generation = ++myVisibleBlocksUpdateGeneration;
        UIAccess uiAccess = UIAccess.current();
        AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> uiAccess.execute(() -> {
            if (!myDisposed && generation == myVisibleBlocksUpdateGeneration) {
                notifyVisibleBlocksChanged();
            }
        }), VISIBLE_BLOCKS_UPDATE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean canGoNextDiff() {
        return isNavigationEnabled() && (myCurrentDiffIterable.canGoNext() || canGoNextBlock());
    }

    @Override
    public boolean canGoPrevDiff() {
        return isNavigationEnabled() && (myCurrentDiffIterable.canGoPrev() || canGoPrevBlock());
    }

    @RequiredUIAccess
    @Override
    public void goNextDiff() {
        if (!isCollapsed(myBlockState.getCurrentBlock()) && myCurrentDiffIterable.canGoNext()) {
            myCurrentDiffIterable.goNext();
        }
        else if (canGoNextBlock()) {
            myBlockState.goNext();
            myCurrentDiffIterable.goFirst(ScrollType.CENTER);
            selectDiffBlock(myBlockState.getCurrentBlock(), ScrollPolicy.SCROLL_TO_BLOCK, true, ScrollType.CENTER, null);
        }
    }

    @RequiredUIAccess
    @Override
    public void goPrevDiff() {
        if (!isCollapsed(myBlockState.getCurrentBlock()) && myCurrentDiffIterable.canGoPrev()) {
            myCurrentDiffIterable.goPrev();
        }
        else if (canGoPrevBlock()) {
            myBlockState.goPrev();
            myCurrentDiffIterable.goLast();
            selectDiffBlock(myBlockState.getCurrentBlock(), ScrollPolicy.SCROLL_TO_BLOCK, true, ScrollType.CENTER, null);
        }
    }

    @Override
    public boolean canGoNextBlock() {
        return myBlockState.canGoNext();
    }

    @Override
    public boolean canGoPrevBlock() {
        return myBlockState.canGoPrev();
    }

    @RequiredUIAccess
    @Override
    public void goNextBlock() {
        if (!canGoNextBlock()) {
            return;
        }
        myBlockState.goNext();
        selectDiffBlock(myBlockState.getCurrentBlock(), ScrollPolicy.SCROLL_TO_BLOCK, true, ScrollType.CENTER, null);
        if (!isCollapsed(myBlockState.getCurrentBlock())) {
            Editor editor = getCurrentEditor(getCurrentDiffViewer());
            if (editor != null) {
                EditorActionUtil.moveCaretToTextStart(editor);
            }
        }
    }

    @RequiredUIAccess
    @Override
    public void goPrevBlock() {
        if (!canGoPrevBlock()) {
            return;
        }
        myBlockState.goPrev();
        selectDiffBlock(myBlockState.getCurrentBlock(), ScrollPolicy.SCROLL_TO_BLOCK, true, ScrollType.CENTER, null);
        if (!isCollapsed(myBlockState.getCurrentBlock())) {
            Editor editor = getCurrentEditor(getCurrentDiffViewer());
            if (editor != null) {
                EditorActionUtil.moveCaretToTextStart(editor);
            }
        }
    }

    private boolean isNavigationEnabled() {
        return !myDiffBlocks.isEmpty();
    }

    @RequiredUIAccess
    private void notifyVisibleBlocksChanged() {
        int delta = CombinedDiffRegistry.getPreloadedBlocksCount();
        Rectangle2D viewRect = myScrollPane.getViewRect();

        if (viewRect.height() == 0) {
            return;
        }

        @Nullable CombinedBlockId[] beforeViewport = new CombinedBlockId[delta];
        @Nullable CombinedBlockId[] afterViewport = new CombinedBlockId[delta];
        List<CombinedBlockId> blocksInViewport = new ArrayList<>();
        List<CombinedBlockId> hiddenBlocks = new ArrayList<>();
        boolean intersectionStarted = false;

        int index = 0;
        for (BlockBounds block : myBlocksPanel.getBlockBounds()) {
            boolean viewportIntersected = intersects(viewRect, block);
            CombinedBlockId id = block.blockId();

            if (!intersectionStarted && viewportIntersected) {
                intersectionStarted = true;
            }

            if (!intersectionStarted) {
                CombinedBlockId hidden = beforeViewport[Math.floorMod(index, delta)];
                if (hidden != null) {
                    hiddenBlocks.add(hidden);
                }
                beforeViewport[Math.floorMod(index, delta)] = id;
            }
            else if (viewportIntersected) {
                blocksInViewport.add(id);
            }
            else if (hasNull(afterViewport)) {
                afterViewport[Math.floorMod(index, delta)] = id;
            }
            else {
                hiddenBlocks.add(id);
            }
            index++;
        }

        processBlocksState(blocksInViewport, nonNull(beforeViewport), nonNull(afterViewport), hiddenBlocks);
    }

    @RequiredUIAccess
    private void processBlocksState(
        List<CombinedBlockId> inViewport,
        List<CombinedBlockId> beforeViewport,
        List<CombinedBlockId> afterViewport,
        List<CombinedBlockId> hidden
    ) {
        CombinedBlockId currentBlock = myBlockState.getCurrentBlock();
        List<CombinedBlockId> blocksAroundCurrent = getBlocksAroundCurrent(currentBlock);
        LinkedHashSet<CombinedBlockId> totalVisibleSet = new LinkedHashSet<>(blocksAroundCurrent);
        totalVisibleSet.addAll(inViewport);
        totalVisibleSet.addAll(afterViewport);
        totalVisibleSet.addAll(beforeViewport);
        List<CombinedBlockId> totalVisible = new ArrayList<>(totalVisibleSet);

        List<CombinedBlockId> blocksToHide = new ArrayList<>();
        for (CombinedBlockId id : hidden) {
            if (!totalVisibleSet.contains(id)) {
                blocksToHide.add(id);
            }
        }

        if (!blocksToHide.isEmpty()) {
            for (BlockListener listener : myBlockListeners) {
                listener.blocksHidden(blocksToHide);
            }
        }

        if (!totalVisible.isEmpty()) {
            for (BlockListener listener : myBlockListeners) {
                listener.blocksVisible(totalVisible);
            }
            if (Boolean.TRUE.equals(myContext.getUserData(CombinedDiffKeys.DISABLE_LOADING_BLOCKS))) {
                return;
            }

            for (CombinedBlockId id : totalVisible) {
                if (myDiffViewers.get(id) != null) {
                    return;
                }
                int height = myBlocksPanel.getHeightForBlock(id);
                CombinedDiffBlockContent content = new CombinedDiffBlockContent(new CombinedDiffLoadingBlock(height), id);
                updateBlockContent(content);
            }
        }
    }

    @RequiredUIAccess
    private void updateLastBlockHeight() {
        CombinedBlockId lastBlock = myBlockState.getOrNull(myBlockState.getBlocksCount() - 1);
        if (lastBlock == null) {
            return;
        }
        myBlocksPanel.setLastBlockMinHeight(isCollapsed(lastBlock) ? 0 : myScrollPane.getViewRect().height());
    }

    @RequiredUIAccess
    private void updateStickyHeader() {
        Rectangle2D viewRect = myScrollPane.getViewRect();
        BlockBounds bounds = null;
        for (BlockBounds blockBounds : myBlocksPanel.getBlockBounds()) {
            if (intersects(viewRect, blockBounds)) {
                bounds = blockBounds;
                break;
            }
        }
        if (bounds == null) {
            return;
        }
        CombinedCollapsibleDiffBlock<?> block = myDiffBlocks.get(bounds.blockId());
        myViewState.setStickyHeaderUnderBorder(false);

        if (block == null || isCollapsed(block.getId()) || bounds.minY() >= viewRect.minY()) {
            setStickyHeader(null);
            return;
        }

        setStickyHeader(block);
        myViewState.setStickyHeaderUnderBorder(true);

        updateDiffInfo(block.getId());
    }

    @RequiredUIAccess
    private void setStickyHeader(@Nullable CombinedCollapsibleDiffBlock<?> block) {
        CombinedBlockId blockId = block != null ? block.getId() : null;
        if (Objects.equals(myStickyHeaderBlock, blockId)) {
            return;
        }
        myStickyHeaderBlock = blockId;
        myScrollPane.setTopOverlay(block != null ? block.getStickyHeaderComponent() : null);
    }

    private List<CombinedBlockId> getBlocksAroundCurrent(CombinedBlockId currentBlock) {
        int delta = CombinedDiffRegistry.getPreloadedBlocksCount();
        int currentIndex = myBlockState.indexOf(currentBlock);
        int startIndex = Math.max(0, currentIndex - delta);
        int endIndex = Math.min(myBlockState.getBlocksCount() - 1, currentIndex + delta);

        List<CombinedBlockId> result = new ArrayList<>();
        for (int i = startIndex; i <= endIndex; i++) {
            CombinedBlockId id = myBlockState.getOrNull(i);
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }

    @RequiredUIAccess
    private void updateDiffInfo(CombinedBlockId blockId) {
        FrameDiffTool.DiffViewer viewer = myDiffViewers.get(blockId);
        ContentDiffRequest request = viewer instanceof DiffViewerBase viewerBase ? viewerBase.getRequest() : null;
        if (request == null) {
            myViewState.setDiffInfo(EmptyDiffInfoState.INSTANCE);
            return;
        }

        List<String> titles = new ArrayList<>();
        for (String title : request.getContentTitles()) {
            if (title != null && !title.isBlank()) {
                titles.add(title);
            }
        }
        DiffInfoState newDiffInfo = switch (titles.size()) {
            case 0 -> EmptyDiffInfoState.INSTANCE;
            case 1 -> new SingleTitleDiffInfoState(titles.get(0));
            default -> new TwoTitlesDiffInfoState(titles.get(0), titles.get(1));
        };
        myViewState.setDiffInfo(newDiffInfo);
    }

    public CombinedBlockId getCurrentBlockId() {
        return myBlockState.getCurrentBlock();
    }

    public int getDiffBlocksCount() {
        return myBlockState.getBlocksCount();
    }

    public FrameDiffTool.@Nullable DiffViewer getCurrentDiffViewer() {
        return myDiffViewers.get(myBlockState.getCurrentBlock());
    }

    FrameDiffTool.@Nullable DiffViewer getDiffViewerForId(CombinedBlockId id) {
        return myDiffViewers.get(id);
    }

    @RequiredUIAccess
    public void selectDiffBlock(CombinedBlockId blockId, boolean focusBlock) {
        selectDiffBlock(blockId, focusBlock, ScrollPolicy.SCROLL_TO_BLOCK);
    }

    @RequiredUIAccess
    public void selectDiffBlock(CombinedBlockId blockId, boolean focusBlock, @Nullable ScrollPolicy scrollPolicy) {
        selectDiffBlock(blockId, scrollPolicy, focusBlock, ScrollType.CENTER, null);
    }

    @RequiredUIAccess
    public void scrollToFirstChange(CombinedBlockId blockId, boolean focusBlock, @Nullable ScrollPolicy scrollPolicy) {
        if (isCollapsed(blockId)) {
            selectDiffBlock(blockId, scrollPolicy, false, ScrollType.CENTER, null);
        }
        else {
            selectDiffBlock(blockId, scrollPolicy, false, ScrollType.RELATIVE, null);
            myCurrentDiffIterable.goFirst(ScrollType.RELATIVE);
            scroll(ScrollPolicy.SCROLL_TO_CARET, blockId, ScrollType.CENTER_DOWN);
        }
    }

    @RequiredUIAccess
    private void selectDiffBlock(
        CombinedBlockId blockId,
        @Nullable ScrollPolicy scrollPolicy,
        boolean focusBlock,
        ScrollType scrollType,
        @Nullable Editor editorToFocus
    ) {
        if (focusBlock) {
            if (isCollapsed(blockId)) {
                requestFocusInBlock(blockId);
            }
            else {
                requestFocusInDiffViewer(blockId, editorToFocus);
            }
        }

        myBlockState.setCurrentBlock(blockId);
        scroll(scrollPolicy, blockId, scrollType);
    }

    @RequiredUIAccess
    private void changeSelection(CombinedBlockId oldBlockId, CombinedBlockId newBlockId) {
        if (!oldBlockId.equals(newBlockId)) {
            CombinedCollapsibleDiffBlock<?> oldBlock = myDiffBlocks.get(oldBlockId);
            if (oldBlock != null) {
                oldBlock.setSelected(false);
            }
        }
        CombinedCollapsibleDiffBlock<?> newBlock = myDiffBlocks.get(newBlockId);
        if (newBlock != null) {
            newBlock.setSelected(true);
        }
        updateDiffInfo(newBlockId);
    }

    @RequiredUIAccess
    private void requestFocusInBlock(CombinedBlockId blockId) {
        CombinedCollapsibleDiffBlock<?> block = myDiffBlocks.get(blockId);
        if (block != null) {
            UnifiedDiffUtil.requestFocus(block.getPreferredFocusComponent());
        }
    }

    @RequiredUIAccess
    private void requestFocusInDiffViewer(CombinedBlockId blockId, @Nullable Editor editor) {
        FrameDiffTool.DiffViewer viewer = myDiffViewers.get(blockId);
        if (viewer == null) {
            return;
        }
        Component componentToFocus;
        Editor currentEditor = getCurrentEditor(viewer);
        if (editor != null) {
            componentToFocus = editor.getContentUIComponent();
        }
        else if (currentEditor != null) {
            componentToFocus = currentEditor.getContentUIComponent();
        }
        else if (viewer.getPreferredFocusedComponent() != null) {
            componentToFocus = viewer.getPreferredFocusedComponent();
        }
        else {
            componentToFocus = viewer.getComponent();
        }
        UnifiedDiffUtil.requestFocus(componentToFocus);
    }

    private List<AnAction> createToolbarActions() {
        TextDiffSettingsHolder.TextDiffSettings settings = TextDiffViewerUtil.getTextSettings(myContext);
        List<AnAction> actions = new ArrayList<>();
        actions.add(new CombinedToggleExpandByDefaultAction(settings, this::getFoldingModels));
        actions.add(new CombinedIgnorePolicySettingAction(settings, this::rediff));
        actions.add(new CombinedHighlightPolicySettingAction(settings, this::rediff));
        return actions;
    }

    @RequiredUIAccess
    void contentChanged() {
        updateSearch(); //as a possible optimization, this should be done after all requests were loaded
    }

    @RequiredUIAccess
    private void updateSearch() {
        getMainUI().updateSearch(createSearchContext());
    }

    public CombinedDiffSearchContext createSearchContext() {
        List<CombinedDiffSearchContext.EditorHolder> holders = new ArrayList<>();
        for (CombinedBlockId id : myBlockState.iterateBlocks()) {
            if (isCollapsed(id)) {
                continue;
            }
            FrameDiffTool.DiffViewer viewer = myDiffViewers.get(id);
            if (viewer != null) {
                holders.add(new CombinedDiffSearchContext.EditorHolder(getEditors(viewer)));
            }
        }
        return new CombinedDiffSearchContext(holders);
    }

    private List<FoldingModelSupport> getFoldingModels() {
        List<FoldingModelSupport> models = new ArrayList<>();
        for (FrameDiffTool.DiffViewer viewer : myDiffViewers.values()) {
            if (viewer instanceof SimpleDiffViewer simpleDiffViewer) {
                models.add(simpleDiffViewer.getFoldingModel());
            }
            else if (viewer instanceof UnifiedDiffViewer unifiedDiffViewer) {
                models.add(unifiedDiffViewer.getFoldingModel());
            }
        }
        return models;
    }

    @RequiredUIAccess
    @Override
    public void moveCaretToPrevBlock() {
        myBlockState.goPrev();

        moveCaretToBlock(false);
    }

    @RequiredUIAccess
    @Override
    public void moveCaretToNextBlock() {
        myBlockState.goNext();

        moveCaretToBlock(true);
    }

    @RequiredUIAccess
    private void moveCaretToBlock(boolean next) {
        CombinedBlockId currentBlockId = myBlockState.getCurrentBlock();

        if (isCollapsed(currentBlockId)) {
            Rectangle2D viewRect = myScrollPane.getViewRect();
            BlockBounds bounds = myBlocksPanel.getBoundsForBlock(currentBlockId);
            if (bounds != null && intersects(viewRect, bounds)) {
                requestFocusInBlock(currentBlockId);
            }
            else {
                selectDiffBlock(currentBlockId, true);
            }
        }
        else {
            Editor editor = getCurrentEditor(getCurrentDiffViewer());
            if (editor == null) {
                return;
            }
            if (next) {
                EditorActionUtil.moveCaretToTextStart(editor);
            }
            else {
                EditorActionUtil.moveCaretToTextEnd(editor);
            }
            requestFocusInDiffViewer(myBlockState.getCurrentBlock(), null);
            scrollToCaret();
        }
    }

    @RequiredUIAccess
    void toggleBlockCollapse() {
        CombinedCollapsibleDiffBlock<?> block = myDiffBlocks.get(myBlockState.getCurrentBlock());
        if (block == null) {
            return;
        }
        block.setCollapsed(!isCollapsed(block.getId()));
    }

    @RequiredUIAccess
    void collapseAllBlocks() {
        for (CombinedCollapsibleDiffBlock<?> block : new ArrayList<>(myDiffBlocks.values())) {
            block.setCollapsed(true);
        }

        myCollapsedDiffBlocks.set(0, myBlockState.getBlocksCount(), true);
    }

    @RequiredUIAccess
    @Override
    public void moveCaretPageUp() {
        movePageUpDown(true);
    }

    @RequiredUIAccess
    @Override
    public void moveCaretPageDown() {
        movePageUpDown(false);
    }

    @RequiredUIAccess
    private void movePageUpDown(boolean pageUp) {
        if (isCollapsed(myBlockState.getCurrentBlock())) {
            if (pageUp) {
                goPrevBlock();
            }
            else {
                goNextBlock();
            }
            return;
        }

        Editor editor = getCurrentEditor(getCurrentDiffViewer());
        if (editor == null) {
            return;
        }
        Caret caret = editor.getCaretModel().getCurrentCaret();

        int lineBeforeJump = caret.getVisualPosition().line;

        if (pageUp) {
            EditorActionUtil.moveCaretPageUp(editor, false);
        }
        else {
            EditorActionUtil.moveCaretPageDown(editor, false);
        }

        int lineAfterJump = editor.getCaretModel().getCurrentCaret().getVisualPosition().line;

        if (lineBeforeJump != lineAfterJump) {
            scrollToCaret();
            return;
        }

        if (pageUp) {
            if (canGoPrevBlock()) {
                moveCaretToPrevBlock();
            }
        }
        else {
            if (canGoNextBlock()) {
                moveCaretToNextBlock();
            }
        }
    }

    @RequiredUIAccess
    public void scrollToCaret() {
        scrollToCaret(ScrollType.RELATIVE);
    }

    @RequiredUIAccess
    public void scrollToEditor(Editor editor, boolean focus) {
        for (Map.Entry<CombinedBlockId, FrameDiffTool.DiffViewer> entry : myDiffViewers.entrySet()) {
            if (getEditors(entry.getValue()).contains(editor)) {
                CombinedBlockId blockId = entry.getKey();
                if (!blockId.equals(getCurrentBlockId())) {
                    selectDiffBlock(blockId, ScrollPolicy.SCROLL_TO_BLOCK, focus, ScrollType.CENTER, editor);
                }
                return;
            }
        }
    }

    List<Editor> getEditors() {
        List<Editor> editors = new ArrayList<>();
        for (FrameDiffTool.DiffViewer viewer : myDiffViewers.values()) {
            editors.addAll(getEditors(viewer));
        }
        return editors;
    }

    private boolean isCollapsed(CombinedBlockId blockId) {
        return myCollapsedDiffBlocks.get(myBlockState.indexOf(blockId));
    }

    @RequiredUIAccess
    private void setupEditorsScrollingListener(FrameDiffTool.DiffViewer newViewer, Disposable blockDisposable) {
        for (Editor editor : getEditors(newViewer)) {
            if (editor.getScrollingModel() instanceof ScrollingModelEx scrollingModel) {
                scrollingModel.addScrollRequestListener(
                    (targetPosition, scrollType) -> {
                        //e.g., scroll to caret emitted from editor search session (next/prev occurence action)
                        FrameDiffTool.DiffViewer currentViewer = getCurrentDiffViewer();
                        if (currentViewer != null && getEditors(currentViewer).contains(editor)) {
                            scrollToCaret(scrollType);
                        }
                    },
                    blockDisposable
                );
            }
        }
    }

    private void installCombinedDiffViewer(FrameDiffTool.DiffViewer newViewer, Disposable blockDisposable) {
        for (Editor editor : getEditors(newViewer)) {
            editor.putUserData(CombinedDiffViewerKeys.COMBINED_DIFF_VIEWER_KEY, this);
            if (editor instanceof EditorEx editorEx) {
                editorEx.addFocusListener(new FocusChangeListener() {
                    @Override
                    public void focusGained(Editor focusedEditor) {
                        for (Map.Entry<CombinedBlockId, FrameDiffTool.DiffViewer> entry : myDiffViewers.entrySet()) {
                            if (getEditors(entry.getValue()).contains(focusedEditor)) {
                                myBlockState.setCurrentBlock(entry.getKey());
                                return;
                            }
                        }
                    }
                }, blockDisposable);
            }
        }
    }

    @RequiredUIAccess
    private void trackEditors(FrameDiffTool.DiffViewer viewer) {
        for (Editor editor : getEditors(viewer)) {
            myScrollPane.setBoundsTracked(editor.getContentUIComponent(), true);
        }
    }

    @RequiredUIAccess
    private void untrackEditors(FrameDiffTool.DiffViewer viewer) {
        for (Editor editor : getEditors(viewer)) {
            myScrollPane.setBoundsTracked(editor.getContentUIComponent(), false);
        }
    }

    @RequiredUIAccess
    private void scroll(@Nullable ScrollPolicy scrollPolicy, CombinedBlockId combinedBlockId, ScrollType scrollType) {
        boolean isEditorBased = getDiffViewerForId(combinedBlockId) instanceof EditorDiffViewer;
        if (scrollPolicy == ScrollPolicy.SCROLL_TO_BLOCK || !isEditorBased) {
            scrollToDiffBlock(combinedBlockId);
        }
        else if (scrollPolicy == ScrollPolicy.SCROLL_TO_CARET) {
            scrollToDiffChangeWithCaret(scrollType);
        }
    }

    @RequiredUIAccess
    private void scrollToDiffChangeWithCaret(ScrollType scrollType) {
        if (!(getCurrentDiffViewer() instanceof EditorDiffViewer)) {
            return; //avoid scrolling for non editor based viewers
        }

        scrollToCaret(scrollType);
    }

    @RequiredUIAccess
    private void scrollToDiffBlock(CombinedBlockId id) {
        BlockBounds bounds = myBlocksPanel.getBoundsForBlock(id);
        if (bounds == null) {
            myPendingScrollBlock = id;
            return;
        }
        myPendingScrollBlock = null;
        myViewportAnchor = null;
        myScrollPane.scrollTo(new Point2D(0, bounds.minY()));
    }

    @RequiredUIAccess
    private void applyPendingScroll() {
        CombinedBlockId pending = myPendingScrollBlock;
        if (pending != null && myBlocksPanel.getBoundsForBlock(pending) != null) {
            scrollToDiffBlock(pending);
        }
    }

    @RequiredUIAccess
    private void scrollToCaret(ScrollType scrollType) {
        Editor editor = getCurrentEditor(getCurrentDiffViewer());
        if (editor == null) {
            return;
        }
        Rectangle2D editorBounds = myScrollPane.getBounds(editor.getContentUIComponent());
        if (editorBounds == null) {
            return;
        }

        int caretTop = editorBounds.minY() + editor.visualLineToY(editor.getCaretModel().getVisualPosition().line);
        int caretBottom = caretTop + editor.getLineHeight();

        Rectangle2D viewRect = myScrollPane.getViewRect();
        int targetY;
        switch (scrollType) {
            case CENTER, CENTER_DOWN, CENTER_UP -> targetY = caretTop - viewRect.height() / 2;
            default -> {
                if (caretTop < viewRect.minY()) {
                    targetY = caretTop - CARET_SCROLL_MARGIN;
                }
                else if (caretBottom > viewRect.maxY()) {
                    targetY = caretBottom - viewRect.height() + CARET_SCROLL_MARGIN;
                }
                else {
                    return;
                }
            }
        }
        myViewportAnchor = null;
        myScrollPane.scrollTo(new Point2D(0, Math.max(0, targetY)));
    }

    /**
     * Keeps the first visible block in place while the blocks around it change their heights.
     */
    @RequiredUIAccess
    private void runPreservingViewportContent(Runnable run) {
        Rectangle2D viewRect = myScrollPane.getViewRect();

        BlockBounds anchorBlock = null;
        for (BlockBounds block : myBlocksPanel.getBlockBounds()) {
            if (intersects(viewRect, block)) {
                anchorBlock = block;
                break;
            }
        }

        run.run();

        if (anchorBlock != null) {
            myViewportAnchor = new ViewportAnchor(anchorBlock.blockId(), anchorBlock.minY(), System.currentTimeMillis());
            applyViewportAnchor();
        }
    }

    @RequiredUIAccess
    private void applyViewportAnchor() {
        ViewportAnchor anchor = myViewportAnchor;
        if (anchor == null) {
            return;
        }
        if (System.currentTimeMillis() - anchor.createdAt() > VIEWPORT_ANCHOR_TIMEOUT_MS) {
            myViewportAnchor = null;
            return;
        }

        BlockBounds newBounds = myBlocksPanel.getBoundsForBlock(anchor.blockId());
        if (newBounds == null) {
            return;
        }

        int shift = newBounds.minY() - anchor.minY();
        if (shift == 0) {
            return;
        }

        myViewportAnchor = new ViewportAnchor(anchor.blockId(), newBounds.minY(), anchor.createdAt());
        Rectangle2D viewRect = myScrollPane.getViewRect();
        myScrollPane.scrollTo(new Point2D(viewRect.minX(), viewRect.minY() + shift));
    }

    private static boolean intersects(Rectangle2D rect, BlockBounds bb) {
        return (bb.minY() >= rect.minY() && bb.minY() < rect.maxY())
            || (bb.maxY() > rect.minY() && bb.maxY() <= rect.maxY())
            || (bb.minY() <= rect.minY() && bb.maxY() >= rect.maxY());
    }

    private static boolean hasNull(@Nullable CombinedBlockId[] ids) {
        for (CombinedBlockId id : ids) {
            if (id == null) {
                return true;
            }
        }
        return false;
    }

    private static List<CombinedBlockId> nonNull(@Nullable CombinedBlockId[] ids) {
        List<CombinedBlockId> result = new ArrayList<>();
        for (CombinedBlockId id : ids) {
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }

    public static @Nullable Editor getCurrentEditor(FrameDiffTool.@Nullable DiffViewer viewer) {
        return viewer instanceof EditorDiffViewer editorDiffViewer ? editorDiffViewer.getCurrentEditor() : null;
    }

    static List<? extends Editor> getEditors(FrameDiffTool.DiffViewer viewer) {
        return viewer instanceof EditorDiffViewer editorDiffViewer ? editorDiffViewer.getEditors() : List.of();
    }

    @RequiredUIAccess
    private static void configureEditorForCombinedDiff(FrameDiffTool.DiffViewer viewer) {
        for (Editor editor : getEditors(viewer)) {
            editor.getSettings().setAdditionalLinesCount(0);
            editor.getSettings().setAdditionalPageAtBottom(false);
            if (editor instanceof EditorEx editorEx) {
                editorEx.setFitContentHeight(true);
            }
        }
    }

    private class MyCombinedBlockListener implements CombinedDiffBlockListener {
        @RequiredUIAccess
        @Override
        public void onCollapseStateChanged(CombinedBlockId id, boolean collapseState) {
            myCollapsedDiffBlocks.set(myBlockState.indexOf(id), collapseState);

            boolean isStickyHeader = id.equals(myStickyHeaderBlock);
            if (isStickyHeader) {
                setStickyHeader(null);
                selectDiffBlock(id, ScrollPolicy.SCROLL_TO_BLOCK, true, ScrollType.CENTER, null);
            }
            else {
                if (collapseState) {
                    requestFocusInBlock(id);
                }
                else {
                    requestFocusInDiffViewer(id, null);
                }
            }
            updateLastBlockHeight();

            updateSearch();
        }
    }

    private class CombinedDiffPrevNextDifferenceIterable implements PrevNextDifferenceIterable {
        private @Nullable PrevNextDifferenceIterable getDifferencesIterable() {
            FrameDiffTool.DiffViewer currentViewer = getCurrentDiffViewer();
            return currentViewer instanceof DiffViewerEx viewerEx ? viewerEx.getDifferenceIterable() : null;
        }

        @Override
        public boolean canGoNext() {
            PrevNextDifferenceIterable iterable = getDifferencesIterable();
            return iterable != null && iterable.canGoNext();
        }

        @Override
        public boolean canGoPrev() {
            PrevNextDifferenceIterable iterable = getDifferencesIterable();
            return iterable != null && iterable.canGoPrev();
        }

        @RequiredUIAccess
        @Override
        public void goNext() {
            PrevNextDifferenceIterable iterable = getDifferencesIterable();
            if (iterable != null) {
                iterable.goNext();
            }
            scrollToDiffChangeWithCaret(ScrollType.CENTER);
        }

        @RequiredUIAccess
        @Override
        public void goPrev() {
            PrevNextDifferenceIterable iterable = getDifferencesIterable();
            if (iterable != null) {
                iterable.goPrev();
            }
            scrollToDiffChangeWithCaret(ScrollType.CENTER);
        }

        @RequiredUIAccess
        void goFirst(ScrollType scrollType) {
            PrevNextDifferenceIterable diffIterable = getDifferencesIterable();
            if (diffIterable == null) {
                return;
            }
            while (diffIterable.canGoPrev()) {
                diffIterable.goPrev();
            }
            scrollToDiffChangeWithCaret(scrollType);
        }

        @RequiredUIAccess
        void goLast() {
            PrevNextDifferenceIterable diffIterable = getDifferencesIterable();
            if (diffIterable == null) {
                return;
            }
            while (diffIterable.canGoNext()) {
                diffIterable.goNext();
            }
            scrollToDiffChangeWithCaret(ScrollType.CENTER);
        }
    }
}
