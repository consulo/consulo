/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vfs.newvfs.impl;

import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.StubVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nonnull;

/**
 * @author max
 */
public class FakeVirtualFile extends StubVirtualFile {
  private final VirtualFile myParent;
  private final String myName;

  public FakeVirtualFile(@Nonnull final VirtualFile parent, @Nonnull final String name) {
    myName = name;
    myParent = parent;
  }

  @Override
  @Nonnull
  public VirtualFile getParent() {
    return myParent;
  }

  @Override
  public boolean isDirectory() {
    return false;
  }

  @Nonnull
  @Override
  public String getPath() {
    final String basePath = myParent.getPath();
    return StringUtil.endsWithChar(basePath, '/') ? basePath + myName : basePath + '/' + myName;
  }

  @Override
  @Nonnull
  @NonNls
  public String getName() {
    return myName;
  }

  @Override
  public String toString() {
    return getPath();
  }
}