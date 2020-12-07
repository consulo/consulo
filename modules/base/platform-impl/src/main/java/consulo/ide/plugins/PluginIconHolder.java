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
import consulo.container.plugin.PluginId;
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

  public static Image get(@Nonnull PluginDescriptor pluginDescriptor) {
    return ourIcons.computeIfAbsent(pluginDescriptor.getPluginId(), pluginId -> {
      byte[] iconBytes = pluginDescriptor.getIconBytes();
      if (iconBytes.length == 0) {
        return ImageEffects.resize(PlatformIconGroup.nodesPlugin(), ourScale);
      }

      try {
        Image image = Image.fromBytes(Image.ImageType.SVG, iconBytes, Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE);
        return ImageEffects.resize(image, ourScale);
      }
      catch (IOException e) {
        LOG.warn(e);
      }

      return ImageEffects.resize(PlatformIconGroup.nodesPlugin(), ourScale);
    });
  }
}
