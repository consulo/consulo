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

import consulo.application.Application;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.action.ConfigurationContext;
import consulo.execution.action.ConfigurationFromContext;
import consulo.execution.action.RunConfigurationProducer;
import consulo.execution.configuration.LocatableConfiguration;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.executor.Executor;
import consulo.execution.internal.ConfigurationFromContextImpl;
import consulo.execution.internal.ExecutionActionValue;
import consulo.execution.internal.RunManagerEx;
import consulo.execution.internal.action.BaseRunConfigurationAction;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionWithDelegate;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.AsyncActionGroup;
import consulo.ui.ex.internal.ActionUpdateInvoker;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class ExecutorAction extends ActionGroup implements ActionWithDelegate<AnAction>, AnActionWithSyncUpdate {
    private static final Key<List<ConfigurationFromContext>> CONFIGURATION_CACHE = Key.create("ConfigurationFromContext");

    public static AnAction[] getActions() {
        return getActions(0);
    }

    public static AnAction[] getActions(int order) {
        return getActionList(order).toArray(AnAction.EMPTY_ARRAY);
    }

    public static List<AnAction> getActionList() {
        return getActionList(0);
    }

    public static List<AnAction> getActionList(int order) {
        ActionManager actionManager = ActionManager.getInstance();
        List<AnAction> result = new ArrayList<>();
        Application.get().getExtensionPoint(Executor.class).forEachExtensionSafe(executor -> {
            AnAction action = actionManager.getAction(executor.getContextActionId());
            if (action != null) {
                result.add(new ExecutorAction(action, executor, order));
            }
        });
        return result;
    }

    public static AnAction wrap(AnAction runContextAction, Executor executor, int order) {
        return new ExecutorAction(runContextAction, executor, order);
    }

    private final AnAction myOrigin;
    private final Executor myExecutor;
    private final int myOrder;

    private ExecutorAction(AnAction origin, Executor executor, int order) {
        myOrigin = origin;
        myExecutor = executor;
        myOrder = order;
        copyFrom(origin);
        if (!isGroupOrigin()) {
            getTemplatePresentation().setPerformGroup(true);
            getTemplatePresentation().setPopupGroup(true);
        }
    }

    public AnAction getOrigin() {
        return myOrigin;
    }

    public Executor getExecutor() {
        return myExecutor;
    }

    public int getOrder() {
        return myOrder;
    }

    @Override
    public AnAction getDelegate() {
        return myOrigin;
    }

    @Override
    public boolean isDumbAware() {
        return myOrigin.isDumbAware();
    }

    @Override
    public void update(AnActionEvent e) {
        if (isGroupOrigin()) {
            ActionUpdateInvoker.updateSync(myOrigin, e);
            return;
        }

        LocalizeValue activeText = getActionText(e.getDataContext(), myExecutor);
        e.getPresentation().setVisible(activeText.isNotEmpty());
        e.getPresentation().setText(activeText);
    }

    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        if (isGroupOrigin() && !(myOrigin instanceof AsyncActionGroup)) {
            return ((ActionGroup) myOrigin).getChildren(e);
        }
        return EMPTY_ARRAY;
    }

    @Override
    public Coroutine<?, List<AnAction>> getChildrenAsync(@Nullable AnActionEvent e) {
        if (isGroupOrigin()) {
            return ((ActionGroup) myOrigin).getChildrenAsync(e);
        }
        return super.getChildrenAsync(e);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        myOrigin.actionPerformed(e);
    }

    private boolean isGroupOrigin() {
        return myOrigin instanceof ActionGroup && !(myOrigin instanceof BaseRunConfigurationAction);
    }

    private static List<ConfigurationFromContext> getConfigurations(DataContext dataContext) {
        List<ConfigurationFromContext> result = DataManager.getInstance().loadFromDataContext(dataContext, CONFIGURATION_CACHE);
        if (result == null) {
            DataManager.getInstance().saveInDataContext(dataContext, CONFIGURATION_CACHE, result = calcConfigurations(dataContext));
        }
        return result;
    }

    private static List<ConfigurationFromContext> calcConfigurations(DataContext dataContext) {
        ConfigurationContext context = ConfigurationContext.getFromContext(dataContext);
        if (context.getLocation() == null) {
            return Collections.emptyList();
        }
        return context.getProject().getApplication().getExtensionPoint(RunConfigurationProducer.class)
            .collectMapped(producer -> createConfiguration(producer, context));
    }

    private LocalizeValue getActionText(DataContext dataContext, Executor executor) {
        List<ConfigurationFromContext> list = getConfigurations(dataContext);
        if (list.isEmpty()) {
            return LocalizeValue.empty();
        }
        ConfigurationFromContext configuration = list.get(myOrder < list.size() ? myOrder : 0);
        String actionName = BaseRunConfigurationAction.suggestRunActionName((LocatableConfiguration) configuration.getConfiguration());
        return ExecutionActionValue.buildWithConfiguration(executor::getStartActiveText, actionName);
    }

    private static @Nullable ConfigurationFromContext createConfiguration(
        RunConfigurationProducer<?> producer,
        ConfigurationContext context
    ) {
        RunConfiguration configuration = producer.createLightConfiguration(context);
        if (configuration == null) {
            return null;
        }
        RunManagerEx runManager = (RunManagerEx) RunManager.getInstance(context.getProject());
        RunnerAndConfigurationSettings settings = runManager.createConfiguration(configuration, false);
        return new ConfigurationFromContextImpl(producer, settings, context.getPsiLocation());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExecutorAction that)) {
            return false;
        }
        return myOrigin.equals(that.myOrigin) && myExecutor.equals(that.myExecutor) && myOrder == that.myOrder;
    }

    @Override
    public int hashCode() {
        int result = myOrigin.hashCode();
        result = 31 * result + myExecutor.hashCode();
        result = 31 * result + myOrder;
        return result;
    }
}
