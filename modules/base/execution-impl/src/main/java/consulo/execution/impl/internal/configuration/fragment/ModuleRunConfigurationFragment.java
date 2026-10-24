package consulo.execution.impl.internal.configuration.fragment;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AccessRule;
import consulo.disposer.Disposable;
import consulo.execution.configuration.ModuleBasedConfiguration;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.fragment.ModuleFragmentState;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentOrder;
import consulo.execution.localize.ExecutionLocalize;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ComboBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * The module a {@link ModuleBasedConfiguration} runs in, chosen from its valid modules.
 */
@ExtensionImpl
public class ModuleRunConfigurationFragment implements RunConfigurationFragment<RunConfiguration> {
    @Override
    public String getId() {
        return "module";
    }

    @Override
    public LocalizeValue getName() {
        return ExecutionLocalize.runConfigurationFragmentModule();
    }

    @Override
    public int getOrder() {
        return StandardRunConfigurationFragmentOrder.MODULE;
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

    @Override
    public Class<ModuleFragmentState> getStateClass() {
        return ModuleFragmentState.class;
    }

    @Override
    public boolean isApplicable(RunConfiguration configuration) {
        return configuration instanceof ModuleBasedConfiguration;
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<RunConfiguration> createEditor(Project project, Disposable parent) {
        MutableFlatDataModel<Module> model = FlatDataModel.of(List.of());
        ComboBox<Module> box = ComboBox.create(model);
        box.setRenderer((presentation, item) -> {
            Module module = item.getValue();
            if (module != null) {
                presentation.withIcon(PlatformIconGroup.nodesModule());
                presentation.append(module.getName());
            }
            else {
                presentation.append(ExecutionLocalize.runConfigurationFragmentModuleNone());
            }
        });

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addLabeled(LocalizeValue.join(ExecutionLocalize.runConfigurationFragmentModule(), LocalizeValue.colon()), box);
            }

            @RequiredUIAccess
            @Override
            public void reset(RunConfiguration target) {
                ModuleBasedConfiguration<?> configuration = (ModuleBasedConfiguration<?>) target;
                Collection<Module> validModules = AccessRule.read(configuration::getValidModules);
                List<Module> modules = new ArrayList<>(validModules == null ? List.of() : validModules);
                modules.sort(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
                modules.add(0, null);
                model.replaceAll(modules);
                box.setValue(configuration.getConfigurationModule().getModule());
            }

            @RequiredUIAccess
            @Override
            public void apply(RunConfiguration target) {
                ((ModuleBasedConfiguration<?>) target).setModule(box.getValue());
            }
        };
    }
}
