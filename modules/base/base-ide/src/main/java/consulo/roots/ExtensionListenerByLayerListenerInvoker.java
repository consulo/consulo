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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.module.Module;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.ModuleExtensionChangeListener;
import consulo.module.extension.ModuleExtensionProviderEP;
import consulo.module.extension.MutableModuleExtension;
import consulo.module.extension.impl.ModuleExtensionProviders;
import consulo.util.lang.Couple;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 31.07.14
 */
public class ExtensionListenerByLayerListenerInvoker extends ModuleRootLayerListener.Adapter {
  @Override
  public void currentLayerChanged(@Nonnull final Module module,
                                  @Nonnull final String oldName,
                                  @Nonnull final ModuleRootLayer oldLayer,
                                  @Nonnull final String newName,
                                  @Nonnull final ModuleRootLayer newLayer) {

    final List<Couple<ModuleExtension>> list = new ArrayList<>();
    for (ModuleExtensionProviderEP providerEP : ModuleExtensionProviders.getProviders()) {
      MutableModuleExtension oldExtension = oldLayer.getExtensionWithoutCheck(providerEP.getKey());
      MutableModuleExtension newExtension = newLayer.getExtensionWithoutCheck(providerEP.getKey());

      if (oldExtension == null || newExtension == null) {
        continue;
      }

      if (oldExtension.isModified(newExtension)) {
        list.add(Couple.<ModuleExtension>of(oldExtension, newExtension));
      }
    }

    ApplicationManager.getApplication().invokeLater(() -> {
      ModuleExtensionChangeListener moduleExtensionChangeListener = module.getProject().getMessageBus().syncPublisher(ModuleExtension.CHANGE_TOPIC);

      for (Couple<ModuleExtension> couple : list) {
        moduleExtensionChangeListener.beforeExtensionChanged(couple.getFirst(), couple.getSecond());
      }
    }, ModalityState.NON_MODAL);
  }
}
