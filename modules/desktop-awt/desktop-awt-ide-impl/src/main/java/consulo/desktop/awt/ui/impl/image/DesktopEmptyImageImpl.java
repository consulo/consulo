/*
 * Copyright 2013-2018 consulo.io
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
package consulo.desktop.awt.ui.impl.image;

import consulo.application.util.ConcurrentFactoryMap;
import consulo.ui.ex.awt.EmptyIcon;
import consulo.ui.image.Image;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import java.util.concurrent.ConcurrentMap;

/**
 * @author VISTALL
 * @since 2018-05-07
 */
public class DesktopEmptyImageImpl extends EmptyIcon implements Image, DesktopAWTImage {
  private static final ConcurrentMap<Pair<Integer, Integer>, DesktopEmptyImageImpl> cache = ConcurrentFactoryMap.createMap(p -> new DesktopEmptyImageImpl(p.getFirst(), p.getSecond()));

  @Nonnull
  public static DesktopEmptyImageImpl get(int width, int height) {
    return cache.get(Pair.create(width, height));
  }

  private DesktopEmptyImageImpl(int width, int height) {
    super(width, height, false);
    setIconPreScaled(false);
  }

  @Override
  public int getHeight() {
    return getIconHeight();
  }

  @Override
  public int getWidth() {
    return getIconWidth();
  }

  @Nonnull
  @Override
  public DesktopAWTImage copyWithNewSize(int width, int height) {
    return get(width, height);
  }

  @Nonnull
  @Override
  public DesktopAWTImage copyWithForceLibraryId(String libraryId) {
    return this;
  }
}
