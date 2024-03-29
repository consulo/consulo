/*
 * Copyright 2013-2020 consulo.io
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
package consulo.module.impl.internal.layer;

import consulo.content.ContentFolderTypeProvider;
import consulo.module.content.layer.ContentFolder;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;
import consulo.virtualFileSystem.pointer.VirtualFilePointerManager;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2020-08-29
 *
 * Optimized version of {@link ContentEntryImpl} without supporting data inside
 */
public class OptimizedSingleContentEntryImpl extends BaseModuleRootLayerChild implements ContentEntryEx {
  @Nonnull
  private final VirtualFilePointer myRoot;

  public OptimizedSingleContentEntryImpl(@Nonnull VirtualFile file, @Nonnull ModuleRootLayerImpl m) {
    this(file.getUrl(), m);
  }

  public OptimizedSingleContentEntryImpl(@Nonnull String url, @Nonnull ModuleRootLayerImpl m) {
    super(m);
    myRoot = VirtualFilePointerManager.getInstance().create(url, this, null);
  }

  public OptimizedSingleContentEntryImpl(@Nonnull Element e, @Nonnull ModuleRootLayerImpl m) {
    this(ContentEntryImpl.getUrlFrom(e), m);
  }

  public void writeExternal(@Nonnull Element element) {
    assert !isDisposed();
    element.setAttribute(ContentEntryImpl.URL_ATTRIBUTE, myRoot.getUrl());
  }

  @Override
  public VirtualFile getFile() {
    return myRoot.getFile();
  }

  @Override
  @Nonnull
  public String getUrl() {
    return myRoot.getUrl();
  }
  
  @Nonnull
  @Override
  public ContentFolder[] getFolders(@Nonnull Predicate<ContentFolderTypeProvider> predicate) {
    return ContentFolder.EMPTY_ARRAY;
  }

  @Nonnull
  @Override
  public VirtualFile[] getFolderFiles(@Nonnull Predicate<ContentFolderTypeProvider> predicate) {
    return VirtualFile.EMPTY_ARRAY;
  }

  @Nonnull
  @Override
  public String[] getFolderUrls(@Nonnull Predicate<ContentFolderTypeProvider> predicate) {
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }

  @Nonnull
  @Override
  public Collection<ContentFolder> getContentFolders() {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public ContentFolder addFolder(@Nonnull VirtualFile file, @Nonnull ContentFolderTypeProvider contentFolderType) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public ContentFolder addFolder(@Nonnull String url, @Nonnull ContentFolderTypeProvider contentFolderType) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeFolder(@Nonnull ContentFolder contentFolder) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ContentEntryEx cloneEntry(ModuleRootLayerImpl layer) {
    assert !isDisposed();
    return new OptimizedSingleContentEntryImpl(myRoot.getUrl(), layer);
  }
}
