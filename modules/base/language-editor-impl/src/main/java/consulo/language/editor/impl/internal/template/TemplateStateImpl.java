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

package consulo.language.editor.impl.internal.template;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.dumb.IndexNotReadyException;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.event.CaretAdapter;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.codeEditor.util.EditorModificationUtil;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.DocumentReference;
import consulo.document.DocumentReferenceManager;
import consulo.document.RangeMarker;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.document.internal.DocumentEx;
import consulo.document.util.DocumentUtil;
import consulo.document.util.TextRange;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.editor.CodeInsightSettings;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.completion.lookup.event.LookupAdapter;
import consulo.language.editor.completion.lookup.event.LookupEvent;
import consulo.language.editor.completion.lookup.event.LookupListener;
import consulo.language.editor.internal.LanguageEditorInternalHelper;
import consulo.language.editor.template.*;
import consulo.language.editor.template.event.TemplateEditingListener;
import consulo.language.editor.template.macro.MacroCallNode;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.util.AttachmentFactoryUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.BasicUndoableAction;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.ProjectUndoManager;
import consulo.undoRedo.event.CommandAdapter;
import consulo.undoRedo.event.CommandEvent;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.BiPredicate;

public class TemplateStateImpl implements TemplateState {
    private static final Logger LOG = Logger.getInstance(TemplateStateImpl.class);
    private Project myProject;
    private Editor myEditor;

    private TemplateImpl myTemplate;
    private TemplateImpl myPrevTemplate;
    private TemplateSegments mySegments = null;
    private Map<String, String> myPredefinedVariableValues;

    private RangeMarker myTemplateRange = null;
    private final List<RangeHighlighter> myTabStopHighlighters = new ArrayList<>();
    private int myCurrentVariableNumber = -1;
    private int myCurrentSegmentNumber = -1;
    private boolean ourLookupShown = false;

    private boolean myDocumentChangesTerminateTemplate = true;
    private boolean myDocumentChanged = false;

    @Nullable
    private CommandAdapter myCommandListener;
    @Nullable
    private CaretListener myCaretListener;
    @Nullable
    private LookupListener myLookupListener;

    private final List<TemplateEditingListener> myListeners = Lists.newLockFreeCopyOnWriteList();
    private DocumentAdapter myEditorDocumentListener;
    private final Map myProperties = new HashMap();
    private boolean myTemplateIndented = false;
    private Document myDocument;
    private boolean myFinished;
    @Nullable
    private BiPredicate<String, String> myProcessor;
    private boolean mySelectionCalculated = false;
    private boolean myStarted;

    TemplateStateImpl(@Nonnull Project project, @Nonnull Editor editor) {
        myProject = project;
        myEditor = editor;
        myDocument = myEditor.getDocument();
    }

    private void initListeners() {
        myEditorDocumentListener = new DocumentAdapter() {
            @Override
            public void beforeDocumentChange(DocumentEvent e) {
                myDocumentChanged = true;
            }
        };
        myLookupListener = new LookupAdapter() {
            @Override
            @RequiredUIAccess
            public void itemSelected(LookupEvent event) {
                if (isCaretOutsideCurrentSegment()) {
                    if (isCaretInsideNextVariable()) {
                        nextTab();
                    }
                    else {
                        gotoEnd(true);
                    }
                }
            }
        };
        LookupManager.getInstance(myProject).addPropertyChangeListener(
            evt -> {
                if (LookupManager.PROP_ACTIVE_LOOKUP.equals(evt.getPropertyName())) {
                    Lookup lookup = (Lookup)evt.getNewValue();
                    if (lookup != null) {
                        lookup.addLookupListener(myLookupListener);
                    }
                }
            },
            this
        );
        myCommandListener = new CommandAdapter() {
            boolean started = false;

            @Override
            public void commandStarted(CommandEvent event) {
                myDocumentChangesTerminateTemplate = isCaretOutsideCurrentSegment();
                started = true;
            }

            @Override
            public void beforeCommandFinished(CommandEvent event) {
                if (started && !isDisposed()) {
                    @RequiredUIAccess
                    Runnable runnable = () -> afterChangedUpdate();
                    LookupEx lookup = myEditor != null ? LookupManager.getActiveLookup(myEditor) : null;
                    if (lookup != null) {
                        lookup.performGuardedChange(runnable);
                    }
                    else {
                        runnable.run();
                    }
                }
            }
        };

        myCaretListener = new CaretAdapter() {
            @Override
            public void caretAdded(CaretEvent e) {
                if (isMultiCaretMode()) {
                    finishTemplateEditing();
                }
            }

            @Override
            public void caretRemoved(CaretEvent e) {
                if (isMultiCaretMode()) {
                    finishTemplateEditing();
                }
            }
        };

        if (myEditor != null) {
            myEditor.getCaretModel().addCaretListener(myCaretListener);
        }
        myDocument.addDocumentListener(myEditorDocumentListener, this);
        CommandProcessor.getInstance().addCommandListener(myCommandListener, this);
    }

    private boolean isCaretInsideNextVariable() {
        if (myEditor != null && myCurrentVariableNumber >= 0) {
            int nextVar = getNextVariableNumber(myCurrentVariableNumber);
            TextRange range = nextVar < 0 ? null : getVariableRange(myTemplate.getVariableNameAt(nextVar));
            return range != null && range.containsOffset(myEditor.getCaretModel().getOffset());
        }
        return false;
    }

    private boolean isCaretOutsideCurrentSegment() {
        if (myEditor != null && myCurrentSegmentNumber >= 0) {
            int offset = myEditor.getCaretModel().getOffset();
            return offset < mySegments.getSegmentStart(myCurrentSegmentNumber) || offset > mySegments.getSegmentEnd(myCurrentSegmentNumber);
        }
        return false;
    }

    private boolean isMultiCaretMode() {
        return myEditor != null && myEditor.getCaretModel().getCaretCount() > 1;
    }

    @Override
    public synchronized void dispose() {
        if (myLookupListener != null) {
            LookupEx lookup = myEditor != null ? LookupManager.getActiveLookup(myEditor) : null;
            if (lookup != null) {
                lookup.removeLookupListener(myLookupListener);
            }
            myLookupListener = null;
        }

        myEditorDocumentListener = null;
        myCommandListener = null;
        myCaretListener = null;

        myProcessor = null;

        //Avoid the leak of the editor
        releaseAll();
        myDocument = null;
    }

    public boolean isToProcessTab() {
        if (isCaretOutsideCurrentSegment()) {
            return false;
        }
        if (ourLookupShown) {
            LookupEx lookup = LookupManager.getActiveLookup(myEditor);
            if (lookup != null && !lookup.isFocused()) {
                return true;
            }
        }

        return !ourLookupShown;
    }

    private void setCurrentVariableNumber(int variableNumber) {
        myCurrentVariableNumber = variableNumber;
        boolean isFinished = isFinished();
        if (myDocument != null) {
            ((DocumentEx)myDocument).setStripTrailingSpacesEnabled(isFinished);
        }
        myCurrentSegmentNumber = isFinished ? -1 : getCurrentSegmentNumber();
    }

    @Override
    @Nullable
    public TextResult getVariableValue(@Nonnull String variableName) {
        if (variableName.equals(TemplateImpl.SELECTION)) {
            return new TextResult(StringUtil.notNullize(getSelectionBeforeTemplate()));
        }
        if (variableName.equals(TemplateImpl.END)) {
            return new TextResult("");
        }
        if (myPredefinedVariableValues != null) {
            String text = myPredefinedVariableValues.get(variableName);
            if (text != null) {
                return new TextResult(text);
            }
        }
        CharSequence text = myDocument.getCharsSequence();
        int segmentNumber = myTemplate.getVariableSegmentNumber(variableName);
        if (segmentNumber < 0 || mySegments.getSegmentsCount() <= segmentNumber) {
            return null;
        }
        int start = mySegments.getSegmentStart(segmentNumber);
        int end = mySegments.getSegmentEnd(segmentNumber);
        int length = myDocument.getTextLength();
        if (start > length || end > length) {
            return null;
        }
        return new TextResult(text.subSequence(start, end).toString());
    }

    @Nullable
    private String getSelectionBeforeTemplate() {
        return (String)getProperties().get(ExpressionContext.SELECTION);
    }

    @Override
    @Nullable
    public TextRange getCurrentVariableRange() {
        int number = getCurrentSegmentNumber();
        if (number == -1) {
            return null;
        }
        return new TextRange(mySegments.getSegmentStart(number), mySegments.getSegmentEnd(number));
    }

    @Nullable
    @Override
    public TextRange getVariableRange(String variableName) {
        int segment = myTemplate.getVariableSegmentNumber(variableName);
        if (segment < 0) {
            return null;
        }

        return new TextRange(mySegments.getSegmentStart(segment), mySegments.getSegmentEnd(segment));
    }

    @Override
    public int getSegmentsCount() {
        return mySegments.getSegmentsCount();
    }

    @Nonnull
    @Override
    public TextRange getSegmentRange(int segment) {
        return new TextRange(mySegments.getSegmentStart(segment), mySegments.getSegmentEnd(segment));
    }

    @Override
    public boolean isFinished() {
        return myCurrentVariableNumber < 0;
    }

    private void releaseAll() {
        if (mySegments != null) {
            mySegments.removeAll();
            mySegments = null;
        }
        if (myTemplateRange != null) {
            myTemplateRange.dispose();
            myTemplateRange = null;
        }
        myPrevTemplate = myTemplate;
        myTemplate = null;
        myProject = null;
        releaseEditor();
    }

    private void releaseEditor() {
        if (myEditor != null) {
            for (RangeHighlighter segmentHighlighter : myTabStopHighlighters) {
                segmentHighlighter.dispose();
            }
            myTabStopHighlighters.clear();
            myEditor = null;
        }
    }

    @RequiredUIAccess
    public void start(
        @Nonnull TemplateImpl template,
        @Nullable BiPredicate<String, String> processor,
        @Nullable Map<String, String> predefinedVarValues
    ) {
        LOG.assertTrue(!myStarted, "Already started");
        myStarted = true;

        PsiFile file = getPsiFile();
        myTemplate = template;

        myProcessor = processor;

        DocumentReference[] refs =
            myDocument != null ? new DocumentReference[]{DocumentReferenceManager.getInstance().create(myDocument)} : null;
        ProjectUndoManager.getInstance(myProject).undoableActionPerformed(new BasicUndoableAction(refs) {
            @Override
            public void undo() {
                if (myDocument != null) {
                    fireTemplateCancelled();
                    LookupManager.getInstance(myProject).hideActiveLookup();
                    int oldVar = myCurrentVariableNumber;
                    setCurrentVariableNumber(-1);
                    currentVariableChanged(oldVar);
                }
            }

            @Override
            public void redo() {
                //TODO:
                // throw new UnexpectedUndoException("Not implemented");
            }
        });
        myTemplateIndented = false;
        myCurrentVariableNumber = -1;
        mySegments = new TemplateSegments(myEditor);
        myPrevTemplate = myTemplate;

        //myArgument = argument;
        myPredefinedVariableValues = predefinedVarValues;

        if (myTemplate.isInline()) {
            int caretOffset = myEditor.getCaretModel().getOffset();
            myTemplateRange = myDocument.createRangeMarker(caretOffset, caretOffset + myTemplate.getTemplateText().length());
        }
        else {
            preprocessTemplate(file, myEditor.getCaretModel().getOffset(), myTemplate.getTemplateText());
            int caretOffset = myEditor.getCaretModel().getOffset();
            myTemplateRange = myDocument.createRangeMarker(caretOffset, caretOffset);
        }
        myTemplateRange.setGreedyToLeft(true);
        myTemplateRange.setGreedyToRight(true);

        processAllExpressions(myTemplate);
    }

    private void fireTemplateCancelled() {
        if (myFinished) {
            return;
        }
        myFinished = true;
        for (TemplateEditingListener listener : myListeners) {
            listener.templateCancelled(myTemplate);
        }
    }

    private void preprocessTemplate(PsiFile file, int caretOffset, String textToInsert) {
        myProject.getApplication().getExtensionPoint(TemplatePreprocessor.class).forEachExtensionSafe(preprocessor -> {
            preprocessor.preprocessTemplate(myEditor, file, caretOffset, textToInsert, myTemplate.getTemplateText());
        });
    }

    @RequiredUIAccess
    private void processAllExpressions(@Nonnull TemplateImpl template) {
        Application.get().runWriteAction(() -> {
            if (!template.isInline()) {
                myDocument.insertString(myTemplateRange.getStartOffset(), template.getTemplateText());
            }
            for (int i = 0; i < template.getSegmentsCount(); i++) {
                int segmentOffset = myTemplateRange.getStartOffset() + template.getSegmentOffset(i);
                mySegments.addSegment(segmentOffset, segmentOffset);
            }

            LOG.assertTrue(myTemplateRange.isValid(), getRangesDebugInfo());
            calcResults(false);
            LOG.assertTrue(myTemplateRange.isValid(), getRangesDebugInfo());
            calcResults(false);  //Fixed SCR #[vk500] : all variables should be recalced twice on start.
            LOG.assertTrue(myTemplateRange.isValid(), getRangesDebugInfo());
            doReformat(null);

            int nextVariableNumber = getNextVariableNumber(-1);

            if (nextVariableNumber >= 0) {
                fireWaitingForInput();
            }

            if (nextVariableNumber == -1) {
                finishTemplateEditing();
            }
            else {
                setCurrentVariableNumber(nextVariableNumber);
                initTabStopHighlighters();
                initListeners();
                focusCurrentExpression();
                currentVariableChanged(-1);
                if (isMultiCaretMode()) {
                    finishTemplateEditing();
                }
            }
        });
    }

    private String getRangesDebugInfo() {
        return myTemplateRange + "\ntemplateKey: " + myTemplate.getKey() + "\ntemplateText: " + myTemplate.getTemplateText() + "\ntemplateString: " + myTemplate;
    }

    @RequiredUIAccess
    private void doReformat(TextRange range) {
        RangeMarker rangeMarker = null;
        if (range != null) {
            rangeMarker = myDocument.createRangeMarker(range);
            rangeMarker.setGreedyToLeft(true);
            rangeMarker.setGreedyToRight(true);
        }
        RangeMarker finalRangeMarker = rangeMarker;
        Runnable action = () -> {
            IntList indices = initEmptyVariables();
            mySegments.setSegmentsGreedy(false);
            LOG.assertTrue(
                myTemplateRange.isValid(),
                "template key: " + myTemplate.getKey() + "; " +
                    "template text" + myTemplate.getTemplateText() + "; " +
                    "variable number: " + getCurrentVariableNumber()
            );
            reformat(finalRangeMarker);
            mySegments.setSegmentsGreedy(true);
            restoreEmptyVariables(indices);
        };
        Application.get().runWriteAction(action);
    }

    public void setSegmentsGreedy(boolean greedy) {
        mySegments.setSegmentsGreedy(greedy);
    }

    public void setTabStopHighlightersGreedy(boolean greedy) {
        for (RangeHighlighter highlighter : myTabStopHighlighters) {
            highlighter.setGreedyToLeft(greedy);
            highlighter.setGreedyToRight(greedy);
        }
    }

    @RequiredUIAccess
    private void shortenReferences() {
        Application.get().runWriteAction(() -> {
            PsiFile file = getPsiFile();
            if (file != null) {
                IntList indices = initEmptyVariables();
                mySegments.setSegmentsGreedy(false);
                myProject.getApplication().getExtensionPoint(TemplateOptionalProcessor.class).forEachExtensionSafe(processor -> {
                    if (processor.isEnabled(myTemplate)) {
                        processor.processText(myProject, myTemplate, myDocument, myTemplateRange, myEditor);
                    }
                });
                mySegments.setSegmentsGreedy(true);
                restoreEmptyVariables(indices);
            }
        });
    }

    @RequiredUIAccess
    private void afterChangedUpdate() {
        if (isFinished()) {
            return;
        }
        LOG.assertTrue(myTemplate != null, presentTemplate(myPrevTemplate));
        if (myDocumentChanged) {
            if (myDocumentChangesTerminateTemplate || mySegments.isInvalid()) {
                int oldIndex = myCurrentVariableNumber;
                setCurrentVariableNumber(-1);
                currentVariableChanged(oldIndex);
                fireTemplateCancelled();
            }
            else {
                calcResults(true);
            }
            myDocumentChanged = false;
        }
    }

    private static String presentTemplate(@Nullable TemplateImpl template) {
        if (template == null) {
            return "no template";
        }

        String message = StringUtil.notNullize(template.getKey());
        message += "\n\nTemplate#string: " + StringUtil.notNullize(template.getString());
        message += "\n\nTemplate#text: " + StringUtil.notNullize(template.getTemplateText());
        return message;
    }

    private String getExpressionString(int index) {
        CharSequence text = myDocument.getCharsSequence();

        if (!mySegments.isValid(index)) {
            return "";
        }

        int start = mySegments.getSegmentStart(index);
        int end = mySegments.getSegmentEnd(index);

        return text.subSequence(start, end).toString();
    }

    private int getCurrentSegmentNumber() {
        int varNumber = myCurrentVariableNumber;
        if (varNumber == -1) {
            return -1;
        }
        String variableName = myTemplate.getVariableNameAt(varNumber);
        int segmentNumber = myTemplate.getVariableSegmentNumber(variableName);
        if (segmentNumber < 0) {
            LOG.error(
                "No segment for variable: var=" + varNumber + "; name=" + variableName +
                    "; " + presentTemplate(myTemplate) + "; offset: " + myEditor.getCaretModel().getOffset(),
                AttachmentFactoryUtil.createAttachment(myDocument)
            );
        }
        return segmentNumber;
    }

    private void focusCurrentExpression() {
        if (isFinished()) {
            return;
        }

        PsiDocumentManager.getInstance(myProject).commitDocument(myDocument);

        int currentSegmentNumber = getCurrentSegmentNumber();

        lockSegmentAtTheSameOffsetIfAny();

        if (currentSegmentNumber < 0) {
            return;
        }
        int start = mySegments.getSegmentStart(currentSegmentNumber);
        int end = mySegments.getSegmentEnd(currentSegmentNumber);
        if (end >= 0) {
            myEditor.getCaretModel().moveToOffset(end);
            myEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
            myEditor.getSelectionModel().removeSelection();
            myEditor.getSelectionModel().setSelection(start, end);
        }

        DumbService.getInstance(myProject).withAlternativeResolveEnabled(() -> {
            Expression expressionNode = getCurrentExpression();
            List<TemplateExpressionLookupElement> lookupItems;
            try {
                lookupItems = getCurrentExpressionLookupItems();
            }
            catch (IndexNotReadyException e) {
                lookupItems = Collections.emptyList();
            }
            PsiFile psiFile = getPsiFile();
            if (!lookupItems.isEmpty()) {
                if (((TemplateManagerImpl)TemplateManager.getInstance(myProject)).shouldSkipInTests()) {
                    insertSingleItem(lookupItems);
                }
                else {
                    for (LookupElement lookupItem : lookupItems) {
                        assert lookupItem != null : expressionNode;
                    }

                    runLookup(lookupItems, expressionNode.getAdvertisingText());
                }
            }
            else {
                try {
                    Result result = expressionNode.calculateResult(getCurrentExpressionContext());
                    if (result != null) {
                        result.handleFocused(
                            psiFile,
                            myDocument,
                            mySegments.getSegmentStart(currentSegmentNumber),
                            mySegments.getSegmentEnd(currentSegmentNumber)
                        );
                    }
                }
                catch (IndexNotReadyException ignore) {
                }
            }
        });
        focusCurrentHighlighter(true);
    }

    PsiFile getPsiFile() {
        return PsiDocumentManager.getInstance(myProject).getPsiFile(myDocument);
    }

    @RequiredUIAccess
    private void insertSingleItem(List<TemplateExpressionLookupElement> lookupItems) {
        TemplateExpressionLookupElement first = lookupItems.get(0);
        EditorModificationUtil.insertStringAtCaret(myEditor, first.getLookupString());
        first.handleTemplateInsert(lookupItems, Lookup.AUTO_INSERT_SELECT_CHAR);
    }

    @Nonnull
    List<TemplateExpressionLookupElement> getCurrentExpressionLookupItems() {
        LookupElement[] elements = getCurrentExpression().calculateLookupItems(getCurrentExpressionContext());
        if (elements == null) {
            return Collections.emptyList();
        }

        List<TemplateExpressionLookupElement> result = new ArrayList<>();
        for (int i = 0; i < elements.length; i++) {
            result.add(new TemplateExpressionLookupElement(this, elements[i], i));
        }
        return result;
    }

    ExpressionContext getCurrentExpressionContext() {
        return createExpressionContext(mySegments.getSegmentStart(getCurrentSegmentNumber()));
    }

    Expression getCurrentExpression() {
        return myTemplate.getExpressionAt(myCurrentVariableNumber);
    }

    private void runLookup(List<TemplateExpressionLookupElement> lookupItems, @Nullable String advertisingText) {
        if (myEditor == null) {
            return;
        }

        LookupManager lookupManager = LookupManager.getInstance(myProject);

        LookupEx lookup = lookupManager.showLookup(myEditor, lookupItems.toArray(new LookupElement[lookupItems.size()]));
        if (lookup == null) {
            return;
        }

        if (CodeInsightSettings.getInstance().AUTO_POPUP_COMPLETION_LOOKUP
            && !LanguageEditorInternalHelper.getInstance().isInlineRefactoringActive(myEditor)) {
            lookup.setStartCompletionWhenNothingMatches(true);
        }

        if (advertisingText != null) {
            lookup.addAdvertisement(advertisingText, null);
        }
        lookup.refreshUi(true, true);
        ourLookupShown = true;
        lookup.addLookupListener(new LookupAdapter() {
            @Override
            public void lookupCanceled(LookupEvent event) {
                lookup.removeLookupListener(this);
                ourLookupShown = false;
            }

            @Override
            @RequiredUIAccess
            public void itemSelected(LookupEvent event) {
                lookup.removeLookupListener(this);
                if (isFinished()) {
                    return;
                }
                ourLookupShown = false;

                LookupElement item = event.getItem();
                if (item instanceof TemplateExpressionLookupElement templateExpressionLookupElement) {
                    templateExpressionLookupElement.handleTemplateInsert(lookupItems, event.getCompletionChar());
                }
            }
        });
    }

    private void unblockDocument() {
        PsiDocumentManager.getInstance(myProject).commitDocument(myDocument);
        PsiDocumentManager.getInstance(myProject).doPostponedOperationsAndUnblockDocument(myDocument);
    }

    // Hours spent fixing code : 3
    @RequiredUIAccess
    void calcResults(boolean isQuick) {
        if (myProcessor != null && myCurrentVariableNumber >= 0) {
            String variableName = myTemplate.getVariableNameAt(myCurrentVariableNumber);
            TextResult value = getVariableValue(variableName);
            if (value != null && !value.getText().isEmpty()) {
                if (!myProcessor.test(variableName, value.getText())) {
                    finishTemplateEditing(); // nextTab(); ?
                    return;
                }
            }
        }

        fixOverlappedSegments(myCurrentSegmentNumber);

        CommandProcessor.getInstance().newCommand()
            .project(myProject)
            .inWriteAction()
            .run(() -> {
                if (isDisposed()) {
                    return;
                }
                BitSet calcedSegments = new BitSet();
                int maxAttempts = (myTemplate.getVariableCount() + 1) * 3;

                do {
                    maxAttempts--;
                    calcedSegments.clear();
                    for (int i = myCurrentVariableNumber + 1; i < myTemplate.getVariableCount(); i++) {
                        String variableName = myTemplate.getVariableNameAt(i);
                        int segmentNumber = myTemplate.getVariableSegmentNumber(variableName);
                        if (segmentNumber < 0) {
                            continue;
                        }
                        Expression expression = myTemplate.getExpressionAt(i);
                        Expression defaultValue = myTemplate.getDefaultValueAt(i);
                        String oldValue = getVariableValueText(variableName);
                        DumbService.getInstance(myProject)
                            .withAlternativeResolveEnabled(() -> recalcSegment(segmentNumber, isQuick, expression, defaultValue));
                        TextResult value = getVariableValue(variableName);
                        assert value != null : "name=" + variableName + "\ntext=" + myTemplate.getTemplateText();
                        String newValue = value.getText();
                        if (!newValue.equals(oldValue)) {
                            calcedSegments.set(segmentNumber);
                        }
                    }

                    List<TemplateDocumentChange> changes = new ArrayList<>();
                    boolean selectionCalculated = false;
                    for (int i = 0; i < myTemplate.getSegmentsCount(); i++) {
                        if (!calcedSegments.get(i)) {
                            String variableName = myTemplate.getSegmentName(i);
                            if (variableName.equals(TemplateImpl.SELECTION)) {
                                if (mySelectionCalculated) {
                                    continue;
                                }
                                selectionCalculated = true;
                            }
                            if (TemplateImpl.END.equals(variableName)) {
                                continue; // No need to update end since it can be placed over some other variable
                            }
                            String newValue = getVariableValueText(variableName);
                            int start = mySegments.getSegmentStart(i);
                            int end = mySegments.getSegmentEnd(i);
                            changes.add(new TemplateDocumentChange(newValue, start, end, i));
                        }
                    }
                    executeChanges(changes);
                    if (selectionCalculated) {
                        mySelectionCalculated = true;
                    }
                }
                while (!calcedSegments.isEmpty() && maxAttempts >= 0);
            });
    }

    private static class TemplateDocumentChange {
        public final String newValue;
        public final int startOffset;
        public final int endOffset;
        public final int segmentNumber;

        private TemplateDocumentChange(String newValue, int startOffset, int endOffset, int segmentNumber) {
            this.newValue = newValue;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.segmentNumber = segmentNumber;
        }
    }

    private void executeChanges(@Nonnull List<TemplateDocumentChange> changes) {
        if (isDisposed() || changes.isEmpty()) {
            return;
        }
        if (changes.size() > 1) {
            ContainerUtil.sort(changes, (o1, o2) -> {
                int startDiff = o2.startOffset - o1.startOffset;
                return startDiff != 0 ? startDiff : o2.endOffset - o1.endOffset;
            });
        }
        DocumentUtil.executeInBulk(myDocument, true, () -> {
            for (TemplateDocumentChange change : changes) {
                replaceString(change.newValue, change.startOffset, change.endOffset, change.segmentNumber);
            }
        });
    }

    /**
     * Must be invoked on every segment change in order to avoid ovelapping editing segment with its neibours
     */
    private void fixOverlappedSegments(int currentSegment) {
        if (currentSegment >= 0) {
            int currentSegmentStart = mySegments.getSegmentStart(currentSegment);
            int currentSegmentEnd = mySegments.getSegmentEnd(currentSegment);
            for (int i = 0; i < mySegments.getSegmentsCount(); i++) {
                if (i > currentSegment) {
                    int startOffset = mySegments.getSegmentStart(i);
                    if (currentSegmentStart <= startOffset && startOffset < currentSegmentEnd) {
                        mySegments.replaceSegmentAt(i, currentSegmentEnd, Math.max(mySegments.getSegmentEnd(i), currentSegmentEnd), true);
                    }
                }
                else if (i < currentSegment) {
                    int endOffset = mySegments.getSegmentEnd(i);
                    if (currentSegmentStart < endOffset && endOffset <= currentSegmentEnd) {
                        mySegments.replaceSegmentAt(
                            i,
                            Math.min(mySegments.getSegmentStart(i), currentSegmentStart),
                            currentSegmentStart,
                            true
                        );
                    }
                }
            }
        }
    }

    @Nonnull
    private String getVariableValueText(String variableName) {
        TextResult value = getVariableValue(variableName);
        return value != null ? value.getText() : "";
    }

    @RequiredUIAccess
    private void recalcSegment(int segmentNumber, boolean isQuick, Expression expressionNode, Expression defaultValue) {
        String oldValue = getExpressionString(segmentNumber);
        int start = mySegments.getSegmentStart(segmentNumber);
        int end = mySegments.getSegmentEnd(segmentNumber);
        boolean commitDocument = !isQuick || expressionNode.requiresCommittedPSI();

        if (commitDocument) {
            PsiDocumentManager.getInstance(myProject).commitDocument(myDocument);
        }

        PsiFile psiFile = getPsiFile();
        PsiElement element = psiFile.findElementAt(start);
        if (element != null) {
            PsiUtilCore.ensureValid(element);
        }

        ExpressionContext context = createExpressionContext(start);
        Result result = isQuick ? expressionNode.calculateQuickResult(context) : expressionNode.calculateResult(context);
        if (isQuick && result == null) {
            if (!oldValue.isEmpty()) {
                return;
            }
        }

        boolean resultIsNullOrEmpty = result == null || result.equalsToText("", element);

        // do not update default value of neighbour segment
        if (resultIsNullOrEmpty &&
            myCurrentSegmentNumber >= 0 &&
            (mySegments.getSegmentStart(segmentNumber) == mySegments.getSegmentEnd(myCurrentSegmentNumber) ||
                mySegments.getSegmentEnd(segmentNumber) == mySegments.getSegmentStart(myCurrentSegmentNumber))) {
            return;
        }
        if (defaultValue != null && resultIsNullOrEmpty) {
            result = defaultValue.calculateResult(context);
        }
        if (element != null) {
            PsiUtilCore.ensureValid(element);
        }
        if (result == null || result.equalsToText(oldValue, element)) {
            return;
        }

        replaceString(StringUtil.notNullize(result.toString()), start, end, segmentNumber);

        if (result instanceof RecalculatableResult recalculatableResult) {
            IntList indices = initEmptyVariables();
            shortenReferences();
            PsiDocumentManager.getInstance(myProject).commitDocument(myDocument);
            recalculatableResult.handleRecalc(
                psiFile,
                myDocument,
                mySegments.getSegmentStart(segmentNumber),
                mySegments.getSegmentEnd(segmentNumber)
            );
            restoreEmptyVariables(indices);
        }
    }

    private void replaceString(String newValue, int start, int end, int segmentNumber) {
        TextRange range = TextRange.create(start, end);
        if (!TextRange.from(0, myDocument.getCharsSequence().length()).contains(range)) {
            LOG.error(
                "Diagnostic for EA-54980. Can't extract " + range + " range. " + presentTemplate(myTemplate),
                AttachmentFactoryUtil.createAttachment(myDocument)
            );
        }
        String oldText = range.subSequence(myDocument.getCharsSequence()).toString();

        if (!oldText.equals(newValue)) {
            mySegments.setNeighboursGreedy(segmentNumber, false);
            myDocument.replaceString(start, end, newValue);
            int newEnd = start + newValue.length();
            mySegments.replaceSegmentAt(segmentNumber, start, newEnd);
            mySegments.setNeighboursGreedy(segmentNumber, true);
            fixOverlappedSegments(segmentNumber);
        }
    }

    @Override
    public int getCurrentVariableNumber() {
        return myCurrentVariableNumber;
    }

    @RequiredUIAccess
    public void previousTab() {
        if (isFinished()) {
            return;
        }

        myDocumentChangesTerminateTemplate = false;

        int oldVar = myCurrentVariableNumber;
        int previousVariableNumber = getPreviousVariableNumber(oldVar);
        if (previousVariableNumber >= 0) {
            focusCurrentHighlighter(false);
            calcResults(false);
            doReformat(null);
            setCurrentVariableNumber(previousVariableNumber);
            focusCurrentExpression();
            currentVariableChanged(oldVar);
        }
    }

    @Override
    @RequiredUIAccess
    public void nextTab() {
        if (isFinished()) {
            return;
        }

        //some psi operations may block the document, unblock here
        unblockDocument();

        myDocumentChangesTerminateTemplate = false;

        int oldVar = myCurrentVariableNumber;
        int nextVariableNumber = getNextVariableNumber(oldVar);
        if (nextVariableNumber == -1) {
            calcResults(false);
            Application.get().runWriteAction(() -> reformat(null));
            finishTemplateEditing();
            return;
        }
        focusCurrentHighlighter(false);
        calcResults(false);
        doReformat(null);
        setCurrentVariableNumber(nextVariableNumber);
        focusCurrentExpression();
        currentVariableChanged(oldVar);
    }

    @RequiredUIAccess
    public void considerNextTabOnLookupItemSelected(LookupElement item) {
        if (item != null) {
            ExpressionContext context = getCurrentExpressionContext();
            for (TemplateCompletionProcessor processor : TemplateCompletionProcessor.EP_NAME.getExtensionList()) {
                if (!processor.nextTabOnItemSelected(context, item)) {
                    return;
                }
            }
        }
        TextRange range = getCurrentVariableRange();
        if (range != null && range.getLength() > 0) {
            int caret = myEditor.getCaretModel().getOffset();
            if (caret == range.getEndOffset() || isCaretInsideNextVariable()) {
                nextTab();
            }
            else if (caret > range.getEndOffset()) {
                gotoEnd(true);
            }
        }
    }

    private void lockSegmentAtTheSameOffsetIfAny() {
        mySegments.lockSegmentAtTheSameOffsetIfAny(getCurrentSegmentNumber());
    }

    private ExpressionContext createExpressionContext(int start) {
        return new ExpressionContext() {
            @Override
            public Project getProject() {
                return myProject;
            }

            @Override
            public Editor getEditor() {
                return myEditor;
            }

            @Override
            public int getStartOffset() {
                return start;
            }

            @Override
            public int getTemplateStartOffset() {
                if (myTemplateRange == null) {
                    return -1;
                }
                return myTemplateRange.getStartOffset();
            }

            @Override
            public int getTemplateEndOffset() {
                if (myTemplateRange == null) {
                    return -1;
                }
                return myTemplateRange.getEndOffset();
            }

            @Override
            public <T> T getProperty(Key<T> key) {
                //noinspection unchecked
                return (T)myProperties.get(key);
            }

            @Nullable
            @Override
            @RequiredReadAction
            public PsiElement getPsiElementAtStartOffset() {
                Project project = getProject();
                int templateStartOffset = getTemplateStartOffset();
                int offset = templateStartOffset > 0 ? getTemplateStartOffset() - 1 : getTemplateStartOffset();

                PsiDocumentManager.getInstance(project).commitAllDocuments();

                Editor editor = getEditor();
                if (editor == null) {
                    return null;
                }
                PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
                return file == null ? null : file.findElementAt(offset);
            }
        };
    }

    @Override
    @RequiredUIAccess
    public void gotoEnd(boolean brokenOff) {
        if (isDisposed()) {
            return;
        }
        LookupManager.getInstance(myProject).hideActiveLookup();
        calcResults(false);
        if (!brokenOff) {
            doReformat(null);
        }
        setFinalEditorState(brokenOff);
        cleanupTemplateState(brokenOff);
    }

    /**
     * @deprecated use this#gotoEnd(true)
     */
    @Override
    public void cancelTemplate() {
        if (isDisposed()) {
            return;
        }
        LookupManager.getInstance(myProject).hideActiveLookup();
        cleanupTemplateState(true);
    }

    private void finishTemplateEditing() {
        if (isDisposed()) {
            return;
        }
        LookupManager.getInstance(myProject).hideActiveLookup();
        setFinalEditorState(false);
        cleanupTemplateState(false);
    }

    private void setFinalEditorState(boolean brokenOff) {
        myEditor.getSelectionModel().removeSelection();
        if (brokenOff && !((TemplateManagerImpl)TemplateManager.getInstance(myProject)).shouldSkipInTests()) {
            return;
        }

        int selectionSegment = myTemplate.getVariableSegmentNumber(TemplateImpl.SELECTION);
        int endSegmentNumber =
            selectionSegment >= 0 && getSelectionBeforeTemplate() == null ? selectionSegment : myTemplate.getEndSegmentNumber();
        int offset = -1;
        if (endSegmentNumber >= 0) {
            offset = mySegments.getSegmentStart(endSegmentNumber);
        }
        else {
            if (!myTemplate.isSelectionTemplate() && !myTemplate.isInline()) { //do not move caret to the end of range for selection templates
                offset = myTemplateRange.getEndOffset();
            }
        }

        if (isMultiCaretMode() && getCurrentVariableNumber() > -1) {
            offset = -1; //do not move caret in multicaret mode if at least one tab had been made already
        }

        if (offset >= 0) {
            myEditor.getCaretModel().moveToOffset(offset);
            myEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        }

        int selStart = myTemplate.getSelectionStartSegmentNumber();
        int selEnd = myTemplate.getSelectionEndSegmentNumber();
        if (selStart >= 0 && selEnd >= 0) {
            myEditor.getSelectionModel().setSelection(mySegments.getSegmentStart(selStart), mySegments.getSegmentStart(selEnd));
        }
    }

    boolean isDisposed() {
        return myDocument == null;
    }

    private void cleanupTemplateState(boolean brokenOff) {
        Editor editor = myEditor;
        fireBeforeTemplateFinished(brokenOff);
        if (!isDisposed()) {
            int oldVar = myCurrentVariableNumber;
            setCurrentVariableNumber(-1);
            currentVariableChanged(oldVar);
            TemplateManagerImpl.clearTemplateState(editor);
            fireTemplateFinished(brokenOff);
        }
        myListeners.clear();
        Disposer.dispose(this);
    }

    private int getNextVariableNumber(int currentVariableNumber) {
        for (int i = currentVariableNumber + 1; i < myTemplate.getVariableCount(); i++) {
            if (checkIfTabStop(i)) {
                return i;
            }
        }
        return -1;
    }

    private int getPreviousVariableNumber(int currentVariableNumber) {
        for (int i = currentVariableNumber - 1; i >= 0; i--) {
            if (checkIfTabStop(i)) {
                return i;
            }
        }
        return -1;
    }

    private boolean checkIfTabStop(int currentVariableNumber) {
        Expression expression = myTemplate.getExpressionAt(currentVariableNumber);
        if (expression == null) {
            return false;
        }
        if (myCurrentVariableNumber == -1 && myTemplate.skipOnStart(currentVariableNumber)) {
            return false;
        }
        String variableName = myTemplate.getVariableNameAt(currentVariableNumber);
        if (!(myPredefinedVariableValues != null
            && myPredefinedVariableValues.containsKey(variableName))
            && myTemplate.isAlwaysStopAt(currentVariableNumber)) {
            return true;
        }
        int segmentNumber = myTemplate.getVariableSegmentNumber(variableName);
        if (segmentNumber < 0) {
            return false;
        }
        int start = mySegments.getSegmentStart(segmentNumber);
        ExpressionContext context = createExpressionContext(start);
        Result result = expression.calculateResult(context);
        if (result == null) {
            return true;
        }
        LookupElement[] items = expression.calculateLookupItems(context);
        return items != null && items.length > 1;
    }

    private IntList initEmptyVariables() {
        int endSegmentNumber = myTemplate.getEndSegmentNumber();
        int selStart = myTemplate.getSelectionStartSegmentNumber();
        int selEnd = myTemplate.getSelectionEndSegmentNumber();
        IntList indices = IntLists.newArrayList();
        List<TemplateDocumentChange> changes = new ArrayList<>();
        for (int i = 0; i < myTemplate.getSegmentsCount(); i++) {
            int length = mySegments.getSegmentEnd(i) - mySegments.getSegmentStart(i);
            if (length != 0) {
                continue;
            }
            if (i == endSegmentNumber || i == selStart || i == selEnd) {
                continue;
            }

            String name = myTemplate.getSegmentName(i);
            for (int j = 0; j < myTemplate.getVariableCount(); j++) {
                if (myTemplate.getVariableNameAt(j).equals(name)) {
                    Expression e = myTemplate.getExpressionAt(j);
                    String marker = "a";
                    if (e instanceof MacroCallNode macroCallNode) {
                        marker = macroCallNode.getMacro().getDefaultValue();
                    }
                    changes.add(new TemplateDocumentChange(marker, mySegments.getSegmentStart(i), mySegments.getSegmentEnd(i), i));
                    indices.add(i);
                    break;
                }
            }
        }
        executeChanges(changes);
        return indices;
    }

    private void restoreEmptyVariables(IntList indices) {
        List<TextRange> rangesToRemove = new ArrayList<>();
        for (int i = 0; i < indices.size(); i++) {
            int index = indices.get(i);
            rangesToRemove.add(TextRange.create(mySegments.getSegmentStart(index), mySegments.getSegmentEnd(index)));
        }
        Collections.sort(rangesToRemove, (o1, o2) -> {
            int startDiff = o2.getStartOffset() - o1.getStartOffset();
            return startDiff != 0 ? startDiff : o2.getEndOffset() - o1.getEndOffset();
        });
        DocumentUtil.executeInBulk(
            myDocument,
            true,
            () -> {
                if (isDisposed()) {
                    return;
                }
                for (TextRange range : rangesToRemove) {
                    myDocument.deleteString(range.getStartOffset(), range.getEndOffset());
                }
            }
        );
    }

    private void initTabStopHighlighters() {
        Set<String> vars = new HashSet<>();
        for (int i = 0; i < myTemplate.getVariableCount(); i++) {
            String variableName = myTemplate.getVariableNameAt(i);
            if (!vars.add(variableName)) {
                continue;
            }
            int segmentNumber = myTemplate.getVariableSegmentNumber(variableName);
            if (segmentNumber < 0) {
                continue;
            }
            RangeHighlighter segmentHighlighter = getSegmentHighlighter(segmentNumber, false, false);
            myTabStopHighlighters.add(segmentHighlighter);
        }

        int endSegmentNumber = myTemplate.getEndSegmentNumber();
        if (endSegmentNumber >= 0) {
            RangeHighlighter segmentHighlighter = getSegmentHighlighter(endSegmentNumber, false, true);
            myTabStopHighlighters.add(segmentHighlighter);
        }
    }

    private RangeHighlighter getSegmentHighlighter(int segmentNumber, boolean isSelected, boolean isEnd) {
        TextAttributes lvAttr =
            EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.LIVE_TEMPLATE_ATTRIBUTES);
        TextAttributes attributes = isSelected ? lvAttr : new TextAttributes();
        TextAttributes endAttributes = new TextAttributes();

        int start = mySegments.getSegmentStart(segmentNumber);
        int end = mySegments.getSegmentEnd(segmentNumber);
        RangeHighlighter segmentHighlighter = myEditor.getMarkupModel().addRangeHighlighter(
            start,
            end,
            HighlighterLayer.LAST + 1,
            isEnd ? endAttributes : attributes,
            HighlighterTargetArea.EXACT_RANGE
        );
        segmentHighlighter.setGreedyToLeft(true);
        segmentHighlighter.setGreedyToRight(true);
        return segmentHighlighter;
    }

    private void focusCurrentHighlighter(boolean toSelect) {
        if (isFinished()) {
            return;
        }
        if (myCurrentVariableNumber >= myTabStopHighlighters.size()) {
            return;
        }
        RangeHighlighter segmentHighlighter = myTabStopHighlighters.get(myCurrentVariableNumber);
        if (segmentHighlighter != null) {
            int segmentNumber = getCurrentSegmentNumber();
            RangeHighlighter newSegmentHighlighter = getSegmentHighlighter(segmentNumber, toSelect, false);
            if (newSegmentHighlighter != null) {
                segmentHighlighter.dispose();
                myTabStopHighlighters.set(myCurrentVariableNumber, newSegmentHighlighter);
            }
        }
    }

    @RequiredReadAction
    private void reformat(RangeMarker rangeMarkerToReformat) {
        PsiFile file = getPsiFile();
        if (file != null) {
            CodeStyleManager style = CodeStyleManager.getInstance(myProject);
            DumbService.getInstance(myProject).withAlternativeResolveEnabled(
                () -> myProject.getApplication().getExtensionPoint(TemplateOptionalProcessor.class).forEachExtensionSafe(processor -> {
                    if (processor.isEnabled(myTemplate)) {
                        processor.processText(myProject, myTemplate, myDocument, myTemplateRange, myEditor);
                    }
                })
            );
            PsiDocumentManager.getInstance(myProject).doPostponedOperationsAndUnblockDocument(myDocument);
            // for Python, we need to indent the template even if reformatting is enabled, because otherwise indents would be broken
            // and reformat wouldn't be able to fix them
            if (myTemplate.isToIndent() && !myTemplateIndented) {
                LOG.assertTrue(myTemplateRange.isValid(), presentTemplate(myTemplate));
                smartIndent(myTemplateRange.getStartOffset(), myTemplateRange.getEndOffset());
                myTemplateIndented = true;
            }
            if (myTemplate.isToReformat()) {
                try {
                    int endSegmentNumber = myTemplate.getEndSegmentNumber();
                    PsiDocumentManager.getInstance(myProject).commitDocument(myDocument);
                    RangeMarker dummyAdjustLineMarkerRange = null;
                    int endVarOffset = -1;
                    if (endSegmentNumber >= 0) {
                        endVarOffset = mySegments.getSegmentStart(endSegmentNumber);
                        TextRange range = TemplateCodeStyleHelper.insertNewLineIndentMarker(file, myDocument, endVarOffset);
                        if (range != null) {
                            dummyAdjustLineMarkerRange = myDocument.createRangeMarker(range);
                        }
                    }
                    int reformatStartOffset = myTemplateRange.getStartOffset();
                    int reformatEndOffset = myTemplateRange.getEndOffset();
                    if (rangeMarkerToReformat != null) {
                        reformatStartOffset = rangeMarkerToReformat.getStartOffset();
                        reformatEndOffset = rangeMarkerToReformat.getEndOffset();
                    }
                    if (dummyAdjustLineMarkerRange == null && endVarOffset >= 0) {
                        // There is a possible case that indent marker element was not inserted (e.g. because there is no blank line
                        // at the target offset). However, we want to reformat white space adjacent to the current template( if any).
                        PsiElement whiteSpaceElement = TemplateCodeStyleHelper.findWhiteSpaceNode(file, endVarOffset);
                        if (whiteSpaceElement != null) {
                            TextRange whiteSpaceRange = whiteSpaceElement.getTextRange();
                            if (whiteSpaceElement.getContainingFile() != null) {
                                // Support injected white space nodes.
                                whiteSpaceRange = InjectedLanguageManager.getInstance(file.getProject())
                                    .injectedToHost(whiteSpaceElement, whiteSpaceRange);
                            }
                            reformatStartOffset = Math.min(reformatStartOffset, whiteSpaceRange.getStartOffset());
                            reformatEndOffset = Math.max(reformatEndOffset, whiteSpaceRange.getEndOffset());
                        }
                    }
                    style.reformatText(file, reformatStartOffset, reformatEndOffset);
                    PsiDocumentManager.getInstance(myProject).commitDocument(myDocument);
                    PsiDocumentManager.getInstance(myProject).doPostponedOperationsAndUnblockDocument(myDocument);

                    if (dummyAdjustLineMarkerRange != null && dummyAdjustLineMarkerRange.isValid()) {
                        //[ven] TODO: [max] correct javadoc reformatting to eliminate isValid() check!!!
                        mySegments.replaceSegmentAt(
                            endSegmentNumber,
                            dummyAdjustLineMarkerRange.getStartOffset(),
                            dummyAdjustLineMarkerRange.getEndOffset()
                        );
                        myDocument.deleteString(dummyAdjustLineMarkerRange.getStartOffset(), dummyAdjustLineMarkerRange.getEndOffset());
                        PsiDocumentManager.getInstance(myProject).commitDocument(myDocument);
                    }
                    if (endSegmentNumber >= 0) {
                        int offset = mySegments.getSegmentStart(endSegmentNumber);
                        int lineStart = myDocument.getLineStartOffset(myDocument.getLineNumber(offset));
                        // if $END$ is at line start, put it at correct indentation
                        if (myDocument.getCharsSequence().subSequence(lineStart, offset).toString().trim().isEmpty()) {
                            int adjustedOffset = style.adjustLineIndent(file, offset);
                            mySegments.replaceSegmentAt(endSegmentNumber, adjustedOffset, adjustedOffset);
                        }
                    }
                }
                catch (IncorrectOperationException e) {
                    LOG.error(e);
                }
            }
        }
    }

    private void smartIndent(int startOffset, int endOffset) {
        int startLineNum = myDocument.getLineNumber(startOffset);
        int endLineNum = myDocument.getLineNumber(endOffset);
        if (endLineNum == startLineNum) {
            return;
        }

        int selectionIndent = -1;
        int selectionStartLine = -1;
        int selectionEndLine = -1;
        int selectionSegment = myTemplate.getVariableSegmentNumber(TemplateImpl.SELECTION);
        if (selectionSegment >= 0) {
            int selectionStart = myTemplate.getSegmentOffset(selectionSegment);
            selectionIndent = 0;
            String templateText = myTemplate.getTemplateText();
            while (selectionStart > 0 && templateText.charAt(selectionStart - 1) == ' ') {
                // TODO handle tabs
                selectionIndent++;
                selectionStart--;
            }
            selectionStartLine = myDocument.getLineNumber(mySegments.getSegmentStart(selectionSegment));
            selectionEndLine = myDocument.getLineNumber(mySegments.getSegmentEnd(selectionSegment));
        }

        int indentLineNum = startLineNum;

        int lineLength = 0;
        for (; indentLineNum >= 0; indentLineNum--) {
            lineLength = myDocument.getLineEndOffset(indentLineNum) - myDocument.getLineStartOffset(indentLineNum);
            if (lineLength > 0) {
                break;
            }
        }
        if (indentLineNum < 0) {
            return;
        }
        StringBuilder buffer = new StringBuilder();
        CharSequence text = myDocument.getCharsSequence();
        for (int i = 0; i < lineLength; i++) {
            char ch = text.charAt(myDocument.getLineStartOffset(indentLineNum) + i);
            if (ch != ' ' && ch != '\t') {
                break;
            }
            buffer.append(ch);
        }
        if (buffer.length() == 0 && selectionIndent <= 0 || startLineNum >= endLineNum) {
            return;
        }
        String stringToInsert = buffer.toString();
        int finalSelectionStartLine = selectionStartLine;
        int finalSelectionEndLine = selectionEndLine;
        int finalSelectionIndent = selectionIndent;
        DocumentUtil.executeInBulk(
            myDocument,
            true,
            () -> {
                for (int i = startLineNum + 1; i <= endLineNum; i++) {
                    if (i > finalSelectionStartLine && i <= finalSelectionEndLine) {
                        myDocument.insertString(myDocument.getLineStartOffset(i), StringUtil.repeatSymbol(' ', finalSelectionIndent));
                    }
                    else {
                        myDocument.insertString(myDocument.getLineStartOffset(i), stringToInsert);
                    }
                }
            }
        );
    }

    @Override
    public void addTemplateStateListener(TemplateEditingListener listener) {
        myListeners.add(listener);
    }

    private void fireTemplateFinished(boolean brokenOff) {
        if (myFinished) {
            return;
        }
        myFinished = true;
        for (TemplateEditingListener listener : myListeners) {
            listener.templateFinished(ObjectUtil.chooseNotNull(myTemplate, myPrevTemplate), brokenOff);
        }
    }

    private void fireBeforeTemplateFinished(boolean brokenOff) {
        for (TemplateEditingListener listener : myListeners) {
            listener.beforeTemplateFinished(this, myTemplate, brokenOff);
        }
    }

    private void fireWaitingForInput() {
        for (TemplateEditingListener listener : myListeners) {
            listener.waitingForInput(myTemplate);
        }
    }

    private void currentVariableChanged(int oldIndex) {
        for (TemplateEditingListener listener : myListeners) {
            listener.currentVariableChanged(this, myTemplate, oldIndex, myCurrentVariableNumber);
        }
        if (myCurrentSegmentNumber < 0) {
            if (myCurrentVariableNumber >= 0) {
                LOG.error("A variable with no segment: " + myCurrentVariableNumber + "; " + presentTemplate(myTemplate));
            }
            Disposer.dispose(this);
        }
    }

    public Map getProperties() {
        return myProperties;
    }

    public TemplateImpl getTemplate() {
        return myTemplate;
    }

    @Nonnull
    @Override
    public Editor getEditor() {
        return myEditor;
    }
}