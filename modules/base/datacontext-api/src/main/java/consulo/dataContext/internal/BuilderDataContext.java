/*
 * Copyright 2013-2022 consulo.io
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
package consulo.dataContext.internal;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.dataContext.GetDataRule;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * @author VISTALL
 * @since 03/02/2022
 */
public class BuilderDataContext implements DataContext {
  private final Map<Key, Object> myDataId2Data;
  private final DataContext myParent;

  public BuilderDataContext(@Nonnull Map<Key, Object> dataId2data, @Nullable DataContext parent) {
    myDataId2Data = dataId2data;
    myParent = parent;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getData(@Nonnull Key<T> dataId) {
    Object result = getDataFromSelfOrParent(dataId);

    if (result == null) {
      GetDataRule rule = ((DataRuleHoler)DataManager.getInstance()).getDataRule(dataId);
      if (rule != null) {
        return (T)rule.getData(this::getDataFromSelfOrParent);
      }
    }

    return (T)result;
  }

  private Object getDataFromSelfOrParent(@Nonnull Key dataId) {
    return myDataId2Data.containsKey(dataId) ? myDataId2Data.get(dataId) : myParent == null ? null : myParent.getData(dataId);
  }
}
