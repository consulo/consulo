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
package consulo.roots;

import com.intellij.ProjectTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Couple;
import com.intellij.util.messages.MessageBus;
import consulo.module.extension.*;
import consulo.module.extension.impl.ModuleExtensionProviders;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 31.07.14
 */
@SuppressWarnings("unchecked")
public class ExtensionListenerByLayerListenerInvoker extends AbstractProjectComponent {
  public ExtensionListenerByLayerListenerInvoker(@NotNull Project project, @NotNull final MessageBus bus) {
    super(project);
    bus.connect().subscribe(ProjectTopics.MODULE_LAYERS, new ModuleRootLayerListener.Adapter() {
      @Override
      public void currentLayerChanged(@NotNull final Module module,
                                      @NotNull final String oldName,
                                      @NotNull final ModuleRootLayer oldLayer,
                                      @NotNull final String newName,
                                      @NotNull final ModuleRootLayer newLayer) {

        final List<Couple<ModuleExtension>> list = new ArrayList<Couple<ModuleExtension>>();
        for (ModuleExtensionProviderEP providerEP : ModuleExtensionProviders.getProviders()) {
          MutableModuleExtension oldExtension = oldLayer.getExtensionWithoutCheck(providerEP.getKey());
          MutableModuleExtension newExtension = newLayer.getExtensionWithoutCheck(providerEP.getKey());

          if(oldExtension == null || newExtension == null) {
            continue;
          }

          if(oldExtension.isModified(newExtension)) {
            list.add(Couple.<ModuleExtension>of(oldExtension, newExtension));
          }
        }

        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            ModuleExtensionChangeListener moduleExtensionChangeListener = bus.syncPublisher(ModuleExtension.CHANGE_TOPIC);

            for (Couple<ModuleExtension> couple : list) {
              moduleExtensionChangeListener.beforeExtensionChanged(couple.getFirst(), couple.getSecond());
            }
          }
        }, ModalityState.NON_MODAL);
      }
    });
  }
}
