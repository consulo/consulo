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

package consulo.ui.ex;

import consulo.disposer.Disposable;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public interface IdeGlassPane {
  void addMousePreprocessor(MouseListener listener, Disposable parent);

  void addMouseMotionPreprocessor(MouseMotionListener listener, Disposable parent);

  void addPainter(Component component, Painter painter, Disposable parent);

  void removePainter(Painter painter);

  void removeMousePreprocessor(MouseListener listener);

  void removeMouseMotionPreprocessor(MouseMotionListener listener);

  void setCursor(@Nullable Cursor cursor, @Nonnull Object requestor);

  interface TopComponent {
    boolean canBePreprocessed(MouseEvent e);
  }
}
