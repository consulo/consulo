/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.editor.impl.internal.documentation;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ReadAction;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.colorScheme.EditorColorsManager;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.documentation.DocumentationManager;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.internal.DocumentationManagerHelper;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.project.Project;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.BaseNavigateToSourceAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.util.collection.ArrayUtil;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-08-19
 */
@ServiceImpl
@Singleton
public class DocumentationManagerImpl implements DocumentationManager {
    private static final String[] ACTION_IDS_TO_IGNORE = {
        IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN,
        IdeActions.ACTION_EDITOR_MOVE_CARET_UP,
        IdeActions.ACTION_EDITOR_MOVE_CARET_PAGE_DOWN,
        IdeActions.ACTION_EDITOR_MOVE_CARET_PAGE_UP,
        IdeActions.ACTION_EDITOR_ESCAPE
    };
    private static final String[] ACTION_PLACES_TO_IGNORE = {ActionPlaces.JAVADOC_INPLACE_SETTINGS, ActionPlaces.JAVADOC_TOOLBAR};

    private final Project myProject;
    private final DocumentationSettings mySettings;
    private final EditorColorsManager myEditorColorsManager;
    private final Provider<DocumentationToolWindowManager> myToolWindowManager;

    private @Nullable Editor myEditor;
    private boolean myCloseOnSneeze;

    private @Nullable JBPopup myPopup;
    private @Nullable DocumentationViewImpl myPopupView;

    @Inject
    public DocumentationManagerImpl(
        Project project,
        DocumentationSettings settings,
        EditorColorsManager editorColorsManager,
        Provider<DocumentationToolWindowManager> toolWindowManager
    ) {
        myProject = project;
        mySettings = settings;
        myEditorColorsManager = editorColorsManager;
        myToolWindowManager = toolWindowManager;

        project.getApplication().getMessageBus().connect(project).subscribe(AnActionListener.class, new AnActionListener() {
            @Override
            public void beforeActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
                if (getDocInfoHint() != null
                    && LookupManager.getActiveLookup(myEditor) == null
                    && !(action instanceof HintManager.ActionToIgnore)
                    && !(action instanceof BaseNavigateToSourceAction)
                    && !ArrayUtil.contains(event.getPlace(), ACTION_PLACES_TO_IGNORE)
                    && !isIgnoredById(action)) {
                    closeDocHint();
                }
            }

            @Override
            public void beforeEditorTyping(char c, DataContext dataContext) {
                JBPopup hint = getDocInfoHint();
                if (hint != null && LookupManager.getActiveLookup(myEditor) == null) {
                    hint.cancel();
                }
            }
        });
    }

    private static boolean isIgnoredById(AnAction action) {
        ActionManager actionManager = ActionManager.getInstance();
        for (String id : ACTION_IDS_TO_IGNORE) {
            if (actionManager.getAction(id) == action) {
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    private void closeDocHint() {
        JBPopup hint = getDocInfoHint();
        if (hint != null) {
            myCloseOnSneeze = false;
            hint.cancel();
        }
    }

    @Override
    @RequiredUIAccess
    public void showJavaDocInfo(
        PsiElement element,
        PsiElement original,
        boolean requestFocus,
        @Nullable Runnable closeCallback,
        @Nullable String documentation,
        boolean useStoredPopupSize
    ) {
        Editor editor = myEditor;
        withSource(element, original, documentation, source -> show(source, editor, requestFocus, closeCallback, null));
    }

    @Override
    @RequiredUIAccess
    public void showJavaDocInfo(
        Editor editor,
        PsiElement element,
        PsiElement original,
        @Nullable Runnable closeCallback,
        @Nullable String documentation,
        boolean closeOnSneeze,
        boolean useStoredPopupSize
    ) {
        myEditor = editor;
        myCloseOnSneeze = closeOnSneeze;
        withSource(element, original, documentation, source -> show(source, editor, false, closeCallback, null));
    }

    @Override
    @RequiredUIAccess
    public void showJavaDocInfo(Editor editor, PsiElement element, PsiElement original, @Nullable RelativePoint popupAnchor) {
        myEditor = editor;
        withSource(element, original, null, source -> show(source, editor, false, null, popupAnchor));
    }

    @Override
    @RequiredUIAccess
    public void showJavaDocInfoAtToolWindow(PsiElement element, PsiElement original) {
        withSource(element, original, null, source -> myToolWindowManager.get().show(source, false));
    }

    @Override
    @RequiredUIAccess
    public void createToolWindow(PsiElement element, PsiElement originalElement) {
        showJavaDocInfoAtToolWindow(element, originalElement);
    }

    @Override
    @RequiredUIAccess
    public void showJavaDocInfo(Editor editor, @Nullable PsiFile file, boolean requestFocus, @Nullable Runnable closeCallback) {
        myEditor = editor;
        if (file == null) {
            return;
        }

        PsiDocumentManager.getInstance(myProject).commitAllDocuments();

        int offset = editor.getCaretModel().getOffset();
        Lookup lookup = LookupManager.getActiveLookup(editor);
        LookupElement lookupItem = lookup == null ? null : lookup.getCurrentItem();

        CoroutineScope.launchAsync(
            myProject.coroutineContext(),
            () -> Coroutine
                .first(ReadLock.<Object, Optional<DocumentationElementSource>>apply(ignored -> {
                    if (!file.isValid() || editor.isDisposed()) {
                        return Optional.empty();
                    }
                    return Optional.ofNullable(DocumentationTargetFinder.findAtOffset(myProject, editor, file, offset, lookupItem));
                }))
                .then(UIAction.<Optional<DocumentationElementSource>, Optional<DocumentationElementSource>>apply(source -> {
                    source.ifPresent(it -> show(it, editor, requestFocus, closeCallback, null));
                    return source;
                }))
        );
    }

    @RequiredUIAccess
    private void withSource(
        PsiElement element,
        @Nullable PsiElement original,
        @Nullable String documentation,
        Consumer<DocumentationElementSource> consumer
    ) {
        CoroutineScope.launchAsync(
            myProject.coroutineContext(),
            () -> Coroutine
                .first(ReadLock.<Object, Optional<DocumentationElementSource>>apply(ignored -> createSource(element, original, documentation)))
                .then(UIAction.<Optional<DocumentationElementSource>, Optional<DocumentationElementSource>>apply(source -> {
                    source.ifPresent(consumer);
                    return source;
                }))
        );
    }

    @RequiredReadAction
    private Optional<DocumentationElementSource> createSource(PsiElement element, @Nullable PsiElement original, @Nullable String documentation) {
        if (!element.isValid()) {
            return Optional.empty();
        }

        if (original != null && original.isValid()) {
            DocumentationManagerHelper.storeOriginalElement(myProject, original, element);
        }

        SmartPointerManager pointerManager = SmartPointerManager.getInstance(myProject);
        SmartPsiElementPointer<PsiElement> originalPointer =
            original == null || !original.isValid() ? null : pointerManager.createSmartPsiElementPointer(original);
        return Optional.of(new DocumentationElementSource(pointerManager.createSmartPsiElementPointer(element), originalPointer, null, documentation));
    }

    @RequiredUIAccess
    private void show(
        DocumentationSource source,
        @Nullable Editor editor,
        boolean requestFocus,
        @Nullable Runnable closeCallback,
        @Nullable RelativePoint anchor
    ) {
        if (myProject.isDisposed()) {
            return;
        }

        DocumentationToolWindowManager toolWindowManager = myToolWindowManager.get();
        if (toolWindowManager.getView() != null || mySettings.isShowInToolWindow()) {
            if (toolWindowManager.show(source, requestFocus)) {
                return;
            }
        }

        DocumentationViewImpl popupView = myPopupView;
        if (getDocInfoHint() != null && popupView != null) {
            popupView.show(source);
            return;
        }

        showInPopup(source, editor, requestFocus, closeCallback, anchor);
    }

    @RequiredUIAccess
    private void showInPopup(
        DocumentationSource source,
        @Nullable Editor editor,
        boolean requestFocus,
        @Nullable Runnable closeCallback,
        @Nullable RelativePoint anchor
    ) {
        JBPopup previous = myPopup;
        if (previous != null) {
            previous.cancel();
        }

        DocumentationViewImpl view = new DocumentationViewImpl(
            myProject,
            mySettings,
            myEditorColorsManager,
            browser -> List.of(new DocumentationOpenInToolWindowAction(browser, this::closeDocHint))
        );
        view.setSizeToContent(true);

        boolean hasLookup = editor != null && LookupManager.getActiveLookup(editor) != null;

        JBPopup popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(view.getComponent(), view.getHtmlView())
            .setProject(myProject)
            .setResizable(true)
            .setMovable(true)
            .setFocusable(true)
            .setRequestFocus(requestFocus)
            .setCancelOnClickOutside(!hasLookup)
            .setModalContext(false)
            .createPopup();

        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(LightweightWindowEvent event) {
                if (myPopup == popup) {
                    myPopup = null;
                    myPopupView = null;
                    myCloseOnSneeze = false;
                    myEditor = null;
                }

                Disposer.dispose(view);

                if (closeCallback != null) {
                    closeCallback.run();
                }
            }
        });

        myPopup = popup;
        myPopupView = view;

        boolean[] shown = {false};
        Disposer.register(view, view.addRenderListener(() -> {
            if (popup.isDisposed()) {
                return;
            }

            if (!shown[0]) {
                shown[0] = true;
                showPopup(popup, editor, anchor);
            }
            else {
                popup.pack(true, true);
            }
        }));
        Disposer.register(view, view.addNavigateListener(popup::cancel));

        view.show(source);
    }

    @RequiredUIAccess
    private void showPopup(JBPopup popup, @Nullable Editor editor, @Nullable RelativePoint anchor) {
        if (anchor != null) {
            popup.show(anchor);
        }
        else if (editor != null && !editor.isDisposed()) {
            EditorPopupHelper.getInstance().showPopupInBestPositionFor(editor, popup);
        }
        else {
            popup.showCenteredInCurrentWindow(myProject);
        }
    }

    @Override
    @RequiredUIAccess
    public @Nullable PsiElement getElementFromLookup(Editor editor, @Nullable PsiFile file) {
        Lookup lookup = LookupManager.getActiveLookup(editor);
        LookupElement item = lookup == null ? null : lookup.getCurrentItem();
        if (item == null || file == null) {
            return null;
        }

        int offset = editor.getCaretModel().getOffset();
        return ReadAction.compute(() -> DocumentationTargetFinder.findForLookupItem(myProject, editor, file, offset, item));
    }

    @Override
    public @Nullable JBPopup getDocInfoHint() {
        JBPopup popup = myPopup;
        return popup != null && !popup.isDisposed() && popup.isVisible() ? popup : null;
    }

    @Override
    @RequiredUIAccess
    public boolean hasActiveDockedDocWindow() {
        return myToolWindowManager.get().isShowing();
    }

    @Override
    @RequiredUIAccess
    public void setAllowContentUpdateFromContext(boolean allow) {
        myToolWindowManager.get().setAutoUpdatePaused(!allow);
    }

    @Override
    @RequiredUIAccess
    public void updateToolwindowContext() {
        DocumentationToolWindowManager toolWindowManager = myToolWindowManager.get();
        if (!toolWindowManager.isShowing()) {
            return;
        }

        Editor editor = FileEditorManager.getInstance(myProject).getSelectedTextEditor();
        if (editor != null) {
            toolWindowManager.updateFromEditor(editor);
        }
    }

    @Override
    public boolean isCloseOnSneeze() {
        return myCloseOnSneeze;
    }

    @Override
    public Project getProject(@Nullable PsiElement element) {
        return myProject;
    }

    @Override
    public Editor getEditor() {
        return myEditor;
    }

    @Override
    public @Nullable PsiElement findTargetElement(Editor editor, int offset, @Nullable PsiFile file, PsiElement contextElement) {
        return ReadAction.compute(() -> DocumentationTargetFinder.findTargetElement(myProject, editor, offset, file, contextElement));
    }

    @Override
    public String generateDocumentation(PsiElement element, @Nullable PsiElement originalElement, boolean onHover) {
        return new ElementDocumentationCollector(myProject, element, originalElement, null, onHover).getDocumentation();
    }
}
