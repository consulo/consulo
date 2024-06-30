/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.errorTreeView;

import consulo.application.AllIcons;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposer;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.ide.impl.idea.ide.OccurenceNavigatorSupport;
import consulo.ide.impl.idea.ide.actions.ExportToTextFileToolbarAction;
import consulo.ide.impl.idea.ide.actions.NextOccurenceToolbarAction;
import consulo.ide.impl.idea.ide.actions.PreviousOccurenceToolbarAction;
import consulo.ide.impl.idea.ide.errorTreeView.impl.ErrorTreeViewConfiguration;
import consulo.ide.impl.idea.ide.errorTreeView.impl.ErrorViewTextExporter;
import consulo.language.editor.PlatformDataKeys;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.navigation.Navigatable;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.project.ui.view.MessageView;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.errorTreeView.ErrorTreeElementKind;
import consulo.ui.ex.errorTreeView.HotfixData;
import consulo.ui.ex.errorTreeView.NewErrorTreeViewPanel;
import consulo.ui.ex.errorTreeView.SimpleErrorData;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

public class NewErrorTreeViewPanelImpl extends JPanel implements DataProvider, NewErrorTreeViewPanel, CopyProvider {
  protected static final Logger LOG = Logger.getInstance(NewErrorTreeViewPanelImpl.class);
  private volatile String myProgressText = "";
  private volatile float myFraction = 0.0f;
  private ErrorViewStructure myErrorViewStructure;
  private ErrorViewTreeBuilder myBuilder;
  private final Alarm myUpdateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
  private volatile boolean myIsDisposed = false;
  private final ErrorTreeViewConfiguration myConfiguration;

  private ActionToolbar myToolbar;
  private final TreeExpander myTreeExpander = new MyTreeExpander();
  private ExporterToTextFile myExporterToTextFile;
  protected Project myProject;
  private String myHelpId;
  protected Tree myTree;
  private JPanel myMessagePanel;
  private ProcessController myProcessController;

  private JLabel myProgressLabel;
  private JPanel myProgressPanel;

  private AutoScrollToSourceHandler myAutoScrollToSourceHandler;
  private MyOccurenceNavigatorSupport myOccurenceNavigatorSupport;

  private boolean myCanHideWarningsOrInfos = true;

  public NewErrorTreeViewPanelImpl(Project project, String helpId) {
    this(project, helpId, true);
  }

  public NewErrorTreeViewPanelImpl(Project project, String helpId, boolean createExitAction) {
    this(project, helpId, createExitAction, true);
  }

  public NewErrorTreeViewPanelImpl(Project project, String helpId, boolean createExitAction, boolean createToolbar) {
    this(project, helpId, createExitAction, createToolbar, null);
  }

  public NewErrorTreeViewPanelImpl(
    Project project,
    String helpId,
    boolean createExitAction,
    boolean createToolbar,
    @Nullable Runnable rerunAction
  ) {
    myProject = project;
    myHelpId = helpId;
    myConfiguration = ErrorTreeViewConfiguration.getInstance(project);
    setLayout(new BorderLayout());

    myAutoScrollToSourceHandler = new AutoScrollToSourceHandler() {
      @Override
      protected boolean isAutoScrollMode() {
        return myConfiguration.isAutoscrollToSource();
      }

      @Override
      protected void setAutoScrollMode(boolean state) {
        myConfiguration.setAutoscrollToSource(state);
      }
    };

    myMessagePanel = new JPanel(new BorderLayout());

    myErrorViewStructure = new ErrorViewStructure(project, canHideWarningsOrInfos());
    DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    root.setUserObject(myErrorViewStructure.createDescriptor(myErrorViewStructure.getRootElement(), null));
    final DefaultTreeModel treeModel = new DefaultTreeModel(root);
    myTree = new Tree(treeModel) {
      @Override
      public void setRowHeight(int i) {
        super.setRowHeight(0);
        // this is needed in order to make UI calculate the height for each particular row
      }
    };
    myBuilder = new ErrorViewTreeBuilder(myTree, treeModel, myErrorViewStructure);

    myExporterToTextFile = new ErrorViewTextExporter(myErrorViewStructure);
    myOccurenceNavigatorSupport = new MyOccurenceNavigatorSupport(myTree);

    myAutoScrollToSourceHandler.install(myTree);
    TreeUtil.installActions(myTree);
    UIUtil.setLineStyleAngled(myTree);
    myTree.setRootVisible(false);
    myTree.setShowsRootHandles(true);
    myTree.setLargeModel(true);

    JScrollPane scrollPane = NewErrorTreeRenderer.install(myTree);
    scrollPane.setBorder(IdeBorderFactory.createBorder(SideBorder.LEFT));
    myMessagePanel.add(scrollPane, BorderLayout.CENTER);

    if (createToolbar) {
      add(createToolbarPanel(rerunAction), BorderLayout.WEST);
    }

    add(myMessagePanel, BorderLayout.CENTER);

    myTree.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          navigateToSource(false);
        }
      }
    });

    myTree.addMouseListener(new PopupHandler() {
      @Override
      public void invokePopup(Component comp, int x, int y) {
        popupInvoked(comp, x, y);
      }
    });

    EditSourceOnDoubleClickHandler.install(myTree);
  }

  @Override
  public void dispose() {
    myIsDisposed = true;
    myErrorViewStructure.clear();
    myUpdateAlarm.cancelAllRequests();
    Disposer.dispose(myUpdateAlarm);
    Disposer.dispose(myBuilder);
  }

  @Override
  public void performCopy(@Nonnull DataContext dataContext) {
    final ErrorTreeNodeDescriptor descriptor = getSelectedNodeDescriptor();
    if (descriptor != null) {
      final String[] lines = descriptor.getElement().getText();
      CopyPasteManager.getInstance().setContents(new StringSelection(StringUtil.join(lines, "\n")));
    }
  }

  @Override
  public boolean isCopyEnabled(@Nonnull DataContext dataContext) {
    return getSelectedNodeDescriptor() != null;
  }

  @Override
  public boolean isCopyVisible(@Nonnull DataContext dataContext) {
    return true;
  }

  @Override
  public Object getData(@Nonnull Key<?> dataId) {
    if (PlatformDataKeys.COPY_PROVIDER == dataId) {
      return this;
    }
    if (Navigatable.KEY == dataId) {
      final NavigatableMessageElement selectedMessageElement = getSelectedMessageElement();
      return selectedMessageElement != null ? selectedMessageElement.getNavigatable() : null;
    }
    else if (PlatformDataKeys.HELP_ID == dataId) {
      return myHelpId;
    }
    else if (PlatformDataKeys.TREE_EXPANDER == dataId) {
      return myTreeExpander;
    }
    else if (PlatformDataKeys.EXPORTER_TO_TEXT_FILE == dataId) {
      return myExporterToTextFile;
    }
    else if (CURRENT_EXCEPTION_DATA_KEY == dataId) {
      NavigatableMessageElement selectedMessageElement = getSelectedMessageElement();
      return selectedMessageElement != null ? selectedMessageElement.getData() : null;
    }
    return null;
  }

  public void selectFirstMessage() {
    final ErrorTreeElement firstError = myErrorViewStructure.getFirstMessage(ErrorTreeElementKind.ERROR);
    if (firstError != null) {
      selectElement(firstError, () -> {
        if (shouldShowFirstErrorInEditor()) {
          navigateToSource(false);
        }
      });
    }
    else {
      ErrorTreeElement firstWarning = myErrorViewStructure.getFirstMessage(ErrorTreeElementKind.WARNING);
      if (firstWarning == null) firstWarning = myErrorViewStructure.getFirstMessage(ErrorTreeElementKind.NOTE);

      if (firstWarning != null) {
        selectElement(firstWarning, null);
      }
      else {
        TreeUtil.selectFirstNode(myTree);
      }
    }
  }

  public boolean containsErrorMessages() {
    return myErrorViewStructure.getFirstMessage(ErrorTreeElementKind.ERROR) != null;
  }

  private void selectElement(final ErrorTreeElement element, final Runnable onDone) {
    myBuilder.select(element, onDone);
  }

  protected boolean shouldShowFirstErrorInEditor() {
    return false;
  }

  public void clearMessages() {
    myErrorViewStructure.clear();
    myBuilder.updateTree();
  }

  public void updateTree() {
    if (!myIsDisposed) {
      myBuilder.updateTree();
    }
  }

  @Override
  public void addMessage(int type, @Nonnull String[] text, @Nullable VirtualFile file, int line, int column, @Nullable Object data) {
    addMessage(type, text, null, file, line, column, data);
  }

  @Override
  public void addMessage(int type, @Nonnull String[] text, @Nullable VirtualFile underFileGroup, @Nullable VirtualFile file, int line, int column, @Nullable Object data) {
    if (myIsDisposed) {
      return;
    }
    myErrorViewStructure.addMessage(ErrorTreeElementKind.convertMessageFromCompilerErrorType(type), text, underFileGroup, file, line, column, data);
    myBuilder.updateTree();
  }

  @Override
  public void addMessage(int type,
                         @Nonnull String[] text,
                         @Nullable String groupName,
                         @Nonnull Navigatable navigatable,
                         @Nullable String exportTextPrefix,
                         @Nullable String rendererTextPrefix,
                         @Nullable Object data) {
    if (myIsDisposed) {
      return;
    }
    VirtualFile file = data instanceof VirtualFile ? (VirtualFile)data : null;
    if (file == null && navigatable instanceof OpenFileDescriptorImpl) {
      file = ((OpenFileDescriptorImpl)navigatable).getFile();
    }
    final String exportPrefix = exportTextPrefix == null ? "" : exportTextPrefix;
    final String renderPrefix = rendererTextPrefix == null ? "" : rendererTextPrefix;
    final ErrorTreeElementKind kind = ErrorTreeElementKind.convertMessageFromCompilerErrorType(type);
    myErrorViewStructure.addNavigatableMessage(groupName, navigatable, kind, text, data, exportPrefix, renderPrefix, file);
    myBuilder.updateTree();
  }

  public ErrorViewStructure getErrorViewStructure() {
    return myErrorViewStructure;
  }

  public static String createExportPrefix(int line) {
    return line < 0 ? "" : IdeLocalize.errortreePrefixLine(line).get();
  }

  public static String createRendererPrefix(int line, int column) {
    if (line < 0) return "";
    if (column < 0) return "(" + line + ")";
    return "(" + line + ", " + column + ")";
  }

  @Override
  @Nonnull
  public JComponent getComponent() {
    return this;
  }

  @Nullable
  private NavigatableMessageElement getSelectedMessageElement() {
    final ErrorTreeElement selectedElement = getSelectedErrorTreeElement();
    return selectedElement instanceof NavigatableMessageElement ? (NavigatableMessageElement)selectedElement : null;
  }

  @Nullable
  public ErrorTreeElement getSelectedErrorTreeElement() {
    final ErrorTreeNodeDescriptor treeNodeDescriptor = getSelectedNodeDescriptor();
    return treeNodeDescriptor == null ? null : treeNodeDescriptor.getElement();
  }

  @Nullable
  public ErrorTreeNodeDescriptor getSelectedNodeDescriptor() {
    TreePath path = myTree.getSelectionPath();
    if (path == null) {
      return null;
    }
    DefaultMutableTreeNode lastPathNode = (DefaultMutableTreeNode)path.getLastPathComponent();
    Object userObject = lastPathNode.getUserObject();
    if (!(userObject instanceof ErrorTreeNodeDescriptor)) {
      return null;
    }
    return (ErrorTreeNodeDescriptor)userObject;
  }

  private void navigateToSource(final boolean focusEditor) {
    NavigatableMessageElement element = getSelectedMessageElement();
    if (element == null) {
      return;
    }
    final Navigatable navigatable = element.getNavigatable();
    if (navigatable.canNavigate()) {
      navigatable.navigate(focusEditor);
    }
  }

  public static String getQualifiedName(final VirtualFile file) {
    return file.getPresentableUrl();
  }

  private void popupInvoked(Component component, int x, int y) {
    final TreePath path = myTree.getLeadSelectionPath();
    if (path == null) {
      return;
    }
    ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
    if (getData(PlatformDataKeys.NAVIGATABLE) != null) {
      group.add(ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_SOURCE));
    }
    group.add(ActionManager.getInstance().getAction(IdeActions.ACTION_COPY));
    addExtraPopupMenuActions(group);

    ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.COMPILER_MESSAGES_POPUP, group.build());
    menu.getComponent().show(component, x, y);
  }

  protected void addExtraPopupMenuActions(ActionGroup.Builder group) {
  }

  @Override
  public void setProcessController(ProcessController controller) {
    myProcessController = controller;
  }

  @Override
  public void stopProcess() {
    myProcessController.stopProcess();
  }

  @Override
  public boolean canControlProcess() {
    return myProcessController != null;
  }

  @Override
  public boolean isProcessStopped() {
    return myProcessController.isProcessStopped();
  }

  private void close() {
    MessageView messageView = MessageView.getInstance(myProject);
    Content content = messageView.getContentManager().getContent(this);
    if (content != null) {
      messageView.getContentManager().removeContent(content, true);
    }
  }

  public void setProgress(final String s, float fraction) {
    initProgressPanel();
    myProgressText = s;
    myFraction = fraction;
    updateProgress();
  }

  public void setProgressText(final String s) {
    initProgressPanel();
    myProgressText = s;
    updateProgress();
  }

  public void setFraction(final float fraction) {
    initProgressPanel();
    myFraction = fraction;
    updateProgress();
  }

  public void clearProgressData() {
    if (myProgressPanel != null) {
      myProgressText = " ";
      myFraction = 0.0f;
      updateProgress();
    }
  }

  private void updateProgress() {
    if (myIsDisposed) {
      return;
    }
    myUpdateAlarm.cancelAllRequests();
    myUpdateAlarm.addRequest(
      () -> {
        final float fraction = myFraction;
        final String text = myProgressText;
        if (fraction > 0.0f) {
          myProgressLabel.setText((int)(fraction * 100 + 0.5) + "%  " + text);
        }
        else {
          myProgressLabel.setText(text);
        }
      },
      50,
      IdeaModalityState.nonModal()
    );
  }

  private void initProgressPanel() {
    if (myProgressPanel == null) {
      myProgressPanel = new JPanel(new GridLayout(1, 2));
      myProgressLabel = new JLabel();
      myProgressPanel.add(myProgressLabel);
      //JLabel secondLabel = new JLabel();
      //myProgressPanel.add(secondLabel);
      myMessagePanel.add(myProgressPanel, BorderLayout.SOUTH);
      myMessagePanel.validate();
    }
  }

  public void collapseAll() {
    TreeUtil.collapseAll(myTree, 2);
  }


  public void expandAll() {
    TreePath[] selectionPaths = myTree.getSelectionPaths();
    TreePath leadSelectionPath = myTree.getLeadSelectionPath();
    int row = 0;
    while (row < myTree.getRowCount()) {
      myTree.expandRow(row);
      row++;
    }

    if (selectionPaths != null) {
      // restore selection
      myTree.setSelectionPaths(selectionPaths);
    }
    if (leadSelectionPath != null) {
      // scroll to lead selection path
      myTree.scrollPathToVisible(leadSelectionPath);
    }
  }

  private JComponent createToolbarPanel(@Nullable Runnable rerunAction) {
    ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
    addActionsBefore(group);
    if (rerunAction != null) {
      group.add(new RerunAction(rerunAction));
    }

    group.add(new StopAction());


    group.add(new PreviousOccurenceToolbarAction(this));
    group.add(new NextOccurenceToolbarAction(this));

    group.add(CommonActionsManager.getInstance().createExpandAllAction(myTreeExpander, this));
    group.add(CommonActionsManager.getInstance().createCollapseAllAction(myTreeExpander, this));

    if (canHideWarningsOrInfos()) {
      group.add(new ShowInfosAction());
      group.add(new ShowWarningsAction());
    }
    group.add(myAutoScrollToSourceHandler.createToggleAction());

    addActionsBefore(group);

    group.add(new ExportToTextFileToolbarAction(myExporterToTextFile));

    addActionsAfter(group);

    myToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.COMPILER_MESSAGES_TOOLBAR, group.build(), false);
    myToolbar.setTargetComponent(myMessagePanel);

    return myToolbar.getComponent();
  }

  protected void addActionsBefore(ActionGroup.Builder group) {
  }

  public void addActionsAfter(ActionGroup.Builder group) {
  }

  @Override
  public OccurenceInfo goNextOccurence() {
    return myOccurenceNavigatorSupport.goNextOccurence();
  }

  @Override
  public OccurenceInfo goPreviousOccurence() {
    return myOccurenceNavigatorSupport.goPreviousOccurence();
  }

  @Override
  public boolean hasNextOccurence() {
    return myOccurenceNavigatorSupport.hasNextOccurence();
  }

  @Override
  public boolean hasPreviousOccurence() {
    return myOccurenceNavigatorSupport.hasPreviousOccurence();
  }

  @Override
  public String getNextOccurenceActionName() {
    return myOccurenceNavigatorSupport.getNextOccurenceActionName();
  }

  @Override
  public String getPreviousOccurenceActionName() {
    return myOccurenceNavigatorSupport.getPreviousOccurenceActionName();
  }

  private class RerunAction extends AnAction {
    private final Runnable myRerunAction;

    public RerunAction(@Nonnull Runnable rerunAction) {
      super(IdeLocalize.actionRefresh(), LocalizeValue.empty(), AllIcons.Actions.Rerun);
      myRerunAction = rerunAction;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      close();

      myRerunAction.run();
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent event) {
      final Presentation presentation = event.getPresentation();
      presentation.setEnabled(canControlProcess() && isProcessStopped());
    }
  }

  private class StopAction extends AnAction {
    public StopAction() {
      super(IdeLocalize.actionStop(), LocalizeValue.empty(), AllIcons.Actions.Suspend);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      if (canControlProcess()) {
        stopProcess();
      }
      myToolbar.updateActionsImmediately();
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent event) {
      Presentation presentation = event.getPresentation();
      presentation.setEnabled(canControlProcess() && !isProcessStopped());
      presentation.setVisible(canControlProcess());
    }
  }

  protected boolean canHideWarningsOrInfos() {
    return myCanHideWarningsOrInfos;
  }

  @Override
  public void setCanHideWarningsOrInfos(boolean canHideWarningsOrInfos) {
    myCanHideWarningsOrInfos = canHideWarningsOrInfos;
  }

  private class ShowInfosAction extends ToggleAction {
    public ShowInfosAction() {
      super(IdeLocalize.actionHideInfos(), LocalizeValue.empty(), AllIcons.General.BalloonInformation);
    }

    @Override
    public boolean isSelected(AnActionEvent event) {
      return myConfiguration.SHOW_INFOS;
    }

    @Override
    public void setSelected(AnActionEvent event, boolean flag) {
      if (myConfiguration.SHOW_INFOS != flag) {
        myConfiguration.SHOW_INFOS = flag;
        myBuilder.updateTree();
      }
    }

    @RequiredUIAccess
    @Override
    public void update(AnActionEvent e) {
      super.update(e);
      e.getPresentation().setTextValue(isSelected(e) ? IdeLocalize.actionHideInfos() : IdeLocalize.actionShowInfos());
    }
  }

  private class ShowWarningsAction extends ToggleAction {
    public ShowWarningsAction() {
      super(IdeLocalize.actionHideWarnings(), LocalizeValue.empty(), AllIcons.General.BalloonWarning);
    }

    @Override
    public boolean isSelected(AnActionEvent event) {
      return myConfiguration.SHOW_WARNINGS;
    }

    @Override
    public void setSelected(AnActionEvent event, boolean flag) {
      if (myConfiguration.SHOW_WARNINGS != flag) {
        myConfiguration.SHOW_WARNINGS = flag;
        myBuilder.updateTree();
      }
    }

    @RequiredUIAccess
    @Override
    public void update(AnActionEvent e) {
      super.update(e);
      e.getPresentation().setTextValue(isSelected(e) ? IdeLocalize.actionHideWarnings() : IdeLocalize.actionShowWarnings());
    }
  }

  private class MyTreeExpander implements TreeExpander {
    @Override
    public void expandAll() {
      NewErrorTreeViewPanelImpl.this.expandAll();
    }

    @Override
    public boolean canExpand() {
      return true;
    }

    @Override
    public void collapseAll() {
      NewErrorTreeViewPanelImpl.this.collapseAll();
    }

    @Override
    public boolean canCollapse() {
      return true;
    }
  }

  private static class MyOccurenceNavigatorSupport extends OccurenceNavigatorSupport {
    public MyOccurenceNavigatorSupport(final Tree tree) {
      super(tree);
    }

    @Override
    protected Navigatable createDescriptorForNode(DefaultMutableTreeNode node) {
      Object userObject = node.getUserObject();
      if (!(userObject instanceof ErrorTreeNodeDescriptor)) {
        return null;
      }
      final ErrorTreeNodeDescriptor descriptor = (ErrorTreeNodeDescriptor)userObject;
      final ErrorTreeElement element = descriptor.getElement();
      if (element instanceof NavigatableMessageElement) {
        return ((NavigatableMessageElement)element).getNavigatable();
      }
      return null;
    }

    @Override
    public String getNextOccurenceActionName() {
      return IdeLocalize.actionNextMessage().get();
    }

    @Override
    public String getPreviousOccurenceActionName() {
      return IdeLocalize.actionPreviousMessage().get();
    }
  }

  @Override
  public List<Object> getGroupChildrenData(final String groupName) {
    return myErrorViewStructure.getGroupChildrenData(groupName);
  }

  @Override
  public void removeGroup(final String name) {
    myErrorViewStructure.removeGroup(name);
  }

  @Override
  public void addFixedHotfixGroup(String text, List<SimpleErrorData> children) {
    myErrorViewStructure.addFixedHotfixGroup(text, children);
  }

  @Override
  public void addHotfixGroup(HotfixData hotfixData, List<SimpleErrorData> children) {
    myErrorViewStructure.addHotfixGroup(hotfixData, children, this);
  }

  @Override
  public void reload() {
    myBuilder.updateTree();
  }
}
