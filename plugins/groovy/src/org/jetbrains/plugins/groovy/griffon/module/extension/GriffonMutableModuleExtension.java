/*
 * Copyright 2013 Consulo.org
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
package org.jetbrains.plugins.groovy.griffon.module.extension;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import org.consulo.module.extension.MutableModuleExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 14:33/30.06.13
 */
public class GriffonMutableModuleExtension extends GriffonModuleExtension implements MutableModuleExtension<GriffonModuleExtension> {
  @NotNull
  private final GriffonModuleExtension myOriginalModuleExtension;

  public GriffonMutableModuleExtension(@NotNull String id, @NotNull Module module, @NotNull GriffonModuleExtension originalModuleExtension) {
    super(id, module);
    myOriginalModuleExtension = originalModuleExtension;
  }

  @Nullable
  @Override
  public JComponent createConfigurablePanel(@NotNull ModifiableRootModel rootModel, @Nullable Runnable updateOnCheck) {
    return null;
  }

  @Override
  public void setEnabled(boolean val) {
    myIsEnabled = val;
  }

  @Override
  public boolean isModified() {
    return myIsEnabled != myOriginalModuleExtension.isEnabled();
  }

  @Override
  public void commit() {
    myOriginalModuleExtension.commit(this);
  }
}
