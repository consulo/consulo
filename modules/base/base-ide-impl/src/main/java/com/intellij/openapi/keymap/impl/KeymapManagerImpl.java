/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.keymap.impl;

import com.intellij.openapi.components.*;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManagerListener;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.options.BaseSchemeProcessor;
import com.intellij.openapi.options.SchemesManager;
import com.intellij.openapi.options.SchemesManagerFactory;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.util.containers.ContainerUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@State(name = "KeymapManager", storages = @Storage(file = StoragePathMacros.APP_CONFIG +
                                                          "/keymap.xml", roamingType = RoamingType.PER_PLATFORM), additionalExportFile = KeymapManagerImpl.KEYMAPS_DIR_PATH)
@Singleton
public class KeymapManagerImpl extends KeymapManagerEx implements PersistentStateComponent<Element> {
  static final String KEYMAPS_DIR_PATH = StoragePathMacros.ROOT_CONFIG + "/keymaps";

  private final List<KeymapManagerListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  private String myActiveKeymapName;
  private final Map<String, String> myBoundShortcuts = new HashMap<>();

  private static final String ACTIVE_KEYMAP = "active_keymap";
  private static final String NAME_ATTRIBUTE = "name";
  private final SchemesManager<Keymap, KeymapImpl> mySchemesManager;

  public static boolean ourKeymapManagerInitialized = false;

  @Inject
  KeymapManagerImpl(DefaultKeymap defaultKeymap, SchemesManagerFactory factory) {
    mySchemesManager = factory.createSchemesManager(KEYMAPS_DIR_PATH, new BaseSchemeProcessor<KeymapImpl>() {
      @Nonnull
      @Override
      public KeymapImpl readScheme(@Nonnull Element element) throws InvalidDataException {
        KeymapImpl keymap = new KeymapImpl();
        keymap.readExternal(element, getAllIncludingDefaultsKeymaps());
        return keymap;
      }

      @Override
      public Element writeScheme(@Nonnull final KeymapImpl scheme) {
        return scheme.writeExternal();
      }

      @Nonnull
      @Override
      public State getState(@Nonnull KeymapImpl scheme) {
        return scheme.canModify() ? State.POSSIBLY_CHANGED : State.NON_PERSISTENT;
      }
    }, RoamingType.PER_USER);

    List<Keymap> keymaps = defaultKeymap.getKeymaps();
    for (Keymap keymap : keymaps) {
      addKeymap(keymap);
      String systemDefaultKeymap = defaultKeymap.getDefaultKeymapName();
      if (systemDefaultKeymap.equals(keymap.getName())) {
        setActiveKeymap(keymap);
      }
    }
    mySchemesManager.loadSchemes();

    //noinspection AssignmentToStaticFieldFromInstanceMethod
    ourKeymapManagerInitialized = true;
  }

  @Override
  public Keymap[] getAllKeymaps() {
    List<Keymap> answer = new ArrayList<>();
    for (Keymap keymap : mySchemesManager.getAllSchemes()) {
      if (!keymap.getPresentableName().startsWith("$")) {
        answer.add(keymap);
      }
    }
    return answer.toArray(new Keymap[answer.size()]);
  }

  public Keymap[] getAllIncludingDefaultsKeymaps() {
    Collection<Keymap> keymaps = mySchemesManager.getAllSchemes();
    return keymaps.toArray(new Keymap[keymaps.size()]);
  }

  @Override
  @Nullable
  public Keymap getKeymap(@Nonnull String name) {
    return mySchemesManager.findSchemeByName(name);
  }

  @Override
  public Keymap getActiveKeymap() {
    return mySchemesManager.getCurrentScheme();
  }

  @Override
  public void setActiveKeymap(Keymap activeKeymap) {
    mySchemesManager.setCurrentSchemeName(activeKeymap == null ? null : activeKeymap.getName());
    fireActiveKeymapChanged();
  }

  @Override
  public void bindShortcuts(String sourceActionId, String targetActionId) {
    myBoundShortcuts.put(targetActionId, sourceActionId);
  }

  @Override
  public void unbindShortcuts(String targetActionId) {
    myBoundShortcuts.remove(targetActionId);
  }

  @Override
  public Set<String> getBoundActions() {
    return myBoundShortcuts.keySet();
  }

  @Override
  public String getActionBinding(String actionId) {
    Set<String> visited = null;
    String id = actionId, next;
    while ((next = myBoundShortcuts.get(id)) != null) {
      if (visited == null) visited = ContainerUtil.newHashSet();
      if (!visited.add(id = next)) break;
    }
    return Comparing.equal(id, actionId) ? null : id;
  }

  @Override
  public SchemesManager<Keymap, KeymapImpl> getSchemesManager() {
    return mySchemesManager;
  }

  public void addKeymap(Keymap keymap) {
    mySchemesManager.addNewScheme(keymap, true);
  }

  public void removeAllKeymapsExceptUnmodifiable() {
    List<Keymap> schemes = mySchemesManager.getAllSchemes();
    for (int i = schemes.size() - 1; i >= 0; i--) {
      Keymap keymap = schemes.get(i);
      if (keymap.canModify()) {
        mySchemesManager.removeScheme(keymap);
      }
    }

    mySchemesManager.setCurrentSchemeName(null);

    Collection<Keymap> keymaps = mySchemesManager.getAllSchemes();
    if (!keymaps.isEmpty()) {
      mySchemesManager.setCurrentSchemeName(keymaps.iterator().next().getName());
    }
  }

  @Override
  public Element getState() {
    Element result = new Element("component");
    if (mySchemesManager.getCurrentScheme() != null) {
      Element e = new Element(ACTIVE_KEYMAP);
      Keymap currentScheme = mySchemesManager.getCurrentScheme();
      if (currentScheme != null) {
        e.setAttribute(NAME_ATTRIBUTE, currentScheme.getName());
      }
      result.addContent(e);
    }
    return result;
  }

  @Override
  public void loadState(final Element state) {
    Element child = state.getChild(ACTIVE_KEYMAP);
    if (child != null) {
      myActiveKeymapName = child.getAttributeValue(NAME_ATTRIBUTE);
    }

    if (myActiveKeymapName != null) {
      Keymap keymap = getKeymap(myActiveKeymapName);
      if (keymap != null) {
        setActiveKeymap(keymap);
      }
    }
  }

  private void fireActiveKeymapChanged() {
    for (KeymapManagerListener listener : myListeners) {
      listener.activeKeymapChanged(mySchemesManager.getCurrentScheme());
    }
  }

  @Override
  public void addKeymapManagerListener(@Nonnull KeymapManagerListener listener) {
    pollQueue();
    myListeners.add(listener);
  }

  @Override
  public void addKeymapManagerListener(@Nonnull final KeymapManagerListener listener, @Nonnull Disposable parentDisposable) {
    pollQueue();
    myListeners.add(listener);
    Disposer.register(parentDisposable, () -> removeKeymapManagerListener(listener));
  }

  private void pollQueue() {
    // assume it is safe to remove elements during iteration, as is the case with the COWAL
    for (KeymapManagerListener listener : myListeners) {
      if (listener instanceof WeakKeymapManagerListener && ((WeakKeymapManagerListener)listener).isDead()) {
        myListeners.remove(listener);
      }
    }
  }

  @Override
  public void removeKeymapManagerListener(@Nonnull KeymapManagerListener listener) {
    pollQueue();
    myListeners.remove(listener);
  }

  @Override
  public void addWeakListener(@Nonnull KeymapManagerListener listener) {
    addKeymapManagerListener(new WeakKeymapManagerListener(this, listener));
  }

  @Override
  public void removeWeakListener(@Nonnull KeymapManagerListener listenerToRemove) {
    // assume it is safe to remove elements during iteration, as is the case with the COWAL
    for (KeymapManagerListener listener : myListeners) {
      if (listener instanceof WeakKeymapManagerListener && ((WeakKeymapManagerListener)listener).isWrapped(listenerToRemove)) {
        myListeners.remove(listener);
      }
    }
  }
}
