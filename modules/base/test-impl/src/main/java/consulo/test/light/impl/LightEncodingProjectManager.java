/*
 * Copyright 2013-2018 consulo.io
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
package consulo.test.light.impl;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.component.util.ModificationTracker;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.encoding.EncodingProjectManager;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.LIGHT_TEST)
public class LightEncodingProjectManager extends LightEncodingManager implements EncodingProjectManager, ModificationTracker {
  @Nonnull
  @Override
  public ModificationTracker getModificationTracker() {
    return this;
  }

  @Nonnull
  @Override
  public Map<? extends VirtualFile, ? extends Charset> getAllMappings() {
    return Map.of();
  }

  @Override
  public long getModificationCount() {
    return 0;
  }
}
