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
package consulo.module.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.module.Module;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.layer.event.ModuleRootLayerListener;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.module.extension.event.ModuleExtensionChangeListener;
import consulo.util.lang.Couple;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 31.07.14
 */
@TopicImpl(ComponentScope.PROJECT)
public class ExtensionListenerByLayerListenerInvoker implements ModuleRootLayerListener {
  private final Application myApplication;

  @Inject
  public ExtensionListenerByLayerListenerInvoker(Application application) {
    myApplication = application;
  }

  @Override
  public void currentLayerChanged(@Nonnull final Module module,
                                  @Nonnull final String oldName,
                                  @Nonnull final ModuleRootLayer oldLayer,
                                  @Nonnull final String newName,
                                  @Nonnull final ModuleRootLayer newLayer) {

    final List<Couple<ModuleExtension>> list = new ArrayList<>();
    for (ModuleExtensionProvider providerEP : myApplication.getExtensionPoint(ModuleExtensionProvider.class).getExtensionList()) {
      MutableModuleExtension oldExtension = oldLayer.getExtensionWithoutCheck(providerEP.getId());
      MutableModuleExtension newExtension = newLayer.getExtensionWithoutCheck(providerEP.getId());

      if (oldExtension == null || newExtension == null) {
        continue;
      }

      if (oldExtension.isModified(newExtension)) {
        list.add(Couple.<ModuleExtension>of(oldExtension, newExtension));
      }
    }

    ApplicationManager.getApplication().invokeLater(() -> {
      ModuleExtensionChangeListener moduleExtensionChangeListener = module.getProject().getMessageBus().syncPublisher(ModuleExtensionChangeListener.class);

      for (Couple<ModuleExtension> couple : list) {
        moduleExtensionChangeListener.beforeExtensionChanged(couple.getFirst(), couple.getSecond());
      }
    }, IdeaModalityState.nonModal());
  }
}
