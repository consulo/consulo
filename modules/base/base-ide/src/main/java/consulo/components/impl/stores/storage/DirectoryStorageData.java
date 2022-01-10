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
package consulo.components.impl.stores.storage;

import com.intellij.openapi.components.StateSplitterEx;
import com.intellij.openapi.util.JDOMUtil;
import consulo.logging.Logger;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import static consulo.components.impl.stores.storage.StateMap.getNewByteIfDiffers;

public class DirectoryStorageData extends StorageDataBase {
  private static final Logger LOG = Logger.getInstance(DirectoryStorageData.class);

  public static final String DEFAULT_EXT = ".xml";

  private final Map<String, StateMap> myStates;

  public DirectoryStorageData() {
    this(new HashMap<>());
  }

  private DirectoryStorageData(@Nonnull Map<String, StateMap> states) {
    myStates = states;
  }

  @Override
  @Nonnull
  public Set<String> getComponentNames() {
    return myStates.keySet();
  }

  public boolean isEmpty() {
    return myStates.isEmpty();
  }

  @Nullable
  public static DirectoryStorageData setStateAndCloneIfNeed(@Nonnull String componentName,
                                                            @Nullable String fileName,
                                                            @Nullable Element newState,
                                                            @Nonnull DirectoryStorageData storageData) {
    StateMap fileToState = storageData.myStates.get(componentName);
    Object oldState = fileToState == null || fileName == null ? null : fileToState.get(fileName);
    if (fileName == null || newState == null || JDOMUtil.isEmpty(newState)) {
      if (fileName == null) {
        if (fileToState == null) {
          return null;
        }
      }
      else if (oldState == null) {
        return null;
      }

      DirectoryStorageData newStorageData = storageData.clone();
      if (fileName == null) {
        newStorageData.myStates.remove(componentName);
      }
      else {
        StateMap clonedFileToState = newStorageData.myStates.get(componentName);
        if (clonedFileToState.size() == 1) {
          newStorageData.myStates.remove(componentName);
        }
        else {
          clonedFileToState.remove(fileName);
          if (clonedFileToState.isEmpty()) {
            newStorageData.myStates.remove(componentName);
          }
        }
      }
      return newStorageData;
    }

    byte[] newBytes = null;
    if (oldState instanceof Element) {
      if (JDOMUtil.areElementsEqual((Element)oldState, newState)) {
        return null;
      }
    }
    else if (oldState != null) {
      newBytes = getNewByteIfDiffers(componentName, newState, (byte[])oldState);
      if (newBytes == null) {
        return null;
      }
    }

    DirectoryStorageData newStorageData = storageData.clone();
    newStorageData.put(componentName, fileName, newBytes == null ? newState : newBytes);
    return newStorageData;
  }

  @Nullable
  public Object setState(@Nonnull String componentName, @Nullable String fileName, @Nullable Element newState) {
    StateMap fileToState = myStates.get(componentName);
    if (fileName == null || newState == null || JDOMUtil.isEmpty(newState)) {
      if (fileToState == null) {
        return null;
      }
      else if (fileName == null) {
        return myStates.remove(componentName);
      }
      else {
        Object oldState = fileToState.remove(fileName);
        if (fileToState.isEmpty()) {
          myStates.remove(componentName);
        }
        return oldState;
      }
    }

    if (fileToState == null) {
      fileToState = new StateMap();
      myStates.put(componentName, fileToState);
      fileToState.put(fileName, newState);
    }
    else {
      Object oldState = fileToState.get(fileName);

      byte[] newBytes = null;
      if (oldState instanceof Element) {
        if (JDOMUtil.areElementsEqual((Element)oldState, newState)) {
          return null;
        }
      }
      else if (oldState != null) {
        newBytes = getNewByteIfDiffers(fileName, newState, (byte[])oldState);
        if (newBytes == null) {
          return null;
        }
      }

      fileToState.put(fileName, newBytes == null ? newState : newBytes);
    }
    return newState;
  }

  private void put(@Nonnull String componentName, @Nonnull String fileName, @Nonnull Object state) {
    StateMap fileToState = myStates.get(componentName);
    if (fileToState == null) {
      fileToState = new StateMap();
      myStates.put(componentName, fileToState);
    }
    fileToState.put(fileName, state);
  }

  public void processComponent(@Nonnull String componentName, @Nonnull BiConsumer<String, Object> consumer) {
    StateMap map = myStates.get(componentName);
    if (map != null) {
      map.forEachEntry(consumer);
    }
  }

  @Override
  protected DirectoryStorageData clone() {
    return new DirectoryStorageData(new HashMap<>(myStates));
  }

  public void clear() {
    myStates.clear();
  }

  @Override
  public boolean hasState(@Nonnull String componentName) {
    StateMap fileToState = myStates.get(componentName);
    return fileToState != null && fileToState.hasStates();
  }

  @Nullable
  public Element getCompositeStateAndArchive(@Nonnull String componentName, @Nonnull StateSplitterEx splitter) {
    StateMap fileToState = myStates.get(componentName);
    Element state = new Element(StorageData.COMPONENT);
    if (fileToState == null || fileToState.isEmpty()) {
      return state;
    }

    for (String fileName : fileToState.keys()) {
      Element subState = fileToState.getStateAndArchive(fileName);
      if (subState == null) {
        return null;
      }
      splitter.mergeStateInto(state, subState);
    }
    return state;
  }

  @Nonnull
  public Element stateToElement(@Nonnull String key, @Nullable Object state) {
    return StateMap.stateToElement(key, state, Collections.<String, Element>emptyMap());
  }

  @Nonnull
  public Set<String> getFileNames(@Nonnull String componentName) {
    StateMap fileToState = myStates.get(componentName);
    return fileToState == null || fileToState.isEmpty() ? Collections.<String>emptySet() : fileToState.keys();
  }
}
