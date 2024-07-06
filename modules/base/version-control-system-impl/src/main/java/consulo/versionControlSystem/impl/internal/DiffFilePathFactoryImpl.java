/*
 * Copyright 2013-2024 consulo.io
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
package consulo.versionControlSystem.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.diff.DiffFilePath;
import consulo.diff.internal.DiffFilePathFactory;
import consulo.versionControlSystem.action.VcsContextFactory;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 06-Jul-24
 */
@Singleton
@ServiceImpl
public class DiffFilePathFactoryImpl implements DiffFilePathFactory {
  private final VcsContextFactory myVcsContextFactory;

  @Inject
  public DiffFilePathFactoryImpl(VcsContextFactory vcsContextFactory) {
    myVcsContextFactory = vcsContextFactory;
  }

  @Override
  public DiffFilePath createFilePath(@Nonnull VirtualFile file) {
    return myVcsContextFactory.createFilePathOn(file);
  }
}
