// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.configuration.deployment;

import consulo.disposer.Disposable;
import consulo.remoteServer.ServerType;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.RemoteServersManager;
import consulo.remoteServer.configuration.ServerConfiguration;
import consulo.remoteServer.impl.internal.configuration.RemoteServerListConfigurable;
import consulo.remoteServer.localize.RemoteServerLocalize;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.PseudoComponent;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.collection.Lists;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class RemoteServerCombo<S extends ServerConfiguration> implements PseudoComponent, Disposable {
    private static final Comparator<RemoteServer<?>> SERVERS_COMPARATOR =
        Comparator.comparing(RemoteServer::getName, String.CASE_INSENSITIVE_ORDER);

    private final ServerType<S> myServerType;
    private final List<Runnable> myChangeListeners = Lists.newLockFreeCopyOnWriteList();
    private final MutableFlatDataModel<ServerItem> myServerListModel = FlatDataModel.of(List.of());
    private final ComboBox<ServerItem> myComboBox;
    private @Nullable ServerItem myLastSelectedItem;
    private @Nullable String myServerNameReminder;
    private boolean myItemChosenInProgress;
    private boolean myDisposed;

    @RequiredUIAccess
    public RemoteServerCombo(ServerType<S> serverType) {
        myServerType = serverType;

        myComboBox = ComboBox.create(myServerListModel);
        myComboBox.setRender((presentation, item) -> {
            ServerItem value = item.getValue();
            if (value != null) {
                value.render(presentation);
            }
        });
        myComboBox.addValueListener(event -> onItemChosen(event.getValue()));

        refillModel(null);
    }

    @Override
    public Component getComponent() {
        return myComboBox;
    }

    public @Nullable ServerItem getSelectedItem() {
        return myComboBox.getValue();
    }

    @SuppressWarnings("unchecked")
    public @Nullable RemoteServer<S> getSelectedServer() {
        ServerItem selected = getSelectedItem();
        return selected == null ? null : (RemoteServer<S>) selected.findRemoteServer();
    }

    @RequiredUIAccess
    public void selectServerInCombo(@Nullable String serverName) {
        ServerItem item = findNonTransientItemForName(serverName);
        if (serverName != null && item == null) {
            item = getMissingServerItem(serverName);
            if (item != null) {
                myServerListModel.add(item, 0);
            }
        }
        myComboBox.setValue(item);
    }

    protected ServerType<S> getServerType() {
        return myServerType;
    }

    protected List<TransientItem> getActionItems() {
        return Collections.singletonList(new CreateNewServerItem());
    }

    protected @Nullable ServerItem getMissingServerItem(String serverName) {
        return new MissingServerItem(serverName);
    }

    /**
     * @return item with <code>result.getServerName() == null</code>
     */
    protected ServerItem getNoServersItem() {
        return new NoServersItem();
    }

    private @Nullable ServerItem findNonTransientItemForName(@Nullable String serverName) {
        for (int i = 0; i < myServerListModel.getSize(); i++) {
            ServerItem item = myServerListModel.get(i);
            if (item != null && !(item instanceof TransientItem) && Objects.equals(item.getServerName(), serverName)) {
                return item;
            }
        }
        return null;
    }

    @Override
    public void dispose() {
        myDisposed = true;
        myChangeListeners.clear();
    }

    protected final boolean isDisposed() {
        return myDisposed;
    }

    protected final void fireStateChanged() {
        for (Runnable changeListener : myChangeListeners) {
            changeListener.run();
        }
    }

    @RequiredUIAccess
    private void onItemChosen(@Nullable ServerItem selectedItem) {
        ServerItem lastSelectedItem = myLastSelectedItem;
        if (lastSelectedItem != selectedItem) {
            myServerNameReminder = lastSelectedItem == null ? null : lastSelectedItem.getServerName();
        }
        myLastSelectedItem = selectedItem;

        if (myItemChosenInProgress) {
            return;
        }

        myItemChosenInProgress = true;
        try {
            if (selectedItem != null) {
                selectedItem.onItemChosen();
            }
            if (!(selectedItem instanceof TransientItem)) {
                fireStateChanged();
            }
        }
        finally {
            myItemChosenInProgress = false;
        }
    }

    protected final boolean editServer(RemoteServerListConfigurable configurable) {
        return false;
    }

    @RequiredUIAccess
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

    @RequiredUIAccess
    protected final void refillModel(@Nullable RemoteServer<?> newSelection) {
        String nameToSelect = newSelection != null ? newSelection.getName() : null;

        List<ServerItem> items = new ArrayList<>();
        ServerItem itemToSelect = null;

        List<RemoteServer<S>> servers = getSortedServers();
        if (servers.isEmpty()) {
            ServerItem noServersItem = getNoServersItem();
            if (nameToSelect == null) {
                itemToSelect = noServersItem;
            }
            items.add(noServersItem);
        }

        for (RemoteServer<S> nextServer : servers) {
            ServerItem nextServerItem = new ServerItemImpl(nextServer.getName());
            if (itemToSelect == null && nextServer.getName().equals(nameToSelect)) {
                itemToSelect = nextServerItem;
            }
            items.add(nextServerItem);
        }

        items.addAll(getActionItems());

        myServerListModel.replaceAll(items);

        setSelectedServerItem(newSelection, itemToSelect);
    }

    @RequiredUIAccess
    protected void setSelectedServerItem(@Nullable RemoteServer<?> newSelection, @Nullable ServerItem itemToSelect) {
        myComboBox.setValue(itemToSelect);
    }

    protected List<RemoteServer<S>> getSortedServers() {
        List<RemoteServer<S>> result = new ArrayList<>(RemoteServersManager.getInstance().getServers(myServerType));
        result.sort(SERVERS_COMPARATOR);
        return result;
    }

    public void addChangeListener(Runnable changeListener) {
        myChangeListeners.add(changeListener);
    }

    public void removeChangeListener(Runnable changeListener) {
        myChangeListeners.remove(changeListener);
    }

    public interface ServerItem {
        @Nullable String getServerName();

        void render(TextItemPresentation presentation);

        @RequiredUIAccess
        void onItemChosen();

        @RequiredUIAccess
        void onBrowseAction();

        @Nullable RemoteServer<?> findRemoteServer();
    }

    /**
     * marker for action items which always temporary and switch selection themselves after being chosen by user
     */
    public interface TransientItem extends ServerItem {
    }

    private class CreateNewServerItem implements TransientItem {
        @Override
        public void render(TextItemPresentation presentation) {
            presentation.withIcon(Image.empty(myServerType.getIcon().getWidth(), myServerType.getIcon().getHeight()));
            presentation.append(RemoteServerLocalize.remoteServerComboCreateNewServer());
        }

        @Override
        public @Nullable String getServerName() {
            return null;
        }

        @RequiredUIAccess
        @Override
        public void onItemChosen() {
            createAndEditNewServer();
        }

        @RequiredUIAccess
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
        private final @Nullable String myServerName;

        public ServerItemImpl(@Nullable String serverName) {
            myServerName = serverName;
        }

        @Override
        public @Nullable String getServerName() {
            return myServerName;
        }

        @RequiredUIAccess
        @Override
        public void onItemChosen() {
        }

        @RequiredUIAccess
        @Override
        public void onBrowseAction() {
            editServer(RemoteServerListConfigurable.createConfigurable(myServerType, myServerName));
        }

        @Override
        public @Nullable RemoteServer<S> findRemoteServer() {
            return myServerName == null ? null : RemoteServersManager.getInstance().findByName(myServerName, myServerType);
        }

        @Override
        public void render(TextItemPresentation presentation) {
            RemoteServer<?> server = findRemoteServer();
            presentation.withIcon(server == null ? null : myServerType.getIcon());
            presentation.append(StringUtil.notNullize(myServerName), server == null ? TextAttribute.ERROR : TextAttribute.REGULAR);
        }
    }

    protected class MissingServerItem extends ServerItemImpl {
        public MissingServerItem(String serverName) {
            super(serverName);
        }

        @Override
        public String getServerName() {
            String result = super.getServerName();
            assert result != null;
            return result;
        }

        @Override
        public void render(TextItemPresentation presentation) {
            presentation.withIcon(myServerType.getIcon());
            presentation.append(getServerName(), TextAttribute.ERROR);
        }
    }

    protected class NoServersItem extends ServerItemImpl {
        public NoServersItem() {
            super(null);
        }

        @Override
        public void render(TextItemPresentation presentation) {
            presentation.append(RemoteServerLocalize.remoteServerComboNoServers(), TextAttribute.ERROR);
        }
    }
}
