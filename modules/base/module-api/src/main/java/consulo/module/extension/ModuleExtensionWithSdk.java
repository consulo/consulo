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
package consulo.module.extension;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTypeId;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 23:26/18.05.13
 */
public interface ModuleExtensionWithSdk<T extends ModuleExtensionWithSdk<T>> extends ModuleExtension<T> {
  @Nonnull
  ModuleInheritableNamedPointer<Sdk> getInheritableSdk();

  @Nullable
  Sdk getSdk();

  @Nullable
  String getSdkName();

  @Nonnull
  Class<? extends SdkTypeId> getSdkTypeClass();
}
