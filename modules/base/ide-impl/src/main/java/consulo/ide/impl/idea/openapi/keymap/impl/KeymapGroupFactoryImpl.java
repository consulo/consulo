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
package consulo.ide.impl.idea.openapi.keymap.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.ui.ex.keymap.KeymapGroup;
import consulo.ui.ex.keymap.KeymapGroupFactory;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.KeymapGroupImpl;
import consulo.ui.image.Image;

import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * @author yole
 */
@Singleton
@ServiceImpl
public class KeymapGroupFactoryImpl extends KeymapGroupFactory {
  @Nonnull
  @Override
  public KeymapGroup createGroup(final String name) {
    return new KeymapGroupImpl(name, null, null);
  }

  @Nonnull
  @Override
  public KeymapGroup createGroup(final String name, final Image icon) {
    return new KeymapGroupImpl(name, icon);
  }
}
