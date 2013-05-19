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
package org.consulo.java.platform.module.extension;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Comparing;
import org.consulo.module.extension.MutableModuleExtensionWithSdk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 12:39/19.05.13
 */
public class JavaMutableModuleExtension extends JavaModuleExtension implements MutableModuleExtensionWithSdk<JavaModuleExtension> {
  @NotNull
  private final JavaModuleExtension myModuleExtension;

  public JavaMutableModuleExtension(@NotNull String id, @NotNull Module module, @NotNull JavaModuleExtension moduleExtension) {
    super(id, module);
    myModuleExtension = moduleExtension;

    commit(myModuleExtension);
  }

  @Override
  public void setEnabled(boolean val) {
    myIsEnabled = val;
  }

  @Override
  public boolean isModified() {
    if(myIsEnabled != myModuleExtension.isEnabled()) {
      return true;
    }
    if(!Comparing.equal(mySdkName, myModuleExtension.getSdkName())) {
      return true;
    }
    return false;
  }

  @Override
  public void commit() {
    myModuleExtension.commit(this);
  }

  @Override
  public void setSdk(@Nullable Sdk sdk) {
    mySdkName = sdk == null ? null : sdk.getName();
  }
}
