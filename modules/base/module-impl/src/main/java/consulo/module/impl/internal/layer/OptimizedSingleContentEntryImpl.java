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
  
  private final VirtualFilePointer myRoot;

  public OptimizedSingleContentEntryImpl(VirtualFile file, ModuleRootLayerImpl m) {
    this(file.getUrl(), m);
  }

  public OptimizedSingleContentEntryImpl(String url, ModuleRootLayerImpl m) {
    super(m);
    myRoot = VirtualFilePointerManager.getInstance().create(url, this, null);
  }

  public OptimizedSingleContentEntryImpl(Element e, ModuleRootLayerImpl m) {
    this(ContentEntryImpl.getUrlFrom(e), m);
  }

  public void writeExternal(Element element) {
    assert !isDisposed();
    element.setAttribute(ContentEntryImpl.URL_ATTRIBUTE, myRoot.getUrl());
  }

  @Override
  public VirtualFile getFile() {
    return myRoot.getFile();
  }

  @Override
  
  public String getUrl() {
    return myRoot.getUrl();
  }
  
  
  @Override
  public ContentFolder[] getFolders(Predicate<ContentFolderTypeProvider> predicate) {
    return ContentFolder.EMPTY_ARRAY;
  }

  
  @Override
  public VirtualFile[] getFolderFiles(Predicate<ContentFolderTypeProvider> predicate) {
    return VirtualFile.EMPTY_ARRAY;
  }

  
  @Override
  public String[] getFolderUrls(Predicate<ContentFolderTypeProvider> predicate) {
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }

  
  @Override
  public Collection<ContentFolder> getContentFolders() {
    return Collections.emptyList();
  }

  
  @Override
  public ContentFolder addFolder(VirtualFile file, ContentFolderTypeProvider contentFolderType) {
    throw new UnsupportedOperationException();
  }

  
  @Override
  public ContentFolder addFolder(String url, ContentFolderTypeProvider contentFolderType) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeFolder(ContentFolder contentFolder) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ContentEntryEx cloneEntry(ModuleRootLayerImpl layer) {
    assert !isDisposed();
    return new OptimizedSingleContentEntryImpl(myRoot.getUrl(), layer);
  }
}
