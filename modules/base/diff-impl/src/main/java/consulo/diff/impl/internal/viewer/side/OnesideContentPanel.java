/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.impl.internal.viewer.side;

import consulo.diff.impl.internal.viewer.EditorHolder;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.WrappedLayout;
import org.jspecify.annotations.Nullable;

public class OnesideContentPanel {
  private final DockLayout myPanel;
  private final WrappedLayout myTitle;

  @RequiredUIAccess
  public OnesideContentPanel(Component content) {
    myTitle = WrappedLayout.create();

    myPanel = DockLayout.create(Space.NONE);
    myPanel.top(myTitle);
    myPanel.center(content);
  }

  public Component getComponent() {
    return myPanel;
  }

  @RequiredUIAccess
  public void setTitle(@Nullable Component title) {
    myTitle.set(title);
  }

  @RequiredUIAccess
  public static OnesideContentPanel createFromHolder(EditorHolder holder) {
    return new OnesideContentPanel(holder.getComponent());
  }
}
