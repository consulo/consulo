/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.ide.impl.idea.profile.codeInspection.ui;

import consulo.application.AllIcons;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.configurable.ConfigurationException;
import consulo.content.scope.NamedScope;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.codeInsight.daemon.impl.SeverityUtil;
import consulo.ide.impl.idea.codeInspection.ex.AppInspectionProfilesVisibleTreeState;
import consulo.ide.impl.idea.codeInspection.ex.Descriptor;
import consulo.ide.impl.idea.codeInspection.ex.ProjectInspectionProfilesVisibleTreeState;
import consulo.ide.impl.idea.codeInspection.ex.VisibleTreeState;
import consulo.ide.impl.idea.ide.ui.search.SearchUtil;
import consulo.configurable.SearchableOptionsRegistrar;
import consulo.ide.impl.idea.openapi.util.JDOMUtil;
import consulo.ide.impl.idea.profile.codeInspection.InspectionProjectProfileManager;
import consulo.ide.impl.idea.profile.codeInspection.ui.filter.InspectionFilterAction;
import consulo.ide.impl.idea.profile.codeInspection.ui.filter.InspectionsFilter;
import consulo.ide.impl.idea.profile.codeInspection.ui.inspectionsTree.InspectionConfigTreeNode;
import consulo.ide.impl.idea.profile.codeInspection.ui.inspectionsTree.InspectionsConfigTreeComparator;
import consulo.ide.impl.idea.profile.codeInspection.ui.inspectionsTree.InspectionsConfigTreeRenderer;
import consulo.ide.impl.idea.profile.codeInspection.ui.inspectionsTree.InspectionsConfigTreeTable;
import consulo.ide.impl.idea.profile.codeInspection.ui.table.ScopesAndSeveritiesTable;
import consulo.application.util.StorageAccessors;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.impl.internal.inspection.scheme.DefaultProjectProfileManager;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionToolRegistrar;
import consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.scheme.*;
import consulo.language.editor.internal.inspection.ScopeToolState;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.SeverityProvider;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.ui.ex.awt.internal.GuiUtils;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.DefaultTreeExpander;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.TableUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.*;

/**
 * User: anna
 * Date: 31-May-2006
 */
public class SingleInspectionProfilePanel extends JPanel {
  private static final Logger LOG = Logger.getInstance(SingleInspectionProfilePanel.class);
  private static final String INSPECTION_FILTER_HISTORY = "INSPECTION_FILTER_HISTORY";
  private static final String EMPTY_HTML = "<html><body></body></html>";
  private static final String VERTICAL_DIVIDER_PROPORTION = "VERTICAL_DIVIDER_PROPORTION";
  private static final String HORIZONTAL_DIVIDER_PROPORTION = "HORIZONTAL_DIVIDER_PROPORTION";
  private final List<ToolDescriptors> myInitialToolDescriptors = new ArrayList<>();
  private final InspectionConfigTreeNode myRoot = new InspectionConfigTreeNode(InspectionLocalize.inspectionRootNodeTitle().get());
  private final Alarm myAlarm = new Alarm();
  private final StorageAccessors myProperties = StorageAccessors.createGlobal("SingleInspectionProfilePanel");
  private final InspectionProjectProfileManager myProjectProfileManager;
  private InspectionProfileImpl mySelectedProfile;
  private JEditorPane myBrowser;
  private JPanel myOptionsPanel;
  private JPanel myInspectionProfilePanel = null;
  private FilterComponent myProfileFilter;
  private final InspectionsFilter myInspectionsFilter = new InspectionsFilter() {
    @Override
    protected void filterChanged() {
      // dont change if we dont visible
      if (myProfileFilter == null) {
        return;
      }
      filterTree(myProfileFilter.getFilter());
    }
  };
  private boolean myModified = false;
  private InspectionsConfigTreeTable myTreeTable;
  private TreeExpander myTreeExpander;
  @Nonnull
  private String myCurrentProfileName;
  private boolean myIsInRestore = false;
  private boolean myShareProfile;
  private Splitter myRightSplitter;
  private Splitter myMainSplitter;

  private String[] myInitialScopesOrder;
  private Disposable myDisposable = Disposable.newDisposable();

  public SingleInspectionProfilePanel(
    @Nonnull InspectionProjectProfileManager projectProfileManager,
    @Nonnull String inspectionProfileName,
    @Nonnull ModifiableModel profile
  ) {
    super(new BorderLayout());
    myProjectProfileManager = projectProfileManager;
    mySelectedProfile = (InspectionProfileImpl)profile;
    myCurrentProfileName = inspectionProfileName;
    myShareProfile = profile.getProfileManager() == projectProfileManager;
  }

  private static VisibleTreeState getExpandedNodes(InspectionProfileImpl profile) {
    if (profile.getProfileManager() instanceof ApplicationProfileManager) {
      return AppInspectionProfilesVisibleTreeState.getInstance().getVisibleTreeState(profile);
    }
    else {
      DefaultProjectProfileManager projectProfileManager = (DefaultProjectProfileManager)profile.getProfileManager();
      return ProjectInspectionProfilesVisibleTreeState.getInstance(projectProfileManager.getProject()).getVisibleTreeState(profile);
    }
  }

  @NonNls
  @Nullable
  @RequiredUIAccess
  public static ModifiableModel createNewProfile(
    final int initValue,
    ModifiableModel selectedProfile,
    JPanel parent,
    String profileName,
    Set<String> existingProfileNames,
    @Nonnull Project project
  ) {
    profileName = Messages.showInputDialog(parent, profileName, "Create New Inspection Profile", UIUtil.getQuestionIcon());
    if (profileName == null) return null;
    final ProfileManager profileManager = selectedProfile.getProfileManager();
    if (existingProfileNames.contains(profileName)) {
      Messages.showErrorDialog(
        InspectionLocalize.inspectionUnableToCreateProfileMessage(profileName).get(),
        InspectionLocalize.inspectionUnableToCreateProfileDialogTitle().get()
      );
      return null;
    }
    InspectionProfileImpl inspectionProfile = new InspectionProfileImpl(profileName, InspectionToolRegistrar.getInstance(), profileManager);
    if (initValue == -1) {
      inspectionProfile.initInspectionTools(project);
      ModifiableModel profileModifiableModel = inspectionProfile.getModifiableModel();
      final InspectionToolWrapper[] profileEntries = profileModifiableModel.getInspectionTools(null);
      for (InspectionToolWrapper toolWrapper : profileEntries) {
        profileModifiableModel.disableTool(toolWrapper.getShortName(), null, project);
      }
      profileModifiableModel.setProjectLevel(false);
      profileModifiableModel.setModified(true);
      return profileModifiableModel;
    }
    else if (initValue == 0) {
      inspectionProfile.copyFrom(selectedProfile);
      inspectionProfile.setName(profileName);
      inspectionProfile.initInspectionTools(project);
      inspectionProfile.setModified(true);
      return inspectionProfile;
    }
    return null;
  }

  @Nullable
  private static InspectionConfigTreeNode findNodeByKey(String name, InspectionConfigTreeNode root) {
    for (int i = 0; i < root.getChildCount(); i++) {
      final InspectionConfigTreeNode child = (InspectionConfigTreeNode)root.getChildAt(i);
      final Descriptor descriptor = child.getDefaultDescriptor();
      if (descriptor != null) {
        if (descriptor.getKey().toString().equals(name)) {
          return child;
        }
      }
      else {
        final InspectionConfigTreeNode node = findNodeByKey(name, child);
        if (node != null) return node;
      }
    }
    return null;
  }

  public static String renderSeverity(HighlightSeverity severity) {
    return StringUtil.capitalizeWords(severity.getName().toLowerCase(), true);
  }

  private static void updateUpHierarchy(final InspectionConfigTreeNode parent) {
    if (parent != null) {
      parent.dropCache();
      updateUpHierarchy((InspectionConfigTreeNode)parent.getParent());
    }
  }

  private static boolean isDescriptorAccepted(
    Descriptor descriptor,
    @NonNls String filter,
    final boolean forceInclude,
    final List<Set<String>> keySetList, final Set<String> quoted
  ) {
    filter = filter.toLowerCase();
    if (StringUtil.containsIgnoreCase(descriptor.getText(), filter)) {
      return true;
    }
    final String[] groupPath = descriptor.getGroup();
    for (String group : groupPath) {
      if (StringUtil.containsIgnoreCase(group, filter)) {
        return true;
      }
    }
    for (String stripped : quoted) {
      if (StringUtil.containsIgnoreCase(descriptor.getText(), stripped)) {
        return true;
      }
      for (String group : groupPath) {
        if (StringUtil.containsIgnoreCase(group, stripped)) {
          return true;
        }
      }
      final String description = descriptor.getToolWrapper().loadDescription();
      if (description != null && StringUtil.containsIgnoreCase(description.toLowerCase(), stripped)) {
        if (!forceInclude) return true;
      }
      else if (forceInclude) return false;
    }
    for (Set<String> keySet : keySetList) {
      if (keySet.contains(descriptor.getKey().toString())) {
        if (!forceInclude) {
          return true;
        }
      }
      else {
        if (forceInclude) {
          return false;
        }
      }
    }
    return forceInclude;
  }

  @RequiredUIAccess
  private static void setConfigPanel(final JPanel configPanelAnchor, final ScopeToolState state, Disposable parentDisposable) {
    configPanelAnchor.removeAll();

    if (!state.isEnabled()) {
      return;
    }
    
    final consulo.ui.Component additionalConfigPanel = state.getConfigurablePanel(parentDisposable);
    //configPanelAnchor.setVisible(additionalConfigPanel != null);
    if (additionalConfigPanel == null) {
      return;
    }

    configPanelAnchor.add(TargetAWT.to(additionalConfigPanel));
  }

  private static InspectionConfigTreeNode getGroupNode(InspectionConfigTreeNode root, String[] groupPath) {
    InspectionConfigTreeNode currentRoot = root;
    for (final String group : groupPath) {
      currentRoot = getGroupNode(currentRoot, group);
    }
    return currentRoot;
  }

  private static InspectionConfigTreeNode getGroupNode(InspectionConfigTreeNode root, String group) {
    final int childCount = root.getChildCount();
    for (int i = 0; i < childCount; i++) {
      InspectionConfigTreeNode child = (InspectionConfigTreeNode)root.getChildAt(i);
      if (group.equals(child.getUserObject())) {
        return child;
      }
    }
    InspectionConfigTreeNode child = new InspectionConfigTreeNode(group);
    root.add(child);
    return child;
  }

  private static void copyUsedSeveritiesIfUndefined(final ModifiableModel selectedProfile, final ProfileManager profileManager) {
    final SeverityRegistrarImpl registrar = (SeverityRegistrarImpl)((SeverityProvider)profileManager).getSeverityRegistrar();
    final Set<HighlightSeverity> severities = ((InspectionProfileImpl)selectedProfile).getUsedSeverities();
    for (Iterator<HighlightSeverity> iterator = severities.iterator(); iterator.hasNext(); ) {
      HighlightSeverity severity = iterator.next();
      if (registrar.isSeverityValid(severity.getName())) {
        iterator.remove();
      }
    }

    if (!severities.isEmpty()) {
      final SeverityRegistrarImpl oppositeRegister =
        (SeverityRegistrarImpl)((SeverityProvider)selectedProfile.getProfileManager()).getSeverityRegistrar();
      for (HighlightSeverity severity : severities) {
        final TextAttributesKey attributesKey = TextAttributesKey.find(severity.getName());
        final TextAttributes textAttributes = oppositeRegister.getTextAttributesBySeverity(severity);
        if (textAttributes == null) {
          continue;
        }
        HighlightInfoType.HighlightInfoTypeImpl info = new HighlightInfoType.HighlightInfoTypeImpl(severity, attributesKey);
        registrar.registerSeverity(new SeverityRegistrarImpl.SeverityBasedTextAttributes(textAttributes.clone(), info),
                                   textAttributes.getErrorStripeColor());
      }
    }
  }

  private void initUI() {
    myInspectionProfilePanel = createInspectionProfileSettingsPanel();
    add(myInspectionProfilePanel, BorderLayout.CENTER);
    UserActivityWatcher userActivityWatcher = new UserActivityWatcher();
    userActivityWatcher.addUserActivityListener(() -> {
      //invoke after all other listeners
      SwingUtilities.invokeLater(() -> {
        if (mySelectedProfile == null) {
          return; //panel was disposed
        }
        updateProperSettingsForSelection();
        wereToolSettingsModified();
      });
    });
    userActivityWatcher.register(myOptionsPanel);
    updateSelectedProfileState();
    reset();
  }

  private void updateSelectedProfileState() {
    if (mySelectedProfile == null) return;
    restoreTreeState();
    repaintTableData();
    updateSelection();
  }

  public void updateSelection() {
    if (myTreeTable != null) {
      final TreePath selectionPath = myTreeTable.getTree().getSelectionPath();
      if (selectionPath != null) {
        TreeUtil.selectNode(myTreeTable.getTree(), (TreeNode)selectionPath.getLastPathComponent());
        final int rowForPath = myTreeTable.getTree().getRowForPath(selectionPath);
        TableUtil.selectRows(myTreeTable, new int[]{rowForPath});
        scrollToCenter();
      }
    }
  }

  private void loadDescriptorsConfigs(boolean onlyModified) {
    for (ToolDescriptors toolDescriptors : myInitialToolDescriptors) {
      loadDescriptorConfig(toolDescriptors.getDefaultDescriptor(), onlyModified);
      for (Descriptor descriptor : toolDescriptors.getNonDefaultDescriptors()) {
        loadDescriptorConfig(descriptor, onlyModified);
      }
    }
  }

  private void loadDescriptorConfig(Descriptor descriptor, boolean ifModifier) {
    if (!ifModifier || mySelectedProfile.isProperSetting(descriptor.getKey().toString())) {
      descriptor.loadConfig();
    }
  }

  private void wereToolSettingsModified() {
    for (final ToolDescriptors toolDescriptor : myInitialToolDescriptors) {
      Descriptor desc = toolDescriptor.getDefaultDescriptor();
      if (wereToolSettingsModified(desc, true)) return;
      List<Descriptor> descriptors = toolDescriptor.getNonDefaultDescriptors();
      for (Descriptor descriptor : descriptors) {
        if (wereToolSettingsModified(descriptor, false)) return;
      }
    }
    myModified = false;
  }

  private boolean wereToolSettingsModified(Descriptor descriptor, boolean isDefault) {
    if (!mySelectedProfile.isToolEnabled(descriptor.getKey(), descriptor.getScope(), myProjectProfileManager.getProject())) {
      return false;
    }
    Element oldConfig = descriptor.getConfig();
    if (oldConfig == null) return false;

    ScopeToolState state = null;
    if (isDefault) {
      state =
        mySelectedProfile.getToolDefaultState(descriptor.getKey().toString(), myProjectProfileManager.getProject());
    }
    else {
      for (ScopeToolState candidate : mySelectedProfile
        .getNonDefaultTools(descriptor.getKey().toString(), myProjectProfileManager.getProject())) {
        final String scope = descriptor.getScopeName();
        if (Comparing.equal(candidate.getScopeId(), scope)) {
          state = candidate;
          break;
        }
      }
    }

    if (state == null) {
      return true;
    }

    Element newConfig = Descriptor.createConfigElement(state.getTool());
    if (!JDOMUtil.areElementsEqual(oldConfig, newConfig)) {
      myAlarm.cancelAllRequests();
      myAlarm.addRequest(() -> myTreeTable.repaint(), 300);
      myModified = true;
      return true;
    }
    return false;
  }

  private void updateProperSettingsForSelection() {
    final TreePath selectionPath = myTreeTable.getTree().getSelectionPath();
    if (selectionPath != null) {
      InspectionConfigTreeNode node = (InspectionConfigTreeNode)selectionPath.getLastPathComponent();
      final Descriptor descriptor = node.getDefaultDescriptor();
      if (descriptor != null) {
        final boolean properSetting = mySelectedProfile.isProperSetting(descriptor.getKey().toString());
        if (node.isProperSetting() != properSetting) {
          myAlarm.cancelAllRequests();
          myAlarm.addRequest(() -> myTreeTable.repaint(), 300);
          node.dropCache();
          updateUpHierarchy((InspectionConfigTreeNode)node.getParent());
        }
      }
    }
  }

  private void initToolStates() {
    final InspectionProfileImpl profile = mySelectedProfile;
    if (profile == null) return;
    myInitialToolDescriptors.clear();
    final Project project = myProjectProfileManager.getProject();
    for (final ScopeToolState state : profile.getDefaultStates(myProjectProfileManager.getProject())) {
      if (!accept(state.getTool())) continue;
      myInitialToolDescriptors.add(ToolDescriptors.fromScopeToolState(state, profile, project));
    }
    myInitialScopesOrder = mySelectedProfile.getScopesOrder();
  }

  protected boolean accept(InspectionToolWrapper entry) {
    return entry.getDefaultLevel() != HighlightDisplayLevel.NON_SWITCHABLE_ERROR;
  }

  @RequiredUIAccess
  private void postProcessModification() {
    wereToolSettingsModified();
    //resetup configs
    for (ScopeToolState state : mySelectedProfile.getAllTools(myProjectProfileManager.getProject())) {
      state.resetConfigPanel();
    }
    fillTreeData(myProfileFilter.getFilter(), true);
    repaintTableData();
    updateOptionsAndDescriptionPanel(myTreeTable.getTree().getSelectionPaths());
  }

  public void setFilter(String filter) {
    myProfileFilter.setFilter(filter);
  }

  private void filterTree(@Nullable String filter) {
    if (myTreeTable != null) {
      getExpandedNodes(mySelectedProfile).saveVisibleState(myTreeTable.getTree());
      fillTreeData(filter, true);
      reloadModel();
      restoreTreeState();
      if (myTreeTable.getTree().getSelectionPath() == null) {
        TreeUtil.selectFirstNode(myTreeTable.getTree());
      }
    }
  }

  private void filterTree() {
    filterTree(myProfileFilter != null ? myProfileFilter.getFilter() : null);
  }

  private void reloadModel() {
    try {
      myIsInRestore = true;
      ((DefaultTreeModel)myTreeTable.getTree().getModel()).reload();
    }
    finally {
      myIsInRestore = false;
    }
  }

  private void restoreTreeState() {
    try {
      myIsInRestore = true;
      getExpandedNodes(mySelectedProfile).restoreVisibleState(myTreeTable.getTree());
    }
    finally {
      myIsInRestore = false;
    }
  }

  private ActionToolbar createTreeToolbarPanel() {
    final CommonActionsManager actionManager = CommonActionsManager.getInstance();

    ActionGroup.Builder actions = ActionGroup.newImmutableBuilder();

    actions.add(new InspectionFilterAction(mySelectedProfile, myInspectionsFilter, myProjectProfileManager.getProject()));
    actions.addSeparator();

    actions.add(actionManager.createExpandAllAction(myTreeExpander, myTreeTable));
    actions.add(actionManager.createCollapseAllAction(myTreeExpander, myTreeTable));
    actions.add(new DumbAwareAction("Reset to Empty", "Reset to empty", AllIcons.General.Reset) {
      @Override
      @RequiredUIAccess
      public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabled(mySelectedProfile != null && mySelectedProfile.isExecutable(myProjectProfileManager.getProject()));
      }

      @Override
      @RequiredUIAccess
      public void actionPerformed(@Nonnull AnActionEvent e) {
        mySelectedProfile.resetToEmpty(e.getData(Project.KEY));
        loadDescriptorsConfigs(false);
        postProcessModification();
      }
    });

    actions.add(new AdvancedSettingsAction(myProjectProfileManager.getProject(), myRoot) {
      @Override
      protected InspectionProfileImpl getInspectionProfile() {
        return mySelectedProfile;
      }

      @Override
      @RequiredUIAccess
      protected void postProcessModification() {
        loadDescriptorsConfigs(true);
        SingleInspectionProfilePanel.this.postProcessModification();
      }
    });

    final ActionToolbar actionToolbar =
      ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, actions.build(), true);
    actionToolbar.setTargetComponent(this);
    return actionToolbar;
  }

  private void repaintTableData() {
    if (myTreeTable != null) {
      getExpandedNodes(mySelectedProfile).saveVisibleState(myTreeTable.getTree());
      reloadModel();
      restoreTreeState();
    }
  }

  public void selectInspectionTool(String name) {
    final InspectionConfigTreeNode node = findNodeByKey(name, myRoot);
    if (node != null) {
      TreeUtil.selectNode(myTreeTable.getTree(), node);
      final int rowForPath = myTreeTable.getTree().getRowForPath(new TreePath(node.getPath()));
      TableUtil.selectRows(myTreeTable, new int[]{rowForPath});
      scrollToCenter();
    }
  }

  private void scrollToCenter() {
    ListSelectionModel selectionModel = myTreeTable.getSelectionModel();
    int maxSelectionIndex = selectionModel.getMaxSelectionIndex();
    final int maxColumnSelectionIndex = Math.max(0, myTreeTable.getColumnModel().getSelectionModel().getMinSelectionIndex());
    Rectangle maxCellRect = myTreeTable.getCellRect(maxSelectionIndex, maxColumnSelectionIndex, false);

    final Point selectPoint = maxCellRect.getLocation();
    final int allHeight = myTreeTable.getVisibleRect().height;
    myTreeTable.scrollRectToVisible(new Rectangle(
      new Point(0, Math.max(0, selectPoint.y - allHeight / 2)),
      new Dimension(0, allHeight)
    ));
  }

  private JScrollPane initTreeScrollPane() {
    fillTreeData(null, true);

    final InspectionsConfigTreeRenderer renderer = new InspectionsConfigTreeRenderer() {
      @Override
      protected String getFilter() {
        return myProfileFilter != null ? myProfileFilter.getFilter() : null;
      }
    };
    myTreeTable = InspectionsConfigTreeTable.create(
      new InspectionsConfigTreeTable.InspectionsConfigTreeTableSettings(myRoot, myProjectProfileManager.getProject()) {
        @Override
        protected void onChanged(final InspectionConfigTreeNode node) {
          updateUpHierarchy((InspectionConfigTreeNode)node.getParent());
        }

        @Override
        @RequiredUIAccess
        public void updateRightPanel() {
          updateOptionsAndDescriptionPanel();
        }

        @Override
        public InspectionProfileImpl getInspectionProfile() {
          return mySelectedProfile;
        }
      },
      myDisposable
    );
    myTreeTable.setTreeCellRenderer(renderer);
    myTreeTable.setRootVisible(false);
    UIUtil.setLineStyleAngled(myTreeTable.getTree());
    TreeUtil.installActions(myTreeTable.getTree());

    myTreeTable.getTree().addTreeSelectionListener(e -> {
      if (myTreeTable.getTree().getSelectionPaths() != null) {
        updateOptionsAndDescriptionPanel(myTreeTable.getTree().getSelectionPaths());
      }
      else {
        initOptionsAndDescriptionPanel();
      }

      if (!myIsInRestore) {
        InspectionProfileImpl selected = mySelectedProfile;
        if (selected != null) {
          InspectionProfileImpl baseProfile = (InspectionProfileImpl)selected.getParentProfile();
          if (baseProfile != null) {
            getExpandedNodes(baseProfile).setSelectionPaths(myTreeTable.getTree().getSelectionPaths());
          }
          getExpandedNodes(selected).setSelectionPaths(myTreeTable.getTree().getSelectionPaths());
        }
      }
    });


    myTreeTable.addMouseListener(new PopupHandler() {
      @Override
      public void invokePopup(Component comp, int x, int y) {
        final int[] selectionRows = myTreeTable.getTree().getSelectionRows();
        if (selectionRows != null &&
          myTreeTable.getTree().getPathForLocation(x, y) != null &&
          Arrays.binarySearch(selectionRows, myTreeTable.getTree().getRowForLocation(x, y)) > -1) {
          compoundPopup().show(comp, x, y);
        }
      }
    });


    new TreeSpeedSearch(myTreeTable.getTree(), o -> {
      final InspectionConfigTreeNode node = (InspectionConfigTreeNode)o.getLastPathComponent();
      final Descriptor descriptor = node.getDefaultDescriptor();
      return descriptor != null
        ? InspectionsConfigTreeComparator.getDisplayTextToSort(descriptor.getText())
        : InspectionsConfigTreeComparator.getDisplayTextToSort(node.getGroupName());
    });


    final JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTreeTable);
    myTreeTable.getTree().setShowsRootHandles(true);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    TreeUtil.collapseAll(myTreeTable.getTree(), 1);

    myTreeTable.getTree().addTreeExpansionListener(new TreeExpansionListener() {


      @Override
      public void treeCollapsed(TreeExpansionEvent event) {
        InspectionProfileImpl selected = mySelectedProfile;
        final InspectionConfigTreeNode node = (InspectionConfigTreeNode)event.getPath().getLastPathComponent();
        final InspectionProfileImpl parentProfile = (InspectionProfileImpl)selected.getParentProfile();
        if (parentProfile != null) {
          getExpandedNodes(parentProfile).saveVisibleState(myTreeTable.getTree());
        }
        getExpandedNodes(selected).saveVisibleState(myTreeTable.getTree());
      }

      @Override
      public void treeExpanded(TreeExpansionEvent event) {
        InspectionProfileImpl selected = mySelectedProfile;
        if (selected != null) {
          final InspectionConfigTreeNode node = (InspectionConfigTreeNode)event.getPath().getLastPathComponent();
          final InspectionProfileImpl parentProfile = (InspectionProfileImpl)selected.getParentProfile();
          if (parentProfile != null) {
            getExpandedNodes(parentProfile).expandNode(node);
          }
          getExpandedNodes(selected).expandNode(node);
        }
      }
    });

    myTreeExpander = new DefaultTreeExpander(myTreeTable.getTree()) {
      @Override
      public boolean canExpand() {
        return myTreeTable.isShowing();
      }

      @Override
      public boolean canCollapse() {
        return myTreeTable.isShowing();
      }
    };
    myProfileFilter = new MyFilterComponent();

    return scrollPane;
  }

  private JPopupMenu compoundPopup() {
    final ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
    final SeverityRegistrarImpl severityRegistrar =
      (SeverityRegistrarImpl)((SeverityProvider)mySelectedProfile.getProfileManager()).getOwnSeverityRegistrar();
    TreeSet<HighlightSeverity> severities = new TreeSet<>(severityRegistrar);
    severities.add(HighlightSeverity.ERROR);
    severities.add(HighlightSeverity.WARNING);
    severities.add(HighlightSeverity.WEAK_WARNING);
    final Collection<SeverityRegistrarImpl.SeverityBasedTextAttributes> infoTypes =
      SeverityUtil.getRegisteredHighlightingInfoTypes(severityRegistrar);
    for (SeverityRegistrarImpl.SeverityBasedTextAttributes info : infoTypes) {
      severities.add(info.getSeverity());
    }
    for (HighlightSeverity severity : severities) {
      final HighlightDisplayLevel level = HighlightDisplayLevel.find(severity);
      group.add(new AnAction(renderSeverity(severity), renderSeverity(severity), level.getIcon()) {
        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
          setNewHighlightingLevel(level);
        }

        @Override
        public boolean isDumbAware() {
          return true;
        }
      });
    }
    group.add(AnSeparator.getInstance());
    ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN, group.build());
    menu.setTargetComponent(this);
    return menu.getComponent();
  }

  @Nonnull
  public InspectionsFilter getInspectionsFilter() {
    return myInspectionsFilter;
  }

  private void fillTreeData(@Nullable String filter, boolean forceInclude) {
    if (mySelectedProfile == null) return;
    myRoot.removeAllChildren();
    myRoot.dropCache();
    List<Set<String>> keySetList = new ArrayList<>();
    final Set<String> quoted = new HashSet<>();
    if (filter != null && !filter.isEmpty()) {
      keySetList.addAll(SearchUtil.findKeys(filter, quoted));
    }
    Project project = myProjectProfileManager.getProject();
    final boolean emptyFilter = myInspectionsFilter.isEmptyFilter();
    for (ToolDescriptors toolDescriptors : myInitialToolDescriptors) {
      final Descriptor descriptor = toolDescriptors.getDefaultDescriptor();
      if (filter != null && !filter.isEmpty() && !isDescriptorAccepted(descriptor, filter, forceInclude, keySetList, quoted)) {
        continue;
      }
      if (!emptyFilter && !myInspectionsFilter.matches(mySelectedProfile.getTools(toolDescriptors.getDefaultDescriptor()
                                                                                                 .getKey()
                                                                                                 .toString(), project))) {
        continue;
      }
      final InspectionConfigTreeNode node = new InspectionConfigTreeNode(toolDescriptors);
      getGroupNode(myRoot, toolDescriptors.getDefaultDescriptor().getGroup()).add(node);
      myRoot.dropCache();
    }
    if (filter != null && forceInclude && myRoot.getChildCount() == 0) {
      final Set<String> filters = SearchableOptionsRegistrar.getInstance().getProcessedWords(filter);
      if (filters.size() > 1 || !quoted.isEmpty()) {
        fillTreeData(filter, false);
      }
    }
    TreeUtil.sort(myRoot, new InspectionsConfigTreeComparator());
  }

  // TODO 134099: see IntentionDescriptionPanel#readHTML
  private boolean readHTML(String text) {
    try {
      myBrowser.read(new StringReader(text), null);
      return true;
    }
    catch (IOException ignored) {
      return false;
    }
  }

  // TODO 134099: see IntentionDescriptionPanel#toHTML
  private String toHTML(String text) {
    final HintHint hintHint = new HintHint(myBrowser, new Point(0, 0));
    hintHint.setFont(UIUtil.getLabelFont());
    return HintUtil.prepareHintText(text, hintHint);
  }

  @RequiredUIAccess
  private void updateOptionsAndDescriptionPanel(final TreePath... paths) {
    if (mySelectedProfile == null || paths == null || paths.length == 0) {
      return;
    }
    final TreePath path = paths[0];
    if (path == null) return;
    final List<InspectionConfigTreeNode> nodes = InspectionsAggregationUtil.getInspectionsNodes(paths);
    if (!nodes.isEmpty()) {
      final InspectionConfigTreeNode singleNode =
        paths.length == 1 && ((InspectionConfigTreeNode)paths[0].getLastPathComponent()).getDefaultDescriptor() != null
          ? ContainerUtil.getFirstItem(nodes) : null;
      if (singleNode != null) {
        if (singleNode.getDefaultDescriptor().loadDescription() != null) {
          // need this in order to correctly load plugin-supplied descriptions
          final Descriptor defaultDescriptor = singleNode.getDefaultDescriptor();
          final String description = defaultDescriptor.loadDescription();
          try {
            if (!readHTML(SearchUtil.markup(toHTML(description), myProfileFilter.getFilter()))) {
              readHTML(toHTML("<b>" + InspectionLocalize.inspectionToolDescriptionUnderConstructionText() + "</b>"));
            }
          }
          catch (Throwable t) {
            LOG.error(
              "Failed to load description for: " + defaultDescriptor.getToolWrapper().getTool().getClass() +
                "; description: " + description,
              t
            );
          }

        }
        else {
          readHTML(toHTML("Can't find inspection description."));
        }
      }
      else {
        readHTML(toHTML("Multiple inspections are selected. You can edit them as a single inspection."));
      }

      myOptionsPanel.removeAll();
      final Project project = myProjectProfileManager.getProject();
      final JPanel severityPanel = new JPanel(new GridBagLayout());
      final double severityPanelWeightY;
      final JPanel configPanelAnchor = new JPanel(new GridLayout());

      final Set<String> scopesNames = new HashSet<>();
      for (final InspectionConfigTreeNode node : nodes) {
        final List<ScopeToolState> nonDefaultTools =
          mySelectedProfile.getNonDefaultTools(node.getDefaultDescriptor().getKey().toString(), project);
        for (final ScopeToolState tool : nonDefaultTools) {
          scopesNames.add(tool.getScopeId());
        }
      }

      if (scopesNames.isEmpty()) {
        final LevelChooserAction severityLevelChooser =
          new LevelChooserAction(mySelectedProfile) {
            @Override
            protected void onChosen(final HighlightSeverity severity) {
              final HighlightDisplayLevel level = HighlightDisplayLevel.find(severity);
              for (final InspectionConfigTreeNode node : nodes) {
                final HighlightDisplayKey key = node.getDefaultDescriptor().getKey();
                final NamedScope scope = node.getDefaultDescriptor().getScope();
                final boolean toUpdate = mySelectedProfile.getErrorLevel(key, scope, project) != level;
                mySelectedProfile.setErrorLevel(key, level, null, project);
                if (toUpdate) node.dropCache();
              }
              myTreeTable.updateUI();
            }
          };
        final HighlightSeverity severity =
          ScopesAndSeveritiesTable.getSeverity(ContainerUtil.map(nodes, node -> node.getDefaultDescriptor().getState()));
        severityLevelChooser.setChosen(severity);

        final ScopesChooser scopesChooser = new ScopesChooser(
          ContainerUtil.map(nodes, InspectionConfigTreeNode::getDefaultDescriptor),
          mySelectedProfile,
          project,
          null
        ) {
            @Override
            @RequiredUIAccess
            protected void onScopesOrderChanged() {
              myTreeTable.updateUI();
              updateOptionsAndDescriptionPanel();
            }

            @Override
            @RequiredUIAccess
            protected void onScopeAdded() {
              myTreeTable.updateUI();
              updateOptionsAndDescriptionPanel();
            }
          };

        severityPanel.add(
          TargetAWT.to(Label.create(InspectionLocalize.inspectionSeverity())),
          new GridBagConstraints(
            0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
            JBUI.insets(10, 0), 0, 0
          )
        );
        final JComponent severityLevelChooserComponent =
          severityLevelChooser.createCustomComponent(severityLevelChooser.getTemplatePresentation(), ActionPlaces.UNKNOWN);
        severityPanel.add(
          severityLevelChooserComponent,
          new GridBagConstraints(
            1, 0, 1, 1, 0, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH,
            JBUI.insets(10, 0), 0, 0
          )
        );
        final JComponent scopesChooserComponent =
          scopesChooser.createCustomComponent(scopesChooser.getTemplatePresentation(), ActionPlaces.UNKNOWN);
        severityPanel.add(
          scopesChooserComponent,
          new GridBagConstraints(
            2, 0, 1, 1, 0, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH,
            JBUI.insets(10, 0), 0, 0
          )
        );
        final JLabel label = new JLabel("", SwingConstants.RIGHT);
        severityPanel.add(
          label,
          new GridBagConstraints(
            3, 0, 1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH,
            JBUI.insets(2, 0), 0, 0
          )
        );
        severityPanelWeightY = 0.0;
        if (singleNode != null) {
          setConfigPanel(
            configPanelAnchor,
            mySelectedProfile.getToolDefaultState(singleNode.getDefaultDescriptor().getKey().toString(), project),
            myDisposable
          );
        }
      }
      else {
        if (singleNode != null) {
          for (final Descriptor descriptor : singleNode.getDescriptors().getNonDefaultDescriptors()) {
            descriptor.loadConfig();
          }
        }
        final JTable scopesAndScopesAndSeveritiesTable =
          new ScopesAndSeveritiesTable(new ScopesAndSeveritiesTable.TableSettings(nodes, mySelectedProfile, project) {
            @Override
            @RequiredUIAccess
            protected void onScopeChosen(@Nonnull final ScopeToolState state) {
              setConfigPanel(configPanelAnchor, state, myDisposable);
              configPanelAnchor.revalidate();
              configPanelAnchor.repaint();
            }

            @Override
            protected void onSettingsChanged() {
              myTreeTable.updateUI();
            }

            @Override
            @RequiredUIAccess
            protected void onScopeAdded() {
              myTreeTable.updateUI();
              updateOptionsAndDescriptionPanel();
            }

            @Override
            @RequiredUIAccess
            protected void onScopesOrderChanged() {
              myTreeTable.updateUI();
              updateOptionsAndDescriptionPanel();
            }

            @Override
            @RequiredUIAccess
            protected void onScopeRemoved(final int scopesCount) {
              myTreeTable.updateUI();
              if (scopesCount == 1) {
                updateOptionsAndDescriptionPanel();
              }
            }
          });

        final ToolbarDecorator wrappedTable =
          ToolbarDecorator.createDecorator(scopesAndScopesAndSeveritiesTable).disableUpDownActions().setRemoveActionUpdater(e -> {
            final int selectedRow = scopesAndScopesAndSeveritiesTable.getSelectedRow();
            final int rowCount = scopesAndScopesAndSeveritiesTable.getRowCount();
            return rowCount - 1 != selectedRow;
          });
        final JPanel panel = wrappedTable.createPanel();
        panel.setMinimumSize(new Dimension(getMinimumSize().width, 3 * scopesAndScopesAndSeveritiesTable.getRowHeight()));
        severityPanel.add(
          new JBLabel("Severity by Scope"),
          new GridBagConstraints(
            0, 0, 1, 1, 1.0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
            JBUI.insets(5, 0, 2, 10), 0, 0
          )
        );
        severityPanel.add(
          panel,
          new GridBagConstraints(
            0, 1, 1, 1, 0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,
            JBUI.emptyInsets(), 0, 0
          )
        );
        severityPanelWeightY = 0.3;
      }
      myOptionsPanel.add(
        severityPanel,
        new GridBagConstraints(
          0, 0, 1, 1, 1.0, severityPanelWeightY, GridBagConstraints.WEST, GridBagConstraints.BOTH,
          JBUI.emptyInsets(), 0, 0
        )
      );
      if (configPanelAnchor.getComponentCount() != 0) {
        configPanelAnchor.setBorder(
          IdeBorderFactory.createTitledBorder("Options", false, JBUI.insetsTop(7))
        );
      }
      GuiUtils.enableChildren(myOptionsPanel, isThoughOneNodeEnabled(nodes));
      if (configPanelAnchor.getComponentCount() != 0 || scopesNames.isEmpty()) {
        myOptionsPanel.add(
          configPanelAnchor,
          new GridBagConstraints(
            0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
            JBUI.emptyInsets(), 0, 0
          )
        );
      }
      myOptionsPanel.revalidate();
    }
    else {
      initOptionsAndDescriptionPanel();
    }
    myOptionsPanel.repaint();
  }

  private boolean isThoughOneNodeEnabled(final List<InspectionConfigTreeNode> nodes) {
    final Project project = myProjectProfileManager.getProject();
    for (final InspectionConfigTreeNode node : nodes) {
      final String toolId = node.getDefaultDescriptor().getKey().toString();
      if (mySelectedProfile.getTools(toolId, project).isEnabled()) {
        return true;
      }
    }
    return false;
  }

  @RequiredUIAccess
  private void updateOptionsAndDescriptionPanel() {
    final TreePath[] paths = myTreeTable.getTree().getSelectionPaths();
    if (paths != null) {
      updateOptionsAndDescriptionPanel(paths);
    }
    else {
      initOptionsAndDescriptionPanel();
    }
  }

  private void initOptionsAndDescriptionPanel() {
    myOptionsPanel.removeAll();
    readHTML(EMPTY_HTML);
    myOptionsPanel.validate();
    myOptionsPanel.repaint();
  }

  public boolean setSelectedProfileModified(boolean modified) {
    mySelectedProfile.setModified(modified);
    return modified;
  }

  public ModifiableModel getSelectedProfile() {
    return mySelectedProfile;
  }

  private void setSelectedProfile(final ModifiableModel modifiableModel) {
    if (mySelectedProfile == modifiableModel) return;
    mySelectedProfile = (InspectionProfileImpl)modifiableModel;
    if (mySelectedProfile != null) {
      myCurrentProfileName = mySelectedProfile.getName();
    }
    initToolStates();
    filterTree();
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(700, 500);
  }

  @RequiredUIAccess
  public void disposeUI() {
    if (myInspectionProfilePanel == null) {
      return;
    }
    myProperties.setFloat(VERTICAL_DIVIDER_PROPORTION, myMainSplitter.getProportion());
    myProperties.setFloat(HORIZONTAL_DIVIDER_PROPORTION, myRightSplitter.getProportion());
    myAlarm.cancelAllRequests();
    myProfileFilter.dispose();
    if (mySelectedProfile != null) {
      for (ScopeToolState state : mySelectedProfile.getAllTools(myProjectProfileManager.getProject())) {
        state.disposeUIResources();
      }
    }
    mySelectedProfile = null;
    Disposer.dispose(myDisposable);
    myDisposable = null;
  }

  private JPanel createInspectionProfileSettingsPanel() {

    myBrowser = new JEditorPane(UIUtil.HTML_MIME, EMPTY_HTML);
    myBrowser.setEditable(false);
    myBrowser.setBorder(IdeBorderFactory.createEmptyBorder(5, 5, 5, 5));
    myBrowser.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE);

    initToolStates();
    fillTreeData(myProfileFilter != null ? myProfileFilter.getFilter() : null, true);

    JPanel descriptionPanel = new JPanel(new BorderLayout());
    descriptionPanel.setBorder(IdeBorderFactory.createTitledBorder(
      InspectionLocalize.inspectionDescriptionTitle().get(),
      false,
      JBUI.insetsTop(2)
    ));
    descriptionPanel.add(ScrollPaneFactory.createScrollPane(myBrowser), BorderLayout.CENTER);

    myRightSplitter = new Splitter(true);
    myRightSplitter.setFirstComponent(descriptionPanel);
    myRightSplitter.setProportion(myProperties.getFloat(HORIZONTAL_DIVIDER_PROPORTION, 0.5f));

    myOptionsPanel = new JPanel(new GridBagLayout());
    initOptionsAndDescriptionPanel();
    myRightSplitter.setSecondComponent(myOptionsPanel);
    myRightSplitter.setHonorComponentsMinimumSize(true);

    final JScrollPane tree = initTreeScrollPane();

    final JPanel northPanel = new JPanel(new GridBagLayout());
    northPanel.setBorder(IdeBorderFactory.createEmptyBorder(2, 0, 2, 0));
    myProfileFilter.setPreferredSize(new Dimension(20, myProfileFilter.getPreferredSize().height));
    northPanel.add(
      myProfileFilter,
      new GridBagConstraints(
        0, 0, 1, 1, 0.5, 1, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.HORIZONTAL,
        JBUI.emptyInsets(), 0, 0
      )
    );
    northPanel.add(
      createTreeToolbarPanel().getComponent(),
      new GridBagConstraints(
        1, 0, 1, 1, 1, 1, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL,
        JBUI.emptyInsets(), 0, 0
      )
    );

    myMainSplitter = new Splitter(false, myProperties.getFloat(VERTICAL_DIVIDER_PROPORTION, 0.5f), 0.01f, 0.99f);
    myMainSplitter.setFirstComponent(tree);
    myMainSplitter.setSecondComponent(myRightSplitter);
    myMainSplitter.setHonorComponentsMinimumSize(false);

    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(northPanel, BorderLayout.NORTH);
    panel.add(myMainSplitter, BorderLayout.CENTER);
    return panel;
  }

  @RequiredUIAccess
  public boolean isModified() {
    if (myModified) return true;
    if (mySelectedProfile.isChanged()) return true;
    if (myShareProfile != (mySelectedProfile.getProfileManager() == myProjectProfileManager)) return true;
    if (!Comparing.strEqual(myCurrentProfileName, mySelectedProfile.getName())) return true;
    if (!Arrays.equals(myInitialScopesOrder, mySelectedProfile.getScopesOrder())) return true;
    if (descriptorsAreChanged()) {
      return true;
    }

    if (mySelectedProfile != null) {
      for (ScopeToolState state : mySelectedProfile.getAllTools(myProjectProfileManager.getProject())) {
        if (state.isModified()) {
          return true;
        }
      }
    }
    return false;
  }

  public void reset() {
    myModified = false;
    setSelectedProfile(mySelectedProfile);
    final String filter = myProfileFilter.getFilter();
    myProfileFilter.reset();
    myProfileFilter.setSelectedItem(filter);
    myShareProfile = mySelectedProfile.getProfileManager() == myProjectProfileManager;
  }

  @RequiredUIAccess
  public void apply() throws ConfigurationException {
    final boolean modified = isModified();
    if (!modified) {
      return;
    }

    if (mySelectedProfile != null) {
      for (ScopeToolState state : mySelectedProfile.getAllTools(myProjectProfileManager.getProject())) {
        state.applyConfigPanel();
      }
    }

    final ModifiableModel selectedProfile = getSelectedProfile();

    if (!Comparing.equal(myCurrentProfileName, selectedProfile.getName())) {
      selectedProfile.getProfileManager().deleteProfile(selectedProfile.getName());
      selectedProfile.setName(myCurrentProfileName);
      selectedProfile.getProfileManager().updateProfile(selectedProfile);
    }
    ProfileManager profileManager = myShareProfile ? myProjectProfileManager : InspectionProfileManager.getInstance();
    selectedProfile.setProjectLevel(myShareProfile);
    if (selectedProfile.getProfileManager() != profileManager) {
      if (selectedProfile.getProfileManager().getProfile(selectedProfile.getName(), false) != null) {
        selectedProfile.getProfileManager().deleteProfile(selectedProfile.getName());
      }
      copyUsedSeveritiesIfUndefined(selectedProfile, profileManager);
      selectedProfile.setProfileManager(profileManager);
    }

    final InspectionProfile parentProfile = selectedProfile.getParentProfile();
    try {
      selectedProfile.commit();
    }
    catch (IOException e) {
      throw new ConfigurationException(e.getMessage());
    }
    setSelectedProfile(parentProfile.getModifiableModel());
    setSelectedProfileModified(false);
    myModified = false;
  }

  @RequiredUIAccess
  private boolean descriptorsAreChanged() {
    for (ToolDescriptors toolDescriptors : myInitialToolDescriptors) {
      Descriptor desc = toolDescriptors.getDefaultDescriptor();
      Project project = myProjectProfileManager.getProject();
      if (mySelectedProfile.isToolEnabled(desc.getKey(), null, project) != desc.isEnabled()) {
        return true;
      }
      if (mySelectedProfile.getErrorLevel(desc.getKey(), desc.getScope(), project) != desc.getLevel()) {
        return true;
      }
      final List<Descriptor> descriptors = toolDescriptors.getNonDefaultDescriptors();
      for (Descriptor descriptor : descriptors) {
        if (mySelectedProfile.isToolEnabled(descriptor.getKey(), descriptor.getScope(), project) != descriptor.isEnabled()) {
          return true;
        }
        if (mySelectedProfile.getErrorLevel(descriptor.getKey(), descriptor.getScope(), project) != descriptor.getLevel()) {
          return true;
        }
      }

      final List<ScopeToolState> tools = mySelectedProfile.getNonDefaultTools(desc.getKey().toString(), project);
      if (tools.size() != descriptors.size()) {
        return true;
      }
      for (int i = 0; i < tools.size(); i++) {
        final ScopeToolState pair = tools.get(i);
        if (!Comparing.equal(pair.getScope(project), descriptors.get(i).getScope())) {
          return true;
        }
      }
    }


    return false;
  }

  public boolean isProfileShared() {
    return myShareProfile;
  }

  public void setProfileShared(boolean profileShared) {
    myShareProfile = profileShared;
  }

  @Nonnull
  public String getCurrentProfileName() {
    return myCurrentProfileName;
  }

  public void setCurrentProfileName(@Nonnull String currentProfileName) {
    myCurrentProfileName = currentProfileName;
  }

  @Override
  public void setVisible(boolean aFlag) {
    if (aFlag && myInspectionProfilePanel == null) {
      initUI();
    }
    super.setVisible(aFlag);
  }

  @RequiredUIAccess
  private void setNewHighlightingLevel(@Nonnull HighlightDisplayLevel level) {
    final int[] rows = myTreeTable.getTree().getSelectionRows();
    final boolean showOptionsAndDescriptorPanels = rows != null && rows.length == 1;
    for (int i = 0; rows != null && i < rows.length; i++) {
      final InspectionConfigTreeNode node = (InspectionConfigTreeNode)myTreeTable.getTree().getPathForRow(rows[i]).getLastPathComponent();
      final InspectionConfigTreeNode parent = (InspectionConfigTreeNode)node.getParent();
      final Object userObject = node.getUserObject();
      if (userObject instanceof ToolDescriptors && (node.getScopeName() != null || node.isLeaf())) {
        updateErrorLevel(node, showOptionsAndDescriptorPanels, level);
        updateUpHierarchy(parent);
      }
      else {
        updateErrorLevelUpInHierarchy(level, showOptionsAndDescriptorPanels, node);
        updateUpHierarchy(parent);
      }
    }
    if (rows != null) {
      updateOptionsAndDescriptionPanel(myTreeTable.getTree().getSelectionPaths());
    }
    else {
      initOptionsAndDescriptionPanel();
    }
    repaintTableData();
  }

  private void updateErrorLevelUpInHierarchy(
    @Nonnull HighlightDisplayLevel level,
    boolean showOptionsAndDescriptorPanels,
    InspectionConfigTreeNode node
  ) {
    node.dropCache();
    for (int j = 0; j < node.getChildCount(); j++) {
      final InspectionConfigTreeNode child = (InspectionConfigTreeNode)node.getChildAt(j);
      final Object userObject = child.getUserObject();
      if (userObject instanceof ToolDescriptors && (child.getScopeName() != null || child.isLeaf())) {
        updateErrorLevel(child, showOptionsAndDescriptorPanels, level);
      }
      else {
        updateErrorLevelUpInHierarchy(level, showOptionsAndDescriptorPanels, child);
      }
    }
  }

  @RequiredUIAccess
  private void updateErrorLevel(
    final InspectionConfigTreeNode child,
    final boolean showOptionsAndDescriptorPanels,
    @Nonnull HighlightDisplayLevel level
  ) {
    final HighlightDisplayKey key = child.getDefaultDescriptor().getKey();
    mySelectedProfile.setErrorLevel(key, level, null, myProjectProfileManager.getProject());
    child.dropCache();
    if (showOptionsAndDescriptorPanels) {
      updateOptionsAndDescriptionPanel(new TreePath(child.getPath()));
    }
  }

  public JComponent getPreferredFocusedComponent() {
    return myTreeTable;
  }

  private class MyFilterComponent extends FilterComponent {
    private MyFilterComponent() {
      super(INSPECTION_FILTER_HISTORY, 10);
      setHistory(Arrays.asList("\"New in 13\""));
    }

    @Override
    public void filter() {
      filterTree(getFilter());
    }

    @Override
    protected void onlineFilter() {
      if (mySelectedProfile == null) return;
      final String filter = getFilter();
      getExpandedNodes(mySelectedProfile).saveVisibleState(myTreeTable.getTree());
      fillTreeData(filter, true);
      reloadModel();
      if (filter == null || filter.isEmpty()) {
        restoreTreeState();
      }
      else {
        TreeUtil.expandAll(myTreeTable.getTree());
      }
    }
  }
}
