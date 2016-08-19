/*
 * Copyright 2013-2016 must-be.org
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
package consulo.ui.internal;

import consulo.ui.Component;
import consulo.ui.SplitLayout;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Map;

/**
 * @author VISTALL
 * @since 13-Jun-16
 */
public abstract class WGwtSplitLayoutImpl extends WGwtLayoutImpl<Boolean> implements SplitLayout {
  private int myProportion = 50;

  @Override
  protected void getState(Map<String, Serializable> map) {
    super.getState(map);
    map.put("proportion", myProportion);
  }

  @Override
  protected void convertConstraint(Map<String, Serializable> map, Boolean constraint) {
    map.put("position", constraint);
  }

  @Override
  public void setProportion(int percent) {
    myProportion = percent;
  }

  @Override
  public SplitLayout setFirstComponent(@NotNull Component component) {
    addChild((WGwtBaseComponent)component, Boolean.TRUE);
    return this;
  }

  @Override
  public SplitLayout setSecondComponent(@NotNull Component component) {
    addChild((WGwtBaseComponent)component, Boolean.FALSE);
    return this;
  }
}
