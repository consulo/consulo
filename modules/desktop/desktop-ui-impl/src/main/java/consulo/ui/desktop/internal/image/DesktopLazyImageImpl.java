/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui.desktop.internal.image;

import consulo.awt.TargetAWT;
import consulo.desktop.util.awt.UIModificationTracker;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2019-01-26
 */
public class DesktopLazyImageImpl extends DesktopBaseLazyImageImpl {
  private static final UIModificationTracker ourTracker = UIModificationTracker.getInstance();

  private final Supplier<Image> myImageSupplier;

  public DesktopLazyImageImpl(Supplier<Image> imageSupplier) {
    myImageSupplier = imageSupplier;
  }

  @Override
  protected long getModificationCount() {
    return ourTracker.getModificationCount();
  }

  @Nonnull
  @Override
  protected Icon calcIcon() {
    return TargetAWT.to(myImageSupplier.get());
  }
}