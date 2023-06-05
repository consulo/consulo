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
package consulo.ide.impl.idea.openapi.keymap.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.*;
import consulo.component.persist.scheme.BaseSchemeProcessor;
import consulo.component.persist.scheme.SchemeManager;
import consulo.component.persist.scheme.SchemeManagerFactory;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.openapi.keymap.ex.KeymapManagerEx;
import consulo.proxy.EventDispatcher;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.event.KeymapManagerListener;
import consulo.util.lang.Comparing;
import consulo.util.xml.serializer.InvalidDataException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import java.util.*;

@State(name = "KeymapManager", storages = @Storage(file = StoragePathMacros.APP_CONFIG +
                                                          "/keymap.xml", roamingType = RoamingType.PER_OS), additionalExportFile = KeymapManagerImpl.KEYMAPS_DIR_PATH)
@Singleton
@ServiceImpl
public class KeymapManagerImpl extends KeymapManagerEx implements PersistentStateComponent<Element> {
  static final String KEYMAPS_DIR_PATH = StoragePathMacros.ROOT_CONFIG + "/keymaps";

  private final EventDispatcher<KeymapManagerListener> myListeners = EventDispatcher.create(KeymapManagerListener.class);
  @Nonnull
  private final DefaultKeymap myDefaultKeymap;
  private String myActiveKeymapName;
  private final Map<String, String> myBoundShortcuts = new HashMap<>();

  private static final String ACTIVE_KEYMAP = "active_keymap";
  private static final String NAME_ATTRIBUTE = "name";
  private final SchemeManager<Keymap, KeymapImpl> mySchemeManager;

  public static boolean ourKeymapManagerInitialized = false;

  @Inject
  KeymapManagerImpl(DefaultKeymap defaultKeymap, SchemeManagerFactory factory) {
    myDefaultKeymap = defaultKeymap;
    mySchemeManager = factory.createSchemeManager(KEYMAPS_DIR_PATH, new BaseSchemeProcessor<Keymap, KeymapImpl>() {
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

      @Nonnull
      @Override
      public String getName(@Nonnull Keymap immutableElement) {
        return immutableElement.getName();
      }
    }, RoamingType.DEFAULT);

    List<Keymap> keymaps = defaultKeymap.getKeymaps();
    for (Keymap keymap : keymaps) {
      addKeymap(keymap);
      String systemDefaultKeymap = defaultKeymap.getDefaultKeymapName();
      if (systemDefaultKeymap.equals(keymap.getName())) {
        setActiveKeymap(keymap);
      }
    }
    mySchemeManager.loadSchemes();

    //noinspection AssignmentToStaticFieldFromInstanceMethod
    ourKeymapManagerInitialized = true;
  }

  @Override
  public Keymap[] getAllKeymaps() {
    List<Keymap> answer = new ArrayList<>();
    for (Keymap keymap : mySchemeManager.getAllSchemes()) {
      if (!keymap.getPresentableName().startsWith("$")) {
        answer.add(keymap);
      }
    }
    return answer.toArray(new Keymap[answer.size()]);
  }

  public Keymap[] getAllIncludingDefaultsKeymaps() {
    Collection<Keymap> keymaps = mySchemeManager.getAllSchemes();
    return keymaps.toArray(new Keymap[keymaps.size()]);
  }

  @Override
  @Nullable
  public Keymap getKeymap(@Nonnull String name) {
    return mySchemeManager.findSchemeByName(name);
  }

  @Override
  public Keymap getActiveKeymap() {
    return mySchemeManager.getCurrentScheme();
  }

  @Override
  public Keymap getDefaultKeymap() {
    return getKeymap(myDefaultKeymap.getDefaultKeymapName());
  }

  @Override
  public void setActiveKeymap(Keymap activeKeymap) {
    mySchemeManager.setCurrentSchemeName(activeKeymap == null ? null : activeKeymap.getName());
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
      if (visited == null) visited = new HashSet<>();
      if (!visited.add(id = next)) break;
    }
    return Comparing.equal(id, actionId) ? null : id;
  }

  @Override
  public SchemeManager<Keymap, KeymapImpl> getSchemeManager() {
    return mySchemeManager;
  }

  public void addKeymap(Keymap keymap) {
    mySchemeManager.addNewScheme(keymap, true);
  }

  public void removeAllKeymapsExceptUnmodifiable() {
    List<Keymap> schemes = mySchemeManager.getAllSchemes();
    for (int i = schemes.size() - 1; i >= 0; i--) {
      Keymap keymap = schemes.get(i);
      if (keymap.canModify()) {
        mySchemeManager.removeScheme(keymap);
      }
    }

    mySchemeManager.setCurrentSchemeName(null);

    Collection<Keymap> keymaps = mySchemeManager.getAllSchemes();
    if (!keymaps.isEmpty()) {
      mySchemeManager.setCurrentSchemeName(keymaps.iterator().next().getName());
    }
  }

  @Override
  public Element getState() {
    Element result = new Element("component");
    if (mySchemeManager.getCurrentScheme() != null) {
      Element e = new Element(ACTIVE_KEYMAP);
      Keymap currentScheme = mySchemeManager.getCurrentScheme();
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
    myListeners.getMulticaster().activeKeymapChanged(mySchemeManager.getCurrentScheme());
  }

  @Override
  public void addKeymapManagerListener(@Nonnull KeymapManagerListener listener) {
    myListeners.addListener(listener);
  }

  @Override
  public void addKeymapManagerListener(@Nonnull final KeymapManagerListener listener, @Nonnull Disposable parentDisposable) {
    myListeners.addListener(listener, parentDisposable);
  }

  @Override
  public void removeKeymapManagerListener(@Nonnull KeymapManagerListener listener) {
    myListeners.removeListener(listener);
  }
}
