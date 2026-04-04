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
import consulo.codeEditor.LogicalPosition;
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
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.documentation.DocumentationManager;
import consulo.language.editor.documentation.DocumentationManagerProtocol;
import consulo.language.editor.documentation.DocumentationProvider;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.inject.InjectedEditorManager;
import consulo.language.editor.internal.DocumentationManagerHelper;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.language.editor.ui.internal.HintManagerEx;
import consulo.language.psi.*;
import consulo.language.psi.search.DefinitionsScopedSearch;
import consulo.language.psi.util.EditSourceUtil;
import consulo.logging.Logger;
import consulo.navigation.ItemPresentation;
import consulo.navigation.Navigatable;
import consulo.navigation.NavigationItem;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.usage.UsageViewShortNameLocation;
import consulo.usage.UsageViewTypeLocation;
import consulo.usage.UsageViewUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.intellij.lang.annotations.JdkConstants;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;

@Singleton
@ServiceAPI(value = ComponentScope.PROJECT)
@ServiceImpl
public final class CtrlMouseHandler {
    private static final Logger LOG = Logger.getInstance(CtrlMouseHandler.class);

    private final Project myProject;

    private HighlightersSet myHighlighter;
    @JdkConstants.InputEventMask
    private int myStoredModifiers;
    private TooltipProvider myTooltipProvider;
    private @Nullable Point myPrevMouseLocation;
    private LightweightHintImpl myHint;

    public enum BrowseMode {
        None,
        Declaration,
        TypeDeclaration,
        Implementation
    }

    private final KeyListener myEditorKeyListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            handleKey(e);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            handleKey(e);
        }

        private void handleKey(KeyEvent e) {
            int modifiers = e.getModifiers();
            if (modifiers == myStoredModifiers) {
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
    };

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
            MouseEvent mouseEvent = e.getMouseEvent();

            Point prevLocation = myPrevMouseLocation;
            myPrevMouseLocation = mouseEvent.getLocationOnScreen();
            if (isMouseOverTooltip(mouseEvent.getLocationOnScreen()) || ScreenUtil.isMovementTowards(prevLocation, mouseEvent.getLocationOnScreen(), getHintBounds())) {
                return;
            }
            cancelPreviousTooltip();

            myStoredModifiers = mouseEvent.getModifiers();
            BrowseMode browseMode = getBrowseMode(myStoredModifiers);

            if (browseMode == BrowseMode.None || e.getArea() != EditorMouseEventArea.EDITING_AREA) {
                disposeHighlighter();
                return;
            }

            Editor editor = e.getEditor();
            if (!(editor instanceof EditorEx) || editor.getProject() != null && editor.getProject() != myProject) {
                return;
            }
            Point point = new Point(mouseEvent.getPoint());
            if (!EditorUtil.isPointOverText(editor, point)) {
                disposeHighlighter();
                return;
            }
            myTooltipProvider = new TooltipProvider((EditorEx) editor, editor.xyToLogicalPosition(point));
            myTooltipProvider.execute(browseMode);
        }
    };

    @Inject
    public CtrlMouseHandler(Project project) {
        myProject = project;
    }

    public EditorMouseListener getEditorMouseAdapter() {
        return myEditorMouseAdapter;
    }

    public EditorMouseMotionListener getEditorMouseMotionListener() {
        return myEditorMouseMotionListener;
    }

    public void caretPositionChanged() {
        if (myHint != null) {
            DocumentationManager.getInstance(myProject).updateToolwindowContext();
        }
    }

    public void cancelPreviousTooltip() {
        if (myTooltipProvider != null) {
            myTooltipProvider.dispose();
            myTooltipProvider = null;
        }
    }

    private boolean isMouseOverTooltip(Point mouseLocationOnScreen) {
        Rectangle bounds = getHintBounds();
        return bounds != null && bounds.contains(mouseLocationOnScreen);
    }

    private @Nullable Rectangle getHintBounds() {
        LightweightHintImpl hint = myHint;
        if (hint == null) {
            return null;
        }
        JComponent hintComponent = hint.getComponent();
        if (!hintComponent.isShowing()) {
            return null;
        }
        return new Rectangle(hintComponent.getLocationOnScreen(), hintComponent.getSize());
    }

    
    private static BrowseMode getBrowseMode(@JdkConstants.InputEventMask int modifiers) {
        if (modifiers != 0) {
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

        InfoSingle(PsiReference ref, PsiElement targetElement) {
            super(ref.getElement(), ReferenceRange.getAbsoluteRanges(ref));
            myTargetElement = targetElement;
        }

        @Override
        
        public DocInfo getInfo() {
            return areElementsValid() ? generateInfo(myTargetElement, myElementAtPointer, isNavigatable()) : DocInfo.EMPTY;
        }

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

        InfoMultiple(PsiElement elementAtPointer, PsiReference ref) {
            super(elementAtPointer, ReferenceRange.getAbsoluteRanges(ref));
        }

        @Override
        
        public DocInfo getInfo() {
            return new DocInfo(CodeInsightBundle.message("multiple.implementations.tooltip"), null);
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
                if (descriptor == null || !descriptor.getNavigateOptions().canNavigate()) {
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
            HintManager.getInstance().hideAllHints();
        }
    }

    private void updateText(String updatedText, Consumer<? super String> newTextConsumer, LightweightHintImpl hint, Editor editor) {
        UIUtil.invokeLaterIfNeeded(() -> {
            // There is a possible case that quick doc control width is changed, e.g. it contained text
            // like 'public final class String implements java.io.Serializable, java.lang.Comparable<java.lang.String>' and
            // new text replaces fully-qualified class names by hyperlinks with short name.
            // That's why we might need to update the control size. We assume that the hint component is located at the
            // layered pane, so, the algorithm is to find an ancestor layered pane and apply new size for the target component.
            JComponent component = hint.getComponent();
            Dimension oldSize = component.getPreferredSize();
            newTextConsumer.accept(updatedText);

            Dimension newSize = component.getPreferredSize();
            if (newSize.width == oldSize.width) {
                return;
            }
            component.setPreferredSize(new Dimension(newSize.width, newSize.height));

            // We're assuming here that there are two possible hint representation modes: popup and layered pane.
            if (hint.isRealPopup()) {

                TooltipProvider tooltipProvider = myTooltipProvider;
                if (tooltipProvider != null) {
                    // There is a possible case that 'raw' control was rather wide but the 'rich' one is narrower. That's why we try to
                    // re-show the hint here. Benefits: there is a possible case that we'll be able to show nice layered pane-based balloon;
                    // the popup will be re-positioned according to the new width.
                    hint.hide();
                    tooltipProvider.showHint(new LightweightHintImpl(component), editor);
                }
                else {
                    component.setPreferredSize(new Dimension(newSize.width, oldSize.height));
                    hint.pack();
                }
                return;
            }

            Container topLevelLayeredPaneChild = null;
            boolean adjustBounds = false;
            for (Container current = component.getParent(); current != null; current = current.getParent()) {
                if (current instanceof JLayeredPane) {
                    adjustBounds = true;
                    break;
                }
                else {
                    topLevelLayeredPaneChild = current;
                }
            }

            if (adjustBounds && topLevelLayeredPaneChild != null) {
                Rectangle bounds = topLevelLayeredPaneChild.getBounds();
                topLevelLayeredPaneChild.setBounds(bounds.x, bounds.y, bounds.width + newSize.width - oldSize.width, bounds.height);
            }
        });
    }

    private final class TooltipProvider {
        
        private final EditorEx myHostEditor;
        private final int myHostOffset;
        private BrowseMode myBrowseMode;
        private boolean myDisposed;
        private CompletableFuture<?> myExecutionProgress;

        TooltipProvider(EditorEx hostEditor, LogicalPosition hostPos) {
            myHostEditor = hostEditor;
            myHostOffset = hostEditor.logicalPositionToOffset(hostPos);
        }

        @SuppressWarnings("CopyConstructorMissesField")
        TooltipProvider(TooltipProvider source) {
            myHostEditor = source.myHostEditor;
            myHostOffset = source.myHostOffset;
        }

        void dispose() {
            myDisposed = true;
            if (myExecutionProgress != null) {
                myExecutionProgress.cancel(false);
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
                        editor.setCustomCursor(CtrlMouseHandler.class, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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

            HyperlinkListener hyperlinkListener = docInfo.docProvider == null ? null : new QuickDocHyperlinkListener(docInfo.docProvider, info.myElementAtPointer);
            Ref<Consumer<? super String>> newTextConsumerRef = new Ref<>();
            JComponent component = HintUtil.createInformationLabel(docInfo.text, hyperlinkListener, null, newTextConsumerRef);
            component.setBorder(JBUI.Borders.empty(6, 6, 5, 6));

            LightweightHintImpl hint = new LightweightHintImpl(wrapInScrollPaneIfNeeded(component, editor));

            myHint = hint;
            hint.addHintListener(__ -> myHint = null);

            showHint(hint, editor);

            Consumer<? super String> newTextConsumer = newTextConsumerRef.get();
            if (newTextConsumer != null) {
                updateOnPsiChanges(hint, info, newTextConsumer, docInfo.text, editor);
            }
        }

        
        private JComponent wrapInScrollPaneIfNeeded(JComponent component, Editor editor) {
            if (!ApplicationManager.getApplication().isHeadlessEnvironment()) {
                Dimension preferredSize = component.getPreferredSize();
                Dimension maxSize = getMaxPopupSize(editor);
                if (preferredSize.width > maxSize.width || preferredSize.height > maxSize.height) {
                    // We expect documentation providers to exercise good judgement in limiting the displayed information,
                    // but in any case, we don't want the hint to cover the whole screen, so we also implement certain limiting here.
                    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(component, true);
                    scrollPane.setPreferredSize(new Dimension(Math.min(preferredSize.width, maxSize.width), Math.min(preferredSize.height, maxSize.height)));
                    return scrollPane;
                }
            }
            return component;
        }

        
        private Dimension getMaxPopupSize(Editor editor) {
            Rectangle rectangle = ScreenUtil.getScreenRectangle(editor.getContentComponent());
            return new Dimension((int) (0.9 * Math.max(640, rectangle.width)), (int) (0.33 * Math.max(480, rectangle.height)));
        }

        private void updateOnPsiChanges(LightweightHintImpl hint, Info info, Consumer<? super String> textConsumer, String oldText, Editor editor) {
            if (!hint.isVisible()) {
                return;
            }
            Disposable hintDisposable = Disposable.newDisposable("CtrlMouseHandler.TooltipProvider.updateOnPsiChanges");
            hint.addHintListener(__ -> Disposer.dispose(hintDisposable));
            myProject.getMessageBus().connect(hintDisposable).subscribe(PsiModificationTrackerListener.class, () -> ReadAction.nonBlocking(() -> {
                    try {
                        DocInfo newDocInfo = info.getInfo();
                        return (Runnable) () -> {
                            if (newDocInfo.text != null && !oldText.equals(newDocInfo.text)) {
                                updateText(newDocInfo.text, textConsumer, hint, editor);
                            }
                        };
                    }
                    catch (IndexNotReadyException e) {
                        showDumbModeNotification(myProject);
                        return createDisposalContinuation();
                    }
                }).finishOnUiThread(Application::getDefaultModalityState, Runnable::run).withDocumentsCommitted(myProject).expireWith(hintDisposable).expireWhen(() -> !info.isValid(editor.getDocument()))
                .coalesceBy(hint).submit(AppExecutorUtil.getAppExecutorService()));
        }

        @RequiredUIAccess
        public void showHint(LightweightHintImpl hint, Editor editor) {
            if ( editor.isDisposed()) {
                return;
            }
            HintManagerEx hintManager = (HintManagerEx) HintManager.getInstance();
            short constraint = HintManager.ABOVE;
            LogicalPosition position = editor.offsetToLogicalPosition(getOffset(editor));
            Point p = hintManager.getHintPosition(hint, editor, position, constraint);
            if (p.y - hint.getComponent().getPreferredSize().height < 0) {
                constraint = HintManager.UNDER;
                p = hintManager.getHintPosition(hint, editor, position, constraint);
            }
            hintManager.showEditorHint(hint, editor, p, HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_SCROLLING, 0, false,
                hintManager.createHintHint(editor, p, hint, constraint).setContentActive(false));
        }
    }

    
    private HighlightersSet installHighlighterSet(Info info, EditorEx editor, boolean highlighterOnly) {
        editor.getContentComponent().addKeyListener(myEditorKeyListener);
        editor.getScrollingModel().addVisibleAreaListener(myVisibleAreaListener);
        if (info.isNavigatable()) {
            editor.setCustomCursor(CtrlMouseHandler.class, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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

        return new HighlightersSet(highlighters, editor, info);
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

        private HighlightersSet(List<? extends RangeHighlighter> highlighters, EditorEx highlighterView, Info storedInfo) {
            myHighlighters = highlighters;
            myHighlighterView = highlighterView;
            myStoredInfo = storedInfo;
        }

        public void uninstall() {
            for (RangeHighlighter highlighter : myHighlighters) {
                highlighter.dispose();
            }

            myHighlighterView.setCustomCursor(CtrlMouseHandler.class, null);
            myHighlighterView.getContentComponent().removeKeyListener(myEditorKeyListener);
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

    private final class QuickDocHyperlinkListener implements HyperlinkListener {
        
        private final DocumentationProvider myProvider;
        
        private final PsiElement myContext;

        QuickDocHyperlinkListener(DocumentationProvider provider, PsiElement context) {
            myProvider = provider;
            myContext = context;
        }

        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
                return;
            }

            String description = e.getDescription();
            if (StringUtil.isEmpty(description) || !description.startsWith(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL)) {
                return;
            }

            String elementName = e.getDescription().substring(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL.length());

            DumbService.getInstance(myProject).withAlternativeResolveEnabled(() -> {
                PsiElement targetElement = myProvider.getDocumentationElementForLink(PsiManager.getInstance(myProject), elementName, myContext);
                if (targetElement != null) {
                    LightweightHintImpl hint = myHint;
                    if (hint != null) {
                        hint.hide(true);
                    }
                    DocumentationManager.getInstance(myProject).showJavaDocInfo(targetElement, myContext, null);
                }
            });
        }
    }
}
