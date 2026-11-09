/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.ui.customization;

import consulo.application.Application;
import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.KeymapGroupImpl;
import consulo.ui.ex.keymap.localize.KeyMapLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.TextBox;
import consulo.ui.TextBoxStyle;
import consulo.ui.TextItemPresentation;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.clipboard.DataTransferType;
import consulo.ui.event.ContextMenuEvent;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static consulo.ide.impl.idea.ide.ui.customization.ActionUrl.ADDED;
import static consulo.ide.impl.idea.ide.ui.customization.ActionUrl.DELETED;
import static consulo.ide.impl.idea.ide.ui.customization.ActionUrl.MOVE;

/**
 * @author anna
 * @since 2005-03-17
 */
public class CustomizableActionsPanel {
    private static final Logger LOG = Logger.getInstance(CustomizableActionsPanel.class);

    private final KeymapGroupImpl myRoot = new KeymapGroupImpl("root");
    private List<KeymapGroupImpl> myDefaultGroups = List.of();

    /**
     * The application schema, resolved once by {@link #reset()}. It loads asynchronously, and the sync
     * {@code Configurable} methods below cannot wait for it.
     */
    private @Nullable CustomActionsSchemaImpl myApplicationSchema;
    private @Nullable CustomActionsSchemaImpl mySelectedSchema;

    private final Tree<CustomizationTreeItem> myTree;
    private final ActionToolbar myToolbar;
    private final Button myAddActionButton;
    private final DockLayout myLayout;

    private final EditIconAction myEditIconAction = new EditIconAction();
    private final RemoveAction myRemoveAction = new RemoveAction();
    private final MoveAction myMoveUpAction = new MoveAction(-1);
    private final MoveAction myMoveDownAction = new MoveAction(1);

    private int myPatchEpoch;

    @RequiredUIAccess
    public CustomizableActionsPanel(Disposable uiDisposable) {
        TreeModel<CustomizationTreeItem> model = (nodeFactory, parentValue) -> {
            KeymapGroupImpl group = parentValue == null ? myRoot : parentValue.getGroup();
            if (group == null) {
                return;
            }
            for (CustomizationTreeItem item : CustomizationTreeItem.children(group)) {
                TreeNode<CustomizationTreeItem> node = nodeFactory.apply(item);
                node.setLeaf(item.getGroup() == null);
                node.setRenderer(this::render);
            }
        };

        myTree = Tree.create(model);
        Disposer.register(uiDisposable, myTree.destroyHook());
        myTree.setAllowMultipleSelect(true);
        myTree.setSpeedSearchConverter(node -> {
            CustomizationTreeItem item = node.getValue();
            return item == null ? "" : CustomizationUtil.getText(item.getComponent());
        });
        myTree.addSelectListener(event -> updateActions());
        myTree.addContextMenuListener(this::showContextMenu);
        myTree.setTransferHandler(new MoveTransferHandler());

        myEditIconAction.registerCustomShortcutSet(CommonShortcuts.getEditSource(), myTree, uiDisposable);
        myRemoveAction.registerCustomShortcutSet(CommonShortcuts.getDelete(), myTree, uiDisposable);
        myMoveUpAction.registerCustomShortcutSet(CommonShortcuts.MOVE_UP, myTree, uiDisposable);
        myMoveDownAction.registerCustomShortcutSet(CommonShortcuts.MOVE_DOWN, myTree, uiDisposable);

        DefaultActionGroup restoreGroup = new DefaultActionGroup(KeyMapLocalize.groupCustomizationsRestoreActionGroup(), true);
        restoreGroup.getTemplatePresentation().setIcon(PlatformIconGroup.actionsRollback());
        restoreGroup.add(new RestoreSelectionAction());
        restoreGroup.add(new RestoreAllAction());

        DefaultActionGroup toolbarGroup = new DefaultActionGroup();
        toolbarGroup.add(myEditIconAction);
        toolbarGroup.add(myMoveUpAction);
        toolbarGroup.add(myMoveDownAction);
        toolbarGroup.addSeparator();
        toolbarGroup.add(myRemoveAction);
        toolbarGroup.add(restoreGroup);

        myToolbar = ActionToolbarFactory.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, toolbarGroup, ActionToolbar.Style.HORIZONTAL);
        myToolbar.setTargetUIComponent(myTree);

        myAddActionButton = Button.create(KeyMapLocalize.groupCustomizationsAddActionButton(), event -> addActions());

        TextBox filter = TextBox.create();
        filter.addStyle(TextBoxStyle.HAS_CLEAR_BUTTON);
        Label searchIcon = Label.create();
        searchIcon.setImage(PlatformIconGroup.actionsSearch());
        filter.setPrefixComponent(searchIcon);
        filter.addValueListener(event -> selectFirstMatch(myTree, myRoot, StringUtil.notNullize(event.getValue())));

        HorizontalLayout actionsPanel = HorizontalLayout.create(Space.SMALL);
        actionsPanel.add(myAddActionButton);
        actionsPanel.add(myToolbar.getUIComponent());

        DockLayout topPanel = DockLayout.create(Space.MEDIUM);
        topPanel.left(actionsPanel);
        topPanel.right(filter);

        myLayout = DockLayout.create(Space.MEDIUM);
        myLayout.top(topPanel);
        myLayout.center(ScrollableLayout.create(myTree));

        updateActions();
    }

    public Component getComponent() {
        return myLayout;
    }

    @RequiredUIAccess
    public void reset() {
        Application application = Application.get();
        UIAccess uiAccess = UIAccess.current();

        CustomActionsSchemaImpl.getInstanceAsync().whenCompleteAsync((schema, throwable) -> {
            if (throwable != null) {
                LOG.error("Failed to load the customization schema", throwable);
                return;
            }

            myApplicationSchema = schema;

            CustomActionsSchemaImpl selectedSchema = new CustomActionsSchemaImpl(application);
            selectedSchema.copyFrom(schema);
            mySelectedSchema = selectedSchema;
            patchActionsTreeCorrespondingToSchema();
        }, uiAccess);
    }

    public boolean isModified() {
        CustomActionsSchemaImpl applicationSchema = myApplicationSchema;
        CustomActionsSchemaImpl selectedSchema = mySelectedSchema;
        if (applicationSchema == null || selectedSchema == null) {
            return false;
        }
        CustomizationUtil.optimizeSchema(myRoot, selectedSchema, myDefaultGroups);
        return applicationSchema.isModified(selectedSchema);
    }

    @RequiredUIAccess
    public void apply() throws ConfigurationException {
        CustomActionsSchemaImpl applicationSchema = myApplicationSchema;
        CustomActionsSchemaImpl selectedSchema = mySelectedSchema;
        if (applicationSchema == null || selectedSchema == null) {
            return;
        }

        CustomizationUtil.optimizeSchema(myRoot, selectedSchema, myDefaultGroups);
        applicationSchema.copyFrom(selectedSchema);
        applicationSchema.initActionIcons();
        setCustomizationSchemaForCurrentProjects();
    }

    @RequiredUIAccess
    private CompletableFuture<?> patchActionsTreeCorrespondingToSchema() {
        CustomActionsSchemaImpl schema = mySelectedSchema;
        if (schema == null) {
            return CompletableFuture.completedFuture(null);
        }

        int epoch = ++myPatchEpoch;
        List<List<String>> expandedPaths = collectExpandedPaths();
        UIAccess uiAccess = UIAccess.current();

        return schema.createActionGroupsAsync()
            .thenCompose(groups -> schema.createActionGroupsAsync().thenApply(defaultGroups -> {
                if (epoch == myPatchEpoch) {
                    myDefaultGroups = defaultGroups;
                }
                return groups;
            }))
            .thenCompose(groups -> resolveActionUrlsAsync(schema).thenApply(ignored -> groups))
            .thenComposeAsync(groups -> {
                if (epoch != myPatchEpoch) {
                    return CompletableFuture.completedFuture(null);
                }
                while (myRoot.getSize() > 0) {
                    myRoot.removeChild(myRoot.getSize() - 1);
                }
                for (KeymapGroupImpl group : groups) {
                    myRoot.addGroup(group);
                }
                for (ActionUrl actionUrl : schema.getActions()) {
                    ActionUrl.changePathInActionsTree(myRoot, actionUrl);
                }
                return myTree.refreshAll().thenCompose(ignored -> restoreExpandedPaths(expandedPaths));
            }, uiAccess)
            .whenCompleteAsync((ignored, throwable) -> {
                if (throwable != null) {
                    LOG.error("Failed to build the customization tree", throwable);
                }
                updateActions();
            }, uiAccess);
    }

    private static CompletableFuture<?> resolveActionUrlsAsync(CustomActionsSchemaImpl schema) {
        CompletableFuture<?> chain = CompletableFuture.completedFuture(null);
        for (ActionUrl actionUrl : schema.getActions()) {
            chain = chain.thenCompose(ignored -> actionUrl.resolveComponentAsync());
        }
        return chain;
    }

    private List<List<String>> collectExpandedPaths() {
        List<List<String>> result = new ArrayList<>();
        for (List<TreeNode<CustomizationTreeItem>> path : myTree.getExpandedPaths()) {
            List<String> names = new ArrayList<>();
            for (TreeNode<CustomizationTreeItem> node : path) {
                CustomizationTreeItem item = node.getValue();
                KeymapGroupImpl group = item != null ? item.getGroup() : null;
                if (group != null) {
                    names.add(group.getName());
                }
            }
            if (!names.isEmpty()) {
                result.add(names);
            }
        }
        return result;
    }

    @RequiredUIAccess
    private CompletableFuture<?> restoreExpandedPaths(List<List<String>> paths) {
        TreeNode<CustomizationTreeItem> rootNode = myTree.getRootNode();
        if (rootNode == null) {
            return CompletableFuture.completedFuture(null);
        }

        UIAccess uiAccess = UIAccess.current();
        CompletableFuture<?> all = CompletableFuture.completedFuture(null);
        for (List<String> path : paths) {
            CompletableFuture<TreeNode<CustomizationTreeItem>> chain = CompletableFuture.completedFuture(rootNode);
            for (String name : path) {
                chain = chain.thenCompose(node -> node == null
                    ? CompletableFuture.completedFuture(null)
                    : node.findChild(value -> value != null && value.getGroup() != null && name.equals(value.getGroup().getName())));
            }
            CompletableFuture<?> expanded = chain.thenComposeAsync(
                node -> node == null ? CompletableFuture.completedFuture(null) : myTree.expand(node),
                uiAccess
            );
            all = CompletableFuture.allOf(all, expanded);
        }
        return all;
    }

    @RequiredUIAccess
    private void refreshAndSelect(@Nullable CustomizationTreeItem toSelect) {
        UIAccess uiAccess = UIAccess.current();
        myTree.refreshAll().whenCompleteAsync((ignored, error) -> {
            if (toSelect != null) {
                CustomizationUtil.selectItem(myTree, toSelect);
            }
            updateActions();
        }, uiAccess);
    }

    @RequiredUIAccess
    private void updateActions() {
        myAddActionButton.setEnabled(isAddApplicable(getSelectedItems()));
        myToolbar.updateActionsAsync();
    }

    private void render(CustomizationTreeItem item, TextItemPresentation presentation) {
        CustomizationUtil.customizePresentation(item.getComponent(), presentation, !item.isTopLevel(), this::getActionIcon);
    }

    private @Nullable Image getActionIcon(String actionId) {
        CustomActionsSchemaImpl applicationSchema = myApplicationSchema;
        CustomActionsSchemaImpl selectedSchema = mySelectedSchema;
        if (applicationSchema == null || selectedSchema == null) {
            return null;
        }
        return selectedSchema.getActionIcon(actionId, applicationSchema);
    }

    private List<CustomizationTreeItem> getSelectedItems() {
        List<CustomizationTreeItem> result = new ArrayList<>();
        for (TreeNode<CustomizationTreeItem> node : myTree.getSelectedNodes()) {
            CustomizationTreeItem item = node.getValue();
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    private @Nullable CustomizationTreeItem getLeadSelectedItem() {
        TreeNode<CustomizationTreeItem> node = myTree.getSelectedNode();
        return node != null ? node.getValue() : null;
    }

    private static boolean isUnderCustomizationRoot(List<CustomizationTreeItem> items) {
        if (items.isEmpty()) {
            return false;
        }
        for (CustomizationTreeItem item : items) {
            if (item.isTopLevel()) {
                return false;
            }
        }
        return true;
    }

    private boolean isAddApplicable(List<CustomizationTreeItem> items) {
        return mySelectedSchema != null && items.size() == 1;
    }

    private static boolean isMoveSupported(List<CustomizationTreeItem> items, int dir) {
        if (!isUnderCustomizationRoot(items)) {
            return false;
        }
        KeymapGroupImpl parent = items.get(0).getParent();
        for (CustomizationTreeItem item : items) {
            if (item.getParent() != parent) {
                return false;
            }
            int index = item.getIndex();
            if (dir > 0 ? index == parent.getSize() - 1 : index == 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isInsideMenu(CustomizationTreeItem item) {
        KeymapGroupImpl topLevelGroup = item.isTopLevel() ? item.getGroup() : item.getParent();
        while (topLevelGroup != null && topLevelGroup.getParent() != null && topLevelGroup.getParent().getParent() != null) {
            topLevelGroup = topLevelGroup.getParent();
        }
        String groupId = topLevelGroup != null ? topLevelGroup.getId() : null;
        return groupId != null && groupId.contains("Menu");
    }

    private void addCustomizedAction(ActionUrl url) {
        CustomActionsSchemaImpl schema = mySelectedSchema;
        if (schema != null) {
            schema.addAction(url);
        }
    }

    @RequiredUIAccess
    private void addActions() {
        CustomActionsSchemaImpl schema = mySelectedSchema;
        CustomActionsSchemaImpl applicationSchema = myApplicationSchema;
        CustomizationTreeItem item = getLeadSelectedItem();
        if (schema == null || applicationSchema == null || item == null) {
            return;
        }

        UIAccess uiAccess = UIAccess.current();
        AddActionDialogDescriptor.showAsync(myLayout, schema, applicationSchema, isInsideMenu(item)).whenCompleteAsync((addedActions, error) -> {
            if (error != null || addedActions.isEmpty()) {
                if (error == null) {
                    refreshAndSelect(item);
                }
                return;
            }

            KeymapGroupImpl selectedGroup = item.getGroup();
            CustomizationTreeItem lastAdded = null;
            for (int ind = 0; ind < addedActions.size(); ind++) {
                Object action = addedActions.get(ind);
                KeymapGroupImpl parent = selectedGroup != null ? selectedGroup : item.getParent();
                int position = selectedGroup != null ? selectedGroup.getSize() : item.getIndex() + ind + 1;
                ActionUrl url = new ActionUrl(CustomizationUtil.getGroupPath(parent), action, ADDED, position);
                addCustomizedAction(url);
                if (ActionUrl.changePathInActionsTree(myRoot, url) != null) {
                    lastAdded = CustomizationTreeItem.at(parent, position);
                }
            }
            refreshAndSelect(lastAdded != null ? lastAdded : item);
        }, uiAccess);
    }

    @RequiredUIAccess
    private void addSeparator() {
        CustomizationTreeItem item = getLeadSelectedItem();
        if (item == null) {
            return;
        }
        int position = item.getIndex() + 1;
        ActionUrl url = new ActionUrl(item.getGroupPath(), AnSeparator.getInstance(), ADDED, position);
        ActionUrl.changePathInActionsTree(myRoot, url);
        addCustomizedAction(url);
        refreshAndSelect(CustomizationTreeItem.at(item.getParent(), position));
    }

    @RequiredUIAccess
    private void removeItems(List<CustomizationTreeItem> items) {
        List<CustomizationTreeItem> sorted = new ArrayList<>(items);
        sorted.sort(Comparator.comparingInt(CustomizationTreeItem::getIndex).reversed());
        for (CustomizationTreeItem item : sorted) {
            ActionUrl url = CustomizationUtil.getActionUrl(item, DELETED);
            ActionUrl.changePathInActionsTree(myRoot, url);
            addCustomizedAction(url);
        }
        refreshAndSelect(null);
    }

    @RequiredUIAccess
    private void moveItems(List<CustomizationTreeItem> items, int dir) {
        List<CustomizationTreeItem> sorted = new ArrayList<>(items);
        Comparator<CustomizationTreeItem> byIndex = Comparator.comparingInt(CustomizationTreeItem::getIndex);
        sorted.sort(dir > 0 ? byIndex.reversed() : byIndex);

        KeymapGroupImpl parent = sorted.get(0).getParent();
        int leadIndex = -1;
        CustomizationTreeItem lead = getLeadSelectedItem();
        for (CustomizationTreeItem item : sorted) {
            ActionUrl url = CustomizationUtil.getActionUrl(item, MOVE);
            int absolutePosition = url.getAbsolutePosition();
            url.setInitialPosition(absolutePosition);
            url.setAbsolutePosition(absolutePosition + dir);
            ActionUrl.changePathInActionsTree(myRoot, url);
            addCustomizedAction(url);
            if (item.equals(lead) || leadIndex < 0) {
                leadIndex = absolutePosition + dir;
            }
        }
        refreshAndSelect(CustomizationTreeItem.at(parent, leadIndex));
    }

    @RequiredUIAccess
    private void editIcon(CustomizationTreeItem item) {
        CustomActionsSchemaImpl schema = mySelectedSchema;
        CustomActionsSchemaImpl applicationSchema = myApplicationSchema;
        String actionId = item.getActionId();
        if (schema == null || applicationSchema == null || actionId == null) {
            return;
        }

        UIAccess uiAccess = UIAccess.current();
        EditIconDialogDescriptor.showAsync(myLayout, schema, applicationSchema, actionId, isInsideMenu(item))
            .whenCompleteAsync((changed, error) -> {
                if (error == null && changed) {
                    refreshAndSelect(item);
                }
            }, uiAccess);
    }

    private Set<String> findSelectedGroupNames(List<CustomizationTreeItem> items) {
        Set<String> names = new LinkedHashSet<>();
        for (CustomizationTreeItem item : items) {
            KeymapGroupImpl group = item.getGroup();
            if (group != null) {
                names.add(group.getName());
            }
        }
        return names;
    }

    private List<ActionUrl> findActionsUnderSelection(List<CustomizationTreeItem> items) {
        CustomActionsSchemaImpl schema = mySelectedSchema;
        List<ActionUrl> actions = new ArrayList<>();
        if (schema == null) {
            return actions;
        }
        for (CustomizationTreeItem item : items) {
            KeymapGroupImpl group = item.getGroup();
            if (group == null) {
                continue;
            }
            List<String> selectedGroupPath = CustomizationUtil.getGroupPath(group);
            for (ActionUrl action : schema.getActions()) {
                if (Collections.indexOfSubList(action.getGroupPath(), selectedGroupPath) > -1) {
                    actions.add(action);
                }
            }
        }
        return actions;
    }

    @RequiredUIAccess
    private void showContextMenu(ContextMenuEvent event) {
        ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
        group.add(myEditIconAction);
        group.add(myRemoveAction);
        group.addSeparator();
        group.add(new AddActionBelowSelectionAction());
        group.add(new AddSeparatorAction());

        ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN, group.build());
        menu.setTargetComponent(myTree);
        menu.show(event.getComponent(), event.getInputDetails().getX(), event.getInputDetails().getY());
    }

    @RequiredUIAccess
    static void selectFirstMatch(Tree<CustomizationTreeItem> tree, KeymapGroupImpl root, String filter) {
        if (StringUtil.isEmptyOrSpaces(filter)) {
            return;
        }
        String lowerFilter = filter.trim().toLowerCase(Locale.ROOT);
        CustomizationTreeItem item = CustomizationUtil.findItem(
            root,
            it -> CustomizationUtil.getText(it.getComponent()).toLowerCase(Locale.ROOT).contains(lowerFilter)
        );
        if (item != null) {
            CustomizationUtil.selectItem(tree, item);
        }
    }

    /**
     * Applies the icon chosen for the action to the schema.
     *
     * @return whether the icon changed
     */
    static boolean setCustomIcon(
        CustomActionsSchemaImpl schema,
        CustomActionsSchemaImpl applicationSchema,
        String actionId,
        ActionIconInfo selectedInfo
    ) {
        Image originalIcon = schema.getActionIcon(actionId, applicationSchema);
        Image selectedIcon = selectedInfo.getIcon();
        if (selectedIcon == originalIcon) {
            return false;
        }
        String iconReference = selectedIcon != null ? selectedInfo.getIconReference() : null;
        return doSetIcon(schema, applicationSchema, actionId, iconReference);
    }

    private static boolean doSetIcon(
        CustomActionsSchemaImpl schema,
        CustomActionsSchemaImpl applicationSchema,
        String actionId,
        @Nullable String path
    ) {
        if (StringUtil.isEmpty(path)) {
            schema.removeIconCustomization(actionId);
            return true;
        }

        ActionManager actionManager = ActionManager.getInstance();
        if (actionManager.getAction(actionId) == null) {
            return false;
        }

        if (actionManager.getActionOrStub(path) != null) {
            Image toSet = applicationSchema.getOriginalIcon(path);
            Image defaultIcon = applicationSchema.getOriginalIcon(actionId);
            if (toSet != defaultIcon) {
                schema.addIconCustomization(actionId, path);
            }
            else {
                schema.removeIconCustomization(actionId);
            }
        }
        else {
            schema.addIconCustomization(actionId, path);
        }
        return true;
    }

    private static void setCustomizationSchemaForCurrentProjects() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            IdeFrameEx frame = WindowManagerEx.getInstanceEx().getIdeFrame(project);
            if (frame != null) {
                frame.updateView();
            }
        }
        IdeFrameEx frame = WindowManagerEx.getInstanceEx().getIdeFrame(null);
        if (frame != null) {
            frame.updateView();
        }
    }

    private abstract class TreeSelectionAction extends DumbAwareAction implements AnActionWithAsyncUpdate {
        private TreeSelectionAction(LocalizeValue text, @Nullable Image icon) {
            super(text, LocalizeValue.empty(), icon);
        }

        @Override
        public Coroutine<?, ?> updateAsync(AnActionEvent e) {
            return Coroutine.first(UIAction.apply(ignored -> {
                updatePresentation(e);
                return null;
            }));
        }

        @RequiredUIAccess
        protected void updatePresentation(AnActionEvent e) {
            e.getPresentation().setEnabled(mySelectedSchema != null && isApplicable(getSelectedItems()));
        }

        protected boolean isApplicable(List<CustomizationTreeItem> items) {
            return isUnderCustomizationRoot(items);
        }
    }

    private final class AddActionBelowSelectionAction extends TreeSelectionAction {
        private AddActionBelowSelectionAction() {
            super(KeyMapLocalize.groupCustomizationsAddActionBelow(), PlatformIconGroup.generalAdd());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            addActions();
        }

        @RequiredUIAccess
        @Override
        protected void updatePresentation(AnActionEvent e) {
            super.updatePresentation(e);
            CustomizationTreeItem item = getLeadSelectedItem();
            boolean isGroup = item != null && item.getGroup() != null;
            e.getPresentation().setText(
                isGroup ? KeyMapLocalize.groupCustomizationsAddActionGroup() : KeyMapLocalize.groupCustomizationsAddActionBelow()
            );
        }

        @Override
        protected boolean isApplicable(List<CustomizationTreeItem> items) {
            return isAddApplicable(items);
        }
    }

    private final class AddSeparatorAction extends TreeSelectionAction {
        private AddSeparatorAction() {
            super(KeyMapLocalize.buttonAddSeparator(), PlatformIconGroup.generalSeparatorh());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            addSeparator();
        }

        @Override
        protected boolean isApplicable(List<CustomizationTreeItem> items) {
            return super.isApplicable(items) && items.size() == 1;
        }
    }

    private final class RemoveAction extends TreeSelectionAction {
        private RemoveAction() {
            super(KeyMapLocalize.buttonRemove(), PlatformIconGroup.actionsGc());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            List<CustomizationTreeItem> items = getSelectedItems();
            if (isApplicable(items)) {
                removeItems(items);
            }
        }
    }

    private final class EditIconAction extends TreeSelectionAction {
        private EditIconAction() {
            super(KeyMapLocalize.buttonEditActionIcon(), PlatformIconGroup.actionsEdit());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            CustomizationTreeItem item = getLeadSelectedItem();
            if (item != null && isApplicable(getSelectedItems())) {
                editIcon(item);
            }
        }

        @Override
        protected boolean isApplicable(List<CustomizationTreeItem> items) {
            if (!super.isApplicable(items)) {
                return false;
            }
            CustomizationTreeItem item = getLeadSelectedItem();
            String actionId = item != null ? item.getActionId() : null;
            return actionId != null && ActionManager.getInstance().getAction(actionId) != null;
        }
    }

    private final class MoveAction extends TreeSelectionAction {
        private final int myDirection;

        private MoveAction(int direction) {
            super(
                direction < 0 ? KeyMapLocalize.buttonMoveUp() : KeyMapLocalize.buttonMoveDown(),
                direction < 0 ? PlatformIconGroup.actionsMoveup() : PlatformIconGroup.actionsMovedown()
            );
            myDirection = direction;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            List<CustomizationTreeItem> items = getSelectedItems();
            if (isApplicable(items)) {
                moveItems(items, myDirection);
            }
        }

        @Override
        protected boolean isApplicable(List<CustomizationTreeItem> items) {
            return isMoveSupported(items, myDirection);
        }
    }

    private final class RestoreSelectionAction extends DumbAwareAction implements AnActionWithAsyncUpdate {
        private RestoreSelectionAction() {
            super(KeyMapLocalize.buttonRestoreSelectedGroups());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            CustomActionsSchemaImpl schema = mySelectedSchema;
            if (schema == null) {
                return;
            }
            List<ActionUrl> otherActions = new ArrayList<>(schema.getActions());
            otherActions.removeAll(findActionsUnderSelection(getSelectedItems()));
            schema.copyFrom(new CustomActionsSchemaImpl(Application.get()));
            for (ActionUrl otherAction : otherActions) {
                schema.addAction(otherAction);
            }
            patchActionsTreeCorrespondingToSchema();
        }

        @Override
        public Coroutine<?, ?> updateAsync(AnActionEvent e) {
            return Coroutine.first(UIAction.apply(ignored -> {
                updatePresentation(e);
                return null;
            }));
        }

        @RequiredUIAccess
        private void updatePresentation(AnActionEvent e) {
            List<CustomizationTreeItem> items = getSelectedItems();
            e.getPresentation().setEnabled(!findActionsUnderSelection(items).isEmpty());
            Set<String> names = findSelectedGroupNames(items);
            e.getPresentation().setText(
                names.size() != 1 ? KeyMapLocalize.buttonRestoreSelectedGroups() : KeyMapLocalize.buttonRestoreSelection(names.iterator().next())
            );
        }
    }

    private final class RestoreAllAction extends DumbAwareAction implements AnActionWithAsyncUpdate {
        private RestoreAllAction() {
            super(KeyMapLocalize.buttonRestoreAll());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            CustomActionsSchemaImpl schema = mySelectedSchema;
            if (schema == null) {
                return;
            }
            schema.copyFrom(new CustomActionsSchemaImpl(Application.get()));
            patchActionsTreeCorrespondingToSchema();
        }

        @Override
        public Coroutine<?, ?> updateAsync(AnActionEvent e) {
            return Coroutine.first(UIAction.apply(ignored -> {
                CustomActionsSchemaImpl schema = mySelectedSchema;
                e.getPresentation().setEnabled(schema != null && schema.isModified(new CustomActionsSchemaImpl(Application.get())));
                return null;
            }));
        }
    }

    private final class MoveTransferHandler implements DragAndDropTransferHandler<TreeNode<CustomizationTreeItem>> {
        @Override
        public @Nullable DataTransfer createTransfer(Component component) {
            return null;
        }

        @Override
        public @Nullable DataTransfer createDragTransfer(Component component, List<TreeNode<CustomizationTreeItem>> items, boolean move) {
            if (!move || items.size() != 1) {
                return null;
            }
            CustomizationTreeItem item = items.get(0).getValue();
            if (item == null || item.isTopLevel()) {
                return null;
            }
            return DataTransfer.builder().put(DataTransferType.TEXT, CustomizationUtil.getText(item.getComponent())).build();
        }

        @RequiredUIAccess
        @Override
        public boolean drop(Component component, DropContext<TreeNode<CustomizationTreeItem>> context) {
            List<TreeNode<CustomizationTreeItem>> items = context.getItems();
            CustomizationTreeItem source = items.size() == 1 ? items.get(0).getValue() : null;
            CustomizationTreeItem target = context.getTarget().getValue();
            if (source == null || target == null || mySelectedSchema == null || !canDrop(source, target, context.getPosition())) {
                return false;
            }
            if (!context.isCheckOnly()) {
                doDrop(source, target, context.getPosition());
            }
            return true;
        }

        private boolean canDrop(CustomizationTreeItem source, CustomizationTreeItem target, DropPosition position) {
            if (source.isTopLevel() || source.equals(target)) {
                return false;
            }

            KeymapGroupImpl sourceGroup = source.getGroup();
            if (sourceGroup != null && (target.getGroup() == sourceGroup || target.isUnder(sourceGroup))) {
                return false;
            }

            if (position == DropPosition.INTO) {
                return target.getGroup() != null && target.getGroup() != source.getParent();
            }
            if (target.isTopLevel()) {
                return false;
            }

            if (source.getParent() == target.getParent()) {
                int sourceIndex = source.getIndex();
                int targetIndex = target.getIndex();
                if (sourceIndex == targetIndex - 1 && position == DropPosition.ABOVE) {
                    return false;
                }
                if (sourceIndex == targetIndex + 1 && position == DropPosition.BELOW) {
                    return false;
                }
            }
            return true;
        }

        @RequiredUIAccess
        private void doDrop(CustomizationTreeItem source, CustomizationTreeItem target, DropPosition position) {
            if (source.getParent() == target.getParent() && position != DropPosition.INTO) {
                int sourceIndex = source.getIndex();
                int targetIndex = target.getIndex();
                int newIndex = position == DropPosition.ABOVE
                    ? (sourceIndex < targetIndex ? targetIndex - 1 : targetIndex)
                    : (sourceIndex < targetIndex ? targetIndex : targetIndex + 1);

                ActionUrl url = CustomizationUtil.getActionUrl(source, MOVE);
                url.setInitialPosition(sourceIndex);
                url.setAbsolutePosition(newIndex);
                ActionUrl.changePathInActionsTree(myRoot, url);
                addCustomizedAction(url);
                refreshAndSelect(CustomizationTreeItem.at(source.getParent(), newIndex));
                return;
            }

            KeymapGroupImpl targetParent = position == DropPosition.INTO ? target.getGroup() : target.getParent();
            ActionUrl removeUrl = CustomizationUtil.getActionUrl(source, DELETED);
            ActionUrl.changePathInActionsTree(myRoot, removeUrl);
            addCustomizedAction(removeUrl);

            int position0 = position == DropPosition.INTO
                ? targetParent.getSize()
                : target.getIndex() + (position == DropPosition.BELOW ? 1 : 0);
            ActionUrl addUrl = new ActionUrl(CustomizationUtil.getGroupPath(targetParent), removeUrl.getComponent(), ADDED, position0);
            ActionUrl.changePathInActionsTree(myRoot, addUrl);
            addCustomizedAction(addUrl);
            refreshAndSelect(CustomizationTreeItem.at(targetParent, position0));
        }
    }
}
