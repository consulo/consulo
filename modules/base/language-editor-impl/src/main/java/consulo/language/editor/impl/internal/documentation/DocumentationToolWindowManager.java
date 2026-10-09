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
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.colorScheme.EditorColorsManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.UIAccess;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.DumbAwareToggleAction;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.ui.ex.toolWindow.ContentManagerWatcher;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class DocumentationToolWindowManager implements Disposable {
    private static final long AUTO_UPDATE_DELAY_MILLISECONDS = 500;

    private final Project myProject;
    private final Provider<ToolWindowManager> myToolWindowManager;
    private final DocumentationSettings mySettings;
    private final EditorColorsManager myEditorColorsManager;
    private final ContentFactory myContentFactory;
    private final EditorFactory myEditorFactory;

    private @Nullable Content myContent;
    private @Nullable DocumentationViewImpl myView;

    private @Nullable ScheduledFuture<?> myAutoUpdate;
    private boolean myCaretListenerInstalled;
    private boolean myAutoUpdatePaused;

    @Inject
    public DocumentationToolWindowManager(
        Project project,
        Provider<ToolWindowManager> toolWindowManager,
        DocumentationSettings settings,
        EditorColorsManager editorColorsManager,
        ContentFactory contentFactory,
        EditorFactory editorFactory
    ) {
        myProject = project;
        myToolWindowManager = toolWindowManager;
        mySettings = settings;
        myEditorColorsManager = editorColorsManager;
        myContentFactory = contentFactory;
        myEditorFactory = editorFactory;
    }

    @RequiredUIAccess
    void initToolWindow(ToolWindow toolWindow) {
        ContentManager contentManager = toolWindow.getContentManager();
        ContentManagerWatcher.watchContentManager(toolWindow, contentManager);

        contentManager.addContentManagerListener(new ContentManagerListener() {
            @Override
            @RequiredUIAccess
            public void contentRemoved(ContentManagerEvent event) {
                if (event.getContent() == myContent) {
                    myContent = null;
                    myView = null;
                    mySettings.setShowInToolWindow(false);
                }
            }
        });

        Supplier<@Nullable DocumentationBrowser> browser = () -> {
            DocumentationViewImpl view = myView;
            return view == null ? null : view.getBrowser();
        };

        toolWindow.setTitleActions(
            new DocumentationBackAction(browser),
            new DocumentationForwardAction(browser),
            new DocumentationEditSourceAction(browser, () -> {
            }),
            new DocumentationExternalAction(browser)
        );

        toolWindow.setAdditionalGearActions(ActionGroup.newImmutableBuilder()
            .add(new ShowInPopupAction())
            .add(new AutoUpdateAction())
            .add(new DocumentationFontSizeGroup(mySettings))
            .build());
    }

    public @Nullable ToolWindow getToolWindow() {
        return myToolWindowManager.get().getToolWindow(ToolWindowId.DOCUMENTATION);
    }

    @RequiredUIAccess
    public boolean isShowing() {
        ToolWindow toolWindow = getToolWindow();
        return myView != null && toolWindow != null && toolWindow.isVisible();
    }

    public @Nullable DocumentationViewImpl getView() {
        return myView;
    }

    @RequiredUIAccess
    public boolean show(DocumentationSource source, boolean requestFocus) {
        ToolWindow toolWindow = getToolWindow();
        if (toolWindow == null) {
            return false;
        }

        DocumentationViewImpl view = getOrCreateView(toolWindow);
        view.show(source);

        mySettings.setShowInToolWindow(true);
        installCaretListener();

        if (requestFocus) {
            toolWindow.activate(null);
        }
        else if (!toolWindow.isVisible()) {
            toolWindow.show();
        }
        return true;
    }

    @RequiredUIAccess
    public void update(DocumentationSource source) {
        DocumentationViewImpl view = myView;
        if (view != null) {
            view.show(source);
        }
    }

    @RequiredUIAccess
    public void close() {
        ToolWindow toolWindow = getToolWindow();
        Content content = myContent;
        if (toolWindow != null && content != null) {
            toolWindow.getContentManager().removeContent(content, true);
        }
        mySettings.setShowInToolWindow(false);
    }

    @RequiredUIAccess
    private DocumentationViewImpl getOrCreateView(ToolWindow toolWindow) {
        DocumentationViewImpl view = myView;
        Content content = myContent;
        if (view != null && content != null) {
            toolWindow.getContentManager().setSelectedContent(content);
            return view;
        }

        DocumentationViewImpl newView = new DocumentationViewImpl(myProject, mySettings, myEditorColorsManager, null);
        Content newContent = myContentFactory.createUIContent(newView.getComponent(), "", false);
        newContent.setCloseable(true);
        newContent.setDisposer(newView);

        Disposer.register(newView, newView.getBrowser().addPageListener(page -> {
            if (myContent == newContent) {
                newContent.setDisplayName(page.title());
            }
        }));

        myView = newView;
        myContent = newContent;

        ContentManager contentManager = toolWindow.getContentManager();
        contentManager.addContent(newContent);
        contentManager.setSelectedContent(newContent);
        return newView;
    }

    @RequiredUIAccess
    private void installCaretListener() {
        if (myCaretListenerInstalled) {
            return;
        }
        myCaretListenerInstalled = true;

        myEditorFactory.getEventMulticaster().addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(CaretEvent event) {
                scheduleAutoUpdate(event.getEditor());
            }
        }, this);
    }

    @RequiredUIAccess
    public void setAutoUpdatePaused(boolean paused) {
        myAutoUpdatePaused = paused;
    }

    private void scheduleAutoUpdate(Editor editor) {
        if (myView == null || myAutoUpdatePaused || !mySettings.isAutoUpdate() || editor.getProject() != myProject) {
            return;
        }

        ScheduledFuture<?> previous = myAutoUpdate;
        if (previous != null) {
            previous.cancel(false);
        }

        myAutoUpdate = AppExecutorUtil.getAppScheduledExecutorService().schedule(
            () -> {
                UIAccess uiAccess = myProject.getUIAccess();
                if (!myProject.isDisposed() && uiAccess.isValid()) {
                    uiAccess.give(() -> autoUpdate(editor));
                }
            },
            AUTO_UPDATE_DELAY_MILLISECONDS,
            TimeUnit.MILLISECONDS
        );
    }

    @RequiredUIAccess
    public void updateFromEditor(Editor editor) {
        autoUpdate(editor);
    }

    @RequiredUIAccess
    private void autoUpdate(Editor editor) {
        if (editor.isDisposed() || !isShowing()) {
            return;
        }

        Document document = editor.getDocument();
        int offset = editor.getCaretModel().getOffset();

        CoroutineScope.launchAsync(
            myProject.coroutineContext(),
            () -> Coroutine
                .first(ReadLock.<Object, Optional<DocumentationElementSource>>apply(ignored -> {
                    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);
                    PsiFile file = documentManager.isCommitted(document) ? documentManager.getPsiFile(document) : null;
                    if (file == null || editor.isDisposed()) {
                        return Optional.empty();
                    }
                    DocumentationElementSource source = DocumentationTargetFinder.findAtOffset(myProject, editor, file, offset, null);
                    return Optional.ofNullable(source == null || isCurrent(source) ? null : source);
                }))
                .then(UIAction.<Optional<DocumentationElementSource>, Optional<DocumentationElementSource>>apply(source -> {
                    source.ifPresent(this::update);
                    return source;
                }))
        );
    }

    @RequiredReadAction
    private boolean isCurrent(DocumentationElementSource source) {
        DocumentationViewImpl view = myView;
        DocumentationPage page = view == null ? null : view.getBrowser().getPage();
        SmartPsiElementPointer<? extends PsiElement> current = page == null ? null : page.element();
        return current != null && SmartPointerManager.getInstance(myProject).pointToTheSameElement(current, source.element());
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> autoUpdate = myAutoUpdate;
        if (autoUpdate != null) {
            autoUpdate.cancel(false);
        }
    }

    private class ShowInPopupAction extends DumbAwareAction {
        ShowInPopupAction() {
            super(CodeInsightLocalize.javadocActionShowInPopup());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            close();
        }
    }

    private class AutoUpdateAction extends DumbAwareToggleAction {
        AutoUpdateAction() {
            super(CodeInsightLocalize.javadocActionAutoUpdate(), LocalizeValue.empty(), PlatformIconGroup.generalAutoscrollfromsource());
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return mySettings.isAutoUpdate();
        }

        @Override
        @RequiredUIAccess
        public void setSelected(AnActionEvent e, boolean state) {
            mySettings.setAutoUpdate(state);
        }
    }
}
