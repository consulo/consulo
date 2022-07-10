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
package consulo.ide.impl.idea.internal.statistic;

import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.ide.impl.idea.internal.statistic.beans.ConvertUsagesUtil;
import consulo.ide.statistic.UsageTrigger;
import consulo.util.xml.serializer.annotation.MapAnnotation;
import consulo.util.xml.serializer.annotation.Tag;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ksafonov
 */
@Singleton
@ServiceImpl
@State(name = "UsageTrigger", storages = @Storage(value = "statistics.application.usages.xml", roamingType = RoamingType.DISABLED))
public class UsageTriggerImpl implements UsageTrigger, PersistentStateComponent<UsageTriggerImpl.State> {

  public static class State {
    @Tag("counts")
    @MapAnnotation(surroundWithTag = false, keyAttributeName = "feature", valueAttributeName = "count")
    public Map<String, Integer> myValues = new HashMap<>();
  }

  private State myState = new State();

  @Override
  public void doTrigger(String feature) {
    ConvertUsagesUtil.assertDescriptorName(feature);
    final Integer count = myState.myValues.get(feature);
    if (count == null) {
      myState.myValues.put(feature, 1);
    }
    else {
      myState.myValues.put(feature, count + 1);
    }
  }

  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(final State state) {
    myState = state;
  }
}
