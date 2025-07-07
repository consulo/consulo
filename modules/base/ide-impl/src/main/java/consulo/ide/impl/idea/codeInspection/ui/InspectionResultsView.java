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
package consulo.ide.impl.idea.codeInspection.ui;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.application.HelpManager;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.colorScheme.TextAttributesKey;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.ide.impl.idea.codeInspection.ex.EditInspectionToolsSettingsAction;
import consulo.ide.impl.idea.codeInspection.ex.GlobalInspectionContextImpl;
import consulo.ide.impl.idea.codeInspection.ex.InspectionRVContentProvider;
import consulo.ide.impl.idea.codeInspection.ex.QuickFixAction;
import consulo.ide.impl.idea.codeInspection.ui.actions.ExportHTMLAction;
import consulo.ide.impl.idea.codeInspection.ui.actions.InspectionsOptionsToolbarAction;
import consulo.ide.impl.idea.codeInspection.ui.actions.InvokeQuickFixAction;
import consulo.ide.impl.idea.ide.OccurenceNavigatorSupport;
import consulo.ide.impl.idea.profile.codeInspection.InspectionProjectProfileManager;
import consulo.language.editor.highlight.HighlightManager;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.inspection.CommonProblemDescriptor;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemDescriptorBase;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.reference.RefElement;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.inspection.scheme.Tools;
import consulo.language.editor.internal.inspection.ScopeToolState;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.ui.scope.AnalysisUIOptions;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.OccurenceNavigator;
import consulo.ui.ex.OpenSourceUtil;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.PopupHandler;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.SideBorder;
import consulo.ui.ex.awt.Splitter;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.tree.SmartExpander;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

/**
 * @author max
 */
public class InspectionResultsView extends JPanel implements Disposable, OccurenceNavigator, DataProvider {
  public static final Key<InspectionResultsView> DATA_KEY = Key.create("inspectionView");

  private final Project myProject;
  private InspectionTree myTree;
  private final Browser myBrowser;
  private Map<HighlightDisplayLevel, Map<String, InspectionGroupNode>> myGroups = null;
  private OccurenceNavigator myOccurenceNavigator;
  private InspectionProfile myInspectionProfile;
  private final AnalysisScope myScope;
  private static final String HELP_ID = "reference.toolWindows.inspections";
  private final Map<HighlightDisplayLevel, InspectionSeverityGroupNode> mySeverityGroupNodes = new TreeMap<>(
    (o1, o2) -> {
      final int severityDiff = o1.getSeverity().compareTo(o2.getSeverity());
      if (severityDiff == 0) {
        return o1.toString().compareTo(o2.toString());
      }
      return severityDiff;
    }
  );

  private final Splitter mySplitter;
  @Nonnull
  private final GlobalInspectionContextImpl myGlobalInspectionContext;
  private boolean myRerun = false;

  @Nonnull
  private final InspectionRVContentProvider myProvider;
  private AnAction myIncludeAction;
  private AnAction myExcludeAction;

  public InspectionResultsView(@Nonnull final Project project,
                               final InspectionProfile inspectionProfile,
                               @Nonnull AnalysisScope scope,
                               @Nonnull GlobalInspectionContextImpl globalInspectionContext,
                               @Nonnull InspectionRVContentProvider provider) {
    setLayout(new BorderLayout());

    myProject = project;
    myInspectionProfile = inspectionProfile;
    myScope = scope;
    myGlobalInspectionContext = globalInspectionContext;
    myProvider = provider;

    myTree = new InspectionTree(project, globalInspectionContext);
    initTreeListeners();

    myOccurenceNavigator = initOccurenceNavigator();

    myBrowser = new Browser(this);

    mySplitter = new Splitter(false, AnalysisUIOptions.getInstance(myProject).SPLITTER_PROPORTION);

    mySplitter.setFirstComponent(ScrollPaneFactory.createScrollPane(myTree, SideBorder.LEFT | SideBorder.RIGHT));
    mySplitter.setSecondComponent(myBrowser);

    mySplitter.addPropertyChangeListener(evt -> {
      if (Splitter.PROP_PROPORTION.equals(evt.getPropertyName())) {
        myGlobalInspectionContext.setSplitterProportion((Float)evt.getNewValue());
      }
    });
    add(mySplitter, BorderLayout.CENTER);

    myBrowser.addClickListener(e -> {
      if (e.getEventType() == Browser.ClickEvent.REF_ELEMENT) {
        final RefElement refElement = e.getClickedElement();
        final OpenFileDescriptorImpl descriptor = getOpenFileDescriptor(refElement);
        if (descriptor != null) {
          FileEditorManager.getInstance(project).openTextEditor(descriptor, false);
        }
      }
      else if (e.getEventType() == Browser.ClickEvent.FILE_OFFSET) {
        final VirtualFile file = e.getFile();
        final OpenFileDescriptorImpl descriptor = new OpenFileDescriptorImpl(project, file, e.getStartOffset());
        final Editor editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
        if (editor != null) {
          TextAttributesKey selectionAttributesKey = EditorColors.SEARCH_RESULT_ATTRIBUTES;
          HighlightManager.getInstance(project).addRangeHighlight(editor, e.getStartOffset(), e.getEndOffset(), selectionAttributesKey, true, null);
        }
      }
    });

    createActionsToolbar();
  }

  @RequiredUIAccess
  private void initTreeListeners() {
    myTree.getSelectionModel().addTreeSelectionListener(e -> {
      syncBrowser();
      if (isAutoScrollMode()) {
        OpenSourceUtil.openSourcesFrom(DataManager.getInstance().getDataContext(InspectionResultsView.this), false);
      }
    });

    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(MouseEvent e) {
        OpenSourceUtil.openSourcesFrom(DataManager.getInstance().getDataContext(InspectionResultsView.this), true);
        return true;
      }
    }.installOn(myTree);

    myTree.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          OpenSourceUtil.openSourcesFrom(DataManager.getInstance().getDataContext(InspectionResultsView.this), false);
        }
      }
    });

    myTree.addMouseListener(new PopupHandler() {
      @Override
      public void invokePopup(Component comp, int x, int y) {
        popupInvoked(comp, x, y);
      }
    });

    SmartExpander.installOn(myTree);
  }

  private OccurenceNavigatorSupport initOccurenceNavigator() {
    return new OccurenceNavigatorSupport(myTree) {
      @Override
      @Nullable
      @RequiredReadAction
      protected Navigatable createDescriptorForNode(@Nonnull DefaultMutableTreeNode node) {
        if (node instanceof RefElementNode refNode) {
          if (refNode.hasDescriptorsUnder()) return null;
          final RefEntity element = refNode.getElement();
          if (element == null || !element.isValid()) return null;
          final CommonProblemDescriptor problem = refNode.getProblem();
          if (problem != null) {
            return navigate(problem);
          }
          if (element instanceof RefElement refElement) {
            return getOpenFileDescriptor(refElement);
          }
        }
        else if (node instanceof ProblemDescriptionNode descriptionNode) {
          if (!descriptionNode.isValid()) return null;
          return navigate(descriptionNode.getDescriptor());
        }
        return null;
      }

      @Nullable
      @RequiredReadAction
      private Navigatable navigate(final CommonProblemDescriptor descriptor) {
        return getSelectedNavigatable(descriptor);
      }

      @Override
      public String getNextOccurenceActionName() {
        return InspectionLocalize.inspectionActionGoNext().get();
      }

      @Override
      public String getPreviousOccurenceActionName() {
        return InspectionLocalize.inspectionActiongoPrev().get();
      }
    };
  }

  private void createActionsToolbar() {
    final JComponent leftActionsToolbar = createLeftActionsToolbar();
    final JComponent rightActionsToolbar = createRightActionsToolbar();

    JPanel westPanel = new JPanel(new BorderLayout());
    westPanel.add(leftActionsToolbar, BorderLayout.WEST);
    westPanel.add(rightActionsToolbar, BorderLayout.EAST);
    add(westPanel, BorderLayout.WEST);
  }

  @SuppressWarnings({"NonStaticInitializer"})
  private JComponent createRightActionsToolbar() {
    myIncludeAction = new AnAction(InspectionLocalize.inspectionsResultViewIncludeActionText()) {
      {
        registerCustomShortcutSet(CommonShortcuts.INSERT, myTree);
      }

      @Override
      @RequiredUIAccess
      public void actionPerformed(@Nonnull AnActionEvent e) {
        ((InspectionTreeNode)myTree.getSelectionPath().getLastPathComponent()).amnesty();
        updateView(false);
      }

      @Override
      public void update(@Nonnull AnActionEvent e) {
        final TreePath path = myTree.getSelectionPath();
        e.getPresentation().setEnabled(path != null && !myGlobalInspectionContext.getUIOptions().FILTER_RESOLVED_ITEMS);
      }
    };

    myExcludeAction = new AnAction(InspectionLocalize.inspectionsResultViewExcludeActionText()) {
      {
        registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)), myTree);
      }

      @Override
      @RequiredUIAccess
      public void actionPerformed(@Nonnull AnActionEvent e) {
        ((InspectionTreeNode)myTree.getSelectionPath().getLastPathComponent()).ignoreElement();
        updateView(false);
      }

      @Override
      public void update(@Nonnull AnActionEvent e) {
        final TreePath path = myTree.getSelectionPath();
        e.getPresentation().setEnabled(path != null);
      }
    };

    DefaultActionGroup specialGroup = new DefaultActionGroup();
    specialGroup.add(myGlobalInspectionContext.getUIOptions().createGroupBySeverityAction(this::update));
    specialGroup.add(myGlobalInspectionContext.getUIOptions().createGroupByDirectoryAction(this::update));
    specialGroup.add(myGlobalInspectionContext.getUIOptions().createFilterResolvedItemsAction(this::update));
    specialGroup.add(myGlobalInspectionContext.getUIOptions().createShowOutdatedProblemsAction(this::update));
    specialGroup.add(myGlobalInspectionContext.getUIOptions().createShowDiffOnlyAction(this::update));
    specialGroup.add(new EditSettingsAction());
    specialGroup.add(new InvokeQuickFixAction(this));
    specialGroup.add(new InspectionsOptionsToolbarAction(this));
    return createToolbar(specialGroup);
  }

  private JComponent createLeftActionsToolbar() {
    final CommonActionsManager actionsManager = CommonActionsManager.getInstance();
    DefaultActionGroup group = new DefaultActionGroup();
    group.add(new RerunAction(this));
    group.add(new CloseAction());
    final TreeExpander treeExpander = new TreeExpander() {
      @Override
      public void expandAll() {
        TreeUtil.expandAll(myTree);
      }

      @Override
      public boolean canExpand() {
        return true;
      }

      @Override
      public void collapseAll() {
        TreeUtil.collapseAll(myTree, 0);
      }

      @Override
      public boolean canCollapse() {
        return true;
      }
    };
    group.add(actionsManager.createExpandAllAction(treeExpander, myTree));
    group.add(actionsManager.createCollapseAllAction(treeExpander, myTree));
    group.add(actionsManager.createPrevOccurenceAction(getOccurenceNavigator()));
    group.add(actionsManager.createNextOccurenceAction(getOccurenceNavigator()));
    group.add(myGlobalInspectionContext.createToggleAutoscrollAction());
    group.add(new ExportHTMLAction(this));
    group.add(new ContextHelpAction(HELP_ID));

    return createToolbar(group);
  }

  private static JComponent createToolbar(final DefaultActionGroup specialGroup) {
    return ActionManager.getInstance().createActionToolbar(ActionPlaces.CODE_INSPECTION, specialGroup, false).getComponent();
  }

  @Override
  public void dispose() {
    mySplitter.dispose();
    myBrowser.dispose();
    myTree = null;
    myOccurenceNavigator = null;
    myInspectionProfile = null;
  }

  @RequiredUIAccess
  private boolean isAutoScrollMode() {
    String activeToolWindowId = ToolWindowManager.getInstance(myProject).getActiveToolWindowId();
    return myGlobalInspectionContext.getUIOptions().AUTOSCROLL_TO_SOURCE
      && (activeToolWindowId == null || activeToolWindowId.equals(ToolWindowId.INSPECTION));
  }

  @Nullable
  private static OpenFileDescriptorImpl getOpenFileDescriptor(final RefElement refElement) {
    final VirtualFile[] file = new VirtualFile[1];
    final int[] offset = new int[1];

    Application.get().runReadAction(() -> {
      PsiElement psiElement = refElement.getPsiElement();
      if (psiElement != null) {
        final PsiFile containingFile = psiElement.getContainingFile();
        if (containingFile != null) {
          file[0] = containingFile.getVirtualFile();
          offset[0] = psiElement.getTextOffset();
        }
      }
      else {
        file[0] = null;
      }
    });

    if (file[0] != null && file[0].isValid()) {
      return new OpenFileDescriptorImpl(refElement.getRefManager().getProject(), file[0], offset[0]);
    }
    return null;
  }

  private void syncBrowser() {
    if (myTree.getSelectionModel().getSelectionCount() != 1) {
      myBrowser.showEmpty();
    }
    else {
      TreePath pathSelected = myTree.getSelectionModel().getLeadSelectionPath();
      if (pathSelected != null) {
        final InspectionTreeNode node = (InspectionTreeNode)pathSelected.getLastPathComponent();
        if (node instanceof RefElementNode refElementNode) {
          final CommonProblemDescriptor problem = refElementNode.getProblem();
          final RefEntity refSelected = refElementNode.getElement();
          if (problem != null) {
            showInBrowser(refSelected, problem);
          }
          else {
            showInBrowser(refSelected);
          }
        }
        else if (node instanceof ProblemDescriptionNode problemNode) {
          showInBrowser(problemNode.getElement(), problemNode.getDescriptor());
        }
        else if (node instanceof InspectionNode inspectionNode) {
          showInBrowser(inspectionNode.getToolWrapper());
        }
        else {
          myBrowser.showEmpty();
        }
      }
    }
  }

  private void showInBrowser(final RefEntity refEntity) {
    Cursor currentCursor = getCursor();
    setCursor(new Cursor(Cursor.WAIT_CURSOR));
    myBrowser.showPageFor(refEntity);
    setCursor(currentCursor);
  }

  private void showInBrowser(@Nonnull InspectionToolWrapper toolWrapper) {
    Cursor currentCursor = getCursor();
    setCursor(new Cursor(Cursor.WAIT_CURSOR));
    myBrowser.showDescription(toolWrapper);
    setCursor(currentCursor);
  }

  private void showInBrowser(final RefEntity refEntity, CommonProblemDescriptor descriptor) {
    Cursor currentCursor = getCursor();
    setCursor(new Cursor(Cursor.WAIT_CURSOR));
    myBrowser.showPageFor(refEntity, descriptor);
    setCursor(currentCursor);
  }

  @Nonnull
  public InspectionNode addTool(@Nonnull final InspectionToolWrapper toolWrapper, HighlightDisplayLevel errorLevel, boolean groupedBySeverity) {
    String groupName = toolWrapper.getJoinedGroupPath();
    InspectionTreeNode parentNode = getToolParentNode(groupName, errorLevel, groupedBySeverity);
    InspectionNode toolNode = new InspectionNode(toolWrapper);
    boolean showStructure = myGlobalInspectionContext.getUIOptions().SHOW_STRUCTURE;
    myProvider.appendToolNodeContent(myGlobalInspectionContext, toolNode, parentNode, showStructure);
    InspectionToolPresentation presentation = myGlobalInspectionContext.getPresentation(toolWrapper);
    toolNode = presentation.createToolNode(myGlobalInspectionContext, toolNode, myProvider, parentNode, showStructure);
    ((DefaultInspectionToolPresentation)presentation).myToolNode = toolNode;

    registerActionShortcuts(presentation);
    return toolNode;
  }

  private void registerActionShortcuts(@Nonnull InspectionToolPresentation presentation) {
    final QuickFixAction[] fixes = presentation.getQuickFixes(RefEntity.EMPTY_ELEMENTS_ARRAY);
    if (fixes != null) {
      for (QuickFixAction fix : fixes) {
        fix.registerCustomShortcutSet(fix.getShortcutSet(), this);
      }
    }
  }

  private void clearTree() {
    myTree.removeAllNodes();
    mySeverityGroupNodes.clear();
  }

  @Nullable
  public String getCurrentProfileName() {
    return myInspectionProfile == null ? null : myInspectionProfile.getDisplayName();
  }

  public InspectionProfile getCurrentProfile() {
    return myInspectionProfile;
  }

  public boolean update() {
    return updateView(true);
  }

  public boolean updateView(boolean strict) {
    if (!strict && !myGlobalInspectionContext.getUIOptions().FILTER_RESOLVED_ITEMS) {
      myTree.repaint();
      return false;
    }
    clearTree();
    boolean resultsFound = buildTree();
    myTree.restoreExpansionAndSelection();
    return resultsFound;
  }

  private boolean buildTree() {
    InspectionProfile profile = myInspectionProfile;
    boolean isGroupedBySeverity = myGlobalInspectionContext.getUIOptions().GROUP_BY_SEVERITY;
    myGroups = new HashMap<>();
    final Map<String, Tools> tools = myGlobalInspectionContext.getTools();
    boolean resultsFound = false;
    for (Tools currentTools : tools.values()) {
      InspectionToolWrapper defaultToolWrapper = currentTools.getDefaultState().getTool();
      final HighlightDisplayKey key = HighlightDisplayKey.find(defaultToolWrapper.getShortName());
      for (ScopeToolState state : currentTools.getTools()) {
        InspectionToolWrapper toolWrapper = state.getTool();
        if (myProvider.checkReportedProblems(myGlobalInspectionContext, toolWrapper)) {
          addTool(toolWrapper, ((InspectionProfileImpl)profile).getErrorLevel(key, state.getScope(myProject), myProject), isGroupedBySeverity);
          resultsFound = true;
        }
      }
    }
    return resultsFound;
  }

  @Nonnull
  public InspectionTreeNode getToolParentNode(@Nonnull String groupName, HighlightDisplayLevel errorLevel, boolean groupedBySeverity) {
    if (groupName.isEmpty()) {
      return getRelativeRootNode(groupedBySeverity, errorLevel);
    }
    if (myGroups == null) {
      myGroups = new HashMap<>();
    }
    Map<String, InspectionGroupNode> map = myGroups.get(errorLevel);
    if (map == null) {
      map = new HashMap<>();
      myGroups.put(errorLevel, map);
    }
    Map<String, InspectionGroupNode> searchMap = new HashMap<>(map);
    if (!groupedBySeverity) {
      for (Map<String, InspectionGroupNode> groupMap : myGroups.values()) {
        searchMap.putAll(groupMap);
      }
    }
    InspectionGroupNode group = searchMap.get(groupName);
    if (group == null) {
      group = new InspectionGroupNode(groupName);
      map.put(groupName, group);
      getRelativeRootNode(groupedBySeverity, errorLevel).add(group);
    }
    return group;
  }

  @Nonnull
  private InspectionTreeNode getRelativeRootNode(boolean isGroupedBySeverity, HighlightDisplayLevel level) {
    if (isGroupedBySeverity) {
      if (mySeverityGroupNodes.containsKey(level)) {
        return mySeverityGroupNodes.get(level);
      }
      final InspectionSeverityGroupNode severityGroupNode = new InspectionSeverityGroupNode(myProject, level);
      mySeverityGroupNodes.put(level, severityGroupNode);
      myTree.getRoot().add(severityGroupNode);
      return severityGroupNode;
    }
    return myTree.getRoot();
  }


  private OccurenceNavigator getOccurenceNavigator() {
    return myOccurenceNavigator;
  }

  @Override
  public boolean hasNextOccurence() {
    return myOccurenceNavigator != null && myOccurenceNavigator.hasNextOccurence();
  }

  @Override
  public boolean hasPreviousOccurence() {
    return myOccurenceNavigator != null && myOccurenceNavigator.hasPreviousOccurence();
  }

  @Override
  public OccurenceInfo goNextOccurence() {
    return myOccurenceNavigator != null ? myOccurenceNavigator.goNextOccurence() : null;
  }

  @Override
  public OccurenceInfo goPreviousOccurence() {
    return myOccurenceNavigator != null ? myOccurenceNavigator.goPreviousOccurence() : null;
  }

  @Override
  public String getNextOccurenceActionName() {
    return myOccurenceNavigator != null ? myOccurenceNavigator.getNextOccurenceActionName() : "";
  }

  @Override
  public String getPreviousOccurenceActionName() {
    return myOccurenceNavigator != null ? myOccurenceNavigator.getPreviousOccurenceActionName() : "";
  }

  @Nonnull
  public Project getProject() {
    return myProject;
  }

  @Override
  @RequiredReadAction
  public Object getData(@Nonnull Key<?> dataId) {
    if (HelpManager.HELP_ID == dataId) return HELP_ID;
    if (DATA_KEY == dataId) return this;
    if (myTree == null) return null;
    TreePath[] paths = myTree.getSelectionPaths();

    if (paths == null || paths.length == 0) return null;

    if (paths.length > 1) {
      if (PsiElement.KEY_OF_ARRAY == dataId) {
        return collectPsiElements();
      }
      return null;
    }

    TreePath path = paths[0];

    InspectionTreeNode selectedNode = (InspectionTreeNode)path.getLastPathComponent();

    if (selectedNode instanceof RefElementNode refElementNode) {
      RefEntity refElement = refElementNode.getElement();
      if (refElement == null) return null;
      final RefEntity item = refElement.getRefManager().getRefinedElement(refElement);

      if (!item.isValid()) return null;

      PsiElement psiElement = item instanceof RefElement ? ((RefElement)item).getPsiElement() : null;
      if (psiElement == null) return null;

      final CommonProblemDescriptor problem = refElementNode.getProblem();
      if (problem != null) {
        if (problem instanceof ProblemDescriptor problemDescriptor) {
          psiElement = problemDescriptor.getPsiElement();
          if (psiElement == null) return null;
        }
        else {
          return null;
        }
      }

      if (Navigatable.KEY == dataId) {
        return getSelectedNavigatable(problem, psiElement);
      }
      else if (PsiElement.KEY == dataId) {
        return psiElement.isValid() ? psiElement : null;
      }
    }
    else if (selectedNode instanceof ProblemDescriptionNode descriptionNode && Navigatable.KEY == dataId) {
      return getSelectedNavigatable(descriptionNode.getDescriptor());
    }

    return null;
  }

  @Nullable
  @RequiredReadAction
  private Navigatable getSelectedNavigatable(final CommonProblemDescriptor descriptor) {
    PsiElement psiElement = descriptor instanceof ProblemDescriptor problemDescriptor ? problemDescriptor.getPsiElement() : null;
    return getSelectedNavigatable(descriptor, psiElement);
  }

  @Nullable
  private Navigatable getSelectedNavigatable(final CommonProblemDescriptor descriptor, final PsiElement psiElement) {
    if (descriptor instanceof ProblemDescriptorBase problemDescriptor) {
      Navigatable navigatable = problemDescriptor.getNavigatable();
      if (navigatable != null) {
        return navigatable;
      }
    }
    if (psiElement == null || !psiElement.isValid()) return null;
    PsiFile containingFile = psiElement.getContainingFile();
    VirtualFile virtualFile = containingFile == null ? null : containingFile.getVirtualFile();

    if (virtualFile != null) {
      int startOffset = psiElement.getTextOffset();
      if (descriptor instanceof ProblemDescriptorBase problemDescriptor) {
        final TextRange textRange = problemDescriptor.getTextRangeForNavigation();
        if (textRange != null) {
          if (virtualFile instanceof VirtualFileWindow virtualFileWindow) {
            virtualFile = virtualFileWindow.getDelegate();
          }
          startOffset = textRange.getStartOffset();
        }
      }
      return new OpenFileDescriptorImpl(myProject, virtualFile, startOffset);
    }
    return null;
  }

  private PsiElement[] collectPsiElements() {
    RefEntity[] refElements = myTree.getSelectedElements();
    List<PsiElement> psiElements = new ArrayList<>();
    for (RefEntity refElement : refElements) {
      PsiElement psiElement = refElement instanceof RefElement ? ((RefElement)refElement).getPsiElement() : null;
      if (psiElement != null && psiElement.isValid()) {
        psiElements.add(psiElement);
      }
    }

    return PsiUtilCore.toPsiElementArray(psiElements);
  }

  private void popupInvoked(Component component, int x, int y) {
    final TreePath path = myTree.getLeadSelectionPath();

    if (path == null) return;

    final DefaultActionGroup actions = new DefaultActionGroup();
    final ActionManager actionManager = ActionManager.getInstance();
    actions.add(actionManager.getAction(IdeActions.ACTION_EDIT_SOURCE));
    actions.add(actionManager.getAction(IdeActions.ACTION_FIND_USAGES));

    actions.add(myIncludeAction);
    actions.add(myExcludeAction);

    actions.addSeparator();

    final InspectionToolWrapper toolWrapper = myTree.getSelectedToolWrapper();
    if (toolWrapper != null) {
      final QuickFixAction[] quickFixes = myProvider.getQuickFixes(toolWrapper, myTree);
      if (quickFixes != null) {
        for (QuickFixAction quickFixe : quickFixes) {
          actions.add(quickFixe);
        }
      }
      final HighlightDisplayKey key = HighlightDisplayKey.find(toolWrapper.getShortName());
      if (key == null) return; //e.g. DummyEntryPointsTool

      //options
      actions.addSeparator();
      actions.add(new EditSettingsAction());
      final List<AnAction> options = new InspectionsOptionsToolbarAction(this).createActions();
      for (AnAction action : options) {
        actions.add(action);
      }
    }

    actions.addSeparator();
    actions.add(actionManager.getAction(IdeActions.GROUP_VERSION_CONTROLS));

    final ActionPopupMenu menu = actionManager.createActionPopupMenu(ActionPlaces.CODE_INSPECTION, actions);
    menu.getComponent().show(component, x, y);
  }

  @Nonnull
  public InspectionTree getTree() {
    return myTree;
  }

  @Nonnull
  public GlobalInspectionContextImpl getGlobalInspectionContext() {
    return myGlobalInspectionContext;
  }

  @Nonnull
  public InspectionRVContentProvider getProvider() {
    return myProvider;
  }

  public boolean isSingleToolInSelection() {
    return myTree != null && myTree.getSelectedToolWrapper() != null;
  }

  public boolean isRerun() {
    boolean rerun = myRerun;
    myRerun = false;
    return rerun;
  }

  private InspectionProfile guessProfileToSelect(final InspectionProjectProfileManager profileManager) {
    final Set<InspectionProfile> profiles = new HashSet<>();
    final RefEntity[] selectedElements = myTree.getSelectedElements();
    for (RefEntity selectedElement : selectedElements) {
      if (selectedElement instanceof RefElement refElement) {
        final PsiElement element = refElement.getPsiElement();
        if (element != null) {
          profiles.add(profileManager.getInspectionProfile());
        }
      }
    }
    if (profiles.isEmpty()) {
      return (InspectionProfile)profileManager.getProjectProfileImpl();
    }
    return profiles.iterator().next();
  }

  public boolean isProfileDefined() {
    return myInspectionProfile != null && myInspectionProfile.isEditable();
  }

  public static void showPopup(AnActionEvent e, JBPopup popup) {
    final InputEvent event = e.getInputEvent();
    if (event instanceof MouseEvent) {
      popup.showUnderneathOf(event.getComponent());
    }
    else {
      popup.showInBestPositionFor(e.getDataContext());
    }
  }

  public AnalysisScope getScope() {
    return myScope;
  }

  private class CloseAction extends AnAction implements DumbAware {
    private CloseAction() {
      super(CommonLocalize.actionClose(), LocalizeValue.empty(), AllIcons.Actions.Cancel);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
      myGlobalInspectionContext.close(true);
    }
  }

  private class EditSettingsAction extends AnAction {
    private EditSettingsAction() {
      super(
        InspectionLocalize.inspectionActionEditSettings(),
        InspectionLocalize.inspectionActionEditSettings(),
        PlatformIconGroup.generalSettings()
      );
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
      final InspectionProjectProfileManager profileManager = InspectionProjectProfileManager.getInstance(myProject);
      final InspectionToolWrapper toolWrapper = myTree.getSelectedToolWrapper();
      InspectionProfile inspectionProfile = myInspectionProfile;
      final boolean profileIsDefined = isProfileDefined();
      if (!profileIsDefined) {
        inspectionProfile = guessProfileToSelect(profileManager);
      }

      if (toolWrapper != null) {
        final HighlightDisplayKey key = HighlightDisplayKey.find(toolWrapper.getShortName()); //do not search for dead code entry point tool
        if (key != null) {
          new EditInspectionToolsSettingsAction(key).editToolSettings(
            myProject,
            (InspectionProfileImpl)inspectionProfile, profileIsDefined
          ).doWhenDone(() -> {
            if (profileIsDefined) {
              updateCurrentProfile();
            }
          });
        }
      }
      else {
        EditInspectionToolsSettingsAction.editToolSettings(myProject, inspectionProfile, profileIsDefined, null)
          .doWhenDone(() -> {
            if (profileIsDefined) {
              updateCurrentProfile();
            }
          });
      }
    }
  }

  public void updateCurrentProfile() {
    final String name = myInspectionProfile.getName();
    myInspectionProfile = (InspectionProfile)myInspectionProfile.getProfileManager().getProfile(name);
  }

  private class RerunAction extends AnAction {
    public RerunAction(JComponent comp) {
      super(
        InspectionLocalize.inspectionActionRerun(),
        InspectionLocalize.inspectionActionRerun(),
        PlatformIconGroup.actionsRerun()
      );
      registerCustomShortcutSet(CommonShortcuts.getRerun(), comp);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      e.getPresentation().setEnabled(myScope.isValid());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
      rerun();
    }

    private void rerun() {
      myRerun = true;
      if (myScope.isValid()) {
        AnalysisUIOptions.getInstance(myProject).save(myGlobalInspectionContext.getUIOptions());
        myGlobalInspectionContext.doInspections(myScope);
      }
    }
  }
}
