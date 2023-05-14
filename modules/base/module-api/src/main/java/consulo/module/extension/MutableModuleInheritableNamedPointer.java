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

import consulo.annotation.access.RequiredReadAction;
import consulo.component.util.pointer.Named;
import consulo.module.Module;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 20:06/15.06.13
 */
public interface MutableModuleInheritableNamedPointer<T extends Named> extends ModuleInheritableNamedPointer<T> {
  @RequiredReadAction
  void set(@Nonnull ModuleInheritableNamedPointer<T> value);

  @RequiredReadAction
  void set(@Nullable String moduleName, @Nullable String name);

  @RequiredReadAction
  void set(@Nullable Module module, @Nullable T named);
}
