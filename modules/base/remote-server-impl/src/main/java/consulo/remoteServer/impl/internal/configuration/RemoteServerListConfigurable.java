// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.configuration;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.component.util.text.UniqueNameGenerator;
import consulo.configurable.*;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.remoteServer.CloudBundle;
import consulo.remoteServer.ServerType;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.RemoteServersManager;
import consulo.remoteServer.localize.RemoteServerLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.MasterDetailsComponent;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.*;
import java.util.*;

@ExtensionImpl
public class RemoteServerListConfigurable extends MasterDetailsComponent implements SearchableConfigurable,
    ApplicationConfigurable,
    Configurable.NoMargin,
    Configurable.NoScroll {
    public static final String ID = "RemoteServers";

    private final RemoteServersManager myServersManager;
    private RemoteServer<?> myLastSelectedServer;
    private final String myInitialSelectedName;
    private final List<ServerType> myDisplayedServerTypes;

    private boolean isTreeInitialized;

    @Inject
    public RemoteServerListConfigurable(@Nonnull Application application, @Nonnull RemoteServersManager manager) {
        this(manager, application.getExtensionList(ServerType.class), null);
    }

    private RemoteServerListConfigurable(@Nonnull RemoteServersManager manager,
                                         @Nonnull ServerType<?> type,
                                         @Nullable String initialSelectedName) {
        this(manager, Collections.singletonList(type), initialSelectedName);
    }

    protected RemoteServerListConfigurable(@Nonnull RemoteServersManager manager,
                                           @Nonnull List<ServerType> displayedServerTypes,
                                           @Nullable String initialSelectedName) {
        myServersManager = manager;
        myDisplayedServerTypes = displayedServerTypes;
        myToReInitWholePanel = true;
        myInitialSelectedName = initialSelectedName;
    }

    @Nullable
    @Override
    public String getParentId() {
        return StandardConfigurableIds.EXECUTION_GROUP;
    }

    @RequiredUIAccess
    @Override
    @Nonnull
    public JComponent createComponent(@Nonnull Disposable uiDisposable) {
        if (!isTreeInitialized) {
            initTree();
            isTreeInitialized = true;
        }
        return super.createComponent(uiDisposable);
    }

    @Nullable
    private ServerType<?> getSingleServerType() {
        List<ServerType> serverTypes = getDisplayedServerTypes();
        return serverTypes.size() == 1 ? serverTypes.get(0) : null;
    }

    public @Nonnull List<ServerType> getDisplayedServerTypes() {
        // `myDisplayedServerTypes` might be `null` here because overridden `reInitWholePanelIfNeeded()`
        // is executed from `super()` before `myDisplayedServerTypes` is initialized
        return myDisplayedServerTypes != null ? myDisplayedServerTypes : Collections.emptyList();
    }

    @Override
    @Nullable
    protected String getEmptySelectionString() {
        final String typeNames = StringUtil.join(getDisplayedServerTypes(), serverType -> serverType.getPresentableName().get(), ", ");

        if (typeNames.length() > 0) {
            return CloudBundle.message("clouds.configure.empty.selection.string", typeNames);
        }
        return null;
    }

    public static RemoteServerListConfigurable createConfigurable(@Nonnull ServerType<?> type) {
        return createConfigurable(type, null);
    }

    public static RemoteServerListConfigurable createConfigurable(@Nonnull ServerType<?> type, @Nullable String nameToSelect) {
        return new RemoteServerListConfigurable(RemoteServersManager.getInstance(), type, nameToSelect);
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return CloudBundle.message("configurable.display.name.clouds");
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        myRoot.removeAllChildren();
        for (RemoteServer<?> server : getServers()) {
            addServerNode(server, false);
        }
        super.reset();
        if (myInitialSelectedName != null) {
            selectNodeInTree(myInitialSelectedName);
        }
    }

    private @Nonnull List<? extends RemoteServer<?>> getServers() {
        return ContainerUtil.filter(myServersManager.getServers(), s -> myDisplayedServerTypes.contains(s.getType()));
    }

    public MyNode addServerNode(RemoteServer<?> server, boolean isNew) {
        MyNode node = new MyNode(new SingleRemoteServerConfigurable(server, TREE_UPDATER, isNew));
        addNode(node, myRoot);
        return node;
    }

    @Override
    @Nonnull
    public String getId() {
        return ID;
    }

    @Override
    @Nullable
    public Runnable enableSearch(final String option) {
        return () -> Objects.requireNonNull(SpeedSearchSupply.getSupply(myTree, true)).findAndSelectElement(option);
    }

    @Override
    protected void initTree() {
        super.initTree();
        TreeSpeedSearch.installOn(myTree, true, treePath -> ((MyNode) treePath.getLastPathComponent()).getDisplayName());
    }

    @Override
    protected void processRemovedItems() {
        Set<RemoteServer<?>> servers = new HashSet<>();
        for (NamedConfigurable<RemoteServer<?>> configurable : getConfiguredServers()) {
            servers.add(configurable.getEditableObject());
        }

        List<RemoteServer<?>> toDelete = new ArrayList<>();
        for (RemoteServer<?> server : getServers()) {
            if (!servers.contains(server)) {
                toDelete.add(server);
            }
        }
        for (RemoteServer<?> server : toDelete) {
            myServersManager.removeServer(server);
        }
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        super.apply();
        Set<RemoteServer<?>> servers = new HashSet<>(getServers());
        for (NamedConfigurable<RemoteServer<?>> configurable : getConfiguredServers()) {
            RemoteServer<?> server = configurable.getEditableObject();
            server.setName(configurable.getDisplayName());
            if (!servers.contains(server)) {
                myServersManager.addServer(server);
            }
        }
    }

    @Override
    protected @Nullable ArrayList<AnAction> createActions(boolean fromPopup) {
        ArrayList<AnAction> actions = new ArrayList<>();
        ServerType<?> singleServerType = getSingleServerType();
        if (singleServerType == null) {
            actions.add(new AddRemoteServerGroup());
        }
        else {
            actions.add(new AddRemoteServerAction(singleServerType, PlatformIconGroup.generalAdd()));
        }
        actions.add(new MyDeleteAction());
        return actions;
    }

    @Override
    protected boolean wasObjectStored(Object editableObject) {
        return true;
    }

    @Override
    public String getHelpTopic() {
        String result = super.getHelpTopic();
        if (result == null) {
            ServerType<?> singleServerType = getSingleServerType();
            if (singleServerType != null) {
                result = singleServerType.getHelpTopic();
            }
        }
        return result != null ? result : "reference.settings.clouds";
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        Object selectedObject = getSelectedObject();
        myLastSelectedServer = selectedObject instanceof RemoteServer<?> ? (RemoteServer) selectedObject : null;
        super.disposeUIResources();
    }

    public @Nullable RemoteServer<?> getLastSelectedServer() {
        return myLastSelectedServer;
    }

    @Override
    protected void reInitWholePanelIfNeeded() {
        super.reInitWholePanelIfNeeded();
        if (myWholePanel.getBorder() == null) {
            myWholePanel.setBorder(JBUI.Borders.emptyLeft(10));
        }
    }

    private List<NamedConfigurable<RemoteServer<?>>> getConfiguredServers() {
        List<NamedConfigurable<RemoteServer<?>>> configurables = new ArrayList<>();
        for (int i = 0; i < myRoot.getChildCount(); i++) {
            MyNode node = (MyNode) myRoot.getChildAt(i);
            configurables.add((NamedConfigurable<RemoteServer<?>>) node.getConfigurable());
        }
        return configurables;
    }

    private final class AddRemoteServerGroup extends ActionGroup implements ActionGroupWithPreselection {
        private AddRemoteServerGroup() {
            super(RemoteServerLocalize.groupActionAddremoteservergroupText(), LocalizeValue.of(), PlatformIconGroup.generalAdd());
            registerCustomShortcutSet(CommonShortcuts.getInsert(), myTree);
        }

        @Override
        @Nonnull
        public AnAction[] getChildren(@Nullable AnActionEvent e) {
            List<ServerType> serverTypes = getDisplayedServerTypes();
            AnAction[] actions = new AnAction[serverTypes.size()];
            for (int i = 0; i < serverTypes.size(); i++) {
                actions[i] = new AddRemoteServerAction(serverTypes.get(i), serverTypes.get(i).getIcon());
            }
            return actions;
        }

        @Override
        public ActionGroup getActionGroup() {
            return this;
        }
    }

    private final class AddRemoteServerAction extends DumbAwareAction {
        private final ServerType<?> myServerType;

        private AddRemoteServerAction(ServerType<?> serverType, final Image icon) {
            super(serverType.getPresentableName(), LocalizeValue.of(), icon);
            myServerType = serverType;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            String name = UniqueNameGenerator.generateUniqueName(myServerType.getPresentableName().get(), s -> {
                for (NamedConfigurable<RemoteServer<?>> configurable : getConfiguredServers()) {
                    if (configurable.getDisplayName().equals(s)) {
                        return false;
                    }
                }
                return true;
            });
            MyNode node = addServerNode(myServersManager.createServer(myServerType, name), true);
            selectNodeInTree(node);
        }
    }
}
