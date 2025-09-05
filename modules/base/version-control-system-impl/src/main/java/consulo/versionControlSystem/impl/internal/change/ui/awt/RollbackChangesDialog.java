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
package consulo.versionControlSystem.impl.internal.change.ui.awt;

import consulo.application.ApplicationPropertiesComponent;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.collection.ContainerUtil;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.impl.internal.change.ui.RollbackWorker;
import consulo.versionControlSystem.impl.internal.ui.awt.InternalChangesBrowser;
import consulo.versionControlSystem.impl.internal.util.RollbackUtil;
import consulo.versionControlSystem.internal.ChangeListManagerEx;
import consulo.versionControlSystem.localize.VcsLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author max
 */
public class RollbackChangesDialog extends DialogWrapper {
    public static final String DELETE_LOCALLY_ADDED_FILES_KEY = "delete.locally.added.files";
    private final Project myProject;
    private final Runnable myAfterVcsRefreshInAwt;
    private final InternalChangesBrowser myBrowser;
    private final boolean myInvokedFromModalContext;
    private final CheckBox myDeleteLocallyAddedFiles;
    private final ChangeInfoCalculator myInfoCalculator;
    private final CommitLegendPanel myCommitLegendPanel;
    private String myOperationName;

    @RequiredUIAccess
    public static void rollbackChanges(Project project, Collection<Change> changes) {
        rollbackChanges(project, changes, true, null);
    }

    @RequiredUIAccess
    public static void rollbackChanges(
        Project project, Collection<Change> changes,
        boolean refreshSynchronously,
        Runnable afterVcsRefreshInAwt
    ) {
        ChangeListManagerEx manager = (ChangeListManagerEx) ChangeListManager.getInstance(project);

        if (changes.isEmpty()) {
            showNoChangesDialog(project);
            return;
        }

        ArrayList<Change> validChanges = new ArrayList<>();
        Set<LocalChangeList> lists = new HashSet<>();
        lists.addAll(manager.getInvolvedListsFilterChanges(changes, validChanges));

        new RollbackChangesDialog(project, new ArrayList<>(lists), validChanges, refreshSynchronously, afterVcsRefreshInAwt).show();
    }

    @RequiredUIAccess
    public static void rollbackChanges(Project project, LocalChangeList changeList) {
        List<Change> changes = new ArrayList<>(changeList.getChanges());

        if (changes.isEmpty()) {
            showNoChangesDialog(project);
            return;
        }

        new RollbackChangesDialog(project, Collections.singletonList(changeList), Collections.<Change>emptyList(), true, null).show();
    }

    @RequiredUIAccess
    private static void showNoChangesDialog(Project project) {
        String operationName = UIUtil.removeMnemonic(RollbackUtil.getRollbackOperationName(project));
        Messages.showWarningDialog(
            project,
            VcsLocalize.commitDialogNoChangesDetectedText().get(),
            VcsLocalize.changesActionRollbackNothing(operationName).get()
        );
    }

    @RequiredUIAccess
    public RollbackChangesDialog(
        final Project project,
        final List<LocalChangeList> changeLists,
        final List<Change> changes,
        boolean refreshSynchronously,
        Runnable afterVcsRefreshInAwt
    ) {
        super(project, true);

        myProject = project;
        myAfterVcsRefreshInAwt = afterVcsRefreshInAwt;
        myInvokedFromModalContext = UIAccess.current().isInModalContext();

        myInfoCalculator = new ChangeInfoCalculator();
        myCommitLegendPanel = new CommitLegendPanel(myInfoCalculator);
        Runnable listChangeListener = new Runnable() {
            @Override
            public void run() {
                if (myBrowser != null) {
                    // We could not utilize "myBrowser.getViewer().getChanges()" here (to get all changes) as currently it is not recursive.
                    List<Change> allChanges = getAllChanges(changeLists);
                    Collection<Change> includedChanges = myBrowser.getViewer().getIncludedChanges();

                    myInfoCalculator.update(allChanges, new ArrayList<>(includedChanges));
                    myCommitLegendPanel.update();

                    boolean hasNewFiles = ContainerUtil.exists(includedChanges, change -> change.getType() == Change.Type.NEW);
                    myDeleteLocallyAddedFiles.setEnabled(hasNewFiles);
                }
            }
        };
        myBrowser =
            new InternalChangesBrowser(project, changeLists, changes, null, true, true, listChangeListener, InternalChangesBrowser.MyUseCase.LOCAL_CHANGES, null) {
                @Nonnull
                @Override
                protected DefaultTreeModel buildTreeModel(List<Change> changes, ChangeNodeDecorator changeNodeDecorator, boolean showFlatten) {
                    TreeModelBuilder builder = new TreeModelBuilder(myProject, showFlatten);
                    // Currently we do not explicitly utilize passed "changeNodeDecorator" instance (which is defined by
                    // "ChangesBrowser.MyUseCase.LOCAL_CHANGES" parameter passed to "ChangesBrowser"). But correct node decorator will still be set
                    // in "TreeModelBuilder.setChangeLists()".
                    return builder.setChangeLists(changeLists).build();
                }
            };
        Disposer.register(getDisposable(), myBrowser);

        myOperationName = operationNameByChanges(project, getAllChanges(changeLists));
        setOKButtonText(myOperationName);

        myOperationName = UIUtil.removeMnemonic(myOperationName);
        setTitle(VcsLocalize.changesActionRollbackCustomTitle(myOperationName));
        setCancelButtonText(CommonLocalize.buttonClose().get());
        myBrowser.setToggleActionTitle(LocalizeValue.localizeTODO("&Include in " + myOperationName.toLowerCase()));

        myDeleteLocallyAddedFiles = CheckBox.create(VcsLocalize.changesCheckboxDeleteLocallyAddedFiles());
        myDeleteLocallyAddedFiles.setValue(ApplicationPropertiesComponent.getInstance().isTrueValue(DELETE_LOCALLY_ADDED_FILES_KEY));
        myDeleteLocallyAddedFiles.addValueListener(
            e -> ApplicationPropertiesComponent.getInstance().setValue(DELETE_LOCALLY_ADDED_FILES_KEY, myDeleteLocallyAddedFiles.getValueOrError())
        );

        init();
        listChangeListener.run();
    }

    @Nonnull
    public static String operationNameByChanges(@Nonnull Project project, @Nonnull Collection<Change> changes) {
        return RollbackUtil.getRollbackOperationName(ChangesUtil.getAffectedVcses(changes, project));
    }

    @Nonnull
    private static List<Change> getAllChanges(@Nonnull List<? extends ChangeList> changeLists) {
        List<Change> result = new ArrayList<>();

        for (ChangeList list : changeLists) {
            result.addAll(list.getChanges());
        }

        return result;
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        RollbackWorker worker = new RollbackWorker(myProject, myOperationName, myInvokedFromModalContext);
        worker.doRollback(myBrowser.getViewer().getIncludedChanges(), myDeleteLocallyAddedFiles.getValueOrError(),
            myAfterVcsRefreshInAwt, null
        );
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gb = new GridBagConstraints(
            0, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
            JBUI.insets(1), 0, 0
        );

        gb.fill = GridBagConstraints.HORIZONTAL;
        gb.weightx = 1;

        JPanel border = new JPanel(new BorderLayout());
        border.setBorder(JBUI.Borders.emptyTop(2));
        border.add(myBrowser, BorderLayout.CENTER);
        gb.fill = GridBagConstraints.BOTH;
        gb.weighty = 1;
        ++gb.gridy;
        panel.add(border, gb);

        JComponent commitLegendPanel = myCommitLegendPanel.getComponent();
        commitLegendPanel.setBorder(JBUI.Borders.emptyLeft(4));
        gb.fill = GridBagConstraints.NONE;
        gb.weightx = 0;
        gb.weighty = 0;
        ++gb.gridy;
        panel.add(commitLegendPanel, gb);

        ++gb.gridy;
        panel.add(TargetAWT.to(myDeleteLocallyAddedFiles), gb);

        return panel;
    }

    @Override
    @RequiredUIAccess
    public JComponent getPreferredFocusedComponent() {
        return myBrowser.getPreferredFocusedComponent();
    }

    @Override
    protected String getDimensionServiceKey() {
        return "RollbackChangesDialog";
    }
}
