/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.desktop.awt.settings;

import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.util.collection.MultiValuesMap;
import consulo.ui.ex.awt.speedSearch.ElementFilter;
import consulo.util.concurrent.AsyncResult;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;

public class OptionsEditorContext {
  ElementFilter.Active myFilter;

  CopyOnWriteArraySet<OptionsEditorColleague> myColleagues = new CopyOnWriteArraySet<>();

  Configurable myCurrentConfigurable;
  Set<Configurable> myModified = new CopyOnWriteArraySet<>();
  Map<Configurable, ConfigurationException> myErrors = new HashMap<>();
  private boolean myHoldingFilter;
  private final Map<Configurable,  Configurable> myConfigurableToParentMap = new HashMap<>();
  private final MultiValuesMap<Configurable, Configurable> myParentToChildrenMap = new MultiValuesMap<>();


  public OptionsEditorContext(ElementFilter.Active filter) {
    myFilter = filter;
  }

  AsyncResult<Void> fireSelected(@Nullable Configurable configurable, @Nonnull OptionsEditorColleague requestor) {
    Configurable old = myCurrentConfigurable;
    myCurrentConfigurable = configurable;

    return notify(colleague -> colleague.onSelected(configurable, old), requestor);
  }

  AsyncResult<Void> fireModifiedAdded(@Nonnull Configurable configurable, @Nullable OptionsEditorColleague requestor) {
    if(myModified.contains(configurable)) {
      return AsyncResult.rejected();
    }
    
    myModified.add(configurable);

    return notify(colleague -> colleague.onModifiedAdded(configurable), requestor);
  }

  AsyncResult<Void> fireModifiedRemoved(@Nonnull Configurable configurable, @Nullable OptionsEditorColleague requestor) {
    if (!myModified.contains(configurable)) return AsyncResult.rejected();

    myModified.remove(configurable);

    return notify(colleague -> colleague.onModifiedRemoved(configurable), requestor);
  }

  AsyncResult<Void> fireErrorsChanged(Map<Configurable, ConfigurationException> errors, OptionsEditorColleague requestor) {
    if (myErrors.equals(errors)) return AsyncResult.rejected();

    myErrors = errors != null ? errors : new HashMap<>();

    return notify(OptionsEditorColleague::onErrorsChanged, requestor);
  }

  AsyncResult<Void> notify(Function<OptionsEditorColleague, AsyncResult<Void>> action, OptionsEditorColleague requestor) {
    List<AsyncResult<Void>> all = new ArrayList<>();
    for (OptionsEditorColleague each : myColleagues) {
      if (each != requestor) {
        all.add(action.apply(each));
      }
    }

    return AsyncResult.merge(all);
  }

  public void fireReset(Configurable configurable) {
    if (myModified.contains(configurable)) {
      fireModifiedRemoved(configurable, null);
    }

    if (myErrors.containsKey(configurable)) {
      HashMap<Configurable, ConfigurationException> newErrors = new HashMap<>();
      newErrors.remove(configurable);
      fireErrorsChanged(newErrors, null);
    }
  }

  public boolean isModified(Configurable configurable) {
    return myModified.contains(configurable);
  }

  public void setHoldingFilter(boolean holding) {
    myHoldingFilter = holding;
  }

  public boolean isHoldingFilter() {
    return myHoldingFilter;
  }

  public Configurable getParentConfigurable(Configurable configurable) {
    return myConfigurableToParentMap.get(configurable);
  }

  public void registerKid(Configurable parent, Configurable kid) {
    myConfigurableToParentMap.put(kid,parent);
    myParentToChildrenMap.put(parent, kid);
  }

  public Collection<Configurable> getChildren(Configurable parent) {
    Collection<Configurable> result = myParentToChildrenMap.get(parent);
    return result == null ? Collections.<Configurable>emptySet() : result;
  }

  @Nonnull
  ElementFilter<Configurable> getFilter() {
    return myFilter;
  }

  public Configurable getCurrentConfigurable() {
    return myCurrentConfigurable;
  }

  public Set<Configurable> getModified() {
    return myModified;
  }

  public Map<Configurable, ConfigurationException> getErrors() {
    return myErrors;
  }

  public void addColleague(OptionsEditorColleague colleague) {
    myColleagues.add(colleague);
  }
}
