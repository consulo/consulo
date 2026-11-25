// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.refactoring.suggested;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.document.util.TextRange;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringState;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringSupport;
import consulo.language.editor.refactoring.suggested.TextRangeUtils;
import consulo.language.editor.template.TemplateManager;
import consulo.language.impl.internal.psi.PsiTreeChangeEventImpl;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.psi.*;
import consulo.language.psi.event.PsiTreeChangeAdapter;
import consulo.language.psi.event.PsiTreeChangeEvent;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.ProjectUndoManager;
import consulo.undoRedo.event.CommandEvent;
import consulo.undoRedo.event.CommandListener;
import consulo.util.concurrent.CancellablePromise;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class SuggestedRefactoringChangeListener {
    private final Project myProject;
    private final SuggestedRefactoringSignatureWatcher myWatcher;
    private final PsiDocumentManager myPsiDocumentManager;
    private final NewIdentifierWatcher myNewIdentifierWatcher = new NewIdentifierWatcher(5);

    private record SignatureEditingState(
        RangeMarker signatureRangeMarker,
        @Nullable RangeMarker importRangeMarker,
        boolean isRefactoringSuppressed
    ) {
        SignatureEditingState withRefactoringSuppressed() {
            return new SignatureEditingState(signatureRangeMarker, importRangeMarker, true);
        }
    }

    private @Nullable SignatureEditingState myEditingState;

    // EDT written, BGT read
    private volatile @Nullable CancellablePromise<?> mySignatureUpdateJob;

    private boolean myIsFirstChangeInsideCommand = false;

    public SuggestedRefactoringChangeListener(Project project, SuggestedRefactoringSignatureWatcher watcher, Disposable parentDisposable) {
        myProject = project;
        myWatcher = watcher;
        myPsiDocumentManager = PsiDocumentManager.getInstance(project);

        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(new MyDocumentListener(), parentDisposable);
        PsiManager.getInstance(project).addPsiTreeChangeListenerBackgroundable(new MyPsiTreeChangeListener(), parentDisposable);

        Application.get().getMessageBus().connect(parentDisposable).subscribe(CommandListener.class, new CommandListener() {
            @Override
            public void commandStarted(CommandEvent event) {
                myIsFirstChangeInsideCommand = true;
            }
        });

        Disposer.register(parentDisposable, () -> {
            CancellablePromise<?> job = mySignatureUpdateJob;
            if (job != null) {
                job.cancel();
            }
        });
    }

    public void reset() {
        reset(false);
    }

    public void reset(boolean withNewIdentifiers) {
        myProject.getMessageBus().syncPublisher(SuggestedRefactoringAvailabilityListener.class).reset();
        SignatureEditingState editingState = myEditingState;
        if (editingState != null) {
            editingState.signatureRangeMarker().dispose();
            if (editingState.importRangeMarker() != null) {
                editingState.importRangeMarker().dispose();
            }
            if (!editingState.isRefactoringSuppressed()) {
                myWatcher.reset();
            }
            myEditingState = null;
        }
        if (withNewIdentifiers) {
            myNewIdentifierWatcher.reset();
        }
    }

    @RequiredReadAction
    public void undoToState(SuggestedRefactoringState state, TextRange signatureRange) {
        PsiFile psiFile = state.getAnchor().getContainingFile();
        Document document = Objects.requireNonNull(PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile));
        myEditingState = createEditingState(document, state.getAnchor(), signatureRange, state.getRefactoringSupport());
    }

    public void suppressForCurrentDeclaration() {
        SignatureEditingState editingState = myEditingState;
        if (editingState != null && !editingState.isRefactoringSuppressed()) {
            myEditingState = editingState.withRefactoringSuppressed();
            myWatcher.reset();
        }
    }

    @RequiredReadAction
    private void processBeforeFirstChangeWithPsiAndDocumentInSync(
        PsiFile psiFile,
        Document document,
        TextRange changeRange,
        SuggestedRefactoringSupport refactoringSupport
    ) {
        if (myEditingState != null) {
            return;
        }

        // it doesn't make sense start track signature changes not in active editor
        Editor[] editors = EditorFactory.getInstance().getEditors(document, myProject);
        if (editors.length == 0) {
            return;
        }

        // suppress if live template is active - otherwise refactoring appears for create from usage and other features
        TemplateManager templateManager = TemplateManager.getInstance(myProject);
        for (Editor editor : editors) {
            if (templateManager.getActiveTemplate(editor) != null) {
                return;
            }
        }

        // undo of some action can't be a start of signature editing

        if (ProjectUndoManager.getInstance(myProject).isUndoInProgress()) {
            return;
        }

        TextRange truncatedChangeRange = TextRangeUtils.stripWhitespace(changeRange, document.getCharsSequence());
        int offset = truncatedChangeRange.getStartOffset();
        PsiElement anchor = anchorByOffsetInSignature(psiFile, refactoringSupport, offset);
        if (anchor == null && changeRange.isEmpty()) { // for text insertion we might need to look to the left and to the right
            TextRange whitespaceRange = TextRangeUtils.extendWithWhitespace(new TextRange(offset, offset), document.getCharsSequence());
            PsiElement leftAnchor = whitespaceRange.getStartOffset() > 0
                ? anchorByOffsetInSignature(psiFile, refactoringSupport, whitespaceRange.getStartOffset() - 1)
                : null;
            PsiElement rightAnchor = whitespaceRange.getEndOffset() > offset
                ? anchorByOffsetInSignature(psiFile, refactoringSupport, whitespaceRange.getEndOffset())
                : null;
            anchor = leftAnchor != null ? leftAnchor : rightAnchor;
            //TODO: support tracking both declarations for a while
        }
        if (anchor == null) {
            return;
        }

        if (refactoringSupport.hasSyntaxError(anchor)) {
            return;
        }

        TextRange signatureRange = refactoringSupport.signatureRange(anchor);
        if (signatureRange == null) {
            return;
        }
        TextRange extendedSignatureRange = signatureRange.union(truncatedChangeRange);

        SignatureEditingState editingState = createEditingState(document, anchor, extendedSignatureRange, refactoringSupport);
        myEditingState = editingState;
        if (!editingState.isRefactoringSuppressed()) {
            myWatcher.editingStarted(anchor, refactoringSupport);
        }
    }

    @RequiredReadAction
    private static @Nullable PsiElement anchorByOffsetInSignature(PsiFile psiFile, SuggestedRefactoringSupport refactoringSupport, int offset) {
        PsiElement anchor = refactoringSupport.anchorByOffset(psiFile, offset);
        if (anchor == null) {
            return null;
        }
        TextRange signatureRange = refactoringSupport.signatureRange(anchor);
        if (signatureRange == null) {
            return null;
        }
        return signatureRange.contains(offset) ? anchor : null;
    }

    @RequiredReadAction
    private SignatureEditingState createEditingState(
        Document document,
        PsiElement declaration,
        TextRange signatureRange,
        SuggestedRefactoringSupport refactoringSupport
    ) {
        RangeMarker signatureRangeMarker = document.createRangeMarker(signatureRange);
        signatureRangeMarker.setGreedyToLeft(true);
        signatureRangeMarker.setGreedyToRight(true);

        TextRange importsRange = refactoringSupport.importsRange(declaration.getContainingFile());
        RangeMarker importRangeMarker = null;
        if (importsRange != null) {
            importRangeMarker = document.createRangeMarker(importsRange);
            importRangeMarker.setGreedyToLeft(true);
            importRangeMarker.setGreedyToRight(true);
        }

        boolean refactoringSuppressed = shouldSuppressRefactoring(declaration, document, refactoringSupport);
        return new SignatureEditingState(signatureRangeMarker, importRangeMarker, refactoringSuppressed);
    }

    @RequiredReadAction
    private boolean shouldSuppressRefactoring(
        PsiElement declaration,
        Document document,
        SuggestedRefactoringSupport refactoringSupport
    ) {
        // suppress for declaration being just typed
        if (myNewIdentifierWatcher.getLastDocument() == document) {
            TextRange nameRange = refactoringSupport.nameRange(declaration);
            if (nameRange != null) {
                List<TextRange> ranges = myNewIdentifierWatcher.lastNewIdentifierRanges();
                int index = ranges.lastIndexOf(nameRange);
                if (index >= 0) {
                    // check that all identifiers typed after declaration name are inside its signature
                    TextRange signatureRange = Objects.requireNonNull(refactoringSupport.signatureRange(declaration));
                    for (TextRange range : ranges.subList(index + 1, ranges.size())) {
                        if (!signatureRange.contains(range)) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }

        return false;
    }

    private static TextRange oldRange(DocumentEvent event) {
        return new TextRange(event.getOffset(), event.getOffset() + event.getOldLength());
    }

    private class MyDocumentListener implements DocumentListener {
        @Override
        @RequiredReadAction
        public void beforeDocumentChange(DocumentEvent event) {
            Document document = event.getDocument();
            PsiFile psiFile = myPsiDocumentManager.getCachedPsiFile(document);
            if (psiFile == null || shouldIgnoreFile(psiFile)) {
                return;
            }

            boolean firstChangeInsideCommand = myIsFirstChangeInsideCommand;
            myIsFirstChangeInsideCommand = false;

            SuggestedRefactoringSupport refactoringSupport = SuggestedRefactoringSupport.forLanguage(psiFile.getLanguage());
            if (refactoringSupport == null) {
                return;
            }

            if (shouldAbortSignatureEditing(event)) {
                reset();
            }

            if (firstChangeInsideCommand &&
                myPsiDocumentManager.isCommitted(document) &&
                !myPsiDocumentManager.isDocumentBlockedByPsi(document)) {
                processBeforeFirstChangeWithPsiAndDocumentInSync(psiFile, document, oldRange(event), refactoringSupport);
            }
        }

        private boolean shouldIgnoreFile(PsiFile file) {
            return !file.isPhysical() || file instanceof PsiBinaryFile || file instanceof PsiCodeFragment;
        }

        private boolean shouldAbortSignatureEditing(DocumentEvent event) {
            SignatureEditingState state = myEditingState;
            if (state == null) {
                return false;
            }
            if (!state.signatureRangeMarker().isValid()) {
                return true;
            }
            if (state.signatureRangeMarker().getDocument() != event.getDocument()) {
                return true;
            }
            RangeMarker importRangeMarker = state.importRangeMarker();
            if (importRangeMarker != null && !importRangeMarker.isValid()) {
                return true;
            }

            TextRange signatureRange = Objects.requireNonNull(TextRangeUtils.asTextRange(state.signatureRangeMarker()));
            TextRange importRange = importRangeMarker != null ? TextRangeUtils.asTextRange(importRangeMarker) : null;

            TextRange oldRange = oldRange(event);
            if (!signatureRange.contains(oldRange) && (importRange == null || !importRange.contains(oldRange))) {
                return !event.getOldFragment().toString().isBlank() || !event.getNewFragment().toString().isBlank();
            }

            return false;
        }

        @Override
        public void documentChanged(DocumentEvent event) {
            if (event.getOldLength() == 0 && event.getNewLength() == 0) {
                return;
            }

            Document document = event.getDocument();
            PsiFile psiFile = myPsiDocumentManager.getCachedPsiFile(document);
            if (psiFile == null || shouldIgnoreFile(psiFile)) {
                return;
            }

            myNewIdentifierWatcher.documentChanged(event, psiFile.getLanguage());

            SignatureEditingState editingState = myEditingState;
            if (editingState == null) {
                return;
            }
            RangeMarker signatureRangeMarker = editingState.signatureRangeMarker();
            RangeMarker importRangeMarker = editingState.importRangeMarker();
            if (!signatureRangeMarker.isValid() || importRangeMarker != null && !importRangeMarker.isValid()) {
                reset();
                return;
            }

            scheduleSignatureUpdate();
        }

        private void scheduleSignatureUpdate() {
            SignatureEditingState state = myEditingState;
            if (state == null) {
                return;
            }

            CancellablePromise<?> job = mySignatureUpdateJob;
            if (job != null) {
                job.cancel();
            }
            mySignatureUpdateJob = ReadAction.nonBlocking(() -> computeSignatureUpdate(state))
                .withDocumentsCommitted(myProject)
                .expireWith(myProject)
                .finishOnUiThread(Application::getDefaultModalityState, update -> applySignatureUpdate(state, update))
                .submitDefault();
        }

        @RequiredReadAction
        private SignatureUpdate computeSignatureUpdate(SignatureEditingState editingState) {
            TextRange watchedRange = TextRangeUtils.asTextRange(editingState.signatureRangeMarker());
            if (watchedRange == null) {
                return SignatureUpdate.RESET;
            }

            Document document = editingState.signatureRangeMarker().getDocument();
            if (myPsiDocumentManager.isUncommited(document)) {
                return SignatureUpdate.NO_UPDATE;
            }
            PsiFile psiFile = myPsiDocumentManager.getPsiFile(document);
            if (psiFile == null) {
                return SignatureUpdate.NO_UPDATE;
            }
            SuggestedRefactoringSupport refactoringSupport = SuggestedRefactoringSupport.forLanguage(psiFile.getLanguage());
            if (refactoringSupport == null) {
                return SignatureUpdate.NO_UPDATE;
            }

            CharSequence chars = document.getCharsSequence();
            TextRange strippedWatchedRange = TextRangeUtils.stripWhitespace(watchedRange, chars);
            PsiElement anchor = refactoringSupport.anchorByOffset(psiFile, strippedWatchedRange.getStartOffset());
            TextRange signatureRange = anchor != null ? refactoringSupport.signatureRange(anchor) : null;

            if (anchor == null || signatureRange == null
                || !strippedWatchedRange.equals(TextRangeUtils.stripWhitespace(signatureRange, chars))) {
                TextRange watchedRangeExtended = TextRangeUtils.extendWithWhitespace(watchedRange, chars);
                TextRange range = signatureRange != null ? signatureRange.union(watchedRangeExtended) : watchedRangeExtended;
                return PsiTreeUtil.hasErrorElementInRange(psiFile, range) ? SignatureUpdate.INCONSISTENT_STATE : SignatureUpdate.RESET;
            }

            return new SignatureUpdate.NextSignature(anchor, refactoringSupport);
        }

        @RequiredUIAccess
        @RequiredReadAction
        private void applySignatureUpdate(SignatureEditingState editingState, SignatureUpdate update) {
            if (myEditingState != editingState) {
                return;
            }

            if (update == SignatureUpdate.RESET) {
                reset();
            }
            else if (update == SignatureUpdate.INCONSISTENT_STATE) {
                if (!editingState.isRefactoringSuppressed()) {
                    myWatcher.inconsistentState();
                }
            }
            else if (update instanceof SignatureUpdate.NextSignature nextSignature) {
                if (!nextSignature.anchor().isValid()) {
                    reset();
                }
                else if (!editingState.isRefactoringSuppressed()) {
                    myWatcher.nextSignature(nextSignature.anchor(), nextSignature.refactoringSupport());
                }
            }
        }
    }

    private sealed interface SignatureUpdate {
        SignatureUpdate NO_UPDATE = new Simple();
        SignatureUpdate RESET = new Simple();
        SignatureUpdate INCONSISTENT_STATE = new Simple();

        final class Simple implements SignatureUpdate {
        }

        record NextSignature(PsiElement anchor, SuggestedRefactoringSupport refactoringSupport) implements SignatureUpdate {
        }
    }

    private class MyPsiTreeChangeListener extends PsiTreeChangeAdapter {
        @Override
        public void beforeChildAddition(PsiTreeChangeEvent event) {
            processBeforeEvent(event);
        }

        @Override
        public void beforeChildRemoval(PsiTreeChangeEvent event) {
            processBeforeEvent(event);
        }

        @Override
        public void beforeChildReplacement(PsiTreeChangeEvent event) {
            processBeforeEvent(event);
        }

        @Override
        public void beforeChildrenChange(PsiTreeChangeEvent event) {
            processBeforeEvent(event);
        }

        @Override
        public void beforeChildMovement(PsiTreeChangeEvent event) {
            processBeforeEvent(event);
        }

        @RequiredReadAction
        private void processBeforeEvent(PsiTreeChangeEvent event) {
            // follows the edits of a command, on the UI thread - the PSI change of a background commit is not one of them
            if (!UIAccess.isUIThread() || myProject.isDisposed() || !myIsFirstChangeInsideCommand) {
                return;
            }

            PsiFile psiFile = event.getFile();
            if (psiFile == null) {
                return;
            }
            Document document = myPsiDocumentManager.getCachedDocument(psiFile);

            if (document == null) {
                myIsFirstChangeInsideCommand = false;
                return;
            }

            if (myPsiDocumentManager.isUncommited(document)) {
                return; // commit in progress, changes have been processed by DocumentListener
            }

            myIsFirstChangeInsideCommand = false;

            if (!(psiFile instanceof PsiFileImpl psiFileImpl) || !psiFileImpl.isContentsLoaded()) {
                return; // no AST loaded
            }

            SuggestedRefactoringSupport refactoringSupport = SuggestedRefactoringSupport.forLanguage(psiFile.getLanguage());
            if (refactoringSupport == null) {
                return;
            }
            PsiTreeChangeEventImpl eventImpl = (PsiTreeChangeEventImpl) event;
            TextRange changeRange = TextRange.from(eventImpl.getOffset(), eventImpl.getOldLength());
            processBeforeFirstChangeWithPsiAndDocumentInSync(psiFile, document, changeRange, refactoringSupport);
        }
    }
}
