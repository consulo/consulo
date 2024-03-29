/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.build.ui;

import consulo.build.ui.event.BuildEventsNls;
import consulo.build.ui.process.BuildProcessHandler;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.RunContentDescriptor;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.Filter;
import consulo.ui.ex.action.AnAction;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Vladislav.Soroka
 */
public class DefaultBuildDescriptor implements BuildDescriptor {

  private final Object myId;
  private final @BuildEventsNls.Title String myTitle;
  private final String myWorkingDir;
  private final long myStartTime;

  private boolean myActivateToolWindowWhenAdded;
  private boolean myActivateToolWindowWhenFailed = true;
  private boolean myAutoFocusContent = false;

  private final
  @Nonnull
  List<AnAction> myActions = new SmartList<>();
  private final
  @Nonnull
  List<AnAction> myRestartActions = new SmartList<>();
  private final
  @Nonnull
  List<Filter> myExecutionFilters = new SmartList<>();
  private final
  @Nonnull
  List<Function<? super ExecutionNode, ? extends AnAction>> myContextActions = new SmartList<>();

  private
  @Nullable
  BuildProcessHandler myProcessHandler;
  private Consumer<? super ConsoleView> myAttachedConsoleConsumer;
  private
  @Nullable
  ExecutionEnvironment myExecutionEnvironment;
  private Supplier<? extends RunContentDescriptor> myContentDescriptorSupplier;

  public DefaultBuildDescriptor(@Nonnull Object id, @Nonnull @BuildEventsNls.Title String title, @Nonnull String workingDir, long startTime) {
    myId = id;
    myTitle = title;
    myWorkingDir = workingDir;
    myStartTime = startTime;
  }

  public DefaultBuildDescriptor(@Nullable @Nonnull BuildDescriptor descriptor) {
    this(descriptor.getId(), descriptor.getTitle(), descriptor.getWorkingDir(), descriptor.getStartTime());
    if (descriptor instanceof DefaultBuildDescriptor) {
      DefaultBuildDescriptor defaultBuildDescriptor = (DefaultBuildDescriptor)descriptor;
      myActivateToolWindowWhenAdded = defaultBuildDescriptor.myActivateToolWindowWhenAdded;
      myActivateToolWindowWhenFailed = defaultBuildDescriptor.myActivateToolWindowWhenFailed;
      myAutoFocusContent = defaultBuildDescriptor.myAutoFocusContent;

      defaultBuildDescriptor.myRestartActions.forEach(this::withRestartAction);
      defaultBuildDescriptor.myActions.forEach(this::withAction);
      defaultBuildDescriptor.myExecutionFilters.forEach(this::withExecutionFilter);
      defaultBuildDescriptor.myContextActions.forEach(this::withContextAction);

      myContentDescriptorSupplier = defaultBuildDescriptor.myContentDescriptorSupplier;
      myExecutionEnvironment = defaultBuildDescriptor.myExecutionEnvironment;
      myProcessHandler = defaultBuildDescriptor.myProcessHandler;
      myAttachedConsoleConsumer = defaultBuildDescriptor.myAttachedConsoleConsumer;
    }
  }

  @Nonnull
  @Override
  public Object getId() {
    return myId;
  }

  @Nonnull
  @Override
  public String getTitle() {
    return myTitle;
  }

  @Nonnull
  @Override
  public String getWorkingDir() {
    return myWorkingDir;
  }

  @Override
  public long getStartTime() {
    return myStartTime;
  }

  @Nonnull
  public List<AnAction> getActions() {
    return Collections.unmodifiableList(myActions);
  }

  @Nonnull
  public List<AnAction> getRestartActions() {
    return Collections.unmodifiableList(myRestartActions);
  }

  @Nonnull
  public List<AnAction> getContextActions(@Nonnull ExecutionNode node) {
    return ContainerUtil.map(myContextActions, function -> function.apply(node));
  }

  @Nonnull
  public List<Filter> getExecutionFilters() {
    return Collections.unmodifiableList(myExecutionFilters);
  }

  public boolean isActivateToolWindowWhenAdded() {
    return myActivateToolWindowWhenAdded;
  }

  public void setActivateToolWindowWhenAdded(boolean activateToolWindowWhenAdded) {
    myActivateToolWindowWhenAdded = activateToolWindowWhenAdded;
  }

  public boolean isActivateToolWindowWhenFailed() {
    return myActivateToolWindowWhenFailed;
  }

  public void setActivateToolWindowWhenFailed(boolean activateToolWindowWhenFailed) {
    myActivateToolWindowWhenFailed = activateToolWindowWhenFailed;
  }

  public boolean isAutoFocusContent() {
    return myAutoFocusContent;
  }

  public void setAutoFocusContent(boolean autoFocusContent) {
    myAutoFocusContent = autoFocusContent;
  }

  @Nullable
  public BuildProcessHandler getProcessHandler() {
    return myProcessHandler;
  }

  @Nullable
  public ExecutionEnvironment getExecutionEnvironment() {
    return myExecutionEnvironment;
  }

  @Nullable
  public Supplier<? extends RunContentDescriptor> getContentDescriptorSupplier() {
    return myContentDescriptorSupplier;
  }

  public Consumer<? super ConsoleView> getAttachedConsoleConsumer() {
    return myAttachedConsoleConsumer;
  }

  public DefaultBuildDescriptor withAction(@Nonnull AnAction action) {
    myActions.add(action);
    return this;
  }

  public DefaultBuildDescriptor withActions(@Nonnull AnAction... actions) {
    myActions.addAll(Arrays.asList(actions));
    return this;
  }

  public DefaultBuildDescriptor withRestartAction(@Nonnull AnAction action) {
    myRestartActions.add(action);
    return this;
  }

  public DefaultBuildDescriptor withRestartActions(@Nonnull AnAction... actions) {
    myRestartActions.addAll(Arrays.asList(actions));
    return this;
  }

  public DefaultBuildDescriptor withContextAction(Function<? super ExecutionNode, ? extends AnAction> contextAction) {
    myContextActions.add(contextAction);
    return this;
  }

  public DefaultBuildDescriptor withContextActions(@Nonnull AnAction... actions) {
    for (AnAction action : actions) {
      myContextActions.add(node -> action);
    }
    return this;
  }

  //@ApiStatus.Experimental
  public DefaultBuildDescriptor withExecutionFilter(@Nonnull Filter filter) {
    myExecutionFilters.add(filter);
    return this;
  }

  public DefaultBuildDescriptor withContentDescriptor(Supplier<? extends RunContentDescriptor> contentDescriptorSupplier) {
    myContentDescriptorSupplier = contentDescriptorSupplier;
    return this;
  }

  public DefaultBuildDescriptor withProcessHandler(@Nullable BuildProcessHandler processHandler, @Nullable Consumer<? super ConsoleView> attachedConsoleConsumer) {
    myProcessHandler = processHandler;
    myAttachedConsoleConsumer = attachedConsoleConsumer;
    return this;
  }

  public DefaultBuildDescriptor withExecutionEnvironment(@Nullable ExecutionEnvironment env) {
    myExecutionEnvironment = env;
    return this;
  }
}
