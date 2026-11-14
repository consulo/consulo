// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.refactoring.suggested;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.impl.internal.LaterInvocator;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.RealEditor;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.TextAttributes;
import consulo.disposer.Disposer;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.refactoring.RefactoringFactory;
import consulo.language.editor.refactoring.RenameRefactoring;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.suggested.SuggestedChangeSignatureData;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringData;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringExecution.NewParameterValue;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringProvider;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringState;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringState.ErrorLevel;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringSupport;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringUI.NewParameterData;
import consulo.language.editor.refactoring.suggested.SuggestedRenameData;
import consulo.language.editor.refactoring.util.TextOccurrencesUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.navigation.OpenFileDescriptor;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.BalloonBuilder;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.ProjectUndoManager;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class PerformSuggestedRefactoring {
    private PerformSuggestedRefactoring() {
    }

    @RequiredUIAccess
    public static void performSuggestedRefactoring(
        Project project,
        Editor originalEditor,
        @Nullable JComponent popupAnchorComponent,
        @Nullable Point popupAnchorPoint,
        boolean showReviewBalloon,
        String actionPlace
    ) {
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        SuggestedRefactoringState state = SuggestedRefactoringProviderImpl.getInstance(project).getState();
        if (state == null || state.getErrorLevel() != ErrorLevel.NO_ERRORS) {
            return;
        }
        state = state.getRefactoringSupport().getAvailability().refineSignaturesWithResolve(state);
        performSuggestedRefactoring(state, originalEditor, project, actionPlace, showReviewBalloon, popupAnchorComponent, popupAnchorPoint);
    }

    /**
     * Launch suggested refactoring based on the specified state
     */
    @RequiredUIAccess
    public static void performSuggestedRefactoring(
        SuggestedRefactoringState state,
        Editor originalEditor,
        Project project,
        String actionPlace,
        boolean showReviewBalloon,
        @Nullable JComponent popupAnchorComponent,
        @Nullable Point popupAnchorPoint
    ) {
        if (state.getErrorLevel() != ErrorLevel.NO_ERRORS || state.getOldSignature().equals(state.getNewSignature())) {
            return;
        }
        SuggestedRefactoringSupport refactoringSupport = state.getRefactoringSupport();

        PsiElement declaration = refactoringSupport.getStateChanges().findDeclaration(state.getAnchor());
        if (declaration == null) {
            return;
        }
        PsiFile file = declaration.getContainingFile();
        Editor editor;
        if (originalEditor.getDocument() == file.getViewProvider().getDocument()) {
            editor = originalEditor;
        }
        else {
            OpenFileDescriptor descriptor = OpenFileDescriptorFactory.getInstance(project).builder(file.getVirtualFile()).build();
            editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
            if (editor == null) {
                return;
            }
        }

        SuggestedRefactoringData refactoringData = refactoringSupport.getAvailability().detectAvailableRefactoring(state);
        if (refactoringData instanceof SuggestedRenameData renameData) {
            Runnable doRefactor = () -> doRefactor(
                renameData, state, editor, actionPlace,
                () -> performRename(refactoringSupport, renameData, project, editor)
            );

            if (!showReviewBalloon || Application.get().isHeadlessEnvironment()) {
                doRefactor.run();
                return;
            }

            RenamePopup popup = new RenamePopup(renameData.getOldName(), renameData.getNewName());

            TextRange rangeToHighlight = Objects.requireNonNull(state.getRefactoringSupport().nameRange(state.getAnchor()));

            BalloonCallbacks<Object> callbacks = createAndShowBalloon(
                popup, project, editor, popupAnchorComponent, popupAnchorPoint, rangeToHighlight,
                RefactoringLocalize.suggestedRefactoringRenameCommandName(),
                data -> doRefactor.run(),
                popup::onRefactor,
                () -> true,
                () -> true,
                isOk -> {
                }
            );
            popup.setOnRefactor(() -> callbacks.onOk().accept(new Object()));
        }
        else if (refactoringData instanceof SuggestedChangeSignatureData changeSignatureData) {
            List<NewParameterData> newParameterData = refactoringSupport.getUi().extractNewParameterData(changeSignatureData);

            List<NewParameterInfo> newParameterValues = new ArrayList<>(newParameterData.size());
            for (NewParameterData data : newParameterData) {
                newParameterValues.add(new NewParameterInfo(
                    data,
                    data.getPresentableName(),
                    data.getOfferToUseAnyVariable() ? NewParameterValue.ANY_VARIABLE : new NewParameterValue.Expression(data.getValueFragment())
                ));
            }
            CommandProcessor.getInstance().newCommand()
                .project(project)
                .name(RefactoringLocalize.suggestedRefactoringChangeSignatureCommandName(changeSignatureData.getNameOfStuffToUpdate()))
                .run(() -> doRefactor(
                    changeSignatureData, state, editor, actionPlace,
                    () -> {
                        SuggestedRefactoringSupport.Signature sig = changeSignatureData.getNewSignature();
                        List<SuggestedRefactoringSupport.Parameter> parameters = new ArrayList<>();
                        for (SuggestedRefactoringSupport.Parameter param : sig.getParameters()) {
                            NewParameterInfo updated = null;
                            for (NewParameterInfo info : newParameterValues) {
                                if (info.newParameterData().getPresentableName().equals(param.getName())) {
                                    updated = info;
                                    break;
                                }
                            }
                            parameters.add(updated == null ? param : param.withName(updated.name()));
                        }
                        SuggestedRefactoringSupport.Signature newSig = Objects.requireNonNull(
                            SuggestedRefactoringSupport.Signature.create(sig.getName(), sig.getType(), parameters, sig.getAdditionalData())
                        );
                        List<NewParameterValue> values = new ArrayList<>(newParameterValues.size());
                        for (NewParameterInfo info : newParameterValues) {
                            values.add(info.value());
                        }
                        performChangeSignature(refactoringSupport, changeSignatureData.withNewSignature(newSig), values, project, editor);
                    }
                ));
        }
    }

    record NewParameterInfo(NewParameterData newParameterData, String name, NewParameterValue value) {
    }

    @RequiredUIAccess
    private static void doRefactor(
        SuggestedRefactoringData refactoringData,
        SuggestedRefactoringState state,
        Editor editor,
        String actionPlace,
        Runnable doRefactor
    ) {
        Project project = state.getAnchor().getProject();
        ProjectUndoManager.getInstance(project).undoableActionPerformed(SuggestedRefactoringUndoableAction.create(editor.getDocument(), state));

        performWithDumbEditor(editor, doRefactor);

        // no refactoring availability anymore even if no usages updated
        SuggestedRefactoringProvider.getInstance(project).reset();
    }

    private record BalloonCallbacks<TData>(Consumer<TData> onOk) {
    }

    @RequiredUIAccess
    private static <TData> BalloonCallbacks<TData> createAndShowBalloon(
        JComponent content,
        Project project,
        Editor editor,
        @Nullable JComponent popupAnchorComponent,
        @Nullable Point popupAnchorPoint,
        TextRange rangeToHighlight,
        LocalizeValue commandName,
        Consumer<TData> doRefactoring,
        Runnable onEnter,
        BooleanSupplier isEnterEnabled,
        BooleanSupplier isEscapeEnabled,
        Consumer<Boolean> onClosed
    ) {
        BalloonBuilder builder = JBPopupFactory.getInstance()
            .createDialogBalloonBuilder(content, null)
            .setRequestFocus(true)
            .setHideOnClickOutside(true)
            .setCloseButtonEnabled(false)
            .setAnimationCycle(0)
            .setBlockClicksThroughBalloon(true)
            .setContentInsets(new Insets(0, 0, 0, 0));

        Color borderColor = UIManager.getColor("InplaceRefactoringPopup.borderColor");
        if (borderColor != null) {
            builder.setBorderColor(borderColor);
        }

        Balloon balloon = builder.createBalloon();

        positionAndShowBalloon(balloon, popupAnchorComponent, popupAnchorPoint, editor, rangeToHighlight);

        Consumer<TData> hideBalloonAndRefactor = data -> {
            balloon.hide(true);

            CommandProcessor.getInstance().newCommand()
                .project(project)
                .name(commandName)
                .run(() -> doRefactoring.accept(data));
        };

        new BalloonAction(onEnter, isEnterEnabled)
            .registerCustomShortcutSet(CustomShortcutSet.fromString("ENTER"), content, balloon);

        new BalloonAction(() -> balloon.hide(false), isEscapeEnabled)
            .registerCustomShortcutSet(CustomShortcutSet.fromString("ESCAPE"), content, balloon);

        TextAttributes attributes = new TextAttributes(
            null,
            editor.getColorsScheme().getColor(EditorColors.CARET_ROW_COLOR),
            null,
            null,
            Font.PLAIN
        );
        RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(
            rangeToHighlight.getStartOffset(),
            rangeToHighlight.getEndOffset(),
            HighlighterLayer.FIRST,
            attributes,
            HighlighterTargetArea.LINES_IN_RANGE
        );

        LaterInvocator.enterModal(balloon);

        Disposer.register(balloon, () -> {
            LaterInvocator.leaveModal(balloon);
            editor.getMarkupModel().removeHighlighter(highlighter);
        });

        balloon.addListener(new JBPopupListener() {
            @Override
            public void onClosed(LightweightWindowEvent event) {
                onClosed.accept(event.isOk());
            }
        });

        return new BalloonCallbacks<>(hideBalloonAndRefactor);
    }

    private static class BalloonAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private final Runnable myAction;
        private final BooleanSupplier myIsEnabled;

        BalloonAction(Runnable action, BooleanSupplier isEnabled) {
            myAction = action;
            myIsEnabled = isEnabled;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            myAction.run();
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(myIsEnabled.getAsBoolean());
        }
    }

    private static void positionAndShowBalloon(
        Balloon balloon,
        @Nullable JComponent popupAnchorComponent,
        @Nullable Point popupAnchorPoint,
        Editor editor,
        TextRange signatureRange
    ) {
        if (popupAnchorComponent != null && popupAnchorPoint != null) {
            balloon.show(new RelativePoint(popupAnchorComponent, popupAnchorPoint), Balloon.Position.below);
        }
        else {
            RelativePoint top = new RelativePoint(editor.getContentComponent(), editor.offsetToXY(signatureRange.getStartOffset()));
            Point bottomPoint = editor.offsetToXY(signatureRange.getEndOffset());
            bottomPoint.y += editor.getLineHeight();
            RelativePoint bottom = new RelativePoint(editor.getContentComponent(), bottomPoint);
            Point caretXY = editor.offsetToXY(editor.getCaretModel().getOffset());

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            if (top.getScreenPoint().y > screenSize.height - bottom.getScreenPoint().y) {
                balloon.show(
                    new RelativePoint(editor.getContentComponent(), new Point(caretXY.x, top.getOriginalPoint().y)),
                    Balloon.Position.above
                );
            }
            else {
                balloon.show(
                    new RelativePoint(editor.getContentComponent(), new Point(caretXY.x, bottom.getOriginalPoint().y)),
                    Balloon.Position.below
                );
            }
        }
    }

    private static void performWithDumbEditor(Editor editor, Runnable action) {
        if (editor instanceof RealEditor realEditor) {
            realEditor.startDumb();
        }
        try {
            action.run();
        }
        finally {
            if (editor instanceof RealEditor realEditor) {
                realEditor.stopDumbLater();
            }
        }
    }

    @RequiredUIAccess
    private static void performRename(SuggestedRefactoringSupport refactoringSupport, SuggestedRenameData data, Project project, Editor editor) {
        if (!FileModificationService.getInstance().preparePsiElementForWrite(data.getDeclaration())) {
            return;
        }

        int relativeCaretOffset = editor.getCaretModel().getOffset() - anchorOffset(refactoringSupport, data.getDeclaration());

        String newName = data.getNewName();
        Application.get().runWriteAction(() -> {
            data.getDeclaration().setName(data.getOldName());
        });

        boolean textOccurrencesEnabled = TextOccurrencesUtil.isSearchTextOccurencesEnabled(data.getDeclaration());

        RenameRefactoring refactoring = RefactoringFactory.getInstance(project).createRename(data.getDeclaration(), newName, true, textOccurrencesEnabled);
        refactoring.respectAllAutomaticRenames();
        boolean hasNonCodeUsages = ProgressManager.getInstance().runProcess(
            () -> Application.get().runReadAction((java.util.function.Supplier<Boolean>) refactoring::hasNonCodeUsages),
            new EmptyProgressIndicator()
        );
        if (hasNonCodeUsages && !Application.get().isHeadlessEnvironment()) {
            String question = RefactoringLocalize.suggestedRefactoringRenameTextOccurrences(data.getOldName(), newName).get();
            int result = Messages.showOkCancelDialog(
                project,
                question,
                RefactoringLocalize.suggestedRefactoringRenameTextOccurrencesTitle().get(),
                RefactoringLocalize.suggestedRefactoringRenameWithPreviewButtonText().get(),
                RefactoringLocalize.suggestedRefactoringIgnoreButtonText().get(),
                Messages.getQuestionIcon()
            );
            if (result != Messages.OK) {
                refactoring.setSearchInComments(false);
                refactoring.setSearchInNonJavaFiles(false);
            }
        }

        refactoring.run();

        if (data.getDeclaration().isValid()) {
            editor.getCaretModel().moveToOffset(relativeCaretOffset + anchorOffset(refactoringSupport, data.getDeclaration()));
        }
    }

    @RequiredUIAccess
    private static void performChangeSignature(
        SuggestedRefactoringSupport refactoringSupport,
        SuggestedChangeSignatureData data,
        List<NewParameterValue> newParameterValues,
        Project project,
        Editor editor
    ) {
        Object preparedData = refactoringSupport.getExecution().prepareChangeSignature(data);

        int relativeCaretOffset = editor.getCaretModel().getOffset() - anchorOffset(refactoringSupport, data.getDeclaration());

        Runnable restoreNewSignature = Application.get().runWriteAction(
            (java.util.function.Supplier<Runnable>) () -> data.restoreInitialState(refactoringSupport)
        );

        refactoringSupport.getExecution().performChangeSignature(data, newParameterValues, preparedData);

        Application.get().runWriteAction(() -> {
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
            restoreNewSignature.run();
            int offset;
            if (!data.getAnchor().equals(data.getDeclaration())) {
                offset = anchorOffset(refactoringSupport, data.getDeclaration());
            }
            else {
                offset = relativeCaretOffset + anchorOffset(refactoringSupport, data.getDeclaration());
            }
            editor.getCaretModel().moveToOffset(offset);
        });
    }

    @RequiredReadAction
    private static int anchorOffset(SuggestedRefactoringSupport refactoringSupport, PsiElement declaration) {
        TextRange nameRange = refactoringSupport.nameRange(declaration);
        return nameRange != null ? nameRange.getStartOffset() : declaration.getTextRange().getStartOffset();
    }
}
