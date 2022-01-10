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
package consulo.ui.desktop.internal.image;

import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.ui.EmptyIcon;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentMap;

/**
 * @author VISTALL
 * @since 2018-05-07
 */
public class DesktopEmptyImageImpl extends EmptyIcon implements Image, DesktopImage<DesktopEmptyImageImpl> {
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
  public DesktopEmptyImageImpl copyWithScale(float scale) {
    return get((int)Math.ceil(getWidth() * scale), (int)Math.ceil(getHeight() * scale));
  }
}
