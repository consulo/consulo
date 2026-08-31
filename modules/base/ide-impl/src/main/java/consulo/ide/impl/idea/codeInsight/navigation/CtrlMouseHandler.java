// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.navigation;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.event.*;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.codeEditor.internal.TextAttributesPatcher;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.codeInsight.navigation.actions.GotoDeclarationAction;
import consulo.ide.impl.idea.codeInsight.navigation.actions.GotoTypeDeclarationAction;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.documentation.DocumentationManager;
import consulo.language.editor.documentation.DocumentationManagerProtocol;
import consulo.language.editor.documentation.DocumentationProvider;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.inject.InjectedEditorManager;
import consulo.language.editor.internal.DocumentationManagerHelper;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.language.editor.ui.internal.EditorDocTooltip;
import consulo.language.editor.ui.internal.EditorDocTooltipService;
import consulo.language.psi.*;
import consulo.language.psi.search.DefinitionsScopedSearch;
import consulo.language.psi.util.EditSourceUtil;
import consulo.logging.Logger;
import consulo.navigation.ItemPresentation;
import consulo.navigation.Navigatable;
import consulo.navigation.NavigationItem;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.Point2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.cursor.StandardCursors;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.details.KeyboardInputDetails;
import consulo.ui.event.details.ModifiedInputDetails;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.usage.UsageViewShortNameLocation;
import consulo.usage.UsageViewTypeLocation;
import consulo.usage.UsageViewUtil;
import consulo.util.concurrent.CancellablePromise;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Consumer;

@Singleton
@ServiceAPI(value = ComponentScope.PROJECT)
@ServiceImpl
public final class CtrlMouseHandler {
    private static final Logger LOG = Logger.getInstance(CtrlMouseHandler.class);

    private final Project myProject;

    private HighlightersSet myHighlighter;
    private Set<ModifiedInputDetails.Modifier> myStoredModifiers = Set.of();
    private TooltipProvider myTooltipProvider;
    private @Nullable Point2D myPrevMouseLocation;
    private @Nullable EditorDocTooltip myTooltip;

    public enum BrowseMode {
        None,
        Declaration,
        TypeDeclaration,
        Implementation
    }

    @RequiredUIAccess
    private void handleKeyEvent(consulo.ui.event.KeyEvent event) {
        KeyboardInputDetails inputDetails = event.getInputDetails();
        handleModifiers(inputDetails == null ? Set.of() : inputDetails.getModifiers());
    }

    @RequiredUIAccess
    private void handleModifiers(Set<ModifiedInputDetails.Modifier> modifiers) {
        if (modifiers.equals(myStoredModifiers)) {
            return;
        }

        BrowseMode browseMode = getBrowseMode(modifiers);

        if (browseMode == BrowseMode.None) {
            disposeHighlighter();
            cancelPreviousTooltip();
        }
        else {
            TooltipProvider tooltipProvider = myTooltipProvider;
            if (tooltipProvider != null) {
                if (browseMode != tooltipProvider.getBrowseMode()) {
                    disposeHighlighter();
                }
                myStoredModifiers = modifiers;
                cancelPreviousTooltip();
                myTooltipProvider = new TooltipProvider(tooltipProvider);
                myTooltipProvider.execute(browseMode);
            }
        }
    }

    private final VisibleAreaListener myVisibleAreaListener = __ -> {
        disposeHighlighter();
        cancelPreviousTooltip();
    };

    private final EditorMouseListener myEditorMouseAdapter = new EditorMouseListener() {
        @Override
        public void mouseReleased(EditorMouseEvent e) {
            disposeHighlighter();
            cancelPreviousTooltip();
        }
    };

    private final EditorMouseMotionListener myEditorMouseMotionListener = new EditorMouseMotionListener() {
        @RequiredUIAccess
        @Override
        public void mouseMoved(EditorMouseEvent e) {
            if (e.isConsumed() || !myProject.isInitialized() || myProject.isDisposed()) {
                return;
            }
            InputDetails inputDetails = e.getInputDetails();
            if (inputDetails == null) {
                return;
            }

            Point2D prevLocation = myPrevMouseLocation;
            Point2D location = inputDetails.getPositionOnScreen();
            myPrevMouseLocation = location;
            EditorDocTooltip tooltip = myTooltip;
            if (tooltip != null && tooltip.shouldSuppressMove(prevLocation, location)) {
                return;
            }
            cancelPreviousTooltip();

            myStoredModifiers = inputDetails instanceof ModifiedInputDetails modified ? modified.getModifiers() : Set.of();
            BrowseMode browseMode = getBrowseMode(myStoredModifiers);

            if (browseMode == BrowseMode.None || e.getArea() != EditorMouseEventArea.EDITING_AREA) {
                disposeHighlighter();
                return;
            }

            Editor editor = e.getEditor();
            if (!(editor instanceof EditorEx) || editor.getProject() != null && editor.getProject() != myProject) {
                return;
            }
            if (!e.isOverText()) {
                disposeHighlighter();
                return;
            }
            myTooltipProvider = new TooltipProvider((EditorEx) editor, e.getOffset());
            myTooltipProvider.execute(browseMode);
        }
    };

    private final EditorDocTooltipService myDocTooltipService;

    @Inject
    public CtrlMouseHandler(Project project, EditorDocTooltipService docTooltipService) {
        myProject = project;
        myDocTooltipService = docTooltipService;
    }

    public EditorMouseListener getEditorMouseAdapter() {
        return myEditorMouseAdapter;
    }

    public EditorMouseMotionListener getEditorMouseMotionListener() {
        return myEditorMouseMotionListener;
    }

    public void caretPositionChanged() {
        if (myTooltip != null) {
            DocumentationManager.getInstance(myProject).updateToolwindowContext();
        }
    }

    public void cancelPreviousTooltip() {
        if (myTooltipProvider != null) {
            myTooltipProvider.dispose();
            myTooltipProvider = null;
        }
    }

    private static BrowseMode getBrowseMode(Set<ModifiedInputDetails.Modifier> modifiers) {
        if (!modifiers.isEmpty()) {
            Keymap activeKeymap = KeymapManager.getInstance().getActiveKeymap();
            if (KeymapUtil.matchActionMouseShortcutsModifiers(activeKeymap, modifiers, IdeActions.ACTION_GOTO_DECLARATION)) {
                return BrowseMode.Declaration;
            }
            if (KeymapUtil.matchActionMouseShortcutsModifiers(activeKeymap, modifiers, IdeActions.ACTION_GOTO_TYPE_DECLARATION)) {
                return BrowseMode.TypeDeclaration;
            }
            if (KeymapUtil.matchActionMouseShortcutsModifiers(activeKeymap, modifiers, IdeActions.ACTION_GOTO_IMPLEMENTATION)) {
                return BrowseMode.Implementation;
            }
        }
        return BrowseMode.None;
    }

    @TestOnly
    public static @Nullable String getInfo(PsiElement element, PsiElement atPointer) {
        return generateInfo(element, atPointer, true).text;
    }

    @RequiredReadAction
    @TestOnly
    public static @Nullable String getInfo(Editor editor, BrowseMode browseMode) {
        Project project = editor.getProject();
        if (project == null) {
            return null;
        }
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (file == null) {
            return null;
        }
        Info info = getInfoAt(project, editor, file, editor.getCaretModel().getOffset(), browseMode);
        return info == null ? null : info.getInfo().text;
    }

    private static DocInfo generateInfo(PsiElement element, PsiElement atPointer, boolean fallbackToBasicInfo) {
        DocumentationProvider documentationProvider = DocumentationManagerHelper.getProviderFromElement(element, atPointer);
        String result = documentationProvider.getQuickNavigateInfo(element, atPointer);
        if (result == null && fallbackToBasicInfo) {
            result = doGenerateInfo(element);
        }
        return result == null ? DocInfo.EMPTY : new DocInfo(result, documentationProvider);
    }

    private static @Nullable String doGenerateInfo(PsiElement element) {
        if (element instanceof PsiFile) {
            VirtualFile virtualFile = ((PsiFile) element).getVirtualFile();
            if (virtualFile != null) {
                return virtualFile.getPresentableUrl();
            }
        }

        String info = getQuickNavigateInfo(element);
        if (info != null) {
            return info;
        }

        if (element instanceof NavigationItem) {
            ItemPresentation presentation = ((NavigationItem) element).getPresentation();
            if (presentation != null) {
                return presentation.getPresentableText();
            }
        }

        return null;
    }

    private static @Nullable String getQuickNavigateInfo(PsiElement element) {
        String name = ElementDescriptionUtil.getElementDescription(element, UsageViewShortNameLocation.INSTANCE);
        if (StringUtil.isEmpty(name)) {
            return null;
        }
        String typeName = ElementDescriptionUtil.getElementDescription(element, UsageViewTypeLocation.INSTANCE);
        PsiFile file = element.getContainingFile();
        StringBuilder sb = new StringBuilder();
        if (StringUtil.isNotEmpty(typeName)) {
            sb.append(typeName).append(" ");
        }
        sb.append("\"").append(name).append("\"");
        if (file != null && file.isPhysical()) {
            sb.append(" [").append(file.getName()).append("]");
        }
        return sb.toString();
    }

    public abstract static class Info {
        final PsiElement myElementAtPointer;

        private final List<TextRange> myRanges;

        public Info(PsiElement elementAtPointer, List<TextRange> ranges) {
            myElementAtPointer = elementAtPointer;
            myRanges = ranges;
        }

        public Info(PsiElement elementAtPointer) {
            this(elementAtPointer, getReferenceRanges(elementAtPointer));
        }

        @RequiredReadAction
        private static List<TextRange> getReferenceRanges(PsiElement elementAtPointer) {
            if (!elementAtPointer.isPhysical()) {
                return Collections.emptyList();
            }
            int textOffset = elementAtPointer.getTextOffset();
            TextRange range = elementAtPointer.getTextRange();
            if (range == null) {
                throw new AssertionError("Null range for " + elementAtPointer + " of " + elementAtPointer.getClass());
            }
            if (textOffset < range.getStartOffset() || textOffset < 0) {
                LOG.error("Invalid text offset " + textOffset + " of element " + elementAtPointer + " of " + elementAtPointer.getClass());
                textOffset = range.getStartOffset();
            }
            return Collections.singletonList(new TextRange(textOffset, range.getEndOffset()));
        }

        boolean isSimilarTo(Info that) {
            return Comparing.equal(myElementAtPointer, that.myElementAtPointer) && myRanges.equals(that.myRanges);
        }

        public List<TextRange> getRanges() {
            return myRanges;
        }

        public abstract DocInfo getInfo();

        public abstract boolean isValid(Document document);

        public abstract boolean isNavigatable();

        boolean rangesAreCorrect(Document document) {
            TextRange docRange = new TextRange(0, document.getTextLength());
            for (TextRange range : getRanges()) {
                if (!docRange.contains(range)) {
                    return false;
                }
            }

            return true;
        }
    }

    private static void showDumbModeNotification(Project project) {
        DumbService.getInstance(project).showDumbModeNotification("Element information is not available during index update");
    }

    private static class InfoSingle extends Info {

        private final PsiElement myTargetElement;

        InfoSingle(PsiElement elementAtPointer, PsiElement targetElement) {
            super(elementAtPointer);
            myTargetElement = targetElement;
        }

        @RequiredReadAction
        InfoSingle(PsiReference ref, PsiElement targetElement) {
            super(ref.getElement(), ReferenceRange.getAbsoluteRanges(ref));
            myTargetElement = targetElement;
        }

        @Override
        public DocInfo getInfo() {
            return areElementsValid() ? generateInfo(myTargetElement, myElementAtPointer, isNavigatable()) : DocInfo.EMPTY;
        }

        @RequiredReadAction
        private boolean areElementsValid() {
            return myTargetElement.isValid() && myElementAtPointer.isValid();
        }

        @Override
        public boolean isValid(Document document) {
            return areElementsValid() && rangesAreCorrect(document);
        }

        @Override
        public boolean isNavigatable() {
            return myTargetElement != myElementAtPointer && myTargetElement != myElementAtPointer.getParent();
        }
    }

    private static class InfoMultiple extends Info {
        InfoMultiple(PsiElement elementAtPointer) {
            super(elementAtPointer);
        }

        @RequiredReadAction
        InfoMultiple(PsiElement elementAtPointer, PsiReference ref) {
            super(elementAtPointer, ReferenceRange.getAbsoluteRanges(ref));
        }

        @Override
        public DocInfo getInfo() {
            return new DocInfo(CodeInsightLocalize.multipleImplementationsTooltip().get(), null);
        }

        @Override
        public boolean isValid(Document document) {
            return rangesAreCorrect(document);
        }

        @Override
        public boolean isNavigatable() {
            return true;
        }
    }

    @RequiredReadAction
    private @Nullable Info getInfoAt(Editor editor, PsiFile file, int offset, BrowseMode browseMode) {
        return getInfoAt(myProject, editor, file, offset, browseMode);
    }

    @RequiredReadAction
    public static @Nullable Info getInfoAt(Project project, Editor editor, PsiFile file, int offset, BrowseMode browseMode) {
        PsiElement targetElement = null;

        if (browseMode == BrowseMode.TypeDeclaration) {
            try {
                targetElement = GotoTypeDeclarationAction.findSymbolType(editor, offset);
            }
            catch (IndexNotReadyException e) {
                showDumbModeNotification(project);
            }
        }
        else if (browseMode == BrowseMode.Declaration) {
            PsiReference ref = TargetElementUtil.findReference(editor, offset);
            List<PsiElement> resolvedElements = ref == null ? Collections.emptyList() : resolve(ref);
            PsiElement resolvedElement = resolvedElements.size() == 1 ? resolvedElements.get(0) : null;

            PsiElement[] targetElements = GotoDeclarationAction.findTargetElementsNoVS(project, editor, offset, false);
            PsiElement elementAtPointer = file.findElementAt(TargetElementUtil.adjustOffset(file, editor.getDocument(), offset));

            if (targetElements != null) {
                if (targetElements.length == 0) {
                    return null;
                }
                else if (targetElements.length == 1) {
                    if (targetElements[0] != resolvedElement && elementAtPointer != null && targetElements[0].isPhysical()) {
                        return ref != null ? new InfoSingle(ref, targetElements[0]) : new InfoSingle(elementAtPointer, targetElements[0]);
                    }
                }
                else {
                    return elementAtPointer != null ? new InfoMultiple(elementAtPointer) : null;
                }
            }

            if (resolvedElements.size() == 1) {
                return new InfoSingle(ref, resolvedElements.get(0));
            }
            if (resolvedElements.size() > 1) {
                return elementAtPointer != null ? new InfoMultiple(elementAtPointer, ref) : null;
            }
        }
        else if (browseMode == BrowseMode.Implementation) {
            PsiElement element = TargetElementUtil.findTargetElement(editor, ImplementationSearcher.getFlags(), offset);
            PsiElement[] targetElements = new ImplementationSearcher() {
                @Override

                protected PsiElement[] searchDefinitions(PsiElement element, Editor editor) {
                    List<PsiElement> found = new ArrayList<>(2);
                    DefinitionsScopedSearch.search(element, getSearchScope(element, editor)).forEach(psiElement -> {
                        found.add(psiElement);
                        return found.size() != 2;
                    });
                    return PsiUtilCore.toPsiElementArray(found);
                }
            }.searchImplementations(editor, element, offset);
            if (targetElements == null) {
                return null;
            }
            if (targetElements.length > 1) {
                PsiElement elementAtPointer = file.findElementAt(offset);
                if (elementAtPointer != null) {
                    return new InfoMultiple(elementAtPointer);
                }
                return null;
            }
            if (targetElements.length == 1) {
                Navigatable descriptor = EditSourceUtil.getDescriptor(targetElements[0]);
                if (descriptor == null || !descriptor.canNavigate()) {
                    return null;
                }
                targetElement = targetElements[0];
            }
        }

        if (targetElement != null && targetElement.isPhysical()) {
            PsiElement elementAtPointer = file.findElementAt(offset);
            if (elementAtPointer != null) {
                return new InfoSingle(elementAtPointer, targetElement);
            }
        }

        final PsiElement element = GotoDeclarationAction.findElementToShowUsagesOf(editor, offset);
        if (element != null) {
            PsiElement identifier = ((PsiNameIdentifierOwner) element).getNameIdentifier();
            if (identifier != null && identifier.isValid()) {
                DocInfo baseDocInfo = generateInfo(element, element, false);

                if (baseDocInfo != DocInfo.EMPTY && !StringUtil.isEmptyOrSpaces(baseDocInfo.text)) {
                    return new Info(identifier) {

                        @Override
                        public DocInfo getInfo() {
                            StringBuilder builder = new StringBuilder("<small>Show usages of </small><br>");
                            builder.append(baseDocInfo.text);
                            return new DocInfo(builder.toString(), null);
                        }

                        @Override
                        public boolean isValid(Document document) {
                            return true;
                        }

                        @Override
                        public boolean isNavigatable() {
                            return true;
                        }
                    };
                }
                else {
                    return new Info(identifier) {

                        @Override
                        public DocInfo getInfo() {
                            String name = UsageViewUtil.getType(element) + " '" + UsageViewUtil.getShortName(element) + "'";
                            return new DocInfo("Show usages of " + name, null);
                        }

                        @Override
                        public boolean isValid(Document document) {
                            return element.isValid();
                        }

                        @Override
                        public boolean isNavigatable() {
                            return true;
                        }
                    };
                }
            }
        }
        return null;
    }

    @RequiredReadAction
    private static List<PsiElement> resolve(PsiReference ref) {
        // IDEA-56727 try resolve first as in GotoDeclarationAction
        PsiElement resolvedElement = ref.resolve();

        if (resolvedElement == null && ref instanceof PsiPolyVariantReference) {
            List<PsiElement> result = new ArrayList<>();
            ResolveResult[] psiElements = ((PsiPolyVariantReference) ref).multiResolve(false);
            for (ResolveResult resolveResult : psiElements) {
                if (resolveResult.getElement() != null) {
                    result.add(resolveResult.getElement());
                }
            }
            return result;
        }
        return resolvedElement == null ? Collections.emptyList() : Collections.singletonList(resolvedElement);
    }

    public void disposeHighlighter() {
        HighlightersSet highlighter = myHighlighter;
        if (highlighter != null) {
            myHighlighter = null;
            highlighter.uninstall();
            myDocTooltipService.hideAllHints();
        }
    }

    private final class TooltipProvider {
        private final EditorEx myHostEditor;
        private final int myHostOffset;
        private BrowseMode myBrowseMode;
        private boolean myDisposed;
        private CancellablePromise<?> myExecutionProgress;

        TooltipProvider(EditorEx hostEditor, int hostOffset) {
            myHostEditor = hostEditor;
            myHostOffset = hostOffset;
        }

        @SuppressWarnings("CopyConstructorMissesField")
        TooltipProvider(TooltipProvider source) {
            myHostEditor = source.myHostEditor;
            myHostOffset = source.myHostOffset;
        }

        void dispose() {
            myDisposed = true;
            if (myExecutionProgress != null) {
                myExecutionProgress.cancel();
            }
        }

        BrowseMode getBrowseMode() {
            return myBrowseMode;
        }

        void execute(BrowseMode browseMode) {
            myBrowseMode = browseMode;

            if (PsiDocumentManager.getInstance(myProject).getPsiFile(myHostEditor.getDocument()) == null) {
                return;
            }

            int selStart = myHostEditor.getSelectionModel().getSelectionStart();
            int selEnd = myHostEditor.getSelectionModel().getSelectionEnd();

            if (myHostOffset >= selStart && myHostOffset < selEnd) {
                disposeHighlighter();
                return;
            }

            myExecutionProgress = ReadAction.nonBlocking(this::doExecute)
                .withDocumentsCommitted(myProject)
                .expireWhen(() -> isTaskOutdated(myHostEditor))
                .finishOnUiThread(Application::getDefaultModalityState, Runnable::run)
                .submit(AppExecutorUtil.getAppExecutorService());
        }

        private Runnable createDisposalContinuation() {
            return CtrlMouseHandler.this::disposeHighlighter;
        }

        private Runnable doExecute() {
            EditorEx editor = getPossiblyInjectedEditor();
            int offset = getOffset(editor);

            PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
            if (file == null) {
                return createDisposalContinuation();
            }

            Info info;
            DocInfo docInfo;
            try {
                info = getInfoAt(editor, file, offset, myBrowseMode);
                if (info == null) {
                    return createDisposalContinuation();
                }
                docInfo = info.getInfo();
            }
            catch (IndexNotReadyException e) {
                showDumbModeNotification(myProject);
                return createDisposalContinuation();
            }

            LOG.debug("Obtained info about element under cursor");
            return () -> addHighlighterAndShowHint(info, docInfo, editor);
        }

        private EditorEx getPossiblyInjectedEditor() {
            Document document = myHostEditor.getDocument();
            if (PsiDocumentManager.getInstance(myProject).isCommitted(document)) {
                PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
                return (EditorEx) InjectedEditorManager.getInstance(myProject).getEditorForInjectedLanguageNoCommit(myHostEditor, psiFile, myHostOffset);
            }
            return myHostEditor;
        }

        private boolean isTaskOutdated(Editor editor) {
            return myDisposed || myProject.isDisposed() || editor.isDisposed() || !ApplicationManager.getApplication().isUnitTestMode() && !editor.getComponent().isShowing();
        }

        private int getOffset(Editor editor) {
            return editor instanceof EditorWindow ? ((EditorWindow) editor).getDocument().hostToInjected(myHostOffset) : myHostOffset;
        }

        private void addHighlighterAndShowHint(Info info, DocInfo docInfo, EditorEx editor) {
            if (myDisposed || editor.isDisposed()) {
                return;
            }
            if (myHighlighter != null) {
                if (!info.isSimilarTo(myHighlighter.getStoredInfo())) {
                    disposeHighlighter();
                }
                else {
                    // highlighter already set
                    if (info.isNavigatable()) {
                        editor.setCustomCursor(CtrlMouseHandler.class, StandardCursors.HAND);
                    }
                    return;
                }
            }

            if (!info.isValid(editor.getDocument()) || !info.isNavigatable() && docInfo.text == null) {
                return;
            }

            boolean highlighterOnly = EditorSettingsExternalizable.getInstance().isShowQuickDocOnMouseOverElement() && DocumentationManager.getInstance(myProject).getDocInfoHint() != null;

            myHighlighter = installHighlighterSet(info, editor, highlighterOnly);

            if (highlighterOnly || docInfo.text == null) {
                return;
            }

            DocumentationProvider docProvider = docInfo.docProvider;
            Consumer<String> linkActivated = docProvider == null ? null : description -> activateQuickDocLink(docProvider, info.myElementAtPointer, description);

            EditorDocTooltip tooltip = myDocTooltipService.show(editor, getOffset(editor), docInfo.text, linkActivated);
            if (tooltip == null) {
                return;
            }

            myTooltip = tooltip;
            tooltip.addHideListener(() -> myTooltip = null);

            updateOnPsiChanges(tooltip, info, docInfo.text, editor);
        }


        private void updateOnPsiChanges(EditorDocTooltip tooltip, Info info, String oldText, Editor editor) {
            if (!tooltip.isVisible()) {
                return;
            }
            Disposable hintDisposable = Disposable.newDisposable("CtrlMouseHandler.TooltipProvider.updateOnPsiChanges");
            tooltip.addHideListener(() -> Disposer.dispose(hintDisposable));
            myProject.getMessageBus().connect(hintDisposable).subscribe(PsiModificationTrackerListener.class, () -> ReadAction.nonBlocking(() -> {
                    try {
                        DocInfo newDocInfo = info.getInfo();
                        return (Runnable) () -> {
                            if (newDocInfo.text != null && !oldText.equals(newDocInfo.text)) {
                                tooltip.updateText(newDocInfo.text);
                            }
                        };
                    }
                    catch (IndexNotReadyException e) {
                        showDumbModeNotification(myProject);
                        return createDisposalContinuation();
                    }
                }).finishOnUiThread(Application::getDefaultModalityState, Runnable::run).withDocumentsCommitted(myProject).expireWith(hintDisposable).expireWhen(() -> !info.isValid(editor.getDocument()))
                .coalesceBy(tooltip).submit(AppExecutorUtil.getAppExecutorService()));
        }

    }

    private HighlightersSet installHighlighterSet(Info info, EditorEx editor, boolean highlighterOnly) {
        consulo.ui.Component contentComponent = editor.getContentUIComponent();
        Disposable keyPressedDisposable = contentComponent.addKeyPressedListener(this::handleKeyEvent);
        Disposable keyReleasedDisposable = contentComponent.addKeyReleasedListener(this::handleKeyEvent);
        editor.getScrollingModel().addVisibleAreaListener(myVisibleAreaListener);
        if (info.isNavigatable()) {
            editor.setCustomCursor(CtrlMouseHandler.class, StandardCursors.HAND);
        }

        List<RangeHighlighter> highlighters = new ArrayList<>();

        if (!highlighterOnly || info.isNavigatable()) {
            TextAttributes attributes = info.isNavigatable()
                ? EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.REFERENCE_HYPERLINK_COLOR)
                : new TextAttributes(null, HintUtil.getInformationColor(), null, null, Font.PLAIN);
            for (TextRange range : info.getRanges()) {
                TextAttributes attr = TextAttributesPatcher.patchAttributesColor(attributes, range, editor);
                RangeHighlighter highlighter =
                    editor.getMarkupModel().addRangeHighlighter(range.getStartOffset(), range.getEndOffset(), HighlighterLayer.HYPERLINK, attr, HighlighterTargetArea.EXACT_RANGE);
                highlighters.add(highlighter);
            }
        }

        return new HighlightersSet(highlighters, editor, info, keyPressedDisposable, keyReleasedDisposable);
    }

    @TestOnly
    public boolean isCalculationInProgress() {
        TooltipProvider provider = myTooltipProvider;
        if (provider == null) {
            return false;
        }
        Future<?> progress = provider.myExecutionProgress;
        if (progress == null) {
            return false;
        }
        return !progress.isDone();
    }

    private final class HighlightersSet {
        private final List<? extends RangeHighlighter> myHighlighters;

        private final EditorEx myHighlighterView;

        private final Info myStoredInfo;
        private final Disposable myKeyPressedDisposable;
        private final Disposable myKeyReleasedDisposable;

        private HighlightersSet(
            List<? extends RangeHighlighter> highlighters,
            EditorEx highlighterView,
            Info storedInfo,
            Disposable keyPressedDisposable,
            Disposable keyReleasedDisposable
        ) {
            myHighlighters = highlighters;
            myHighlighterView = highlighterView;
            myStoredInfo = storedInfo;
            myKeyPressedDisposable = keyPressedDisposable;
            myKeyReleasedDisposable = keyReleasedDisposable;
        }

        public void uninstall() {
            for (RangeHighlighter highlighter : myHighlighters) {
                highlighter.dispose();
            }

            myHighlighterView.setCustomCursor(CtrlMouseHandler.class, null);
            myKeyPressedDisposable.dispose();
            myKeyReleasedDisposable.dispose();
            myHighlighterView.getScrollingModel().removeVisibleAreaListener(myVisibleAreaListener);
        }


        Info getStoredInfo() {
            return myStoredInfo;
        }
    }

    public static final class DocInfo {
        public static final DocInfo EMPTY = new DocInfo(null, null);

        public final @Nullable String text;
        final @Nullable DocumentationProvider docProvider;

        DocInfo(@Nullable String text, @Nullable DocumentationProvider provider) {
            this.text = text;
            docProvider = provider;
        }
    }

    private void activateQuickDocLink(DocumentationProvider provider, PsiElement context, String description) {
        if (StringUtil.isEmpty(description) || !description.startsWith(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL)) {
            return;
        }

        String elementName = description.substring(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL.length());

        DumbService.getInstance(myProject).withAlternativeResolveEnabled(() -> {
            PsiElement targetElement = provider.getDocumentationElementForLink(PsiManager.getInstance(myProject), elementName, context);
            if (targetElement != null) {
                EditorDocTooltip tooltip = myTooltip;
                if (tooltip != null) {
                    tooltip.hide();
                }
                DocumentationManager.getInstance(myProject).showJavaDocInfo(targetElement, context, null);
            }
        });
    }
}
