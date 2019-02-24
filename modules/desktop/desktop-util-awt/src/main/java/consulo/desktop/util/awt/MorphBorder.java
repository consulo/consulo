/*
 * Copyright 2013-2017 consulo.io
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
package consulo.desktop.util.awt;

import com.intellij.util.NotNullProducer;

import javax.annotation.Nonnull;

import javax.swing.border.Border;
import java.awt.*;

/**
 * @author VISTALL
 * @since 19-Jun-17
 */
public class MorphBorder implements Border {
  private static final UIModificationTracker ourTracker = UIModificationTracker.getInstance();

  public static MorphBorder of(@Nonnull NotNullProducer<Border> func) {
    return new MorphBorder(func);
  }

  private final NotNullProducer<Border> myBorderProducer;

  private long myLastModificationCount;
  private Border myLastComputedBorder;

  private MorphBorder(NotNullProducer<Border> borderProducer) {
    myBorderProducer = borderProducer;
    myLastModificationCount = UIModificationTracker.getInstance().getModificationCount();
    myLastComputedBorder = borderProducer.produce();
  }

  @Nonnull
  private Border getBorder() {
    long modificationCount = ourTracker.getModificationCount();
    if (myLastModificationCount == modificationCount) {
      return myLastComputedBorder;
    }

    myLastModificationCount = modificationCount;
    return myLastComputedBorder = myBorderProducer.produce();
  }

  @Override
  public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    getBorder().paintBorder(c, g, x, y, width, height);
  }

  @Override
  public Insets getBorderInsets(Component c) {
    return getBorder().getBorderInsets(c);
  }

  @Override
  public boolean isBorderOpaque() {
    return getBorder().isBorderOpaque();
  }
}
