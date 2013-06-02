/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.intellij.openapi.roots.impl;

import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.roots.ContentFolderType;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.serialization.module.JpsModuleRootModelSerializer;

/**
 * @author dsl
 */
public class ContentFolderImpl extends RootModelComponentBase implements ContentFolder, ClonableContentFolder, Comparable<ContentFolderImpl> {
  @NonNls
  public static final String URL_ATTRIBUTE = JpsModuleRootModelSerializer.URL_ATTRIBUTE;
  @NonNls
  public static final String TYPE_ATTRIBUTE = JpsModuleRootModelSerializer.TYPE_ATTRIBUTE;
  @NonNls
  public static final String ELEMENT_NAME = "content-folder";

  private boolean myIsSynthetic;
  private final VirtualFilePointer myFilePointer;
  protected final ContentEntryImpl myContentEntry;
  private final ContentFolderType myContentFolderType;

  ContentFolderImpl(@NotNull VirtualFile file, @NotNull ContentFolderType contentFolderType, @NotNull ContentEntryImpl contentEntry) {
    super(contentEntry.getRootModel());
    myContentEntry = contentEntry;
    myContentFolderType = contentFolderType;
    myFilePointer = VirtualFilePointerManager.getInstance().create(file, this, null);
  }

  ContentFolderImpl(@NotNull String url, @NotNull ContentFolderType contentFolderType, @NotNull ContentEntryImpl contentEntry) {
    super(contentEntry.getRootModel());
    myContentEntry = contentEntry;
    myContentFolderType = contentFolderType;
    myFilePointer = VirtualFilePointerManager.getInstance().create(url, this, null);
  }

  protected ContentFolderImpl(@NotNull ContentFolderImpl that,
                              @NotNull ContentEntryImpl contentEntry) {
    this(that.myFilePointer, that.getType(), contentEntry);
  }

  ContentFolderImpl(@NotNull Element element, @NotNull ContentEntryImpl contentEntry) throws InvalidDataException {
    this(getUrlFrom(element), getType(element), contentEntry);
  }

  protected ContentFolderImpl(@NotNull VirtualFilePointer filePointer,
                              @NotNull ContentFolderType contentFolderType,
                              @NotNull ContentEntryImpl contentEntry) {
    super(contentEntry.getRootModel());
    myContentEntry = contentEntry;
    myContentFolderType = contentFolderType;
    myFilePointer = VirtualFilePointerManager.getInstance().duplicate(filePointer, this, null);
  }

  private static String getUrlFrom(Element element) throws InvalidDataException {
    String url = element.getAttributeValue(URL_ATTRIBUTE);
    if (url == null) {
      throw new InvalidDataException();
    }
    return url;
  }

  private static ContentFolderType getType(Element element) throws InvalidDataException {
    String type = element.getAttributeValue(TYPE_ATTRIBUTE);
    if (type == null) {
      throw new InvalidDataException();
    }
    return ContentFolderType.valueOf(type);
  }

  @Override
  public VirtualFile getFile() {
    if (!myFilePointer.isValid()) {
      return null;
    }
    final VirtualFile file = myFilePointer.getFile();
    return file == null || !file.isDirectory() ? null : file;
  }

  @Override
  @NotNull
  public ContentEntry getContentEntry() {
    return myContentEntry;
  }

  protected void writeExternal(Element element) {
    element.setAttribute(TYPE_ATTRIBUTE, myContentFolderType.name());
    element.setAttribute(URL_ATTRIBUTE, myFilePointer.getUrl());
  }

  @Override
  @NotNull
  public String getUrl() {
    return myFilePointer.getUrl();
  }

  @Override
  public boolean isSynthetic() {
    return myIsSynthetic;
  }

  @NotNull
  @Override
  public ContentFolderType getType() {
    return myContentFolderType;
  }

  @Override
  public int compareTo(ContentFolderImpl folder) {
    return getUrl().compareTo(folder.getUrl());
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ContentFolderImpl)) return false;
    return compareTo((ContentFolderImpl)obj) == 0;
  }

  @Override
  public int hashCode() {
    return getUrl().hashCode();
  }

  @Nullable
  @Override
  public String toString() {
    return myFilePointer == null ? null : getUrl();
  }

  public void setSynthetic() {
    assert !myIsSynthetic;
    myIsSynthetic = true;
  }

  @Override
  public ContentFolder cloneFolder(ContentEntry contentEntry) {
    assert !((ContentEntryImpl)contentEntry).isDisposed() : "target entry already disposed: " + contentEntry;
    assert !isDisposed() : "Already disposed: " + this;
    return new ContentFolderImpl(this, (ContentEntryImpl)contentEntry);
  }
}
