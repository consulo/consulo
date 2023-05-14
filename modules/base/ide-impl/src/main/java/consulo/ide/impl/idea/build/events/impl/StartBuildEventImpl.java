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
package consulo.ide.impl.idea.build.events.impl;

import consulo.build.ui.BuildDescriptor;
import consulo.build.ui.BuildViewSettingsProvider;
import consulo.build.ui.DefaultBuildDescriptor;
import consulo.build.ui.event.BuildEventsNls;
import consulo.build.ui.event.StartBuildEvent;
import consulo.build.ui.process.BuildProcessHandler;
import consulo.execution.ui.console.Filter;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.RunContentDescriptor;
import consulo.ui.ex.action.AnAction;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Vladislav.Soroka
 */
public class StartBuildEventImpl extends StartEventImpl implements StartBuildEvent {

  private final @Nonnull
  DefaultBuildDescriptor myBuildDescriptor;
  private @Nullable
  BuildViewSettingsProvider myBuildViewSettingsProvider;

  public StartBuildEventImpl(@Nonnull BuildDescriptor descriptor, @Nonnull @BuildEventsNls.Message  String message) {
    super(descriptor.getId(), null, descriptor.getStartTime(), message);
    myBuildDescriptor =
      descriptor instanceof DefaultBuildDescriptor ? (DefaultBuildDescriptor)descriptor : new DefaultBuildDescriptor(descriptor);
  }

  //@ApiStatus.Experimental
  @Nonnull
  @Override
  public DefaultBuildDescriptor getBuildDescriptor() {
    return myBuildDescriptor;
  }

  /**
   * @deprecated use {@link DefaultBuildDescriptor#withProcessHandler}
   */
  @Deprecated
  public StartBuildEventImpl withProcessHandler(@Nullable BuildProcessHandler processHandler,
                                                @Nullable Consumer<? super ConsoleView> attachedConsoleConsumer) {
    myBuildDescriptor.withProcessHandler(processHandler, attachedConsoleConsumer);
    return this;
  }

  /**
   * @deprecated use {@link DefaultBuildDescriptor#withProcessHandler}
   */
  @Deprecated
  public StartBuildEventImpl withRestartAction(@Nonnull AnAction anAction) {
    myBuildDescriptor.withRestartAction(anAction);
    return this;
  }

  /**
   * @deprecated use {@link DefaultBuildDescriptor#withProcessHandler}
   */
  @Deprecated
  public StartBuildEventImpl withRestartActions(AnAction... actions) {
    Arrays.stream(actions).forEach(myBuildDescriptor::withRestartAction);
    return this;
  }

  /**
   * @deprecated use {@link DefaultBuildDescriptor#withProcessHandler}
   */
  @Deprecated
  public StartBuildEventImpl withContentDescriptorSupplier(Supplier<? extends RunContentDescriptor> contentDescriptorSupplier) {
    myBuildDescriptor.withContentDescriptor(contentDescriptorSupplier);
    return this;
  }

  /**
   * @deprecated use {@link DefaultBuildDescriptor#withProcessHandler}
   */
  @Deprecated
  public StartBuildEventImpl withExecutionFilter(@Nonnull Filter filter) {
    myBuildDescriptor.withExecutionFilter(filter);
    return this;
  }

  /**
   * @deprecated use {@link DefaultBuildDescriptor#withProcessHandler}
   */
  @Deprecated
  public StartBuildEventImpl withExecutionFilters(Filter... filters) {
    Arrays.stream(filters).forEach(myBuildDescriptor::withExecutionFilter);
    return this;
  }

  @Nullable
  //@ApiStatus.Experimental
  public BuildViewSettingsProvider getBuildViewSettingsProvider() {
    return myBuildViewSettingsProvider;
  }

  //@ApiStatus.Experimental
  public StartBuildEventImpl withBuildViewSettingsProvider(@Nullable BuildViewSettingsProvider viewSettingsProvider) {
    myBuildViewSettingsProvider = viewSettingsProvider;
    return this;
  }
}
