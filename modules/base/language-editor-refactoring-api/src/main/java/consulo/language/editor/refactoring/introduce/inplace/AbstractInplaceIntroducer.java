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
package consulo.language.editor.refactoring.introduce.inplace;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.codeEditor.*;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.document.util.TextRange;
import consulo.language.editor.highlight.HighlightManager;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.rename.inplace.InplaceRefactoring;
import consulo.language.editor.template.TemplateManager;
import consulo.language.editor.template.TemplateState;
import consulo.language.editor.template.TextResult;
import consulo.language.psi.*;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.PositionTracker;
import consulo.ui.ex.awt.JBUI;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.internal.StartMarkAction;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author anna
 * @since 2011-03-15
 */
public abstract class AbstractInplaceIntroducer<V extends PsiNameIdentifierOwner, E extends PsiElement> extends InplaceVariableIntroducer<E> {
    protected V myLocalVariable;
    protected RangeMarker myLocalMarker;

    protected final String myExprText;
    private final String myLocalName;

    public static final Key<AbstractInplaceIntroducer> ACTIVE_INTRODUCE = Key.create("ACTIVE_INTRODUCE");

    private EditorEx myPreview;
    private final JComponent myPreviewComponent;

    private DocumentAdapter myDocumentAdapter;
    protected final JPanel myWholePanel;
    private boolean myFinished = false;

    @RequiredUIAccess
    public AbstractInplaceIntroducer(
        Project project,
        Editor editor,
        E expr,
        @Nullable V localVariable,
        E[] occurrences,
        String title,
        FileType languageFileType
    ) {
        super(null, editor, project, title, occurrences, expr);
        myLocalVariable = localVariable;
        if (localVariable != null) {
            PsiElement nameIdentifier = localVariable.getNameIdentifier();
            if (nameIdentifier != null) {
                myLocalMarker = createMarker(nameIdentifier);
            }
        }
        else {
            myLocalMarker = null;
        }
        myExprText = getExpressionText(expr);
        myLocalName = localVariable != null ? localVariable.getName() : null;

        myPreview = createPreviewComponent(project, languageFileType);
        myPreviewComponent = new JPanel(new BorderLayout());
        myPreviewComponent.add(myPreview.getComponent(), BorderLayout.CENTER);
        myPreviewComponent.setBorder(JBUI.Borders.empty(2, 2, 6, 2));

        myWholePanel = new JPanel(new GridBagLayout());
        myWholePanel.setBorder(null);

        showDialogAdvertisement(getActionName());
    }

    @Nullable
    @RequiredReadAction
    protected String getExpressionText(E expr) {
        return expr != null ? expr.getText() : null;
    }

    @RequiredUIAccess
    protected final void setPreviewText(String text) {
        if (myPreview == null) {
            return; //already disposed
        }
        Application.get().runWriteAction(
            () -> myPreview.getDocument().replaceString(0, myPreview.getDocument().getTextLength(), text)
        );
    }

    protected final JComponent getPreviewComponent() {
        return myPreviewComponent;
    }

    protected final Editor getPreviewEditor() {
        return myPreview;
    }


    @Override
    @RequiredUIAccess
    protected StartMarkAction startRename() throws StartMarkAction.AlreadyStartedException {
        return StartMarkAction.start(myEditor.getDocument(), myProject, getCommandName());
    }

    /**
     * Returns ID of the action the shortcut of which is used to show the non-in-place refactoring dialog.
     *
     * @return action ID
     */
    protected abstract String getActionName();

    /**
     * Creates an initial version of the declaration for the introduced element. Note that this method is not called in a write action
     * and most likely needs to create one itself.
     *
     * @param replaceAll whether all occurrences are going to be replaced
     * @param names      the suggested names for the declaration
     * @return the declaration
     */
    @Nullable
    protected abstract V createFieldToStartTemplateOn(boolean replaceAll, String[] names);

    /**
     * Returns the suggested names for the introduced element.
     *
     * @param replaceAll whether all occurrences are going to be replaced
     * @param variable   introduced element declaration, if already created.
     * @return the suggested names
     */
    protected abstract String[] suggestNames(boolean replaceAll, @Nullable V variable);

    protected abstract void performIntroduce();

    protected void performPostIntroduceTasks() {
    }

    public abstract boolean isReplaceAllOccurrences();

    public abstract void setReplaceAllOccurrences(boolean allOccurrences);

    @Override
    protected abstract JComponent getComponent();

    protected abstract void saveSettings(@Nonnull V variable);

    @Override
    @RequiredReadAction
    protected abstract V getVariable();

    public abstract E restoreExpression(PsiFile containingFile, V variable, RangeMarker marker, String exprText);

    /**
     * Begins the in-place refactoring operation.
     *
     * @return true if the in-place refactoring was successfully started, false if it failed to start and a dialog should be shown instead.
     */
    @RequiredUIAccess
    public boolean startInplaceIntroduceTemplate() {
        boolean replaceAllOccurrences = isReplaceAllOccurrences();
        return CommandProcessor.getInstance().<Boolean>newCommand()
            .project(myProject)
            .name(LocalizeValue.ofNullable(getCommandName()))
            .groupId(getCommandName())
            .compute(() -> {
                String[] names = suggestNames(replaceAllOccurrences, getLocalVariable());
                V variable = createFieldToStartTemplateOn(replaceAllOccurrences, names);
                boolean started = false;
                if (variable != null) {
                    int caretOffset = getCaretOffset();
                    myEditor.getCaretModel().moveToOffset(caretOffset);
                    myEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);

                    LinkedHashSet<String> nameSuggestions = new LinkedHashSet<>();
                    nameSuggestions.add(variable.getName());
                    nameSuggestions.addAll(Arrays.asList(names));
                    initOccurrencesMarkers();
                    setElementToRename(variable);
                    updateTitle(getVariable());
                    started = AbstractInplaceIntroducer.super.performInplaceRefactoring(nameSuggestions);
                    if (started) {
                        myDocumentAdapter = new DocumentAdapter() {
                            @Override
                            public void documentChanged(DocumentEvent e) {
                                if (myPreview == null) {
                                    return;
                                }
                                TemplateState templateState = TemplateManager.getInstance(myProject).getTemplateState(myEditor);
                                if (templateState != null) {
                                    TextResult value = templateState.getVariableValue(InplaceRefactoring.PRIMARY_VARIABLE_NAME);
                                    if (value != null) {
                                        updateTitle(getVariable(), value.getText());
                                    }
                                }
                            }
                        };
                        myEditor.getDocument().addDocumentListener(myDocumentAdapter);
                        updateTitle(getVariable());
                        if (TemplateManager.getInstance(myProject).getTemplateState(myEditor) != null) {
                            myEditor.putUserData(ACTIVE_INTRODUCE, AbstractInplaceIntroducer.this);
                        }
                    }
                }
                if (!started) {
                    finish(true);
                }
                return started;
            });
    }

    protected int getCaretOffset() {
        RangeMarker r;
        if (myLocalMarker != null) {
            PsiReference reference = myExpr != null ? myExpr.getReference() : null;
            if (reference != null && reference.resolve() == myLocalVariable) {
                r = myExprMarker;
            }
            else {
                r = myLocalMarker;
            }
        }
        else {
            r = myExprMarker;
        }
        return r != null ? r.getStartOffset() : 0;
    }

    @RequiredUIAccess
    protected void updateTitle(@Nullable V variable, String value) {
        if (variable == null) {
            return;
        }

        String variableText = variable.getText();
        PsiElement identifier = variable.getNameIdentifier();
        if (identifier != null) {
            int startOffsetInParent = identifier.getStartOffsetInParent();
            setPreviewText(
                variableText.substring(0, startOffsetInParent) + value +
                    variableText.substring(startOffsetInParent + identifier.getTextLength())
            );
        }
        else {
            setPreviewText(variableText.replaceFirst(variable.getName(), value));
        }
        revalidate();
    }

    @RequiredUIAccess
    protected void updateTitle(@Nullable V variable) {
        if (variable == null) {
            return;
        }
        setPreviewText(variable.getText());
        revalidate();
    }

    protected void revalidate() {
        myWholePanel.revalidate();
        if (myTarget != null) {
            myBalloon.revalidate(new PositionTracker.Static<>(myTarget));
        }
    }

    private boolean myShouldSelect = true;

    @Override
    protected boolean shouldSelectAll() {
        return myShouldSelect;
    }

    @RequiredUIAccess
    public void restartInplaceIntroduceTemplate() {
        CommandProcessor.getInstance().newCommand()
            .project(myProject)
            .name(LocalizeValue.ofNullable(getCommandName()))
            .groupId(getCommandName())
            .run(() -> {
                TemplateState templateState = TemplateManager.getInstance(myProject).getTemplateState(myEditor);
                if (templateState != null) {
                    myEditor.putUserData(INTRODUCE_RESTART, true);
                    try {
                        TextRange range = templateState.getCurrentVariableRange();
                        if (range != null && range.isEmpty()) {
                            String[] names = suggestNames(isReplaceAllOccurrences(), getLocalVariable());
                            Application.get().runWriteAction(
                                () -> myEditor.getDocument().insertString(myEditor.getCaretModel().getOffset(), names[0])
                            );
                        }
                        templateState.gotoEnd(true);
                        try {
                            myShouldSelect = false;
                            startInplaceIntroduceTemplate();
                        }
                        finally {
                            myShouldSelect = true;
                        }
                    }
                    finally {
                        myEditor.putUserData(INTRODUCE_RESTART, false);
                    }
                }
                updateTitle(getVariable());
            });
    }

    @Override
    protected void restoreSelection() {
        if (!shouldSelectAll()) {
            myEditor.getSelectionModel().removeSelection();
        }
    }

    public String getInputName() {
        return myInsertedName;
    }


    @Override
    @RequiredUIAccess
    public void finish(boolean success) {
        myFinished = true;
        TemplateState templateState = TemplateManager.getInstance(myProject).getTemplateState(myEditor);
        if (templateState != null) {
            myEditor.putUserData(ACTIVE_INTRODUCE, null);
        }
        if (myDocumentAdapter != null) {
            myEditor.getDocument().removeDocumentListener(myDocumentAdapter);
        }
        if (myBalloon == null) {
            releaseIfNotRestart();
        }
        super.finish(success);
        if (success) {
            PsiDocumentManager.getInstance(myProject).commitAllDocuments();
            V variable = getVariable();
            if (variable == null) {
                return;
            }
            restoreState(variable);
        }
    }

    @Override
    protected void releaseResources() {
        super.releaseResources();
        if (myPreview == null) {
            return;
        }

        EditorFactory.getInstance().releaseEditor(myPreview);
        myPreview = null;
    }

    @Override
    @RequiredReadAction
    protected void addReferenceAtCaret(Collection<PsiReference> refs) {
        V variable = getLocalVariable();
        if (variable != null) {
            for (PsiReference reference : ReferencesSearch.search(variable)) {
                refs.add(reference);
            }
        }
        else {
            refs.clear();
        }
    }

    @Override
    @RequiredReadAction
    protected void collectAdditionalElementsToRename(List<Pair<PsiElement, TextRange>> stringUsages) {
        if (isReplaceAllOccurrences()) {
            for (E expression : getOccurrences()) {
                LOG.assertTrue(expression.isValid(), expression.getText());
                stringUsages.add(Pair.<PsiElement, TextRange>create(expression, new TextRange(0, expression.getTextLength())));
            }
        }
        else if (getExpr() != null) {
            correctExpression();
            E expr = getExpr();
            LOG.assertTrue(expr.isValid(), expr.getText());
            stringUsages.add(Pair.<PsiElement, TextRange>create(expr, new TextRange(0, expr.getTextLength())));
        }

        V localVariable = getLocalVariable();
        if (localVariable != null) {
            PsiElement nameIdentifier = localVariable.getNameIdentifier();
            if (nameIdentifier != null) {
                int length = nameIdentifier.getTextLength();
                stringUsages.add(Pair.<PsiElement, TextRange>create(nameIdentifier, new TextRange(0, length)));
            }
        }
    }

    protected void correctExpression() {
    }

    @Override
    @RequiredUIAccess
    protected void addHighlights(
        @Nonnull Map<TextRange, TextAttributesKey> ranges,
        @Nonnull Editor editor,
        @Nonnull Collection<RangeHighlighter> highlighters,
        @Nonnull HighlightManager highlightManager
    ) {
        TextAttributesKey attributesKey = EditorColors.SEARCH_RESULT_ATTRIBUTES;
        V variable = getVariable();
        if (variable != null) {
            String name = variable.getName();
            LOG.assertTrue(name != null, variable);
            int variableNameLength = name.length();
            if (isReplaceAllOccurrences()) {
                for (RangeMarker marker : getOccurrenceMarkers()) {
                    int startOffset = marker.getStartOffset();
                    highlightManager.addOccurrenceHighlight(
                        editor,
                        startOffset,
                        startOffset + variableNameLength,
                        attributesKey,
                        0,
                        highlighters
                    );
                }
            }
            else if (getExpr() != null) {
                int startOffset = getExprMarker().getStartOffset();
                highlightManager.addOccurrenceHighlight(
                    editor,
                    startOffset,
                    startOffset + variableNameLength,
                    attributesKey,
                    0,
                    highlighters
                );
            }
        }

        for (RangeHighlighter highlighter : highlighters) {
            highlighter.setGreedyToLeft(true);
            highlighter.setGreedyToRight(true);
        }
    }

    @RequiredUIAccess
    protected void restoreState(V psiField) {
        Application.get().runWriteAction(() -> {
            PsiFile containingFile = psiField.getContainingFile();
            RangeMarker exprMarker = getExprMarker();
            if (exprMarker != null) {
                myExpr = restoreExpression(containingFile, psiField, exprMarker, myExprText);
                if (myExpr != null && myExpr.isPhysical()) {
                    myExprMarker = createMarker(myExpr);
                }
            }
            if (myLocalMarker != null) {
                PsiElement refVariableElement = containingFile.findElementAt(myLocalMarker.getStartOffset());
                if (refVariableElement != null) {
                    PsiElement parent = refVariableElement.getParent();
                    if (parent instanceof PsiNamedElement namedElement) {
                        namedElement.setName(myLocalName);
                    }
                }

                V localVariable = getLocalVariable();
                if (localVariable != null && localVariable.isPhysical()) {
                    myLocalVariable = localVariable;
                    PsiElement nameIdentifier = localVariable.getNameIdentifier();
                    if (nameIdentifier != null) {
                        myLocalMarker = createMarker(nameIdentifier);
                    }
                }
            }
            List<RangeMarker> occurrenceMarkers = getOccurrenceMarkers();
            for (int i = 0, occurrenceMarkersSize = occurrenceMarkers.size(); i < occurrenceMarkersSize; i++) {
                RangeMarker marker = occurrenceMarkers.get(i);
                if (getExprMarker() != null && marker.getStartOffset() == getExprMarker().getStartOffset() && myExpr != null) {
                    myOccurrences[i] = myExpr;
                    continue;
                }
                E psiExpression =
                    restoreExpression(containingFile, psiField, marker, getLocalVariable() != null ? myLocalName : myExprText);
                if (psiExpression != null) {
                    myOccurrences[i] = psiExpression;
                }
            }

            myOccurrenceMarkers = null;
            deleteTemplateField(psiField);
        });
    }

    protected void deleteTemplateField(V psiField) {
        if (psiField.isValid()) {
            psiField.delete();
        }
    }

    @Override
    @RequiredUIAccess
    protected boolean performRefactoring() {
        String newName = getInputName();
        if (getLocalVariable() == null && myExpr == null
            || newName == null || getLocalVariable() != null && !getLocalVariable().isValid()
            || myExpr != null && !myExpr.isValid()) {
            super.moveOffsetAfter(false);
            return false;
        }
        if (getLocalVariable() != null) {
            CommandProcessor.getInstance().newCommand()
                .project(myProject)
                .name(LocalizeValue.ofNullable(getCommandName()))
                .groupId(getCommandName())
                .inWriteAction()
                .compute(() -> getLocalVariable().setName(myLocalName));
        }

        if (!isIdentifier(newName, myExpr != null ? myExpr.getLanguage() : getLocalVariable().getLanguage())) {
            return false;
        }
        CommandProcessor.getInstance().newCommand()
            .project(myProject)
            .name(LocalizeValue.ofNullable(getCommandName()))
            .groupId(getCommandName())
            .run(this::performIntroduce);

        V variable = getVariable();
        if (variable != null) {
            saveSettings(variable);
        }
        return false;
    }

    @Override
    @RequiredReadAction
    protected void moveOffsetAfter(boolean success) {
        if (getLocalVariable() != null && getLocalVariable().isValid()) {
            myEditor.getCaretModel().moveToOffset(getLocalVariable().getTextOffset());
            myEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
        }
        else if (getExprMarker() != null) {
            RangeMarker exprMarker = getExprMarker();
            if (exprMarker.isValid()) {
                myEditor.getCaretModel().moveToOffset(exprMarker.getStartOffset());
                myEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
            }
        }
        super.moveOffsetAfter(success);
        if (myLocalMarker != null && !isRestart()) {
            myLocalMarker.dispose();
        }
        if (success) {
            performPostIntroduceTasks();
        }
    }

    @Override
    @RequiredReadAction
    protected boolean startsOnTheSameElement(RefactoringActionHandler handler, PsiElement element) {
        return super.startsOnTheSameElement(handler, element) || getLocalVariable() == element;
    }

    @RequiredReadAction
    @SuppressWarnings("unchecked")
    public V getLocalVariable() {
        if (myLocalVariable != null && myLocalVariable.isValid()) {
            return myLocalVariable;
        }
        if (myLocalMarker != null) {
            V variable = getVariable();
            PsiFile containingFile;
            if (variable != null) {
                containingFile = variable.getContainingFile();
            }
            else {
                containingFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
            }
            PsiNameIdentifierOwner identifierOwner = PsiTreeUtil.getParentOfType(
                containingFile.findElementAt(myLocalMarker.getStartOffset()),
                PsiNameIdentifierOwner.class,
                false
            );
            return identifierOwner != null && identifierOwner.getClass() == myLocalVariable.getClass() ? (V)identifierOwner : null;

        }
        return myLocalVariable;
    }

    @RequiredUIAccess
    public void stopIntroduce(Editor editor) {
        TemplateState templateState = TemplateManager.getInstance(myProject).getTemplateState(editor);
        if (templateState != null) {
            CommandProcessor.getInstance().newCommand()
                .project(myProject)
                .name(LocalizeValue.ofNullable(getCommandName()))
                .groupId(getCommandName())
                .run(() -> templateState.gotoEnd(true));
        }
    }

    @Override
    @RequiredUIAccess
    protected void navigateToAlreadyStarted(Document oldDocument, int exitCode) {
        finish(true);
        super.navigateToAlreadyStarted(oldDocument, exitCode);
    }

    @Override
    protected void showBalloon() {
        if (myFinished) {
            return;
        }
        super.showBalloon();
    }

    public boolean startsOnTheSameElement(E expr, V localVariable) {
        if (myExprMarker != null && myExprMarker.isValid() && expr != null && myExprMarker.getStartOffset() == expr.getTextOffset()) {
            return true;
        }

        if (myLocalMarker != null && myLocalMarker.isValid() && localVariable != null
            && myLocalMarker.getStartOffset() == localVariable.getTextOffset()) {
            return true;
        }
        return isRestart();
    }

    @Nullable
    public static AbstractInplaceIntroducer getActiveIntroducer(@Nullable Editor editor) {
        if (editor == null) {
            return null;
        }
        return editor.getUserData(ACTIVE_INTRODUCE);
    }
}
