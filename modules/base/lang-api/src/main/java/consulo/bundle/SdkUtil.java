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
package consulo.bundle;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.util.ObjectUtil;
import consulo.annotation.DeprecationInfo;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.pointers.NamedPointer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 21.08.14
 */
public class SdkUtil {
  @Nonnull
  @DeprecationInfo(value = "Use SdkPointerManager.getInstance()", until = "2.0")
  public static NamedPointer<Sdk> createPointer(@Nonnull Sdk sdk) {
    return SdkPointerManager.getInstance().create(sdk);
  }

  @Nonnull
  @DeprecationInfo(value = "Use SdkPointerManager.getInstance()", until = "2.0")
  public static NamedPointer<Sdk> createPointer(@Nonnull String name) {
    return SdkPointerManager.getInstance().create(name);
  }

  @Nonnull
  public static Image getIcon(@Nullable Sdk sdk) {
    if (sdk == null) {
      return AllIcons.Actions.Help;
    }
    SdkType sdkType = (SdkType)sdk.getSdkType();
    Image icon = ObjectUtil.notNull(sdkType.getIcon(), AllIcons.Actions.Help);
    if(sdk.isPredefined()) {
      return ImageEffects.layered(icon, AllIcons.Nodes.Locked);
    }
    else {
      return icon;
    }
  }
}
