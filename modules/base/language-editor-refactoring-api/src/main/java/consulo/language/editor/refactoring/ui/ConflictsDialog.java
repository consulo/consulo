/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.editor.refactoring.ui;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.collection.MultiMap;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Jeka
 * @since 2001-09-12
 */
@Deprecated
@DeprecationInfo("Use ConflictsDialogDescriptor")
public class ConflictsDialog extends DialogWrapper {
    private static final int SHOW_CONFLICTS_EXIT_CODE = 4;

    private Collection<LocalizeValue> myConflictDescriptions;
    private MultiMap<PsiElement, LocalizeValue> myElementConflictDescription;
    private final Project myProject;
    private Runnable myDoRefactoringRunnable;
    private final boolean myCanShowConflictsInView;

    private LocalizeValue myCommandName;

    public ConflictsDialog(Project project, MultiMap<PsiElement, LocalizeValue> conflictDescriptions) {
        this(project, conflictDescriptions, null, true, true);
    }

    public ConflictsDialog(
        Project project,
        MultiMap<PsiElement, LocalizeValue> conflictDescriptions,
        @Nullable Runnable doRefactoringRunnable
    ) {
        this(project, conflictDescriptions, doRefactoringRunnable, true, true);
    }

    public ConflictsDialog(
        Project project,
        MultiMap<PsiElement, LocalizeValue> conflictDescriptions,
        @Nullable Runnable doRefactoringRunnable,
        boolean alwaysShowOkButton,
        boolean canShowConflictsInView
    ) {
        super(project, true);
        myProject = project;
        myDoRefactoringRunnable = doRefactoringRunnable;
        myCanShowConflictsInView = canShowConflictsInView;
        Set<LocalizeValue> conflicts = new LinkedHashSet<>();

        for (LocalizeValue conflict : conflictDescriptions.values()) {
            conflicts.add(conflict);
        }
        myConflictDescriptions = conflicts;
        myElementConflictDescription = conflictDescriptions;
        setTitle(RefactoringLocalize.problemsDetectedTitle());
        setOKButtonText(RefactoringLocalize.continueButton());
        setOKActionEnabled(alwaysShowOkButton || myDoRefactoringRunnable != null);
        init();
    }

    @Deprecated
    public ConflictsDialog(Project project, Collection<LocalizeValue> conflictDescriptions) {
        super(project, true);
        myProject = project;
        myConflictDescriptions = conflictDescriptions;
        myCanShowConflictsInView = true;
        setTitle(RefactoringLocalize.problemsDetectedTitle());
        setOKButtonText(RefactoringLocalize.continueButton());
        init();
    }

    @Deprecated
    @SuppressWarnings("deprecation")
    public ConflictsDialog(Project project, LocalizeValue... conflictDescriptions) {
        this(project, Arrays.asList(conflictDescriptions));
    }

    @Override
    protected Action[] createActions() {
        Action okAction = getOKAction();
        boolean showUsagesButton = myElementConflictDescription != null && myCanShowConflictsInView;

        if (showUsagesButton || !okAction.isEnabled()) {
            okAction.putValue(DEFAULT_ACTION, null);
        }

        if (!showUsagesButton) {
            return new Action[]{okAction, new CancelAction()};
        }
        return new Action[]{okAction, new MyShowConflictsInUsageViewAction(), new CancelAction()};
    }

    public boolean isShowConflicts() {
        return getExitCode() == SHOW_CONFLICTS_EXIT_CODE;
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 2));

        panel.add(new JLabel(RefactoringLocalize.theFollowingProblemsWereFound().get()), BorderLayout.NORTH);

        StringBuilder buf = new StringBuilder();
        for (LocalizeValue description : myConflictDescriptions) {
            buf.append(description);
            buf.append("<br><br>");
        }
        JEditorPane messagePane = new JEditorPane(UIUtil.HTML_MIME, buf.toString());
        messagePane.setEditable(false);
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(
            messagePane,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scrollPane.setPreferredSize(new Dimension(500, 400));
        panel.add(scrollPane, BorderLayout.CENTER);

        if (getOKAction().isEnabled()) {
            panel.add(new JLabel(RefactoringLocalize.doYouWishToIgnoreThemAndContinue().get()), BorderLayout.SOUTH);
        }

        return panel;
    }

    public void setCommandName(LocalizeValue commandName) {
        myCommandName = commandName;
    }

    private class CancelAction extends AbstractAction {
        public CancelAction() {
            super(RefactoringLocalize.cancelButton().get());
            putValue(DEFAULT_ACTION, Boolean.TRUE);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            doCancelAction();
        }
    }

    private class MyShowConflictsInUsageViewAction extends AbstractAction {
        public MyShowConflictsInUsageViewAction() {
            super(RefactoringLocalize.showConflictsInViewButton().get());
        }

        @Override
        @RequiredReadAction
        public void actionPerformed(ActionEvent e) {
            ConflictsUsageView.show(myProject, myElementConflictDescription, myDoRefactoringRunnable, myCommandName);
            close(SHOW_CONFLICTS_EXIT_CODE);
        }
    }
}
