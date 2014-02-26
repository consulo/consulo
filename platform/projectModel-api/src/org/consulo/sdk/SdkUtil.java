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
package org.consulo.sdk;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IconDescriptor;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.util.ObjectUtils;
import org.consulo.util.pointers.NamedPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 17:51/15.06.13
 */
public class SdkUtil {
  @NotNull
  public static NamedPointer<Sdk> createPointer(@NotNull Sdk sdk) {
    final SdkPointerManager service = ServiceManager.getService(SdkPointerManager.class);
    return service.create(sdk);
  }

  @NotNull
  public static NamedPointer<Sdk> createPointer(@NotNull String name) {
    final SdkPointerManager service = ServiceManager.getService(SdkPointerManager.class);
    return service.create(name);
  }

  @NotNull
  public static Icon getIcon(@Nullable Sdk sdk) {
    if (sdk == null) {
      return AllIcons.Toolbar.Unknown;
    }
    SdkType sdkType = (SdkType)sdk.getSdkType();
    Icon icon = ObjectUtils.notNull(sdkType.getIcon(), AllIcons.Toolbar.Unknown);
    if(sdk.isBundled()) {
      return new IconDescriptor(icon).addLayerIcon(AllIcons.Nodes.Locked).toIcon();
    }
    else {
      return icon;
    }
  }
}
