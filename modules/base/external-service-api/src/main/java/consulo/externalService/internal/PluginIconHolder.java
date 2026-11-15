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
package consulo.externalService.internal;

import consulo.container.plugin.PluginDescriptor;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.IconDeferrer;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.style.StyleManager;
import consulo.util.io.UnsyncByteArrayInputStream;


/**
 * @author VISTALL
 * @since 2020-12-07
 */
public class PluginIconHolder {
    private static final String PLUGIN_ICON_KEY = "PLUGIN_ICON_KEY";
    private static final String PLUGIN_LARGE_ICON_KEY = "PLUGIN_LARGE_ICON_KEY";
    private static final String PLUGIN_SMALL_ICON_KEY = "PLUGIN_SMALL_ICON_KEY";
    private static final String PLUGIN_IMAGE_KEY = "PLUGIN_IMAGE_KEY";

    public static final int ICON_SIZE = 32;
    public static final int LARGE_ICON_SIZE = 64;
    public static final int SMALL_ICON_SIZE = 16;

    private static final Logger LOG = Logger.getInstance(PluginIconHolder.class);

    /**
     * The icon of a plugin in lists.
     */
    public static Image get(PluginDescriptor pluginDescriptor) {
        return get(pluginDescriptor, PLUGIN_ICON_KEY, ICON_SIZE);
    }

    /**
     * The icon of a plugin in the title of its page.
     */
    public static Image getLarge(PluginDescriptor pluginDescriptor) {
        return get(pluginDescriptor, PLUGIN_LARGE_ICON_KEY, LARGE_ICON_SIZE);
    }

    public static Image getSmall(PluginDescriptor pluginDescriptor) {
        return pluginDescriptor.computeUserData(
            PLUGIN_SMALL_ICON_KEY,
            s -> IconDeferrer.getInstance().defer(
                PlatformIconGroup.nodesPlugin(),
                pluginDescriptor.getPluginId().getIdString() + "@" + SMALL_ICON_SIZE,
                it -> ImageEffects.resize(initializeImage(pluginDescriptor), SMALL_ICON_SIZE, SMALL_ICON_SIZE)
            )
        );
    }

    /**
     * Shows the image instead of the icon of the plugin, in every size.
     */
    public static void put(PluginDescriptor descriptor, Image image) {
        descriptor.computeUserData(PLUGIN_IMAGE_KEY, key -> image);
    }

    private static Image get(PluginDescriptor pluginDescriptor, String key, int size) {
        return pluginDescriptor.computeUserData(
            key,
            s -> IconDeferrer.getInstance().defer(
                ImageEffects.resize(PlatformIconGroup.nodesPlugin(), size, size),
                pluginDescriptor.getPluginId().getIdString() + "@" + size,
                it -> ImageEffects.canvas(
                    size,
                    size,
                    canvas2D -> {
                        // canvas state will dropped on theme change
                        canvas2D.drawImage(initializeImage(pluginDescriptor, size), 0, 0);
                    }
                )
            )
        );
    }

    public static Image decorateIcon(Image image) {
        return ImageEffects.resize(image, LARGE_ICON_SIZE, LARGE_ICON_SIZE);
    }

    public static Image initializeImage(PluginDescriptor pluginDescriptor) {
        return initializeImage(pluginDescriptor, LARGE_ICON_SIZE);
    }

    private static Image initializeImage(PluginDescriptor pluginDescriptor, int size) {
        Image image = pluginDescriptor.getUserData(PLUGIN_IMAGE_KEY);
        if (image == null) {
            image = readImage(pluginDescriptor, StyleManager.get().getCurrentStyle().isDark());
        }
        return ImageEffects.resize(image, size, size);
    }

    public static Image initializeImage(PluginDescriptor pluginDescriptor, boolean isDark) {
        return decorateIcon(readImage(pluginDescriptor, isDark));
    }

    private static Image readImage(PluginDescriptor pluginDescriptor, boolean isDark) {
        byte[] iconBytes = pluginDescriptor.getIconBytes(isDark);
        if (iconBytes.length == 0) {
            return PlatformIconGroup.nodesPlugin();
        }

        try {
            return Image.fromStream(Image.ImageType.SVG, new UnsyncByteArrayInputStream(iconBytes));
        }
        catch (Throwable e) {
            LOG.warn(e);
        }

        return PlatformIconGroup.nodesPlugin();
    }
}
