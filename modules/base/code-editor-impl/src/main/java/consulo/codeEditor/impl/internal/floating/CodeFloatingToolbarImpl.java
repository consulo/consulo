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
package consulo.codeEditor.impl.internal.floating;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.concurrent.coroutine.DisposableCoroutineScope;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.codeEditor.Editor;
import consulo.codeEditor.PersistentEditorSettings;
import consulo.codeEditor.SelectionModel;
import consulo.codeEditor.event.SelectionEvent;
import consulo.codeEditor.event.SelectionListener;
import consulo.codeEditor.event.VisibleAreaEvent;
import consulo.codeEditor.internal.CaretPixelLocationProvider;
import consulo.codeEditor.internal.CaretPixelLocationProvider.CaretPixelLocation;
import consulo.codeEditor.internal.EditorSelectionGestureListener;
import consulo.codeEditor.internal.EditorSelectionGestureSupport;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbar;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbarActions;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbarConfiguration;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbarListener;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbarProvider;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbarSelection;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.project.Project;
import consulo.ui.LightPopup;
import consulo.ui.ModalityState;
import consulo.ui.Popup;
import consulo.ui.PopupOptions;
import consulo.ui.PopupPosition;
import consulo.ui.UIAccess;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.CustomActionsSchema;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.MoreActionGroup;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Represents floating toolbar which is shown for selected text inside editor.
 * Toolbar is visible only if mouse was used for the selection.
 *
 * @author VISTALL
 * @since 2026-11-09
 */
public final class CodeFloatingToolbarImpl implements CodeFloatingToolbar, Disposable {
    private static final long REQUEST_DELAY_MS = 50;
    private static final long SHOW_UNDERNEATH_DELAY_MS = 50;
    private static final int SHOW_UNDERNEATH_ATTEMPTS = 40;

    private enum Request {
        SHOW,
        HIDE
    }

    private final Application myApplication;
    private final Editor myEditor;
    private final Project myProject;
    private final ActionManager myActionManager;
    private final PersistentEditorSettings myEditorSettings;

    private @Nullable LightPopup myPopup;
    private @Nullable ActionToolbar myToolbar;
    private @Nullable CaretPixelLocation myLocation;
    private @Nullable Rectangle myLocationArea;
    private boolean myLocationAbove;
    private List<AnAction> myVisibleActions = List.of();

    private @Nullable Disposable myShowAttempt;
    private long myRequestGeneration;

    /**
     * Prevents toolbar to be shown again if it was already recently closed.
     * At least mouse should be moved out of the selection first.
     */
    private boolean myPreventShowing;
    private @Nullable String myLastSelection;
    private int myDisabledCount;

    private @Nullable JBPopup myActivePopup;
    private @Nullable AnAction myActivePopupAction;

    @RequiredUIAccess
    public CodeFloatingToolbarImpl(
        Application application,
        Editor editor,
        Project project,
        ActionManager actionManager,
        PersistentEditorSettings editorSettings
    ) {
        myApplication = application;
        myEditor = editor;
        myProject = project;
        myActionManager = actionManager;
        myEditorSettings = editorSettings;

        if (editor instanceof EditorSelectionGestureSupport gestureSupport) {
            gestureSupport.addSelectionGestureListener(new EditorSelectionGestureListener() {
                @Override
                @RequiredUIAccess
                public void mouseReleased(int clickCount) {
                    onMouseReleased(clickCount);
                }

                @Override
                @RequiredUIAccess
                public void mouseOverSelectionChanged(boolean overSelection) {
                    if (overSelection) {
                        scheduleShow();
                    }
                    else if (!isShown()) {
                        myPreventShowing = false;
                    }
                }
            }, this);
        }

        editor.getSelectionModel().addSelectionListener(new SelectionListener() {
            @Override
            @RequiredUIAccess
            public void selectionChanged(SelectionEvent e) {
                myPreventShowing = false;
                if (isShown()) {
                    scheduleHide();
                }
            }
        }, this);

        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            @RequiredUIAccess
            public void documentChanged(DocumentEvent event) {
                myPreventShowing = false;
                scheduleHide();
            }
        }, this);

        editor.getScrollingModel().addVisibleAreaListener(this::scrolled, this);

        if (editor instanceof CaretPixelLocationProvider provider) {
            provider.addCaretPixelLocationListener(this::relocate, this);
        }
    }

    @RequiredUIAccess
    private void onMouseReleased(int clickCount) {
        if (clickCount == 2) {
            myPreventShowing = true;
        }

        String newSelection = myEditor.getSelectionModel().getSelectedText();
        if (newSelection == null) {
            scheduleHide();
        }
        else if (!newSelection.equals(myLastSelection)) {
            scheduleShow();
        }
        myLastSelection = newSelection;
    }

    @RequiredUIAccess
    private boolean isEnabled() {
        return myEditorSettings.isShowCodeFloatingToolbar() && myEditor.getDocument().isWritable() && myDisabledCount == 0;
    }

    @Override
    @RequiredUIAccess
    public boolean isShown() {
        return myPopup != null;
    }

    @Override
    @RequiredUIAccess
    public void scheduleShow() {
        if (isEnabled() && !myPreventShowing) {
            request(Request.SHOW);
        }
    }

    @Override
    @RequiredUIAccess
    public void scheduleHide() {
        request(Request.HIDE);
    }

    @Override
    @RequiredUIAccess
    public void allowInstantShowing() {
        myPreventShowing = false;
    }

    @RequiredUIAccess
    private void request(Request request) {
        long generation = ++myRequestGeneration;
        UIAccess.current().getScheduler().schedule(() -> {
            if (generation != myRequestGeneration || Disposer.isDisposed(this)) {
                return;
            }

            if (request == Request.SHOW) {
                showIfHidden(new CompletableFuture<>());
            }
            else {
                hide();
            }
        }, ModalityState.any(), REQUEST_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    @RequiredUIAccess
    private CompletableFuture<Boolean> showIfHidden(CompletableFuture<Boolean> result) {
        myPreventShowing = true;
        if (!isEnabled()) {
            result.complete(false);
            return result;
        }

        if (isShown()) {
            relocate();
            result.complete(true);
            return result;
        }

        cancelShowAttempt();

        SelectionModel selectionModel = myEditor.getSelectionModel();
        CodeFloatingToolbarSelection selection = new CodeFloatingToolbarSelection(
            selectionModel.getSelectionStart(),
            selectionModel.getSelectionEnd(),
            myEditor.getCaretModel().getOffset(),
            selectionModel.hasSelection()
        );
        long generation = myRequestGeneration;
        Disposable attempt = Disposable.newDisposable("CodeFloatingToolbar.show");
        Disposer.register(this, attempt);
        Disposer.register(attempt, () -> result.complete(false));
        myShowAttempt = attempt;

        DisposableCoroutineScope.launchAsync(
            myProject.coroutineContext(),
            attempt,
            () -> Coroutine
                .first(ReadLock.<Object, @Nullable CodeFloatingToolbarConfiguration>apply(ignored -> findConfiguration(selection)))
                .then(CompletableFutureStep.<@Nullable CodeFloatingToolbarConfiguration, @Nullable ActionGroup>await(this::createActionGroup))
                .then(UIAction.<@Nullable ActionGroup, Object>apply(group -> {
                    if (group != null && generation == myRequestGeneration && !Disposer.isDisposed(attempt)) {
                        createToolbar(group, selection, generation, result);
                    }
                    else {
                        result.complete(false);
                    }
                    return group;
                }))
        );
        return result;
    }

    @RequiredReadAction
    private @Nullable CodeFloatingToolbarConfiguration findConfiguration(CodeFloatingToolbarSelection selection) {
        if (myEditor.isDisposed() || myProject.isDisposed()) {
            return null;
        }

        return myApplication.getExtensionPoint(CodeFloatingToolbarProvider.class)
            .computeSafeIfAny(provider -> provider.getConfiguration(myEditor, selection));
    }

    private CompletableFuture<@Nullable ActionGroup> createActionGroup(@Nullable CodeFloatingToolbarConfiguration configuration) {
        if (configuration == null) {
            return CompletableFuture.completedFuture(null);
        }

        List<String> actionIds = configuration.actionIds();
        List<CompletableFuture<@Nullable AnAction>> futures = new ArrayList<>(actionIds.size());
        for (String actionId : actionIds) {
            futures.add(CustomActionsSchema.getCorrectedActionAsync(actionId));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(ignored -> {
            List<AnAction> actions = new ArrayList<>();
            for (CompletableFuture<@Nullable AnAction> future : futures) {
                AnAction action = future.join();
                if (action != null) {
                    actions.add(action);
                }
            }

            if (actions.isEmpty()) {
                return null;
            }

            if (!configuration.configurationsGroupHidden()) {
                MoreActionGroup configureGroup = new MoreActionGroup();
                configureGroup.setPopup(true);
                configureGroup.add(new CustomizeCodeFloatingToolbarAction());
                AnAction disableAction = myActionManager.getAction(CodeFloatingToolbarActions.DISABLE);
                if (disableAction != null) {
                    configureGroup.add(disableAction);
                }
                actions.add(configureGroup);
            }

            return new DefaultActionGroup(actions);
        });
    }

    @RequiredUIAccess
    private void createToolbar(
        ActionGroup group,
        CodeFloatingToolbarSelection selection,
        long generation,
        CompletableFuture<Boolean> result
    ) {
        ActionToolbar toolbar = myActionManager.createActionToolbar(ActionPlaces.EDITOR_FLOATING_TOOLBAR, group, true);
        toolbar.setDataContextProvider(myEditor::getDataContext);

        UIAccess uiAccess = UIAccess.current();
        toolbar.updateActionsAsync().whenCompleteAsync((actions, error) -> {
            if (error != null || actions == null || actions.isEmpty()) {
                result.complete(false);
                return;
            }

            if (generation != myRequestGeneration || Disposer.isDisposed(this) || isShown()) {
                result.complete(isShown());
                return;
            }

            result.complete(showToolbar(toolbar, actions, selection));
        }, uiAccess);
    }

    @RequiredUIAccess
    private boolean showToolbar(ActionToolbar toolbar, List<? extends AnAction> actions, CodeFloatingToolbarSelection selection) {
        CaretPixelLocation location = getCaretLocation();
        if (location == null) {
            return false;
        }

        boolean above = isOneLineSelection() || selection.caretOffset() != selection.end();

        LightPopup popup = LightPopup.create(
            PopupOptions.builder()
                .disableRequestFocus()
                .disableCancelOnClickOutside()
                .disableCancelOnEscape()
                .position(above ? PopupPosition.TOP : PopupPosition.BOTTOM)
                .build()
        );
        popup.setContent(toolbar.getUIComponent());
        popup.addCloseListener(event -> onPopupClosed(popup));

        myPopup = popup;
        myToolbar = toolbar;
        myLocationAbove = above;
        myVisibleActions = actions.stream().filter(action -> !(action instanceof AnSeparator)).map(AnAction.class::cast).toList();

        place(location);
        myProject.getMessageBus().syncPublisher(CodeFloatingToolbarListener.class).toolbarShown(myEditor);
        return true;
    }

    private boolean isOneLineSelection() {
        SelectionModel selectionModel = myEditor.getSelectionModel();
        Document document = myEditor.getDocument();
        return document.getLineNumber(selectionModel.getSelectionStart()) == document.getLineNumber(selectionModel.getSelectionEnd());
    }

    private @Nullable CaretPixelLocation getCaretLocation() {
        return myEditor instanceof CaretPixelLocationProvider provider ? provider.getCaretPixelLocation() : null;
    }

    @RequiredUIAccess
    private void place(CaretPixelLocation location) {
        LightPopup popup = myPopup;
        if (popup == null) {
            return;
        }

        boolean oneLine = isOneLineSelection();

        myLocation = location;
        myLocationArea = new Rectangle(myEditor.getScrollingModel().getVisibleArea());

        int x = oneLine ? location.x() : location.textX();
        popup.showAt(myEditor.getContentUIComponent(), x, location.y(), location.height());
    }

    @RequiredUIAccess
    private void relocate() {
        if (!isShown()) {
            return;
        }

        CaretPixelLocation location = getCaretLocation();
        if (location == null) {
            hide();
            return;
        }

        boolean oneLine = isOneLineSelection();
        boolean above = oneLine || myEditor.getCaretModel().getOffset() != myEditor.getSelectionModel().getSelectionEnd();
        if (above != myLocationAbove) {
            hide();
            myPreventShowing = false;
            scheduleShow();
            return;
        }

        place(location);
    }

    @RequiredUIAccess
    private void scrolled(VisibleAreaEvent event) {
        LightPopup popup = myPopup;
        CaretPixelLocation location = myLocation;
        Rectangle locationArea = myLocationArea;
        if (popup == null || location == null || locationArea == null) {
            return;
        }

        Rectangle visibleArea = event.getNewRectangle();
        boolean scrolledWithComponent = myEditor instanceof CaretPixelLocationProvider provider && provider.isLocationScrolledWithComponent();
        if (scrolledWithComponent) {
            if (location.y() + location.height() <= visibleArea.y || location.y() >= visibleArea.y + visibleArea.height) {
                hide();
            }
            return;
        }

        int x = location.x() - (visibleArea.x - locationArea.x);
        int y = location.y() - (visibleArea.y - locationArea.y);
        if (y + location.height() <= 0 || y >= visibleArea.height) {
            hide();
            return;
        }

        place(new CaretPixelLocation(x, y, location.height(), location.textX()));
    }

    @RequiredUIAccess
    private void hide() {
        cancelShowAttempt();

        LightPopup popup = myPopup;
        if (popup != null) {
            popup.close();
        }
    }

    @RequiredUIAccess
    private void onPopupClosed(LightPopup popup) {
        if (myPopup != popup) {
            return;
        }

        myPopup = null;
        myLocation = null;
        myLocationArea = null;
        myToolbar = null;
        myVisibleActions = List.of();
        cancelActivePopup();
    }

    private void cancelShowAttempt() {
        Disposable attempt = myShowAttempt;
        if (attempt != null) {
            myShowAttempt = null;
            Disposer.dispose(attempt);
        }
    }

    @Override
    @RequiredUIAccess
    public void hideWhilePopupVisible(JBPopup popup) {
        Runnable release = disableWhileShown();
        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(LightweightWindowEvent event) {
                release.run();
            }
        });
    }

    @Override
    @RequiredUIAccess
    public void hideWhilePopupVisible(Popup popup) {
        Runnable release = disableWhileShown();
        popup.addCloseListener(event -> release.run());
    }

    @RequiredUIAccess
    private Runnable disableWhileShown() {
        boolean wasVisible = isShown();
        myDisabledCount++;
        hide();

        boolean[] released = new boolean[1];
        return () -> {
            if (released[0]) {
                return;
            }
            released[0] = true;
            myDisabledCount--;
            if (wasVisible) {
                allowInstantShowing();
            }
        };
    }

    @Override
    @RequiredUIAccess
    public void attachPopup(AnAction action, JBPopup popup) {
        JBPopup previous = myActivePopup;
        if (previous != null && previous != popup) {
            previous.cancel();
        }

        myActivePopup = popup;
        myActivePopupAction = action;
        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(LightweightWindowEvent event) {
                if (myActivePopup == popup) {
                    myActivePopup = null;
                    myActivePopupAction = null;
                }
            }
        });
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<Boolean> showAsync() {
        myRequestGeneration++;
        myPreventShowing = false;
        return showIfHidden(new CompletableFuture<>());
    }

    @Override
    @RequiredUIAccess
    public boolean showPopupUnderneath(AnAction action, JBPopup popup) {
        LightPopup toolbarPopup = myPopup;
        ActionToolbar toolbar = myToolbar;
        if (toolbarPopup == null || toolbar == null) {
            return false;
        }

        attachPopup(action, popup);
        showUnderneathWhenVisible(toolbarPopup, toolbar, popup, SHOW_UNDERNEATH_ATTEMPTS);
        return true;
    }

    @RequiredUIAccess
    private void showUnderneathWhenVisible(LightPopup toolbarPopup, ActionToolbar toolbar, JBPopup popup, int attempts) {
        if (myPopup != toolbarPopup || popup.isDisposed()) {
            return;
        }

        if (toolbarPopup.isVisible() || attempts <= 0) {
            popup.showBy(toolbar.getUIComponent(), null, PopupPosition.BOTTOM);
            return;
        }

        UIAccess.current().getScheduler().schedule(
            () -> showUnderneathWhenVisible(toolbarPopup, toolbar, popup, attempts - 1),
            ModalityState.any(),
            SHOW_UNDERNEATH_DELAY_MS,
            TimeUnit.MILLISECONDS
        );
    }

    @Override
    @RequiredUIAccess
    public void cancelActivePopup() {
        JBPopup popup = myActivePopup;
        myActivePopup = null;
        myActivePopupAction = null;
        if (popup != null) {
            popup.cancel();
        }
    }

    @Override
    @RequiredUIAccess
    public @Nullable AnAction getNextMenuAction(boolean forward) {
        List<AnAction> actions = myVisibleActions;
        AnAction current = myActivePopupAction;
        if (!isShown() || actions.isEmpty() || current == null) {
            return null;
        }

        int index = actions.indexOf(current);
        if (index < 0) {
            return null;
        }

        int next = Math.floorMod(index + (forward ? 1 : -1), actions.size());
        return actions.get(next);
    }

    @RequiredUIAccess
    public @Nullable Popup getPopup() {
        return isShown() ? Objects.requireNonNull(myPopup) : null;
    }

    @Override
    public void dispose() {
        cancelShowAttempt();
        LightPopup popup = myPopup;
        myPopup = null;
        if (popup != null) {
            UIAccess.assertIsUIThread();
            popup.close();
        }
    }
}
