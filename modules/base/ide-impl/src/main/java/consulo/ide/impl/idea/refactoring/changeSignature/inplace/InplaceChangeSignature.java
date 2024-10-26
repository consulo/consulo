/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.idea.refactoring.changeSignature.inplace;

import consulo.application.Result;
import consulo.codeEditor.*;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.document.util.TextRange;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.highlight.HighlightManager;
import consulo.language.editor.refactoring.changeSignature.ChangeInfo;
import consulo.language.editor.refactoring.changeSignature.ChangeSignatureHandler;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.rename.inplace.InplaceRefactoring;
import consulo.language.findUsage.DescriptiveNameUtil;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.PositionTracker;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.NonFocusableCheckBox;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.BalloonBuilder;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.undoRedo.internal.FinishMarkAction;
import consulo.undoRedo.internal.StartMarkAction;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class InplaceChangeSignature implements DocumentListener {
    public static final Key<InplaceChangeSignature> INPLACE_CHANGE_SIGNATURE = Key.create("EditorInplaceChangeSignature");
    private ChangeInfo myCurrentInfo;
    private ChangeInfo myStableChange;
    private String myInitialSignature;
    private String myInitialName;
    private Editor myEditor;
    private LanguageChangeSignatureDetector<ChangeInfo> myDetector;

    private final Project myProject;
    private final PsiDocumentManager myDocumentManager;
    private final ArrayList<RangeHighlighter> myHighlighters = new ArrayList<>();
    private StartMarkAction myMarkAction;
    private Balloon myBalloon;
    private boolean myDelegate;
    private EditorEx myPreview;

    @RequiredUIAccess
    public InplaceChangeSignature(Project project, Editor editor, @Nonnull PsiElement element) {
        myDocumentManager = PsiDocumentManager.getInstance(project);
        myProject = project;
        try {
            myMarkAction = StartMarkAction.start(editor.getDocument(), project, ChangeSignatureHandler.REFACTORING_NAME.get());
        }
        catch (StartMarkAction.AlreadyStartedException e) {
            final int exitCode = Messages.showYesNoDialog(
                myProject,
                e.getMessage(),
                ChangeSignatureHandler.REFACTORING_NAME.get(),
                "Navigate to Started",
                CommonLocalize.buttonCancel().get(),
                UIUtil.getErrorIcon()
            );
            if (exitCode == Messages.CANCEL) {
                return;
            }
            PsiElement method = myStableChange.getMethod();
            VirtualFile virtualFile = PsiUtilCore.getVirtualFile(method);
            new OpenFileDescriptorImpl(project, virtualFile, method.getTextOffset()).navigate(true);
            return;
        }


        myEditor = editor;
        myDetector = LanguageChangeSignatureDetector.forLanguage(element.getLanguage());
        myStableChange = myDetector.createInitialChangeInfo(element);
        myInitialSignature = myDetector.extractSignature(myStableChange);
        myInitialName = DescriptiveNameUtil.getDescriptiveName(myStableChange.getMethod());
        TextRange highlightingRange = myDetector.getHighlightingRange(myStableChange);

        HighlightManager highlightManager = HighlightManager.getInstance(myProject);
        TextAttributes attributes =
            EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.LIVE_TEMPLATE_ATTRIBUTES);
        highlightManager.addRangeHighlight(
            editor,
            highlightingRange.getStartOffset(),
            highlightingRange.getEndOffset(),
            attributes,
            false,
            myHighlighters
        );
        for (RangeHighlighter highlighter : myHighlighters) {
            highlighter.setGreedyToRight(true);
            highlighter.setGreedyToLeft(true);
        }
        myEditor.getDocument().addDocumentListener(this);
        myEditor.putUserData(INPLACE_CHANGE_SIGNATURE, this);
        myPreview = InplaceRefactoring.createPreviewComponent(project, myDetector.getFileType());
        showBalloon();
    }

    @Nullable
    public static InplaceChangeSignature getCurrentRefactoring(@Nonnull Editor editor) {
        return editor.getUserData(INPLACE_CHANGE_SIGNATURE);
    }

    public ChangeInfo getCurrentInfo() {
        return myCurrentInfo;
    }

    public String getInitialName() {
        return myInitialName;
    }

    public String getInitialSignature() {
        return myInitialSignature;
    }

    @Nonnull
    public ChangeInfo getStableChange() {
        return myStableChange;
    }

    public void cancel() {
        TextRange highlightingRange = myDetector.getHighlightingRange(getStableChange());
        Document document = myEditor.getDocument();
        String initialSignature = myInitialSignature;
        detach();
        temporallyRevertChanges(highlightingRange, document, initialSignature, myProject);
    }

    @Override
    public void documentChanged(DocumentEvent event) {
        RangeMarker marker = event.getDocument().createRangeMarker(event.getOffset(), event.getOffset());
        myDocumentManager.performWhenAllCommitted(() -> {
            if (myDetector == null) {
                return;
            }
            PsiFile file = myDocumentManager.getPsiFile(event.getDocument());
            if (file == null) {
                return;
            }
            PsiElement element = file.findElementAt(marker.getStartOffset());
            marker.dispose();
            if (element == null || myDetector.ignoreChanges(element)) {
                return;
            }

            if (element instanceof PsiWhiteSpace) {
                PsiElement method = myStableChange.getMethod();
                if (PsiTreeUtil.skipSiblingsForward(element, PsiWhiteSpace.class) == method) {
                    return;
                }
            }

            if (!myDetector.isChangeSignatureAvailableOnElement(element, myStableChange)) {
                detach();
                return;
            }

            updateCurrentInfo();
        });
    }

    private void updateCurrentInfo() {
        if (myCurrentInfo == null) {
            myCurrentInfo = myStableChange;
        }
        String signature = myDetector.extractSignature(myCurrentInfo);
        ChangeInfo changeInfo = myDetector.createNextChangeInfo(signature, myCurrentInfo, myDelegate);
        if (changeInfo == null && myCurrentInfo != null) {
            myStableChange = myCurrentInfo;
        }
        if (changeInfo != null) {
            updateMethodSignature(changeInfo);
        }
        myCurrentInfo = changeInfo;
    }

    private void updateMethodSignature(ChangeInfo changeInfo) {
        ArrayList<TextRange> deleteRanges = new ArrayList<>();
        ArrayList<TextRange> newRanges = new ArrayList<>();
        String methodSignature = myDetector.getMethodSignaturePreview(changeInfo, deleteRanges, newRanges);

        myPreview.getMarkupModel().removeAllHighlighters();
        new WriteCommandAction(null) {
            @Override
            protected void run(@Nonnull Result result) throws Throwable {
                myPreview.getDocument().replaceString(0, myPreview.getDocument().getTextLength(), methodSignature);
            }
        }.execute();
        TextAttributes deprecatedAttributes =
            EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.DEPRECATED_ATTRIBUTES);
        for (TextRange range : deleteRanges) {
            myPreview.getMarkupModel().addRangeHighlighter(
                range.getStartOffset(),
                range.getEndOffset(),
                HighlighterLayer.ADDITIONAL_SYNTAX,
                deprecatedAttributes,
                HighlighterTargetArea.EXACT_RANGE
            );
        }
        TextAttributes todoAttributes =
            EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.TODO_DEFAULT_ATTRIBUTES);
        for (TextRange range : newRanges) {
            myPreview.getMarkupModel().addRangeHighlighter(
                range.getStartOffset(),
                range.getEndOffset(),
                HighlighterLayer.ADDITIONAL_SYNTAX,
                todoAttributes, HighlighterTargetArea.EXACT_RANGE
            );
        }
    }

    protected void showBalloon() {
        NonFocusableCheckBox checkBox = new NonFocusableCheckBox(RefactoringLocalize.delegationPanelDelegateViaOverloadingMethod().get());
        checkBox.addActionListener(e -> {
            myDelegate = checkBox.isSelected();
            updateCurrentInfo();
        });
        JPanel content = new JPanel(new BorderLayout());
        content.add(new JBLabel("Performed signature modifications:"), BorderLayout.NORTH);
        content.add(myPreview.getComponent(), BorderLayout.CENTER);
        updateMethodSignature(myStableChange);
        content.add(checkBox, BorderLayout.SOUTH);
        final BalloonBuilder balloonBuilder = JBPopupFactory.getInstance().createDialogBalloonBuilder(content, null).setSmallVariant(true);
        myBalloon = balloonBuilder.createBalloon();
        myEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
        myBalloon.show(
            new PositionTracker<>(myEditor.getContentComponent()) {
                @Override
                public RelativePoint recalculateLocation(Balloon object) {
                    int offset = myStableChange.getMethod().getTextOffset();
                    VisualPosition visualPosition = myEditor.offsetToVisualPosition(offset);
                    Point point = myEditor.visualPositionToXY(new VisualPosition(visualPosition.line, visualPosition.column));
                    return new RelativePoint(myEditor.getContentComponent(), point);
                }
            },
            Balloon.Position.above
        );
        Disposer.register(
            myBalloon,
            () -> {
                EditorFactory.getInstance().releaseEditor(myPreview);
                myPreview = null;
            }
        );
    }

    public void detach() {
        myEditor.getDocument().removeDocumentListener(this);
        HighlightManager highlightManager = HighlightManager.getInstance(myProject);
        for (RangeHighlighter highlighter : myHighlighters) {
            highlightManager.removeSegmentHighlighter(myEditor, highlighter);
        }
        myHighlighters.clear();
        myBalloon.hide();
        myDetector = null;
        FinishMarkAction.finish(myProject, myEditor.getDocument(), myMarkAction);
        myEditor.putUserData(INPLACE_CHANGE_SIGNATURE, null);
    }

    public static void temporallyRevertChanges(
        final TextRange signatureRange,
        final Document document,
        final String initialSignature,
        Project project
    ) {
        WriteCommandAction.runWriteCommandAction(
            project,
            () -> {
                document.replaceString(signatureRange.getStartOffset(), signatureRange.getEndOffset(), initialSignature);
                PsiDocumentManager.getInstance(project).commitDocument(document);
            }
        );
    }
}
