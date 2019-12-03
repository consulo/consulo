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
package consulo.roots.impl;

import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ContentFolder;
import consulo.util.dataholder.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.roots.ContentFolderTypeProvider;

import java.util.Collections;
import java.util.Map;

/**
 * @author VISTALL
 * @since 05.03.14
 */
public class LightContentFolderImpl implements ContentFolder {
  private final VirtualFilePointer myFile;
  private final ContentFolderTypeProvider myContentFolderTypeProvider;
  private final ContentEntry myContentEntry;

  public LightContentFolderImpl(VirtualFilePointer file, ContentFolderTypeProvider contentFolderTypeProvider, ContentEntry contentEntry) {
    myFile = file;
    myContentFolderTypeProvider = contentFolderTypeProvider;
    myContentEntry = contentEntry;
  }

  @Nonnull
  @Override
  public ContentFolderTypeProvider getType() {
    return myContentFolderTypeProvider;
  }

  @Nullable
  @Override
  public VirtualFile getFile() {
    return myFile.getFile();
  }

  @Nonnull
  @Override
  public ContentEntry getContentEntry() {
    return myContentEntry;
  }

  @Nonnull
  @Override
  public String getUrl() {
    return myFile.getUrl();
  }

  @Nonnull
  @Override
  public Map<Key, Object> getProperties() {
    return Collections.emptyMap();
  }

  @Nullable
  @Override
  public <T> T getPropertyValue(@Nonnull Key<T> key) {
    return null;
  }

  @Override
  public <T> void setPropertyValue(@Nonnull Key<T> key, @javax.annotation.Nullable T value) {

  }

  @Override
  public boolean isSynthetic() {
    return true;
  }
}
