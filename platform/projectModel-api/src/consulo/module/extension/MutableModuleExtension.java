/*
 * Copyright 2013 must-be.org
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
package consulo.module.extension;

import consulo.roots.ModifiableModuleRootLayer;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import consulo.annotations.RequiredDispatchThread;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 10:58/19.05.13
 */
public interface MutableModuleExtension<T extends ModuleExtension<T>> extends ModuleExtension<T> {
  @Nullable
  @RequiredDispatchThread
  default JComponent createConfigurablePanel(@NotNull Runnable updateOnCheck) {
    return null;
  }

  @Nullable
  @RequiredUIAccess
  default Component createConfigurationComponent(@NotNull Runnable updateOnCheck) {
    return null;
  }

  @Override
  @NotNull
  ModifiableModuleRootLayer getModuleRootLayer();

  void setEnabled(boolean val);

  boolean isModified(@NotNull T originalExtension);
}
