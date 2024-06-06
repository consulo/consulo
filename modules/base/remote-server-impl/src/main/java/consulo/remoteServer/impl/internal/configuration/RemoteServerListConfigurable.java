package consulo.remoteServer.impl.internal.configuration;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.util.text.UniqueNameGenerator;
import consulo.configurable.*;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.remoteServer.ServerType;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.RemoteServersManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.MasterDetailsComponent;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author nik
 */
@ExtensionImpl
public class RemoteServerListConfigurable extends MasterDetailsComponent
        implements OptionalConfigurable, ApplicationConfigurable, SearchableConfigurable, Configurable.NoScroll, Configurable.NoMargin {
  private final RemoteServersManager myServersManager;
  @Nullable
  private final ServerType<?> myServerType;
  private RemoteServer<?> myLastSelectedServer;

  @Inject
  public RemoteServerListConfigurable(@Nonnull RemoteServersManager manager) {
    this(manager, null);
  }

  private RemoteServerListConfigurable(@Nonnull RemoteServersManager manager, @Nullable ServerType<?> type) {
    myServersManager = manager;
    myServerType = type;
    initTree();
  }

  @Nonnull
  @Nls
  @Override
  public String getDisplayName() {
    return "Clouds";
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.EXECUTION_GROUP;
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    myRoot.removeAllChildren();
    List<RemoteServer<?>> servers = getServers();
    for (RemoteServer<?> server : servers) {
      addServerNode(server, false);
    }
    super.reset();
  }

  private List<RemoteServer<?>> getServers() {
    if (myServerType == null) {
      return myServersManager.getServers();
    }
    else {
      //code won't compile without this ugly cast (at least in jdk 1.6)
      return (List<RemoteServer<?>>)((List)myServersManager.getServers(myServerType));
    }
  }

  private MyNode addServerNode(RemoteServer<?> server, boolean isNew) {
    MyNode node = new MyNode(new RemoteServerConfigurable(server, TREE_UPDATER, isNew));
    addNode(node, myRoot);
    return node;
  }

  @Nonnull
  @Override
  public String getId() {
    return "remote.servers";
  }

  @Nullable
  @Override
  public Runnable enableSearch(String option) {
    return null;
  }

  @Override
  protected void processRemovedItems() {
    Set<RemoteServer<?>> servers = new HashSet<>();
    for (MasterDetailsConfigurable<RemoteServer<?>> configurable : getConfiguredServers()) {
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
    for (MasterDetailsConfigurable<RemoteServer<?>> configurable : getConfiguredServers()) {
      RemoteServer<?> server = configurable.getEditableObject();
      server.setName(configurable.getDisplayName());
      if (!servers.contains(server)) {
        myServersManager.addServer(server);
      }
    }
  }

  @Nullable
  @Override
  protected ArrayList<AnAction> createActions(boolean fromPopup) {
    ArrayList<AnAction> actions = new ArrayList<>();
    if (myServerType == null) {
      actions.add(new AddRemoteServerGroup());
    }
    else {
      actions.add(new AddRemoteServerAction(myServerType, PlatformIconGroup.generalAdd()));
    }
    actions.add(new MyDeleteAction());
    return actions;
  }

  @Override
  public boolean needDisplay() {
    return ServerType.EP_NAME.hasAnyExtensions();
  }

  @Override
  protected boolean wasObjectStored(Object editableObject) {
    return true;
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    Object selectedObject = getSelectedObject();
    myLastSelectedServer = selectedObject instanceof RemoteServer<?> ? (RemoteServer)selectedObject : null;
    super.disposeUIResources();
  }

  @Nullable
  public RemoteServer<?> getLastSelectedServer() {
    return myLastSelectedServer;
  }

  private List<MasterDetailsConfigurable<RemoteServer<?>>> getConfiguredServers() {
    List<MasterDetailsConfigurable<RemoteServer<?>>> configurables = new ArrayList<>();
    for (int i = 0; i < myRoot.getChildCount(); i++) {
      MyNode node = (MyNode)myRoot.getChildAt(i);
      configurables.add((MasterDetailsConfigurable<RemoteServer<?>>)node.getConfigurable());
    }
    return configurables;
  }

  private class AddRemoteServerGroup extends ActionGroup implements ActionGroupWithPreselection {
    private AddRemoteServerGroup() {
      super("Add", "", PlatformIconGroup.generalAdd());
      registerCustomShortcutSet(CommonShortcuts.INSERT, myTree);
    }

    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
      List<ServerType> serverTypes = ServerType.EP_NAME.getExtensionList();
      AnAction[] actions = new AnAction[serverTypes.size()];
      for (int i = 0; i < serverTypes.size(); i++) {
        ServerType serverType = serverTypes.get(i);
        actions[i] = new AddRemoteServerAction(serverType, serverType.getIcon());
      }
      return actions;
    }

    @Override
    public ActionGroup getActionGroup() {
      return this;
    }

    @Override
    public int getDefaultIndex() {
      return 0;
    }
  }

  private class AddRemoteServerAction extends DumbAwareAction {
    private final ServerType<?> myServerType;

    private AddRemoteServerAction(ServerType<?> serverType, final Image icon) {
      super(serverType.getPresentableName(), null, icon);
      myServerType = serverType;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      String name = UniqueNameGenerator.generateUniqueName(myServerType.getPresentableName().get(), s -> {
        for (MasterDetailsConfigurable<RemoteServer<?>> configurable : getConfiguredServers()) {
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
