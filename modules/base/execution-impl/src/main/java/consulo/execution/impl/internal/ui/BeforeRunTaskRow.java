package consulo.execution.impl.internal.ui;

import consulo.execution.BeforeRunFragment;
import consulo.execution.BeforeRunTask;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.impl.internal.BeforeRunFragments;
import consulo.execution.impl.internal.RunConfigurableBeforeRunTask;
import consulo.execution.impl.internal.RunConfigurationBeforeRunFragment;
import consulo.execution.impl.internal.configuration.RunManagerImpl;
import consulo.execution.impl.internal.configuration.UnknownRunConfiguration;
import consulo.execution.localize.ExecutionLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Badge;
import consulo.ui.BadgeStyle;
import consulo.ui.Component;
import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.MessageBoxes;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.FlowLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Before launch of the fragment page: one chip per task ({@link Badge} with configure/remove), reordered by dragging in a
 * {@link FlowLayout}, hidden while there are no tasks; "Add ..." actions for "Modify options"; task settings are edited in
 * {@link BeforeRunTaskDialog}.
 */
final class BeforeRunTaskRow {
    private final Runnable myChangeListener;
    private final List<BeforeRunTask> myTasks = new ArrayList<>();
    private final Map<Component, BeforeRunTask> myChipTasks = new IdentityHashMap<>();

    private RunConfiguration myRunConfiguration;

    private @Nullable DockLayout myComponent;
    private @Nullable FlowLayout myTiles;

    @RequiredUIAccess
    BeforeRunTaskRow(RunnerAndConfigurationSettings settings, Runnable changeListener) {
        myChangeListener = changeListener;
        myRunConfiguration = settings.getConfiguration();
        loadTasks(settings);
    }

    @RequiredUIAccess
    public Component createComponent() {
        DockLayout component = myComponent;
        if (component != null) {
            return component;
        }

        FlowLayout tiles = FlowLayout.create(Space.MEDIUM);
        tiles.setTransferHandler(new ChipDragAndDropHandler());
        myTiles = tiles;

        Label label = Label.create(LocalizeValue.join(ExecutionLocalize.beforeLaunchPanelTitle(), LocalizeValue.colon()));
        label.paddingBuilder().rightSet(Space.MEDIUM).apply();

        component = DockLayout.create(Space.SMALL);
        component.left(label);
        component.center(tiles);
        myComponent = component;

        rebuild();
        return component;
    }

    @RequiredUIAccess
    public void reset(RunnerAndConfigurationSettings settings) {
        loadTasks(settings);
        rebuild();
    }

    private void loadTasks(RunnerAndConfigurationSettings settings) {
        myRunConfiguration = settings.getConfiguration();
        myTasks.clear();
        myTasks.addAll(RunManagerImpl.getInstanceImpl(myRunConfiguration.getProject()).getBeforeRunTasks(myRunConfiguration));
    }

    public List<BeforeRunTask> getTasks(boolean applyCurrentState) {
        return List.copyOf(myTasks);
    }

    @RequiredUIAccess
    public void addTask(BeforeRunTask task) {
        myTasks.add(task);
        onTasksChanged();
    }

    @RequiredUIAccess
    private void onTasksChanged() {
        rebuild();
        myChangeListener.run();
    }

    @RequiredUIAccess
    private void rebuild() {
        FlowLayout tiles = myTiles;
        if (tiles == null) {
            return;
        }

        tiles.removeAll();
        myChipTasks.clear();
        DockLayout component = myComponent;
        if (component != null) {
            component.setVisible(!myTasks.isEmpty());
        }
        for (int i = 0; i < myTasks.size(); i++) {
            BeforeRunTask task = myTasks.get(i);
            Component chip = createChip(i, task);
            myChipTasks.put(chip, task);
            tiles.add(chip);
        }
    }

    @RequiredUIAccess
    private Component createChip(int index, BeforeRunTask task) {
        BeforeRunFragment<BeforeRunTask> fragment = BeforeRunFragments.find(myRunConfiguration.getProject(), task);

        LocalizeValue description = fragment == null ? LocalizeValue.of(task.getClass().getSimpleName()) : fragment.getDescription(task);
        Badge badge = Badge.create(LocalizeValue.join(LocalizeValue.of((index + 1) + ". "), description));
        badge.addStyle(BadgeStyle.NEUTRAL);
        if (fragment != null) {
            badge.setIcon(fragment.getTaskIcon(myRunConfiguration, task));
        }

        HorizontalLayout actions = HorizontalLayout.create(Space.X_SMALL);
        if (fragment != null && fragment.isConfigurable()) {
            Hyperlink configure = Hyperlink.create(LocalizeValue.empty(), event -> configure(fragment, task));
            configure.setIcon(PlatformIconGroup.actionsEdit());
            configure.setToolTipText(ExecutionLocalize.runConfigurationBeforeRunConfigureTask());
            actions.add(configure);
        }

        Hyperlink remove = Hyperlink.create(LocalizeValue.empty(), event -> {
            myTasks.remove(task);
            onTasksChanged();
        });
        remove.setIcon(PlatformIconGroup.actionsClose());
        remove.setToolTipText(ExecutionLocalize.runConfigurationFragmentRemove());
        actions.add(remove);

        badge.setSuffixComponent(actions);
        return badge;
    }

    @RequiredUIAccess
    private void configure(BeforeRunFragment<BeforeRunTask> fragment, BeforeRunTask task) {
        BeforeRunTaskDialog.configure(fragment, myRunConfiguration, task).whenCompleteAsync((value, error) -> {
            if (error == null && Boolean.TRUE.equals(value)) {
                onTasksChanged();
            }
        }, UIAccess.current());
    }

    /**
     * "Add &lt;task&gt;" for every applicable before-run fragment; shown in "Modify options".
     */
    List<AnAction> createAddActions() {
        RunConfiguration configuration = myRunConfiguration;
        Set<Object> present = new HashSet<>();
        for (BeforeRunTask task : myTasks) {
            present.add(BeforeRunFragments.identity(task));
        }

        List<AnAction> group = new ArrayList<>();
        if (configuration instanceof UnknownRunConfiguration) {
            return group;
        }

        configuration.getProject().getExtensionPoint(BeforeRunFragment.class).forEach(each -> {
            @SuppressWarnings("unchecked")
            BeforeRunFragment<BeforeRunTask> fragment = each;
            if (fragment.createTask(configuration) == null || fragment.isSingleton() && present.contains(fragment.getStateClass())) {
                return;
            }

            LocalizeValue text = ExecutionLocalize.runConfigurationBeforeRunAddTaskWith(fragment.getName());
            group.add(new AnAction(text, fragment.getName(), fragment.getIcon(configuration)) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(AnActionEvent e) {
                    BeforeRunTask task = fragment.createTask(configuration);
                    if (task == null) {
                        return;
                    }
                    task.setEnabled(true);

                    if (!fragment.isConfigurable()) {
                        addTask(task);
                        return;
                    }

                    UIAccess uiAccess = UIAccess.current();
                    BeforeRunTaskDialog.configure(fragment, configuration, task).whenCompleteAsync((value, error) -> {
                        if (error != null || !fragment.canExecuteTask(configuration, task)) {
                            return;
                        }

                        if (createsCycle(configuration, task)) {
                            MessageBoxes.okWarning(ExecutionLocalize.beforeLaunchPanelCyclic_dependency_warning(
                                configuration.getName(),
                                fragment.getDescription(task)
                            )).title(ExecutionLocalize.warningCommonTitle()).showAsync(myComponent);
                            return;
                        }
                        addTask(task);
                    }, uiAccess);
                }
            });
        });
        return group;
    }

    static boolean createsCycle(RunConfiguration configuration, BeforeRunTask task) {
        Set<RunConfiguration> configurationSet = new HashSet<>();
        collectRunBeforeRuns(configuration, task, configurationSet);
        return configurationSet.contains(configuration);
    }

    private static void collectRunBeforeRuns(RunConfiguration owner, BeforeRunTask task, Set<RunConfiguration> configurationSet) {
        if (task instanceof RunConfigurableBeforeRunTask runTask) {
            RunnerAndConfigurationSettings settings = RunConfigurationBeforeRunFragment.findSettings(owner.getProject(), runTask);
            if (settings == null) {
                return;
            }

            RunConfiguration configuration = settings.getConfiguration();
            List<BeforeRunTask> tasks = RunManagerImpl.getInstanceImpl(configuration.getProject()).getBeforeRunTasks(configuration);
            for (BeforeRunTask beforeRunTask : tasks) {
                if (beforeRunTask instanceof RunConfigurableBeforeRunTask configurableBeforeRunTask) {
                    RunnerAndConfigurationSettings next =
                        RunConfigurationBeforeRunFragment.findSettings(configuration.getProject(), configurableBeforeRunTask);
                    if (next != null && configurationSet.add(next.getConfiguration())) {
                        collectRunBeforeRuns(owner, beforeRunTask, configurationSet);
                    }
                }
            }
        }
    }

    private class ChipDragAndDropHandler implements DragAndDropTransferHandler<Component> {
        @Override
        public @Nullable DataTransfer createTransfer(Component component) {
            return null;
        }

        @Override
        public @Nullable DataTransfer createDragTransfer(Component component, List<Component> items, boolean move) {
            if (!move || items.size() != 1 || !myChipTasks.containsKey(items.get(0))) {
                return null;
            }
            return DataTransfer.of("before-run-task");
        }

        @RequiredUIAccess
        @Override
        public boolean drop(Component component, DropContext<Component> context) {
            List<Component> items = context.getItems();
            if (items.size() != 1) {
                return false;
            }

            BeforeRunTask dragged = myChipTasks.get(items.get(0));
            BeforeRunTask target = myChipTasks.get(context.getTarget());
            if (dragged == null || target == null || dragged == target) {
                return false;
            }

            if (!context.isCheckOnly()) {
                myTasks.remove(dragged);
                int index = myTasks.indexOf(target);
                myTasks.add(context.getPosition() == DropPosition.ABOVE ? index : index + 1, dragged);
                onTasksChanged();
            }
            return true;
        }
    }
}
