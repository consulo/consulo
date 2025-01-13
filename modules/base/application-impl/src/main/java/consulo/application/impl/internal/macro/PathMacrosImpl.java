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
package consulo.application.impl.internal.macro;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.macro.PathMacros;
import consulo.component.macro.ExpandMacroToPathMap;
import consulo.component.macro.PathMacroProtocolProvider;
import consulo.component.macro.PathMacroUtil;
import consulo.component.macro.ReplacePathToMacroMap;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.logging.Logger;
import consulo.util.collection.Lists;
import consulo.util.xml.serializer.InvalidDataException;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author dsl
 */
@State(name = "PathMacrosImpl", storages = @Storage(value = PathMacrosImpl.STORE_FILE, roamingType = RoamingType.DISABLED))
@Singleton
@ServiceImpl(profiles = ComponentProfiles.PRODUCTION)
public class PathMacrosImpl implements PathMacros, PersistentStateComponent<Element> {
  private static final Logger LOG = Logger.getInstance(PathMacrosImpl.class);
  public static final String STORE_FILE = "path.macros.xml";

  private final Map<String, String> myLegacyMacros = new HashMap<>();
  private final Map<String, String> myMacros = new HashMap<>();
  private int myModificationStamp = 0;
  private final ReentrantReadWriteLock myLock = new ReentrantReadWriteLock();
  private final List<String> myIgnoredMacros = Lists.newLockFreeCopyOnWriteList();

  public static final String MACRO_ELEMENT = "macro";
  public static final String NAME_ATTR = "name";
  public static final String VALUE_ATTR = "value";

  public static final String IGNORED_MACRO_ELEMENT = "ignoredMacro";

  // predefined macros
  public static final String APPLICATION_HOME_MACRO_NAME = PathMacroUtil.APPLICATION_HOME_DIR;
  public static final String PROJECT_DIR_MACRO_NAME = PathMacroUtil.PROJECT_DIR_MACRO_NAME;
  public static final String MODULE_DIR_MACRO_NAME = PathMacroUtil.MODULE_DIR_MACRO_NAME;
  public static final String USER_HOME_MACRO_NAME = PathMacroUtil.USER_HOME_MACRO_NAME;

  private static final Set<String> SYSTEM_MACROS = new HashSet<>();

  static {
    SYSTEM_MACROS.add(APPLICATION_HOME_MACRO_NAME);
    SYSTEM_MACROS.add(PROJECT_DIR_MACRO_NAME);
    SYSTEM_MACROS.add(MODULE_DIR_MACRO_NAME);
    SYSTEM_MACROS.add(USER_HOME_MACRO_NAME);
  }

  private final Provider<PathMacroProtocolProvider> myPathMacroProtocolProviderProvider;

  @Inject
  public PathMacrosImpl(Provider<PathMacroProtocolProvider> pathMacroProtocolProviderProvider) {
    myPathMacroProtocolProviderProvider = pathMacroProtocolProviderProvider;
  }

  @Override
  public Set<String> getUserMacroNames() {
    myLock.readLock().lock();
    try {
      return new HashSet<>(myMacros.keySet()); // keyset should not escape the lock
    }
    finally {
      myLock.readLock().unlock();
    }
  }

  @Override
  public Set<String> getSystemMacroNames() {
    return SYSTEM_MACROS;
  }

  @Override
  public Collection<String> getIgnoredMacroNames() {
    return myIgnoredMacros;
  }

  @Override
  public void setIgnoredMacroNames(@Nonnull final Collection<String> names) {
    myIgnoredMacros.clear();
    myIgnoredMacros.addAll(names);
  }

  @Override
  public void addIgnoredMacro(@Nonnull String name) {
    if (!myIgnoredMacros.contains(name)) myIgnoredMacros.add(name);
  }

  public int getModificationStamp() {
    myLock.readLock().lock();
    try {
      return myModificationStamp;
    }
    finally {
      myLock.readLock().unlock();
    }
  }

  @Override
  public boolean isIgnoredMacroName(@Nonnull String macro) {
    return myIgnoredMacros.contains(macro);
  }

  @Override
  public Set<String> getAllMacroNames() {
    final Set<String> userMacroNames = getUserMacroNames();
    final Set<String> systemMacroNames = getSystemMacroNames();
    final Set<String> allNames = new HashSet<>(userMacroNames.size() + systemMacroNames.size());
    allNames.addAll(systemMacroNames);
    allNames.addAll(userMacroNames);
    return allNames;
  }

  @Override
  public String getValue(String name) {
    try {
      myLock.readLock().lock();
      return myMacros.get(name);
    }
    finally {
      myLock.readLock().unlock();
    }
  }

  @Override
  public void removeAllMacros() {
    try {
      myLock.writeLock().lock();
      myMacros.clear();
    }
    finally {
      myModificationStamp++;
      myLock.writeLock().unlock();
    }
  }

  @Override
  public Collection<String> getLegacyMacroNames() {
    try {
      myLock.readLock().lock();
      return new HashSet<>(myLegacyMacros.keySet()); // keyset should not escape the lock
    }
    finally {
      myLock.readLock().unlock();
    }
  }

  @Override
  public void setMacro(@Nonnull String name, @Nonnull String value) {
    if (value.trim().isEmpty()) return;
    try {
      myLock.writeLock().lock();
      myMacros.put(name, value);
    }
    finally {
      myModificationStamp++;
      myLock.writeLock().unlock();
    }
  }

  @Override
  public void addLegacyMacro(@Nonnull String name, @Nonnull String value) {
    try {
      myLock.writeLock().lock();
      myLegacyMacros.put(name, value);
      myMacros.remove(name);
    }
    finally {
      myModificationStamp++;
      myLock.writeLock().unlock();
    }
  }

  @Override
  public void removeMacro(String name) {
    try {
      myLock.writeLock().lock();
      final String value = myMacros.remove(name);
      LOG.assertTrue(value != null);
    }
    finally {
      myModificationStamp++;
      myLock.writeLock().unlock();
    }
  }

  @Override
  public void loadState(Element state) {
    try {
      myLock.writeLock().lock();

      final List<Element> children = state.getChildren(MACRO_ELEMENT);
      for (Element aChildren : children) {
        final String name = aChildren.getAttributeValue(NAME_ATTR);
        String value = aChildren.getAttributeValue(VALUE_ATTR);
        if (name == null || value == null) {
          throw new InvalidDataException();
        }

        if (SYSTEM_MACROS.contains(name)) {
          continue;
        }

        if (value.length() > 1 && value.charAt(value.length() - 1) == '/') {
          value = value.substring(0, value.length() - 1);
        }

        myMacros.put(name, value);
      }

      final List<Element> ignoredChildren = state.getChildren(IGNORED_MACRO_ELEMENT);
      for (final Element child : ignoredChildren) {
        final String ignoredName = child.getAttributeValue(NAME_ATTR);
        if (ignoredName != null && !ignoredName.isEmpty() && !myIgnoredMacros.contains(ignoredName)) {
          myIgnoredMacros.add(ignoredName);
        }
      }
    }
    finally {
      myModificationStamp++;
      myLock.writeLock().unlock();
    }
  }

  @Override
  public Element getState() {
    Element state = new Element("state");
    try {
      myLock.writeLock().lock();

      final Set<Map.Entry<String, String>> entries = myMacros.entrySet();
      for (Map.Entry<String, String> entry : entries) {
        final String value = entry.getValue();
        if (value != null && !value.trim().isEmpty()) {
          final Element macro = new Element(MACRO_ELEMENT);
          macro.setAttribute(NAME_ATTR, entry.getKey());
          macro.setAttribute(VALUE_ATTR, value);
          state.addContent(macro);
        }
      }

      for (final String macro : myIgnoredMacros) {
        final Element macroElement = new Element(IGNORED_MACRO_ELEMENT);
        macroElement.setAttribute(NAME_ATTR, macro);
        state.addContent(macroElement);
      }
    }
    finally {
      myLock.writeLock().unlock();
    }
    return state;
  }

  @Override
  public void addMacroReplacements(ReplacePathToMacroMap result) {
    for (final String name : getUserMacroNames()) {
      final String value = getValue(name);
      if (value != null && !value.trim().isEmpty()) result.addMacroReplacement(myPathMacroProtocolProviderProvider.get(), value, name);
    }
  }


  @Override
  public void addMacroExpands(ExpandMacroToPathMap result) {
    for (final String name : getUserMacroNames()) {
      final String value = getValue(name);
      if (value != null && !value.trim().isEmpty()) result.addMacroExpand(name, value);
    }

    myLock.readLock().lock();
    try {
      for (Map.Entry<String, String> entry : myLegacyMacros.entrySet()) {
        result.addMacroExpand(entry.getKey(), entry.getValue());
      }
    }
    finally {
      myLock.readLock().unlock();
    }
  }
}
