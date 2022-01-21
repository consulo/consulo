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
package consulo.content.bundle;

import consulo.annotation.DeprecationInfo;
import consulo.component.util.pointer.NamedPointer;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 21.08.14
 */
public class SdkUtil {
  @Nonnull
  @DeprecationInfo(value = "Use SdkPointerManager.getInstance()")
  public static NamedPointer<Sdk> createPointer(@Nonnull Sdk sdk) {
    return SdkPointerManager.getInstance().create(sdk);
  }

  @Nonnull
  @DeprecationInfo(value = "Use SdkPointerManager.getInstance()")
  public static NamedPointer<Sdk> createPointer(@Nonnull String name) {
    return SdkPointerManager.getInstance().create(name);
  }

  @Nonnull
  public static Image getIcon(@Nullable Sdk sdk) {
    if (sdk == null) {
      return PlatformIconGroup.actionsHelp();
    }
    SdkType sdkType = (SdkType)sdk.getSdkType();
    Image icon = sdkType.getIcon();
    if (sdk.isPredefined()) {
      return ImageEffects.layered(icon, PlatformIconGroup.nodesLocked());
    }
    else {
      return icon;
    }
  }
}
