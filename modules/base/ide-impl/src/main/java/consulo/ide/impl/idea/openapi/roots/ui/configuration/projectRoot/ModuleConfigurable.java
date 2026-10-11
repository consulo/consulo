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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot;

import consulo.configurable.ConfigurationException;
import consulo.configurable.MasterDetailsConfigurable;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.ModuleEditor;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.ModulesConfiguratorImpl;
import consulo.ide.impl.roots.ui.configuration.projectRoot.moduleLayerActions.DeleteLayerAction;
import consulo.ide.impl.roots.ui.configuration.projectRoot.moduleLayerActions.NewLayerAction;
import consulo.localize.LocalizeValue;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleWithNameAlreadyExistsException;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.util.FormBuilder;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * @author anna
 * @since 2006-06-04
 */
public class ModuleConfigurable implements MasterDetailsConfigurable<Module> {
    private final Module myModule;
    private final ModulesConfiguratorImpl myConfigurator;
    private final BiConsumer<Module, String> myNameChanged;

    private String myModuleName;

    private @Nullable DockLayout myMainPanel;
    private @Nullable TextBox myNameComponent;
    private ModuleEditor.@Nullable ChangeListener myLayerListener;

    public ModuleConfigurable(ModulesConfiguratorImpl configurator, Module module, BiConsumer<Module, String> nameChanged) {
        myModule = module;
        myConfigurator = configurator;
        myNameChanged = nameChanged;
        myModuleName = module.getName();
    }

    @RequiredUIAccess
    @Override
    public Component createUIComponent(Disposable parentUIDisposable) {
        DockLayout mainPanel = myMainPanel;
        if (mainPanel == null) {
            mainPanel = createMainPanel(parentUIDisposable);
            myMainPanel = mainPanel;
        }
        return mainPanel;
    }

    @RequiredUIAccess
    private DockLayout createMainPanel(Disposable parentUIDisposable) {
        ModuleEditor moduleEditor = getModuleEditor();

        TextBox nameComponent = TextBox.create(myModuleName);
        nameComponent.addValueListener(event -> rename(Objects.requireNonNullElse(event.getValue(), "")));
        myNameComponent = nameComponent;

        FormBuilder formBuilder = FormBuilder.create();
        formBuilder.addLabeled(CommonLocalize.nameLabelText(), nameComponent);
        formBuilder.addLabeled(ProjectLocalize.moduleLayerLabel(), createLayerComponent(moduleEditor));

        DockLayout mainPanel = DockLayout.create();
        mainPanel.top(formBuilder.build());
        mainPanel.center(moduleEditor.getUIComponent(parentUIDisposable));
        return mainPanel;
    }

    @RequiredUIAccess
    private Component createLayerComponent(ModuleEditor moduleEditor) {
        ModifiableRootModel rootModel = moduleEditor.getModifiableRootModelProxy();

        MutableFlatDataModel<String> layers = FlatDataModel.of(rootModel.getLayers().keySet());
        ComboBox<String> layerComponent = ComboBox.create(layers);
        layerComponent.setValue(rootModel.getCurrentLayerName(), false);
        layerComponent.setEnabled(rootModel.getLayers().size() > 1);
        layerComponent.addValueListener(event -> {
            String layer = event.getValue();
            ModifiableRootModel proxy = moduleEditor.getModifiableRootModelProxy();
            if (layer != null && !Objects.equals(proxy.getCurrentLayerName(), layer)) {
                proxy.setCurrentLayer(layer);
            }
        });

        ModuleEditor.ChangeListener layerListener = model -> {
            layers.replaceAll(model.getLayers().keySet());
            layerComponent.setValue(model.getCurrentLayerName(), false);
            layerComponent.setEnabled(model.getLayers().size() > 1);
        };
        moduleEditor.addChangeListener(layerListener);
        myLayerListener = layerListener;

        ActionGroup group = ActionGroup.newImmutableBuilder()
            .add(new NewLayerAction(moduleEditor, false, () -> layerComponent))
            .add(new DeleteLayerAction(moduleEditor))
            .add(new NewLayerAction(moduleEditor, true, () -> layerComponent))
            .build();

        ActionToolbar toolbar = ActionToolbarFactory.getInstance()
            .createActionToolbar(ActionPlaces.UNKNOWN, group, ActionToolbar.Style.HORIZONTAL);
        toolbar.setTargetUIComponent(layerComponent);

        DockLayout layerRow = DockLayout.create();
        layerRow.center(layerComponent);
        layerRow.right(toolbar.getUIComponent());
        return layerRow;
    }

    @RequiredUIAccess
    private void rename(String newName) {
        String name = newName.trim();
        if (name.equals(myModuleName)) {
            return;
        }

        if (!name.isEmpty()) {
            ModifiableModuleModel moduleModel = myConfigurator.getModuleModel();
            try {
                moduleModel.renameModule(myModule, name);
            }
            catch (ModuleWithNameAlreadyExistsException ignored) {
            }
            myConfigurator.moduleRenamed(myModule, myModuleName, name);
        }

        myModuleName = name;
        myConfigurator.setModified(!Objects.equals(myModuleName, myModule.getName()));
        myNameChanged.accept(myModule, name);
    }

    public String getModuleName() {
        return myModuleName;
    }

    @RequiredUIAccess
    @Override
    public void updateName() {
        TextBox nameComponent = myNameComponent;
        if (nameComponent != null) {
            nameComponent.setValue(myModuleName, false);
        }
    }

    @Override
    public Module getEditableObject() {
        return myModule;
    }

    public Module getModule() {
        return myModule;
    }

    @Override
    public LocalizeValue getBannerSlogan() {
        return ProjectLocalize.projectRootsModuleBannerText(myModuleName);
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.of(myModuleName);
    }

    @Override
    public @Nullable Image getIcon() {
        return PlatformIconGroup.nodesModule();
    }

    @Override
    public @Nullable String getHelpTopic() {
        ModuleEditor moduleEditor = myConfigurator.getModuleEditor(myModule);
        return moduleEditor != null ? moduleEditor.getHelpTopic() : null;
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        return false;
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        ModuleEditor.ChangeListener layerListener = myLayerListener;
        ModuleEditor moduleEditor = myConfigurator.getModuleEditor(myModule);
        if (layerListener != null && moduleEditor != null) {
            moduleEditor.removeChangeListener(layerListener);
        }
        myLayerListener = null;
        myMainPanel = null;
        myNameComponent = null;
    }

    public ModuleEditor getModuleEditor() {
        return myConfigurator.getOrCreateModuleEditor(myModule);
    }
}
