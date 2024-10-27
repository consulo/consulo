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
import consulo.codeEditor.Editor;
import consulo.codeEditor.RealEditor;
import consulo.content.scope.SearchScope;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.rename.AutomaticRenamerFactory;
import consulo.language.editor.refactoring.rename.RenameProcessor;
import consulo.language.editor.refactoring.rename.RenamePsiElementProcessor;
import consulo.language.findUsage.DescriptiveNameUtil;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.content.scope.ProjectScopes;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.internal.FinishMarkAction;
import consulo.undoRedo.internal.StartMarkAction;
import consulo.usage.UsageViewUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * @author anna
 * @since 2011-11-09
 */
public class MemberInplaceRenamer extends VariableInplaceRenamer {
    private final PsiElement mySubstituted;
    private RangeMarker mySubstitutedRange;

    @RequiredReadAction
    public MemberInplaceRenamer(@Nonnull PsiNamedElement elementToRename, PsiElement substituted, Editor editor) {
        this(elementToRename, substituted, editor, elementToRename.getName(), elementToRename.getName());
    }

    @RequiredReadAction
    public MemberInplaceRenamer(
        @Nonnull PsiNamedElement elementToRename,
        PsiElement substituted,
        Editor editor,
        String initialName,
        String oldName
    ) {
        super(elementToRename, editor, elementToRename.getProject(), initialName, oldName);
        mySubstituted = substituted;
        if (mySubstituted != null && mySubstituted != myElementToRename && mySubstituted.getTextRange() != null) {
            final PsiFile containingFile = mySubstituted.getContainingFile();
            if (!notSameFile(containingFile.getVirtualFile(), containingFile)) {
                mySubstitutedRange = myEditor.getDocument().createRangeMarker(mySubstituted.getTextRange());
            }
        }
        else {
            mySubstitutedRange = null;
        }

        showDialogAdvertisement("RenameElement");
    }

    @Override
    @RequiredReadAction
    protected VariableInplaceRenamer createInplaceRenamerToRestart(PsiNamedElement variable, Editor editor, String initialName) {
        return new MemberInplaceRenamer(variable, getSubstituted(), editor, initialName, myOldName);
    }

    @Override
    @RequiredReadAction
    protected boolean acceptReference(PsiReference reference) {
        final PsiElement element = reference.getElement();
        final TextRange textRange = reference.getRangeInElement();
        final String referenceText = element.getText().substring(textRange.getStartOffset(), textRange.getEndOffset());
        return Comparing.strEqual(referenceText, myElementToRename.getName());
    }

    @Override
    protected PsiElement checkLocalScope() {
        PsiFile currentFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
        if (currentFile != null) {
            return currentFile;
        }
        return super.checkLocalScope();
    }

    @Override
    @RequiredReadAction
    protected PsiElement getNameIdentifier() {
        final PsiFile currentFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
        if (currentFile == myElementToRename.getContainingFile()) {
            return super.getNameIdentifier();
        }
        if (currentFile != null) {
            int offset = myEditor.getCaretModel().getOffset();
            offset = TargetElementUtil.adjustOffset(currentFile, myEditor.getDocument(), offset);
            final PsiElement elementAt = currentFile.findElementAt(offset);
            if (elementAt != null) {
                final PsiElement referenceExpression = elementAt.getParent();
                if (referenceExpression != null) {
                    final PsiReference reference = referenceExpression.getReference();
                    if (reference != null && reference.resolve() == myElementToRename) {
                        return elementAt;
                    }
                }
            }
            return null;
        }
        return null;
    }

    @Override
    @RequiredReadAction
    protected Collection<PsiReference> collectRefs(SearchScope referencesSearchScope) {
        final ArrayList<PsiReference> references = new ArrayList<>(super.collectRefs(referencesSearchScope));
        final PsiNamedElement variable = getVariable();
        if (variable != null) {
            final PsiElement substituted = getSubstituted();
            if (substituted != null && substituted != variable) {
                references.addAll(ReferencesSearch.search(substituted, referencesSearchScope, false).findAll());
            }
        }
        return references;
    }

    @Override
    protected boolean notSameFile(@Nullable VirtualFile file, @Nonnull PsiFile containingFile) {
        final PsiFile currentFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
        if (currentFile == null) {
            return true;
        }
        InjectedLanguageManager manager = InjectedLanguageManager.getInstance(containingFile.getProject());
        return manager.getTopLevelFile(containingFile) != manager.getTopLevelFile(currentFile);
    }

    @Override
    protected SearchScope getReferencesSearchScope(VirtualFile file) {
        PsiFile currentFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
        return currentFile != null ? new LocalSearchScope(currentFile)
            : ProjectScopes.getProjectScope(myProject);
    }

    @Override
    @RequiredReadAction
    protected boolean appendAdditionalElement(Collection<PsiReference> refs, Collection<Pair<PsiElement, TextRange>> stringUsages) {
        boolean showChooser = super.appendAdditionalElement(refs, stringUsages);
        PsiNamedElement variable = getVariable();
        if (variable != null) {
            final PsiElement substituted = getSubstituted();
            if (substituted != null) {
                appendAdditionalElement(stringUsages, variable, substituted);
                RenamePsiElementProcessor processor = RenamePsiElementProcessor.forElement(substituted);
                final HashMap<PsiElement, String> allRenames = new HashMap<>();
                PsiFile currentFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
                processor.prepareRenaming(substituted, "", allRenames, new LocalSearchScope(currentFile));
                for (PsiElement element : allRenames.keySet()) {
                    appendAdditionalElement(stringUsages, variable, element);
                }
            }
        }
        return showChooser;
    }

    @Override
    protected boolean shouldCreateSnapshot() {
        return false;
    }

    @RequiredReadAction
    private void appendAdditionalElement(
        Collection<Pair<PsiElement, TextRange>> stringUsages,
        PsiNamedElement variable,
        PsiElement element
    ) {
        if (element != variable && element instanceof PsiNameIdentifierOwner nameIdentifierOwner &&
            !notSameFile(null, element.getContainingFile())) {
            final PsiElement identifier = nameIdentifierOwner.getNameIdentifier();
            if (identifier != null) {
                stringUsages.add(Pair.create(identifier, new TextRange(0, identifier.getTextLength())));
            }
        }
    }

    @Override
    @RequiredUIAccess
    protected void performRefactoringRename(final String newName, final StartMarkAction markAction) {
        try {
            final PsiNamedElement variable = getVariable();
            if (variable != null && !newName.equals(myOldName)) {
                if (isIdentifier(newName, variable.getLanguage())) {
                    final PsiElement substituted = getSubstituted();
                    if (substituted == null) {
                        return;
                    }

                    CommandProcessor.getInstance().newCommand()
                        .project(myProject)
                        .name(RefactoringLocalize.renaming01To2(
                            UsageViewUtil.getType(variable),
                            DescriptiveNameUtil.getDescriptiveName(variable),
                            newName
                        ))
                        .run(() -> {
                            performRenameInner(substituted, newName);
                            PsiDocumentManager.getInstance(myProject).commitAllDocuments();
                        });
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

    @RequiredUIAccess
    protected void performRenameInner(PsiElement element, String newName) {
        final RenamePsiElementProcessor elementProcessor = RenamePsiElementProcessor.forElement(element);
        final RenameProcessor renameProcessor = new RenameProcessor(
            myProject,
            element,
            newName,
            elementProcessor.isToSearchInComments(element),
            elementProcessor.isToSearchForTextOccurrences(element)
        ) {
            @Override
            @RequiredUIAccess
            public void doRun() {
                try {
                    super.doRun();
                }
                finally {
                    restoreCaretOffsetAfterRename();
                }
            }
        };
        for (AutomaticRenamerFactory factory : AutomaticRenamerFactory.EP_NAME.getExtensionList()) {
            if (factory.getOptionName() != LocalizeValue.of() && factory.isApplicable(element)) {
                renameProcessor.addRenamerFactory(factory);
            }
        }
        renameProcessor.run();
    }

    protected void restoreCaretOffsetAfterRename() {
        if (myBeforeRevert != null) {
            myEditor.getCaretModel().moveToOffset(myBeforeRevert.getEndOffset());
            myBeforeRevert.dispose();
        }
    }

    @Override
    @RequiredReadAction
    protected void collectAdditionalElementsToRename(List<Pair<PsiElement, TextRange>> stringUsages) {
        //do not highlight non-code usages in file
    }

    @Override
    @RequiredUIAccess
    protected void revertStateOnFinish() {
        final Editor editor = EditorWindow.getTopLevelEditor(myEditor);
        if (editor == FileEditorManager.getInstance(myProject).getSelectedTextEditor()) {
            ((RealEditor)editor).startDumb();
        }
        revertState();
    }

    @Nullable
    @RequiredReadAction
    public PsiElement getSubstituted() {
        if (mySubstituted != null && mySubstituted.isValid()) {
            if (mySubstituted instanceof PsiNameIdentifierOwner nameIdentifierOwner) {
                if (Comparing.strEqual(myOldName, nameIdentifierOwner.getName())) {
                    return mySubstituted;
                }

                final RangeMarker rangeMarker = mySubstitutedRange != null ? mySubstitutedRange : myRenameOffset;
                if (rangeMarker != null) {
                    return PsiTreeUtil.getParentOfType(
                        mySubstituted.getContainingFile().findElementAt(rangeMarker.getStartOffset()),
                        PsiNameIdentifierOwner.class
                    );
                }
            }
            return mySubstituted;
        }
        if (mySubstitutedRange != null) {
            final PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
            if (psiFile != null) {
                return PsiTreeUtil.getParentOfType(
                    psiFile.findElementAt(mySubstitutedRange.getStartOffset()),
                    PsiNameIdentifierOwner.class
                );
            }
        }
        return getVariable();
    }
}
