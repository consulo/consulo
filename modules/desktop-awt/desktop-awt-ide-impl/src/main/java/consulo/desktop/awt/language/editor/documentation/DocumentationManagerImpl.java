// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.language.editor.documentation;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.ui.DimensionService;
import consulo.application.util.DateFormatUtil;
import consulo.codeEditor.Editor;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.content.scope.PackageSet;
import consulo.content.scope.PackageSetBase;
import consulo.dataContext.DataContext;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.codeInsight.documentation.DockablePopupManager;
import consulo.ide.impl.idea.codeInsight.documentation.QuickDocUtil;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.codeInsight.hint.ParameterInfoController;
import consulo.ide.impl.idea.ide.BrowserUtil;
import consulo.ide.impl.idea.ide.actions.BaseNavigateToSourceAction;
import consulo.ide.impl.idea.ide.actions.WindowAction;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.ide.impl.idea.ide.util.gotoByName.ChooseByNameBase;
import consulo.ide.impl.idea.ide.util.gotoByName.QuickSearchComponent;
import consulo.ide.impl.idea.lang.documentation.DocumentationMarkup;
import consulo.ide.impl.idea.openapi.roots.libraries.LibraryUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.impl.idea.reference.SoftReference;
import consulo.ide.impl.idea.ui.popup.AbstractPopup;
import consulo.ide.impl.idea.ui.popup.PopupUpdateProcessor;
import consulo.ide.impl.idea.ui.tabs.FileColorManagerImpl;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.setting.module.OrderEntryTypeEditor;
import consulo.language.Language;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.FileColorManager;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.documentation.*;
import consulo.language.editor.impl.internal.completion.CompletionUtil;
import consulo.language.editor.internal.DocumentationManagerHelper;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.plain.PlainTextFileType;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.psi.util.SymbolPresentationUtil;
import consulo.logging.Logger;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntryType;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.Rectangle2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.ex.awt.ScrollingUtil;
import consulo.ui.ex.awt.SwingActionDelegate;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.internal.GuiUtils;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.ex.toolWindow.ToolWindowType;
import consulo.ui.util.ColorValueUtil;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.SmartList;
import consulo.util.concurrent.ActionCallback;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.Conditions;
import consulo.util.lang.ref.Ref;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveFileType;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Singleton
@ServiceImpl
public final class DocumentationManagerImpl extends DockablePopupManager<DocumentationComponent> implements DocumentationManager {
    private static final Logger LOG = Logger.getInstance(DocumentationManagerImpl.class);
    private static final String SHOW_DOCUMENTATION_IN_TOOL_WINDOW = "ShowDocumentationInToolWindow";
    private static final String DOCUMENTATION_AUTO_UPDATE_ENABLED = "DocumentationAutoUpdateEnabled";

    private static final long DOC_GENERATION_TIMEOUT_MILLISECONDS = 60000;
    private static final long DOC_GENERATION_PAUSE_MILLISECONDS = 100;

    private static final Class[] ACTION_CLASSES_TO_IGNORE = {
        HintManagerImpl.ActionToIgnore.class,
        ScrollingUtil.ScrollingAction.class,
        SwingActionDelegate.class,
        BaseNavigateToSourceAction.class,
        WindowAction.class
    };
    private static final String[] ACTION_IDS_TO_IGNORE = {
        IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN,
        IdeActions.ACTION_EDITOR_MOVE_CARET_UP,
        IdeActions.ACTION_EDITOR_MOVE_CARET_PAGE_DOWN,
        IdeActions.ACTION_EDITOR_MOVE_CARET_PAGE_UP,
        IdeActions.ACTION_EDITOR_ESCAPE
    };
    private static final String[] ACTION_PLACES_TO_IGNORE = {ActionPlaces.JAVADOC_INPLACE_SETTINGS, ActionPlaces.JAVADOC_TOOLBAR};

    private Editor myEditor;
    private final Alarm myUpdateDocAlarm;
    private WeakReference<JBPopup> myDocInfoHintRef;
    private WeakReference<Component> myFocusedBeforePopup;

    private boolean myCloseOnSneeze;
    private String myPrecalculatedDocumentation;

    private ActionCallback myLastAction;
    private DocumentationComponent myTestDocumentationComponent;

    private AnAction myRestorePopupAction;

    @Override
    protected String getToolwindowId() {
        return ToolWindowId.DOCUMENTATION;
    }

    @Override
    protected DocumentationComponent createComponent() {
        return new DocumentationComponent(this);
    }

    @Override
    protected String getRestorePopupDescription() {
        return "Restore popup view mode";
    }

    @Override
    protected String getAutoUpdateDescription() {
        return "Refresh documentation on selection change automatically";
    }

    @Override
    protected String getAutoUpdateTitle() {
        return "Auto-update from Source";
    }

    @Override
    protected boolean getAutoUpdateDefault() {
        return true;
    }

    @Nonnull
    @Override
    protected AnAction createRestorePopupAction() {
        myRestorePopupAction = super.createRestorePopupAction();
        return myRestorePopupAction;
    }

    @Override
    public void restorePopupBehavior() {
        super.restorePopupBehavior();
        Component previouslyFocused = SoftReference.dereference(myFocusedBeforePopup);
        if (previouslyFocused != null && previouslyFocused.isShowing()) {
            UIUtil.runWhenFocused(previouslyFocused, () -> updateComponent(true));
            ProjectIdeFocusManager.getInstance(myProject).requestFocus(previouslyFocused, true);
        }
    }

    @Override
    public void createToolWindow(PsiElement element, PsiElement originalElement) {
        super.createToolWindow(element, originalElement);

        if (myToolWindow != null) {
            myToolWindow.getComponent().putClientProperty(ChooseByNameBase.TEMPORARILY_FOCUSABLE_COMPONENT_KEY, Boolean.TRUE);

            if (myRestorePopupAction != null) {
                ShortcutSet quickDocShortcut = ActionManager.getInstance().getAction(IdeActions.ACTION_QUICK_JAVADOC).getShortcutSet();
                myRestorePopupAction.registerCustomShortcutSet(quickDocShortcut, myToolWindow.getComponent());
                myRestorePopupAction = null;
            }
        }
    }

    /**
     * @return {@code true} if quick doc control is configured to not prevent user-IDE interaction (e.g. should be closed if
     * the user presses a key);
     * {@code false} otherwise
     */
    @Override
    public boolean isCloseOnSneeze() {
        return myCloseOnSneeze;
    }

    @Override
    protected void installComponentActions(ToolWindow toolWindow, DocumentationComponent component) {
        toolWindow.setTitleActions(component.getActions());
        DefaultActionGroup group = new DefaultActionGroup(createActions());
        group.add(component.getFontSizeAction());
        toolWindow.setAdditionalGearActions(group);
        component.removeCornerMenu();
    }

    @Override
    @RequiredUIAccess
    protected void setToolwindowDefaultState() {
        Rectangle2D rectangle = WindowManager.getInstance().getIdeFrame(myProject).suggestChildFrameBounds();
        myToolWindow.setDefaultState(
            ToolWindowAnchor.RIGHT,
            ToolWindowType.DOCKED,
            new Rectangle2D(rectangle.getWidth() / 4, rectangle.getHeight())
        );
        myToolWindow.setType(ToolWindowType.DOCKED, null);
        myToolWindow.setSplitMode(true, null);
        myToolWindow.setAutoHide(false);
    }

    @Inject
    public DocumentationManagerImpl(@Nonnull Project project) {
        super(project);
        AnActionListener actionListener = new AnActionListener() {
            @Override
            public void beforeActionPerformed(@Nonnull AnAction action, @Nonnull DataContext dataContext, @Nonnull AnActionEvent event) {
                if (getDocInfoHint() != null &&
                    LookupManager.getActiveLookup(myEditor) == null &&
                    // let the lookup manage all the actions
                    !Conditions.instanceOf(ACTION_CLASSES_TO_IGNORE).value(action) &&
                    !ArrayUtil.contains(event.getPlace(), ACTION_PLACES_TO_IGNORE) &&
                    !ContainerUtil.exists(ACTION_IDS_TO_IGNORE, id -> ActionManager.getInstance().getAction(id) == action)) {
                    closeDocHint();
                }
            }

            @Override
            public void beforeEditorTyping(char c, @Nonnull DataContext dataContext) {
                JBPopup hint = getDocInfoHint();
                if (hint != null && LookupManager.getActiveLookup(myEditor) == null) {
                    hint.cancel();
                }
            }
        };
        myProject.getApplication().getMessageBus().connect(project).subscribe(AnActionListener.class, actionListener);
        myUpdateDocAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, myProject);
    }

    private void closeDocHint() {
        JBPopup hint = getDocInfoHint();
        if (hint == null) {
            return;
        }
        myCloseOnSneeze = false;
        hint.cancel();
        Component toFocus = SoftReference.dereference(myFocusedBeforePopup);
        hint.cancel();
        if (toFocus != null) {
            ProjectIdeFocusManager.getInstance(myProject).requestFocus(toFocus, true);
        }
    }

    @Override
    @RequiredUIAccess
    public void setAllowContentUpdateFromContext(boolean allow) {
        if (hasActiveDockedDocWindow()) {
            restartAutoUpdate(allow);
        }
    }

    @Override
    @RequiredUIAccess
    public void updateToolwindowContext() {
        if (hasActiveDockedDocWindow()) {
            updateComponent();
        }
    }

    @Override
    @RequiredUIAccess
    public void showJavaDocInfoAtToolWindow(@Nonnull PsiElement element, @Nonnull PsiElement original) {
        Content content = recreateToolWindow(element, original);
        if (content == null) {
            return;
        }
        DocumentationComponent component = (DocumentationComponent)content.getComponent();
        myUpdateDocAlarm.cancelAllRequests();
        doFetchDocInfo(component, new MyCollector(myProject, element, original, null, false)).doWhenDone(component::clearHistory);
    }

    @Override
    @RequiredUIAccess
    public void showJavaDocInfo(@Nonnull PsiElement element, PsiElement original) {
        showJavaDocInfo(element, original, null);
    }

    /**
     * Asks to show quick doc for the target element.
     *
     * @param editor             editor with an element for which quick do should be shown
     * @param element            target element which documentation should be shown
     * @param original           element that was used as a quick doc anchor. Example: consider a code like {@code Runnable task;}.
     *                           A user wants to see javadoc for the {@code Runnable}, so, original element is a class name from the variable
     *                           declaration but {@code 'element'} argument is a {@code Runnable} descriptor
     * @param closeCallback      callback to be notified on target hint close (if any)
     * @param documentation      precalculated documentation
     * @param closeOnSneeze      flag that defines whether quick doc control should be as non-obtrusive as possible. E.g. there are at least
     *                           two possible situations - the quick doc is shown automatically on mouse over element; the quick doc is shown
     *                           on explicit action call (Ctrl+Q). We want to close the doc on, say, editor viewport position change
     *                           at the first situation but don't want to do that at the second
     * @param useStoredPopupSize whether popup size previously set by user (via mouse-dragging) should be used, or default one should be used
     */
    @Override
    @RequiredUIAccess
    public void showJavaDocInfo(
        @Nonnull Editor editor,
        @Nonnull PsiElement element,
        @Nonnull PsiElement original,
        @Nullable Runnable closeCallback,
        @Nullable String documentation,
        boolean closeOnSneeze,
        boolean useStoredPopupSize
    ) {
        myEditor = editor;
        myCloseOnSneeze = closeOnSneeze;
        showJavaDocInfo(element, original, false, closeCallback, documentation, useStoredPopupSize);
    }

    @Override
    @RequiredUIAccess
    public void showJavaDocInfo(@Nonnull PsiElement element, PsiElement original, @Nullable Runnable closeCallback) {
        showJavaDocInfo(element, original, false, closeCallback);
    }

    @Override
    @RequiredUIAccess
    public void showJavaDocInfo(@Nonnull PsiElement element, PsiElement original, boolean requestFocus, @Nullable Runnable closeCallback) {
        showJavaDocInfo(element, original, requestFocus, closeCallback, null, true);
    }

    @Override
    @RequiredUIAccess
    public void showJavaDocInfo(
        @Nonnull PsiElement element,
        PsiElement original,
        boolean requestFocus,
        @Nullable Runnable closeCallback,
        @Nullable String documentation,
        boolean useStoredPopupSize
    ) {
        if (!element.isValid()) {
            return;
        }

        PopupUpdateProcessor updateProcessor = new PopupUpdateProcessor(element.getProject()) {
            @Override
            @RequiredUIAccess
            public void updatePopup(Object lookupItemObject) {
                if (lookupItemObject instanceof PsiElement psiElement) {
                    doShowJavaDocInfo(psiElement, requestFocus, this, original, null, null, useStoredPopupSize);
                }
            }
        };

        doShowJavaDocInfo(element, requestFocus, updateProcessor, original, closeCallback, documentation, useStoredPopupSize);
    }

    @Override
    @RequiredUIAccess
    public void showJavaDocInfo(Editor editor, @Nullable PsiFile file, boolean requestFocus) {
        showJavaDocInfo(editor, file, requestFocus, null);
    }

    @Override
    @RequiredUIAccess
    public void showJavaDocInfo(Editor editor, @Nullable PsiFile file, boolean requestFocus, @Nullable Runnable closeCallback) {
        myEditor = editor;
        Project project = getProject(file);
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        PsiElement list = ParameterInfoController.findArgumentList(file, editor.getCaretModel().getOffset(), -1);
        PsiElement expressionList = null;
        if (list != null) {
            LookupEx lookup = LookupManager.getInstance(myProject).getActiveLookup();
            if (lookup != null) {
                expressionList = null; // take completion variants for documentation then
            }
            else {
                expressionList = list;
            }
        }

        PsiElement originalElement = getContextElement(editor, file);
        PsiElement element = assertSameProject(findTargetElement(editor, file));

        if (element == null && expressionList != null) {
            element = expressionList;
        }

        if (element == null && file == null) {
            return; //file == null for text field editor
        }

        if (element == null) { // look if we are within a javadoc comment
            element = assertSameProject(originalElement);
            if (element == null) {
                return;
            }

            PsiComment comment = PsiTreeUtil.getParentOfType(element, PsiComment.class);
            if (comment == null) {
                return;
            }

            element = comment instanceof PsiDocCommentBase psiDocCommentBase ? psiDocCommentBase.getOwner() : comment.getParent();
            if (element == null) {
                return;
            }
            //if (!(element instanceof PsiDocCommentOwner)) return null;
        }

        PsiElement finalElement = element;
        PopupUpdateProcessor updateProcessor = new PopupUpdateProcessor(project) {
            @Override
            @RequiredUIAccess
            public void updatePopup(Object lookupIteObject) {
                if (lookupIteObject == null) {
                    doShowJavaDocInfo(
                        finalElement,
                        false,
                        this,
                        originalElement,
                        closeCallback,
                        CodeInsightLocalize.noDocumentationFound().get(),
                        true
                    );
                    return;
                }
                if (lookupIteObject instanceof PsiElement psiElement) {
                    doShowJavaDocInfo(psiElement, false, this, originalElement, closeCallback, null, true);
                    return;
                }

                DocumentationProvider documentationProvider = DocumentationManagerHelper.getProviderFromElement(file);

                PsiElement element = documentationProvider.getDocumentationElementForLookupItem(
                    PsiManager.getInstance(myProject),
                    lookupIteObject,
                    originalElement
                );

                if (element == null) {
                    doShowJavaDocInfo(
                        finalElement,
                        false,
                        this,
                        originalElement,
                        closeCallback,
                        CodeInsightLocalize.noDocumentationFound().get(),
                        true
                    );
                    return;
                }

                if (myEditor != null) {
                    PsiFile file = element.getContainingFile();
                    if (file != null) {
                        Editor editor = myEditor;
                        showJavaDocInfo(myEditor, file, false);
                        myEditor = editor;
                    }
                }
                else {
                    doShowJavaDocInfo(element, false, this, originalElement, closeCallback, null, true);
                }
            }
        };

        doShowJavaDocInfo(element, requestFocus, updateProcessor, originalElement, closeCallback, null, true);
    }

    @RequiredUIAccess
    public PsiElement findTargetElement(Editor editor, PsiFile file) {
        return findTargetElement(editor, file, getContextElement(editor, file));
    }

    @RequiredUIAccess
    private static PsiElement getContextElement(Editor editor, PsiFile file) {
        return file != null ? file.findElementAt(editor.getCaretModel().getOffset()) : null;
    }

    @RequiredUIAccess
    private void doShowJavaDocInfo(
        @Nonnull PsiElement element,
        boolean requestFocus,
        @Nonnull PopupUpdateProcessor updateProcessor,
        PsiElement originalElement,
        @Nullable Runnable closeCallback,
        @Nullable String documentation,
        boolean useStoredPopupSize
    ) {
        Project project = getProject(element);
        if (!project.isOpen()) {
            return;
        }

        DocumentationManagerHelper.storeOriginalElement(project, originalElement, element);

        JBPopup prevHint = getDocInfoHint();
        //if (PreviewManager.SERVICE.preview(myProject, DocumentationPreviewPanelProvider.ID, Couple.of(element, originalElement), requestFocus) != null) {
        //  return;
        //}

        myPrecalculatedDocumentation = documentation;
        if (myToolWindow == null && PropertiesComponent.getInstance().isTrueValue(SHOW_DOCUMENTATION_IN_TOOL_WINDOW)) {
            createToolWindow(element, originalElement);
        }
        else if (myToolWindow != null) {
            Content content = myToolWindow.getContentManager().getSelectedContent();
            if (content != null) {
                DocumentationComponent component = (DocumentationComponent)content.getComponent();
                boolean sameElement = element.getManager().areElementsEquivalent(component.getElement(), element);
                if (sameElement) {
                    JComponent preferredFocusableComponent = content.getPreferredFocusableComponent();
                    // focus toolwindow on the second actionPerformed
                    boolean focus = requestFocus || CommandProcessor.getInstance().getCurrentCommand() != null;
                    if (preferredFocusableComponent != null && focus) {
                        ProjectIdeFocusManager.getInstance(myProject).requestFocus(preferredFocusableComponent, true);
                    }
                }
                if (!sameElement || !component.isUpToDate()) {
                    cancelAndFetchDocInfo(component, new MyCollector(myProject, element, originalElement, null, false))
                        .doWhenDone(component::clearHistory);
                }
            }

            if (!myToolWindow.isVisible()) {
                myToolWindow.show(null);
            }
        }
        else if (prevHint != null && prevHint.isVisible() && prevHint instanceof AbstractPopup popup) {
            DocumentationComponent component = (DocumentationComponent)popup.getComponent();
            ActionCallback result = cancelAndFetchDocInfo(component, new MyCollector(myProject, element, originalElement, null, false));
            if (requestFocus) {
                result.doWhenDone(() -> {
                    JBPopup hint = getDocInfoHint();
                    if (hint != null) {
                        ((AbstractPopup)hint).focusPreferredComponent();
                    }
                });
            }
        }
        else {
            showInPopup(element, requestFocus, updateProcessor, originalElement, closeCallback, useStoredPopupSize);
        }
    }

    @RequiredUIAccess
    private void showInPopup(
        @Nonnull PsiElement element,
        boolean requestFocus,
        PopupUpdateProcessor updateProcessor,
        PsiElement originalElement,
        @Nullable Runnable closeCallback,
        boolean useStoredPopupSize
    ) {
        Component focusedComponent = WindowManagerEx.getInstanceEx().getFocusedComponent(myProject);
        myFocusedBeforePopup = new WeakReference<>(focusedComponent);

        DocumentationComponent component = myTestDocumentationComponent == null
            ? new DocumentationComponent(this, useStoredPopupSize)
            : myTestDocumentationComponent;
        ActionListener actionListener = __ -> {
            createToolWindow(element, originalElement);
            JBPopup hint = getDocInfoHint();
            if (hint != null && hint.isVisible()) {
                hint.cancel();
            }
        };
        List<Pair<ActionListener, KeyStroke>> actions = new SmartList<>();
        AnAction quickDocAction = ActionManager.getInstance().getAction(IdeActions.ACTION_QUICK_JAVADOC);
        for (Shortcut shortcut : quickDocAction.getShortcutSet().getShortcuts()) {
            if (!(shortcut instanceof KeyboardShortcut)) {
                continue;
            }
            actions.add(Pair.create(actionListener, ((KeyboardShortcut)shortcut).getFirstKeyStroke()));
        }

        boolean hasLookup = LookupManager.getActiveLookup(myEditor) != null;
        AbstractPopup hint = (AbstractPopup)JBPopupFactory.getInstance()
            .createComponentPopupBuilder(component, component)
            .setProject(element.getProject())
            .addListener(updateProcessor)
            .addUserData(updateProcessor)
            .setKeyboardActions(actions)
            .setResizable(true)
            .setMovable(true)
            .setFocusable(true)
            .setRequestFocus(requestFocus)
            .setCancelOnClickOutside(!hasLookup) // otherwise selecting lookup items by mouse would close the doc
            .setModalContext(false)
            .setCancelCallback(() -> {
                if (MenuSelectionManager.defaultManager().getSelectedPath().length > 0) {
                    return false;
                }
                myCloseOnSneeze = false;

                if (closeCallback != null) {
                    closeCallback.run();
                }
                findQuickSearchComponent().ifPresent(QuickSearchComponent::unregisterHint);

                Disposer.dispose(component);
                myEditor = null;
                return Boolean.TRUE;
            })
            .setKeyEventHandler(e -> {
                if (myCloseOnSneeze) {
                    closeDocHint();
                }
                if (AbstractPopup.isCloseRequest(e) && getDocInfoHint() != null) {
                    closeDocHint();
                    return true;
                }
                return false;
            })
            .createPopup();

        component.setHint(hint);
        component.setToolwindowCallback(() -> {
            createToolWindow(element, originalElement);
            myToolWindow.setAutoHide(false);
            hint.cancel();
        });

        if (useStoredPopupSize
            && DimensionService.getInstance().getSize(DocumentationManagerHelper.NEW_JAVADOC_LOCATION_AND_SIZE, myProject) != null) {
            hint.setDimensionServiceKey(DocumentationManagerHelper.NEW_JAVADOC_LOCATION_AND_SIZE);
        }

        if (myEditor == null) {
            // subsequent invocation of javadoc popup from completion will have myEditor == null because of cancel invoked,
            // so reevaluate the editor for proper popup placement
            Lookup lookup = LookupManager.getInstance(myProject).getActiveLookup();
            myEditor = lookup != null ? lookup.getEditor() : null;
        }
        cancelAndFetchDocInfo(component, new MyCollector(myProject, element, originalElement, null, false));

        myDocInfoHintRef = new WeakReference<>(hint);

        findQuickSearchComponent().ifPresent(quickSearch -> quickSearch.registerHint(hint));

        IdeEventQueue.getInstance().addDispatcher(e -> {
            if (e.getID() == MouseEvent.MOUSE_PRESSED && e.getSource() == hint.getPopupWindow()) {
                myCloseOnSneeze = false;
            }
            return false;
        }, component);
    }

    @RequiredReadAction
    static String getTitle(@Nonnull PsiElement element, boolean isShort) {
        String title = SymbolPresentationUtil.getSymbolPresentableText(element);
        return isShort ? title != null ? title : element.getText()
            : CodeInsightLocalize.javadocInfoTitle(title != null ? title : element.getText()).get();
    }

    @Override
    @Nullable
    @RequiredUIAccess
    public PsiElement findTargetElement(Editor editor, int offset, @Nullable PsiFile file, PsiElement contextElement) {
        try {
            return findTargetElementUnsafe(editor, offset, file, contextElement);
        }
        catch (IndexNotReadyException ex) {
            LOG.warn("Index not ready");
            LOG.debug(ex);
            return null;
        }
    }

    /**
     * in case index is not ready will throw IndexNotReadyException
     */
    @Nullable
    @RequiredUIAccess
    private PsiElement findTargetElementUnsafe(Editor editor, int offset, @Nullable PsiFile file, PsiElement contextElement) {
        if (LookupManager.getInstance(myProject).getActiveLookup() != null) {
            return assertSameProject(getElementFromLookup(editor, file));
        }

        PsiElement element = null;
        if (file != null) {
            DocumentationProvider documentationProvider = DocumentationManagerHelper.getProviderFromElement(file);
            if (documentationProvider instanceof DocumentationProviderEx providerEx) {
                element = assertSameProject(providerEx.getCustomDocumentationElement(editor, file, contextElement));
            }
        }

        if (element == null) {
            element = assertSameProject(TargetElementUtil.findTargetElement(editor, TargetElementUtil.getAllAccepted(), offset));

            // Allow context doc over xml tag content
            if (element != null || contextElement != null) {
                PsiElement adjusted =
                    assertSameProject(TargetElementUtil.adjustElement(editor, TargetElementUtil.getAllAccepted(), element, contextElement));
                if (adjusted != null) {
                    element = adjusted;
                }
            }
        }

        if (element == null) {
            PsiReference ref = TargetElementUtil.findReference(editor, offset);
            if (ref != null) {
                element = assertSameProject(TargetElementUtil.adjustReference(ref));
                if (ref instanceof PsiPolyVariantReference) {
                    element = assertSameProject(ref.getElement());
                }
            }
        }

        DocumentationManagerHelper.storeOriginalElement(myProject, contextElement, element);

        return element;
    }

    @Override
    @Nullable
    @RequiredUIAccess
    public PsiElement getElementFromLookup(Editor editor, @Nullable PsiFile file) {
        Lookup activeLookup = LookupManager.getInstance(myProject).getActiveLookup();

        if (activeLookup != null) {
            LookupElement item = activeLookup.getCurrentItem();
            if (item != null) {
                int offset = editor.getCaretModel().getOffset();
                if (offset > 0 && offset == editor.getDocument().getTextLength()) {
                    offset--;
                }
                PsiReference ref = TargetElementUtil.findReference(editor, offset);
                PsiElement contextElement = file == null ? null : ObjectUtil.coalesce(file.findElementAt(offset), file);
                PsiElement targetElement = ref != null ? ref.getElement() : contextElement;
                if (targetElement != null) {
                    PsiUtilCore.ensureValid(targetElement);
                }

                DocumentationProvider documentationProvider = DocumentationManagerHelper.getProviderFromElement(file);
                PsiManager psiManager = PsiManager.getInstance(myProject);
                PsiElement fromProvider = targetElement == null ? null
                    : documentationProvider.getDocumentationElementForLookupItem(psiManager, item.getObject(), targetElement);
                return fromProvider != null ? fromProvider : CompletionUtil.getTargetElement(item);
            }
        }
        return null;
    }

    @Override
    public String generateDocumentation(@Nonnull PsiElement element, @Nullable PsiElement originalElement, boolean onHover) {
        return new MyCollector(myProject, element, originalElement, null, onHover).getDocumentation();
    }

    @Override
    @Nullable
    public JBPopup getDocInfoHint() {
        if (myDocInfoHintRef == null) {
            return null;
        }
        JBPopup hint = myDocInfoHintRef.get();
        if (hint == null || !hint.isVisible() && !myProject.getApplication().isUnitTestMode()) {
            if (hint != null) {
                // hint's window might've been hidden by AWT without notifying us
                // dispose to remove the popup from IDE hierarchy and avoid leaking components
                hint.cancel();
            }
            myDocInfoHintRef = null;
            return null;
        }
        return hint;
    }

    @RequiredReadAction
    public void fetchDocInfo(@Nonnull PsiElement element, @Nonnull DocumentationComponent component) {
        cancelAndFetchDocInfo(component, new MyCollector(myProject, element, null, null, false));
    }

    public ActionCallback queueFetchDocInfo(@Nonnull PsiElement element, @Nonnull DocumentationComponent component) {
        return doFetchDocInfo(component, new MyCollector(myProject, element, null, null, false));
    }

    @RequiredReadAction
    private ActionCallback cancelAndFetchDocInfo(@Nonnull DocumentationComponent component, @Nonnull DocumentationCollector provider) {
        updateToolWindowTabName(provider.element);
        myUpdateDocAlarm.cancelAllRequests();
        return doFetchDocInfo(component, provider);
    }

    @RequiredReadAction
    void updateToolWindowTabName(@Nonnull PsiElement element) {
        if (myToolWindow != null) {
            Content content = myToolWindow.getContentManager().getSelectedContent();
            if (content != null) {
                content.setDisplayName(getTitle(element, true));
            }
        }
    }

    private ActionCallback doFetchDocInfo(@Nonnull DocumentationComponent component, @Nonnull DocumentationCollector collector) {
        ActionCallback callback = new ActionCallback();
        myLastAction = callback;
        if (myPrecalculatedDocumentation != null) {
            LOG.debug("Setting precalculated documentation:\n", myPrecalculatedDocumentation);
            PsiElement element = collector.element;
            PsiElement originalElement = collector instanceof MyCollector myCollector ? myCollector.originalElement : element;
            DocumentationProvider provider =
                ReadAction.compute(() -> DocumentationManagerHelper.getProviderFromElement(element, originalElement));
            component.setData(element, myPrecalculatedDocumentation, collector.effectiveUrl, collector.ref, provider);
            callback.setDone();
            myPrecalculatedDocumentation = null;
            return callback;
        }
        boolean wasEmpty = component.isEmpty();
        component.startWait();
        if (wasEmpty) {
            component.setText(CodeInsightLocalize.javadocFetchingProgress().get(), collector.element, collector.provider);
            AbstractPopup jbPopup = (AbstractPopup)getDocInfoHint();
            if (jbPopup != null) {
                jbPopup.setDimensionServiceKey(null);
            }
        }

        IdeaModalityState modality = IdeaModalityState.defaultModalityState();

        myUpdateDocAlarm.addRequest(() -> {
            if (myProject.isDisposed()) {
                return;
            }
            LOG.debug("Started fetching documentation...");

            PsiElement element = ReadAction.compute(() -> collector.element.isValid() ? collector.element : null);
            if (element == null) {
                LOG.debug("Element for which documentation was requested is not available anymore");
                return;
            }

            Throwable fail = null;
            String text = null;
            try {
                text = collector.getDocumentation();
            }
            catch (Throwable e) {
                LOG.info(e);
                fail = e;
            }

            if (fail != null) {
                Throwable finalFail = fail;
                GuiUtils.invokeLaterIfNeeded(() -> {
                    String message = finalFail instanceof IndexNotReadyException
                        ? "Documentation is not available until indices are built."
                        : CodeInsightBundle.message("javadoc.external.fetch.error.message");
                    component.setText(message, null, collector.provider);
                    component.clearHistory();
                    callback.setDone();
                }, IdeaModalityState.any());
                return;
            }

            LOG.debug("Documentation fetched successfully:\n", text);

            String finalText = text;
            PsiDocumentManager.getInstance(myProject).performLaterWhenAllCommitted(() -> {
                if (!element.isValid()) {
                    LOG.debug("Element for which documentation was requested is not valid");
                    callback.setDone();
                    return;
                }
                String currentText = component.getText();
                if (finalText == null) {
                    component.setText(CodeInsightLocalize.noDocumentationFound().get(), element, collector.provider);
                }
                else if (finalText.isEmpty()) {
                    component.setText(currentText, element, collector.provider);
                }
                else {
                    component.setData(element, finalText, collector.effectiveUrl, collector.ref, collector.provider);
                }
                if (wasEmpty) {
                    component.clearHistory();
                }
                callback.setDone();
            }, modality);
        }, 10);
        return callback;
    }

    @RequiredUIAccess
    public void navigateByLink(DocumentationComponent component, String url) {
        component.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        PsiElement psiElement = component.getElement();
        if (psiElement == null) {
            return;
        }
        PsiManager manager = PsiManager.getInstance(getProject(psiElement));
        if (url.equals("external_doc")) {
            component.showExternalDoc();
            return;
        }
        if (url.startsWith("open")) {
            PsiFile containingFile = psiElement.getContainingFile();
            OrderEntry libraryEntry = null;
            if (containingFile != null) {
                VirtualFile virtualFile = containingFile.getVirtualFile();
                libraryEntry = LibraryUtil.findLibraryEntry(virtualFile, myProject);
            }
            else if (psiElement instanceof PsiDirectoryContainer directoryContainer) {
                PsiDirectory[] directories = directoryContainer.getDirectories();
                for (PsiDirectory directory : directories) {
                    VirtualFile virtualFile = directory.getVirtualFile();
                    libraryEntry = LibraryUtil.findLibraryEntry(virtualFile, myProject);
                    if (libraryEntry != null) {
                        break;
                    }
                }
            }

            if (libraryEntry != null) {
                OrderEntryType type = libraryEntry.getType();
                OrderEntryTypeEditor editor = OrderEntryTypeEditor.getEditor(type.getId());
                editor.navigate(libraryEntry);
            }
        }
        else if (url.startsWith(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL)) {
            String refText = url.substring(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL.length());
            int separatorPos = refText.lastIndexOf(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL_REF_SEPARATOR);
            String ref = null;
            if (separatorPos >= 0) {
                ref = refText.substring(separatorPos + DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL_REF_SEPARATOR.length());
                refText = refText.substring(0, separatorPos);
            }
            DocumentationProvider provider = DocumentationManagerHelper.getProviderFromElement(psiElement);
            PsiElement targetElement = provider.getDocumentationElementForLink(manager, refText, psiElement);
            if (targetElement == null) {
                for (DocumentationProvider documentationProvider
                    : Application.get().getExtensionList(UnrestrictedDocumentationProvider.class)) {
                    targetElement = documentationProvider.getDocumentationElementForLink(manager, refText, psiElement);
                    if (targetElement != null) {
                        break;
                    }
                }
            }
            if (targetElement == null) {
                for (Language language : Language.getRegisteredLanguages()) {
                    DocumentationProvider documentationProvider = LanguageDocumentationProvider.forLanguageComposite(language);
                    if (documentationProvider != null) {
                        targetElement = documentationProvider.getDocumentationElementForLink(manager, refText, psiElement);
                        if (targetElement != null) {
                            break;
                        }
                    }
                }
            }
            if (targetElement != null) {
                cancelAndFetchDocInfo(component, new MyCollector(myProject, targetElement, null, ref, false));
            }
        }
        else {
            DocumentationProvider provider = DocumentationManagerHelper.getProviderFromElement(psiElement);
            boolean processed = false;
            if (provider instanceof CompositeDocumentationProvider compositeDocumentationProvider) {
                for (DocumentationProvider p : compositeDocumentationProvider.getAllProviders()) {
                    if (!(p instanceof ExternalDocumentationHandler)) {
                        continue;
                    }

                    ExternalDocumentationHandler externalHandler = (ExternalDocumentationHandler)p;
                    if (externalHandler.canFetchDocumentationLink(url)) {
                        String ref = externalHandler.extractRefFromLink(url);
                        cancelAndFetchDocInfo(component, new DocumentationCollector(psiElement, url, ref, p) {
                            @Override
                            public String getDocumentation() {
                                return externalHandler.fetchExternalDocumentation(url, psiElement);
                            }
                        });
                        processed = true;
                    }
                    else if (externalHandler.handleExternalLink(manager, url, psiElement)) {
                        processed = true;
                        break;
                    }
                }
            }

            if (!processed) {
                cancelAndFetchDocInfo(component, new DocumentationCollector(psiElement, url, null, provider) {
                    @Override
                    public String getDocumentation() {
                        if (BrowserUtil.isAbsoluteURL(url)) {
                            BrowserUtil.browse(url);
                            return "";
                        }
                        else {
                            return CodeInsightLocalize.javadocErrorResolvingUrl(url).get();
                        }
                    }
                });
            }
        }

        component.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    @Nonnull
    @Override
    public Project getProject(@Nullable PsiElement element) {
        assertSameProject(element);
        return myProject;
    }

    private PsiElement assertSameProject(@Nullable PsiElement element) {
        if (element != null && element.isValid() && myProject != element.getProject()) {
            throw new AssertionError(myProject + "!=" + element.getProject() + "; element=" + element);
        }
        return element;
    }

    public static void createHyperlink(StringBuilder buffer, String refText, String label, boolean plainLink) {
        DocumentationManagerUtil.createHyperlink(buffer, refText, label, plainLink);
    }

    @Override
    public String getShowInToolWindowProperty() {
        return SHOW_DOCUMENTATION_IN_TOOL_WINDOW;
    }

    @Override
    public String getAutoUpdateEnabledProperty() {
        return DOCUMENTATION_AUTO_UPDATE_ENABLED;
    }

    @Override
    @RequiredUIAccess
    protected void doUpdateComponent(PsiElement element, PsiElement originalElement, DocumentationComponent component) {
        cancelAndFetchDocInfo(component, new MyCollector(myProject, element, originalElement, null, false));
    }

    @Override
    @RequiredUIAccess
    protected void doUpdateComponent(Editor editor, PsiFile psiFile, boolean requestFocus) {
        showJavaDocInfo(editor, psiFile, requestFocus, null);
    }

    @Override
    @RequiredUIAccess
    protected void doUpdateComponent(Editor editor, PsiFile psiFile) {
        doUpdateComponent(editor, psiFile, false);
    }

    @Override
    @RequiredUIAccess
    protected void doUpdateComponent(@Nonnull PsiElement element) {
        showJavaDocInfo(element, element, null);
    }

    @Override
    @RequiredReadAction
    protected String getTitle(PsiElement element) {
        return getTitle(element, true);
    }

    @Nullable
    Image getElementImage(@Nonnull PsiElement element, @Nonnull String imageSpec) {
        DocumentationProvider provider = DocumentationManagerHelper.getProviderFromElement(element);
        if (provider instanceof CompositeDocumentationProvider compositeProvider) {
            for (DocumentationProvider p : compositeProvider.getAllProviders()) {
                if (p instanceof DocumentationProviderEx providerEx) {
                    Image image = providerEx.getLocalImageForElement(element, imageSpec);
                    if (image != null) {
                        return image;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Editor getEditor() {
        return myEditor;
    }

    @TestOnly
    public ActionCallback getLastAction() {
        return myLastAction;
    }

    @TestOnly
    public void setDocumentationComponent(DocumentationComponent documentationComponent) {
        myTestDocumentationComponent = documentationComponent;
    }

    private abstract static class DocumentationCollector {
        final PsiElement element;
        final String ref;

        volatile DocumentationProvider provider;
        String effectiveUrl;

        DocumentationCollector(PsiElement element, String effectiveUrl, String ref, DocumentationProvider provider) {
            this.element = element;
            this.ref = ref;
            this.effectiveUrl = effectiveUrl;
            this.provider = provider;
        }

        @Nullable
        abstract String getDocumentation() throws Exception;
    }

    private static class MyCollector extends DocumentationCollector {

        final Project project;
        final PsiElement originalElement;
        final boolean onHover;

        MyCollector(@Nonnull Project project, @Nonnull PsiElement element, PsiElement originalElement, String ref, boolean onHover) {
            super(element, null, ref, null);
            this.project = project;
            this.originalElement = originalElement;
            this.onHover = onHover;
        }

        @Override
        @Nullable
        public String getDocumentation() {
            provider = ReadAction.compute(() -> DocumentationManagerHelper.getProviderFromElement(element, originalElement));
            LOG.debug("Using provider ", provider);

            if (provider instanceof ExternalDocumentationProvider externalProvider) {
                List<String> urls = ReadAction.compute(() -> {
                    SmartPsiElementPointer originalElementPtr = element.getUserData(DocumentationManagerHelper.ORIGINAL_ELEMENT_KEY);
                    PsiElement originalElement = originalElementPtr != null ? originalElementPtr.getElement() : null;
                    return provider.getUrlFor(element, originalElement);
                });
                LOG.debug("External documentation URLs: ", urls);
                if (urls != null) {
                    for (String url : urls) {
                        String doc = externalProvider.fetchExternalDocumentation(project, element, Collections.singletonList(url));
                        if (doc != null) {
                            LOG.debug("Fetched documentation from ", url);
                            effectiveUrl = url;
                            return doc;
                        }
                    }
                }
            }

            Ref<String> result = new Ref<>();
            QuickDocUtil.runInReadActionWithWriteActionPriorityWithRetries(() -> {
                if (!element.isValid()) {
                    return;
                }
                SmartPsiElementPointer originalPointer = element.getUserData(DocumentationManagerHelper.ORIGINAL_ELEMENT_KEY);
                PsiElement originalPsi = originalPointer != null ? originalPointer.getElement() : null;
                String doc = onHover ? provider.generateHoverDoc(element, originalPsi) : provider.generateDoc(element, originalPsi);
                if (element instanceof PsiFile file) {
                    String fileDoc = generateFileDoc(file, doc == null);
                    if (fileDoc != null) {
                        doc = doc == null ? fileDoc : doc + fileDoc;
                    }
                }
                result.set(doc);
            }, DOC_GENERATION_TIMEOUT_MILLISECONDS, DOC_GENERATION_PAUSE_MILLISECONDS);
            return result.get();
        }
    }

    @Nullable
    @RequiredReadAction
    private static String generateFileDoc(@Nonnull PsiFile psiFile, boolean withUrl) {
        VirtualFile file = PsiUtilCore.getVirtualFile(psiFile);
        File ioFile = file == null || !file.isInLocalFileSystem() ? null : VfsUtilCore.virtualToIoFile(file);
        BasicFileAttributes attr = null;
        try {
            attr = ioFile == null ? null : Files.readAttributes(Paths.get(ioFile.toURI()), BasicFileAttributes.class);
        }
        catch (Exception ignored) {
        }
        if (attr == null) {
            return null;
        }
        FileType type = file.getFileType();
        String typeName = type == UnknownFileType.INSTANCE
            ? "Unknown"
            : type == PlainTextFileType.INSTANCE
            ? "Text"
            : type instanceof ArchiveFileType
            ? "Archive"
            : type.getId();
        String languageName = type.isBinary() ? "" : psiFile.getLanguage().getDisplayName();
        return (
            withUrl
                ? DocumentationMarkup.DEFINITION_START + file.getPresentableUrl() + DocumentationMarkup.DEFINITION_END +
                DocumentationMarkup.CONTENT_START
                : ""
        ) +
            getVcsStatus(psiFile.getProject(), file) +
            getScope(psiFile.getProject(), file) +
            "<p><span class='grayed'>Size:</span> " +
            StringUtil.formatFileSize(attr.size()) +
            "<p><span class='grayed'>Type:</span> " +
            typeName +
            (type.isBinary() || typeName.equals(languageName) ? "" : " (" + languageName + ")") +
            "<p><span class='grayed'>Modified:</span> " +
            DateFormatUtil.formatDateTime(attr.lastModifiedTime().toMillis()) +
            "<p><span class='grayed'>Created:</span> " +
            DateFormatUtil.formatDateTime(attr.creationTime().toMillis()) +
            (withUrl ? DocumentationMarkup.CONTENT_END : "");
    }

    private static String getScope(Project project, VirtualFile file) {
        FileColorManagerImpl colorManager = (FileColorManagerImpl)FileColorManager.getInstance(project);
        Color color = colorManager.getRendererBackground(file);
        if (color == null) {
            return "";
        }
        for (NamedScopesHolder holder : NamedScopesHolder.getAllNamedScopeHolders(project)) {
            for (NamedScope scope : holder.getScopes()) {
                PackageSet packageSet = scope.getValue();
                String name = scope.getName();
                if (packageSet instanceof PackageSetBase
                    && packageSet.contains(file, project, holder)
                    && colorManager.getScopeColor(name) == color) {
                    return "<p><span class='grayed'>Scope:</span> <span bgcolor='" + ColorUtil.toHex(color) + "'>" + scope.getName() + "</span>";
                }
            }
        }
        return "";
    }

    @Nonnull
    private static String getVcsStatus(Project project, VirtualFile file) {
        FileStatus status = ChangeListManager.getInstance(project).getStatus(file);
        return status != FileStatus.NOT_CHANGED
            ? "<p><span class='grayed'>VCS Status:</span> <span color='" + ColorValueUtil.toHex(status.getColor()) + "'>" +
            status.getText() + "</span>"
            : "";
    }

    private Optional<QuickSearchComponent> findQuickSearchComponent() {
        Component c = SoftReference.dereference(myFocusedBeforePopup);
        while (c != null) {
            if (c instanceof QuickSearchComponent quickSearchComponent) {
                return Optional.of(quickSearchComponent);
            }
            c = c.getParent();
        }
        return Optional.empty();
    }
}
