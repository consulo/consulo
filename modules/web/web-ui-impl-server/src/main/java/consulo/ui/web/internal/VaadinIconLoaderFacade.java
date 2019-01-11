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
package consulo.ui.web.internal;

import com.intellij.openapi.util.Computable;
import com.intellij.util.ui.JBUI;
import consulo.ui.migration.IconLoaderFacade;
import consulo.ui.migration.SwingImageRef;
import consulo.ui.web.internal.image.WGwtImageImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 * @author VISTALL
 * @since 2018-05-08
 */
public class VaadinIconLoaderFacade implements IconLoaderFacade {
  @Override
  public void activate() {

  }

  @Override
  public void resetDark() {

  }

  @Override
  public Icon getDisabledIcon(@Nullable Icon icon) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Icon getTransparentIcon(@Nonnull Icon icon, float alpha) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Icon getIconSnapshot(@Nonnull Icon icon) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Icon createLazyIcon(Computable<Icon> iconComputable) {
    return iconComputable.compute();
  }

  @Override
  public SwingImageRef findIcon(URL url, boolean useCache) {
    return new WGwtImageImpl(url);
  }

  @Override
  public void set(Icon icon, String originalPath, ClassLoader classLoader) {

  }

  @Override
  public Image toImage(Icon icon, @Nullable JBUI.ScaleContext ctx) {
    throw new UnsupportedOperationException();
  }
}
