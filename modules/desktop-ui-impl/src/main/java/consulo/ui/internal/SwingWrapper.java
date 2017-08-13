/*
 * Copyright 2013-2016 consulo.io
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
import consulo.ui.shared.Size;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * @author VISTALL
 * @since 19-Nov-16.
 */
public interface SwingWrapper extends Component {
  @Nullable
  @Override
  default Component getParentComponent() {
    Container container = (Container)this;
    return (Component)container.getParent();
  }

  @RequiredUIAccess
  @Override
  default void setSize(@NotNull Size size) {
    Container container = (Container)this;
    container.setPreferredSize(new Dimension(size.getWidth(), size.getHeight()));
  }

  @Override
  default void dispose() {
  }
}
