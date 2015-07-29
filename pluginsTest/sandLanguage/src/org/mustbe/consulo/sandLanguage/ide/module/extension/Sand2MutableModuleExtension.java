/*
 * Copyright 2013-2014 must-be.org
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
package org.mustbe.consulo.sandLanguage.ide.module.extension;

import com.intellij.openapi.roots.ModuleRootLayer;
import org.consulo.module.extension.MutableModuleExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredDispatchThread;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 30.08.14
 */
public class Sand2MutableModuleExtension extends Sand2ModuleExtension implements MutableModuleExtension<Sand2ModuleExtension> {
  public Sand2MutableModuleExtension(@NotNull String id, @NotNull ModuleRootLayer moduleRootLayer) {
    super(id, moduleRootLayer);
  }

  @RequiredDispatchThread
  @Nullable
  @Override
  public JComponent createConfigurablePanel(@NotNull Runnable updateOnCheck) {
    return null;
  }

  @Override
  public void setEnabled(boolean val) {
    myIsEnabled = val;
  }

  @Override
  public boolean isModified(@NotNull Sand2ModuleExtension originalExtension) {
    return myIsEnabled != originalExtension.isEnabled();
  }
}
