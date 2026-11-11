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
package consulo.ai.impl.internal.chat;

import consulo.ai.AIModel;
import consulo.ai.AIProvider;
import consulo.ai.AIProviderTable;
import consulo.ai.icon.AIIconGroup;
import consulo.ai.impl.internal.setting.AIProviderConfigurable;
import consulo.ai.localize.AILocalize;
import consulo.ai.impl.internal.chat.state.AIChatEntry;
import consulo.ai.impl.internal.chat.state.AIChatGroupEntry;
import consulo.configurable.internal.ShowConfigurableService;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.Alerts;
import consulo.ui.Component;
import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.InputBoxBuilder;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.event.ContextMenuEvent;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The tree of the AI chat tool window: the groups of the user, then the chats of every account newest first - the chats
 * which providers keep on their own side and nobody filed yet among the ones at the top. A double click opens a chat in
 * an editor tab.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
public final class AIChatTreePanel implements Disposable {
    private static final Logger LOG = Logger.getInstance(AIChatTreePanel.class);
    private static final String PLACE = "AIChatTree";

    sealed interface Node permits GroupNode, ChatNode, ProviderChatNode {
    }

    record GroupNode(AIChatGroupEntry group) implements Node {
    }

    record ChatNode(AIChatEntry chat) implements Node {
    }


    record ProviderChatNode(AIChatView.ProviderChat chat) implements Node {
    }

    private final Project myProject;
    private final AIChatView myChatView;
    private final AIChatStore myStore;
    private final AIProviderTable myProviderTable;
    private final ShowConfigurableService myShowConfigurableService;
    private final UIAccess myUIAccess;

    private final Tree<Node> myTree;
    private final DockLayout myRoot = DockLayout.create();

    private List<AIChatView.ProviderChat> myProviderChats = List.of();
    private Set<String> myConfigured = Set.of();
    private boolean myDisposed;
    /**
     * The selection as the tree last reported it - read by the toolbar off the ui thread.
     */
    private volatile List<Node> mySelection = List.of();

    @RequiredUIAccess
    public AIChatTreePanel(Project project,
                           AIChatView chatView,
                           AIProviderTable providerTable,
                           ShowConfigurableService showConfigurableService) {
        myProject = project;
        myChatView = chatView;
        myStore = chatView.getStore();
        myProviderTable = providerTable;
        myShowConfigurableService = showConfigurableService;
        myUIAccess = UIAccess.current();

        myTree = Tree.create(new Model());
        myTree.setSpeedSearchConverter(node -> {
            Node value = node.getValue();
            return value == null ? "" : text(value);
        });
        myTree.addContextMenuListener(this::showContextMenu);
        myTree.setAllowMultipleSelect(true);
        myTree.addSelectListener(event -> mySelection = selectedNodes());
        myTree.setTransferHandler(new DragAndDrop());
        Disposer.register(this, myTree.destroyHook());

        DefaultActionGroup toolbarGroup = new DefaultActionGroup();
        toolbarGroup.add(new NewChatAction());
        toolbarGroup.add(new NewGroupAction());
        toolbarGroup.add(new DeleteAction());
        toolbarGroup.addSeparator();
        toolbarGroup.add(new RefreshAction());
        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar(PLACE, toolbarGroup, ActionToolbar.Style.HORIZONTAL);
        toolbar.setTargetUIComponent(myTree);

        myRoot.top(toolbar.getUIComponent());
        myRoot.center(ScrollableLayout.create(myTree));

        myStore.addListener(() -> myUIAccess.give(this::rebuild), this);

        refresh();
    }

    public Component getComponent() {
        return myRoot;
    }

    /**
     * Reads again which accounts can answer and which chats the providers keep on their side.
     */
    @RequiredUIAccess
    public void refresh() {
        myProviderTable.getConfiguredProviders().whenCompleteAsync((configured, error) -> {
            myConfigured = configured == null ? Set.of() : configured.stream().map(AIProvider::getName).collect(Collectors.toSet());
        }, myUIAccess);
        myChatView.loadProviderChats().whenCompleteAsync((chats, error) -> {
            if (error != null) {
                LOG.warn("Cannot list the chats of the providers", error);
            }
            myProviderChats = chats == null ? List.of() : chats;
            rebuild();
        }, myUIAccess);
    }

    @RequiredUIAccess
    private void rebuild() {
        if (myDisposed) {
            return;
        }
        // a chat the store took over since the providers were last asked is filed now
        myProviderChats = myProviderChats.stream().filter(chat -> myStore.findChat(chat.info().id()) == null).toList();
        myTree.refreshAll();
    }

    // region selection

    private List<Node> selectedNodes() {
        List<Node> nodes = new ArrayList<>();
        for (TreeNode<Node> node : myTree.getSelectedNodes()) {
            Node value = node.getValue();
            if (value != null) {
                nodes.add(value);
            }
        }
        return nodes;
    }

    private static List<Node> deletable(List<Node> nodes) {
        List<Node> result = new ArrayList<>();
        for (Node node : nodes) {
            if (node instanceof ChatNode || node instanceof GroupNode || node instanceof ProviderChatNode) {
                result.add(node);
            }
        }
        return result;
    }

    private @Nullable Node selected() {
        TreeNode<Node> node = myTree.getSelectedNode();
        return node == null ? null : node.getValue();
    }

    /**
     * Where a new chat or group goes - into the selected group, or the group of the selected chat.
     */
    private String selectedGroupId() {
        return switch (selected()) {
            case GroupNode group -> group.group().ID;
            case ChatNode chat -> myStore.findGroup(chat.chat().GROUP_ID) == null ? "" : chat.chat().GROUP_ID;
            case null, default -> "";
        };
    }

    @RequiredUIAccess
    private void open(Node node) {
        switch (node) {
            case ChatNode chat -> myChatView.openChat(chat.chat(), true);
            case ProviderChatNode providerChat -> myChatView.openProviderChat(providerChat.chat(), "");
            default -> {
            }
        }
    }

    // endregion

    // region presentation

    private void render(Node value, TextItemPresentation presentation) {
        switch (value) {
            case GroupNode group -> {
                presentation.withIcon(PlatformIconGroup.nodesFolder());
                presentation.append(group.group().NAME);
            }
            case ChatNode chat -> {
                AIProvider provider = myStore.findProvider(chat.chat());
                presentation.withIcon(provider == null ? AIIconGroup.toolwindowAiassistant() : provider.getType().getIcon());
                presentation.append(AIChatView.title(chat.chat()), provider == null ? TextAttribute.GRAYED : TextAttribute.REGULAR);
                presentation.append("  " + chat.chat().PROVIDER_NAME, TextAttribute.GRAY);
            }
            case ProviderChatNode providerChat -> {
                AIChatView.ProviderChat chat = providerChat.chat();
                presentation.withIcon(chat.provider().getType().getIcon());
                String title = chat.info().title();
                presentation.append(title.isBlank() ? chat.info().id() : title);
                // a chat which ran elsewhere than the project says where
                Path directory = chat.info().workingDirectory();
                String basePath = myProject.getBasePath();
                if (directory != null && (basePath == null || !directory.equals(Path.of(basePath))) && directory.getFileName() != null) {
                    presentation.append("  " + directory.getFileName(), TextAttribute.GRAY);
                }
                presentation.append("  " + chat.provider().getName(), TextAttribute.GRAY);
            }
        }
    }

    private static String text(Node value) {
        return switch (value) {
            case GroupNode group -> group.group().NAME;
            case ChatNode chat -> AIChatView.title(chat.chat());
            case ProviderChatNode providerChat -> providerChat.chat().info().title();
        };
    }

    // endregion

    /**
     * Files the chat or group into the group - a chat of a provider which nobody filed yet is taken over by the store on
     * the way.
     */
    private void move(Node node, String groupId) {
        switch (node) {
            case ChatNode chat -> myStore.moveChat(chat.chat(), groupId);
            case GroupNode moved -> myStore.moveGroup(moved.group(), groupId);
            case ProviderChatNode providerChat -> {
                AIChatView.ProviderChat chat = providerChat.chat();
                if (myStore.findChat(chat.info().id()) == null) {
                    AIChatEntry entry = myStore.addChat(
                        chat.info().id(),
                        chat.provider(),
                        "",
                        chat.info().title(),
                        groupId,
                        chat.info().workingDirectory()
                    );
                    entry.UPDATED = chat.info().updated().toEpochMilli();
                }
            }
        }
    }

    private static long updated(Node node) {
        return switch (node) {
            case ChatNode chat -> chat.chat().UPDATED;
            case ProviderChatNode providerChat -> providerChat.chat().info().updated().toEpochMilli();
            case GroupNode ignored -> 0;
        };
    }

    private static boolean isMovable(@Nullable Node node) {
        return node instanceof ChatNode || node instanceof GroupNode || node instanceof ProviderChatNode;
    }

    @RequiredUIAccess
    private void showContextMenu(ContextMenuEvent event) {
        // the menu is built for the node it is opened on, so it holds only what applies to that node
        Node node = selected();
        boolean chat = node instanceof ChatNode;
        boolean filed = chat || node instanceof GroupNode;

        DefaultActionGroup group = new DefaultActionGroup();
        if (chat || node instanceof ProviderChatNode) {
            group.add(new OpenAction());
            group.addSeparator();
        }
        group.add(new NewChatAction());
        group.add(new NewGroupAction());
        if (filed || node instanceof ProviderChatNode) {
            group.addSeparator();
        }
        if (filed) {
            group.add(new RenameAction());
        }
        if (filed || node instanceof ProviderChatNode) {
            group.add(new MoveAction());
        }
        if (filed || node instanceof ProviderChatNode) {
            group.add(new DeleteAction());
        }

        ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(PLACE, group);
        menu.setTargetComponent(myTree);
        menu.show(event.getComponent(), event.getInputDetails().getX(), event.getInputDetails().getY());
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }

    /**
     * Drags chats and groups - several at a time - between groups: a drop into a group files them in it, a drop above or
     * below a node files them next to it. A group is never dropped into itself.
     */
    private final class DragAndDrop implements DragAndDropTransferHandler<TreeNode<Node>> {
        @Override
        public @Nullable DataTransfer createTransfer(Component component) {
            return null;
        }

        @Override
        public @Nullable DataTransfer createDragTransfer(Component component, List<TreeNode<Node>> items, boolean move) {
            if (!move || items.isEmpty()) {
                return null;
            }
            StringBuilder text = new StringBuilder();
            for (TreeNode<Node> item : items) {
                if (!(item.getValue() instanceof Node value) || !isMovable(value)) {
                    return null;
                }
                if (!text.isEmpty()) {
                    text.append('\n');
                }
                text.append(text(value));
            }
            return DataTransfer.of(text.toString());
        }

        @RequiredUIAccess
        @Override
        public boolean drop(Component component, DropContext<TreeNode<Node>> context) {
            List<TreeNode<Node>> items = context.getItems();
            String groupId = targetGroupId(context.getTarget().getValue(), context.getPosition());
            if (items.isEmpty() || groupId == null) {
                return false;
            }
            AIChatGroupEntry targetGroup = myStore.findGroup(groupId);
            for (TreeNode<Node> item : items) {
                Node value = item.getValue();
                if (!isMovable(value)) {
                    return false;
                }
                if (value instanceof GroupNode moved && targetGroup != null && myStore.isInside(targetGroup, moved.group().ID)) {
                    return false;
                }
            }
            if (context.isCheckOnly()) {
                return true;
            }
            for (TreeNode<Node> item : items) {
                Node value = item.getValue();
                if (value != null) {
                    move(value, groupId);
                }
            }
            return true;
        }

        /**
         * The group a drop files into, null where nothing can be dropped - among the chats nobody filed.
         */
        private @Nullable String targetGroupId(@Nullable Node target, DropPosition position) {
            return switch (target) {
                case GroupNode group -> position == DropPosition.INTO ? group.group().ID : group.group().PARENT_ID;
                case ChatNode chat -> myStore.findGroup(chat.chat().GROUP_ID) == null ? "" : chat.chat().GROUP_ID;
                case ProviderChatNode ignored -> "";
                case null -> null;
            };
        }
    }

    private final class Model implements TreeModel<Node> {
        @Override
        public void buildChildren(Function<Node, TreeNode<Node>> nodeFactory, @Nullable Node parentValue) {
            if (myDisposed) {
                return;
            }
            switch (parentValue) {
                case null -> addGroup(nodeFactory, "", myProviderChats);
                case GroupNode group -> addGroup(nodeFactory, group.group().ID, List.of());
                default -> {
                }
            }
        }

        /**
         * The groups first, then the chats newest first - at the top the chats nobody filed among them.
         */
        private void addGroup(Function<Node, TreeNode<Node>> nodeFactory, String groupId, List<AIChatView.ProviderChat> unfiled) {
            for (AIChatGroupEntry group : myStore.getGroups(groupId)) {
                add(nodeFactory, new GroupNode(group), false);
            }
            List<Node> chats = new ArrayList<>();
            for (AIChatEntry chat : myStore.getChats(groupId)) {
                chats.add(new ChatNode(chat));
            }
            for (AIChatView.ProviderChat chat : unfiled) {
                chats.add(new ProviderChatNode(chat));
            }
            chats.sort(Comparator.comparingLong(AIChatTreePanel::updated).reversed());
            for (Node chat : chats) {
                add(nodeFactory, chat, true);
            }
        }

        private void add(Function<Node, TreeNode<Node>> nodeFactory, Node value, boolean leaf) {
            TreeNode<Node> node = nodeFactory.apply(value);
            node.setLeaf(leaf);
            node.setRenderer(AIChatTreePanel.this::render);
        }

        @RequiredUIAccess
        @Override
        public boolean onDoubleClick(Tree<Node> tree, TreeNode<Node> node) {
            Node value = node.getValue();
            if (value instanceof ChatNode || value instanceof ProviderChatNode) {
                open(value);
                return false;
            }
            return true;
        }
    }

    // region actions

    private final class OpenAction extends DumbAwareAction {
        OpenAction() {
            super(AILocalize.actionOpenChat());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            Node node = selected();
            if (node != null) {
                open(node);
            }
        }

    }

    /**
     * Every account, the ones which can not answer yet too - choosing one of those opens its settings.
     */
    private final class NewChatAction extends DumbAwareAction {
        NewChatAction() {
            super(AILocalize.actionNewChat(), LocalizeValue.empty(), PlatformIconGroup.generalAdd());
        }

        /**
         * Which accounts can answer is asked now - an account may have been signed in since the tree was shown.
         */
        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            myProviderTable.getConfiguredProviders().whenCompleteAsync((configured, error) -> {
                myConfigured = configured == null ? Set.of() : configured.stream().map(AIProvider::getName).collect(Collectors.toSet());
                showPopup(e);
            }, myUIAccess);
        }

        @RequiredUIAccess
        private void showPopup(AnActionEvent e) {
            String groupId = selectedGroupId();
            List<AIProvider> providers = myProviderTable.getProviders();

            DefaultActionGroup group = new DefaultActionGroup();
            for (AIProvider provider : providers) {
                boolean configured = myConfigured.contains(provider.getName());
                LocalizeValue name = configured
                    ? LocalizeValue.of(provider.getName())
                    : AILocalize.labelAccountNotConfigured(provider.getName());
                if (!configured) {
                    DumbAwareAction configure = new DumbAwareAction(name, LocalizeValue.empty(), provider.getType().getIcon()) {
                        @RequiredUIAccess
                        @Override
                        public void actionPerformed(AnActionEvent event) {
                            myShowConfigurableService.showAndSelect(myProject, AIProviderConfigurable.class);
                        }
                    };
                    group.add(configure);
                    continue;
                }
                DefaultActionGroup models = new DefaultActionGroup(name, true);
                models.getTemplatePresentation().setIcon(provider.getType().getIcon());
                for (AIModel model : provider.getModels()) {
                    models.add(new DumbAwareAction(model.getDisplayName()) {
                        @RequiredUIAccess
                        @Override
                        public void actionPerformed(AnActionEvent event) {
                            myChatView.startChat(provider, model, groupId).whenCompleteAsync((chat, error) -> {
                                if (error != null) {
                                    Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                                    Alerts.okError(AILocalize.labelFailed(String.valueOf(cause.getMessage()))).showAsync(myTree);
                                }
                            }, myUIAccess);
                        }
                    });
                }
                group.add(models);
            }
            if (!providers.isEmpty()) {
                group.addSeparator();
            }
            group.add(new DumbAwareAction(AILocalize.actionAddAccountEllipsis(), LocalizeValue.empty(), PlatformIconGroup.generalAdd()) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(AnActionEvent event) {
                    myShowConfigurableService.showAndSelect(myProject, AIProviderConfigurable.class);
                }
            });
            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                AILocalize.actionNewChat().get(),
                group,
                e.getDataContext(),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false
            );
            popup.showUnderneathOf(e);
        }
    }

    private final class NewGroupAction extends DumbAwareAction {
        NewGroupAction() {
            super(AILocalize.actionNewGroup(), LocalizeValue.empty(), PlatformIconGroup.actionsNewfolder());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            String parentId = selectedGroupId();
            InputBoxBuilder.text()
                .title(AILocalize.dialogNewGroupTitle())
                .text(AILocalize.dialogGroupNamePrompt())
                .showAsync(myTree)
                .whenCompleteAsync((name, error) -> {
                    if (error == null && name != null && !name.isBlank()) {
                        myStore.addGroup(name.strip(), parentId);
                    }
                }, myUIAccess);
        }
    }

    private final class RenameAction extends DumbAwareAction {
        RenameAction() {
            super(AILocalize.actionRenameEllipsis());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            switch (selected()) {
                case ChatNode chat -> myChatView.renameChat(chat.chat(), myTree);
                case GroupNode group -> InputBoxBuilder.text()
                    .title(AILocalize.dialogRenameGroupTitle())
                    .text(AILocalize.dialogGroupNamePrompt())
                    .value(group.group().NAME)
                    .showAsync(myTree)
                    .whenCompleteAsync((name, error) -> {
                        if (error == null && name != null && !name.isBlank()) {
                            myStore.renameGroup(group.group(), name.strip());
                        }
                    }, myUIAccess);
                case null, default -> {
                }
            }
        }

    }

    /**
     * Files the selected chat or group into another group - a chat of a provider which nobody filed yet is taken
     * over by the store on the way.
     */
    private final class MoveAction extends DumbAwareAction {
        MoveAction() {
            super(AILocalize.actionMoveToGroupEllipsis());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            Node node = selected();
            if (node == null) {
                return;
            }

            DefaultActionGroup group = new DefaultActionGroup();
            group.add(moveTo(node, "", AILocalize.labelTopLevel()));
            for (AIChatGroupEntry target : myStore.getAllGroups()) {
                if (node instanceof GroupNode moved && myStore.isInside(target, moved.group().ID)) {
                    continue;
                }
                group.add(moveTo(node, target.ID, LocalizeValue.of(path(target))));
            }
            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                AILocalize.actionMoveToGroupEllipsis().get(),
                group,
                e.getDataContext(),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false
            );
            popup.showInBestPositionFor(e.getDataContext());
        }

        private DumbAwareAction moveTo(Node node, String groupId, LocalizeValue text) {
            return new DumbAwareAction(text, LocalizeValue.empty(), PlatformIconGroup.nodesFolder()) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(AnActionEvent event) {
                    move(node, groupId);
                }
            };
        }

        private String path(AIChatGroupEntry group) {
            StringBuilder builder = new StringBuilder(group.NAME);
            AIChatGroupEntry parent = myStore.findGroup(group.PARENT_ID);
            while (parent != null) {
                builder.insert(0, parent.NAME + " / ");
                parent = myStore.findGroup(parent.PARENT_ID);
            }
            return builder.toString();
        }

    }

    /**
     * Deletes every selected chat and group after one confirmation - a chat nobody filed is deleted from its provider.
     * Enabled by the selection as the tree last reported it, since the toolbar asks off the ui thread.
     */
    private final class DeleteAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        DeleteAction() {
            super(AILocalize.actionDelete(), LocalizeValue.empty(), PlatformIconGroup.generalDelete());
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(!deletable(mySelection).isEmpty());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            List<Node> nodes = deletable(selectedNodes());
            if (nodes.isEmpty()) {
                return;
            }
            LocalizeValue title;
            LocalizeValue message;
            if (nodes.size() > 1) {
                title = AILocalize.dialogDeleteItemsTitle();
                message = AILocalize.dialogDeleteItemsMessage(nodes.size());
            }
            else if (nodes.getFirst() instanceof GroupNode group) {
                title = AILocalize.dialogDeleteGroupTitle();
                message = AILocalize.dialogDeleteGroupMessage(group.group().NAME);
            }
            else {
                title = AILocalize.dialogDeleteChatTitle();
                message = AILocalize.dialogDeleteChatMessage(text(nodes.getFirst()));
            }
            Alerts.yesNo()
                .asWarning()
                .title(title)
                .text(message)
                .showAsync(myTree)
                .whenCompleteAsync((yes, error) -> {
                    if (Boolean.TRUE.equals(yes)) {
                        delete(nodes);
                    }
                }, myUIAccess);
        }

        @RequiredUIAccess
        private void delete(List<Node> nodes) {
            boolean providerChats = false;
            for (Node node : nodes) {
                switch (node) {
                    case ChatNode chat -> myChatView.deleteChat(chat.chat());
                    case GroupNode group -> myStore.removeGroup(group.group());
                    case ProviderChatNode providerChat -> {
                        providerChats = true;
                        myChatView.deleteProviderChat(providerChat.chat()).whenCompleteAsync((ignored, error) -> {
                            if (error != null) {
                                LOG.warn("Cannot delete the chat " + providerChat.chat().info().id(), error);
                            }
                            refresh();
                        }, myUIAccess);
                    }
                }
            }
            if (!providerChats) {
                rebuild();
            }
        }
    }

    private final class RefreshAction extends DumbAwareAction {
        RefreshAction() {
            super(AILocalize.actionRefreshChats(), LocalizeValue.empty(), PlatformIconGroup.actionsRefresh());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            refresh();
        }
    }

    // endregion
}
