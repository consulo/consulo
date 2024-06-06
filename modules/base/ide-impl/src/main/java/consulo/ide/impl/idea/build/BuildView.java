// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.build.ui.BuildDescriptor;
import consulo.build.ui.BuildViewSettingsProvider;
import consulo.build.ui.DefaultBuildDescriptor;
import consulo.build.ui.ViewManager;
import consulo.build.ui.event.BuildEvent;
import consulo.build.ui.event.OutputBuildEvent;
import consulo.build.ui.event.StartBuildEvent;
import consulo.build.ui.process.BuildProcessHandler;
import consulo.build.ui.progress.BuildProgressListener;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.configuration.RunProfile;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.ExecutionConsole;
import consulo.execution.ui.RunContentDescriptor;
import consulo.execution.ui.console.*;
import consulo.ide.IdeBundle;
import consulo.build.ui.impl.internal.event.StartBuildEventImpl;
import consulo.execution.impl.internal.action.StopAction;
import consulo.ide.impl.idea.execution.actions.StopProcessAction;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessEvent;
import consulo.project.Project;
import consulo.ui.ex.OccurenceNavigator;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

/**
 * @author Vladislav.Soroka
 */
public class BuildView extends CompositeView<ExecutionConsole> implements BuildProgressListener, ConsoleView, DataProvider, Filterable<ExecutionNodeImpl>, OccurenceNavigator, ObservableConsoleView {
  public static final String CONSOLE_VIEW_NAME = "consoleView";
  //@ApiStatus.Experimental
  public static final Key<List<AnAction>> RESTART_ACTIONS = Key.create("restart actions");
  private final
  @Nonnull
  Project myProject;
  private final
  @Nonnull
  ViewManager myViewManager;
  private final AtomicBoolean isBuildStartEventProcessed = new AtomicBoolean();
  private final List<BuildEvent> myAfterStartEvents = ContainerUtil.createConcurrentList();
  private final
  @Nonnull
  DefaultBuildDescriptor myBuildDescriptor;
  private volatile
  @Nullable
  ExecutionConsole myExecutionConsole;
  private volatile BuildViewSettingsProvider myViewSettingsProvider;

  public BuildView(@Nonnull Project project, @Nonnull BuildDescriptor buildDescriptor, @NonNls @Nullable String selectionStateKey, @Nonnull ViewManager viewManager) {
    this(project, null, buildDescriptor, selectionStateKey, viewManager);
  }

  public BuildView(@Nonnull Project project,
                   @Nullable ExecutionConsole executionConsole,
                   @Nonnull BuildDescriptor buildDescriptor,
                   @NonNls @Nullable String selectionStateKey,
                   @Nonnull ViewManager viewManager) {
    super(selectionStateKey);
    myProject = project;
    myViewManager = viewManager;
    myExecutionConsole = executionConsole;
    myBuildDescriptor = buildDescriptor instanceof DefaultBuildDescriptor ? (DefaultBuildDescriptor)buildDescriptor : new DefaultBuildDescriptor(buildDescriptor);
    Disposer.register(project, this);
  }

  @Override
  public void onEvent(@Nonnull Object buildId, @Nonnull BuildEvent event) {
    if (event instanceof StartBuildEvent) {
      ApplicationManager.getApplication().invokeAndWait(() -> {
        onStartBuild(buildId, (StartBuildEvent)event);
        for (BuildEvent buildEvent : myAfterStartEvents) {
          processEvent(buildId, buildEvent);
        }
        myAfterStartEvents.clear();
        isBuildStartEventProcessed.set(true);
      });
      return;
    }

    if (!isBuildStartEventProcessed.get()) {
      myAfterStartEvents.add(event);
    }
    else {
      processEvent(buildId, event);
    }
  }

  private void processEvent(@Nonnull Object buildId, @Nonnull BuildEvent event) {
    if (event instanceof OutputBuildEvent && (event.getParentId() == null || event.getParentId() == myBuildDescriptor.getId())) {
      ExecutionConsole consoleView = getConsoleView();
      if (consoleView instanceof BuildProgressListener) {
        ((BuildProgressListener)consoleView).onEvent(buildId, event);
      }
    }
    else {
      BuildTreeConsoleView eventView = getEventView();
      if (eventView != null) {
        SwingUtilities.invokeLater(() -> eventView.onEvent(buildId, event));
      }
    }
  }

  private void onStartBuild(@Nonnull Object buildId, @Nonnull StartBuildEvent startBuildEvent) {
    Application application = ApplicationManager.getApplication();
    if (application.isHeadlessEnvironment() && !application.isUnitTestMode()) return;

    if (startBuildEvent instanceof StartBuildEventImpl) {
      myViewSettingsProvider = ((StartBuildEventImpl)startBuildEvent).getBuildViewSettingsProvider();
    }
    if (myViewSettingsProvider == null) {
      myViewSettingsProvider = () -> false;
    }
    if (myExecutionConsole == null) {
      Supplier<? extends RunContentDescriptor> descriptorSupplier = myBuildDescriptor.getContentDescriptorSupplier();
      RunContentDescriptor runContentDescriptor = descriptorSupplier != null ? descriptorSupplier.get() : null;
      myExecutionConsole = runContentDescriptor != null && runContentDescriptor.getExecutionConsole() != null && runContentDescriptor.getExecutionConsole() != this
                           ? runContentDescriptor.getExecutionConsole()
                           : new BuildTextConsoleView(myProject, false, myBuildDescriptor.getExecutionFilters());
      if (runContentDescriptor != null && Disposer.findRegisteredObject(runContentDescriptor, this) == null) {
        Disposer.register(this, runContentDescriptor);
      }
    }
    ExecutionConsole executionConsole = myExecutionConsole;
    if (executionConsole != null) {
      executionConsole.getComponent(); //create editor to be able to add console editor actions
      if (myViewSettingsProvider.isExecutionViewHidden()) {
        addViewAndShowIfNeeded(executionConsole, CONSOLE_VIEW_NAME, myViewManager.isConsoleEnabledByDefault());
      }
    }

    BuildTreeConsoleView eventView = null;
    if (!myViewSettingsProvider.isExecutionViewHidden()) {
      eventView = getEventView();
      if (eventView == null) {
        String eventViewName = BuildTreeConsoleView.class.getName();
        eventView = new BuildTreeConsoleView(myProject, myBuildDescriptor, myExecutionConsole, myViewSettingsProvider);
        addView(eventView, eventViewName);
        showView(eventViewName);
      }
    }

    BuildProcessHandler processHandler = myBuildDescriptor.getProcessHandler();
    if (myExecutionConsole instanceof ConsoleView) {
      ConsoleView consoleView = (ConsoleView)myExecutionConsole;
      if (consoleView != null && !(consoleView instanceof BuildTextConsoleView)) {
        myBuildDescriptor.getExecutionFilters().forEach(consoleView::addMessageFilter);
      }

      if (processHandler != null) {
        assert consoleView != null;
        consoleView.attachToProcess(processHandler);
        java.util.function.Consumer<? super ConsoleView> attachedConsoleConsumer = myBuildDescriptor.getAttachedConsoleConsumer();
        if (attachedConsoleConsumer != null) {
          attachedConsoleConsumer.accept(consoleView);
        }
      }
    }
    if (processHandler != null && !processHandler.isStartNotified()) {
      processHandler.startNotify();
    }

    if (eventView != null) {
      eventView.onEvent(buildId, startBuildEvent);
    }
  }

  @Nullable
    //@ApiStatus.Internal
  ExecutionConsole getConsoleView() {
    return myExecutionConsole;
  }

  @Nullable
    //@ApiStatus.Internal
  BuildTreeConsoleView getEventView() {
    return getView(BuildTreeConsoleView.class.getName(), BuildTreeConsoleView.class);
  }

  @Override
  public void addChangeListener(@Nonnull ChangeListener listener, @Nonnull Disposable parent) {
    ExecutionConsole console = getConsoleView();
    if (console instanceof ObservableConsoleView) {
      ((ObservableConsoleView)console).addChangeListener(listener, parent);
    }
  }

  @Override
  public void print(@Nonnull String text, @Nonnull ConsoleViewContentType contentType) {
    delegateToConsoleView(view -> view.print(text, contentType));
  }

  private void delegateToConsoleView(Consumer<? super ConsoleView> viewConsumer) {
    ExecutionConsole console = getConsoleView();
    if (console instanceof ConsoleView) {
      viewConsumer.accept((ConsoleView)console);
    }
  }

  @Nullable
  private <R> R getConsoleViewValue(Function<? super ConsoleView, ? extends R> viewConsumer) {
    ExecutionConsole console = getConsoleView();
    if (console instanceof ConsoleView) {
      return viewConsumer.apply((ConsoleView)console);
    }
    return null;
  }

  @Override
  public void clear() {
    delegateToConsoleView(ConsoleView::clear);
  }

  @Override
  public void scrollTo(int offset) {
    delegateToConsoleView(view -> view.scrollTo(offset));
  }

  @Override
  public void attachToProcess(@Nonnull ProcessHandler processHandler) {
    delegateToConsoleView(view -> view.attachToProcess(processHandler));
  }

  @Override
  public boolean isOutputPaused() {
    Boolean result = getConsoleViewValue(ConsoleView::isOutputPaused);
    return result != null && result;
  }

  @Override
  public void setOutputPaused(boolean value) {
    delegateToConsoleView(view -> view.setOutputPaused(value));
  }

  @Override
  public void setProcessTextFilter(@Nullable BiPredicate<ProcessEvent, Key> filter) {
    delegateToConsoleView(view -> view.setProcessTextFilter(filter));
  }

  @Nullable
  @Override
  public BiPredicate<ProcessEvent, Key> getProcessTextFilter() {
    return getConsoleViewValue(ConsoleView::getProcessTextFilter);
  }

  @Override
  public boolean hasDeferredOutput() {
    Boolean result = getConsoleViewValue(ConsoleView::hasDeferredOutput);
    return result != null && result;
  }

  @Override
  public void performWhenNoDeferredOutput(@Nonnull Runnable runnable) {
    delegateToConsoleView(view -> view.performWhenNoDeferredOutput(runnable));
  }

  @Override
  public void setHelpId(@Nonnull String helpId) {
    delegateToConsoleView(view -> view.setHelpId(helpId));
  }

  @Override
  public void addMessageFilter(@Nonnull Filter filter) {
    delegateToConsoleView(view -> view.addMessageFilter(filter));
  }

  @Override
  public void printHyperlink(@Nonnull String hyperlinkText, @Nullable HyperlinkInfo info) {
    delegateToConsoleView(view -> view.printHyperlink(hyperlinkText, info));
  }

  @Override
  public int getContentSize() {
    Integer result = getConsoleViewValue(ConsoleView::getContentSize);
    return result == null ? 0 : result;
  }

  @Override
  public boolean canPause() {
    Boolean result = getConsoleViewValue(ConsoleView::canPause);
    return result != null && result;
  }

  @Override
  @Nonnull
  public AnAction[] createConsoleActions() {
    if (!myViewManager.isBuildContentView()) {
      // console actions should be integrated with the provided toolbar when the console is shown not on Build tw
      return AnAction.EMPTY_ARRAY;
    }
    final DefaultActionGroup rerunActionGroup = new DefaultActionGroup();
    AnAction stopAction = null;
    if (myBuildDescriptor.getProcessHandler() != null) {
      stopAction = new StopProcessAction(IdeBundle.message("action.DumbAware.BuildView.text.stop"), IdeBundle.message("action.DumbAware.CopyrightProfilesPanel.description.stop"),
                                         myBuildDescriptor.getProcessHandler());
      ActionUtil.copyFrom(stopAction, IdeActions.ACTION_STOP_PROGRAM);
      stopAction.registerCustomShortcutSet(stopAction.getShortcutSet(), this);
    }

    ExecutionConsole consoleView = getConsoleView();
    if (consoleView instanceof ConsoleView) {
      consoleView.getComponent(); //create editor to be able to add console editor actions
      if (stopAction == null) {
        final AnAction[] consoleActions = ((ConsoleView)consoleView).createConsoleActions();
        stopAction = ContainerUtil.find(consoleActions, StopAction.class::isInstance);
      }
    }
    final DefaultActionGroup actionGroup = new DefaultActionGroup();
    for (AnAction anAction : myBuildDescriptor.getRestartActions()) {
      rerunActionGroup.add(anAction);
    }

    if (stopAction != null) {
      rerunActionGroup.add(stopAction);
    }
    actionGroup.add(rerunActionGroup);
    final DefaultActionGroup otherActionGroup = new DefaultActionGroup();

    List<AnAction> otherActions = myBuildDescriptor.getActions();
    if (!otherActions.isEmpty()) {
      otherActionGroup.addSeparator();
      for (AnAction anAction : otherActions) {
        otherActionGroup.add(anAction);
      }
      otherActionGroup.addSeparator();
    }
    return new AnAction[]{actionGroup, otherActionGroup};
  }

  @Override
  public void allowHeavyFilters() {
    delegateToConsoleView(ConsoleView::allowHeavyFilters);
  }

  @Nullable
  @Override
  public Object getData(@Nonnull Key dataId) {
    if (KEY == dataId) {
      return getConsoleView();
    }
    Object data = super.getData(dataId);
    if (data != null) return data;
    if (RunProfile.KEY == dataId) {
      ExecutionEnvironment environment = myBuildDescriptor.getExecutionEnvironment();
      return environment == null ? null : environment.getRunProfile();
    }
    if (ExecutionEnvironment.KEY == dataId) {
      return myBuildDescriptor.getExecutionEnvironment();
    }
    if (RESTART_ACTIONS == dataId) {
      return myBuildDescriptor.getRestartActions();
    }
    return null;
  }

  @Override
  public boolean isFilteringEnabled() {
    return getEventView() != null;
  }

  @Nonnull
  @Override
  public Predicate<ExecutionNodeImpl> getFilter() {
    BuildTreeConsoleView eventView = getEventView();
    return eventView == null ? executionNode -> true : eventView.getFilter();
  }

  @Override
  public void addFilter(@Nonnull Predicate<? super ExecutionNodeImpl> filter) {
    BuildTreeConsoleView eventView = getEventView();
    if (eventView != null) {
      eventView.addFilter(filter);
    }
  }

  @Override
  public void removeFilter(@Nonnull Predicate<? super ExecutionNodeImpl> filter) {
    BuildTreeConsoleView eventView = getEventView();
    if (eventView != null) {
      eventView.removeFilter(filter);
    }
  }

  @Override
  public boolean contains(@Nonnull Predicate<? super ExecutionNodeImpl> filter) {
    BuildTreeConsoleView eventView = getEventView();
    return eventView != null && eventView.contains(filter);
  }

  @Nonnull
  private OccurenceNavigator getOccurenceNavigator() {
    BuildTreeConsoleView eventView = getEventView();
    if (eventView != null) return eventView;
    ExecutionConsole executionConsole = getConsoleView();
    if (executionConsole instanceof OccurenceNavigator) {
      return (OccurenceNavigator)executionConsole;
    }
    return EMPTY;
  }

  @Override
  public boolean hasNextOccurence() {
    return getOccurenceNavigator().hasNextOccurence();
  }

  @Override
  public boolean hasPreviousOccurence() {
    return getOccurenceNavigator().hasPreviousOccurence();
  }

  @Override
  public OccurenceInfo goNextOccurence() {
    return getOccurenceNavigator().goNextOccurence();
  }

  @Override
  public OccurenceInfo goPreviousOccurence() {
    return getOccurenceNavigator().goPreviousOccurence();
  }

  @Nonnull
  @Override
  public String getNextOccurenceActionName() {
    return getOccurenceNavigator().getNextOccurenceActionName();
  }

  @Nonnull
  @Override
  public String getPreviousOccurenceActionName() {
    return getOccurenceNavigator().getPreviousOccurenceActionName();
  }
}
