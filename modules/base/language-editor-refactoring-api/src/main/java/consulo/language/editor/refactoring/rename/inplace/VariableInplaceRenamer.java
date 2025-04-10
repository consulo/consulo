/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.editor.refactoring.rename.inplace;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.progress.ProgressManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.RealEditor;
import consulo.codeEditor.SelectionModel;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.refactoring.ResolveSnapshotProvider;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.event.RefactoringElementListener;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.rename.*;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.editor.refactoring.util.TextOccurrencesUtil;
import consulo.language.psi.*;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.internal.FinishMarkAction;
import consulo.undoRedo.internal.StartMarkAction;
import consulo.usage.UsageInfo;
import consulo.util.collection.MultiMap;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author ven
 */
public class VariableInplaceRenamer extends InplaceRefactoring {
    private ResolveSnapshotProvider.ResolveSnapshot mySnapshot;
    private TextRange mySelectedRange;
    private Language myLanguage;

    @RequiredReadAction
    public VariableInplaceRenamer(@Nonnull PsiNamedElement elementToRename, Editor editor) {
        this(elementToRename, editor, elementToRename.getProject());
    }

    @RequiredReadAction
    public VariableInplaceRenamer(PsiNamedElement elementToRename, Editor editor, Project project) {
        this(
            elementToRename,
            editor,
            project,
            elementToRename != null ? elementToRename.getName() : null,
            elementToRename != null ? elementToRename.getName() : null
        );
    }

    @RequiredReadAction
    public VariableInplaceRenamer(
        PsiNamedElement elementToRename,
        Editor editor,
        Project project,
        String initialName,
        String oldName
    ) {
        super(editor, elementToRename, project, initialName, oldName);
    }

    @Override
    @RequiredReadAction
    protected boolean startsOnTheSameElement(RefactoringActionHandler handler, PsiElement element) {
        return super.startsOnTheSameElement(handler, element) && handler instanceof VariableInplaceRenameHandler;
    }

    @RequiredUIAccess
    public boolean performInplaceRename() {
        return performInplaceRefactoring(null);
    }

    @Override
    @RequiredReadAction
    protected void collectAdditionalElementsToRename(List<Pair<PsiElement, TextRange>> stringUsages) {
        String stringToSearch = myElementToRename.getName();
        PsiFile currentFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
        if (stringToSearch != null) {
            TextOccurrencesUtil.processUsagesInStringsAndComments(
                myElementToRename,
                stringToSearch,
                true,
                (psiElement, textRange) -> {
                    if (psiElement.getContainingFile() == currentFile) {
                        stringUsages.add(Pair.create(psiElement, textRange));
                    }
                    return true;
                }
            );
        }
    }

    @Override
    @RequiredUIAccess
    protected boolean buildTemplateAndStart(
        Collection<PsiReference> refs,
        Collection<Pair<PsiElement, TextRange>> stringUsages,
        PsiElement scope,
        PsiFile containingFile
    ) {
        if (appendAdditionalElement(refs, stringUsages)) {
            return super.buildTemplateAndStart(refs, stringUsages, scope, containingFile);
        }
        else {
            RenameChooser renameChooser = new RenameChooser(myEditor) {
                @Override
                @RequiredUIAccess
                protected void runRenameTemplate(Collection<Pair<PsiElement, TextRange>> stringUsages) {
                    VariableInplaceRenamer.super.buildTemplateAndStart(refs, stringUsages, scope, containingFile);
                }
            };
            renameChooser.showChooser(refs, stringUsages);
        }
        return true;
    }

    protected boolean appendAdditionalElement(Collection<PsiReference> refs, Collection<Pair<PsiElement, TextRange>> stringUsages) {
        return stringUsages.isEmpty() || StartMarkAction.canStart(myProject) != null;
    }

    protected boolean shouldCreateSnapshot() {
        return true;
    }

    @Override
    @RequiredReadAction
    protected void beforeTemplateStart() {
        super.beforeTemplateStart();
        myLanguage = myScope.getLanguage();
        if (shouldCreateSnapshot()) {
            ResolveSnapshotProvider resolveSnapshotProvider = ResolveSnapshotProvider.forLanguage(myLanguage);
            mySnapshot = resolveSnapshotProvider != null ? resolveSnapshotProvider.createSnapshot(myScope) : null;
        }

        SelectionModel selectionModel = myEditor.getSelectionModel();
        mySelectedRange =
            selectionModel.hasSelection() ? new TextRange(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd()) : null;
    }

    @Override
    protected void restoreSelection() {
        if (mySelectedRange != null) {
            myEditor.getSelectionModel().setSelection(mySelectedRange.getStartOffset(), mySelectedRange.getEndOffset());
        }
        else if (!shouldSelectAll()) {
            myEditor.getSelectionModel().removeSelection();
        }
    }

    @Override
    protected int restoreCaretOffset(int offset) {
        if (myCaretRangeMarker.isValid()) {
            if (myCaretRangeMarker.getStartOffset() <= offset && myCaretRangeMarker.getEndOffset() >= offset) {
                return offset;
            }
            return myCaretRangeMarker.getEndOffset();
        }
        return offset;
    }

    @Override
    protected boolean shouldSelectAll() {
        if (myEditor.getSettings().isPreselectRename()) {
            return true;
        }
        Boolean selectAll = myEditor.getUserData(RenameHandlerRegistry.SELECT_ALL);
        return selectAll != null && selectAll;
    }

    @RequiredReadAction
    protected VariableInplaceRenamer createInplaceRenamerToRestart(PsiNamedElement variable, Editor editor, String initialName) {
        return new VariableInplaceRenamer(variable, editor, myProject, initialName, myOldName);
    }

    @RequiredReadAction
    protected void performOnInvalidIdentifier(String newName, LinkedHashSet<String> nameSuggestions) {
        PsiNamedElement variable = getVariable();
        if (variable != null) {
            int offset = variable.getTextOffset();
            restoreCaretOffset(offset);
            myEditor.showPopupInBestPositionFor(JBPopupFactory.getInstance().createConfirmation(
                "Inserted identifier is not valid",
                "Continue editing",
                CommonLocalize.buttonCancel().get(),
                () -> createInplaceRenamerToRestart(variable, myEditor, newName).performInplaceRefactoring(nameSuggestions),
                0
            ));
        }
    }

    protected void renameSynthetic(String newName) {
    }

    @RequiredUIAccess
    protected void performRefactoringRename(String newName, StartMarkAction markAction) {
        try {
            if (!isIdentifier(newName, myLanguage)) {
                return;
            }
            PsiNamedElement elementToRename = getVariable();
            if (elementToRename != null) {
                CommandProcessor.getInstance().newCommand()
                    .project(myProject)
                    .name(LocalizeValue.ofNullable(getCommandName()))
                    .inWriteAction()
                    .run(() -> renameSynthetic(newName));
            }
            for (AutomaticRenamerFactory factory : AutomaticRenamerFactory.EP_NAME.getExtensionList()) {
                if (factory.isApplicable(elementToRename)) {
                    List<UsageInfo> usages = new ArrayList<>();
                    AutomaticRenamer renamer = factory.createRenamer(elementToRename, newName, new ArrayList<>());
                    if (renamer.hasAnythingToRename()) {
                        if (!myProject.getApplication().isUnitTestMode()) {
                            AutomaticRenamingDialog renamingDialog = new AutomaticRenamingDialog(myProject, renamer);
                            renamingDialog.show();
                            if (!renamingDialog.isOK()) {
                                return;
                            }
                        }

                        Runnable runnable = () -> myProject.getApplication()
                            .runReadAction(() -> renamer.findUsages(usages, false, false));

                        if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(
                            runnable,
                            RefactoringLocalize.searchingForVariables().get(),
                            true,
                            myProject
                        )) {
                            return;
                        }

                        if (!CommonRefactoringUtil.checkReadOnlyStatus(myProject, PsiUtilCore.toPsiElementArray(renamer.getElements()))) {
                            return;
                        }
                        Runnable performAutomaticRename = () -> {
                            CommandProcessor.getInstance().markCurrentCommandAsGlobal(myProject);
                            UsageInfo[] usageInfos = usages.toArray(new UsageInfo[usages.size()]);
                            MultiMap<PsiElement, UsageInfo> classified = RenameProcessor.classifyUsages(renamer.getElements(), usageInfos);
                            for (PsiNamedElement element : renamer.getElements()) {
                                String newElementName = renamer.getNewName(element);
                                if (newElementName != null) {
                                    Collection<UsageInfo> infos = classified.get(element);
                                    RenameUtil.doRename(
                                        element,
                                        newElementName,
                                        infos.toArray(new UsageInfo[infos.size()]),
                                        myProject,
                                        RefactoringElementListener.DEAF
                                    );
                                }
                            }
                        };
                        CommandProcessor.getInstance().newCommand()
                            .project(myProject)
                            .name(LocalizeValue.ofNullable(getCommandName()))
                            .inLaterIf(!myProject.getApplication().isUnitTestMode())
                            .inWriteAction()
                            .run(performAutomaticRename::run);
                    }
                }
            }
        }
        finally {
            try {
                ((RealEditor)EditorWindow.getTopLevelEditor(myEditor)).stopDumbLater();
            }
            finally {
                FinishMarkAction.finish(myProject, myEditor.getDocument(), markAction);
            }
        }
    }

    @Override
    protected String getCommandName() {
        return RefactoringLocalize.renamingCommandName(myInitialName).get();
    }

    @Override
    @RequiredUIAccess
    protected boolean performRefactoring() {
        boolean bind = false;
        if (myInsertedName != null) {
            CommandProcessor commandProcessor = CommandProcessor.getInstance();
            if (commandProcessor.hasCurrentCommand() && getVariable() != null) {
                commandProcessor.setCurrentCommandName(getCommandName());
            }

            bind = true;
            if (!isIdentifier(myInsertedName, myLanguage)) {
                performOnInvalidIdentifier(myInsertedName, myNameSuggestions);
            }
            else if (mySnapshot != null && isIdentifier(myInsertedName, myLanguage)) {
                myProject.getApplication().runWriteAction(() -> mySnapshot.apply(myInsertedName));
            }
            performRefactoringRename(myInsertedName, myMarkAction);
        }
        return bind;
    }

    @Override
    @RequiredUIAccess
    public void finish(boolean success) {
        super.finish(success);
        if (success) {
            revertStateOnFinish();
        }
        else {
            ((RealEditor)EditorWindow.getTopLevelEditor(myEditor)).stopDumbLater();
        }
    }

    @RequiredUIAccess
    protected void revertStateOnFinish() {
        if (myInsertedName == null || !isIdentifier(myInsertedName, myLanguage)) {
            revertState();
        }
    }
}
