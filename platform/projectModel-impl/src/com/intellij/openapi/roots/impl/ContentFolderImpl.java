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
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import com.intellij.util.containers.HashMap;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.roots.ContentFolderPropertyProvider;
import org.mustbe.consulo.roots.ContentFolderTypeProvider;
import org.mustbe.consulo.roots.impl.UnknownContentFolderTypeProvider;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author dsl
 */
public class ContentFolderImpl extends RootModelComponentBase
  implements ContentFolder, ClonableContentFolder, Comparable<ContentFolderImpl> {
  private static Map<Key, ContentFolderPropertyProvider> ourPropertiesToKeyCache = new HashMap<Key, ContentFolderPropertyProvider>();

  static {
    for (ContentFolderPropertyProvider propertyProvider : ContentFolderPropertyProvider.EP_NAME.getExtensions()) {
      ourPropertiesToKeyCache.put(propertyProvider.getKey(), propertyProvider);
    }
  }

  @NonNls
  public static final String URL_ATTRIBUTE = "url";
  @NonNls
  public static final String TYPE_ATTRIBUTE = "type";
  @NonNls
  public static final String ELEMENT_NAME = "content-folder";

  private final VirtualFilePointer myFilePointer;
  protected final ContentEntryImpl myContentEntry;
  private final ContentFolderTypeProvider myContentFolderType;
  private Map<Key, Object> myProperties;

  ContentFolderImpl(@NotNull VirtualFile file,
                    @NotNull ContentFolderTypeProvider contentFolderType,
                    @NotNull ContentEntryImpl contentEntry) {
    super(contentEntry.getRootModel());
    myContentEntry = contentEntry;
    myContentFolderType = contentFolderType;
    myFilePointer = VirtualFilePointerManager.getInstance().create(file, this, null);
  }

  ContentFolderImpl(@NotNull String url,
                    @NotNull ContentFolderTypeProvider contentFolderType,
                    @Nullable Map<Key, Object> map,
                    @NotNull ContentEntryImpl contentEntry) {
    super(contentEntry.getRootModel());
    myContentEntry = contentEntry;
    myContentFolderType = contentFolderType;
    myProperties = map == null ? null : new HashMap<Key, Object>(map);
    myFilePointer = VirtualFilePointerManager.getInstance().create(url, this, null);
  }

  protected ContentFolderImpl(@NotNull ContentFolderImpl that, @NotNull ContentEntryImpl contentEntry) {
    this(that.myFilePointer, that.myProperties, that.getType(), contentEntry);
  }

  ContentFolderImpl(@NotNull Element element, @NotNull ContentEntryImpl contentEntry) throws InvalidDataException {
    this(getUrlFrom(element), getType(element), getProperties(element), contentEntry);
  }

  protected ContentFolderImpl(@NotNull VirtualFilePointer filePointer,
                              @Nullable Map<Key, Object> properties,
                              @NotNull ContentFolderTypeProvider contentFolderType,
                              @NotNull ContentEntryImpl contentEntry) {
    super(contentEntry.getRootModel());
    myContentEntry = contentEntry;
    myContentFolderType = contentFolderType;
    myProperties = properties == null ? null : new HashMap<Key, Object>(properties);
    myFilePointer = VirtualFilePointerManager.getInstance().duplicate(filePointer, this, null);
  }

  private static String getUrlFrom(Element element) throws InvalidDataException {
    String url = element.getAttributeValue(URL_ATTRIBUTE);
    if (url == null) {
      throw new InvalidDataException();
    }
    return url;
  }

  @Nullable
  private static Map<Key, Object> getProperties(Element element) throws InvalidDataException {
    List<Element> elementChildren = element.getChildren("property");
    if (elementChildren.isEmpty()) {
      return null;
    }
    Map<Key, Object> map = new HashMap<Key, Object>();

    for (Element elementChild : elementChildren) {
      String key = elementChild.getAttributeValue("key");
      String value = elementChild.getAttributeValue("value");

      Key<?> keyAsKey = Key.findKeyByName(key);

      ContentFolderPropertyProvider propertyProvider = keyAsKey != null ? ourPropertiesToKeyCache.get(keyAsKey) : null;
      if(propertyProvider != null) {
        Object b = propertyProvider.fromString(value);
        map.put(keyAsKey, b);
      }
      else {
        map.put(keyAsKey, value);
      }
    }
    return map;
  }

  private static ContentFolderTypeProvider getType(Element element) throws InvalidDataException {
    String type = element.getAttributeValue(TYPE_ATTRIBUTE);
    if (type == null) {
      throw new InvalidDataException();
    }

    for (ContentFolderTypeProvider contentFolderTypeProvider : ContentFolderTypeProvider.EP_NAME.getExtensions()) {
      if (Comparing.equal(contentFolderTypeProvider.getId(), type)) {
        return contentFolderTypeProvider;
      }
    }

    return new UnknownContentFolderTypeProvider(type);
  }

  @Override
  public VirtualFile getFile() {
    if (!myFilePointer.isValid()) {
      return null;
    }
    return myFilePointer.getFile();
  }

  @Override
  @NotNull
  public ContentEntry getContentEntry() {
    return myContentEntry;
  }

  @SuppressWarnings("unchecked")
  protected void writeExternal(Element element) {
    element.setAttribute(TYPE_ATTRIBUTE, myContentFolderType.getId());
    element.setAttribute(URL_ATTRIBUTE, myFilePointer.getUrl());

    if(myProperties == null) {
      return;
    }
    for (Map.Entry<Key, Object> entry : myProperties.entrySet()) {
      Key key = entry.getKey();
      Object value = entry.getValue();

      Element child = new Element("property");
      child.setAttribute("key", key.toString());
      ContentFolderPropertyProvider propertiesProvider = ourPropertiesToKeyCache.get(key);
      if(propertiesProvider != null) {
        child.setAttribute("value", propertiesProvider.toString(value));
      }
      else {
        child.setAttribute("value", (String)value);
      }
      element.addContent(child);
    }
  }

  @Override
  @NotNull
  public String getUrl() {
    return myFilePointer.getUrl();
  }

  @NotNull
  @Override
  public Map<Key, Object> getProperties() {
    return myProperties == null ? Collections.<Key, Object>emptyMap() : myProperties;
  }

  @Nullable
  @Override
  public <T> T getPropertyValue(@NotNull Key<T> key) {
    if (myProperties == null) {
      return null;
    }
    return key.get(myProperties);
  }

  @Override
  public <T> void setPropertyValue(@NotNull Key<T> key, @Nullable T value) {
    if (value == null && myProperties == null) {
      return;
    }

    if (value == null) {
      myProperties.remove(key);
    }
    else {
      if (myProperties == null) {
        myProperties = new HashMap<Key, Object>();
      }

      key.set(myProperties, value);
    }
  }

  @Override
  public boolean isSynthetic() {
    return false;
  }

  @NotNull
  @Override
  public ContentFolderTypeProvider getType() {
    return myContentFolderType;
  }

  @Override
  public int compareTo(@NotNull ContentFolderImpl folder) {
    int typeCompare = getType().getId().compareToIgnoreCase(folder.getType().getId());
    if (typeCompare != 0) {
      return typeCompare;
    }

    int diff = (myProperties == null ? 0 : myProperties.hashCode()) - (folder.myProperties == null ? 0 : folder.myProperties.hashCode());
    if (diff != 0) {
      return diff > 0 ? 1 : -1;
    }
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

  @Override
  public ContentFolder cloneFolder(ContentEntry contentEntry) {
    assert !((ContentEntryImpl)contentEntry).isDisposed() : "target entry already disposed: " + contentEntry;
    assert !isDisposed() : "Already disposed: " + this;
    return new ContentFolderImpl(this, (ContentEntryImpl)contentEntry);
  }
}
