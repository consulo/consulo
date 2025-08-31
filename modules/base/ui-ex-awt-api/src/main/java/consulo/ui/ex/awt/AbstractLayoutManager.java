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

package consulo.ui.ex.awt;

import java.awt.*;

public abstract class AbstractLayoutManager implements LayoutManager2 {
  @Override
  public void addLayoutComponent(Component comp, Object constraints) {
  }

  @Override
  public Dimension maximumLayoutSize(Container target) {
    return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  @Override
  public float getLayoutAlignmentX(Container target) {
    return 0;
  }

  @Override
  public float getLayoutAlignmentY(Container target) {
    return 0;
  }

  @Override
  public void invalidateLayout(Container target) {
  }

  @Override
  public void addLayoutComponent(String name, Component comp) {
  }

  @Override
  public void removeLayoutComponent(Component comp) {
  }

  @Override
  public Dimension minimumLayoutSize(Container parent) {
    return new Dimension(0, 0);
  }
}
