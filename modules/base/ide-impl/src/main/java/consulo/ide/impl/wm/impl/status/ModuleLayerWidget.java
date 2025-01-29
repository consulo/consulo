/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.impl.wm.impl.status;

import consulo.application.WriteAction;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.openapi.wm.impl.status.EditorBasedStatusBarPopup;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootLayerListener;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.project.Project;
import consulo.project.ui.wm.CustomStatusBarWidget;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.util.collection.ListWithSelection;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;

/**
 * @author VISTALL
 * @since 30.07.14
 */
public class ModuleLayerWidget extends EditorBasedStatusBarPopup implements CustomStatusBarWidget {
    public ModuleLayerWidget(@Nonnull Project project, @Nonnull StatusBarWidgetFactory factory) {
        super(project, factory, false);
    }

    @Nonnull
    @Override
    protected WidgetState getWidgetState(@Nullable VirtualFile file) {
        if (file == null) {
            return WidgetState.HIDDEN;
        }

        Module module = ModuleUtilCore.findModuleForFile(file, getProject());
        if (module == null) {
            return WidgetState.HIDDEN;
        }
        ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
        Map<String, ModuleRootLayer> layers = moduleRootManager.getLayers();
        if (layers.size() == 1) {
            return WidgetState.HIDDEN;
        }

        String currentLayerName = moduleRootManager.getCurrentLayerName();
        return new WidgetState("Module Layer: " + currentLayerName, currentLayerName, true);
    }

    @Nonnull
    @Override
    protected StatusBarWidget createInstance(@Nonnull Project project) {
        return new ModuleLayerWidget(project, myFactory);
    }

    @Override
    public void install(@Nonnull StatusBar statusBar) {
        super.install(statusBar);

        myProject.getMessageBus().connect(this).subscribe(ModuleRootListener.class, new ModuleRootListener() {
            @Override
            public void rootsChanged(ModuleRootEvent event) {
                update();
            }
        });
    }

    @Nullable
    @Override
    protected ListPopup createPopup(DataContext context) {
        ActionGroup.Builder builder = ActionGroup.newImmutableBuilder();

        ListWithSelection<String> profiles = getLayers();
        assert profiles != null;
        for (String profile : profiles) {
            if (Comparing.equal(profile, profiles.getSelection())) {
                continue;
            }

            builder.add(new AnAction(profile) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    Project project = e.getData(Project.KEY);
                    VirtualFile selectedFile = getSelectedFile();
                    if (selectedFile == null || project == null) {
                        return;
                    }
                    Module moduleForFile = ModuleUtilCore.findModuleForFile(selectedFile, project);
                    if (moduleForFile == null) {
                        return;
                    }

                    ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(moduleForFile).getModifiableModel();
                    modifiableModel.setCurrentLayer(profile);
                    WriteAction.run(modifiableModel::commit);
                }
            });
        }

        return JBPopupFactory.getInstance().createActionGroupPopup("Module Layer", builder.build(), context, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);
    }

    @Nullable
    private ListWithSelection<String> getLayers() {
        VirtualFile file = getSelectedFile();
        Project project = getProject();

        Module moduleForFile = file == null ? null : ModuleUtilCore.findModuleForFile(file, project);
        if (moduleForFile == null) {
            return null;
        }

        ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(moduleForFile);
        Map<String, ModuleRootLayer> layers = moduleRootManager.getLayers();
        if (layers.size() == 1) {
            return null;
        }
        String currentLayerName = moduleRootManager.getCurrentLayerName();

        return new ListWithSelection<>(layers.keySet(), currentLayerName);
    }
}
