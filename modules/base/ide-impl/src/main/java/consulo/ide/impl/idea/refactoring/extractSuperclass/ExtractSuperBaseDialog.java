/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.refactoring.extractSuperclass;

import consulo.application.HelpManager;
import consulo.language.editor.ui.util.DocCommentPanel;
import consulo.language.editor.refactoring.BaseRefactoringProcessor;
import consulo.language.editor.refactoring.classMember.MemberInfoBase;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.ui.RefactoringDialog;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RecentsManager;
import consulo.ui.ex.awt.ComponentWithBrowseButton;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author dsl
 */
public abstract class ExtractSuperBaseDialog<ClassType extends PsiElement, MemberInfoType extends MemberInfoBase> extends RefactoringDialog {
    private LocalizeValue myRefactoringName;
    protected final ClassType mySourceClass;
    protected PsiDirectory myTargetDirectory;
    protected final List<MemberInfoType> myMemberInfos;

    private JRadioButton myRbExtractSuperclass;
    private JRadioButton myRbExtractSubclass;

    private JTextField mySourceClassField;
    private JLabel myClassNameLabel;
    private JTextField myExtractedSuperNameField;
    protected JLabel myPackageNameLabel;
    protected ComponentWithBrowseButton myPackageNameField;
    protected DocCommentPanel myDocCommentPanel;
    private JPanel myDestinationRootPanel;

    protected abstract ComponentWithBrowseButton createPackageNameField();

    protected JPanel createDestinationRootPanel() {
        return null;
    }

    protected abstract JTextField createSourceClassField();

    @Nonnull
    protected abstract LocalizeValue getDocCommentPanelName();

    protected abstract String getExtractedSuperNameNotSpecifiedMessage();

    protected abstract BaseRefactoringProcessor createProcessor();

    protected abstract int getDocCommentPolicySetting();

    protected abstract void setDocCommentPolicySetting(int policy);

    @Override
    protected abstract String getHelpId();

    @Nullable
    protected abstract String validateName(String name);

    protected abstract String getTopLabelText();

    protected abstract String getClassNameLabelText();

    protected abstract String getPackageNameLabelText();

    protected abstract String getEntityName();

    protected abstract void preparePackage() throws OperationFailedException;

    protected abstract String getDestinationPackageRecentKey();

    public ExtractSuperBaseDialog(Project project,
                                  ClassType sourceClass,
                                  List<MemberInfoType> members,
                                  @Nonnull LocalizeValue refactoringName) {
        super(project, true);
        myRefactoringName = refactoringName;

        mySourceClass = sourceClass;
        myMemberInfos = members;
        myTargetDirectory = mySourceClass.getContainingFile().getContainingDirectory();
    }

    @Override
    protected void init() {
        setTitle(myRefactoringName);

        myPackageNameField = createPackageNameField();
        myDestinationRootPanel = createDestinationRootPanel();
        mySourceClassField = createSourceClassField();
        myExtractedSuperNameField = createExtractedSuperNameField();

        myDocCommentPanel = new DocCommentPanel(getDocCommentPanelName());
        myDocCommentPanel.setPolicy(getDocCommentPolicySetting());

        super.init();
        updateDialog();
    }

    protected JTextField createExtractedSuperNameField() {
        return new JTextField();
    }

    protected JComponent createActionComponent() {
        Box box = Box.createHorizontalBox();
        final String s = StringUtil.decapitalize(getEntityName());
        myRbExtractSuperclass = new JRadioButton();
        myRbExtractSuperclass.setText(RefactoringLocalize.extractsuperExtract(s).get());
        myRbExtractSubclass = new JRadioButton();
        myRbExtractSubclass.setText(RefactoringLocalize.extractsuperRenameOriginalClass(s).get());
        box.add(myRbExtractSuperclass);
        box.add(myRbExtractSubclass);
        box.add(Box.createHorizontalGlue());
        final ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(myRbExtractSuperclass);
        buttonGroup.add(myRbExtractSubclass);
        customizeRadiobuttons(box, buttonGroup);
        myRbExtractSuperclass.setSelected(true);

        ItemListener listener = e -> updateDialog();
        myRbExtractSuperclass.addItemListener(listener);
        myRbExtractSubclass.addItemListener(listener);
        return box;
    }

    protected void customizeRadiobuttons(Box box, ButtonGroup buttonGroup) {
    }

    @Override
    protected JComponent createNorthPanel() {
        Box box = Box.createVerticalBox();

        JPanel _panel = new JPanel(new BorderLayout());
        _panel.add(new JLabel(getTopLabelText()), BorderLayout.NORTH);
        _panel.add(mySourceClassField, BorderLayout.CENTER);
        box.add(_panel);

        box.add(Box.createVerticalStrut(10));

        box.add(createActionComponent());

        box.add(Box.createVerticalStrut(10));

        myClassNameLabel = new JLabel();

        _panel = new JPanel(new BorderLayout());
        _panel.add(myClassNameLabel, BorderLayout.NORTH);
        _panel.add(myExtractedSuperNameField, BorderLayout.CENTER);
        box.add(_panel);
        box.add(Box.createVerticalStrut(5));

        _panel = new JPanel(new BorderLayout());
        myPackageNameLabel = new JLabel();

        _panel.add(myPackageNameLabel, BorderLayout.NORTH);
        _panel.add(myPackageNameField, BorderLayout.CENTER);
        if (myDestinationRootPanel != null) {
            _panel.add(myDestinationRootPanel, BorderLayout.SOUTH);
        }
        box.add(_panel);
        box.add(Box.createVerticalStrut(10));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(box, BorderLayout.CENTER);
        return panel;
    }

    @Override
    @RequiredUIAccess
    public JComponent getPreferredFocusedComponent() {
        return myExtractedSuperNameField;
    }

    protected void updateDialog() {
        myClassNameLabel.setText(getClassNameLabelText());
        myPackageNameLabel.setText(getPackageNameLabelText());
        getPreviewAction().setEnabled(!isExtractSuperclass());
    }

    public String getExtractedSuperName() {
        return myExtractedSuperNameField.getText().trim();
    }

    protected abstract String getTargetPackageName();

    public PsiDirectory getTargetDirectory() {
        return myTargetDirectory;
    }

    @RequiredUIAccess
    public int getDocCommentPolicy() {
        return myDocCommentPanel.getPolicy();
    }

    public boolean isExtractSuperclass() {
        return myRbExtractSuperclass != null && myRbExtractSuperclass.isSelected();
    }

    @Override
    @RequiredUIAccess
    protected void doAction() {
        final String[] errorString = new String[]{null};
        final String extractedSuperName = getExtractedSuperName();
        final String packageName = getTargetPackageName();
        RecentsManager.getInstance(myProject).registerRecentEntry(getDestinationPackageRecentKey(), packageName);

        if ("".equals(extractedSuperName)) {
            // TODO just disable OK button
            errorString[0] = getExtractedSuperNameNotSpecifiedMessage();
            myExtractedSuperNameField.requestFocusInWindow();
        }
        else {
            String nameError = validateName(extractedSuperName);
            if (nameError != null) {
                errorString[0] = nameError;
                myExtractedSuperNameField.requestFocusInWindow();
            }
            else {
                CommandProcessor.getInstance().newCommand()
                    .project(myProject)
                    .name(RefactoringLocalize.createDirectory())
                    .run(() -> {
                        try {
                            preparePackage();
                        }
                        catch (IncorrectOperationException | OperationFailedException e) {
                            errorString[0] = e.getMessage();
                            myPackageNameField.requestFocusInWindow();
                        }
                    });
            }
        }
        if (errorString[0] != null) {
            if (errorString[0].length() > 0) {
                CommonRefactoringUtil.showErrorMessage(myRefactoringName.get(), errorString[0], getHelpId(), myProject);
            }
            return;
        }

        if (!checkConflicts()) {
            return;
        }

        executeRefactoring();
        setDocCommentPolicySetting(getDocCommentPolicy());
        closeOKAction();
    }

    protected void executeRefactoring() {
        if (!isExtractSuperclass()) {
            invokeRefactoring(createProcessor());
        }
    }

    @Override
    @RequiredUIAccess
    protected void doHelpAction() {
        HelpManager.getInstance().invokeHelp(getHelpId());
    }

    protected boolean checkConflicts() {
        return true;
    }

    protected static class OperationFailedException extends Exception {
        public OperationFailedException(String message) {
            super(message);
        }
    }

    public Collection<MemberInfoType> getSelectedMemberInfos() {
        ArrayList<MemberInfoType> result = new ArrayList<>(myMemberInfos.size());
        for (MemberInfoType info : myMemberInfos) {
            if (info.isChecked()) {
                result.add(info);
            }
        }
        return result;
    }
}
