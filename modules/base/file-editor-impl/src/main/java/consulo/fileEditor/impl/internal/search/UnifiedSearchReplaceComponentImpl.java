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
package consulo.fileEditor.impl.internal.search;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.fileEditor.internal.SearchReplaceComponent;
import consulo.find.FindInProjectSettings;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.proxy.EventDispatcher;
import consulo.ui.Component;
import consulo.ui.HasFocus;
import consulo.ui.Space;
import consulo.ui.TextArea;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.action.*;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.layout.DockLayout;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StandardColors;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-02
 */
public class UnifiedSearchReplaceComponentImpl implements SearchReplaceComponent {
    private static final int FIELD_COLUMNS = 40;

    private final EventDispatcher<Listener> myEventDispatcher = EventDispatcher.create(Listener.class);

    private final @Nullable Project myProject;
    private final Component myTargetComponent;

    private final @Nullable Runnable myCloseAction;
    private final @Nullable Runnable myReplaceAction;

    private final @Nullable UiDataProvider myDataProviderDelegate;

    private final DefaultActionGroup mySearchFieldActions;
    private final DefaultActionGroup myReplaceFieldActions;

    private final List<AnAction> mySearchSuffixActions = new ArrayList<>();
    private final List<AnAction> myReplaceSuffixActions = new ArrayList<>();

    private final DockLayout myRootLayout;
    private final DockLayout myReplaceRow;

    private final UnifiedSearchTextArea mySearchArea;
    private final UnifiedSearchTextArea myReplaceArea;

    private final TextArea mySearchField;
    private final TextArea myReplaceField;

    private final ActionGroup mySearchToolbarGroup;
    private final ActionToolbar mySearchActionsToolbar;

    private final ActionGroup myReplaceToolbarGroup;
    private final ActionToolbar myReplaceActionsToolbar;

    private final ActionToolbar myCloseToolbar;

    private boolean myMultilineMode;
    private String myStatusText = "";
    private @Nullable ColorValue myStatusColor;

    @RequiredUIAccess
    public UnifiedSearchReplaceComponentImpl(
        @Nullable Project project,
        Component targetComponent,
        DefaultActionGroup searchToolbar1Actions,
        DefaultActionGroup searchToolbar2Actions,
        DefaultActionGroup searchFieldActions,
        DefaultActionGroup replaceToolbar1Actions,
        DefaultActionGroup replaceToolbar2Actions,
        DefaultActionGroup replaceFieldActions,
        @Nullable Runnable replaceAction,
        @Nullable Runnable closeAction,
        @Nullable UiDataProvider dataProvider
    ) {
        myProject = project;
        myTargetComponent = targetComponent;
        mySearchFieldActions = searchFieldActions;
        myReplaceFieldActions = replaceFieldActions;
        myReplaceAction = replaceAction;
        myCloseAction = closeAction;

        moveEmbeddableActions(searchToolbar2Actions, mySearchSuffixActions);
        moveEmbeddableActions(replaceToolbar2Actions, myReplaceSuffixActions);

        searchToolbar1Actions.addAll(searchToolbar2Actions.getChildren(null));
        replaceToolbar1Actions.addAll(replaceToolbar2Actions.getChildren(null));

        myRootLayout = DockLayout.create(Space.NONE);

        mySearchArea = createField(true, mySearchSuffixActions);
        myReplaceArea = createField(false, myReplaceSuffixActions);

        mySearchField = mySearchArea.getTextArea();
        myReplaceField = myReplaceArea.getTextArea();

        mySearchField.borderBuilder().rightSet().apply();
        myReplaceField.borderBuilder().topSet().rightSet().apply();

        mySearchToolbarGroup = createSearchToolbarGroup(searchToolbar1Actions);
        mySearchActionsToolbar = createToolbar(ActionPlaces.EDITOR_TOOLBAR, mySearchToolbarGroup, ActionToolbar.Style.HORIZONTAL);

        myReplaceToolbarGroup = replaceToolbar1Actions;
        myReplaceActionsToolbar = createToolbar(ActionPlaces.EDITOR_TOOLBAR, myReplaceToolbarGroup, ActionToolbar.Style.HORIZONTAL);

        myCloseToolbar = createToolbar(
            "SearchCloseGroup",
            ActionGroup.newImmutableBuilder().add(new SearchCloseAction(this::close)).build(),
            ActionToolbar.Style.HORIZONTAL
        );

        DockLayout searchRow = DockLayout.create(Space.NONE);
        searchRow.left(mySearchField);
        searchRow.center(mySearchActionsToolbar.getUIComponent());
        searchRow.right(myCloseToolbar.getUIComponent());

        myReplaceRow = DockLayout.create(Space.NONE);
        myReplaceRow.left(myReplaceField);
        myReplaceRow.center(myReplaceActionsToolbar.getUIComponent());

        myRootLayout.top(searchRow);
        myRootLayout.center(myReplaceRow);
        myRootLayout.setBackgroundColor(ComponentColors.LAYOUT);
        myRootLayout.borderBuilder().bottomSet().apply();
        myRootLayout.putUserData(UiDataProvider.KEY, this);

        myRootLayout.addAttachListener(event -> myEventDispatcher.getMulticaster().componentShown());
        myRootLayout.addDetachListener(event -> myEventDispatcher.getMulticaster().componentHidden());

        installKeyActions();

        update("", "", false, false);

        myDataProviderDelegate = dataProvider;
    }

    private static void moveEmbeddableActions(DefaultActionGroup group, List<AnAction> target) {
        for (AnAction action : group.getChildren(null)) {
            if (action instanceof Embeddable) {
                target.add(action);

                group.remove(action);
            }
        }
    }

    @RequiredUIAccess
    private UnifiedSearchTextArea createField(boolean search, List<AnAction> suffixActions) {
        UnifiedSearchTextArea area = new UnifiedSearchTextArea(myProject, search, myRootLayout);
        area.getTextArea().setVisibleLength(FIELD_COLUMNS);
        area.setSuffixActions(suffixActions);

        area.getTextArea().addValueListener(event -> UIAccess.current().give(() -> {
            if (search) {
                myEventDispatcher.getMulticaster().searchFieldDocumentChanged();
            }
            else {
                myEventDispatcher.getMulticaster().replaceFieldDocumentChanged();
            }
        }));
        return area;
    }

    private ActionGroup createSearchToolbarGroup(DefaultActionGroup group) {
        ActionGroup.Builder toolbarGroup = ActionGroup.newImmutableBuilder();

        ContextFilterActionGroup contextGroup = new ContextFilterActionGroup();

        for (AnAction action : group.getChildren(null)) {
            if (action instanceof EditorHeaderSetSearchContextAction) {
                contextGroup.add(action);
            }
            else {
                toolbarGroup.add(action);
            }
        }

        toolbarGroup.add(contextGroup);
        return toolbarGroup.build();
    }

    private ActionToolbar createToolbar(String place, ActionGroup group, ActionToolbar.Style style) {
        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar(place, group, style);
        toolbar.setTargetUIComponent(myRootLayout);
        if (style == ActionToolbar.Style.HORIZONTAL) {
            toolbar.getUIComponent().paddingBuilder().topReset().bottomReset().apply();
        }
        return toolbar;
    }

    @RequiredUIAccess
    private void installKeyActions() {
        DumbAwareAction closeAction = DumbAwareAction.create(e -> close());
        ShortcutSet escape = KeymapUtil.getActiveKeymapShortcuts(IdeActions.ACTION_EDITOR_ESCAPE);
        closeAction.registerCustomShortcutSet(escape, mySearchField);
        closeAction.registerCustomShortcutSet(escape, myReplaceField);

        int menuModifier = Platform.current().os().isMac() ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK;
        DumbAwareAction.create(e -> {
            String text = getSearchText();
            if (text.isEmpty()) {
                close();
            }
            else {
                focusTarget();
                addTextToRecent(text, true);
            }
        }).registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, menuModifier)), mySearchField);

        DumbAwareAction.create(e -> replace())
            .registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)), myReplaceField);
    }

    @RequiredUIAccess
    private void focusTarget() {
        if (myTargetComponent instanceof HasFocus hasFocus) {
            hasFocus.focus();
        }
    }

    @RequiredUIAccess
    private void updateBindings() {
        updateBindings(mySearchFieldActions.getChildren(null), mySearchField);
        updateBindings(mySearchToolbarGroup.getChildren(null), mySearchField);

        updateBindings(myReplaceFieldActions.getChildren(null), myReplaceField);
        updateBindings(myReplaceToolbarGroup.getChildren(null), myReplaceField);
    }

    @RequiredUIAccess
    private void updateBindings(AnAction[] actions, Component shortcutHolder) {
        DataContext context = DataManager.getInstance().getDataContext(myRootLayout);
        for (AnAction action : actions) {
            ShortcutSet shortcut = null;
            if (action instanceof ContextAwareShortcutProvider contextAwareShortcutProvider) {
                shortcut = contextAwareShortcutProvider.getShortcut(context);
            }
            else if (action instanceof ShortcutProvider shortcutProvider) {
                shortcut = shortcutProvider.getShortcut();
            }

            if (shortcut != null) {
                action.registerCustomShortcutSet(shortcut, shortcutHolder);
            }
        }
    }

    @RequiredUIAccess
    private void close() {
        if (myCloseAction != null) {
            myCloseAction.run();
        }
    }

    @RequiredUIAccess
    private void replace() {
        if (myReplaceAction != null) {
            myReplaceAction.run();
        }
    }

    @Override
    public void uiDataSnapshot(DataSink sink) {
        UiDataProvider dataProvider = myDataProviderDelegate;
        if (dataProvider != null) {
            sink.uiDataSnapshot(dataProvider);
        }
    }

    @Override
    @RequiredUIAccess
    public void setRegularBackground() {
        mySearchField.setBackgroundColor(ComponentColors.COMPONENT_BACKGROUND);
        myStatusColor = null;
    }

    @Override
    @RequiredUIAccess
    public void setNotFoundBackground() {
        mySearchField.setBackgroundColor(StandardColors.LIGHT_RED);
        myStatusColor = ComponentColors.ERROR_FOREGROUND;
    }

    @Override
    public Component getUIComponent() {
        return myRootLayout;
    }

    @Override
    public void setStatusText(String status) {
        myStatusText = status;
    }

    @Override
    public String getStatusText() {
        return myStatusText;
    }

    @Override
    public @Nullable ColorValue getStatusColor() {
        return myStatusColor;
    }

    @Override
    public void resetUndoRedoActions() {
    }

    @Override
    @RequiredUIAccess
    public void updateActions() {
        mySearchActionsToolbar.updateActionsAsync();
        myReplaceActionsToolbar.updateActionsAsync();

        mySearchArea.updateAllAsync();
        myReplaceArea.updateAllAsync();
    }

    @Override
    public boolean isMultiline() {
        return myMultilineMode;
    }

    @RequiredUIAccess
    private void setMultilineInternal(boolean multiline) {
        boolean stateChanged = multiline != myMultilineMode;
        myMultilineMode = multiline;
        if (stateChanged) {
            myEventDispatcher.getMulticaster().multilineStateChanged();
        }
    }

    @Override
    public void addListener(Listener listener) {
        myEventDispatcher.addListener(listener);
    }

    @Override
    public @Nullable Project getProject() {
        return myProject;
    }

    @Override
    public String getSearchText() {
        return StringUtil.notNullize(mySearchField.getValue());
    }

    @Override
    @RequiredUIAccess
    public void setSearchText(String text) {
        mySearchField.setValue(text);
    }

    @Override
    public String getReplaceText() {
        return StringUtil.notNullize(myReplaceField.getValue());
    }

    @Override
    @RequiredUIAccess
    public void setReplaceText(String text) {
        myReplaceField.setValue(text);
    }

    @Override
    @RequiredUIAccess
    public void update(String findText, String replaceText, boolean replaceMode, boolean multiline) {
        setMultilineInternal(multiline);

        replaceTextEnsuringSelection(mySearchField, findText);
        replaceTextEnsuringSelection(myReplaceField, replaceText);

        myReplaceRow.setVisible(replaceMode);

        updateBindings();
        updateActions();
    }

    @RequiredUIAccess
    private static void replaceTextEnsuringSelection(TextArea field, String text) {
        if (!Objects.equals(field.getValue(), text)) {
            field.setValue(text);
            field.selectAll();
        }
    }

    @Override
    @RequiredUIAccess
    public void selectSearchAll() {
        mySearchField.selectAll();
    }

    @Override
    @RequiredUIAccess
    public void requestFocusInTheSearchFieldAndSelectContent(Project project) {
        mySearchField.selectAll();
        mySearchField.focus();
        myReplaceField.selectAll();
    }

    @Override
    public void addTextToRecent(String text, boolean search) {
        Project project = myProject;
        if (text.isEmpty() || project == null) {
            return;
        }

        FindInProjectSettings findInProjectSettings = FindInProjectSettings.getInstance(project);
        if (search) {
            findInProjectSettings.addStringToFind(text);
        }
        else {
            findInProjectSettings.addStringToReplace(text);
        }
    }

    @Override
    @RequiredUIAccess
    public void updateEmptyText(Supplier<String> textSupplier) {
        mySearchField.setPlaceholder(LocalizeValue.of(textSupplier.get()));
    }

    @Override
    public boolean isJustClearedSearch() {
        return mySearchArea.isJustCleared();
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<?> prepareAsync() {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        futures.add(mySearchActionsToolbar.updateActionsAsync());
        futures.add(myReplaceActionsToolbar.updateActionsAsync());
        futures.add(myCloseToolbar.updateActionsAsync());
        futures.add(mySearchArea.updateAllAsync());
        futures.add(myReplaceArea.updateAllAsync());
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }
}
