/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.ide.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataValidator;
import consulo.module.Module;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author yole
 */
@ExtensionImpl
public class ModuleDataValidator implements DataValidator<Module> {
  @Nonnull
  @Override
  public Key<Module> getKey() {
    return Module.KEY;
  }

  @Nullable
  @Override
  public Module findInvalid(Key<Module> key, Module data, Object dataSource) {
    return data.isDisposed() ? data : null;
  }
}
