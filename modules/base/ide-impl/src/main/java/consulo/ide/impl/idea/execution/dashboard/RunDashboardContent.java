/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.execution.dashboard;

import consulo.application.ApplicationManager;
import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.ExecutionBundle;
import consulo.fileEditor.structureView.tree.ActionPresentation;
import consulo.ide.impl.idea.execution.dashboard.tree.DashboardGrouper;
import consulo.ide.impl.idea.execution.dashboard.tree.RunDashboardTreeStructure;
import consulo.ui.ex.awt.tree.DefaultTreeExpander;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.AbstractTreeBuilder;
import consulo.ui.ex.awt.tree.NodeRenderer;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.event.ContentManagerAdapter;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.ui.ex.tree.IndexComparator;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author konstantin.aleev
 */
public class RunDashboardContent extends JPanel implements TreeContent, Disposable {
  public static final Key<RunDashboardContent> KEY = Key.create("runDashboardContent");

  @NonNls
  private static final String PLACE_TOOLBAR = "RunDashboardContent#Toolbar";
  @NonNls
  private static final String RUN_DASHBOARD_TOOLBAR = "RunDashboardToolbar";
  @NonNls
  private static final String RUN_DASHBOARD_POPUP = "RunDashboardPopup";

  private static final String MESSAGE_CARD = "message";
  private static final String CONTENT_CARD = "content";

  private final Tree myTree;
  private final CardLayout myDetailsPanelLayout;
  private final JPanel myDetailsPanel;
  private final JBPanelWithEmptyText myMessagePanel;

  private final DefaultTreeModel myTreeModel;
  private AbstractTreeBuilder myBuilder;
  private AbstractTreeNode<?> myLastSelection;
  private Set<Object> myCollapsedTreeNodeValues = new HashSet<>();
  private List<DashboardGrouper> myGroupers;

  @Nonnull
  private final ContentManager myContentManager;
  @Nonnull
  private final ContentManagerListener myContentManagerListener;

  @Nonnull
  private final Project myProject;

  public RunDashboardContent(@Nonnull Project project, @Nonnull ContentManager contentManager, @Nonnull List<DashboardGrouper> groupers) {
    super(new BorderLayout());
    myProject = project;
    myGroupers = groupers;

    myTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
    myTree = new Tree(myTreeModel);
    myTree.setRootVisible(false);

    myTree.setShowsRootHandles(true);
    myTree.setCellRenderer(new NodeRenderer());
    myTree.setLineStyleAngled();

    add(createToolbar(), BorderLayout.WEST);

    Splitter splitter = new OnePixelSplitter(false, 0.3f);
    splitter.setFirstComponent(ScrollPaneFactory.createScrollPane(myTree, SideBorder.LEFT));
    myDetailsPanelLayout = new CardLayout();
    myDetailsPanel = new JPanel(myDetailsPanelLayout);
    myMessagePanel = new JBPanelWithEmptyText().withEmptyText(ExecutionBundle.message("run.dashboard.empty.selection.message"));
    myDetailsPanel.add(MESSAGE_CARD, myMessagePanel);
    splitter.setSecondComponent(myDetailsPanel);
    add(splitter, BorderLayout.CENTER);

    myContentManager = contentManager;
    myContentManagerListener = new ContentManagerAdapter() {
      @Override
      public void selectionChanged(final ContentManagerEvent event) {
        if (ContentManagerEvent.ContentOperation.add != event.getOperation()) {
          return;
        }
        myBuilder.queueUpdate().doWhenDone(() -> myBuilder.accept(DashboardNode.class, new Predicate<DashboardNode>() {
          @Override
          public boolean test(@Nonnull DashboardNode node) {
            if (node.getContent() == event.getContent()) {
              myBuilder.select(node);
            }
            return false;
          }
        }));
        showContentPanel();
      }
    };
    myContentManager.addContentManagerListener(myContentManagerListener);
    myDetailsPanel.add(CONTENT_CARD, myContentManager.getComponent());

    setupBuilder();

    myTree.addTreeSelectionListener(e -> onSelectionChanged());
    myTree.addTreeExpansionListener(new TreeExpansionListener() {
      @Override
      public void treeExpanded(TreeExpansionEvent event) {
        Object value = getNodeValue(event);
        if (value != null) {
          myCollapsedTreeNodeValues.remove(value);
        }
      }

      @Override
      public void treeCollapsed(TreeExpansionEvent event) {
        Object value = getNodeValue(event);
        if (value != null) {
          myCollapsedTreeNodeValues.add(value);
        }
      }

      private Object getNodeValue(TreeExpansionEvent event) {
        DefaultMutableTreeNode treeNode = ObjectUtil.tryCast(event.getPath().getLastPathComponent(), DefaultMutableTreeNode.class);
        if (treeNode == null) {
          return null;
        }
        AbstractTreeNode nodeDescriptor = ObjectUtil.tryCast(treeNode.getUserObject(), AbstractTreeNode.class);
        if (nodeDescriptor == null) {
          return null;
        }
        return nodeDescriptor.getValue();
      }
    });

    DefaultActionGroup popupActionGroup = new DefaultActionGroup();
    popupActionGroup.add(ActionManager.getInstance().getAction(RUN_DASHBOARD_TOOLBAR));
    popupActionGroup.add(ActionManager.getInstance().getAction(RUN_DASHBOARD_POPUP));
    PopupHandler.installPopupHandler(myTree, popupActionGroup, ActionPlaces.RUN_DASHBOARD_POPUP, ActionManager.getInstance());

    new TreeSpeedSearch(myTree, TreeSpeedSearch.NODE_DESCRIPTOR_TOSTRING, true);
  }

  private void onSelectionChanged() {
    Set<AbstractTreeNode> nodes = myBuilder.getSelectedElements(AbstractTreeNode.class);
    if (nodes.size() != 1) {
      showMessagePanel(ExecutionBundle.message("run.dashboard.empty.selection.message"));
      myLastSelection = null;
      return;
    }

    AbstractTreeNode<?> node = nodes.iterator().next();
    if (Comparing.equal(node, myLastSelection)) {
      return;
    }

    myLastSelection = node;
    if (node instanceof DashboardNode) {
      Content content = ((DashboardNode)node).getContent();
      if (content != null) {
        if (content != myContentManager.getSelectedContent()) {
          myContentManager.removeContentManagerListener(myContentManagerListener);
          myContentManager.setSelectedContent(content);
          myContentManager.addContentManagerListener(myContentManagerListener);
        }
        showContentPanel();
        return;
      }
      if (node instanceof DashboardRunConfigurationNode) {
        showMessagePanel(ExecutionBundle.message("run.dashboard.not.started.configuration.message"));
        return;
      }
    }

    showMessagePanel(ExecutionBundle.message("run.dashboard.empty.selection.message"));
  }

  private void showMessagePanel(String text) {
    Content selectedContent = myContentManager.getSelectedContent();
    if (selectedContent != null) {
      myContentManager.removeContentManagerListener(myContentManagerListener);
      myContentManager.removeFromSelection(selectedContent);
      myContentManager.addContentManagerListener(myContentManagerListener);
    }

    myMessagePanel.getEmptyText().setText(text);
    myDetailsPanelLayout.show(myDetailsPanel, MESSAGE_CARD);
  }

  private void showContentPanel() {
    myDetailsPanelLayout.show(myDetailsPanel, CONTENT_CARD);
  }

  private void setupBuilder() {
    RunDashboardTreeStructure structure = new RunDashboardTreeStructure(myProject, myGroupers);
    myBuilder = new AbstractTreeBuilder(myTree, myTreeModel, structure, IndexComparator.INSTANCE) {
      @Override
      protected boolean isAutoExpandNode(NodeDescriptor nodeDescriptor) {
        return super.isAutoExpandNode(nodeDescriptor) || !myCollapsedTreeNodeValues.contains(((AbstractTreeNode)nodeDescriptor).getValue());
      }
    };
    myBuilder.initRootNode();
    Disposer.register(this, myBuilder);
  }

  private JComponent createToolbar() {
    JPanel toolBarPanel = new JPanel(new GridLayout());
    DefaultActionGroup leftGroup = new DefaultActionGroup();
    leftGroup.add(ActionManager.getInstance().getAction(RUN_DASHBOARD_TOOLBAR));
    // TODO [konstantin.aleev] provide context help ID
    //leftGroup.add(new Separator());
    //leftGroup.add(new ContextHelpAction(HELP_ID));

    ActionToolbar leftActionToolBar = ActionManager.getInstance().createActionToolbar(PLACE_TOOLBAR, leftGroup, false);
    toolBarPanel.add(leftActionToolBar.getComponent());

    myTree.putClientProperty(DataManager.CLIENT_PROPERTY_DATA_PROVIDER, new DataProvider() {
      @Override
      public Object getData(@Nonnull @NonNls Key dataId) {
        if (KEY == dataId) {
          return RunDashboardContent.this;
        }
        return null;
      }
    });
    leftActionToolBar.setTargetComponent(myTree);

    DefaultActionGroup rightGroup = new DefaultActionGroup();

    TreeExpander treeExpander = new DefaultTreeExpander(myTree);
    AnAction expandAllAction = CommonActionsManager.getInstance().createExpandAllAction(treeExpander, this);
    rightGroup.add(expandAllAction);

    AnAction collapseAllAction = CommonActionsManager.getInstance().createCollapseAllAction(treeExpander, this);
    rightGroup.add(collapseAllAction);

    rightGroup.add(new AnSeparator());
    myGroupers.stream().filter(grouper -> !grouper.getRule().isAlwaysEnabled()).forEach(grouper -> rightGroup.add(new GroupAction(grouper)));

    ActionToolbar rightActionToolBar = ActionManager.getInstance().createActionToolbar(PLACE_TOOLBAR, rightGroup, false);
    toolBarPanel.add(rightActionToolBar.getComponent());
    rightActionToolBar.setTargetComponent(myTree);
    return toolBarPanel;
  }

  @Override
  public void dispose() {
  }

  public void updateContent(boolean withStructure) {
    ApplicationManager.getApplication().invokeLater(() -> myBuilder.queueUpdate(withStructure).doWhenDone(() -> {
      if (!withStructure) {
        return;
      }
      // Remove nodes not presented in the tree from collapsed node values set.
      // Children retrieving is quick since grouping and run configuration nodes are already constructed.
      Set<Object> nodes = new HashSet<>();
      myBuilder.accept(AbstractTreeNode.class, new Predicate<AbstractTreeNode>() {
        @Override
        public boolean test(@Nonnull AbstractTreeNode node) {
          nodes.add(node.getValue());
          return false;
        }
      });
      myCollapsedTreeNodeValues.retainAll(nodes);
    }), myProject.getDisposed());
  }

  @Override
  @Nonnull
  public AbstractTreeBuilder getBuilder() {
    return myBuilder;
  }

  private class GroupAction extends ToggleAction implements DumbAware {
    private DashboardGrouper myGrouper;

    public GroupAction(DashboardGrouper grouper) {
      super();
      myGrouper = grouper;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      super.update(e);
      Presentation presentation = e.getPresentation();
      ActionPresentation actionPresentation = myGrouper.getRule().getPresentation();
      presentation.setText(actionPresentation.getText());
      presentation.setDescription(actionPresentation.getDescription());
      presentation.setIcon(actionPresentation.getIcon());
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      return myGrouper.isEnabled();
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      myGrouper.setEnabled(state);
      updateContent(true);
    }
  }
}
