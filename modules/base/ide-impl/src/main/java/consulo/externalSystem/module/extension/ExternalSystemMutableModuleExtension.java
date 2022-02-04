/*
 * Copyright 2013-2017 consulo.io
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
package consulo.externalSystem.module.extension;

import consulo.module.extension.MutableModuleExtension;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 03-Jun-17
 */
public interface ExternalSystemMutableModuleExtension<T extends ExternalSystemModuleExtension<T>> extends ExternalSystemModuleExtension<T>, MutableModuleExtension<T> {
  void setOption(@Nonnull String key, @Nullable String value);

  void removeOption(@Nonnull String key);

  void removeAllOptions();
}
