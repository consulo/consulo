/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.fileEditor;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.component.ComponentManager;
import consulo.component.extension.ExtensionExtender;
import consulo.ide.impl.idea.ui.EditorNotificationProvider;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 18-Jul-22
 */
@ExtensionImpl
public class NewEditorNotificationProviderExtender implements ExtensionExtender<EditorNotificationProvider> {
  @Override
  public void extend(@Nonnull ComponentManager componentManager, @Nonnull Consumer<EditorNotificationProvider> consumer) {
    for (consulo.fileEditor.EditorNotificationProvider provider : componentManager.getExtensionPoint(consulo.fileEditor.EditorNotificationProvider.class)) {
      if (provider instanceof DumbAware) {
        consumer.accept(new DumbAwareNewEditorNotificationProvider(provider));
      }
      else {
        consumer.accept(new NewEditorNotificationProvider(provider));
      }
    }
  }

  @Nonnull
  @Override
  public Class<EditorNotificationProvider> getExtensionClass() {
    return EditorNotificationProvider.class;
  }

  @Override
  public boolean hasAnyExtensions(ComponentManager componentManager) {
    return componentManager.getExtensionPoint(consulo.fileEditor.EditorNotificationProvider.class).hasAnyExtensions();
  }
}
