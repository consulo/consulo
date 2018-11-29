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
package com.intellij.credentialStore.kdbx;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jdom.Content;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

/**
 *  from kotlin
 */
public class KdbxEntry {
  private final Element entryElement;
  private final KeePassDatabase database;
  @Nullable
  private volatile KdbxGroup group;

  public KdbxEntry(Element entryElement, KeePassDatabase database, @Nullable KdbxGroup group) {
    this.entryElement = entryElement;
    this.database = database;
    this.group = group;
  }

  @Nonnull
  public Element getEntryElement() {
    return entryElement;
  }

  @Nullable
  public KdbxGroup getGroup() {
    return group;
  }

  public void setGroup(@Nullable KdbxGroup group) {
    this.group = group;
  }

  public String getTitle() {
    return getProperty(KdbxEntryElementNames.title);
  }

  public void setTitle(String value) {
    setProperty(entryElement, value, KdbxEntryElementNames.title);
  }

  public String getUserName() {
    return getProperty(KdbxEntryElementNames.userName);
  }

  public void setUserName(String value) {
    setProperty(entryElement, value, KdbxEntryElementNames.userName);
  }

  @Nullable
  public synchronized SecureString getPassword() {
    Element passwordElement = getPropertyElement(entryElement, KdbxEntryElementNames.password);

    Element valueElement = passwordElement == null ? null : passwordElement.getChild(KdbxEntryElementNames.value);

    if (valueElement == null) {
      return null;
    }

    Content value = ContainerUtil.getFirstItem(valueElement.getContent());
    if (value == null) {
      return null;
    }

    if (value instanceof SecureString) {
      return (SecureString)value;
    }

    // if value was not originally protected, protect it
    valueElement.setAttribute(KdbxAttributeNames._protected, "True");
    UnsavedProtectedValue result = new UnsavedProtectedValue(database.protectValue(value.getValue()));
    valueElement.setContent(result);
    return result;
  }

  public synchronized void setPassword(SecureString value) {
    if (value == null) {
      Iterator<Element> iterator = entryElement.getChildren(KdbxEntryElementNames.string).iterator();
      while (iterator.hasNext()) {
        Element element = iterator.next();

        if (Comparing.equal(element.getChildText(KdbxEntryElementNames.key), KdbxEntryElementNames.password)) {
          iterator.remove();
          touch();
        }
      }

      return;
    }

    Element valueElement = JDOMUtil.getOrCreate(getOrCreatePropertyElement(KdbxEntryElementNames.password), KdbxEntryElementNames.value);
    valueElement.setAttribute(KdbxAttributeNames._protected, "True");

    Content oldValue = ContainerUtil.getFirstItem(valueElement.getContent());
    if(oldValue == value) {
      return;
    }

    valueElement.setContent(new UnsavedProtectedValue((StringProtectedByStreamCipher)value));
    touch();
  }

  @Nonnull
  private Element getOrCreatePropertyElement(String name) {
    Element propertyElement = getPropertyElement(entryElement, name);
    if (propertyElement != null) {
      return propertyElement;
    }
    return createPropertyElement(entryElement, name);
  }

  private synchronized String getProperty(String propertyName) {
    Element propertyElement = getPropertyElement(entryElement, propertyName);

    Element valueElement = propertyElement == null ? null : propertyElement.getChild(KdbxEntryElementNames.value);

    if (valueElement == null) {
      return null;
    }

    if (ProtectedValueKt.isValueProtected(valueElement)) {
      throw new UnsupportedOperationException(propertyName + " protection is not supported");
    }

    return StringUtil.nullize(valueElement.getText());
  }

  @Nullable
  private synchronized Element setProperty(Element entryElement, String value, String propertyName) {
    String normalizedValue = StringUtil.nullize(value);

    Element propertyElement = getPropertyElement(entryElement, propertyName);
    if (propertyElement == null) {
      if (normalizedValue == null) {
        return null;
      }

      propertyElement = createPropertyElement(entryElement, propertyName);
    }

    Element valueElement = JDOMUtil.getOrCreate(propertyElement, KdbxEntryElementNames.value);
    if (Comparing.equal(StringUtil.nullize(valueElement.getText()), normalizedValue)) {
      return null;
    }

    valueElement.setText(value);

    if (entryElement == this.entryElement) {
      touch();
    }

    return valueElement;
  }

  private synchronized void touch() {
    Element times = JDOMUtil.getOrCreate(entryElement, "Times");

    Element lastModificationTime = JDOMUtil.getOrCreate(times, "LastModificationTime");
    lastModificationTime.setText(KeePassDatabaseKt.formattedNow());

    database.setDirty(true);
  }

  @Nullable
  private static Element getPropertyElement(Element element, String propertyName) {
    List<Element> children = element.getChildren(KdbxEntryElementNames.string);
    return ContainerUtil.find(children, child -> propertyName.equals(child.getChildText(KdbxEntryElementNames.key)));
  }

  @Nonnull
  private static Element createPropertyElement(Element parentElement, String propertyName) {
    Element propertyElement = new Element(KdbxEntryElementNames.string);
    propertyElement.addContent(new Element(KdbxEntryElementNames.key).setText(propertyName));
    parentElement.addContent(propertyElement);
    return propertyElement;
  }
}
