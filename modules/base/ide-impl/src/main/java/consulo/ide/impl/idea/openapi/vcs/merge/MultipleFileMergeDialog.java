/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.openapi.vcs.merge;

import consulo.application.Application;
import consulo.diff.DiffManager;
import consulo.diff.DiffRequestFactory;
import consulo.diff.InvalidDiffRequestException;
import consulo.diff.impl.internal.util.DiffImplUtil;
import consulo.diff.merge.MergeRequest;
import consulo.diff.merge.MergeResult;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.StoreReloadManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.table.ListTableModel;
import consulo.ui.ex.awt.table.TableView;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.SmartList;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.VcsDirtyScopeManager;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.merge.*;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFilePresentation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author yole
 */
public class MultipleFileMergeDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(MultipleFileMergeDialog.class);

    private JPanel myRootPanel;
    private JButton myAcceptYoursButton;
    private JButton myAcceptTheirsButton;
    private JButton myMergeButton;
    private TableView<VirtualFile> myTable;
    private JBLabel myDescriptionLabel;
    private final MergeProvider myProvider;
    private final MergeSession myMergeSession;
    private final List<VirtualFile> myFiles;
    private final ListTableModel<VirtualFile> myModel;
    @Nullable
    private final Project myProject;
    private final StoreReloadManager myStoreReloadManager;
    private final List<VirtualFile> myProcessedFiles = new SmartList<>();
    private final Set<VirtualFile> myBinaryFiles = new HashSet<>();
    private final MergeDialogCustomizer myMergeDialogCustomizer;

    private final VirtualFileRenderer myVirtualFileRenderer = new VirtualFileRenderer();

    public MultipleFileMergeDialog(
        @Nullable Project project,
        @Nonnull List<VirtualFile> files,
        @Nonnull MergeProvider provider,
        @Nonnull MergeDialogCustomizer mergeDialogCustomizer
    ) {
        super(project);

        myProject = project;
        myStoreReloadManager = StoreReloadManager.getInstance(Objects.requireNonNull(project));
        myStoreReloadManager.blockReloadingProjectOnExternalChanges();
        myFiles = new ArrayList<>(files);
        myProvider = provider;
        myMergeDialogCustomizer = mergeDialogCustomizer;

        String description = myMergeDialogCustomizer.getMultipleFileMergeDescription(files);
        if (!StringUtil.isEmptyOrSpaces(description)) {
            myDescriptionLabel.setText(description);
        }

        List<ColumnInfo> columns = new ArrayList<>();
        columns.add(new ColumnInfo<VirtualFile, VirtualFile>(VcsLocalize.multipleFileMergeColumnName().get()) {
            @Override
            public VirtualFile valueOf(VirtualFile virtualFile) {
                return virtualFile;
            }

            @Override
            public TableCellRenderer getRenderer(VirtualFile virtualFile) {
                return myVirtualFileRenderer;
            }
        });
        columns.add(new ColumnInfo<VirtualFile, String>(VcsLocalize.multipleFileMergeColumnType().get()) {
            @Override
            public String valueOf(VirtualFile virtualFile) {
                return myBinaryFiles.contains(virtualFile)
                    ? VcsLocalize.multipleFileMergeTypeBinary().get()
                    : VcsLocalize.multipleFileMergeTypeText().get();
            }

            @Override
            public String getMaxStringValue() {
                return VcsLocalize.multipleFileMergeTypeBinary().get();
            }

            @Override
            public int getAdditionalWidth() {
                return 10;
            }
        });
        if (myProvider instanceof MergeProvider2 mergeProvider2) {
            myMergeSession = mergeProvider2.createMergeSession(files);
            Collections.addAll(columns, myMergeSession.getMergeInfoColumns());
        }
        else {
            myMergeSession = null;
        }
        myModel = new ListTableModel<>(columns.toArray(new ColumnInfo[columns.size()]));
        myModel.setItems(files);
        myTable.setModelAndUpdateColumns(myModel);
        myVirtualFileRenderer.setFont(UIUtil.getListFont());
        myTable.setRowHeight(myVirtualFileRenderer.getPreferredSize().height);
        setTitle(myMergeDialogCustomizer.getMultipleFileDialogTitle());
        init();
        myAcceptYoursButton.addActionListener(e -> acceptRevision(true));
        myAcceptTheirsButton.addActionListener(e -> acceptRevision(false));
        myTable.getSelectionModel().addListSelectionListener(e -> updateButtonState());
        for (VirtualFile file : files) {
            if (file.getFileType().isBinary() || provider.isBinary(file)) {
                myBinaryFiles.add(file);
            }
        }
        myTable.getSelectionModel().setSelectionInterval(0, 0);
    }

    private void updateButtonState() {
        boolean haveSelection = myTable.getSelectedRowCount() > 0;
        boolean haveUnmergeableFiles = false;
        for (VirtualFile file : myTable.getSelection()) {
            if (myMergeSession != null) {
                boolean canMerge = myMergeSession.canMerge(file);
                if (!canMerge) {
                    haveUnmergeableFiles = true;
                    break;
                }
            }
        }
        myAcceptYoursButton.setEnabled(haveSelection);
        myAcceptTheirsButton.setEnabled(haveSelection);
        myMergeButton.setEnabled(haveSelection && !haveUnmergeableFiles);
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        return myRootPanel;
    }

    @Nonnull
    @Override
    protected Action[] createActions() {
        return new Action[]{getCancelAction()};
    }

    @Nonnull
    @Override
    protected LocalizeAction getCancelAction() {
        LocalizeAction action = super.getCancelAction();
        action.setText(CommonLocalize.buttonClose());
        return action;
    }

    @Override
    protected void dispose() {
        myStoreReloadManager.unblockReloadingProjectOnExternalChanges();
        super.dispose();
    }

    protected boolean beforeResolve(Collection<VirtualFile> files) {
        return true;
    }

    @Override
    protected String getDimensionServiceKey() {
        return "MultipleFileMergeDialog";
    }

    @RequiredUIAccess
    private void acceptRevision(boolean isCurrent) {
        FileDocumentManager.getInstance().saveAllDocuments();
        Collection<VirtualFile> files = myTable.getSelection();
        if (!beforeResolve(files)) {
            return;
        }

        for (VirtualFile file : files) {
            Exception ex = CommandProcessor.getInstance().<Exception>newCommand()
                .project(myProject)
                .name(
                    isCurrent
                        ? VcsLocalize.multipleFileMergeAcceptYours().map(Presentation.NO_MNEMONIC)
                        : VcsLocalize.multipleFileMergeAcceptTheirs().map(Presentation.NO_MNEMONIC)
                )
                .inWriteAction()
                .compute(() -> {
                    try {
                        if (!(myProvider instanceof MergeProvider2) || myMergeSession.canMerge(file)) {
                            if (!DiffImplUtil.makeWritable(myProject, file)) {
                                throw new IOException("File is read-only: " + file.getPresentableName());
                            }
                            MergeData data = myProvider.loadRevisions(file);
                            if (isCurrent) {
                                file.setBinaryContent(data.CURRENT);
                            }
                            else {
                                file.setBinaryContent(data.LAST);
                                checkMarkModifiedProject(file);
                            }
                        }
                        markFileProcessed(file, isCurrent ? MergeSession.Resolution.AcceptedYours : MergeSession.Resolution.AcceptedTheirs);
                        return null;
                    }
                    catch (Exception e) {
                        return e;
                    }
                });
            if (ex != null) {
                //noinspection ThrowableResultOfMethodCallIgnored
                Messages.showErrorDialog(myRootPanel, "Error saving merged data: " + ex.getMessage());
                break;
            }
        }
        updateModelFromFiles();
    }

    private void markFileProcessed(@Nonnull VirtualFile file, @Nonnull MergeSession.Resolution resolution) {
        myFiles.remove(file);
        if (myProvider instanceof MergeProvider2) {
            myMergeSession.conflictResolvedForFile(file, resolution);
        }
        else {
            myProvider.conflictResolvedForFile(file);
        }
        myProcessedFiles.add(file);
        if (myProject != null) {
            VcsDirtyScopeManager.getInstance(myProject).fileDirty(file);
        }
    }

    private void updateModelFromFiles() {
        if (myFiles.isEmpty()) {
            doCancelAction();
        }
        else {
            int selIndex = myTable.getSelectionModel().getMinSelectionIndex();
            myModel.setItems(myFiles);
            if (selIndex >= myFiles.size()) {
                selIndex = myFiles.size() - 1;
            }
            myTable.getSelectionModel().setSelectionInterval(selIndex, selIndex);
        }
    }

    @RequiredUIAccess
    private void showMergeDialog() {
        DiffRequestFactory requestFactory = DiffRequestFactory.getInstance();
        Collection<VirtualFile> files = myTable.getSelection();
        if (!beforeResolve(files)) {
            return;
        }

        for (VirtualFile file : files) {
            MergeData mergeData;
            try {
                mergeData = myProvider.loadRevisions(file);
            }
            catch (VcsException ex) {
                Messages.showErrorDialog(myRootPanel, "Error loading revisions to merge: " + ex.getMessage());
                break;
            }

            if (mergeData.CURRENT == null || mergeData.LAST == null || mergeData.ORIGINAL == null) {
                Messages.showErrorDialog(myRootPanel, "Error loading revisions to merge");
                break;
            }

            String leftTitle = myMergeDialogCustomizer.getLeftPanelTitle(file);
            String baseTitle = myMergeDialogCustomizer.getCenterPanelTitle(file);
            String rightTitle = myMergeDialogCustomizer.getRightPanelTitle(file, mergeData.LAST_REVISION_NUMBER);
            String title = myMergeDialogCustomizer.getMergeWindowTitle(file);

            List<byte[]> byteContents = ContainerUtil.list(mergeData.CURRENT, mergeData.ORIGINAL, mergeData.LAST);
            List<String> contentTitles = ContainerUtil.list(leftTitle, baseTitle, rightTitle);

            Consumer<MergeResult> callback = result -> {
                Document document = FileDocumentManager.getInstance().getCachedDocument(file);
                if (document != null) {
                    FileDocumentManager.getInstance().saveDocument(document);
                }
                checkMarkModifiedProject(file);

                if (result != MergeResult.CANCEL) {
                    Application.get().runWriteAction(() -> markFileProcessed(file, getSessionResolution(result)));
                }
            };

            MergeRequest request;
            try {
                if (myProvider.isBinary(file)) { // respect MIME-types in svn
                    request = requestFactory.createBinaryMergeRequest(myProject, file, byteContents, title, contentTitles, callback);
                }
                else {
                    request = requestFactory.createMergeRequest(myProject, file, byteContents, title, contentTitles, callback);
                }
            }
            catch (InvalidDiffRequestException e) {
                LOG.error(e);
                Messages.showErrorDialog(myRootPanel, "Can't show merge dialog");
                break;
            }

            DiffManager.getInstance().showMerge(myProject, request);
        }
        updateModelFromFiles();
    }

    @Nonnull
    private static MergeSession.Resolution getSessionResolution(@Nonnull MergeResult result) {
        return switch (result) {
            case LEFT -> MergeSession.Resolution.AcceptedYours;
            case RIGHT -> MergeSession.Resolution.AcceptedTheirs;
            case RESOLVED -> MergeSession.Resolution.Merged;
            default -> throw new IllegalArgumentException(result.name());
        };
    }

    private void checkMarkModifiedProject(@Nonnull VirtualFile file) {
    }

    private void createUIComponents() {
        Action mergeAction = new AbstractAction() {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull ActionEvent e) {
                showMergeDialog();
            }
        };
        mergeAction.putValue(DEFAULT_ACTION, Boolean.TRUE);
        myMergeButton = createJButtonForAction(mergeAction);
    }

    @Override
    @RequiredUIAccess
    public JComponent getPreferredFocusedComponent() {
        return myTable;
    }

    @Nonnull
    public List<VirtualFile> getProcessedFiles() {
        return myProcessedFiles;
    }

    private static class VirtualFileRenderer extends ColoredTableCellRenderer {
        @Override
        protected void customizeCellRenderer(JTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
            VirtualFile vf = (VirtualFile)value;
            setIcon(VirtualFilePresentation.getIcon(vf));
            append(vf.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            VirtualFile parent = vf.getParent();
            if (parent != null) {
                append(" (" + FileUtil.toSystemDependentName(parent.getPresentableUrl()) + ")", SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }
        }
    }
}
