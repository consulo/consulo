// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build;

import consulo.build.ui.BuildContentManager;
import consulo.build.ui.BuildDescriptor;
import consulo.build.ui.event.*;
import consulo.build.ui.progress.BuildProgressListener;
import consulo.execution.process.AnsiEscapeDecoder;
import consulo.process.ProcessOutputTypes;
import consulo.execution.ui.RunContentDescriptor;
import consulo.ide.IdeBundle;
import consulo.ui.ex.OccurenceNavigator;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ide.impl.idea.openapi.diagnostic.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.OnePixelDivider;
import consulo.util.lang.Pair;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.awt.OnePixelSplitter;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.awt.util.Alarm;
import consulo.util.collection.SmartList;
import consulo.ui.ex.concurrent.EdtExecutorService;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * @author Vladislav.Soroka
 */
//@ApiStatus.Experimental
public class MultipleBuildsView implements BuildProgressListener, Disposable {
  private static final Logger LOG = Logger.getInstance(MultipleBuildsView.class);
  @NonNls
  private static final String SPLITTER_PROPERTY = "MultipleBuildsView.Splitter.Proportion";

  protected final Project myProject;
  protected final BuildContentManager myBuildContentManager;
  private final AtomicBoolean isInitializeStarted;
  private final AtomicBoolean isFirstErrorShown = new AtomicBoolean();
  private final List<Runnable> myPostponedRunnables;
  private final ProgressWatcher myProgressWatcher;
  private final OnePixelSplitter myThreeComponentsSplitter;
  private final JBList<AbstractViewManager.BuildInfo> myBuildsList;
  private final Map<Object, AbstractViewManager.BuildInfo> myBuildsMap;
  private final Map<AbstractViewManager.BuildInfo, BuildView> myViewMap;
  private final AbstractViewManager myViewManager;
  private volatile Content myContent;
  private volatile DefaultActionGroup myToolbarActions;
  private volatile boolean myDisposed;

  public MultipleBuildsView(Project project, BuildContentManager buildContentManager, AbstractViewManager viewManager) {
    myProject = project;
    myBuildContentManager = buildContentManager;
    myViewManager = viewManager;
    isInitializeStarted = new AtomicBoolean();
    myPostponedRunnables = ContainerUtil.createConcurrentList();
    myThreeComponentsSplitter = new OnePixelSplitter(SPLITTER_PROPERTY, 0.25f);
    myBuildsList = new JBList<>();
    myBuildsList.setModel(new DefaultListModel<>());
    myBuildsList.setFixedCellHeight(UIUtil.LIST_FIXED_CELL_HEIGHT * 2);
    AnsiEscapeDecoder ansiEscapeDecoder = new AnsiEscapeDecoder();
    myBuildsList.installCellRenderer(obj -> {
      JPanel panel = new JPanel(new BorderLayout());
      SimpleColoredComponent mainComponent = new SimpleColoredComponent();
      mainComponent.setIcon(obj.getIcon());
      mainComponent.append(obj.getTitle() + ": ", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
      mainComponent.append(obj.message, SimpleTextAttributes.REGULAR_ATTRIBUTES);
      panel.add(mainComponent, BorderLayout.NORTH);
      if (obj.statusMessage != null) {
        SimpleColoredComponent statusComponent = new SimpleColoredComponent();
        statusComponent.setIcon(Image.empty(Image.DEFAULT_ICON_SIZE));
        ansiEscapeDecoder.escapeText(obj.statusMessage, ProcessOutputTypes.STDOUT, (text, attributes) -> {
          statusComponent.append(text, SimpleTextAttributes.REGULAR_ATTRIBUTES); //NON-NLS
        });
        panel.add(statusComponent, BorderLayout.SOUTH);
      }
      return panel;
    });
    myViewMap = new ConcurrentHashMap<>();
    myBuildsMap = new ConcurrentHashMap<>();
    myProgressWatcher = new ProgressWatcher();
  }

  @Override
  public void dispose() {
    myDisposed = true;
  }

  public Content getContent() {
    return myContent;
  }

  public Map<BuildDescriptor, BuildView> getBuildsMap() {
    return Collections.unmodifiableMap(myViewMap);
  }

  public boolean shouldConsume(@Nonnull Object buildId) {
    return myBuildsMap.containsKey(buildId);
  }

  @Override
  public void onEvent(@Nonnull Object buildId, @Nonnull BuildEvent event) {
    List<Runnable> runOnEdt = new SmartList<>();
    AbstractViewManager.BuildInfo buildInfo;
    if (event instanceof StartBuildEvent) {
      StartBuildEvent startBuildEvent = (StartBuildEvent)event;
      if (isInitializeStarted.get()) {
        clearOldBuilds(runOnEdt, startBuildEvent);
      }
      buildInfo = new AbstractViewManager.BuildInfo(((StartBuildEvent)event).getBuildDescriptor());
      myBuildsMap.put(buildId, buildInfo);
    }
    else {
      buildInfo = myBuildsMap.get(buildId);
    }
    if (buildInfo == null) {
      LOG.warn("Build can not be found for buildId: '" + buildId + "'");
      return;
    }

    runOnEdt.add(() -> {
      if (event instanceof StartBuildEvent) {
        buildInfo.message = event.getMessage();

        DefaultListModel<AbstractViewManager.BuildInfo> listModel = (DefaultListModel<AbstractViewManager.BuildInfo>)myBuildsList.getModel();
        listModel.addElement(buildInfo);

        RunContentDescriptor contentDescriptor;
        Supplier<? extends RunContentDescriptor> contentDescriptorSupplier = buildInfo.getContentDescriptorSupplier();
        contentDescriptor = contentDescriptorSupplier != null ? contentDescriptorSupplier.get() : null;
        final Runnable activationCallback;
        if (contentDescriptor != null) {
          buildInfo.setActivateToolWindowWhenAdded(contentDescriptor.isActivateToolWindowWhenAdded());
          if (contentDescriptor instanceof BuildContentDescriptor) {
            buildInfo.setActivateToolWindowWhenFailed(((BuildContentDescriptor)contentDescriptor).isActivateToolWindowWhenFailed());
          }
          buildInfo.setAutoFocusContent(contentDescriptor.isAutoFocusContent());
          activationCallback = contentDescriptor.getActivationCallback();
        }
        else {
          activationCallback = null;
        }

        BuildView view = myViewMap.computeIfAbsent(buildInfo, info -> {
          String selectionStateKey = "build.toolwindow." + myViewManager.getViewName() + ".selection.state";
          BuildView buildView = new BuildView(myProject, buildInfo, selectionStateKey, myViewManager);
          Disposer.register(this, buildView);
          if (contentDescriptor != null) {
            Disposer.register(buildView, contentDescriptor);
          }
          return buildView;
        });
        view.onEvent(buildId, event);

        myContent.setPreferredFocusedComponent(view::getPreferredFocusableComponent);

        myBuildContentManager.setSelectedContent(myContent, buildInfo.isAutoFocusContent(), buildInfo.isAutoFocusContent(), buildInfo.isActivateToolWindowWhenAdded(), activationCallback);
        buildInfo.content = myContent;

        if (myThreeComponentsSplitter.getSecondComponent() == null) {
          myThreeComponentsSplitter.setSecondComponent(view);
          myViewManager.configureToolbar(myToolbarActions, this, view);
        }
        if (myBuildsList.getModel().getSize() > 1) {
          myThreeComponentsSplitter.setFirstComponent(ScrollPaneFactory.createScrollPane(myBuildsList, true));
          myBuildsList.setVisible(true);
          myBuildsList.setSelectedIndex(0);

          for (BuildView consoleView : myViewMap.values()) {
            BuildTreeConsoleView buildConsoleView = consoleView.getView(BuildTreeConsoleView.class.getName(), BuildTreeConsoleView.class);
            if (buildConsoleView != null) {
              buildConsoleView.hideRootNode();
            }
          }
        }
        else {
          myThreeComponentsSplitter.setFirstComponent(null);
        }
        myViewManager.onBuildStart(buildInfo);
        myProgressWatcher.addBuild(buildInfo);
        ((BuildContentManagerImpl)myBuildContentManager).startBuildNotified(buildInfo, buildInfo.content, buildInfo.getProcessHandler());
      }
      else {
        if (!isFirstErrorShown.get() && (event instanceof FinishEvent && ((FinishEvent)event).getResult() instanceof FailureResult) ||
            (event instanceof MessageEvent && ((MessageEvent)event).getResult().getKind() == MessageEvent.Kind.ERROR)) {
          if (isFirstErrorShown.compareAndSet(false, true)) {
            ListModel<AbstractViewManager.BuildInfo> listModel = myBuildsList.getModel();
            IntStream.range(0, listModel.getSize()).filter(i -> buildInfo == listModel.getElementAt(i)).findFirst().ifPresent(myBuildsList::setSelectedIndex);
          }
        }
        BuildView view = myViewMap.get(buildInfo);
        if (view != null) {
          view.onEvent(buildId, event);
        }
        if (event instanceof FinishBuildEvent) {
          buildInfo.endTime = event.getEventTime();
          buildInfo.message = event.getMessage();
          buildInfo.result = ((FinishBuildEvent)event).getResult();
          myProgressWatcher.stopBuild(buildInfo);
          ((BuildContentManagerImpl)myBuildContentManager).finishBuildNotified(buildInfo, buildInfo.content);
          myViewManager.onBuildFinish(buildInfo);
        }
        else {
          buildInfo.statusMessage = event.getMessage();
        }

      }
    });

    if (myContent == null) {
      myPostponedRunnables.addAll(runOnEdt);
      if (isInitializeStarted.compareAndSet(false, true)) {
        EdtExecutorService.getInstance().execute(() -> {
          if (myDisposed) return;
          myBuildsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
          myBuildsList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
              AbstractViewManager.BuildInfo selectedBuild = myBuildsList.getSelectedValue();
              if (selectedBuild == null) return;

              BuildView view = myViewMap.get(selectedBuild);
              JComponent lastComponent = myThreeComponentsSplitter.getSecondComponent();
              if (view != null && lastComponent != view.getComponent()) {
                myThreeComponentsSplitter.setSecondComponent(view.getComponent());
                view.getComponent().setVisible(true);
                if (lastComponent != null) {
                  lastComponent.setVisible(false);
                }
                myViewManager.configureToolbar(myToolbarActions, MultipleBuildsView.this, view);
                view.getComponent().repaint();
              }
            }
          });

          final JComponent consoleComponent = new MultipleBuildsPanel();
          consoleComponent.add(myThreeComponentsSplitter, BorderLayout.CENTER);
          myToolbarActions = new DefaultActionGroup();
          ActionToolbar tb = ActionManager.getInstance().createActionToolbar("BuildView", myToolbarActions, false);
          tb.setTargetComponent(consoleComponent);
          tb.getComponent().setBorder(JBUI.Borders.merge(tb.getComponent().getBorder(), JBUI.Borders.customLine(OnePixelDivider.BACKGROUND, 0, 0, 0, 1), true));
          consoleComponent.add(tb.getComponent(), BorderLayout.WEST);

          myContent = ContentFactory.getInstance().createContent(consoleComponent, myViewManager.getViewName(), true);
          Disposer.register(myContent, new Disposable() {
            @Override
            public void dispose() {
              Disposer.dispose(MultipleBuildsView.this);
            }
          });
          Disposer.register(myContent, new Disposable() {
            @Override
            public void dispose() {
              myViewManager.onBuildsViewRemove(MultipleBuildsView.this);
            }
          });
          Image contentIcon = myViewManager.getContentIcon();
          if (contentIcon != null) {
            myContent.setIcon(contentIcon);
            myContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
          }
          myBuildContentManager.addContent(myContent);

          List<Runnable> postponedRunnables = new ArrayList<>(myPostponedRunnables);
          myPostponedRunnables.clear();
          for (Runnable postponedRunnable : postponedRunnables) {
            postponedRunnable.run();
          }
        });
      }
    }
    else {
      EdtExecutorService.getInstance().execute(() -> {
        if (myDisposed) return;
        for (Runnable runnable : runOnEdt) {
          runnable.run();
        }
      });
    }
  }

  private void clearOldBuilds(List<Runnable> runOnEdt, StartBuildEvent startBuildEvent) {
    long currentTime = System.currentTimeMillis();
    DefaultListModel<AbstractViewManager.BuildInfo> listModel = (DefaultListModel<AbstractViewManager.BuildInfo>)myBuildsList.getModel();
    boolean clearAll = !listModel.isEmpty();
    List<AbstractViewManager.BuildInfo> sameBuildsToClear = new SmartList<>();
    for (int i = 0; i < listModel.getSize(); i++) {
      AbstractViewManager.BuildInfo build = listModel.getElementAt(i);
      boolean sameBuild = build.getWorkingDir().equals(startBuildEvent.getBuildDescriptor().getWorkingDir());
      if (!build.isRunning() && sameBuild) {
        sameBuildsToClear.add(build);
      }
      boolean buildFinishedRecently = currentTime - build.endTime < TimeUnit.SECONDS.toMillis(1);
      if (build.isRunning() || !sameBuild && buildFinishedRecently) {
        clearAll = false;
      }
    }
    if (clearAll) {
      myBuildsMap.clear();
      SmartList<BuildView> viewsToDispose = new SmartList<>(myViewMap.values());
      runOnEdt.add(() -> viewsToDispose.forEach(Disposer::dispose));

      myViewMap.clear();
      listModel.clear();
      runOnEdt.add(() -> {
        myBuildsList.setVisible(false);
        myThreeComponentsSplitter.setFirstComponent(null);
        myThreeComponentsSplitter.setSecondComponent(null);
      });
      myToolbarActions.removeAll();
      isFirstErrorShown.set(false);
    }
    else {
      sameBuildsToClear.forEach(info -> {
        BuildView buildView = myViewMap.remove(info);
        if (buildView != null) {
          runOnEdt.add(() -> Disposer.dispose(buildView));
        }
        listModel.removeElement(info);
      });
    }
  }

  //@ApiStatus.Internal
  public BuildView getBuildView(Object buildId) {
    AbstractViewManager.BuildInfo buildInfo = myBuildsMap.get(buildId);
    if (buildInfo == null) return null;
    return myViewMap.get(buildInfo);
  }

  private class MultipleBuildsPanel extends JPanel implements OccurenceNavigator {
    MultipleBuildsPanel() {
      super(new BorderLayout());
    }

    @Override
    public boolean hasNextOccurence() {
      return getOccurenceNavigator(true) != null;
    }

    private
    @Nullable
    Pair<Integer, Supplier<OccurenceInfo>> getOccurenceNavigator(boolean next) {
      if (myBuildsList.getItemsCount() == 0) return null;
      int index = Math.max(myBuildsList.getSelectedIndex(), 0);

      Function<Integer, Pair<Integer, Supplier<OccurenceInfo>>> function = i -> {
        AbstractViewManager.BuildInfo buildInfo = myBuildsList.getModel().getElementAt(i);
        BuildView buildView = myViewMap.get(buildInfo);
        if (buildView == null) return null;
        if (i != index) {
          BuildTreeConsoleView eventView = buildView.getEventView();
          if (eventView == null) return null;
          eventView.getTree().clearSelection();
        }
        if (next) {
          if (buildView.hasNextOccurence()) return Pair.create(i, buildView::goNextOccurence);
        }
        else {
          if (buildView.hasPreviousOccurence()) {
            return Pair.create(i, buildView::goPreviousOccurence);
          }
          else if (i != index && buildView.hasNextOccurence()) {
            return Pair.create(i, buildView::goNextOccurence);
          }
        }
        return null;
      };
      if (next) {
        for (int i = index; i < myBuildsList.getItemsCount(); i++) {
          Pair<Integer, Supplier<OccurenceInfo>> buildViewPair = function.apply(i);
          if (buildViewPair != null) return buildViewPair;
        }
      }
      else {
        for (int i = index; i >= 0; i--) {
          Pair<Integer, Supplier<OccurenceInfo>> buildViewPair = function.apply(i);
          if (buildViewPair != null) return buildViewPair;
        }
      }
      return null;
    }

    @Override
    public boolean hasPreviousOccurence() {
      return getOccurenceNavigator(false) != null;
    }

    @Override
    public OccurenceInfo goNextOccurence() {
      Pair<Integer, Supplier<OccurenceInfo>> navigator = getOccurenceNavigator(true);
      if (navigator != null) {
        myBuildsList.setSelectedIndex(navigator.first);
        return navigator.second.get();
      }
      return null;
    }

    @Override
    public OccurenceInfo goPreviousOccurence() {
      Pair<Integer, Supplier<OccurenceInfo>> navigator = getOccurenceNavigator(false);
      if (navigator != null) {
        myBuildsList.setSelectedIndex(navigator.first);
        return navigator.second.get();
      }
      return null;
    }

    @Override
    @Nonnull
    public String getNextOccurenceActionName() {
      return IdeBundle.message("action.next.problem");
    }

    @Override
    @Nonnull
    public String getPreviousOccurenceActionName() {
      return IdeBundle.message("action.previous.problem");
    }
  }

  private class ProgressWatcher implements Runnable {

    private final Alarm myRefreshAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
    private final Set<AbstractViewManager.BuildInfo> myBuilds = ContainerUtil.newConcurrentSet();

    @Override
    public void run() {
      myRefreshAlarm.cancelAllRequests();
      JComponent firstComponent = myThreeComponentsSplitter.getFirstComponent();
      if (firstComponent != null) {
        firstComponent.revalidate();
        firstComponent.repaint();
      }
      if (!myBuilds.isEmpty()) {
        myRefreshAlarm.addRequest(this, 300);
      }
    }

    void addBuild(AbstractViewManager.BuildInfo buildInfo) {
      myBuilds.add(buildInfo);
      if (myBuilds.size() > 1) {
        myRefreshAlarm.cancelAllRequests();
        myRefreshAlarm.addRequest(this, 300);
      }
    }

    void stopBuild(AbstractViewManager.BuildInfo buildInfo) {
      myBuilds.remove(buildInfo);
    }
  }
}
