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
import consulo.util.dataholder.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import consulo.roots.ContentFolderPropertyProvider;
import consulo.roots.ContentFolderTypeProvider;
import consulo.roots.impl.UnknownContentFolderTypeProvider;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dsl
 */
public class ContentFolderImpl extends BaseModuleRootLayerChild
  implements ContentFolder, ClonableContentFolder, Comparable<ContentFolderImpl> {

  @NonNls
  public static final String URL_ATTRIBUTE = "url";
  @NonNls
  public static final String TYPE_ATTRIBUTE = "type";
  @NonNls
  public static final String ELEMENT_NAME = "content-folder";

  private final VirtualFilePointer myFilePointer;
  protected final ContentEntryImpl myContentEntry;
  private final ContentFolderTypeProvider myContentFolderType;
  private Map<String, Object> myProperties;

  private Map<Key, Object> myPropertiesByKeyCache;

  ContentFolderImpl(@Nonnull VirtualFile file,
                    @Nonnull ContentFolderTypeProvider contentFolderType,
                    @Nonnull ContentEntryImpl contentEntry) {
    super(contentEntry.getModuleRootLayer());
    myContentEntry = contentEntry;
    myContentFolderType = contentFolderType;
    myFilePointer = VirtualFilePointerManager.getInstance().create(file, this, null);
  }

  ContentFolderImpl(@Nonnull String url,
                    @Nonnull ContentFolderTypeProvider contentFolderType,
                    @Nullable Map<String, Object> map,
                    @Nonnull ContentEntryImpl contentEntry) {
    super(contentEntry.getModuleRootLayer());
    myContentEntry = contentEntry;
    myContentFolderType = contentFolderType;
    myProperties = map == null ? null : new HashMap<String, Object>(map);
    myFilePointer = VirtualFilePointerManager.getInstance().create(url, this, null);
  }

  protected ContentFolderImpl(@Nonnull ContentFolderImpl that, @Nonnull ContentEntryImpl contentEntry) {
    this(that.myFilePointer, that.myProperties, that.getType(), contentEntry);
  }

  ContentFolderImpl(@Nonnull Element element, @Nonnull ContentEntryImpl contentEntry) throws InvalidDataException {
    this(getUrlFrom(element), getType(element), readProperties(element), contentEntry);
  }

  protected ContentFolderImpl(@Nonnull VirtualFilePointer filePointer,
                              @Nullable Map<String, Object> properties,
                              @Nonnull ContentFolderTypeProvider contentFolderType,
                              @Nonnull ContentEntryImpl contentEntry) {
    super(contentEntry.getModuleRootLayer());
    myContentEntry = contentEntry;
    myContentFolderType = contentFolderType;
    myProperties = properties;
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
  private static Map<String, Object> readProperties(Element element) throws InvalidDataException {
    List<Element> elementChildren = element.getChildren("property");
    if (elementChildren.isEmpty()) {
      return null;
    }
    Map<String, Object> map = new HashMap<String, Object>();

    for (Element elementChild : elementChildren) {
      String key = elementChild.getAttributeValue("key");
      String value = elementChild.getAttributeValue("value");

      ContentFolderPropertyProvider propertyProvider = ContentFolderPropertyProvider.findProvider(key);
      if(propertyProvider != null) {
        Object b = propertyProvider.fromString(value);
        map.put(key, b);
      }
      else {
        map.put(key, value);
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
  @Nonnull
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
    for (Map.Entry<String, Object> entry : myProperties.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      Element child = new Element("property");
      child.setAttribute("key", key);

      ContentFolderPropertyProvider propertiesProvider = ContentFolderPropertyProvider.findProvider(key);
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
  @Nonnull
  public String getUrl() {
    return myFilePointer.getUrl();
  }

  @Nonnull
  @Override
  public Map<Key, Object> getProperties() {
    initPropertiesByKeyCache();
    return myPropertiesByKeyCache;
  }

  private void initPropertiesByKeyCache() {
    if (myPropertiesByKeyCache != null) {
      return;
    }

    if(myProperties == null) {
      myPropertiesByKeyCache = Collections.emptyMap();
    }
    else {
      myPropertiesByKeyCache = new HashMap<Key, Object>(myProperties.size());
      for (Map.Entry<String, Object> entry : myProperties.entrySet()) {
        ContentFolderPropertyProvider<?> provider = ContentFolderPropertyProvider.findProvider(entry.getKey());
        if(provider == null) {
          continue;
        }
        myPropertiesByKeyCache.put(provider.getKey(), entry.getValue());
      }
    }
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public <T> T getPropertyValue(@Nonnull Key<T> key) {
    if (myProperties == null) {
      return null;
    }
    return (T)myProperties.get(key.toString());
  }

  @Override
  public <T> void setPropertyValue(@Nonnull Key<T> key, @Nullable T value) {
    myPropertiesByKeyCache = null;

    if (value == null && myProperties == null) {
      return;
    }

    if (value == null) {
      myProperties.remove(key.toString());
    }
    else {
      if (myProperties == null) {
        myProperties = new HashMap<String, Object>();
      }

      myProperties.put(key.toString(), value);
    }
  }

  @Override
  public boolean isSynthetic() {
    return false;
  }

  @Nonnull
  @Override
  public ContentFolderTypeProvider getType() {
    return myContentFolderType;
  }

  @Override
  public int compareTo(@Nonnull ContentFolderImpl folder) {
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
