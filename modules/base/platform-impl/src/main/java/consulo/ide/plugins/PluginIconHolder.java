/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.plugins;

import consulo.container.plugin.PluginDescriptor;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.style.StyleManager;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 07/12/2020
 */
public class PluginIconHolder {
  private static final String PLUGIN_ICON_KEY = "PLUGIN_ICON_KEY";

  private static final int ICON_SIZE = 32;

  private static final Logger LOG = Logger.getInstance(PluginIconHolder.class);

  @Nonnull
  public static Image get(@Nonnull PluginDescriptor pluginDescriptor) {
    return pluginDescriptor.computeUserData(PLUGIN_ICON_KEY, s -> ImageEffects.canvas(ICON_SIZE, ICON_SIZE, canvas2D -> {
      // canvas state will dropped on theme change
      Image pluginImage = initializeImage(pluginDescriptor);

      canvas2D.drawImage(pluginImage, 0, 0);
    }));
  }

  @Nonnull
  public static Image decorateIcon(@Nonnull Image image) {
    return ImageEffects.resize(image, ICON_SIZE, ICON_SIZE);
  }

  @Nonnull
  private static Image initializeImage(@Nonnull PluginDescriptor pluginDescriptor) {
    byte[] iconBytes = pluginDescriptor.getIconBytes(StyleManager.get().getCurrentStyle().isDark());
    if (iconBytes.length == 0) {
      return decorateIcon(PlatformIconGroup.nodesPluginBig());
    }

    try {
      Image image = Image.fromBytes(Image.ImageType.SVG, iconBytes, ICON_SIZE, ICON_SIZE);
      return decorateIcon(image);
    }
    catch (Throwable e) {
      LOG.warn(e);
    }
    
    return decorateIcon(PlatformIconGroup.nodesPluginBig());
  }
}
