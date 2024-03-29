/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author yole
 */
public interface SdkTypeId {
  @Deprecated
  @DeprecationInfo("Use #getId()")
  String getName();

  @Nonnull
  @SuppressWarnings("deprecation")
  default String getId() {
    return getName();
  }

  @Nullable
  String getVersionString(Sdk sdk);

  void saveAdditionalData(SdkAdditionalData additionalData, Element additional);

  @Nullable
  SdkAdditionalData loadAdditionalData(Sdk currentSdk, Element additional);
}
