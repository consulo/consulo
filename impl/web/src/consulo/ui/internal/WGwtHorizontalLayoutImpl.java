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
import consulo.ui.RequiredUIAccess;
import consulo.ui.HorizontalLayout;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class WGwtHorizontalLayoutImpl extends WGwtLayoutImpl<Object> implements HorizontalLayout {
  @NotNull
  @Override
  @RequiredUIAccess
  public HorizontalLayout add(@NotNull Component component) {
    addChild((WGwtBaseComponent)component, new Object());
    return this;
  }
}
