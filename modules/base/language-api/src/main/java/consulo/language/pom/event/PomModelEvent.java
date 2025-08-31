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
package consulo.language.pom.event;

import consulo.language.pom.PomModel;
import consulo.language.pom.PomModelAspect;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

public class PomModelEvent extends EventObject {
  private Map<PomModelAspect, PomChangeSet> myChangeSets;

  public PomModelEvent(PomModel source) {
    super(source);
  }

  @Nonnull
  public Set<PomModelAspect> getChangedAspects() {
    if (myChangeSets != null) {
      return myChangeSets.keySet();
    }
    else {
      return Collections.emptySet();
    }
  }

  public void registerChangeSet(PomModelAspect aspect, PomChangeSet set) {
    if (myChangeSets == null) {
      myChangeSets = new HashMap<>();
    }
    if (set == null) {
      myChangeSets.remove(aspect);
    }
    else {
      myChangeSets.put(aspect, set);
    }
  }

  public <T extends PomChangeSet> T registerChangeSetIfAbsent(PomModelAspect aspect, @Nonnull T set) {
    PomChangeSet oldSet = getChangeSet(aspect);
    if (oldSet != null) return (T)oldSet;

    registerChangeSet(aspect, set);
    return set;
  }


  @Nullable
  public PomChangeSet getChangeSet(PomModelAspect aspect) {
    if (myChangeSets == null) return null;
    return myChangeSets.get(aspect);
  }

  public void merge(@Nonnull PomModelEvent event) {
    if (event.myChangeSets == null) return;
    if (myChangeSets == null) {
      myChangeSets = new HashMap<>(event.myChangeSets);
      return;
    }
    for (Map.Entry<PomModelAspect, PomChangeSet> entry : event.myChangeSets.entrySet()) {
      PomModelAspect aspect = entry.getKey();
      PomChangeSet pomChangeSet = myChangeSets.get(aspect);
      if (pomChangeSet != null) {
        pomChangeSet.merge(entry.getValue());
      }
      else {
        myChangeSets.put(aspect, entry.getValue());
      }
    }
  }


  @Override
  public PomModel getSource() {
    return (PomModel)super.getSource();
  }

  public void beforeNestedTransaction() {
    if (myChangeSets != null) {
      for (PomChangeSet changeSet : myChangeSets.values()) {
        changeSet.beforeNestedTransaction();
      }
    }
  }
}
