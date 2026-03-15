/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.versionControlSystem.internal;

import consulo.ui.ex.action.AnAction;
import consulo.util.dataholder.Key;
import consulo.diff.DiffDialogHints;
import consulo.versionControlSystem.change.Change;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class ShowDiffContext {
  
  private final DiffDialogHints myDialogHints;

  @Nullable private List<AnAction> myActions;
  @Nullable
  private Map<Key, Object> myChainContext;
  @Nullable
  private Map<Change, Map<Key, Object>> myRequestContext;

  public ShowDiffContext() {
    this(DiffDialogHints.DEFAULT);
  }

  public ShowDiffContext(DiffDialogHints dialogHints) {
    myDialogHints = dialogHints;
  }

  
  public DiffDialogHints getDialogHints() {
    return myDialogHints;
  }

  
  public List<AnAction> getActions() {
    if (myActions == null) return Collections.emptyList();
    return myActions;
  }

  
  public Map<Key, Object> getChainContext() {
    if (myChainContext == null) return Collections.emptyMap();
    return myChainContext;
  }

  
  public Map<Key, Object> getChangeContext(Change change) {
    if (myRequestContext == null) return Collections.emptyMap();
    Map<Key, Object> map = myRequestContext.get(change);
    if (map == null) return Collections.emptyMap();
    return map;
  }

  public void addActions(List<AnAction> action) {
    if (myActions == null) myActions = new ArrayList<>();
    myActions.addAll(action);
  }

  public void addAction(AnAction action) {
    if (myActions == null) myActions = new ArrayList<>();
    myActions.add(action);
  }

  public <T> void putChainContext(Key<T> key, T value) {
    if (myChainContext == null) {
      myChainContext = new HashMap<>();
    }
    myChainContext.put(key, value);
  }

  public <T> void putChangeContext(Change change, Key<T> key, T value) {
    if (myRequestContext == null) myRequestContext = new HashMap<>();
    if (!myRequestContext.containsKey(change)) myRequestContext.put(change, new HashMap<>());
    myRequestContext.get(change).put(key, value);
  }
}
