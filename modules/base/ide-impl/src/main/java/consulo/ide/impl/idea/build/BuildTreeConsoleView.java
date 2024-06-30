// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build;

import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.application.util.DateFormatUtil;
import consulo.application.util.UserHomeFileUtil;
import consulo.build.ui.*;
import consulo.build.ui.event.*;
import consulo.build.ui.impl.internal.event.FailureResultImpl;
import consulo.build.ui.impl.internal.event.FileNavigatable;
import consulo.build.ui.impl.internal.event.SkippedResultImpl;
import consulo.build.ui.localize.BuildLocalize;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.SoftWrapAppliancePlaces;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.execution.ui.ExecutionConsole;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.execution.ui.console.Filter;
import consulo.execution.ui.console.HyperlinkInfo;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.ide.impl.idea.execution.impl.ConsoleViewImpl;
import consulo.ide.impl.idea.ide.OccurenceNavigatorSupport;
import consulo.ide.impl.idea.ide.actions.EditSourceAction;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.ide.impl.idea.openapi.editor.actions.ToggleUseSoftWrapsToolbarAction;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.openapi.progress.util.ProgressWindow;
import consulo.util.io.FileUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.impl.idea.util.EditSourceOnEnterKeyHandler;
import consulo.ide.impl.idea.util.concurrency.InvokerImpl;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.navigation.Navigatable;
import consulo.navigation.NonNavigatable;
import consulo.platform.Platform;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ide.localize.IdeLocalize;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessEvent;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.OccurenceNavigator;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.SpeedSearchComparator;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.*;
import consulo.ui.ex.awt.util.ComponentUtil;
import consulo.ui.ex.awt.util.UISettingsUtil;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.image.Image;
import consulo.util.collection.SmartHashSet;
import consulo.util.concurrent.Promise;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static consulo.ide.impl.idea.build.BuildConsoleUtils.getMessageTitle;
import static consulo.ide.impl.idea.build.BuildView.CONSOLE_VIEW_NAME;
import static consulo.ide.impl.idea.util.containers.ContainerUtil.addIfNotNull;
import static consulo.ui.ex.SimpleTextAttributes.GRAYED_ATTRIBUTES;
import static consulo.ui.ex.awt.AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED;
import static consulo.ui.ex.awt.UIUtil.*;
import static consulo.ui.ex.awt.internal.laf.DefaultTreeUI.AUTO_EXPAND_ALLOWED;
import static consulo.ui.ex.awt.util.RenderingHelper.SHRINK_LONG_RENDERER;
import static consulo.util.lang.ObjectUtil.chooseNotNull;
import static consulo.util.lang.StringUtil.isEmpty;

/**
 * @author Vladislav.Soroka
 */
public class BuildTreeConsoleView implements ConsoleView, DataProvider, BuildConsoleView, Filterable<ExecutionNodeImpl>, OccurenceNavigator {
  private static final Logger LOG = Logger.getInstance(BuildTreeConsoleView.class);

  @NonNls
  private static final String TREE = "tree";
  @NonNls
  private static final String SPLITTER_PROPERTY = "BuildView.Splitter.Proportion";
  private final JPanel myPanel = new JPanel();
  private final Map<Object, ExecutionNodeImpl> nodesMap = new ConcurrentHashMap<>();

  private final
  @Nonnull
  Project myProject;
  private final
  @Nonnull
  DefaultBuildDescriptor myBuildDescriptor;
  private final
  @Nonnull
  String myWorkingDir;
  private final ConsoleViewHandler myConsoleViewHandler;
  private final AtomicBoolean myFinishedBuildEventReceived = new AtomicBoolean();
  private final AtomicBoolean myDisposed = new AtomicBoolean();
  private final AtomicBoolean myShownFirstError = new AtomicBoolean();
  private final AtomicBoolean myExpandedFirstMessage = new AtomicBoolean();
  private final boolean myNavigateToTheFirstErrorLocation;
  private final StructureTreeModel<AbstractTreeStructure> myTreeModel;
  private final Tree myTree;
  private final ExecutionNodeImpl myRootNode;
  private final ExecutionNodeImpl myBuildProgressRootNode;
  private final Set<Predicate<? super ExecutionNodeImpl>> myNodeFilters;
  private final ProblemOccurrenceNavigatorSupport myOccurrenceNavigatorSupport;
  private final Set<BuildEvent> myDeferredEvents = ConcurrentHashMap.newKeySet();

  public BuildTreeConsoleView(
    @Nonnull Project project,
    @Nonnull BuildDescriptor buildDescriptor,
    @Nullable ExecutionConsole executionConsole,
    @Nonnull BuildViewSettingsProvider buildViewSettingsProvider
  ) {
    myProject = project;
    myBuildDescriptor = buildDescriptor instanceof DefaultBuildDescriptor defaultBuildDescriptor
      ? defaultBuildDescriptor : new DefaultBuildDescriptor(buildDescriptor);
    myNodeFilters = ConcurrentHashMap.newKeySet();
    myWorkingDir = FileUtil.toSystemIndependentName(buildDescriptor.getWorkingDir());
    myNavigateToTheFirstErrorLocation = project.getInstance(BuildWorkspaceConfiguration.class).isShowFirstErrorInEditor();

    myRootNode = new ExecutionNodeImpl(myProject, null, true, this::isCorrectThread);
    myBuildProgressRootNode = new ExecutionNodeImpl(myProject, myRootNode, true, this::isCorrectThread);
    myRootNode.setFilter(getFilter());
    myRootNode.add(myBuildProgressRootNode);

    AbstractTreeStructure treeStructure = new MyTreeStructure();
    myTreeModel = new StructureTreeModel<>(
      treeStructure,
      null,
      InvokerImpl.forBackgroundThreadWithoutReadAction(this),
      this
    );
    AsyncTreeModel asyncTreeModel = new AsyncTreeModel(myTreeModel, this);
    asyncTreeModel.addTreeModelListener(new ExecutionNodeAutoExpandingListener());
    myTree = initTree(asyncTreeModel);

    JPanel myContentPanel = new JPanel();
    myContentPanel.setLayout(new CardLayout());
    myContentPanel.add(ScrollPaneFactory.createScrollPane(myTree, SideBorder.NONE), TREE);

    myPanel.setLayout(new BorderLayout());
    OnePixelSplitter myThreeComponentsSplitter = new OnePixelSplitter(SPLITTER_PROPERTY, 0.33f);
    myThreeComponentsSplitter.setFirstComponent(myContentPanel);
    List<Filter> filters = myBuildDescriptor.getExecutionFilters();
    myConsoleViewHandler = new ConsoleViewHandler(
      myProject,
      myTree,
      myBuildProgressRootNode,
      this,
      executionConsole,
      filters,
      buildViewSettingsProvider
    );
    myThreeComponentsSplitter.setSecondComponent(myConsoleViewHandler.getComponent());
    myPanel.add(myThreeComponentsSplitter, BorderLayout.CENTER);
    BuildTreeFilters.install(this);
    myOccurrenceNavigatorSupport = new ProblemOccurrenceNavigatorSupport(myTree);
  }

  private boolean isCorrectThread() {
    return myTreeModel == null || myTreeModel.getInvoker().isValidThread();
  }

  private void installContextMenu() {
    invokeLaterIfNeeded(() -> {
      final DefaultActionGroup rerunActionGroup = new DefaultActionGroup();
      List<AnAction> restartActions = myBuildDescriptor.getRestartActions();
      rerunActionGroup.addAll(restartActions);
      if (!restartActions.isEmpty()) {
        rerunActionGroup.addSeparator();
      }

      final DefaultActionGroup sourceActionGroup = new DefaultActionGroup();
      EditSourceAction edit = new EditSourceAction();
      ActionUtil.copyFrom(edit, "EditSource");
      sourceActionGroup.add(edit);
      DefaultActionGroup filteringActionsGroup = BuildTreeFilters.createFilteringActionsGroup(this);
      final DefaultActionGroup navigationActionGroup = new DefaultActionGroup();
      final CommonActionsManager actionsManager = CommonActionsManager.getInstance();
      final AnAction prevAction = actionsManager.createPrevOccurenceAction(this);
      navigationActionGroup.add(prevAction);
      final AnAction nextAction = actionsManager.createNextOccurenceAction(this);
      navigationActionGroup.add(nextAction);

      myTree.addMouseListener(new PopupHandler() {
        @Override
        public void invokePopup(Component comp, int x, int y) {
          final DefaultActionGroup group = new DefaultActionGroup();
          group.addAll(rerunActionGroup);
          group.addAll(sourceActionGroup);
          group.addSeparator();
          ExecutionNodeImpl[] selectedNodes = getSelectedNodes();
          if (selectedNodes.length == 1) {
            ExecutionNodeImpl selectedNode = selectedNodes[0];
            List<AnAction> contextActions = myBuildDescriptor.getContextActions(selectedNode);
            if (!contextActions.isEmpty()) {
              group.addAll(contextActions);
              group.addSeparator();
            }
          }
          group.addAll(filteringActionsGroup);
          group.addSeparator();
          group.addAll(navigationActionGroup);
          ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu("BuildView", group);
          popupMenu.setTargetComponent(myTree);
          JPopupMenu menu = popupMenu.getComponent();
          menu.show(comp, x, y);
        }
      });
    });
  }

  @Override
  public void clear() {
    myTreeModel.getInvoker().runOrInvokeLater(() -> {
      getRootElement().removeChildren();
      nodesMap.clear();
      myConsoleViewHandler.clear();
    });
    scheduleUpdate(getRootElement(), true);
  }

  @Override
  public boolean isFilteringEnabled() {
    return true;
  }

  @Override
  public
  @Nonnull
  Predicate<ExecutionNodeImpl> getFilter() {
    return executionNode -> executionNode == getBuildProgressRootNode()
      || executionNode.isRunning()
      || executionNode.isFailed()
      || myNodeFilters.stream().anyMatch(predicate -> predicate.test(executionNode));
  }

  @Override
  public void addFilter(@Nonnull Predicate<? super ExecutionNodeImpl> executionTreeFilter) {
    myNodeFilters.add(executionTreeFilter);
    updateFilter();
  }

  @Override
  public void removeFilter(@Nonnull Predicate<? super ExecutionNodeImpl> filter) {
    myNodeFilters.remove(filter);
    updateFilter();
  }

  @Override
  public boolean contains(@Nonnull Predicate<? super ExecutionNodeImpl> filter) {
    return myNodeFilters.contains(filter);
  }

  private void updateFilter() {
    ExecutionNodeImpl rootElement = getRootElement();
    myTreeModel.getInvoker().runOrInvokeLater(() -> {
      rootElement.setFilter(getFilter());
      scheduleUpdate(rootElement, true);
    });
  }

  private ExecutionNodeImpl getRootElement() {
    return myRootNode;
  }

  private ExecutionNodeImpl getBuildProgressRootNode() {
    return myBuildProgressRootNode;
  }

  @Override
  public void print(@Nonnull String text, @Nonnull ConsoleViewContentType contentType) {
  }

  private
  @Nullable
  ExecutionNodeImpl getOrMaybeCreateParentNode(@Nonnull BuildEvent event) {
    ExecutionNodeImpl parentNode = event.getParentId() == null ? null : nodesMap.get(event.getParentId());
    if (event instanceof MessageEvent) {
      parentNode = createMessageParentNodes((MessageEvent)event, parentNode);
      if (parentNode != null) {
        scheduleUpdate(parentNode, true); // To update its parent.
      }
    }
    return parentNode;
  }

  private void onEventInternal(@Nonnull Object buildId, @Nonnull BuildEvent event) {
    SmartHashSet<ExecutionNodeImpl> structureChanged = new SmartHashSet<>();
    final ExecutionNodeImpl parentNode = getOrMaybeCreateParentNode(event);
    final Object eventId = event.getId();
    ExecutionNodeImpl currentNode = nodesMap.get(eventId);
    ExecutionNodeImpl buildProgressRootNode = getBuildProgressRootNode();
    Runnable selectErrorNodeTask = null;
    boolean isMessageEvent = event instanceof MessageEvent;
    if (event instanceof StartEvent || isMessageEvent) {
      if (currentNode == null) {
        if (event instanceof DuplicateMessageAware) {
          if (myFinishedBuildEventReceived.get()) {
            if (parentNode != null && parentNode.findFirstChild(node -> event.getMessage().equals(node.getName())) != null) {
              return;
            }
          }
          else {
            myDeferredEvents.add(event);
            return;
          }
        }
        if (event instanceof StartBuildEvent) {
          currentNode = buildProgressRootNode;
          installContextMenu();
          currentNode.setTitle(myBuildDescriptor.getTitle());
        }
        else {
          currentNode = new ExecutionNodeImpl(myProject, parentNode, false, this::isCorrectThread);

          if (isMessageEvent) {
            currentNode.setAlwaysLeaf(event instanceof FileMessageEvent);
            MessageEvent messageEvent = (MessageEvent)event;
            currentNode.setStartTime(messageEvent.getEventTime());
            addIfNotNull(structureChanged, currentNode.setEndTime(messageEvent.getEventTime()));
            Navigatable messageEventNavigatable = messageEvent.getNavigatable(myProject);
            currentNode.setNavigatable(messageEventNavigatable);
            MessageEventResult messageEventResult = messageEvent.getResult();
            addIfNotNull(structureChanged, currentNode.setResult(messageEventResult));

            if (messageEventResult instanceof FailureResult) {
              for (Failure failure : ((FailureResult)messageEventResult).getFailures()) {
                selectErrorNodeTask = selectErrorNodeTask != null
                  ? selectErrorNodeTask : showErrorIfFirst(currentNode, failure.getNavigatable());
              }
            }
            if (messageEvent.getKind() == MessageEvent.Kind.ERROR) {
              selectErrorNodeTask = selectErrorNodeTask != null
                ? selectErrorNodeTask : showErrorIfFirst(currentNode, messageEventNavigatable);
            }

            if (parentNode != null) {
              if (parentNode != buildProgressRootNode) {
                myConsoleViewHandler.addOutput(parentNode, buildId, event);
                myConsoleViewHandler.addOutput(parentNode, "\n", true);
              }
              reportMessageKind(messageEvent.getKind(), parentNode);
            }
            myConsoleViewHandler.addOutput(currentNode, buildId, event);
          }
          if (parentNode != null) {
            structureChanged.add(parentNode);
            parentNode.add(currentNode);
          }
        }
        nodesMap.put(eventId, currentNode);
      }
      else {
        LOG.warn("start event id collision found:" + eventId + ", was also in node: " + currentNode.getTitle());
        return;
      }
    }
    else {
      boolean isProgress = event instanceof ProgressBuildEvent;
      currentNode = nodesMap.get(eventId);
      if (currentNode == null) {
        if (isProgress) {
          currentNode = new ExecutionNodeImpl(
            myProject,
            parentNode,
            parentNode == buildProgressRootNode,
            this::isCorrectThread
          );
          nodesMap.put(eventId, currentNode);
          if (parentNode != null) {
            structureChanged.add(parentNode);
            parentNode.add(currentNode);
          }
        }
        else if (event instanceof OutputBuildEvent && parentNode != null) {
          myConsoleViewHandler.addOutput(parentNode, buildId, event);
        }
        else if (event instanceof PresentableBuildEvent) {
          currentNode = addAsPresentableEventNode(
            (PresentableBuildEvent)event,
            structureChanged,
            parentNode,
            eventId,
            buildProgressRootNode
          );
        }
      }

      if (isProgress) {
        ProgressBuildEvent progressBuildEvent = (ProgressBuildEvent)event;
        long total = progressBuildEvent.getTotal();
        long progress = progressBuildEvent.getProgress();
        if (currentNode == myBuildProgressRootNode) {
          myConsoleViewHandler.updateProgressBar(total, progress);
        }
      }
    }

    if (currentNode == null) {
      return;
    }

    currentNode.setName(event.getMessage());
    currentNode.setHint(event.getHint());
    if (currentNode.getStartTime() == 0) {
      currentNode.setStartTime(event.getEventTime());
    }

    if (event instanceof FinishEvent) {
      EventResult result = ((FinishEvent)event).getResult();
      if (result instanceof DerivedResult) {
        result = calculateDerivedResult((DerivedResult)result, currentNode);
      }
      addIfNotNull(structureChanged, currentNode.setResult(result));
      addIfNotNull(structureChanged, currentNode.setEndTime(event.getEventTime()));
      SkippedResult skippedResult = new SkippedResultImpl();
      finishChildren(structureChanged, currentNode, skippedResult);
      if (result instanceof FailureResult) {
        for (Failure failure : ((FailureResult)result).getFailures()) {
          Runnable task = addChildFailureNode(
            currentNode,
            failure,
            event.getMessage(),
            event.getEventTime(),
            structureChanged
          );
          if (selectErrorNodeTask == null) selectErrorNodeTask = task;
        }
      }
    }

    if (event instanceof FinishBuildEvent) {
      myFinishedBuildEventReceived.set(true);
      String aHint = event.getHint();
      String time = DateFormatUtil.formatDateTime(event.getEventTime());
      aHint = aHint == null ? BuildLocalize.buildEventMessageAt(time).get() : BuildLocalize.buildEventMessage0At1(aHint, time).get();
      currentNode.setHint(aHint);
      myDeferredEvents.forEach(buildEvent -> onEventInternal(buildId, buildEvent));
      if (myConsoleViewHandler.myExecutionNode == null) {
        invokeLater(() -> myConsoleViewHandler.setNode(buildProgressRootNode));
      }
      myConsoleViewHandler.stopProgressBar();
    }
    if (structureChanged.isEmpty()) {
      scheduleUpdate(currentNode, false);
    }
    else {
      for (ExecutionNodeImpl node : structureChanged) {
        scheduleUpdate(node, true);
      }
    }
    if (selectErrorNodeTask != null) {
      myExpandedFirstMessage.set(true);
      Runnable finalSelectErrorTask = selectErrorNodeTask;
      myTreeModel.invalidate(getRootElement(), true).onProcessed(p -> finalSelectErrorTask.run());
    }
    else {
      if (isMessageEvent && myExpandedFirstMessage.compareAndSet(false, true)) {
        ExecutionNodeImpl finalCurrentNode = currentNode;
        myTreeModel.invalidate(getRootElement(), false)
          .onProcessed(p -> TreeUtil.promiseMakeVisible(myTree, visitor(finalCurrentNode)));
      }
    }
  }

  @Nonnull
  private ExecutionNodeImpl addAsPresentableEventNode(
    @Nonnull PresentableBuildEvent event,
    @Nonnull SmartHashSet<ExecutionNodeImpl> structureChanged,
    @Nullable ExecutionNodeImpl parentNode,
    @Nonnull Object eventId,
    @Nonnull ExecutionNodeImpl buildProgressRootNode
  ) {
    ExecutionNodeImpl executionNode = new ExecutionNodeImpl(
      myProject,
      parentNode,
      parentNode == buildProgressRootNode,
      this::isCorrectThread
    );
    BuildEventPresentationData presentationData = event.getPresentationData();
    executionNode.applyFrom(presentationData);
    nodesMap.put(eventId, executionNode);
    if (parentNode != null) {
      structureChanged.add(parentNode);
      parentNode.add(executionNode);
    }
    myConsoleViewHandler.maybeAddExecutionConsole(executionNode, presentationData);
    return executionNode;
  }

  //@ApiStatus.Internal
  @TestOnly
  public
  @Nullable
  ExecutionConsole getSelectedNodeConsole() {
    ExecutionConsole console = myConsoleViewHandler.getCurrentConsole();
    if (console instanceof ConsoleViewImpl consoleView) {
      consoleView.flushDeferredText();
    }
    return console;
  }

  private static EventResult calculateDerivedResult(DerivedResult result, ExecutionNodeImpl node) {
    if (node.getResult() != null) {
      return node.getResult(); // if another thread set result for child
    }
    if (node.isFailed()) {
      return result.createFailureResult();
    }

    return result.createDefaultResult();
  }

  private void reportMessageKind(@Nonnull MessageEvent.Kind eventKind, @Nonnull ExecutionNodeImpl parentNode) {
    if (eventKind == MessageEvent.Kind.ERROR
      || eventKind == MessageEvent.Kind.WARNING
      || eventKind == MessageEvent.Kind.INFO) {
      ExecutionNodeImpl executionNode = parentNode;
      do {
        ExecutionNodeImpl updatedRoot = executionNode.reportChildMessageKind(eventKind);
        if (updatedRoot != null) {
          scheduleUpdate(updatedRoot, true);
        }
        else {
          scheduleUpdate(executionNode, false);
        }
      }
      while ((executionNode = executionNode.getParent()) != null);
      scheduleUpdate(getRootElement(), false);
    }
  }

  @Nullable
  private Runnable showErrorIfFirst(@Nonnull ExecutionNodeImpl node, @Nullable Navigatable navigatable) {
    if (myShownFirstError.compareAndSet(false, true)) {
      return () -> {
        TreeUtil.promiseSelect(myTree, visitor(node));
        if (myNavigateToTheFirstErrorLocation && navigatable != null && navigatable != NonNavigatable.INSTANCE) {
          Application.get().invokeLater(
            () -> navigatable.navigate(true),
            Application.get().getDefaultModalityState(),
            myProject.getDisposed()
          );
        }
      };
    }
    return null;
  }

  @Override
  public boolean hasNextOccurence() {
    return myOccurrenceNavigatorSupport.hasNextOccurence();
  }

  @Override
  public boolean hasPreviousOccurence() {
    return myOccurrenceNavigatorSupport.hasPreviousOccurence();
  }

  @Override
  public OccurenceInfo goNextOccurence() {
    return myOccurrenceNavigatorSupport.goNextOccurence();
  }

  @Override
  public OccurenceInfo goPreviousOccurence() {
    return myOccurrenceNavigatorSupport.goPreviousOccurence();
  }

  @Override
  public
  @Nonnull
  String getNextOccurenceActionName() {
    return myOccurrenceNavigatorSupport.getNextOccurenceActionName();
  }

  @Override
  public
  @Nonnull
  String getPreviousOccurenceActionName() {
    return myOccurrenceNavigatorSupport.getPreviousOccurenceActionName();
  }

  private static
  @Nonnull
  TreeVisitor visitor(@Nonnull ExecutionNodeImpl executionNode) {
    TreePath treePath = TreePathUtil.pathToCustomNode(executionNode, ExecutionNodeImpl::getParent);
    return new TreeVisitor.ByTreePath<>(treePath, o -> (ExecutionNodeImpl)TreeUtil.getUserObject(o));
  }

  private
  @Nullable
  Runnable addChildFailureNode(
    @Nonnull ExecutionNodeImpl parentNode,
    @Nonnull Failure failure,
    @Nonnull String defaultFailureMessage,
    long eventTime,
    @Nonnull Set<ExecutionNodeImpl> structureChanged
  ) {
    String message = chooseNotNull(failure.getMessage(), failure.getDescription());
    if (message == null) {
      Throwable error = failure.getError();
      message = error != null ? error.getMessage() : defaultFailureMessage;
    }
    String failureNodeName = getMessageTitle(message);
    Navigatable failureNavigatable = failure.getNavigatable();
    FilePosition filePosition = null;
    if (failureNavigatable instanceof OpenFileDescriptorImpl) {
      OpenFileDescriptorImpl fileDescriptor = (OpenFileDescriptorImpl)failureNavigatable;
      File file = VfsUtilCore.virtualToIoFile(fileDescriptor.getFile());
      filePosition = new FilePosition(file, fileDescriptor.getLine(), fileDescriptor.getColumn());
      parentNode = createMessageParentNodes(eventTime, filePosition, failureNavigatable, parentNode);
    }
    else if (failureNavigatable instanceof FileNavigatable fileNavigatable) {
      filePosition = fileNavigatable.getFilePosition();
      parentNode = createMessageParentNodes(eventTime, filePosition, failureNavigatable, parentNode);
    }

    ExecutionNodeImpl failureNode = parentNode.findFirstChild(executionNode -> failureNodeName.equals(executionNode.getName()));
    if (failureNode == null) {
      failureNode = new ExecutionNodeImpl(myProject, parentNode, true, this::isCorrectThread);
      failureNode.setName(failureNodeName);
      if (filePosition != null && filePosition.getStartLine() >= 0) {
        String hint = ":" + (filePosition.getStartLine() + 1);
        failureNode.setHint(hint);
      }
      parentNode.add(failureNode);
      reportMessageKind(MessageEvent.Kind.ERROR, parentNode);
    }
    if (failureNavigatable != null && failureNavigatable != NonNavigatable.INSTANCE) {
      failureNode.setNavigatable(failureNavigatable);
    }

    List<Failure> failures;
    EventResult result = failureNode.getResult();
    if (result instanceof FailureResult failureResult) {
      failures = new ArrayList<>(failureResult.getFailures());
      failures.add(failure);
    }
    else {
      failures = Collections.singletonList(failure);
    }
    ExecutionNodeImpl updatedRoot = failureNode.setResult(new FailureResultImpl(failures));
    if (updatedRoot == null) {
      updatedRoot = parentNode;
    }
    structureChanged.add(updatedRoot);
    myConsoleViewHandler.addOutput(failureNode, failure);
    return showErrorIfFirst(failureNode, failureNavigatable);
  }

  private static void finishChildren(
    @Nonnull SmartHashSet<ExecutionNodeImpl> structureChanged,
    @Nonnull ExecutionNodeImpl node,
    @Nonnull EventResult result
  ) {
    List<ExecutionNodeImpl> childList = node.getChildList();
    if (childList.isEmpty()) return;
    // Make a copy of the list since child.setResult may remove items from the collection.
    for (ExecutionNodeImpl child : new ArrayList<>(childList)) {
      if (!child.isRunning()) {
        continue;
      }
      finishChildren(structureChanged, child, result);
      addIfNotNull(structureChanged, child.setResult(result));
    }
  }

  @Override
  public void scrollTo(int offset) {
  }

  @Override
  public void attachToProcess(@Nonnull ProcessHandler processHandler) {
  }

  @Override
  public boolean isOutputPaused() {
    return false;
  }

  @Override
  public void setOutputPaused(boolean value) {
  }

  @Override
  public boolean hasDeferredOutput() {
    return false;
  }

  @Override
  public void performWhenNoDeferredOutput(@Nonnull Runnable runnable) {
  }

  @Override
  public void setHelpId(@Nonnull String helpId) {
  }

  @Override
  public void addMessageFilter(@Nonnull Filter filter) {
  }

  @Override
  public void printHyperlink(@Nonnull String hyperlinkText, @Nullable HyperlinkInfo info) {
  }

  @Override
  public void setProcessTextFilter(@Nullable BiPredicate<ProcessEvent, Key> filter) {

  }

  @Nullable
  @Override
  public BiPredicate<ProcessEvent, Key> getProcessTextFilter() {
    return null;
  }

  @Override
  public int getContentSize() {
    return 0;
  }

  @Override
  public boolean canPause() {
    return false;
  }

  @Override
  @Nonnull
  public AnAction[] createConsoleActions() {
    return AnAction.EMPTY_ARRAY;
  }

  @Override
  public void allowHeavyFilters() {
  }

  @Override
  public
  @Nonnull
  JComponent getComponent() {
    return myPanel;
  }

  @Override
  public JComponent getPreferredFocusableComponent() {
    return myTree;
  }

  @Override
  public void dispose() {
    myDisposed.set(true);
  }

  public boolean isDisposed() {
    return myDisposed.get();
  }

  @Override
  public void onEvent(@Nonnull Object buildId, @Nonnull BuildEvent event) {
    myTreeModel.getInvoker().invoke(() -> onEventInternal(buildId, event));
  }

  void scheduleUpdate(ExecutionNodeImpl executionNode, boolean parentStructureChanged) {
    ExecutionNodeImpl node = (executionNode.getParent() == null || !parentStructureChanged) ? executionNode : executionNode.getParent();
    myTreeModel.invalidate(node, parentStructureChanged);
  }

  private ExecutionNodeImpl createMessageParentNodes(MessageEvent messageEvent, ExecutionNodeImpl parentNode) {
    Object messageEventParentId = messageEvent.getParentId();
    if (messageEventParentId == null) return null;
    if (messageEvent instanceof FileMessageEvent) {
      return createMessageParentNodes(
        messageEvent.getEventTime(),
        ((FileMessageEvent)messageEvent).getFilePosition(),
        messageEvent.getNavigatable(myProject),
        parentNode
      );
    }
    else {
      return parentNode;
    }
  }

  private ExecutionNodeImpl createMessageParentNodes(
    long eventTime,
    @Nonnull FilePosition filePosition,
    @Nullable Navigatable navigatable,
    ExecutionNodeImpl parentNode
  ) {
    String filePath = FileUtil.toSystemIndependentName(filePosition.getFile().getPath());
    String parentsPath = "";

    String relativePath = FileUtil.getRelativePath(myWorkingDir, filePath, '/');
    if (relativePath != null) {
      if (relativePath.equals(".")) {
        return parentNode;
      }
      if (!relativePath.startsWith("../../")) {
        parentsPath = myWorkingDir;
      }
    }

    if (isEmpty(parentsPath)) {
      File userHomeDir = Platform.current().user().homePath().toFile();
      if (FileUtil.isAncestor(userHomeDir, new File(filePath), true)) {
        relativePath = UserHomeFileUtil.getLocationRelativeToUserHome(filePath, false);
      }
      else {
        relativePath = filePath;
      }
    }
    else {
      relativePath = getRelativePath(parentsPath, filePath);
    }
    Path path = Paths.get(relativePath);
    String nodeName = path.getFileName().toString();
    Path pathParent = path.getParent();
    String pathHint = pathParent == null ? null : pathParent.toString();
    parentNode = getOrCreateMessagesNode(eventTime, filePath, parentNode, nodeName, pathHint, () -> {
      VirtualFile file = VfsUtil.findFileByIoFile(filePosition.getFile(), false);
      if (file != null) {
        return file.getFileType().getIcon();
      }
      return null;
    }, navigatable, nodesMap, myProject);
    return parentNode;
  }

  private static String getRelativePath(@Nonnull String basePath, @Nonnull String filePath) {
    String path = ObjectUtil.notNull(FileUtil.getRelativePath(basePath, filePath, '/'), filePath);
    File userHomeDir = Platform.current().user().homePath().toFile();
    if (path.startsWith("..") && FileUtil.isAncestor(userHomeDir, new File(filePath), true)) {
      return UserHomeFileUtil.getLocationRelativeToUserHome(filePath, false);
    }
    return path;
  }

  public void hideRootNode() {
    invokeLaterIfNeeded(() -> {
      if (myTree != null) {
        myTree.setRootVisible(false);
        myTree.setShowsRootHandles(true);
      }
    });
  }

  @Override
  @Nullable
  public Object getData(@Nonnull Key dataId) {
    if (PlatformDataKeys.HELP_ID == dataId) return "reference.build.tool.window";
    if (Project.KEY == dataId) return myProject;
    if (Navigatable.KEY_OF_ARRAY == dataId) return extractSelectedNodesNavigatables();
    if (Navigatable.KEY == dataId) return extractSelectedNodeNavigatable();
    return null;
  }

  private
  @Nullable
  Object extractSelectedNodeNavigatable() {
    TreePath selectedPath = TreeUtil.getSelectedPathIfOne(myTree);
    if (selectedPath == null) return null;
    DefaultMutableTreeNode node = ObjectUtil.tryCast(selectedPath.getLastPathComponent(), DefaultMutableTreeNode.class);
    if (node == null) return null;
    ExecutionNodeImpl executionNode = ObjectUtil.tryCast(node.getUserObject(), ExecutionNodeImpl.class);
    if (executionNode == null) return null;
    List<Navigatable> navigatables = executionNode.getNavigatables();
    if (navigatables.size() != 1) return null;
    return navigatables.get(0);
  }

  private Object extractSelectedNodesNavigatables() {
    final List<Navigatable> navigatables = new ArrayList<>();
    for (ExecutionNodeImpl each : getSelectedNodes()) {
      List<Navigatable> navigatable = each.getNavigatables();
      navigatables.addAll(navigatable);
    }
    return navigatables.isEmpty() ? null : navigatables.toArray(new Navigatable[0]);
  }

  private ExecutionNodeImpl[] getSelectedNodes() {
    final ExecutionNodeImpl[] result = new ExecutionNodeImpl[0];
    if (myTree != null) {
      final List<ExecutionNodeImpl> nodes =
        TreeUtil.collectSelectedObjects(myTree, path -> TreeUtil.getLastUserObject(ExecutionNodeImpl.class, path));
      return nodes.toArray(result);
    }
    return result;
  }

  //@ApiStatus.Internal
  public JTree getTree() {
    return myTree;
  }

  private static Tree initTree(@Nonnull AsyncTreeModel model) {
    Tree tree = new Tree(model);
    tree.setLargeModel(true);
    ComponentUtil.putClientProperty(tree, ANIMATION_IN_RENDERER_ALLOWED, true);
    ComponentUtil.putClientProperty(tree, AUTO_EXPAND_ALLOWED, false);
    tree.setRootVisible(false);
    EditSourceOnDoubleClickHandler.install(tree);
    EditSourceOnEnterKeyHandler.install(tree);
    new TreeSpeedSearch(tree).setComparator(new SpeedSearchComparator(false));
    TreeUtil.installActions(tree);
    tree.setCellRenderer(new MyNodeRenderer());
    tree.putClientProperty(SHRINK_LONG_RENDERER, true);
    return tree;
  }

  private
  @Nonnull
  ExecutionNodeImpl getOrCreateMessagesNode(
    long eventTime,
    String nodeId,
    ExecutionNodeImpl parentNode,
    String nodeName,
    @Nullable @BuildEventsNls.Hint String hint,
    @Nullable Supplier<? extends Image> iconProvider,
    @Nullable Navigatable navigatable,
    Map<Object, ExecutionNodeImpl> nodesMap,
    Project project
  ) {
    ExecutionNodeImpl node = nodesMap.get(nodeId);
    if (node == null) {
      node = new ExecutionNodeImpl(project, parentNode, false, this::isCorrectThread);
      node.setName(nodeName);
      if (hint != null) {
        node.setHint(hint);
      }
      node.setStartTime(eventTime);
      node.setEndTime(eventTime);
      if (iconProvider != null) {
        node.setIconProvider(iconProvider);
      }
      if (navigatable != null) {
        node.setNavigatable(navigatable);
      }
      parentNode.add(node);
      nodesMap.put(nodeId, node);
    }
    return node;
  }

  //@ApiStatus.Internal
  public Promise<?> invokeLater(@Nonnull Runnable task) {
    return myTreeModel.getInvoker().invokeLater(task);
  }

  private static class ConsoleViewHandler implements Disposable {
    private static final String EMPTY_CONSOLE_NAME = "empty";
    private final Project myProject;
    private final JPanel myPanel;
    private final CompositeView<ExecutionConsole> myView;
    private final AtomicReference<String> myNodeConsoleViewName = new AtomicReference<>();
    private final Map<String, List<Consumer<? super BuildTextConsoleView>>> deferredNodeOutput = new ConcurrentHashMap<>();
    private final
    @Nonnull
    BuildViewSettingsProvider myViewSettingsProvider;
    private
    @Nullable
    ExecutionNodeImpl myExecutionNode;
    private
    @Nonnull
    final List<Filter> myExecutionConsoleFilters;
    private final BuildProgressStripe myPanelWithProgress;
    private final DefaultActionGroup myConsoleToolbarActionGroup;
    private final ActionToolbar myToolbar;

    ConsoleViewHandler(
      @Nonnull Project project,
      @Nonnull Tree tree,
      @Nonnull ExecutionNodeImpl buildProgressRootNode,
      @Nonnull Disposable parentDisposable,
      @Nullable ExecutionConsole executionConsole,
      @Nonnull List<Filter> executionConsoleFilters,
      @Nonnull BuildViewSettingsProvider buildViewSettingsProvider
    ) {
      myProject = project;
      myPanel = new NonOpaquePanel(new BorderLayout());
      myPanelWithProgress = new BuildProgressStripe(
        myPanel,
        parentDisposable,
        ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS
      );
      myViewSettingsProvider = buildViewSettingsProvider;
      myExecutionConsoleFilters = executionConsoleFilters;
      Disposer.register(parentDisposable, this);
      myView = new CompositeView<>(null) {
        @Override
        public void addView(@Nonnull ExecutionConsole view, @Nonnull String viewName) {
          super.addView(view, viewName);
          removeScrollBorder(view.getComponent());
        }
      };
      Disposer.register(this, myView);
      if (executionConsole != null) {
        String nodeConsoleViewName = getNodeConsoleViewName(buildProgressRootNode);
        myView.addViewAndShowIfNeeded(executionConsole, nodeConsoleViewName, true);
        myNodeConsoleViewName.set(nodeConsoleViewName);
      }
      ConsoleView emptyConsole = new ConsoleViewImpl(project, GlobalSearchScope.EMPTY_SCOPE, true, false);
      myView.addView(emptyConsole, EMPTY_CONSOLE_NAME);
      JComponent consoleComponent = emptyConsole.getComponent();
      consoleComponent.setFocusable(true);
      myPanel.add(myView.getComponent(), BorderLayout.CENTER);
      myConsoleToolbarActionGroup = new DefaultActionGroup();
      myToolbar = ActionManager.getInstance().createActionToolbar("BuildConsole", myConsoleToolbarActionGroup, false);
      myToolbar.setTargetComponent(myPanel);
      showTextConsoleToolbarActions();
      myPanel.add(myToolbar.getComponent(), BorderLayout.EAST);
      tree.addTreeSelectionListener(e -> {
        TreePath path = e.getPath();
        if (path == null || !e.isAddedPath()) {
          return;
        }
        TreePath selectionPath = tree.getSelectionPath();
        setNode(selectionPath != null ? (DefaultMutableTreeNode)selectionPath.getLastPathComponent() : null);
      });
    }

    private void showTextConsoleToolbarActions() {
      myConsoleToolbarActionGroup.copyFromGroup(createDefaultTextConsoleToolbar());
      updateToolbarActionsImmediately();
    }

    private void showCustomConsoleToolbarActions(@Nullable ActionGroup actionGroup) {
      if (actionGroup instanceof DefaultActionGroup defaultActionGroup) {
        myConsoleToolbarActionGroup.copyFromGroup(defaultActionGroup);
      }
      else if (actionGroup != null) {
        myConsoleToolbarActionGroup.copyFrom(actionGroup);
      }
      else {
        myConsoleToolbarActionGroup.removeAll();
      }
      updateToolbarActionsImmediately();
    }

    private void updateToolbarActionsImmediately() {
      invokeLaterIfNeeded(myToolbar::updateActionsImmediately);
    }

    @Nonnull
    private DefaultActionGroup createDefaultTextConsoleToolbar() {
      DefaultActionGroup textConsoleToolbarActionGroup = new DefaultActionGroup();
      textConsoleToolbarActionGroup.add(new ToggleUseSoftWrapsToolbarAction(SoftWrapAppliancePlaces.CONSOLE) {
        @Override
        protected
        @Nullable
        Editor getEditor(@Nonnull AnActionEvent e) {
          return ConsoleViewHandler.this.getEditor();
        }
      });
      textConsoleToolbarActionGroup.add(new ScrollEditorToTheEndAction(this));
      return textConsoleToolbarActionGroup;
    }

    private void updateProgressBar(long total, long progress) {
      myPanelWithProgress.updateProgress(total, progress);
    }

    private
    @Nullable
    ExecutionConsole getCurrentConsole() {
      String nodeConsoleViewName = myNodeConsoleViewName.get();
      if (nodeConsoleViewName == null) return null;
      return myView.getView(nodeConsoleViewName);
    }

    private
    @Nullable
    Editor getEditor() {
      ExecutionConsole console = getCurrentConsole();
      if (console instanceof ConsoleViewImpl consoleView) {
        return consoleView.getEditor();
      }
      return null;
    }

    private boolean setNode(@Nonnull ExecutionNodeImpl node) {
      String nodeConsoleViewName = getNodeConsoleViewName(node);
      myNodeConsoleViewName.set(nodeConsoleViewName);
      ExecutionConsole view = myView.getView(nodeConsoleViewName);
      if (view != null) {
        List<Consumer<? super BuildTextConsoleView>> deferredOutput = deferredNodeOutput.get(nodeConsoleViewName);
        if (view instanceof BuildTextConsoleView && deferredOutput != null && !deferredOutput.isEmpty()) {
          deferredNodeOutput.remove(nodeConsoleViewName);
          deferredOutput.forEach(consumer -> consumer.accept((BuildTextConsoleView)view));
        }
        else {
          deferredNodeOutput.remove(nodeConsoleViewName);
        }
        myView.showView(nodeConsoleViewName, false);
        if (view instanceof PresentableBuildEventExecutionConsole) {
          showCustomConsoleToolbarActions(((PresentableBuildEventExecutionConsole)view).myActions);
        }
        else {
          showTextConsoleToolbarActions();
        }
        myPanel.setVisible(true);
        return true;
      }

      List<Consumer<? super BuildTextConsoleView>> deferredOutput = deferredNodeOutput.get(nodeConsoleViewName);
      if (deferredOutput != null && !deferredOutput.isEmpty()) {
        BuildTextConsoleView textConsoleView = new BuildTextConsoleView(myProject, true, myExecutionConsoleFilters);
        deferredNodeOutput.remove(nodeConsoleViewName);
        deferredOutput.forEach(consumer -> consumer.accept(textConsoleView));
        myView.addView(textConsoleView, nodeConsoleViewName);
        myView.showView(nodeConsoleViewName, false);
      }
      else {
        myView.showView(EMPTY_CONSOLE_NAME, false);
        return true;
      }
      return true;
    }

    public void maybeAddExecutionConsole(@Nonnull ExecutionNodeImpl node, @Nonnull BuildEventPresentationData presentationData) {
      invokeLaterIfNeeded(() -> {
        ExecutionConsole executionConsole = presentationData.getExecutionConsole();
        if (executionConsole == null) return;
        String nodeConsoleViewName = getNodeConsoleViewName(node);
        PresentableBuildEventExecutionConsole presentableEventView =
          new PresentableBuildEventExecutionConsole(executionConsole, presentationData.consoleToolbarActions());
        myView.addView(presentableEventView, nodeConsoleViewName);
      });
    }

    private void addOutput(@Nonnull ExecutionNodeImpl node, @Nonnull String text, boolean stdOut) {
      addOutput(node, view -> view.append(text, stdOut));
    }

    private void addOutput(@Nonnull ExecutionNodeImpl node, @Nonnull Object buildId, BuildEvent event) {
      addOutput(node, view -> view.onEvent(buildId, event));
    }

    private void addOutput(@Nonnull ExecutionNodeImpl node, Failure failure) {
      addOutput(node, view -> view.append(failure));
    }

    private void addOutput(@Nonnull ExecutionNodeImpl node, Consumer<? super BuildTextConsoleView> consumer) {
      String nodeConsoleViewName = getNodeConsoleViewName(node);
      ExecutionConsole viewView = myView.getView(nodeConsoleViewName);
      if (viewView instanceof BuildTextConsoleView) {
        consumer.accept((BuildTextConsoleView)viewView);
      }
      if (viewView == null) {
        deferredNodeOutput.computeIfAbsent(nodeConsoleViewName, s -> new ArrayList<>()).add(consumer);
      }
    }

    @Override
    public void dispose() {
      deferredNodeOutput.clear();
    }

    private void stopProgressBar() {
      myPanelWithProgress.stopLoading();
    }

    private static
    @Nonnull
    String getNodeConsoleViewName(@Nonnull ExecutionNodeImpl node) {
      return String.valueOf(System.identityHashCode(node));
    }

    private void setNode(@Nullable DefaultMutableTreeNode node) {
      if (myProject.isDisposed()) return;
      if (node == null || node.getUserObject() == myExecutionNode) return;
      if (node.getUserObject() instanceof ExecutionNodeImpl) {
        myExecutionNode = (ExecutionNodeImpl)node.getUserObject();
        if (setNode((ExecutionNodeImpl)node.getUserObject())) {
          return;
        }
      }

      myExecutionNode = null;
      if (myView.getView(CONSOLE_VIEW_NAME) != null/* && myViewSettingsProvider.isSideBySideView()*/) {
        myView.showView(CONSOLE_VIEW_NAME, false);
        myPanel.setVisible(true);
      }
      else {
        myPanel.setVisible(false);
      }
    }

    public JComponent getComponent() {
      return myPanelWithProgress;
    }

    public void clear() {
      myPanel.setVisible(false);
    }

    private static class PresentableBuildEventExecutionConsole implements ExecutionConsole {
      private final ExecutionConsole myExecutionConsole;
      private final
      @Nullable
      ActionGroup myActions;

      private PresentableBuildEventExecutionConsole(@Nonnull ExecutionConsole executionConsole, @Nullable ActionGroup toolbarActions) {
        myExecutionConsole = executionConsole;
        myActions = toolbarActions;
      }

      @Override
      public
      @Nonnull
      JComponent getComponent() {
        return myExecutionConsole.getComponent();
      }

      @Override
      public JComponent getPreferredFocusableComponent() {
        return myExecutionConsole.getPreferredFocusableComponent();
      }

      @Override
      public void dispose() {
        Disposer.dispose(myExecutionConsole);
      }
    }
  }

  private static class ProblemOccurrenceNavigatorSupport extends OccurenceNavigatorSupport {
    ProblemOccurrenceNavigatorSupport(final Tree tree) {
      super(tree);
    }

    @Override
    protected Navigatable createDescriptorForNode(@Nonnull DefaultMutableTreeNode node) {
      Object userObject = node.getUserObject();
      if (!(userObject instanceof ExecutionNodeImpl)) {
        return null;
      }
      final ExecutionNodeImpl executionNode = (ExecutionNodeImpl)userObject;
      if (node.getChildCount() != 0 || !executionNode.hasWarnings() && !executionNode.isFailed()) {
        return null;
      }
      List<Navigatable> navigatables = executionNode.getNavigatables();
      if (!navigatables.isEmpty()) {
        return navigatables.get(0);
      }
      return null;
    }

    @Override
    @Nonnull
    public String getNextOccurenceActionName() {
      return IdeLocalize.actionNextProblem().get();
    }

    @Override
    @Nonnull
    public String getPreviousOccurenceActionName() {
      return IdeLocalize.actionPreviousProblem().get();
    }
  }

  private static class ScrollEditorToTheEndAction extends ToggleAction implements DumbAware {
    private final
    @Nonnull
    ConsoleViewHandler myConsoleViewHandler;

    ScrollEditorToTheEndAction(@Nonnull ConsoleViewHandler handler) {
      super(ActionLocalize.actionEditorconsolescrolltotheendText(), LocalizeValue.empty(), AllIcons.RunConfigurations.Scroll_down);
      myConsoleViewHandler = handler;
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
      Editor editor = myConsoleViewHandler.getEditor();
      if (editor == null) return false;
      Document document = editor.getDocument();
      return document.getLineCount() == 0 || document.getLineNumber(editor.getCaretModel().getOffset()) == document.getLineCount() - 1;
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
      Editor editor = myConsoleViewHandler.getEditor();
      if (editor == null) return;
      if (state) {
        EditorUtil.scrollToTheEnd(editor);
      }
      else {
        int lastLine = Math.max(0, editor.getDocument().getLineCount() - 1);
        LogicalPosition currentPosition = editor.getCaretModel().getLogicalPosition();
        LogicalPosition position =
          new LogicalPosition(Math.max(0, Math.min(currentPosition.line, lastLine - 1)), currentPosition.column);
        editor.getCaretModel().moveToLogicalPosition(position);
      }
    }
  }

  private static class MyNodeRenderer extends NodeRenderer {
    private String myDurationText;
    private Color myDurationColor;
    private int myDurationWidth;
    private int myDurationOffset;

    @RequiredUIAccess
    @Override
    public void customizeCellRenderer(
      @Nonnull JTree tree,
      Object value,
      boolean selected,
      boolean expanded,
      boolean leaf,
      int row,
      boolean hasFocus
    ) {
      super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);
      myDurationText = null;
      myDurationColor = null;
      myDurationWidth = 0;
      myDurationOffset = 0;
      final DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
      final Object userObj = node.getUserObject();
      if (userObj instanceof ExecutionNodeImpl) {
        myDurationText = ((ExecutionNodeImpl)userObj).getDuration();
        if (myDurationText != null) {
          FontMetrics metrics = getFontMetrics(RelativeFont.SMALL.derive(getFont()));
          myDurationWidth = metrics.stringWidth(myDurationText);
          myDurationOffset = metrics.getHeight() / 2; // an empty area before and after the text
          myDurationColor = selected ? getTreeSelectionForeground(hasFocus) : GRAYED_ATTRIBUTES.getFgColor();
        }
      }
    }

    @Override
    protected void paintComponent(Graphics g) {
      UISettingsUtil.setupAntialiasing(g);
      Shape clip = null;
      int width = getWidth();
      int height = getHeight();
      if (isOpaque()) {
        // paint background for expanded row
        g.setColor(getBackground());
        g.fillRect(0, 0, width, height);
      }
      if (myDurationWidth > 0) {
        width -= myDurationWidth + myDurationOffset;
        if (width > 0 && height > 0) {
          g.setColor(myDurationColor);
          g.setFont(RelativeFont.SMALL.derive(getFont()));
          g.drawString(myDurationText, width + myDurationOffset / 2, getTextBaseLine(g.getFontMetrics(), height));
          clip = g.getClip();
          g.clipRect(0, 0, width, height);
        }
      }

      super.paintComponent(g);
      // restore clip area if needed
      if (clip != null) g.setClip(clip);
    }
  }

  private class MyTreeStructure extends AbstractTreeStructure {
    @Override
    public
    @Nonnull
    Object getRootElement() {
      return myRootNode;
    }

    @Override
    public
    @Nonnull
    Object[] getChildElements(@Nonnull Object element) {
      // This .toArray() is still slow but it is called less frequently because of batching in AsyncTreeModel and process less data if
      // filters are applied.
      return ((ExecutionNodeImpl)element).getChildList().toArray();
    }

    @Override
    public
    @Nullable
    Object getParentElement(@Nonnull Object element) {
      return ((ExecutionNodeImpl)element).getParent();
    }

    @Override
    public
    @Nonnull
    NodeDescriptor createDescriptor(@Nonnull Object element, @Nullable NodeDescriptor parentDescriptor) {
      return ((NodeDescriptor)element);
    }

    @Override
    public void commit() {
    }

    @Override
    public boolean hasSomethingToCommit() {
      return false;
    }

    @Override
    public boolean isAlwaysLeaf(@Nonnull Object element) {
      return ((ExecutionNodeImpl)element).isAlwaysLeaf();
    }
  }

  private class ExecutionNodeAutoExpandingListener implements TreeModelListener {
    @Override
    public void treeNodesInserted(TreeModelEvent e) {
      maybeExpand(e.getTreePath());
    }

    @Override
    public void treeNodesChanged(TreeModelEvent e) {
      // ExecutionNodeImpl should never change its isAutoExpand state. Ignore calls and do nothing.
    }

    @Override
    public void treeNodesRemoved(TreeModelEvent e) {
      // A removed node is not a reason to expand it parent. Ignore calls and do nothing.
    }

    @Override
    public void treeStructureChanged(TreeModelEvent e) {
      // We do not expect this event to happen in cases other than clearing the tree (including changing the filter).
      // Ignore calls and do nothing.
    }

    private boolean maybeExpand(TreePath path) {
      if (myTree == null || path == null) return false;
      Object last = path.getLastPathComponent();
      if (last instanceof DefaultMutableTreeNode) {
        DefaultMutableTreeNode mutableTreeNode = (DefaultMutableTreeNode)last;
        boolean expanded = false;
        Enumeration<?> children = mutableTreeNode.children();
        if (children.hasMoreElements()) {
          while (children.hasMoreElements()) {
            Object next = children.nextElement();
            if (next != null) {
              expanded = maybeExpand(path.pathByAddingChild(next)) || expanded;
            }
          }
          if (expanded) return true;
          Object lastUserObject = mutableTreeNode.getUserObject();
          if (lastUserObject instanceof ExecutionNodeImpl) {
            if (((ExecutionNodeImpl)lastUserObject).isAutoExpandNode()) {
              if (!myTree.isExpanded(path)) {
                myTree.expandPath(path);
                return true;
              }
            }
          }
        }
      }
      return false;
    }
  }
}
