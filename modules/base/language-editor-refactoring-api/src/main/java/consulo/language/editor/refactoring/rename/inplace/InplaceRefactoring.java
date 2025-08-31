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
import consulo.application.util.function.CommonProcessors;
import consulo.application.util.query.Query;
import consulo.codeEditor.*;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributesKey;
import consulo.content.scope.SearchScope;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.language.Language;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupFocusDegree;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.highlight.HighlightManager;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.refactoring.NamesValidator;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.editor.template.*;
import consulo.language.editor.template.event.TemplateEditingAdapter;
import consulo.language.file.FileViewProvider;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.inject.InjectedLanguageManagerUtil;
import consulo.language.psi.*;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.scope.PsiSearchScopeUtil;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.content.ProjectRootManager;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.project.content.scope.ProjectScopes;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.PositionTracker;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.DottedBorder;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.BalloonBuilder;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.internal.FinishMarkAction;
import consulo.undoRedo.internal.StartMarkAction;
import consulo.undoRedo.util.UndoUtil;
import consulo.util.collection.Stack;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author anna
 * @since 2012-01-11
 */
public abstract class InplaceRefactoring {
    protected static final Logger LOG = Logger.getInstance(InplaceRefactoring.class);
    protected static final String PRIMARY_VARIABLE_NAME = "PrimaryVariable";
    protected static final String OTHER_VARIABLE_NAME = "OtherVariable";
    protected static final Stack<InplaceRefactoring> ourRenamersStack = new Stack<>();
    public static final Key<InplaceRefactoring> INPLACE_RENAMER = Key.create("EditorInplaceRenamer");
    public static final Key<Boolean> INTRODUCE_RESTART = Key.create("INTRODUCE_RESTART");

    protected PsiNamedElement myElementToRename;
    protected final Editor myEditor;
    protected final Project myProject;
    protected RangeMarker myRenameOffset;
    protected String myAdvertisementText;
    private ArrayList<RangeHighlighter> myHighlighters;
    protected String myInitialName;
    protected String myOldName;
    protected RangeMarker myBeforeRevert = null;
    protected String myInsertedName;
    protected LinkedHashSet<String> myNameSuggestions;

    protected StartMarkAction myMarkAction;
    protected PsiElement myScope;

    protected RangeMarker myCaretRangeMarker;

    protected Balloon myBalloon;
    protected String myTitle;
    protected RelativePoint myTarget;

    @RequiredReadAction
    public InplaceRefactoring(Editor editor, PsiNamedElement elementToRename, Project project) {
        this(
            editor,
            elementToRename,
            project,
            elementToRename != null ? elementToRename.getName() : null,
            elementToRename != null ? elementToRename.getName() : null
        );
    }

    @RequiredReadAction
    public InplaceRefactoring(Editor editor, PsiNamedElement elementToRename, Project project, String oldName) {
        this(editor, elementToRename, project, elementToRename != null ? elementToRename.getName() : null, oldName);
    }

    @RequiredReadAction
    public InplaceRefactoring(Editor editor, PsiNamedElement elementToRename, Project project, String initialName, String oldName) {
        myEditor = /*(editor instanceof EditorWindow)? ((EditorWindow)editor).getDelegate() : */editor;
        myElementToRename = elementToRename;
        myProject = project;
        myOldName = oldName;
        if (myElementToRename != null) {
            myInitialName = initialName;
            PsiFile containingFile = myElementToRename.getContainingFile();
            if (!notSameFile(getTopLevelVirtualFile(containingFile.getViewProvider()), containingFile)
                && myElementToRename != null && myElementToRename.getTextRange() != null) {
                myRenameOffset = myEditor.getDocument().createRangeMarker(myElementToRename.getTextRange());
                myRenameOffset.setGreedyToRight(true);
                myRenameOffset.setGreedyToLeft(true); // todo not sure if we need this
            }
        }
    }

    @RequiredUIAccess
    public static void unableToStartWarning(Project project, Editor editor) {
        StartMarkAction startMarkAction = StartMarkAction.canStart(project);
        String message = startMarkAction.getCommandName() + " is not finished yet.";
        Document oldDocument = startMarkAction.getDocument();
        if (editor == null || oldDocument != editor.getDocument()) {
            int exitCode = Messages.showYesNoDialog(
                project,
                message,
                RefactoringLocalize.cannotPerformRefactoring().get(),
                "Continue Started",
                "Cancel Started",
                UIUtil.getErrorIcon()
            );
            navigateToStarted(oldDocument, project, exitCode);
        }
        else {
            CommonRefactoringUtil.showErrorHint(project, editor, message, RefactoringLocalize.cannotPerformRefactoring().get(), null);
        }
    }

    public void setAdvertisementText(String advertisementText) {
        myAdvertisementText = advertisementText;
    }


    @RequiredUIAccess
    public boolean performInplaceRefactoring(LinkedHashSet<String> nameSuggestions) {
        myNameSuggestions = nameSuggestions;
        if (InjectedLanguageManagerUtil.isInInjectedLanguagePrefixSuffix(myElementToRename)) {
            return false;
        }

        FileViewProvider fileViewProvider = myElementToRename.getContainingFile().getViewProvider();
        VirtualFile file = getTopLevelVirtualFile(fileViewProvider);

        SearchScope referencesSearchScope = getReferencesSearchScope(file);

        Collection<PsiReference> refs = collectRefs(referencesSearchScope);

        addReferenceAtCaret(refs);

        for (PsiReference ref : refs) {
            PsiFile containingFile = ref.getElement().getContainingFile();

            if (notSameFile(file, containingFile)) {
                return false;
            }
        }

        PsiElement scope = checkLocalScope();

        if (scope == null) {
            return false; // Should have valid local search scope for inplace rename
        }

        PsiFile containingFile = scope.getContainingFile();
        if (containingFile == null) {
            return false; // Should have valid local search scope for inplace rename
        }
        //no need to process further when file is read-only
        if (!CommonRefactoringUtil.checkReadOnlyStatus(myProject, containingFile)) {
            return true;
        }

        myEditor.putUserData(INPLACE_RENAMER, this);
        ourRenamersStack.push(this);

        List<Pair<PsiElement, TextRange>> stringUsages = new ArrayList<>();
        collectAdditionalElementsToRename(stringUsages);
        return buildTemplateAndStart(refs, stringUsages, scope, containingFile);
    }

    protected boolean notSameFile(@Nullable VirtualFile file, @Nonnull PsiFile containingFile) {
        return !Comparing.equal(getTopLevelVirtualFile(containingFile.getViewProvider()), file);
    }

    protected SearchScope getReferencesSearchScope(VirtualFile file) {
        return file == null || ProjectRootManager.getInstance(myProject).getFileIndex().isInContent(file)
            ? ProjectScopes.getProjectScope(myElementToRename.getProject())
            : new LocalSearchScope(myElementToRename.getContainingFile());
    }

    @Nullable
    protected PsiElement checkLocalScope() {
        SearchScope searchScope = PsiSearchScopeUtil.getUseScope(myElementToRename);
        if (searchScope instanceof LocalSearchScope localSearchScope) {
            PsiElement[] elements = localSearchScope.getScope();
            return PsiTreeUtil.findCommonParent(elements);
        }

        return null;
    }

    protected abstract void collectAdditionalElementsToRename(List<Pair<PsiElement, TextRange>> stringUsages);

    protected abstract boolean shouldSelectAll();

    @RequiredReadAction
    protected MyLookupExpression createLookupExpression(PsiElement selectedElement) {
        return new MyLookupExpression(
            getInitialName(),
            myNameSuggestions,
            myElementToRename,
            selectedElement,
            shouldSelectAll(),
            myAdvertisementText
        );
    }

    @Nonnull
    @RequiredReadAction
    protected Expression createTemplateExpression(PsiElement selectedElement) {
        return createLookupExpression(selectedElement);
    }

    protected boolean shouldStopAtLookupExpression(Expression expression) {
        return expression instanceof MyLookupExpression;
    }

    protected boolean acceptReference(PsiReference reference) {
        return true;
    }

    protected Collection<PsiReference> collectRefs(SearchScope referencesSearchScope) {
        Query<PsiReference> search = ReferencesSearch.search(myElementToRename, referencesSearchScope, false);

        CommonProcessors.CollectProcessor<PsiReference> processor = new CommonProcessors.CollectProcessor<>() {
            @Override
            protected boolean accept(PsiReference reference) {
                return acceptReference(reference);
            }
        };

        search.forEach(processor);
        return processor.getResults();
    }

    @RequiredUIAccess
    protected boolean buildTemplateAndStart(
        Collection<PsiReference> refs,
        Collection<Pair<PsiElement, TextRange>> stringUsages,
        PsiElement scope,
        PsiFile containingFile
    ) {
        PsiElement context = InjectedLanguageManager.getInstance(containingFile.getProject()).getInjectionHost(containingFile);
        myScope = context != null ? context.getContainingFile() : scope;
        TemplateBuilder builder = TemplateBuilderFactory.getInstance().createTemplateBuilder(myScope);

        PsiElement nameIdentifier = getNameIdentifier();
        int offset = myEditor.getCaretModel().getOffset();
        PsiElement selectedElement = getSelectedInEditorElement(nameIdentifier, refs, stringUsages, offset);

        boolean subrefOnPrimaryElement = false;
        boolean hasReferenceOnNameIdentifier = false;
        for (PsiReference ref : refs) {
            if (isReferenceAtCaret(selectedElement, ref)) {
                Expression expression = createTemplateExpression(selectedElement);
                builder.replaceElement(ref.getElement(), getRangeToRename(ref), PRIMARY_VARIABLE_NAME, expression,
                    shouldStopAtLookupExpression(expression));
                subrefOnPrimaryElement = true;
                continue;
            }
            addVariable(ref, selectedElement, builder, offset);
            hasReferenceOnNameIdentifier |= isReferenceAtCaret(nameIdentifier, ref);
        }
        if (nameIdentifier != null) {
            hasReferenceOnNameIdentifier |= selectedElement.getTextRange().contains(nameIdentifier.getTextRange());
            if (!subrefOnPrimaryElement || !hasReferenceOnNameIdentifier) {
                addVariable(nameIdentifier, selectedElement, builder);
            }
        }
        for (Pair<PsiElement, TextRange> usage : stringUsages) {
            addVariable(usage.first, usage.second, selectedElement, builder);
        }
        addAdditionalVariables(builder);
        try {
            myMarkAction = startRename();
        }
        catch (StartMarkAction.AlreadyStartedException e) {
            Document oldDocument = e.getDocument();
            if (oldDocument != myEditor.getDocument()) {
                int exitCode = Messages.showYesNoCancelDialog(
                    myProject,
                    e.getMessage(),
                    getCommandName(),
                    "Navigate to Started",
                    "Cancel Started",
                    "Cancel",
                    UIUtil.getErrorIcon()
                );
                if (exitCode == Messages.CANCEL) {
                    return true;
                }
                navigateToAlreadyStarted(oldDocument, exitCode);
                return true;
            }
            else {

                if (!ourRenamersStack.isEmpty() && ourRenamersStack.peek() == this) {
                    ourRenamersStack.pop();
                    if (!ourRenamersStack.empty()) {
                        myOldName = ourRenamersStack.peek().myOldName;
                    }
                }

                revertState();
                TemplateState templateState =
                    TemplateManager.getInstance(myProject).getTemplateState(EditorWindow.getTopLevelEditor(myEditor));
                if (templateState != null) {
                    templateState.gotoEnd(true);
                }
            }
            return false;
        }

        beforeTemplateStart();

        CommandProcessor.getInstance().newCommand()
            .project(myProject)
            .name(LocalizeValue.ofNullable(getCommandName()))
            .inWriteAction()
            .run(() -> startTemplate(builder));

        if (myBalloon == null) {
            showBalloon();
        }
        return true;
    }

    @RequiredReadAction
    protected boolean isReferenceAtCaret(PsiElement selectedElement, PsiReference ref) {
        TextRange textRange = ref.getRangeInElement().shiftRight(ref.getElement().getTextRange().getStartOffset());
        if (selectedElement != null) {
            TextRange selectedElementRange = selectedElement.getTextRange();
            LOG.assertTrue(selectedElementRange != null, selectedElement);
            if (selectedElementRange != null && selectedElementRange.contains(textRange)) {
                return true;
            }
        }
        return false;
    }

    protected void beforeTemplateStart() {
        myCaretRangeMarker = myEditor.getDocument()
            .createRangeMarker(new TextRange(myEditor.getCaretModel().getOffset(), myEditor.getCaretModel().getOffset()));
        myCaretRangeMarker.setGreedyToLeft(true);
        myCaretRangeMarker.setGreedyToRight(true);
    }

    @RequiredReadAction
    private void startTemplate(TemplateBuilder builder) {
        final Disposable disposable = Disposable.newDisposable();
        DaemonCodeAnalyzer.getInstance(myProject).disableUpdateByTimer(disposable);

        MyTemplateListener templateListener = new MyTemplateListener() {
            @Override
            protected void restoreDaemonUpdateState() {
                Disposer.dispose(disposable);
            }
        };

        int offset = myEditor.getCaretModel().getOffset();

        Template template = builder.buildInlineTemplate();
        template.setToReformat(false);
        TextRange range = myScope.getTextRange();
        assert range != null;
        myHighlighters = new ArrayList<>();
        Editor topLevelEditor = EditorWindow.getTopLevelEditor(myEditor);
        topLevelEditor.getCaretModel().moveToOffset(range.getStartOffset());

        TemplateManager.getInstance(myProject).startTemplate(topLevelEditor, template, templateListener);
        restoreOldCaretPositionAndSelection(offset);
        highlightTemplateVariables(template, topLevelEditor);
    }

    private void highlightTemplateVariables(Template template, Editor topLevelEditor) {
        //add highlights
        if (myHighlighters != null) { // can be null if finish is called during testing
            Map<TextRange, TextAttributesKey> rangesToHighlight = new HashMap<>();
            TemplateState templateState = TemplateManager.getInstance(myProject).getTemplateState(topLevelEditor);
            if (templateState != null) {
                EditorColorsManager colorsManager = EditorColorsManager.getInstance();
                for (int i = 0; i < templateState.getSegmentsCount(); i++) {
                    TextRange segmentOffset = templateState.getSegmentRange(i);
                    String name = template.getSegmentName(i);
                    TextAttributesKey attributes = null;
                    if (name.equals(PRIMARY_VARIABLE_NAME)) {
                        attributes = EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES;
                    }
                    else if (name.equals(OTHER_VARIABLE_NAME)) {
                        attributes = EditorColors.SEARCH_RESULT_ATTRIBUTES;
                    }

                    if (attributes == null) {
                        continue;
                    }

                    rangesToHighlight.put(segmentOffset, attributes);
                }
            }
            addHighlights(rangesToHighlight, topLevelEditor, myHighlighters, HighlightManager.getInstance(myProject));
        }
    }

    private void restoreOldCaretPositionAndSelection(int offset) {
        //move to old offset
        Runnable runnable = () -> {
            myEditor.getCaretModel().moveToOffset(restoreCaretOffset(offset));
            restoreSelection();
        };

        LookupEx lookup = LookupManager.getActiveLookup(myEditor);
        if (lookup != null && lookup.getLookupStart() <= (restoreCaretOffset(offset))) {
            lookup.setFocusDegree(LookupFocusDegree.UNFOCUSED);
            lookup.performGuardedChange(runnable);
        }
        else {
            runnable.run();
        }
    }

    protected void restoreSelection() {
    }

    protected int restoreCaretOffset(int offset) {
        return myCaretRangeMarker.isValid() ? myCaretRangeMarker.getEndOffset() : offset;
    }

    protected void navigateToAlreadyStarted(Document oldDocument, int exitCode) {
        navigateToStarted(oldDocument, myProject, exitCode);
    }

    private static void navigateToStarted(Document oldDocument, Project project, int exitCode) {
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(oldDocument);
        if (file != null) {
            VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile != null) {
                FileEditor[] editors = FileEditorManager.getInstance(project).getEditors(virtualFile);
                for (FileEditor editor : editors) {
                    if (editor instanceof TextEditor tEditor) {
                        Editor textEditor = tEditor.getEditor();
                        TemplateState templateState = TemplateManager.getInstance(project).getTemplateState(textEditor);
                        if (templateState != null) {
                            if (exitCode == DialogWrapper.OK_EXIT_CODE) {
                                TextRange range = templateState.getVariableRange(PRIMARY_VARIABLE_NAME);
                                if (range != null) {
                                    OpenFileDescriptorFactory.getInstance(project).builder(virtualFile)
                                        .offset(range.getStartOffset())
                                        .build()
                                        .navigate(true);
                                    return;
                                }
                            }
                            else if (exitCode > 0) {
                                templateState.gotoEnd();
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    @Nullable
    @RequiredReadAction
    protected PsiElement getNameIdentifier() {
        return myElementToRename instanceof PsiNameIdentifierOwner nameIdentifierOwner ? nameIdentifierOwner.getNameIdentifier() : null;
    }

    public static EditorEx createPreviewComponent(Project project, FileType languageFileType) {
        Document document = EditorFactory.getInstance().createDocument("");
        UndoUtil.disableUndoFor(document);
        EditorEx previewEditor = (EditorEx)EditorFactory.getInstance().createEditor(document, project, languageFileType, true);
        previewEditor.setOneLineMode(true);
        EditorSettings settings = previewEditor.getSettings();
        settings.setAdditionalLinesCount(0);
        settings.setAdditionalColumnsCount(1);
        settings.setRightMarginShown(false);
        settings.setFoldingOutlineShown(false);
        settings.setLineNumbersShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setIndentGuidesShown(false);
        settings.setVirtualSpace(false);
        previewEditor.setHorizontalScrollbarVisible(false);
        previewEditor.setVerticalScrollbarVisible(false);
        previewEditor.setCaretEnabled(false);
        settings.setLineCursorWidth(1);

        ColorValue bg = previewEditor.getColorsScheme().getColor(EditorColors.CARET_ROW_COLOR);
        previewEditor.setBackgroundColor(bg);
        previewEditor.setBorder(BorderFactory.createCompoundBorder(new DottedBorder(Color.gray), new LineBorder(TargetAWT.to(bg), 2)));

        return previewEditor;
    }

    @Nullable
    @RequiredUIAccess
    protected StartMarkAction startRename() throws StartMarkAction.AlreadyStartedException {
        return CommandProcessor.getInstance().<StartMarkAction>newCommand()
            .project(myProject)
            .name(LocalizeValue.ofNullable(getCommandName()))
            .canThrow(StartMarkAction.AlreadyStartedException.class)
            .compute(() -> StartMarkAction.start(myEditor.getDocument(), myProject, getCommandName()));
    }

    @Nullable
    @RequiredReadAction
    protected PsiNamedElement getVariable() {
        // todo we can use more specific class, shouldn't we?
        //Class clazz = myElementToRename != null? myElementToRename.getClass() : PsiNameIdentifierOwner.class;
        if (myElementToRename != null && myElementToRename.isValid()) {
            if (Comparing.strEqual(myOldName, myElementToRename.getName())) {
                return myElementToRename;
            }
            if (myRenameOffset != null) {
                return PsiTreeUtil.findElementOfClassAtRange(
                    myElementToRename.getContainingFile(),
                    myRenameOffset.getStartOffset(),
                    myRenameOffset.getEndOffset(),
                    PsiNameIdentifierOwner.class
                );
            }
        }

        if (myRenameOffset != null) {
            PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
            if (psiFile != null) {
                return PsiTreeUtil.findElementOfClassAtRange(
                    psiFile,
                    myRenameOffset.getStartOffset(),
                    myRenameOffset.getEndOffset(),
                    PsiNameIdentifierOwner.class
                );
            }
        }
        return myElementToRename;
    }

    /**
     * Called after the completion of the refactoring, either a successful or a failed one.
     *
     * @param success true if the refactoring was accepted, false if it was cancelled (by undo or Esc)
     */
    protected void moveOffsetAfter(boolean success) {
        if (myCaretRangeMarker != null) {
            myCaretRangeMarker.dispose();
        }
    }

    protected void addAdditionalVariables(TemplateBuilder builder) {
    }

    @RequiredReadAction
    protected void addReferenceAtCaret(Collection<PsiReference> refs) {
        PsiFile myEditorFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
        // Note, that myEditorFile can be different from myElement.getContainingFile() e.g. in injections: myElement declaration in one
        // file / usage in another !
        PsiReference reference = (myEditorFile != null ? myEditorFile : myElementToRename.getContainingFile())
            .findReferenceAt(myEditor.getCaretModel().getOffset());
        if (reference instanceof PsiMultiReference psiMultiReference) {
            PsiReference[] references = psiMultiReference.getReferences();
            for (PsiReference ref : references) {
                addReferenceIfNeeded(refs, ref);
            }
        }
        else {
            addReferenceIfNeeded(refs, reference);
        }
    }

    @RequiredReadAction
    private void addReferenceIfNeeded(@Nonnull Collection<PsiReference> refs, @Nullable PsiReference reference) {
        if (reference != null && reference.isReferenceTo(myElementToRename) && !refs.contains(reference)) {
            refs.add(reference);
        }
    }

    protected void showDialogAdvertisement(String actionId) {
        Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
        Shortcut[] shortcuts = keymap.getShortcuts(actionId);
        if (shortcuts.length > 0) {
            setAdvertisementText("Press " + KeymapUtil.getShortcutText(shortcuts[0]) + " to show dialog with more options");
        }
    }

    @RequiredReadAction
    public String getInitialName() {
        if (myInitialName == null) {
            PsiNamedElement variable = getVariable();
            if (variable != null) {
                return variable.getName();
            }
        }
        return myInitialName;
    }

    @RequiredUIAccess
    protected void revertState() {
        if (myOldName == null) {
            return;
        }
        CommandProcessor.getInstance().newCommand()
            .project(myProject)
            .name(LocalizeValue.ofNullable(getCommandName()))
            .run(() -> {
                Editor topLevelEditor = EditorWindow.getTopLevelEditor(myEditor);
                myProject.getApplication().runWriteAction(() -> {
                    TemplateState state = TemplateManager.getInstance(myProject).getTemplateState(topLevelEditor);
                    assert state != null;
                    int segmentsCount = state.getSegmentsCount();
                    Document document = topLevelEditor.getDocument();
                    for (int i = 0; i < segmentsCount; i++) {
                        TextRange segmentRange = state.getSegmentRange(i);
                        document.replaceString(segmentRange.getStartOffset(), segmentRange.getEndOffset(), myOldName);
                    }
                });
                if (!myProject.isDisposed() && myProject.isOpen()) {
                    PsiDocumentManager.getInstance(myProject).commitDocument(topLevelEditor.getDocument());
                }
            });
    }

    /**
     * Returns the name of the command performed by the refactoring.
     *
     * @return command name
     */
    protected abstract String getCommandName();

    public void finish(boolean success) {
        if (!ourRenamersStack.isEmpty() && ourRenamersStack.peek() == this) {
            ourRenamersStack.pop();
        }
        if (myHighlighters != null) {
            if (!myProject.isDisposed()) {
                HighlightManager highlightManager = HighlightManager.getInstance(myProject);
                for (RangeHighlighter highlighter : myHighlighters) {
                    highlightManager.removeSegmentHighlighter(myEditor, highlighter);
                }
            }

            myHighlighters = null;
            myEditor.putUserData(INPLACE_RENAMER, null);
        }
        if (myBalloon != null && !isRestart()) {
            myBalloon.hide();
        }
    }

    protected void addHighlights(
        @Nonnull Map<TextRange, TextAttributesKey> ranges,
        @Nonnull Editor editor,
        @Nonnull Collection<RangeHighlighter> highlighters,
        @Nonnull HighlightManager highlightManager
    ) {
        for (Map.Entry<TextRange, TextAttributesKey> entry : ranges.entrySet()) {
            TextRange range = entry.getKey();
            TextAttributesKey attributesKey = entry.getValue();
            highlightManager.addOccurrenceHighlight(
                editor,
                range.getStartOffset(),
                range.getEndOffset(),
                attributesKey,
                0,
                highlighters
            );
        }

        for (RangeHighlighter highlighter : highlighters) {
            highlighter.setGreedyToLeft(true);
            highlighter.setGreedyToRight(true);
        }
    }

    protected abstract boolean performRefactoring();

    @RequiredReadAction
    private void addVariable(PsiReference reference, PsiElement selectedElement, TemplateBuilder builder, int offset) {
        PsiElement element = reference.getElement();
        if (element == selectedElement && checkRangeContainsOffset(offset, reference.getRangeInElement(), element)) {
            Expression expression = createTemplateExpression(selectedElement);
            builder.replaceElement(reference.getElement(), getRangeToRename(reference), PRIMARY_VARIABLE_NAME, expression,
                shouldStopAtLookupExpression(expression));
        }
        else {
            builder.replaceElement(reference.getElement(), getRangeToRename(reference), OTHER_VARIABLE_NAME, PRIMARY_VARIABLE_NAME, false);
        }
    }

    @RequiredReadAction
    private void addVariable(PsiElement element, PsiElement selectedElement, TemplateBuilder builder) {
        addVariable(element, null, selectedElement, builder);
    }

    @RequiredReadAction
    private void addVariable(
        PsiElement element,
        @Nullable TextRange textRange,
        PsiElement selectedElement,
        TemplateBuilder builder
    ) {
        if (element == selectedElement) {
            Expression expression = createTemplateExpression(myElementToRename);
            builder.replaceElement(element, getRangeToRename(element), PRIMARY_VARIABLE_NAME, expression, shouldStopAtLookupExpression(expression));
        }
        else {
            builder.replaceElement(element, Objects.requireNonNullElseGet(textRange, () -> getRangeToRename(element)), OTHER_VARIABLE_NAME,
                PRIMARY_VARIABLE_NAME, false);
        }
    }

    @Nonnull
    @RequiredReadAction
    protected TextRange getRangeToRename(@Nonnull PsiElement element) {
        return new TextRange(0, element.getTextLength());
    }

    @Nonnull
    @RequiredReadAction
    protected TextRange getRangeToRename(@Nonnull PsiReference reference) {
        return reference.getRangeInElement();
    }

    public void setElementToRename(PsiNamedElement elementToRename) {
        myElementToRename = elementToRename;
    }

    protected boolean isIdentifier(String newName, Language language) {
        NamesValidator namesValidator = NamesValidator.forLanguage(language);
        return namesValidator == null || namesValidator.isIdentifier(newName, myProject);
    }

    protected static VirtualFile getTopLevelVirtualFile(FileViewProvider fileViewProvider) {
        VirtualFile file = fileViewProvider.getVirtualFile();
        return file instanceof VirtualFileWindow virtualFileWindow ? virtualFileWindow.getDelegate() : file;
    }

    @TestOnly
    public static void checkCleared() {
        try {
            assert ourRenamersStack.isEmpty() : ourRenamersStack;
        }
        finally {
            ourRenamersStack.clear();
        }
    }

    @RequiredReadAction
    private PsiElement getSelectedInEditorElement(
        @Nullable PsiElement nameIdentifier,
        Collection<PsiReference> refs,
        Collection<Pair<PsiElement, TextRange>> stringUsages,
        int offset
    ) {
        //prefer reference in case of self-references
        for (PsiReference ref : refs) {
            PsiElement element = ref.getElement();
            if (checkRangeContainsOffset(offset, ref.getRangeInElement(), element)) {
                return element;
            }
        }

        if (nameIdentifier != null) {
            TextRange range = nameIdentifier.getTextRange();
            if (range != null && range.containsOffset(offset)) {
                return nameIdentifier;
            }
        }

        for (Pair<PsiElement, TextRange> stringUsage : stringUsages) {
            if (checkRangeContainsOffset(offset, stringUsage.second, stringUsage.first)) {
                return stringUsage.first;
            }
        }

        LOG.error(nameIdentifier + " by " + this.getClass().getName());
        return null;
    }

    @RequiredReadAction
    private boolean checkRangeContainsOffset(int offset, TextRange textRange, PsiElement element) {
        int startOffset = element.getTextRange().getStartOffset();
        InjectedLanguageManager injectedLanguageManager = InjectedLanguageManager.getInstance(myProject);
        PsiLanguageInjectionHost injectionHost = injectedLanguageManager.getInjectionHost(element);
        if (injectionHost != null) {
            PsiElement nameIdentifier = getNameIdentifier();
            PsiLanguageInjectionHost initialInjectedHost =
                nameIdentifier != null ? injectedLanguageManager.getInjectionHost(nameIdentifier) : null;
            if (initialInjectedHost != null && initialInjectedHost != injectionHost) {
                return false;
            }
        }
        return textRange.shiftRight(startOffset).containsOffset(offset);
    }

    protected boolean isRestart() {
        Boolean isRestart = myEditor.getUserData(INTRODUCE_RESTART);
        return isRestart != null && isRestart;
    }

    @RequiredReadAction
    public static boolean canStartAnotherRefactoring(
        Editor editor,
        Project project,
        RefactoringActionHandler handler,
        PsiElement... element
    ) {
        InplaceRefactoring inplaceRefactoring = getActiveInplaceRenamer(editor);
        return StartMarkAction.canStart(project) == null
            || (inplaceRefactoring != null && element.length == 1 && inplaceRefactoring.startsOnTheSameElement(handler, element[0]));
    }

    public static InplaceRefactoring getActiveInplaceRenamer(Editor editor) {
        return editor != null ? editor.getUserData(INPLACE_RENAMER) : null;
    }

    @RequiredReadAction
    protected boolean startsOnTheSameElement(RefactoringActionHandler handler, PsiElement element) {
        return getVariable() == element;
    }

    protected void releaseResources() {
    }

    @Nullable
    protected JComponent getComponent() {
        return null;
    }

    protected void showBalloon() {
        JComponent component = getComponent();
        if (component == null) {
            return;
        }
        if (myProject.getApplication().isHeadlessEnvironment()) {
            return;
        }
        BalloonBuilder balloonBuilder =
            JBPopupFactory.getInstance().createDialogBalloonBuilder(component, null).setSmallVariant(true);
        myBalloon = balloonBuilder.createBalloon();
        final Editor topLevelEditor = EditorWindow.getTopLevelEditor(myEditor);
        Disposer.register(myProject, myBalloon);
        Disposer.register(
            myBalloon,
            () -> {
                releaseIfNotRestart();
                topLevelEditor.putUserData(EditorPopupHelper.ANCHOR_POPUP_POSITION, null);
            }
        );
        topLevelEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
        JBPopupFactory popupFactory = JBPopupFactory.getInstance();
        myBalloon.show(
            new PositionTracker<>(topLevelEditor.getContentComponent()) {
                @Override
                public RelativePoint recalculateLocation(Balloon object) {
                    EditorPopupHelper editorPopupHelper = EditorPopupHelper.getInstance();
                    if (myTarget != null && !editorPopupHelper.isBestPopupLocationVisible(topLevelEditor)) {
                        return myTarget;
                    }
                    if (myCaretRangeMarker != null && myCaretRangeMarker.isValid()) {
                        topLevelEditor.putUserData(
                            EditorPopupHelper.ANCHOR_POPUP_POSITION,
                            topLevelEditor.offsetToVisualPosition(myCaretRangeMarker.getStartOffset())
                        );
                    }
                    RelativePoint target = editorPopupHelper.guessBestPopupLocation(topLevelEditor);
                    Point screenPoint = target.getScreenPoint();
                    int y = screenPoint.y;
                    if (target.getPoint().getY() > topLevelEditor.getLineHeight() + myBalloon.getPreferredSize().getHeight()) {
                        y -= topLevelEditor.getLineHeight();
                    }
                    myTarget = new RelativePoint(new Point(screenPoint.x, y));
                    return myTarget;
                }
            },
            Balloon.Position.above
        );
    }

    protected void releaseIfNotRestart() {
        if (!isRestart()) {
            releaseResources();
        }
    }

    private abstract class MyTemplateListener extends TemplateEditingAdapter {

        protected abstract void restoreDaemonUpdateState();

        @Override
        public void beforeTemplateFinished(TemplateState templateState, Template template) {
            try {
                TextResult value = templateState.getVariableValue(PRIMARY_VARIABLE_NAME);
                myInsertedName = value != null ? value.toString() : null;

                TextRange range = templateState.getCurrentVariableRange();
                int currentOffset = myEditor.getCaretModel().getOffset();
                if (range == null && myRenameOffset != null) {
                    range = new TextRange(myRenameOffset.getStartOffset(), myRenameOffset.getEndOffset());
                }
                myBeforeRevert = range != null && range.getEndOffset() >= currentOffset && range.getStartOffset() <= currentOffset
                    ? myEditor.getDocument().createRangeMarker(range.getStartOffset(), currentOffset)
                    : null;
                if (myBeforeRevert != null) {
                    myBeforeRevert.setGreedyToRight(true);
                }
                finish(true);
            }
            finally {
                restoreDaemonUpdateState();
            }
        }

        @Override
        public void templateFinished(Template template, boolean brokenOff) {
            boolean bind = false;
            try {
                super.templateFinished(template, brokenOff);
                if (!brokenOff) {
                    bind = performRefactoring();
                }
                moveOffsetAfter(!brokenOff);
            }
            finally {
                if (!bind) {
                    try {
                        ((RealEditor)EditorWindow.getTopLevelEditor(myEditor)).stopDumbLater();
                    }
                    finally {
                        FinishMarkAction.finish(myProject, myEditor.getDocument(), myMarkAction);
                        if (myBeforeRevert != null) {
                            myBeforeRevert.dispose();
                        }
                    }
                }
            }
        }

        @Override
        public void templateCancelled(Template template) {
            try {
                PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);
                documentManager.commitAllDocuments();
                finish(false);
                moveOffsetAfter(false);
            }
            finally {
                try {
                    restoreDaemonUpdateState();
                }
                finally {
                    FinishMarkAction.finish(myProject, myEditor.getDocument(), myMarkAction);
                }
            }
        }
    }
}
