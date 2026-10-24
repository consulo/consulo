/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change.patch;

import consulo.application.ReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.dataContext.UiDataProvider;
import consulo.diff.DiffDialogHints;
import consulo.diff.DiffManager;
import consulo.diff.chain.DiffRequestChain;
import consulo.diff.chain.DiffRequestProducer;
import consulo.diff.chain.DiffRequestProducerException;
import consulo.diff.internal.GoToChangePopupBuilder;
import consulo.diff.request.DiffRequest;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.language.plain.PlainTextFileType;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.LabelOptions;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.UIAccess;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.*;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.ex.dialog.action.DialogCancelAction;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.PlaceholderLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.io.FileUtil;
import consulo.util.lang.Couple;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.change.patch.FilePatch;
import consulo.versionControlSystem.change.patch.TextFilePatch;
import consulo.versionControlSystem.impl.internal.change.shelf.ShelvedBinaryFilePatch;
import consulo.versionControlSystem.impl.internal.change.ui.ChangeListChooserPanel;
import consulo.versionControlSystem.impl.internal.change.ui.ChangeListDialogDescriptor;
import consulo.versionControlSystem.impl.internal.change.ui.ChangesLegend;
import consulo.versionControlSystem.impl.internal.change.ui.ChangesTreeSnapshot;
import consulo.versionControlSystem.impl.internal.change.ui.ChangesTreeSnapshotBuilder;
import consulo.versionControlSystem.impl.internal.change.ui.GroupByDirectoryToggleAction;
import consulo.versionControlSystem.impl.internal.change.ui.UnifiedChangesTree;
import consulo.versionControlSystem.impl.internal.diff.ChangeGoToChangePopupAction;
import consulo.versionControlSystem.impl.internal.patch.PatchFileType;
import consulo.versionControlSystem.impl.internal.patch.PatchReader;
import consulo.versionControlSystem.impl.internal.patch.PatchSyntaxException;
import consulo.versionControlSystem.impl.internal.patch.PatchVirtualFileReader;
import consulo.versionControlSystem.impl.internal.util.ZipperUpdater;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.VirtualFileEvent;
import consulo.virtualFileSystem.event.VirtualFileListener;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Applies a patch file - or unshelves a shelved change list - file by file: every file of the patch is matched to a
 * base in the project, the bases can be remapped and stripped, and the chosen files go to a change list.
 */
public class ApplyPatchDifferentiatedDialog extends ChangeListDialogDescriptor {
    private static final Logger LOG = Logger.getInstance(ApplyPatchDifferentiatedDialog.class);

    private static final int ADDED = 0;
    private static final int MODIFIED = 1;
    private static final int DELETED = 2;
    private static final int INAPPLICABLE = 3;
    private static final int LEGEND_SIZE = 4;

    protected final Project myProject;
    private final ApplyPatchExecutor myCallback;
    private final List<ApplyPatchExecutor> myExecutors;
    private final boolean myCanChangePatchFile;
    private final boolean myUseProjectRootAsPredefinedBase;
    private final boolean myShouldUpdateChangeListName;
    private final @Nullable VirtualFile myPatchFile;
    private final @Nullable List<TextFilePatch> myInitialPatches;
    private final @Nullable LocalChangeList myDefaultList;
    private final @Nullable String myExternalCommitMessage;
    private final @Nullable List<ShelvedBinaryFilePatch> myBinaryShelvedPatches;
    private final @Nullable Collection<Change> myPreselectedChanges;

    private final List<AbstractFilePatchInProgress> myPatches = new ArrayList<>();
    private final AtomicReference<@Nullable FilePresentationModel> myRecentPathFileChange = new AtomicReference<>();
    private @Nullable PatchReader myReader;
    private boolean myContainBasedChanges;
    private boolean myShowFlatten;
    private boolean myModal;
    private String myHelpId = "reference.dialogs.vcs.patch.apply";
    private LocalizeValue myOkText = CommonLocalize.buttonOk();

    private @Nullable UIAccess myUIAccess;
    private @Nullable ZipperUpdater myLoadQueue;
    private @Nullable UnifiedChangesTree myChangesTree;
    private @Nullable PlaceholderLayout myTreeContent;
    private @Nullable Label myEmptyText;
    private @Nullable ChangesLegend myLegend;
    private @Nullable ChangeListChooserPanel myChangeListChooser;
    private LocalizeValue myChangeListError = LocalizeValue.empty();

    @RequiredUIAccess
    public ApplyPatchDifferentiatedDialog(
        Project project,
        ApplyPatchExecutor callback,
        List<ApplyPatchExecutor> executors,
        ApplyPatchMode applyPatchMode,
        VirtualFile patchFile
    ) {
        this(project, callback, executors, applyPatchMode, patchFile, null, null, null, null, null, false);
    }

    @RequiredUIAccess
    public ApplyPatchDifferentiatedDialog(
        Project project,
        ApplyPatchExecutor callback,
        List<ApplyPatchExecutor> executors,
        ApplyPatchMode applyPatchMode,
        List<TextFilePatch> patches,
        @Nullable LocalChangeList defaultList
    ) {
        this(project, callback, executors, applyPatchMode, null, patches, defaultList, null, null, null, false);
    }

    @RequiredUIAccess
    public ApplyPatchDifferentiatedDialog(
        Project project,
        ApplyPatchExecutor callback,
        List<ApplyPatchExecutor> executors,
        ApplyPatchMode applyPatchMode,
        @Nullable VirtualFile patchFile,
        @Nullable List<TextFilePatch> patches,
        @Nullable LocalChangeList defaultList,
        @Nullable List<ShelvedBinaryFilePatch> binaryShelvedPatches,
        @Nullable Collection<Change> preselectedChanges,
        @Nullable String externalCommitMessage,
        boolean useProjectRootAsPredefinedBase
    ) {
        super(applyPatchMode.getTitle());
        myProject = project;
        myCallback = callback;
        myExecutors = executors;
        myCanChangePatchFile = applyPatchMode.isCanChangePatchFile();
        myUseProjectRootAsPredefinedBase = useProjectRootAsPredefinedBase;
        myShouldUpdateChangeListName = defaultList == null && externalCommitMessage == null;
        myPatchFile = patchFile;
        myInitialPatches = patches;
        myDefaultList = defaultList;
        myExternalCommitMessage = externalCommitMessage;
        myBinaryShelvedPatches = binaryShelvedPatches;
        myPreselectedChanges = preselectedChanges;
    }

    public static FileChooserDescriptor createSelectPatchDescriptor() {
        return new FileChooserDescriptor(true, false, false, false, false, false) {
            @Override
            @RequiredUIAccess
            public boolean isFileSelectable(VirtualFile file) {
                return file.getFileType() == PatchFileType.INSTANCE || file.getFileType() == PlainTextFileType.INSTANCE;
            }
        };
    }

    @RequiredUIAccess
    public CompletableFuture<?> showAsync() {
        return myProject.getApplication().getInstance(DialogService.class).build(myProject, this).showAsync();
    }

    public void setHelpId(String helpId) {
        myHelpId = helpId;
    }

    public void setOKButtonText(LocalizeValue okText) {
        myOkText = okText;
    }

    public void setModal(boolean modal) {
        myModal = modal;
    }

    @Override
    public boolean isModal() {
        return myModal;
    }

    @Override
    public @Nullable String getHelpId() {
        return myHelpId;
    }

    @Override
    public @Nullable String getDimensionServiceKey() {
        return "vcs.ApplyPatchDifferentiatedDialog";
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofFont(80, 40);
    }

    /**
     * An option shown under the change list chooser, e.g. what to do with the patch once it is applied.
     */
    @RequiredUIAccess
    protected @Nullable Component createDoNotAskCheckbox() {
        return null;
    }

    @Override
    protected DialogOkAction createOkAction() {
        return new DialogOkAction(myOkText);
    }

    @Override
    public AnAction[] createActions(boolean inverseOrder) {
        if (myExecutors.isEmpty()) {
            return super.createActions(inverseOrder);
        }
        List<AnAction> actions = new ArrayList<>();
        actions.add(createOkAction());
        for (ApplyPatchExecutor executor : myExecutors) {
            actions.add(new ExecutorAction(executor));
        }
        actions.add(new DialogCancelAction());
        if (inverseOrder) {
            Collections.reverse(actions);
        }
        return actions.toArray(AnAction[]::new);
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        UIAccess uiAccess = UIAccess.current();
        myUIAccess = uiAccess;
        myLoadQueue = new ZipperUpdater(500, uiDisposable);

        UnifiedChangesTree tree = new UnifiedChangesTree(myProject, true, uiDisposable);
        myChangesTree = tree;
        tree.setAutoIncludeDefaultList(false);
        tree.setIncludableFilter(change -> !(change instanceof AbstractFilePatchInProgress.PatchChange patchChange) || patchChange.isValid());
        tree.setChangeDecorator(this::decorate);
        tree.setDoubleClickHandler(item -> {
            List<AbstractFilePatchInProgress.PatchChange> selectedChanges = getSelectedChanges();
            if (selectedChanges.size() == 1 && !selectedChanges.get(0).isValid()) {
                new NewBaseSelector(false).run();
                return;
            }
            showDiff();
        });
        tree.addInclusionListener(this::inclusionChanged);

        ChangesLegend legend = new ChangesLegend();
        legend.addEntry(VcsLocalize.commitLegendNew(), FileStatus.ADDED);
        legend.addEntry(VcsLocalize.commitLegendModified(), FileStatus.MODIFIED);
        legend.addEntry(VcsLocalize.commitLegendDeleted(), FileStatus.DELETED);
        legend.addEntry(VcsLocalize.patchApplyMissingBaseFileLabel(), FileStatus.MERGED_WITH_CONFLICTS);
        myLegend = legend;

        Label emptyText = Label.create(VcsLocalize.commitDialogNoChangesDetectedText(),
            LabelOptions.builder().horizontalAlignment(HorizontalAlignment.CENTER).build());
        emptyText.setForegroundColor(ComponentColors.DISABLED_TEXT);
        myEmptyText = emptyText;
        DockLayout placeholder = DockLayout.create();
        placeholder.center(emptyText);

        PlaceholderLayout treeContent = PlaceholderLayout.create();
        treeContent.setContent(ScrollableLayout.create(tree.getComponent()));
        treeContent.setPlaceholder(placeholder);
        treeContent.setPlaceholderVisibility(myPatches::isEmpty);
        myTreeContent = treeContent;

        DockLayout treePanel = DockLayout.create();
        treePanel.putUserData(UiDataProvider.KEY, tree::uiDataSnapshot);
        treePanel.top(createToolbar(treePanel, tree).getUIComponent());
        treePanel.center(treeContent);
        treePanel.bottom(legend.getComponent());

        ChangeListChooserPanel chooser = new ChangeListChooserPanel(myProject, errorMessage -> {
            myChangeListError = errorMessage;
            setErrorText(errorMessage);
        });
        myChangeListChooser = chooser;
        chooser.setChangeLists(ChangeListManager.getInstance(myProject).getChangeListsCopy());
        if (myDefaultList != null) {
            chooser.setDefaultSelection(myDefaultList);
        }
        else if (myExternalCommitMessage != null) {
            chooser.setSuggestedName(myExternalCommitMessage);
        }
        chooser.init();

        VerticalLayout bottom = VerticalLayout.create();
        bottom.add(chooser.getComponent());
        Component doNotAsk = createDoNotAskCheckbox();
        if (doNotAsk != null) {
            bottom.add(doNotAsk);
        }

        TwoComponentSplitLayout splitter = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);
        splitter.setFirstComponent(treePanel);
        splitter.setSecondComponent(bottom);
        splitter.setProportion(70);

        DockLayout root = DockLayout.create();
        if (myCanChangePatchFile) {
            root.top(createPatchFileField(uiDisposable));
        }
        root.center(splitter);

        if (myPatchFile != null && myPatchFile.isValid()) {
            myRecentPathFileChange.set(new FilePresentationModel(myPatchFile));
            queueRequest();
        }
        else if (myInitialPatches != null) {
            init(myInitialPatches);
        }

        if (myCanChangePatchFile) {
            VirtualFileListener listener = new VirtualFileListener() {
                @Override
                @RequiredUIAccess
                public void contentsChanged(VirtualFileEvent event) {
                    uiAccess.give(() -> syncUpdatePatchFileAndScheduleReloadIfNeeded(event.getFile()));
                }
            };
            VirtualFileManager fileManager = VirtualFileManager.getInstance();
            fileManager.addVirtualFileListener(listener);
            Disposer.register(uiDisposable, () -> fileManager.removeVirtualFileListener(listener));
        }

        return withErrorLabel(root);
    }

    @RequiredUIAccess
    private Component createPatchFileField(Disposable uiDisposable) {
        FileChooserTextBoxBuilder.Controller patchFile = FileChooserTextBoxBuilder.create(myProject)
            .fileChooserDescriptor(createSelectPatchDescriptor())
            .dialogTitle(VcsLocalize.patchApplySelectTitle())
            .uiDisposable(uiDisposable)
            .build();
        if (myPatchFile != null && myPatchFile.isValid()) {
            patchFile.setValue(myPatchFile.getPresentableUrl(), false);
        }
        patchFile.getComponent().addValueListener(event -> {
            myRecentPathFileChange.set(new FilePresentationModel(patchFile.getValue()));
            queueRequest();
        });

        Label label = Label.create(VcsLocalize.patchApplyFileNameField());
        label.setTarget(patchFile.getComponent());

        VerticalLayout field = VerticalLayout.create();
        field.add(label);
        field.add(patchFile.getComponent());
        return field;
    }

    @RequiredUIAccess
    private ActionToolbar createToolbar(Component target, UnifiedChangesTree tree) {
        DefaultActionGroup group = new DefaultActionGroup();

        MyShowDiff diffAction = new MyShowDiff();
        diffAction.registerCustomShortcutSet(CommonShortcuts.getDiff(), tree.getComponent());
        group.add(diffAction);

        DefaultActionGroup mapDirectoryGroup = new DefaultActionGroup(VcsLocalize.patchApplyChangeDirectoryPathsGroup(), true);
        mapDirectoryGroup.getTemplatePresentation().setIcon(PlatformIconGroup.nodesFolder());
        mapDirectoryGroup.add(new MapDirectory());
        mapDirectoryGroup.add(new StripUp(VcsLocalize.patchApplyRemoveLeadingDirectoryAction()));
        mapDirectoryGroup.add(new ZeroStrip());
        mapDirectoryGroup.add(new StripDown(VcsLocalize.patchApplyRestoreLeadingDirectoryAction()));
        mapDirectoryGroup.add(new ResetStrip());
        group.add(mapDirectoryGroup);

        if (myCanChangePatchFile) {
            group.add(new DumbAwareAction(CommonLocalize.actionRefresh(), CommonLocalize.actionRefresh(), PlatformIconGroup.actionsRefresh()) {
                @Override
                @RequiredUIAccess
                public void actionPerformed(AnActionEvent e) {
                    syncUpdatePatchFileAndScheduleReloadIfNeeded(null);
                }
            });
        }
        group.addSeparator();
        group.add(new GroupByDirectoryToggleAction(() -> !myShowFlatten, grouped -> {
            myShowFlatten = !grouped;
            updateTree(false);
        }));
        TreeExpander expander = TreeExpander.of(tree.getComponent());
        CommonActionsManager commonActionsManager = CommonActionsManager.getInstance();
        group.add(commonActionsManager.createExpandAllAction(expander));
        group.add(commonActionsManager.createCollapseAllAction(expander));

        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar("APPLY_PATCH", group, ActionToolbar.Style.HORIZONTAL);
        toolbar.setTargetUIComponent(target);
        return toolbar;
    }

    @Override
    public boolean doUpdateOkButtonState() {
        return myChangeListError.isEmpty() && isChangeTreeEnabled();
    }

    @RequiredUIAccess
    @Override
    public Component getPreferredFocusedComponent() {
        ChangeListChooserPanel chooser = myChangeListChooser;
        return chooser == null ? null : chooser.getPreferredFocusedComponent();
    }

    @RequiredUIAccess
    @Override
    public boolean canHandle(AnAction action, @Nullable DialogValue value) {
        if (action instanceof DialogOkAction) {
            runExecutorAfterClose(myCallback);
        }
        return true;
    }

    @RequiredUIAccess
    private void updateOkActions() {
        ChangeListChooserPanel chooser = myChangeListChooser;
        if (isChangeTreeEnabled() && chooser != null) {
            chooser.updateEnabled();
        }
        updateOkButtonState();
    }

    private boolean isChangeTreeEnabled() {
        UnifiedChangesTree tree = myChangesTree;
        return tree != null && !tree.getIncludedChanges().isEmpty();
    }

    @RequiredUIAccess
    private void queueRequest() {
        ZipperUpdater loadQueue = myLoadQueue;
        if (loadQueue == null) {
            return;
        }
        paintBusy(true);
        loadQueue.queue(this::loadPatchesInBackground);
    }

    @RequiredUIAccess
    private void init(List<? extends FilePatch> patches) {
        UIAccess uiAccess = UIAccess.current();
        CompletableFuture.supplyAsync(() -> new MatchPatchPaths(myProject).execute(patches, myUseProjectRootAsPredefinedBase))
            .whenCompleteAsync((matchedPatches, error) -> {
                if (error != null) {
                    LOG.error(error);
                    return;
                }
                myPatches.clear();
                myPatches.addAll(matchedPatches);
                updateTree(true);
            }, uiAccess);
    }

    /**
     * Runs on the load queue, off the UI thread.
     */
    private void loadPatchesInBackground() {
        UIAccess uiAccess = myUIAccess;
        if (uiAccess == null) {
            return;
        }

        FilePresentationModel filePresentationModel = myRecentPathFileChange.get();
        VirtualFile file = filePresentationModel != null ? filePresentationModel.getVf() : null;
        if (file == null) {
            uiAccess.give(this::reset);
            return;
        }

        PatchReader patchReader = loadPatches(file);
        List<FilePatch> filePatches = patchReader != null ? new ArrayList<>(patchReader.getPatches()) : new ArrayList<>();
        if (!ContainerUtil.isEmpty(myBinaryShelvedPatches)) {
            filePatches.addAll(myBinaryShelvedPatches);
        }
        List<AbstractFilePatchInProgress> matchedPatches =
            new MatchPatchPaths(myProject).execute(filePatches, myUseProjectRootAsPredefinedBase);

        uiAccess.give(() -> {
            ChangeListChooserPanel chooser = myChangeListChooser;
            if (myShouldUpdateChangeListName && chooser != null) {
                chooser.setSuggestedName(file.getNameWithoutExtension().replace('_', ' ').trim());
            }
            myPatches.clear();
            myPatches.addAll(matchedPatches);
            myReader = patchReader;
            updateTree(true);
            paintBusy(false);
            updateOkActions();
        });
    }

    private static @Nullable PatchReader loadPatches(VirtualFile patchFile) {
        PatchReader reader;
        patchFile.refresh(false, false);
        try {
            reader = ReadAction.compute(() -> PatchVirtualFileReader.create(patchFile));
        }
        catch (IOException e) {
            LOG.warn("Can't read patchFile: " + patchFile.getPresentableName(), e);
            return null;
        }
        try {
            reader.parseAllPatches();
        }
        catch (PatchSyntaxException e) {
            return null;
        }

        return reader;
    }

    @RequiredUIAccess
    private void syncUpdatePatchFileAndScheduleReloadIfNeeded(@Nullable VirtualFile eventFile) {
        FilePresentationModel filePresentationModel = myRecentPathFileChange.get();
        VirtualFile filePresentationVf = filePresentationModel != null ? filePresentationModel.getVf() : null;
        if (filePresentationVf != null && (eventFile == null || filePresentationVf.equals(eventFile))) {
            filePresentationVf.refresh(false, false);
            queueRequest();
        }
    }

    @RequiredUIAccess
    private void reset() {
        myPatches.clear();
        myReader = null;
        myContainBasedChanges = false;
        updateTree(true);
        paintBusy(false);
        updateOkActions();
    }

    @RequiredUIAccess
    private void paintBusy(boolean isBusy) {
        Label emptyText = myEmptyText;
        if (emptyText != null) {
            emptyText.setText(isBusy ? CommonLocalize.treeNodeLoading() : VcsLocalize.commitDialogNoChangesDetectedText());
        }
        PlaceholderLayout treeContent = myTreeContent;
        if (treeContent != null) {
            treeContent.testPlaceholder();
        }
    }

    @RequiredUIAccess
    private void updateTree(boolean doInitCheck) {
        UnifiedChangesTree tree = myChangesTree;
        if (tree == null) {
            return;
        }

        List<AbstractFilePatchInProgress.PatchChange> changes = getAllChanges();
        Collection<AbstractFilePatchInProgress.PatchChange> included = getIncluded(doInitCheck, changes);

        ChangesTreeSnapshot snapshot = new ChangesTreeSnapshotBuilder(myProject, myShowFlatten)
            .changes(new ArrayList<>(changes))
            .build();
        CompletableFuture<?> refresh = tree.setSnapshot(snapshot);
        tree.setIncluded(included);
        if (doInitCheck) {
            UIAccess uiAccess = UIAccess.current();
            refresh.whenComplete((o, e) -> uiAccess.give(() -> tree.getComponent().expandAll()));
        }

        myContainBasedChanges = false;
        for (AbstractFilePatchInProgress patch : myPatches) {
            if (patch.baseExistsOrAdded()) {
                myContainBasedChanges = true;
                break;
            }
        }

        PlaceholderLayout treeContent = myTreeContent;
        if (treeContent != null) {
            treeContent.testPlaceholder();
        }
    }

    private List<AbstractFilePatchInProgress.PatchChange> getAllChanges() {
        List<AbstractFilePatchInProgress.PatchChange> changes = new ArrayList<>(myPatches.size());
        for (AbstractFilePatchInProgress patch : myPatches) {
            changes.add(patch.getChange());
        }
        return changes;
    }

    private List<AbstractFilePatchInProgress.PatchChange> getSelectedChanges() {
        UnifiedChangesTree tree = myChangesTree;
        if (tree == null) {
            return List.of();
        }
        return toPatchChanges(tree.getSelectedChanges());
    }

    private List<AbstractFilePatchInProgress.PatchChange> getIncludedChanges() {
        UnifiedChangesTree tree = myChangesTree;
        if (tree == null) {
            return List.of();
        }
        return toPatchChanges(tree.getIncludedChanges());
    }

    private static List<AbstractFilePatchInProgress.PatchChange> toPatchChanges(Collection<Change> changes) {
        List<AbstractFilePatchInProgress.PatchChange> result = new ArrayList<>(changes.size());
        for (Change change : changes) {
            if (change instanceof AbstractFilePatchInProgress.PatchChange patchChange) {
                result.add(patchChange);
            }
        }
        return result;
    }

    @RequiredUIAccess
    private void inclusionChanged() {
        int[] included = new int[LEGEND_SIZE];
        Set<Couple<String>> seen = new HashSet<>();
        for (AbstractFilePatchInProgress.PatchChange change : getIncludedChanges()) {
            FilePatch patch = change.getPatchInProgress().getPatch();
            if (seen.add(Couple.of(patch.getBeforeName(), patch.getAfterName()))) {
                acceptChange(included, change);
            }
        }

        int[] total = new int[LEGEND_SIZE];
        for (AbstractFilePatchInProgress patch : myPatches) {
            acceptChange(total, patch.getChange());
        }
        updateLegend(total, included);
        updateOkActions();
    }

    @RequiredUIAccess
    private void updateLegend(int[] total, int[] included) {
        ChangesLegend legend = myLegend;
        if (legend == null) {
            return;
        }
        legend.setCount(ADDED, total[ADDED], included[ADDED]);
        legend.setCount(MODIFIED, total[MODIFIED], included[MODIFIED]);
        legend.setCount(DELETED, total[DELETED], included[DELETED]);
        legend.setCount(INAPPLICABLE, total[INAPPLICABLE], total[INAPPLICABLE]);
    }

    private static void acceptChange(int[] nameStatuses, AbstractFilePatchInProgress.PatchChange change) {
        AbstractFilePatchInProgress patchInProgress = change.getPatchInProgress();
        if (FilePatchStatus.ADDED.equals(patchInProgress.getStatus())) {
            nameStatuses[ADDED]++;
        }
        else if (FilePatchStatus.DELETED.equals(patchInProgress.getStatus())) {
            nameStatuses[DELETED]++;
        }
        else {
            nameStatuses[MODIFIED]++;
        }
        if (!patchInProgress.baseExistsOrAdded()) {
            nameStatuses[INAPPLICABLE]++;
        }
    }

    private Collection<AbstractFilePatchInProgress.PatchChange> getIncluded(
        boolean doInitCheck,
        List<AbstractFilePatchInProgress.PatchChange> changes
    ) {
        List<AbstractFilePatchInProgress.PatchChange> included = new ArrayList<>();
        if (doInitCheck) {
            for (AbstractFilePatchInProgress.PatchChange change : changes) {
                AbstractFilePatchInProgress patchInProgress = change.getPatchInProgress();
                if (patchInProgress.baseExistsOrAdded() && (myPreselectedChanges == null || myPreselectedChanges.contains(change))) {
                    included.add(change);
                }
            }
        }
        else {
            Set<AbstractFilePatchInProgress> toBeIncluded = new HashSet<>();
            for (AbstractFilePatchInProgress.PatchChange change : getIncludedChanges()) {
                toBeIncluded.add(change.getPatchInProgress());
            }
            for (AbstractFilePatchInProgress.PatchChange change : changes) {
                AbstractFilePatchInProgress patch = change.getPatchInProgress();
                if (toBeIncluded.contains(patch) && patch.baseExistsOrAdded()) {
                    included.add(change);
                }
            }
        }
        return included;
    }

    private void decorate(Change change, TextItemPresentation presentation) {
        if (!(change instanceof AbstractFilePatchInProgress.PatchChange patchChange)) {
            return;
        }
        AbstractFilePatchInProgress patchInProgress = patchChange.getPatchInProgress();
        if (patchInProgress.getCurrentStrip() > 0) {
            presentation.append(LocalizeValue.of(" "));
            presentation.append(
                VcsLocalize.patchApplyStrippedDescription(patchInProgress.getCurrentStrip()),
                new TextAttribute(TextAttribute.STYLE_ITALIC, ComponentColors.DISABLED_TEXT)
            );
        }
        LocalizeValue status;
        if (FilePatchStatus.ADDED.equals(patchInProgress.getStatus())) {
            status = VcsLocalize.patchApplyAddedStatus();
        }
        else if (FilePatchStatus.DELETED.equals(patchInProgress.getStatus())) {
            status = VcsLocalize.patchApplyDeletedStatus();
        }
        else {
            status = VcsLocalize.patchApplyModifiedStatus();
        }
        presentation.append(LocalizeValue.of("   "));
        presentation.append(status, TextAttribute.GRAYED);
        if (!patchInProgress.baseExistsOrAdded()) {
            presentation.append(LocalizeValue.of("  "));
            presentation.append(
                VcsLocalize.patchApplySelectMissingBaseLink(),
                new TextAttribute(TextAttribute.STYLE_PLAIN, ComponentColors.LINK_FOREGROUND)
            );
        }
        else if (!patchInProgress.getStatus().equals(FilePatchStatus.ADDED) && basePathWasChanged(patchInProgress)) {
            presentation.append(LocalizeValue.of("  "));
            presentation.append(
                VcsLocalize.patchApplyNewBaseDetectedNodeDescription(),
                new TextAttribute(TextAttribute.STYLE_ITALIC, ComponentColors.DISABLED_TEXT)
            );
            presentation.withTooltip(VcsLocalize.patchApplyOldNewBaseInfo(
                patchInProgress.getOriginalBeforePath(),
                String.valueOf(myProject.getBasePath()),
                patchInProgress.getPatch().getBeforeName(),
                patchInProgress.getBase().getPath()
            ));
        }
    }

    private boolean basePathWasChanged(AbstractFilePatchInProgress patchInProgress) {
        return !FileUtil.filesEqual(
            patchInProgress.getIoCurrentBase(),
            new File(myProject.getBasePath(), patchInProgress.getOriginalBeforePath())
        );
    }

    private static boolean sameBase(List<AbstractFilePatchInProgress.PatchChange> selectedChanges) {
        VirtualFile base = null;
        for (AbstractFilePatchInProgress.PatchChange change : selectedChanges) {
            VirtualFile changeBase = change.getPatchInProgress().getBase();
            if (base == null) {
                base = changeBase;
            }
            else if (!base.equals(changeBase)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Captures what the executor applies while the dialog still shows it - the executor itself runs once the dialog
     * is gone, since applying may ask questions of its own.
     */
    @RequiredUIAccess
    private void runExecutorAfterClose(ApplyPatchExecutor executor) {
        Runnable apply = prepareExecutor(executor);
        if (apply != null) {
            UIAccess.current().give(apply);
        }
    }

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    private @Nullable Runnable prepareExecutor(ApplyPatchExecutor executor) {
        List<AbstractFilePatchInProgress> included = new ArrayList<>();
        for (AbstractFilePatchInProgress.PatchChange change : getIncludedChanges()) {
            included.add(change.getPatchInProgress());
        }
        if (included.isEmpty()) {
            return null;
        }
        MultiMap<VirtualFile, AbstractFilePatchInProgress> patchGroups = new MultiMap<>();
        for (AbstractFilePatchInProgress patchInProgress : included) {
            patchGroups.putValue(patchInProgress.getBase(), patchInProgress);
        }
        ChangeListChooserPanel chooser = myChangeListChooser;
        LocalChangeList selected = chooser == null ? null : chooser.getSelectedList(myProject);
        FilePresentationModel presentation = myRecentPathFileChange.get();
        VirtualFile vf = presentation != null ? presentation.getVf() : null;
        List<FilePatch> remaining = getOriginalRemaining(included);
        PatchReader reader = myReader;
        return () -> executor.apply(
            remaining,
            patchGroups,
            selected,
            vf == null ? null : vf.getName(),
            reader == null ? null : reader.getAdditionalInfo(ApplyPatchDefaultExecutor.pathsFromGroups(patchGroups))
        );
    }

    private List<FilePatch> getOriginalRemaining(Collection<AbstractFilePatchInProgress> included) {
        Collection<AbstractFilePatchInProgress> notIncluded = ContainerUtil.subtract(myPatches, included);
        List<FilePatch> remainingOriginal = new ArrayList<>();
        for (AbstractFilePatchInProgress progress : notIncluded) {
            progress.reset();
            remainingOriginal.add(progress.getPatch());
        }
        return remainingOriginal;
    }

    @RequiredUIAccess
    private void showDiff() {
        if (ChangeListManager.getInstance(myProject).isFreezedWithNotification(null)) {
            return;
        }
        if (myPatches.isEmpty() || !myContainBasedChanges) {
            return;
        }
        List<AbstractFilePatchInProgress.PatchChange> changes = getAllChanges();
        UnifiedChangesTree tree = myChangesTree;
        if (tree != null) {
            Map<Change, Integer> treeOrder = new HashMap<>();
            for (Change change : tree.getAllChanges()) {
                treeOrder.putIfAbsent(change, treeOrder.size());
            }
            changes.sort(Comparator.comparingInt(change -> treeOrder.getOrDefault(change, Integer.MAX_VALUE)));
        }
        else {
            changes.sort(this::compareChanges);
        }
        List<AbstractFilePatchInProgress.PatchChange> selectedChanges = getSelectedChanges();
        if (selectedChanges.isEmpty()) {
            selectedChanges = changes;
        }

        int selectedIdx = 0;
        List<DiffRequestProducer> diffRequestPresentableList = new ArrayList<>(changes.size());
        if (!selectedChanges.isEmpty()) {
            AbstractFilePatchInProgress selectedPatch = selectedChanges.get(0).getPatchInProgress();
            for (AbstractFilePatchInProgress.PatchChange change : changes) {
                AbstractFilePatchInProgress patchInProgress = change.getPatchInProgress();
                if (!patchInProgress.baseExistsOrAdded()) {
                    diffRequestPresentableList.add(createBaseNotFoundErrorRequest(patchInProgress));
                }
                else {
                    diffRequestPresentableList.add(patchInProgress.getDiffRequestProducers(myProject, myReader));
                }
                if (patchInProgress == selectedPatch) {
                    selectedIdx = diffRequestPresentableList.size() - 1;
                }
            }
        }
        if (diffRequestPresentableList.isEmpty()) {
            return;
        }
        MyDiffRequestChain chain = new MyDiffRequestChain(diffRequestPresentableList, changes, selectedIdx);
        DiffManager.getInstance().showDiff(myProject, chain, DiffDialogHints.DEFAULT);
    }

    private int compareChanges(AbstractFilePatchInProgress.PatchChange o1, AbstractFilePatchInProgress.PatchChange o2) {
        if (myShowFlatten) {
            return o1.getPatchInProgress().getIoCurrentBase().getName().compareTo(o2.getPatchInProgress().getIoCurrentBase().getName());
        }
        return FileUtil.compareFiles(o1.getPatchInProgress().getIoCurrentBase(), o2.getPatchInProgress().getIoCurrentBase());
    }

    private static DiffRequestProducer createBaseNotFoundErrorRequest(AbstractFilePatchInProgress patchInProgress) {
        String beforePath = patchInProgress.getPatch().getBeforeName();
        String afterPath = patchInProgress.getPatch().getAfterName();
        return new DiffRequestProducer() {
            @Override
            public String getName() {
                File ioCurrentBase = patchInProgress.getIoCurrentBase();
                return ioCurrentBase == null ? patchInProgress.getCurrentPath() : ioCurrentBase.getPath();
            }

            @Override
            public DiffRequest process(UserDataHolder context, ProgressIndicator indicator)
                throws DiffRequestProducerException, ProcessCanceledException {
                throw new DiffRequestProducerException(
                    VcsLocalize.changesErrorCannotFindBaseForPath(beforePath != null ? beforePath : afterPath).get()
                );
            }
        };
    }

    private static class FilePresentationModel {
        private final String myPath;
        private @Nullable VirtualFile myVf;

        private FilePresentationModel(String path) {
            myPath = path;
            myVf = null;
        }

        private FilePresentationModel(VirtualFile file) {
            myPath = file.getPath();
            myVf = file;
        }

        public @Nullable VirtualFile getVf() {
            if (myVf == null) {
                VirtualFile file = VirtualFileUtil.findFileByIoFile(new File(myPath), true);
                myVf = file != null && !file.isDirectory() ? file : null;
            }
            return myVf;
        }
    }

    private final class ExecutorAction extends DumbAwareAction {
        private final ApplyPatchExecutor myExecutor;

        private ExecutorAction(ApplyPatchExecutor executor) {
            super(LocalizeValue.of(executor.getName()));
            myExecutor = executor;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            Dialog dialog = e.getRequiredData(Dialog.KEY);
            runExecutorAfterClose(myExecutor);
            dialog.doOkAction(DialogValue.OK_VALUE);
        }
    }

    private final class NewBaseSelector implements Runnable {
        private final boolean myDirectorySelector;

        private NewBaseSelector(boolean directorySelector) {
            myDirectorySelector = directorySelector;
        }

        @Override
        @RequiredUIAccess
        public void run() {
            FileChooserDescriptor descriptor = myDirectorySelector
                ? FileChooserDescriptorFactory.createSingleFolderDescriptor()
                : FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor();
            descriptor.withTitle(myDirectorySelector ? VcsLocalize.patchApplySelectBaseDirectoryTitle() : VcsLocalize.patchApplySelectBaseFileTitle());

            List<AbstractFilePatchInProgress.PatchChange> selectedChanges = getSelectedChanges();
            FileChooser.chooseFile(descriptor, myProject, null).whenCompleteAsync((selectedFile, error) -> {
                if (error != null || selectedFile == null || selectedChanges.isEmpty()) {
                    return;
                }
                for (AbstractFilePatchInProgress.PatchChange patchChange : selectedChanges) {
                    AbstractFilePatchInProgress patch = patchChange.getPatchInProgress();
                    if (myDirectorySelector) {
                        patch.setNewBase(selectedFile);
                    }
                    else {
                        FilePatch filePatch = patch.getPatch();
                        filePatch.setAfterName(selectedFile.getName());
                        filePatch.setBeforeName(selectedFile.getName());
                        patch.setNewBase(selectedFile.getParent());
                    }
                }
                updateTree(false);
            }, UIAccess.current());
        }
    }

    private final class MapDirectory extends DumbAwareAction implements AnActionWithSyncUpdate {
        private MapDirectory() {
            super(VcsLocalize.patchApplyMapBaseDirectoryAction(), LocalizeValue.empty(), PlatformIconGroup.vcsMapbase());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            List<AbstractFilePatchInProgress.PatchChange> selectedChanges = getSelectedChanges();
            if (selectedChanges.isEmpty() || !sameBase(selectedChanges)) {
                return;
            }
            AbstractFilePatchInProgress patch = selectedChanges.get(0).getPatchInProgress();
            List<VirtualFile> autoBases = patch.getAutoBasesCopy();
            if (autoBases.isEmpty() || autoBases.size() == 1 && autoBases.get(0).equals(patch.getBase())) {
                new NewBaseSelector(true).run();
            }
            else {
                autoBases.add(null);
                JBPopupFactory.getInstance().createListPopup(new MapPopup(autoBases, selectedChanges)).showCenteredInCurrentWindow(myProject);
            }
        }

        @Override
        public void update(AnActionEvent e) {
            List<AbstractFilePatchInProgress.PatchChange> selectedChanges = getSelectedChanges();
            e.getPresentation().setEnabled(!selectedChanges.isEmpty() && sameBase(selectedChanges));
        }
    }

    private final class MapPopup extends BaseListPopupStep<@Nullable VirtualFile> {
        private final List<AbstractFilePatchInProgress.PatchChange> mySelectedChanges;

        private MapPopup(List<@Nullable VirtualFile> values, List<AbstractFilePatchInProgress.PatchChange> selectedChanges) {
            super(VcsLocalize.patchApplySelectBaseDirectoryForAPathPopup().get(), values);
            mySelectedChanges = selectedChanges;
        }

        @Override
        public boolean isSpeedSearchEnabled() {
            return true;
        }

        @Override
        @RequiredUIAccess
        public @Nullable PopupStep onChosen(@Nullable VirtualFile selectedValue, boolean finalChoice) {
            if (selectedValue == null) {
                return doFinalStep(() -> new NewBaseSelector(true).run());
            }
            for (AbstractFilePatchInProgress.PatchChange patchChange : mySelectedChanges) {
                patchChange.getPatchInProgress().setNewBase(selectedValue);
            }
            updateTree(false);
            return FINAL_CHOICE;
        }

        @Override
        public String getTextFor(@Nullable VirtualFile value) {
            return value == null ? VcsLocalize.patchApplySelectBaseForAPathMessage().get() : value.getPath();
        }
    }

    private class StripUp extends DumbAwareAction implements AnActionWithSyncUpdate {
        private StripUp(LocalizeValue text) {
            super(text);
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(ContainerUtil.exists(getSelectedChanges(), change -> change.getPatchInProgress().canUp()));
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            getSelectedChanges().forEach(change -> change.getPatchInProgress().up());
            updateTree(false);
        }
    }

    private final class ZeroStrip extends StripUp {
        private ZeroStrip() {
            super(VcsLocalize.patchApplyRemoveAllLeadingDirectoriesAction());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            getSelectedChanges().forEach(change -> change.getPatchInProgress().setZero());
            updateTree(false);
        }
    }

    private class StripDown extends DumbAwareAction implements AnActionWithSyncUpdate {
        private StripDown(LocalizeValue text) {
            super(text);
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(ContainerUtil.exists(getSelectedChanges(), change -> change.getPatchInProgress().canDown()));
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            getSelectedChanges().forEach(change -> change.getPatchInProgress().down());
            updateTree(false);
        }
    }

    private final class ResetStrip extends StripDown {
        private ResetStrip() {
            super(VcsLocalize.patchApplyRestoreAllLeadingDirectoriesAction());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            getSelectedChanges().forEach(change -> change.getPatchInProgress().reset());
            updateTree(false);
        }
    }

    private final class MyShowDiff extends DumbAwareAction implements AnActionWithSyncUpdate {
        private MyShowDiff() {
            super(VcsLocalize.actionNameShowDifference(), LocalizeValue.empty(), PlatformIconGroup.actionsDiff());
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(!myPatches.isEmpty() && myContainBasedChanges);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            showDiff();
        }
    }

    private static class MyDiffRequestChain extends UserDataHolderBase implements DiffRequestChain, GoToChangePopupBuilder.Chain {
        private final List<DiffRequestProducer> myRequests;
        private final List<? extends Change> myChanges;
        private int myIndex;

        private MyDiffRequestChain(List<DiffRequestProducer> requests, List<? extends Change> changes, int index) {
            myRequests = requests;
            myChanges = changes;
            myIndex = index >= 0 ? index : 0;
        }

        @Override
        public List<? extends DiffRequestProducer> getRequests() {
            return myRequests;
        }

        @Override
        public int getIndex() {
            return myIndex;
        }

        @Override
        public void setIndex(int index) {
            assert index >= 0 && index < myRequests.size();
            myIndex = index;
        }

        @Override
        public AnAction createGoToChangeAction(Consumer<Integer> onSelected) {
            return new ChangeGoToChangePopupAction.Fake<>(this, myIndex, onSelected) {
                @Override
                protected FilePath getFilePath(int index) {
                    return ChangesUtil.getFilePath(myChanges.get(index));
                }

                @Override
                protected FileStatus getFileStatus(int index) {
                    return myChanges.get(index).getFileStatus();
                }
            };
        }
    }
}
