package consulo.remoteServer.impl.internal.ui;

import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.impl.internal.runtime.log.LoggingHandlerImpl;
import consulo.remoteServer.impl.internal.ui.tree.DeploymentNode;
import consulo.remoteServer.impl.internal.ui.tree.ServerNode;
import consulo.remoteServer.impl.internal.ui.tree.ServersTreeStructure;
import consulo.remoteServer.runtime.ConnectionStatus;
import consulo.remoteServer.runtime.ServerConnection;
import consulo.remoteServer.runtime.ServerConnectionListener;
import consulo.remoteServer.runtime.ServerConnectionManager;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.tree.NodeRenderer;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeNode;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.util.lang.EmptyRunnable;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Created by IntelliJ IDEA.
 * User: michael.golubev
 */
public class ServersToolWindowContent extends JPanel implements Disposable {
  public static final Key<ServersToolWindowContent> KEY = Key.create("serversToolWindowContent");
  @NonNls private static final String PLACE_TOOLBAR = "ServersToolWindowContent#Toolbar";
  @NonNls private static final String SERVERS_TOOL_WINDOW_TOOLBAR = "RemoteServersViewToolbar";

  @NonNls
  private static final String HELP_ID = "Application_Servers_tool_window";
  private static final String MESSAGE_CARD = "message";
  private static final String EMPTY_SELECTION_MESSAGE = "Select a server or deployment in the tree to view details";

  private final Tree myTree;
  private final CardLayout myPropertiesPanelLayout;
  private final JPanel myPropertiesPanel;
  private final JLabel myMessageLabel;
  private final Map<String, JComponent> myLogComponents = new HashMap<String, JComponent>();

  private final DefaultTreeModel myTreeModel;
  private TreeBuilderBase myBuilder;
  private AbstractTreeNode<?> myLastSelection;

  private final Project myProject;

  public ServersToolWindowContent(@Nonnull Project project) {
    super(new BorderLayout());
    myProject = project;

    myTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
    myTree = new Tree(myTreeModel);
    myTree.setRootVisible(false);

    myTree.setShowsRootHandles(true);
    myTree.setCellRenderer(new NodeRenderer());
    myTree.setLineStyleAngled();

    getMainPanel().add(createToolbar(), BorderLayout.WEST);
    Splitter splitter = new OnePixelSplitter(false, 0.3f);
    splitter.setFirstComponent(ScrollPaneFactory.createScrollPane(myTree, SideBorder.LEFT));
    myPropertiesPanelLayout = new CardLayout();
    myPropertiesPanel = new JPanel(myPropertiesPanelLayout);
    myMessageLabel = new JLabel(EMPTY_SELECTION_MESSAGE, SwingConstants.CENTER);
    myPropertiesPanel.add(MESSAGE_CARD, new Wrapper(myMessageLabel));
    splitter.setSecondComponent(myPropertiesPanel);
    getMainPanel().add(splitter, BorderLayout.CENTER);

    setupBuilder(project);

    for (RemoteServersViewContributor contributor : RemoteServersViewContributor.EP_NAME.getExtensionList()) {
      contributor.setupTree(myProject, myTree, myBuilder);
    }

    myTree.addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        onSelectionChanged();
      }
    });
    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(MouseEvent event) {
        Set<ServersTreeStructure.RemoteServerNode> nodes = getSelectedRemoteServerNodes();
        if (nodes.size() == 1) {
          RemoteServer<?> server = nodes.iterator().next().getValue();
          ServerConnectionManager.getInstance().getOrCreateConnection(server).computeDeployments(EmptyRunnable.INSTANCE);
          return true;
        }
        return false;
      }
    }.installOn(myTree);
  }

  private void onSelectionChanged() {
    Set<AbstractTreeNode> nodes = myBuilder.getSelectedElements(AbstractTreeNode.class);
    if (nodes.size() != 1) {
      showMessageLabel(EMPTY_SELECTION_MESSAGE);
      myLastSelection = null;
      return;
    }

    AbstractTreeNode<?> node = nodes.iterator().next();
    if (Comparing.equal(node, myLastSelection)) {
      return;
    }

    myLastSelection = node;
    if (node instanceof ServersTreeStructure.LogProvidingNode) {
      ServersTreeStructure.LogProvidingNode logNode = (ServersTreeStructure.LogProvidingNode)node;
      LoggingHandlerImpl loggingHandler = logNode.getLoggingHandler();
      if (loggingHandler != null) {
        String cardName = logNode.getLogId();
        JComponent oldComponent = myLogComponents.get(cardName);
        JComponent logComponent = loggingHandler.getConsole().getComponent();
        if (!logComponent.equals(oldComponent)) {
          myLogComponents.put(cardName, logComponent);
          if (oldComponent != null) {
            myPropertiesPanel.remove(oldComponent);
          }
          myPropertiesPanel.add(cardName, logComponent);
        }
        myPropertiesPanelLayout.show(myPropertiesPanel, cardName);
      }
    }
    else if (node instanceof ServersTreeStructure.RemoteServerNode) {
      updateServerDetails((ServersTreeStructure.RemoteServerNode)node);
    }
    else {
      showMessageLabel("");
    }
  }

  private void updateServerDetails(ServersTreeStructure.RemoteServerNode node) {
    RemoteServer<?> server = ((ServersTreeStructure.RemoteServerNode)node).getValue();
    ServerConnection connection = ServerConnectionManager.getInstance().getConnection(server);
    if (connection == null || connection.getStatus() == ConnectionStatus.DISCONNECTED) {
      showMessageLabel("Double-click on the server node to connect");
    }
    else {
      showMessageLabel(connection.getStatusText());
    }
  }

  private void showMessageLabel(final String text) {
    myMessageLabel.setText(UIUtil.toHtml(text));
    myPropertiesPanelLayout.show(myPropertiesPanel, MESSAGE_CARD);
  }

  private void setupBuilder(final @Nonnull Project project) {
    ServersTreeStructure structure = new ServersTreeStructure(project);
    myBuilder = new TreeBuilderBase(myTree, structure, myTreeModel) {
      @Override
      protected boolean isAutoExpandNode(NodeDescriptor nodeDescriptor) {
        return nodeDescriptor instanceof ServersTreeStructure.RemoteServerNode || nodeDescriptor instanceof ServersTreeStructure.DeploymentNodeImpl;
      }
    };
    Disposer.register(this, myBuilder);

    project.getMessageBus().connect().subscribe(ServerConnectionListener.class, new ServerConnectionListener() {
      @Override
      public void onConnectionCreated(@Nonnull ServerConnection<?> connection) {
        getBuilder().queueUpdate();
      }

      @Override
      public void onConnectionStatusChanged(@Nonnull ServerConnection<?> connection) {
        getBuilder().queueUpdate();
        updateSelectedServerDetails();
      }

      @Override
      public void onDeploymentsChanged(@Nonnull ServerConnection<?> connection) {
        getBuilder().queueUpdate();
        updateSelectedServerDetails();
      }
    });
  }

  private void updateSelectedServerDetails() {
    if (myLastSelection instanceof ServersTreeStructure.RemoteServerNode) {
      updateServerDetails((ServersTreeStructure.RemoteServerNode)myLastSelection);
    }
  }

  private JComponent createToolbar() {
    DefaultActionGroup group = new DefaultActionGroup();
    group.add(ActionManager.getInstance().getAction(SERVERS_TOOL_WINDOW_TOOLBAR));
    group.add(AnSeparator.getInstance());
    group.add(new ContextHelpAction(HELP_ID));

    ActionToolbar actionToolBar = ActionManager.getInstance().createActionToolbar(PLACE_TOOLBAR, group, false);


    myTree.putClientProperty(DataManager.CLIENT_PROPERTY_DATA_PROVIDER, new DataProvider() {

      @Override
      public Object getData(@Nonnull Key dataId) {
        if (KEY == dataId) {
          return ServersToolWindowContent.this;
        }
        for (RemoteServersViewContributor contributor : RemoteServersViewContributor.EP_NAME.getExtensionList()) {
          Object data = contributor.getData(dataId, ServersToolWindowContent.this);
          if (data != null) {
            return data;
          }
        }
        return null;
      }
    });
    actionToolBar.setTargetComponent(myTree);
    return actionToolBar.getComponent();
  }

  public JPanel getMainPanel() {
    return this;
  }

  public Set<ServerNode> getSelectedServerNodes() {
    return myBuilder.getSelectedElements(ServerNode.class);
  }

  public Set<DeploymentNode> getSelectedDeploymentNodes() {
    return myBuilder.getSelectedElements(DeploymentNode.class);
  }

  public Set<ServersTreeStructure.RemoteServerNode> getSelectedRemoteServerNodes() {
    return myBuilder.getSelectedElements(ServersTreeStructure.RemoteServerNode.class);
  }

  @Override
  public void dispose() {
  }

  public TreeBuilderBase getBuilder() {
    return myBuilder;
  }

  @Nonnull
  public Project getProject() {
    return myProject;
  }

  public void select(@Nonnull final ServerConnection<?> connection) {
    myBuilder.select(ServersTreeStructure.RemoteServerNode.class, new Predicate<ServersTreeStructure.RemoteServerNode>() {
      @Override
      public boolean test(@Nonnull ServersTreeStructure.RemoteServerNode node) {
        return node.getValue().equals(connection.getServer());
      }
    }, null, false);
  }

  public void select(@Nonnull final ServerConnection<?> connection, @Nonnull final String deploymentName) {
    myBuilder.getUi().queueUpdate(connection).doWhenDone(new Runnable() {
      @Override
      public void run() {
        myBuilder.select(ServersTreeStructure.DeploymentNodeImpl.class, new Predicate<ServersTreeStructure.DeploymentNodeImpl>() {
          @Override
          public boolean test(@Nonnull ServersTreeStructure.DeploymentNodeImpl node) {
            TreeNode parent = node.getParent();
            return parent instanceof ServersTreeStructure.RemoteServerNode &&
                   ((ServersTreeStructure.RemoteServerNode)parent).getValue().equals(connection.getServer())
                   && node.getValue().getName().equals(deploymentName);
          }
        }, null, false);
      }
    });
  }
}
