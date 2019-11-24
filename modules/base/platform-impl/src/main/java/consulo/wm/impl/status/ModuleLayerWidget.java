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
package consulo.wm.impl.status;

import com.intellij.ProjectTopics;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup;
import com.intellij.util.ListWithSelection;
import consulo.roots.ModuleRootLayer;
import consulo.roots.ModuleRootLayerListener;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * @author VISTALL
 * @since 30.07.14
 */
public class ModuleLayerWidget extends EditorBasedStatusBarPopup implements CustomStatusBarWidget {
  public ModuleLayerWidget(@Nonnull Project project) {
    super(project, false);
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
    return new ModuleLayerWidget(project);
  }

  @Override
  public void install(@Nonnull StatusBar statusBar) {
    super.install(statusBar);

    myProject.getMessageBus().connect().subscribe(ProjectTopics.MODULE_LAYERS, new ModuleRootLayerListener.Adapter() {
      @Override
      public void layerRemove(@Nonnull Module module, @Nonnull ModuleRootLayer removed) {
        update();
      }

      @Override
      public void layerAdded(@Nonnull Module module, @Nonnull ModuleRootLayer added) {
        update();
      }

      @Override
      public void currentLayerChanged(@Nonnull Module module, @Nonnull String oldName, @Nonnull ModuleRootLayer oldLayer, @Nonnull String newName, @Nonnull ModuleRootLayer newLayer) {
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
          Project project = e.getProject();
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

  @Nonnull
  @Override
  public String ID() {
    return "ModuleLayerWidget";
  }
}
