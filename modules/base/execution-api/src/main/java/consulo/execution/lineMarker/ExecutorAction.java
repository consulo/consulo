/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.lineMarker;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.internal.ExecutionActionValue;
import consulo.execution.internal.action.BaseRunConfigurationAction;
import consulo.execution.action.ConfigurationContext;
import consulo.execution.action.ConfigurationFromContext;
import consulo.execution.action.RunConfigurationProducer;
import consulo.execution.configuration.LocatableConfiguration;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorRegistry;
import consulo.execution.internal.ConfigurationFromContextImpl;
import consulo.execution.internal.RunManagerEx;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * @author Dmitry Avdeev
 */
public class ExecutorAction extends AnAction {
  private static final Key<List<ConfigurationFromContext>> CONFIGURATION_CACHE = Key.create("ConfigurationFromContext");

  @Nonnull
  public static AnAction[] getActions(final int order) {
    return ContainerUtil.map2Array(ExecutorRegistry.getInstance().getRegisteredExecutors(), AnAction.class,
                                   (Function<Executor, AnAction>)executor -> new ExecutorAction(ActionManager.getInstance().getAction(executor.getContextActionId()), executor, order));
  }

  private final AnAction myOrigin;
  private final Executor myExecutor;
  private final int myOrder;

  private ExecutorAction(@Nonnull AnAction origin, @Nonnull Executor executor, int order) {
    myOrigin = origin;
    myExecutor = executor;
    myOrder = order;
    copyFrom(origin);
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    LocalizeValue activeText = getActionText(e.getDataContext(), myExecutor);
    e.getPresentation().setVisible(activeText != LocalizeValue.of());
    e.getPresentation().setTextValue(activeText);
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    myOrigin.actionPerformed(e);
  }

  @Nonnull
  private static List<ConfigurationFromContext> getConfigurations(DataContext dataContext) {
    List<ConfigurationFromContext> result = DataManager.getInstance().loadFromDataContext(dataContext, CONFIGURATION_CACHE);
    if (result == null) {
      DataManager.getInstance().saveInDataContext(dataContext, CONFIGURATION_CACHE, result = calcConfigurations(dataContext));
    }
    return result;
  }

  @Nonnull
  private static List<ConfigurationFromContext> calcConfigurations(DataContext dataContext) {
    final ConfigurationContext context = ConfigurationContext.getFromContext(dataContext);
    if (context.getLocation() == null) return Collections.emptyList();
    return context.getProject().getApplication().getExtensionPoint(RunConfigurationProducer.class)
        .collectMapped(producer -> createConfiguration(producer, context));
  }

  @Nonnull
  private LocalizeValue getActionText(DataContext dataContext, @Nonnull Executor executor) {
    List<ConfigurationFromContext> list = getConfigurations(dataContext);
    if (list.isEmpty()) return LocalizeValue.of();
    ConfigurationFromContext configuration = list.get(myOrder < list.size() ? myOrder : 0);
    String actionName = BaseRunConfigurationAction.suggestRunActionName((LocatableConfiguration)configuration.getConfiguration());
    return ExecutionActionValue.buildWithConfiguration(executor::getStartActiveText, actionName);
  }

  @Nullable
  private static ConfigurationFromContext createConfiguration(RunConfigurationProducer<?> producer, ConfigurationContext context) {
    RunConfiguration configuration = producer.createLightConfiguration(context);
    if (configuration == null) return null;
    RunManagerEx runManager = (RunManagerEx)RunManager.getInstance(context.getProject());
    RunnerAndConfigurationSettings settings = runManager.createConfiguration(configuration, false);
    return new ConfigurationFromContextImpl(producer, settings, context.getPsiLocation());
  }
}
