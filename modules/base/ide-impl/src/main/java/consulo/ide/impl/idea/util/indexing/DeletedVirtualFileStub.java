/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.util.indexing;

import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.language.file.light.LightVirtualFile;
import jakarta.annotation.Nonnull;

/**
 * @author Maxim.Mossienko on 11/18/2015.
 */
class DeletedVirtualFileStub extends LightVirtualFile implements VirtualFileWithId {
  private final int myFileId;
  private boolean myResurrected;

  DeletedVirtualFileStub(VirtualFileWithId original) {
    setOriginalFile((VirtualFile)original);
    myFileId = -Math.abs(original.getId());
  }

  @Override
  public int getId() {
    return myFileId;
  }

  @Override
  public boolean isValid() {
    return false;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof DeletedVirtualFileStub) {
      return ((DeletedVirtualFileStub)obj).getId() == myFileId;
    }
    return super.equals(obj);
  }

  public boolean isResurrected() {
    return myResurrected;
  }

  public void setResurrected(boolean resurrected) {
    myResurrected = resurrected;
  }

  @Override
  public int hashCode() {
    return myFileId;
  }

  @Override
  public String toString() {
    return "invalid:" + getOriginalFile().toString() + ", resurrected:" + myResurrected;
  }

  @Nonnull
  @Override
  public String getUrl() {
    return "invalid:" + super.getUrl();
  }
}
