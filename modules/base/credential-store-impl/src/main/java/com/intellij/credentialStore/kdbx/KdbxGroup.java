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
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashMap;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * from kotlin
 */
public class KdbxGroup {
  private Element element;
  private KeePassDatabase database;
  @Nullable
  private volatile KdbxGroup parent;

  private final Map<String, KdbxGroup> groups = new THashMap<>();

  private final NotNullLazyValue<List<KdbxEntry>> entries = NotNullLazyValue.createValue(
          () -> ContainerUtil.createLockFreeCopyOnWriteList(element.getChildren(KdbxDbElementNames.entry).stream().map(it -> new KdbxEntry(it, database, this)).collect(Collectors.toList())));

  public KdbxGroup(Element element, KeePassDatabase database, @Nullable KdbxGroup parent) {
    this.element = element;
    this.database = database;
    this.parent = parent;
  }

  public synchronized void setName(String value) {
    Element namedElement = JDOMUtil.getOrCreate(element, KdbxDbElementNames.name);
    if(Comparing.equal(namedElement.getText(), value)) {
      return;
    }

    namedElement.setText(value);
    database.setDirty(true);
  }

  private KdbxGroup createGroup(String name) {
    KdbxGroup result = KdbxGroupKt.createGroup(database, this);
    result.setName(name);

    if(result == database.getRootGroup())  {
      throw new IllegalStateException("Cannot set root group as child of another group")
    }
  }

  @Nullable
  public KdbxEntry getEntry(Predicate<KdbxEntry> matcher) {
    return entries.getValue().stream().filter(matcher).findFirst().orElse(null);
  }

  @Nullable
  public KdbxEntry getEntry(String title, String userName) {
    return getEntry(entry -> Comparing.equal(entry.getTitle(), title) && (Comparing.equal(entry.getUserName(), userName) || userName == null));
  }

  @Nullable
  public synchronized KdbxEntry removeEntry(String title, String userName)  {
    KdbxEntry entry = getEntry(title, userName);
    if(entry != null) {
      removeEntry(entry);
    }
    return entry;
  }

  @Nonnull
  public synchronized KdbxEntry getOrCreateEntry(String title, String userName) {
    KdbxEntry entry = getEntry(title, userName);
    if(entry == null) {
      entry = database.createEntry(title);
      entry.setUserName(userName);
      addEntry(entry);
    }
    return entry;
  }

  @Nonnull
  public synchronized KdbxEntry addEntry(@Nonnull KdbxEntry entry) {
    KdbxGroup group = entry.getGroup();
    if(group != null) {
      group.removeEntry(entry);
    }
    entries.getValue().add(entry);
    entry.setGroup(group);
    database.setDirty(true);
    element.addContent(entry.getEntryElement());
    return entry;
  }

  @Nonnull
  private KdbxEntry removeEntry(@Nonnull KdbxEntry entry) {
    if(entries.getValue().remove(entry)) {
      entry.setGroup(null);
      element.getContent().remove(entry.getEntryElement());
      database.setDirty(true);
    }
    return entry;
  }
}
