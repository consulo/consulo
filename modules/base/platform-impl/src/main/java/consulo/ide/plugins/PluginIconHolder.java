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

import com.intellij.ide.plugins.PluginNode;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author VISTALL
 * @since 07/12/2020
 */
public class PluginIconHolder {
  private static final Logger LOG = Logger.getInstance(PluginIconHolder.class);

  private static final float ourScale = 2;
  private static final Map<PluginId, Image> ourIcons = new ConcurrentHashMap<>();

  @Nonnull
  public static Image get(@Nonnull PluginDescriptor pluginDescriptor) {
    if (pluginDescriptor instanceof PluginNode) {
      Image forceIcon = ((PluginNode)pluginDescriptor).getInitializedIcon();
      if (forceIcon != null) {
        return forceIcon;
      }

      Image image = initializeImage(pluginDescriptor);
      ((PluginNode)pluginDescriptor).setInitializedIcon(image);
      return image;
    }
    else {
      return ourIcons.computeIfAbsent(pluginDescriptor.getPluginId(), pluginId -> {
        PluginDescriptor plugin = PluginManager.findPlugin(pluginId);
        if (plugin == null) {
          return decorateIcon(PlatformIconGroup.nodesPlugin());
        }
        return initializeImage(pluginDescriptor);
      });
    }
  }

  @Nonnull
  public static Image decorateIcon(@Nonnull Image image) {
    return ImageEffects.resize(image, ourScale);
  }

  @Nonnull
  private static Image initializeImage(@Nonnull PluginDescriptor pluginDescriptor) {
    byte[] iconBytes = pluginDescriptor.getIconBytes();
    if (iconBytes.length == 0) {
      return decorateIcon(PlatformIconGroup.nodesPlugin());
    }

    try {
      Image image = Image.fromBytes(Image.ImageType.SVG, iconBytes, Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE);
      return decorateIcon(image);
    }
    catch (IOException e) {
      LOG.warn(e);
    }
    
    return decorateIcon(PlatformIconGroup.nodesPlugin());
  }
}
