// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.configuration.deployment;

import consulo.application.util.RecursionManager;
import consulo.remoteServer.ServerType;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.RemoteServersManager;
import consulo.remoteServer.configuration.ServerConfiguration;
import consulo.remoteServer.impl.internal.configuration.RemoteServerListConfigurable;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.UserActivityProviderComponent;
import consulo.ui.ex.awt.*;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.*;

public class RemoteServerCombo<S extends ServerConfiguration> extends ComboboxWithBrowseButton implements UserActivityProviderComponent {
    private static final Comparator<RemoteServer<?>> SERVERS_COMPARATOR =
        Comparator.comparing(RemoteServer::getName, String.CASE_INSENSITIVE_ORDER);

    private final ServerType<S> myServerType;
    private final List<ChangeListener> myChangeListeners = Lists.newLockFreeCopyOnWriteList();
    private final CollectionComboBoxModel<ServerItem> myServerListModel;
    private String myServerNameReminder;

    public RemoteServerCombo(@NotNull ServerType<S> serverType) {
        this(serverType, new CollectionComboBoxModel<>());
    }

    private RemoteServerCombo(@NotNull ServerType<S> serverType, @NotNull CollectionComboBoxModel<ServerItem> model) {
        super(new ComboBox<>(model));
        myServerType = serverType;
        myServerListModel = model;

        refillModel(null);

        addActionListener(this::onBrowseServer);
        getComboBox().addActionListener(this::onItemChosen);
        getComboBox().addItemListener(this::onItemUnselected);

        //noinspection unchecked
        getComboBox().setRenderer(new ColoredListCellRenderer<ServerItem>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends ServerItem> list, ServerItem value,
                                                 int index, boolean selected, boolean focused) {
                if (value == null) return;
                value.render(this);
            }
        });
    }

    public ServerItem getSelectedItem() {
        return (ServerItem) myServerListModel.getSelectedItem();
    }

    public @Nullable RemoteServer<S> getSelectedServer() {
        ServerItem selected = getSelectedItem();
        //noinspection unchecked
        return selected == null ? null : (RemoteServer<S>) selected.findRemoteServer();
    }

    public void selectServerInCombo(@Nullable String serverName) {
        ServerItem item = findNonTransientItemForName(serverName);
        if (serverName != null && item == null) {
            item = getMissingServerItem(serverName);
            if (item != null) {
                myServerListModel.add(0, item);
            }
        }
        getComboBox().setSelectedItem(item);
    }

    protected ServerType<S> getServerType() {
        return myServerType;
    }

    protected @NotNull List<TransientItem> getActionItems() {
        return Collections.singletonList(new CreateNewServerItem());
    }

    protected @Nullable ServerItem getMissingServerItem(@NotNull String serverName) {
        return new MissingServerItem(serverName);
    }

    /**
     * @return item with <code>result.getServerName() == null</code>
     */
    protected @NotNull ServerItem getNoServersItem() {
        return new NoServersItem();
    }

    private ServerItem findNonTransientItemForName(@Nullable String serverName) {
        return myServerListModel.getItems().stream()
            .filter(Objects::nonNull)
            .filter(item -> !(item instanceof TransientItem))
            .filter(item -> Objects.equals(item.getServerName(), serverName))
            .findAny().orElse(null);
    }

    @Override
    public void dispose() {
        super.dispose();
        myChangeListeners.clear();
    }

    protected final void fireStateChanged() {
        ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener changeListener : myChangeListeners) {
            changeListener.stateChanged(event);
        }
    }

    private void onBrowseServer(ActionEvent e) {
        ServerItem item = getSelectedItem();
        if (item != null) {
            item.onBrowseAction();
        }
        else {
            editServer(RemoteServerListConfigurable.createConfigurable(myServerType, null));
        }
    }

    private void onItemChosen(ActionEvent e) {
        RecursionManager.doPreventingRecursion(this, false, () -> {
            ServerItem selectedItem = getSelectedItem();
            if (selectedItem != null) {
                selectedItem.onItemChosen();
            }
            if (!(selectedItem instanceof TransientItem)) {
                fireStateChanged();
            }
            return null;
        });
    }

    private void onItemUnselected(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.DESELECTED) {
            ServerItem item = (ServerItem) e.getItem();
            myServerNameReminder = item == null ? null : item.getServerName();
        }
    }

    protected final boolean editServer(@NotNull RemoteServerListConfigurable configurable) {
        boolean isOk = ShowSettingsUtil.getInstance().editConfigurable(this, configurable);
        if (isOk) {
            RemoteServer<?> lastSelectedServer = configurable.getLastSelectedServer();
            refillModel(lastSelectedServer);
        }
        return isOk;
    }

    protected final void createAndEditNewServer() {
        String selectedBefore = myServerNameReminder;
        RemoteServersManager manager = RemoteServersManager.getInstance();
        RemoteServer<?> newServer = manager.createServer(myServerType);
        manager.addServer(newServer);
        if (!editServer(RemoteServerListConfigurable.createConfigurable(myServerType, newServer.getName()))) {
            manager.removeServer(newServer);
            selectServerInCombo(selectedBefore);
        }
    }

    protected final void refillModel(@Nullable RemoteServer<?> newSelection) {
        String nameToSelect = newSelection != null ? newSelection.getName() : null;

        myServerListModel.removeAll();
        ServerItem itemToSelect = null;

        List<RemoteServer<S>> servers = getSortedServers();
        if (servers.isEmpty()) {
            ServerItem noServersItem = getNoServersItem();
            if (nameToSelect == null) {
                itemToSelect = noServersItem;
            }
            myServerListModel.add(noServersItem);
        }

        for (RemoteServer<S> nextServer : getSortedServers()) {
            ServerItem nextServerItem = new ServerItemImpl(nextServer.getName());
            if (itemToSelect == null && nextServer.getName().equals(nameToSelect)) {
                itemToSelect = nextServerItem;
            }
            myServerListModel.add(nextServerItem);
        }

        for (TransientItem nextAction : getActionItems()) {
            myServerListModel.add(nextAction);
        }

        setSelectedServerItem(newSelection, itemToSelect);
    }

    protected void setSelectedServerItem(@Nullable RemoteServer<?> newSelection, @Nullable ServerItem itemToSelect) {
        getComboBox().setSelectedItem(itemToSelect);
    }

    protected @NotNull List<RemoteServer<S>> getSortedServers() {
        List<RemoteServer<S>> result = new ArrayList<>(RemoteServersManager.getInstance().getServers(myServerType));
        result.sort(SERVERS_COMPARATOR);
        return result;
    }

    @Override
    public void addChangeListener(@NotNull ChangeListener changeListener) {
        myChangeListeners.add(changeListener);
    }

    @Override
    public void removeChangeListener(@NotNull ChangeListener changeListener) {
        myChangeListeners.remove(changeListener);
    }

    public interface ServerItem {
        @Nullable
        String getServerName();

        void render(@NotNull SimpleColoredComponent ui);

        void onItemChosen();

        void onBrowseAction();

        @Nullable
        RemoteServer<?> findRemoteServer();
    }

    /**
     * marker for action items which always temporary and switch selection themselves after being chosen by user
     */
    public interface TransientItem extends ServerItem {
        //
    }

    private class CreateNewServerItem implements TransientItem {

        @Override
        public void render(@NotNull SimpleColoredComponent ui) {
            ui.setIcon(EmptyIcon.create(myServerType.getIcon()));
            ui.append(CloudBundle.message("remote.server.combo.create.new.server"), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }

        @Override
        public String getServerName() {
            return null;
        }

        @Override
        public void onItemChosen() {
            getChildComponent().hidePopup();
            createAndEditNewServer();
        }

        @Override
        public void onBrowseAction() {
            createAndEditNewServer();
        }

        @Override
        public @Nullable RemoteServer<S> findRemoteServer() {
            return null;
        }
    }

    public class ServerItemImpl implements ServerItem {
        private final @NlsSafe String myServerName;

        public ServerItemImpl(@NlsSafe String serverName) {
            myServerName = serverName;
        }

        @Override
        public @NlsSafe String getServerName() {
            return myServerName;
        }

        @Override
        public void onItemChosen() {
            //
        }

        @Override
        public void onBrowseAction() {
            editServer(RemoteServerListConfigurable.createConfigurable(myServerType, myServerName));
        }

        @Override
        public @Nullable RemoteServer<S> findRemoteServer() {
            return myServerName == null ? null : RemoteServersManager.getInstance().findByName(myServerName, myServerType);
        }

        @Override
        public void render(@NotNull SimpleColoredComponent ui) {
            RemoteServer<?> server = findRemoteServer();
            SimpleTextAttributes attributes = server == null ? SimpleTextAttributes.ERROR_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES;
            ui.setIcon(server == null ? null : myServerType.getIcon());
            ui.append(StringUtil.notNullize(myServerName), attributes);
        }
    }

    protected class MissingServerItem extends ServerItemImpl {

        public MissingServerItem(@NotNull String serverName) {
            super(serverName);
        }

        @Override
        public @NotNull @NlsSafe String getServerName() {
            String result = super.getServerName();
            assert result != null;
            return result;
        }

        @Override
        public void render(@NotNull SimpleColoredComponent ui) {
            ui.setIcon(myServerType.getIcon());
            ui.append(getServerName(), SimpleTextAttributes.ERROR_ATTRIBUTES);
        }
    }

    protected class NoServersItem extends ServerItemImpl {
        public NoServersItem() {
            super(null);
        }

        @Override
        public void render(@NotNull SimpleColoredComponent ui) {
            ui.setIcon(null);
            ui.append(CloudBundle.message("remote.server.combo.no.servers"), SimpleTextAttributes.ERROR_ATTRIBUTES);
        }
    }
}
