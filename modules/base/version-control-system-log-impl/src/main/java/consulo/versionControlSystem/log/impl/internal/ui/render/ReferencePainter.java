/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.ui.render;

import consulo.versionControlSystem.log.VcsLogRefManager;
import consulo.versionControlSystem.log.VcsRef;

import org.jspecify.annotations.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.Collection;

public interface ReferencePainter {
  void customizePainter(JComponent component,
                        Collection<VcsRef> references,
                        @Nullable VcsLogRefManager manager,
                        Color background,
                        Color foreground);

  void paint(Graphics2D g2, int x, int y, int height);

  Dimension getSize();

  boolean isLeftAligned();

  default Font getReferenceFont() {
    return RectanglePainter.getFont();
  }
}
