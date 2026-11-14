// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.refactoring.suggested;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.codeEditor.event.EditorFactoryEvent;
import consulo.codeEditor.event.EditorFactoryListener;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.TextAttributes;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.suggested.SuggestedChangeSignatureData;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringData;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringSupport;
import consulo.language.editor.refactoring.suggested.SuggestedRenameData;
import consulo.language.editor.refactoring.suggested.TextRangeUtils;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.ex.keymap.util.KeymapUtil;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SuggestedRefactoringAvailabilityIndicator {
    private record Data(
        Document document,
        RangeMarker highlighterRangeMarker,
        RangeMarker availabilityRangeMarker,
        boolean refactoringEnabled,
        LocalizeValue tooltip,
        LocalizeValue intentionText
    ) {
        @Override
        public boolean equals(Object other) {
            return other instanceof Data data
                && data.document == document
                && Objects.equals(TextRangeUtils.asTextRange(data.highlighterRangeMarker), TextRangeUtils.asTextRange(highlighterRangeMarker))
                && Objects.equals(TextRangeUtils.asTextRange(data.availabilityRangeMarker), TextRangeUtils.asTextRange(availabilityRangeMarker))
                && data.refactoringEnabled == refactoringEnabled
                && data.tooltip.equals(tooltip)
                && data.intentionText.equals(intentionText);
        }

        @Override
        public int hashCode() {
            return tooltip.hashCode();
        }
    }

    public static LocalizeValue disabledRefactoringTooltip() {
        return RefactoringLocalize.suggestedRefactoringDisabledGutterIconTooltip();
    }

    private final Project myProject;

    private @Nullable Data myData;

    private final Map<Editor, RangeHighlighter> myEditorsAndHighlighters = new HashMap<>();

    private final CaretListener myCaretListener = new CaretListener() {
        @Override
        public void caretPositionChanged(CaretEvent event) {
            updateHighlighter(event.getEditor());
        }
    };

    public SuggestedRefactoringAvailabilityIndicator(Project project) {
        myProject = project;

        EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryListener() {
            @Override
            public void editorCreated(EditorFactoryEvent event) {
                attachToEditor(event.getEditor());
            }

            @Override
            public void editorReleased(EditorFactoryEvent event) {
                myEditorsAndHighlighters.remove(event.getEditor());
            }
        }, project);
    }

    public Project getProject() {
        return myProject;
    }

    @RequiredUIAccess
    public void show(
        Document document,
        TextRange markerRange,
        TextRange availabilityRange,
        boolean refactoringEnabled,
        LocalizeValue tooltip,
        LocalizeValue intentionText
    ) {
        UIAccess.assertIsUIThread();

        RangeMarker availabilityRangeMarker = document.createRangeMarker(availabilityRange);
        availabilityRangeMarker.setGreedyToLeft(true);
        availabilityRangeMarker.setGreedyToRight(true);
        Data newData = new Data(
            document,
            document.createRangeMarker(markerRange),
            availabilityRangeMarker,
            refactoringEnabled,
            tooltip,
            intentionText
        );
        if (newData.equals(myData)) {
            return;
        }

        clear();

        myData = newData;

        for (Editor editor : EditorFactory.getInstance().getEditors(document, myProject)) {
            attachToEditor(editor);
        }
    }

    @RequiredUIAccess
    public void clear() {
        UIAccess.assertIsUIThread();
        if (myData == null) {
            return;
        }

        myData = null;
        for (Map.Entry<Editor, RangeHighlighter> entry : myEditorsAndHighlighters.entrySet()) {
            Editor editor = entry.getKey();
            editor.getCaretModel().removeCaretListener(myCaretListener);
            editor.getMarkupModel().removeHighlighter(entry.getValue());
        }
        myEditorsAndHighlighters.clear();
    }

    @RequiredUIAccess
    public void disable() {
        UIAccess.assertIsUIThread();
        Data data = myData;
        if (data == null) {
            return;
        }
        if (data.refactoringEnabled()) {
            TextRange highlighterRange = TextRangeUtils.asTextRange(data.highlighterRangeMarker());
            TextRange availabilityRange = TextRangeUtils.asTextRange(data.availabilityRangeMarker());
            if (highlighterRange == null || availabilityRange == null) {
                return;
            }
            show(data.document(), highlighterRange, availabilityRange, false, data.tooltip(), data.intentionText());
        }
    }

    @RequiredUIAccess
    public boolean editorHasRefactoringAvailable(Editor editor) {
        UIAccess.assertIsUIThread();

        return myEditorsAndHighlighters.get(editor) != null;
    }

    public boolean hasData() {
        return myData != null;
    }

    private void attachToEditor(Editor editor) {
        Data data = myData;
        if (data != null && editor.getDocument() == data.document()) {
            editor.getCaretModel().addCaretListener(myCaretListener);
            updateHighlighter(editor);
        }
    }

    private void updateHighlighter(Editor editor) {
        UIAccess.assertIsUIThread();

        RangeHighlighter prevHighlighter = myEditorsAndHighlighters.get(editor);
        if (prevHighlighter != null) {
            editor.getMarkupModel().removeHighlighter(prevHighlighter);
            myEditorsAndHighlighters.remove(editor);
        }

        Data data = myData;
        if (data == null) {
            return;
        }
        TextRange range = TextRangeUtils.asTextRange(data.availabilityRangeMarker());
        if (range == null) {
            return;
        }
        if (!range.containsOffset(editor.getCaretModel().getOffset())) {
            return;
        }
        TextRange highlighterRange = TextRangeUtils.asTextRange(data.highlighterRangeMarker());
        if (highlighterRange == null) {
            return;
        }

        RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(
            highlighterRange.getStartOffset(),
            highlighterRange.getEndOffset(),
            HighlighterLayer.LAST,
            new TextAttributes(),
            HighlighterTargetArea.EXACT_RANGE
        );

        if (data.refactoringEnabled()) {
            highlighter.setGutterIconRenderer(new RefactoringAvailableGutterIconRenderer(data.tooltip()));
        }
        else {
            highlighter.setGutterIconRenderer(new RefactoringDisabledGutterIconRenderer(data.tooltip()));
        }

        myEditorsAndHighlighters.put(editor, highlighter);
    }

    @RequiredReadAction
    public void update(
        PsiElement anchor,
        @Nullable SuggestedRefactoringData refactoringData,
        SuggestedRefactoringSupport refactoringSupport
    ) {
        PsiFile psiFile = anchor.getContainingFile();
        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(psiFile.getProject());
        Document document = Objects.requireNonNull(psiDocumentManager.getDocument(psiFile));
        if (!psiDocumentManager.isCommitted(document)) {
            throw new IllegalArgumentException("Document must be committed");
        }

        boolean refactoringAvailable;
        LocalizeValue tooltip;
        TextRange markerRange;
        TextRange availabilityRange;

        if (refactoringData instanceof SuggestedRenameData renameData) {
            refactoringAvailable = true;
            tooltip = RefactoringLocalize.suggestedRefactoringRenameGutterIconTooltip(
                renameData.getOldName(),
                renameData.getNewName(),
                intentionActionShortcutHint()
            );
            markerRange = Objects.requireNonNull(refactoringSupport.nameRange(renameData.getDeclaration()));
            availabilityRange = markerRange;
        }
        else if (refactoringData instanceof SuggestedChangeSignatureData changeSignatureData) {
            refactoringAvailable = true;
            tooltip = RefactoringLocalize.suggestedRefactoringChangeSignatureGutterIconTooltip(
                changeSignatureData.getNameOfStuffToUpdate(),
                changeSignatureData.getOldSignature().getName(),
                intentionActionShortcutHint()
            );
            markerRange = Objects.requireNonNull(refactoringSupport.nameRange(changeSignatureData.getAnchor()));
            availabilityRange = changeSignatureAvailabilityRange(refactoringSupport, changeSignatureData.getAnchor());
        }
        else {
            refactoringAvailable = false;
            tooltip = disabledRefactoringTooltip();
            markerRange = Objects.requireNonNull(refactoringSupport.nameRange(anchor));
            availabilityRange = changeSignatureAvailabilityRange(refactoringSupport, anchor);
        }

        LocalizeValue intentionText = refactoringData != null ? refactoringData.getIntentionText() : LocalizeValue.empty();
        Runnable doUpdate = () -> {
            if (availabilityRange != null) {
                show(document, markerRange, availabilityRange, refactoringAvailable, tooltip, intentionText);
            }
            else {
                clear();
            }
        };

        Application application = Application.get();
        if (application.isDispatchThread()) {
            doUpdate.run();
        }
        else {
            long modificationStamp = document.getModificationStamp();
            application.invokeLater(() -> {
                if (document.getModificationStamp() == modificationStamp) {
                    doUpdate.run();
                }
            });
        }
    }

    private static String intentionActionShortcutHint() {
        KeyboardShortcut shortcut = ActionManager.getInstance().getKeyboardShortcut("ShowIntentionActions");
        if (shortcut == null) {
            return "";
        }
        return "(" + KeymapUtil.getShortcutText(shortcut) + ")";
    }

    @RequiredReadAction
    static @Nullable TextRange changeSignatureAvailabilityRange(SuggestedRefactoringSupport refactoringSupport, PsiElement anchor) {
        PsiFile file = anchor.getContainingFile();
        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(file.getProject());
        Document document = Objects.requireNonNull(psiDocumentManager.getDocument(file));
        if (!psiDocumentManager.isCommitted(document)) {
            throw new IllegalArgumentException("Document must be committed");
        }
        TextRange signatureRange = refactoringSupport.signatureRange(anchor);
        if (signatureRange == null) {
            return null;
        }
        return TextRangeUtils.extend(signatureRange, document.getCharsSequence(), c -> c == ' ' || c == '\t');
    }
}
