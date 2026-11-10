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
package consulo.ide.impl.idea.usages.impl;

import consulo.application.ReadAction;
import consulo.application.internal.ProgressWrapper;
import consulo.application.progress.ProgressIndicator;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.find.impl.UnifiedFindPopupPanel;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiInvalidElementAccessException;
import consulo.localize.LocalizeValue;
import consulo.navigation.ItemPresentation;
import consulo.navigation.Navigatable;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.MessageBoxes;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.content.Content;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.undoRedo.CommandProcessor;
import consulo.usage.TextChunk;
import consulo.usage.Usage;
import consulo.usage.UsageInfo;
import consulo.usage.UsageInfo2UsageAdapter;
import consulo.usage.UsageSearcher;
import consulo.usage.UsageTarget;
import consulo.usage.UsageView;
import consulo.usage.UsageViewContentManager;
import consulo.usage.UsageViewManager;
import consulo.usage.UsageViewPresentation;
import consulo.usage.localize.UsageLocalize;
import consulo.usage.rule.UsageInFile;
import consulo.usage.rule.UsageInFiles;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFilePresentation;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The usage view of the unified frontends: the searched targets, then the usages in code and the other occurrences,
 * each grouped by file, with a preview of the selected usage.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
public class UnifiedUsageViewImpl implements UsageViewInternal {
    private static final int EXPAND_ALL_LIMIT = 1000;

    private final Project myProject;
    private final UsageViewPresentation myPresentation;
    private final UsageTarget[] myTargets;
    private final @Nullable Supplier<UsageSearcher> myUsageSearcherFactory;
    private final UIAccess myUIAccess;

    private final UnifiedUsageGroup myTargetsGroup;
    private final UnifiedUsageGroup myCodeUsagesGroup;
    private final UnifiedUsageGroup myNonCodeUsagesGroup;

    private final Object myLock = new Object();
    private final Map<Usage, UnifiedUsagePresentation> myUsages = new LinkedHashMap<>();
    private final Map<Usage, VirtualFile> myUsageFiles = new HashMap<>();
    private final Set<Usage> myNonCodeUsages = new HashSet<>();
    private final Set<Usage> myExcludedUsages = new HashSet<>();
    private final Map<VirtualFile, UnifiedUsagePresentation> myFiles = new HashMap<>();
    private final Map<UsageTarget, UnifiedUsagePresentation> myTargetPresentations = new LinkedHashMap<>();

    private final List<AnAction> myLowerPaneActions = new ArrayList<>();
    private final List<ExcludeListener> myExcludeListeners = new ArrayList<>();
    private final AtomicBoolean myRefreshScheduled = new AtomicBoolean();

    private volatile boolean mySearchInProgress = true;
    private volatile boolean myDisposed;
    private volatile boolean myChangesDetected;
    private volatile @Nullable ProgressIndicator myAssociatedProgress;
    private @Nullable Action myRerunAction;
    private @Nullable Content myContent;

    private @Nullable Tree<Object> myTree;
    private @Nullable DockLayout myRootLayout;
    private @Nullable UnifiedUsagePreviewPanel myPreviewPanel;

    public UnifiedUsageViewImpl(
        Project project,
        UsageViewPresentation presentation,
        UsageTarget[] targets,
        @Nullable Supplier<UsageSearcher> usageSearcherFactory
    ) {
        myProject = project;
        myPresentation = presentation;
        myTargets = targets;
        myUsageSearcherFactory = usageSearcherFactory;
        myUIAccess = project.getUIAccess();

        myTargetsGroup = new UnifiedUsageGroup(LocalizeValue.of(presentation.getTargetsNodeText()), true, false);
        myCodeUsagesGroup = new UnifiedUsageGroup(LocalizeValue.of(presentation.getCodeUsagesString()), false, false);
        myNonCodeUsagesGroup = new UnifiedUsageGroup(LocalizeValue.of(presentation.getNonCodeUsagesString()), false, true);

        ReadAction.run(() -> {
            for (UsageTarget target : targets) {
                myTargetPresentations.put(target, createTargetPresentation(target));
            }
        });

        UsageModelTracker modelTracker = new UsageModelTracker(project);
        Disposer.register(this, modelTracker);
        modelTracker.addListener(isPropertyChange -> {
            if (!isPropertyChange) {
                myChangesDetected = true;
            }
        }, this);
    }

    private static UnifiedUsagePresentation createTargetPresentation(UsageTarget target) {
        ItemPresentation presentation = target.getPresentation();
        String text = presentation == null ? String.valueOf(target.getName()) : presentation.getPresentableText().get();
        return new UnifiedUsagePresentation(
            presentation == null ? null : presentation.getIcon(),
            List.of(Pair.create(text, TextAttribute.REGULAR))
        );
    }

    @Override
    @RequiredUIAccess
    public Content addContent(UsageViewPresentation presentation) {
        Content content = UsageViewContentManager.getInstance(myProject).addUIContent(
            presentation.getTabText(),
            presentation.getTabName(),
            presentation.getToolwindowTitle(),
            true,
            getUIComponent(),
            presentation.isOpenInNewTab(),
            true
        );
        content.putUserData(UsageView.USAGE_VIEW_KEY, this);
        myContent = content;
        return content;
    }

    @RequiredUIAccess
    public Component getUIComponent() {
        DockLayout rootLayout = myRootLayout;
        if (rootLayout != null) {
            return rootLayout;
        }

        Tree<Object> tree = Tree.create(new UsageTreeModel());
        tree.setAllowMultipleSelect(true);
        tree.addSelectListener(event -> updatePreview());
        myTree = tree;

        UnifiedUsagePreviewPanel previewPanel = new UnifiedUsagePreviewPanel(myProject);
        Disposer.register(this, previewPanel);
        myPreviewPanel = previewPanel;

        TwoComponentSplitLayout splitLayout = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        splitLayout.setFirstComponent(ScrollableLayout.create(tree));
        splitLayout.setSecondComponent(previewPanel.getComponent());
        splitLayout.setProportion(50);

        rootLayout = DockLayout.create();
        rootLayout.center(splitLayout);
        rootLayout.left(createToolbar(rootLayout).getUIComponent());
        rootLayout.putUserData(UiDataProvider.KEY, this::uiDataSnapshot);
        myRootLayout = rootLayout;

        if (myPresentation.isShowCancelButton()) {
            myLowerPaneActions.add(0, DumbAwareAction.create(UsageLocalize.usageViewCancelButton(), e -> close()));
        }
        rebuildLowerPane();
        return rootLayout;
    }

    @RequiredUIAccess
    private ActionToolbar createToolbar(Component target) {
        ActionGroup group = ActionGroup.newImmutableBuilder()
            .add(new RerunAction())
            .addSeparator()
            .add(new ExpandAllAction())
            .add(new CollapseAllAction())
            .addSeparator()
            .add(new ExcludeAction(true))
            .add(new ExcludeAction(false))
            .build();
        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar("UsageViewToolbar", group, ActionToolbar.Style.VERTICAL);
        toolbar.setTargetUIComponent(target);
        toolbar.updateActionsAsync();
        return toolbar;
    }

    @RequiredUIAccess
    private void rebuildLowerPane() {
        DockLayout rootLayout = myRootLayout;
        if (rootLayout == null || myLowerPaneActions.isEmpty()) {
            return;
        }
        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar(
            "UsageViewLowerPane",
            ActionGroup.newImmutableBuilder().addAll(myLowerPaneActions).build(),
            ActionToolbar.Style.BUTTON
        );
        toolbar.setTargetUIComponent(rootLayout);
        toolbar.updateActionsAsync();
        rootLayout.bottom(toolbar.getUIComponent());
    }

    private void uiDataSnapshot(DataSink sink) {
        sink.set(UsageView.USAGE_VIEW_KEY, this);
        sink.set(Project.KEY, myProject);

        List<Usage> selected = new ArrayList<>(getSelectedUsages());
        if (!selected.isEmpty()) {
            sink.set(UsageView.USAGES_KEY, selected.toArray(Usage.EMPTY_ARRAY));
        }
        if (selected.size() == 1 && selected.get(0).canNavigate()) {
            sink.set(Navigatable.KEY, selected.get(0));
        }
    }

    @RequiredUIAccess
    private void updatePreview() {
        UnifiedUsagePreviewPanel previewPanel = myPreviewPanel;
        if (previewPanel == null) {
            return;
        }
        List<UsageInfo> infos = new ArrayList<>();
        for (Usage usage : getSelectedUsages()) {
            if (usage instanceof UsageInfo2UsageAdapter adapter) {
                infos.add(adapter.getUsageInfo());
            }
        }
        previewPanel.updateLayout(infos.isEmpty() ? null : infos);
    }

    @Override
    public void appendUsage(Usage usage) {
        if (myDisposed || !usage.isValid()) {
            return;
        }

        VirtualFile file = usage instanceof UsageInFile usageInFile ? usageInFile.getFile() : null;
        boolean nonCode = usage instanceof UsageInfo2UsageAdapter adapter && adapter.getUsageInfo().isNonCodeUsage;

        TextChunk[] chunks = usage.getPresentation().getText();
        List<Pair<String, TextAttribute>> text = new ArrayList<>(chunks.length);
        for (TextChunk chunk : chunks) {
            text.add(Pair.create(chunk.getText(), UnifiedFindPopupPanel.toTextAttribute(chunk, null)));
        }
        UnifiedUsagePresentation presentation = new UnifiedUsagePresentation(usage.getPresentation().getIcon(), List.copyOf(text));

        synchronized (myLock) {
            myUsages.put(usage, presentation);
            if (nonCode) {
                myNonCodeUsages.add(usage);
            }
            if (file != null) {
                myUsageFiles.put(usage, file);
                myFiles.computeIfAbsent(file, it -> new UnifiedUsagePresentation(
                    VirtualFilePresentation.getIcon(it),
                    List.of(Pair.create(it.getName(), TextAttribute.REGULAR))
                ));
            }
        }
        scheduleRefresh();
    }

    @Override
    public CompletableFuture<?> appendUsagesInBulk(Collection<? extends Usage> usages) {
        ReadAction.run(() -> {
            for (Usage usage : usages) {
                appendUsage(usage);
            }
        });
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void removeUsage(Usage usage) {
        removeUsagesBulk(List.of(usage));
    }

    @Override
    public void removeUsagesBulk(Collection<Usage> usages) {
        synchronized (myLock) {
            for (Usage usage : usages) {
                myUsages.remove(usage);
                myUsageFiles.remove(usage);
                myNonCodeUsages.remove(usage);
                myExcludedUsages.remove(usage);
            }
            myFiles.keySet().retainAll(myUsageFiles.values());
        }
        scheduleRefresh();
    }

    @Override
    public void includeUsages(Usage[] usages) {
        setExcluded(List.of(usages), false);
    }

    @Override
    public void excludeUsages(Usage[] usages) {
        setExcluded(List.of(usages), true);
    }

    private void setExcluded(Collection<Usage> usages, boolean excluded) {
        Set<Usage> changed = new LinkedHashSet<>();
        synchronized (myLock) {
            for (Usage usage : usages) {
                if (excluded ? myExcludedUsages.add(usage) : myExcludedUsages.remove(usage)) {
                    changed.add(usage);
                }
            }
        }
        if (changed.isEmpty()) {
            return;
        }
        for (ExcludeListener listener : List.copyOf(myExcludeListeners)) {
            listener.fireExcluded(changed, excluded);
        }
        scheduleRefresh();
    }

    @Override
    public void addExcludeListener(Disposable disposable, ExcludeListener listener) {
        myExcludeListeners.add(listener);
        Disposer.register(disposable, () -> myExcludeListeners.remove(listener));
    }

    @Override
    public void selectUsages(Usage[] usages) {
        if (usages.length == 0) {
            return;
        }
        Usage usage = usages[0];
        myUIAccess.give(() -> {
            Tree<Object> tree = myTree;
            TreeNode<Object> root = tree == null ? null : tree.getRootNode();
            if (root != null) {
                root.findChildDeep(value -> value == usage).whenCompleteAsync((node, error) -> {
                    if (node != null) {
                        tree.select(node);
                    }
                }, myUIAccess);
            }
        });
    }

    private void scheduleRefresh() {
        if (myDisposed || !myRefreshScheduled.compareAndSet(false, true)) {
            return;
        }
        myUIAccess.getScheduler().schedule(() -> myUIAccess.give(this::refreshTree), 100, TimeUnit.MILLISECONDS);
    }

    @RequiredUIAccess
    private CompletableFuture<?> refreshTree() {
        myRefreshScheduled.set(false);
        Tree<Object> tree = myTree;
        if (myDisposed || tree == null) {
            return CompletableFuture.completedFuture(null);
        }
        return tree.refreshAll();
    }

    @Override
    public void drainQueuedUsageNodes() {
        myUIAccess.give(this::refreshTree);
    }

    @Override
    public int getShownUsageCount() {
        synchronized (myLock) {
            return myUsages.size();
        }
    }

    @Override
    @RequiredUIAccess
    public void expandRoot() {
        Tree<Object> tree = myTree;
        if (tree == null) {
            return;
        }
        refreshTree().whenCompleteAsync((result, error) -> {
            if (getUsagesCount() < EXPAND_ALL_LIMIT) {
                tree.expandAll();
            }
            else {
                tree.expandAll(2);
            }
        }, myUIAccess);
    }

    @Override
    public void expandAll() {
        myUIAccess.give(() -> {
            Tree<Object> tree = myTree;
            if (tree != null) {
                refreshTree().whenCompleteAsync((result, error) -> tree.expandAll(), myUIAccess);
            }
        });
    }

    @Override
    public void close() {
        cancelCurrentSearch();
        Content content = myContent;
        if (content != null) {
            myUIAccess.give(() -> UsageViewContentManager.getInstance(myProject).closeContent(content));
        }
    }

    @Override
    public boolean isSearchInProgress() {
        return mySearchInProgress;
    }

    @Override
    public void setSearchInProgress(boolean searchInProgress) {
        mySearchInProgress = searchInProgress;
        if (!searchInProgress) {
            myUIAccess.give(() -> {
                if (!myDisposed) {
                    expandRoot();
                }
            });
        }
    }

    @Override
    public void searchFinished() {
        drainQueuedUsageNodes();
        setSearchInProgress(false);
    }

    @Override
    public boolean searchHasBeenCancelled() {
        ProgressIndicator progress = myAssociatedProgress;
        return progress != null && progress.isCanceled();
    }

    @Override
    public void cancelCurrentSearch() {
        ProgressIndicator progress = myAssociatedProgress;
        if (progress != null) {
            ProgressWrapper.unwrap(progress).cancel();
        }
    }

    @Override
    public void associateProgress(ProgressIndicator indicator) {
        myAssociatedProgress = indicator;
    }

    @Override
    public void waitForUpdateRequestsCompletion() {
    }

    @Override
    @RequiredUIAccess
    public void refreshUsages() {
        synchronized (myLock) {
            myUsages.clear();
            myUsageFiles.clear();
            myNonCodeUsages.clear();
            myExcludedUsages.clear();
            myFiles.clear();
        }
        myChangesDetected = false;
        refreshTree();

        Action rerunAction = myRerunAction;
        if (rerunAction != null) {
            rerunAction.actionPerformed(null);
            return;
        }
        UsageViewPresentation rerunPresentation = myPresentation.copy();
        rerunPresentation.setOpenInNewTab(false);
        UsageViewManager.getInstance(myProject).searchAndShowUsages(myTargets, myUsageSearcherFactory, true, false, rerunPresentation, null);
    }

    @Override
    public boolean canPerformReRun() {
        return ReadAction.compute(() -> {
            for (UsageTarget target : myTargets) {
                if (!target.isValid()) {
                    return false;
                }
            }
            if (myRerunAction != null && myRerunAction.isEnabled()) {
                return true;
            }
            try {
                return myUsageSearcherFactory != null && myUsageSearcherFactory.get() != null;
            }
            catch (PsiInvalidElementAccessException e) {
                return false;
            }
        });
    }

    @Override
    public void setRerunAction(Action rerunAction) {
        myRerunAction = rerunAction;
    }

    @Override
    @RequiredUIAccess
    public void addButtonToLowerPane(AnAction action) {
        myLowerPaneActions.add(action);
        rebuildLowerPane();
    }

    @Override
    public void setAdditionalComponent(@Nullable JComponent component) {
    }

    @Override
    @RequiredUIAccess
    public void addPerformOperationAction(
        Runnable processRunnable,
        LocalizeValue commandName,
        LocalizeValue cannotMakeString,
        LocalizeValue actionText,
        boolean checkReadOnlyStatus
    ) {
        addButtonToLowerPane(() -> performOperation(processRunnable, commandName, cannotMakeString, checkReadOnlyStatus), actionText);
    }

    @RequiredUIAccess
    private void performOperation(
        Runnable processRunnable,
        LocalizeValue commandName,
        LocalizeValue cannotMakeString,
        boolean checkReadOnlyStatus
    ) {
        if (checkReadOnlyStatus && !checkReadonlyUsages()) {
            return;
        }
        PsiDocumentManager.getInstance(myProject).commitAllDocuments();
        if (cannotMakeString.isNotEmpty() && myChangesDetected) {
            LocalizeValue title = UsageLocalize.changesDetectedErrorTitle();
            if (canPerformReRun()) {
                MessageBoxes.okCancel()
                    .asError()
                    .title(title)
                    .text(LocalizeValue.join("\n\n", cannotMakeString, UsageLocalize.dialogRerunSearch()))
                    .showAsync(myProject)
                    .whenCompleteAsync((rerun, error) -> {
                        if (Boolean.TRUE.equals(rerun)) {
                            refreshUsages();
                        }
                    }, myUIAccess);
            }
            else {
                MessageBoxes.okError(cannotMakeString).title(title).showAsync(myProject);
            }
            return;
        }

        close();
        CommandProcessor.getInstance().newCommand()
            .project(myProject)
            .name(commandName)
            .run(processRunnable);
    }

    @RequiredUIAccess
    private boolean checkReadonlyUsages() {
        Set<VirtualFile> files = new HashSet<>();
        synchronized (myLock) {
            for (Usage usage : myUsages.keySet()) {
                if (myExcludedUsages.contains(usage) || !usage.isReadOnly()) {
                    continue;
                }
                if (usage instanceof UsageInFile usageInFile) {
                    VirtualFile file = usageInFile.getFile();
                    if (file != null && file.isValid()) {
                        files.add(file);
                    }
                }
                if (usage instanceof UsageInFiles usageInFiles) {
                    Collections.addAll(files, usageInFiles.getFiles());
                }
            }
        }
        for (UsageTarget target : myTargets) {
            VirtualFile[] targetFiles = target.getFiles();
            if (targetFiles != null) {
                Collections.addAll(files, targetFiles);
            }
        }
        return files.isEmpty() || !ReadonlyStatusHandler.getInstance(myProject).ensureFilesWritable(files).hasReadonlyFiles();
    }

    @Override
    public UsageViewPresentation getPresentation() {
        return myPresentation;
    }

    @Override
    public Set<Usage> getExcludedUsages() {
        synchronized (myLock) {
            return new HashSet<>(myExcludedUsages);
        }
    }

    @Override
    public Set<Usage> getSelectedUsages() {
        Tree<Object> tree = myTree;
        if (tree == null) {
            return Set.of();
        }
        Set<Usage> result = new LinkedHashSet<>();
        for (TreeNode<Object> node : tree.getSelectedNodes()) {
            Object value = node.getValue();
            if (value instanceof Usage usage) {
                result.add(usage);
            }
            else if (value instanceof UnifiedUsageFileGroup fileGroup) {
                result.addAll(usagesOf(fileGroup));
            }
            else if (value instanceof UnifiedUsageGroup group && !group.targets()) {
                result.addAll(usagesOf(group.nonCode()));
            }
        }
        return result;
    }

    @Override
    public Set<Usage> getUsages() {
        synchronized (myLock) {
            return new LinkedHashSet<>(myUsages.keySet());
        }
    }

    @Override
    public List<Usage> getSortedUsages() {
        List<Usage> usages = new ArrayList<>(getUsages());
        usages.sort(UsageViewImpl.USAGE_COMPARATOR);
        return usages;
    }

    @Override
    public JComponent getComponent() {
        throw new UnsupportedOperationException("The unified usage view has no Swing component, use getUIComponent()");
    }

    @Override
    public int getUsagesCount() {
        synchronized (myLock) {
            return myUsages.size();
        }
    }

    @Override
    public boolean isDisposed() {
        return myDisposed;
    }

    @Override
    @RequiredUIAccess
    public void select() {
        Tree<Object> tree = myTree;
        if (tree != null) {
            tree.focus();
        }
    }

    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public void dispose() {
        myDisposed = true;
        cancelCurrentSearch();
        myRerunAction = null;
    }

    private List<Usage> usagesOf(boolean nonCode) {
        synchronized (myLock) {
            List<Usage> result = new ArrayList<>();
            for (Usage usage : myUsages.keySet()) {
                if (myNonCodeUsages.contains(usage) == nonCode) {
                    result.add(usage);
                }
            }
            return result;
        }
    }

    private List<Usage> usagesOf(UnifiedUsageFileGroup fileGroup) {
        synchronized (myLock) {
            List<Usage> result = new ArrayList<>();
            for (Usage usage : myUsages.keySet()) {
                if (fileGroup.file().equals(myUsageFiles.get(usage)) && myNonCodeUsages.contains(usage) == fileGroup.nonCode()) {
                    result.add(usage);
                }
            }
            return result;
        }
    }

    private void render(Object value, TextItemPresentation presentation) {
        if (value instanceof UnifiedUsageGroup group) {
            presentation.append(group.text().get(), TextAttribute.REGULAR_BOLD);
            if (!group.targets()) {
                appendCount(presentation, usagesOf(group.nonCode()).size());
            }
            return;
        }

        UnifiedUsagePresentation itemPresentation;
        boolean excluded;
        int count = -1;
        synchronized (myLock) {
            if (value instanceof Usage usage) {
                itemPresentation = myUsages.get(usage);
                excluded = myExcludedUsages.contains(usage);
            }
            else if (value instanceof UnifiedUsageFileGroup fileGroup) {
                itemPresentation = myFiles.get(fileGroup.file());
                excluded = false;
                count = usagesOf(fileGroup).size();
            }
            else if (value instanceof UsageTarget target) {
                itemPresentation = myTargetPresentations.get(target);
                excluded = false;
            }
            else {
                itemPresentation = null;
                excluded = false;
            }
        }
        if (itemPresentation == null) {
            presentation.append(String.valueOf(value));
            return;
        }

        presentation.withIcon(itemPresentation.icon());
        for (Pair<String, TextAttribute> fragment : itemPresentation.text()) {
            presentation.append(fragment.getFirst(), excluded ? TextAttribute.GRAYED : fragment.getSecond());
        }
        if (count >= 0) {
            appendCount(presentation, count);
        }
    }

    private static void appendCount(TextItemPresentation presentation, int count) {
        presentation.append("  " + UsageLocalize.usageViewCounter(count).get(), TextAttribute.GRAYED);
    }

    private final class UsageTreeModel implements TreeModel<Object> {
        @Override
        public void buildChildren(Function<Object, TreeNode<Object>> nodeFactory, @Nullable Object parentValue) {
            if (parentValue == null) {
                if (myTargets.length > 0) {
                    group(nodeFactory, myTargetsGroup);
                }
                if (!usagesOf(false).isEmpty()) {
                    group(nodeFactory, myCodeUsagesGroup);
                }
                if (!usagesOf(true).isEmpty()) {
                    group(nodeFactory, myNonCodeUsagesGroup);
                }
                return;
            }

            if (parentValue instanceof UnifiedUsageGroup group) {
                if (group.targets()) {
                    for (UsageTarget target : myTargets) {
                        leaf(nodeFactory, target);
                    }
                    return;
                }
                buildFileGroups(nodeFactory, group.nonCode());
                return;
            }

            if (parentValue instanceof UnifiedUsageFileGroup fileGroup) {
                List<Usage> usages = usagesOf(fileGroup);
                usages.sort(UsageViewImpl.USAGE_COMPARATOR);
                for (Usage usage : usages) {
                    leaf(nodeFactory, usage);
                }
            }
        }

        private void buildFileGroups(Function<Object, TreeNode<Object>> nodeFactory, boolean nonCode) {
            Set<VirtualFile> files = new LinkedHashSet<>();
            List<Usage> withoutFile = new ArrayList<>();
            synchronized (myLock) {
                for (Usage usage : usagesOf(nonCode)) {
                    VirtualFile file = myUsageFiles.get(usage);
                    if (file == null) {
                        withoutFile.add(usage);
                    }
                    else {
                        files.add(file);
                    }
                }
            }
            List<VirtualFile> sortedFiles = new ArrayList<>(files);
            sortedFiles.sort(Comparator.comparing(VirtualFile::getPath));
            for (VirtualFile file : sortedFiles) {
                group(nodeFactory, new UnifiedUsageFileGroup(file, nonCode));
            }
            for (Usage usage : withoutFile) {
                leaf(nodeFactory, usage);
            }
        }

        private void group(Function<Object, TreeNode<Object>> nodeFactory, Object value) {
            TreeNode<Object> node = nodeFactory.apply(value);
            node.setRenderer((item, presentation) -> render(item, presentation));
            node.setLeaf(false);
        }

        private void leaf(Function<Object, TreeNode<Object>> nodeFactory, Object value) {
            TreeNode<Object> node = nodeFactory.apply(value);
            node.setRenderer((item, presentation) -> render(item, presentation));
            node.setLeaf(true);
        }

        @Override
        public boolean onDoubleClick(Tree<Object> tree, TreeNode<Object> node) {
            Object value = node.getValue();
            if (value instanceof Navigatable navigatable && navigatable.canNavigate()) {
                navigatable.navigate(true);
                return false;
            }
            return true;
        }
    }

    private final class RerunAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private RerunAction() {
            super(UsageLocalize.actionRerun(), UsageLocalize.actionDescriptionRerun(), PlatformIconGroup.actionsRerun());
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(!mySearchInProgress && canPerformReRun());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            refreshUsages();
        }
    }

    private final class ExpandAllAction extends DumbAwareAction {
        private ExpandAllAction() {
            super(ActionLocalize.actionExpandallText(), LocalizeValue.empty(), PlatformIconGroup.actionsExpandall());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            Tree<Object> tree = myTree;
            if (tree != null) {
                tree.expandAll();
            }
        }
    }

    private final class CollapseAllAction extends DumbAwareAction {
        private CollapseAllAction() {
            super(ActionLocalize.actionCollapseallText(), LocalizeValue.empty(), PlatformIconGroup.actionsCollapseall());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            Tree<Object> tree = myTree;
            if (tree != null) {
                tree.collapseAll();
            }
        }
    }

    private final class ExcludeAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private final boolean myExclude;

        private ExcludeAction(boolean exclude) {
            super(
                exclude ? ActionLocalize.actionUsageviewExcludeText() : ActionLocalize.actionUsageviewIncludeText(),
                LocalizeValue.empty(),
                exclude ? PlatformIconGroup.actionsCancel() : PlatformIconGroup.actionsUndo()
            );
            myExclude = exclude;
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(!getSelectedUsages().isEmpty());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            setExcluded(getSelectedUsages(), myExclude);
        }
    }
}
