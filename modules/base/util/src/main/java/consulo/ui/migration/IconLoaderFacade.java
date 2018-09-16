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
package consulo.ui.migration;

import com.intellij.openapi.util.Computable;
import com.intellij.util.ui.JBUI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 * @author VISTALL
 * @since 2018-05-07
 */
@SuppressWarnings("deprecation")
public interface IconLoaderFacade {
  void activate();

  void setUseDarkIcons(boolean useDarkIcons);

  Icon getDisabledIcon(@Nullable Icon icon);

  Icon getTransparentIcon(@Nonnull final Icon icon, final float alpha);

  Icon getIconSnapshot(@Nonnull Icon icon);

  Icon createLazyIcon(Computable<Icon> iconComputable);

  SwingImageRef findIcon(URL url, boolean useCache);

  void set(Icon icon, String originalPath, ClassLoader classLoader);

  Image toImage(Icon icon, @Nullable JBUI.ScaleContext ctx);
}
