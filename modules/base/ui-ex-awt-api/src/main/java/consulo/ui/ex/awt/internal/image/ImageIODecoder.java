/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ui.ex.awt.internal.image;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.component.extension.SPIClassLoaderExtension;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import org.jspecify.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Decodes image bytes on the jvm, with the readers of the jdk and those plugins bring along through
 * {@link SPIClassLoaderExtension} for {@link ImageIO} - a webp or a photoshop file reads here when its plugin is
 * installed.
 * <p/>
 * Every frontend falls back to it for a format it cannot show by itself, so whatever reads here shows everywhere.
 * Nothing of it draws, it runs headless as well.
 *
 * @author VISTALL
 * @since 2026-10-01
 */
public final class ImageIODecoder {
    private ImageIODecoder() {
    }

    public static @Nullable BufferedImage read(byte[] data) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image != null) {
                return image;
            }
        }
        catch (IOException | RuntimeException ignored) {
            // not a format of the jdk - one of the plugins may still read it
        }

        for (ClassLoader classLoader : getPluginClassLoaders()) {
            for (ImageReaderSpi readerSpi : ServiceLoader.load(ImageReaderSpi.class, classLoader)) {
                BufferedImage image = read(readerSpi, data);
                if (image != null) {
                    return image;
                }
            }
        }
        return null;
    }

    /**
     * @return the image re-encoded as png, null when nothing reads it
     */
    public static byte @Nullable [] toPng(byte[] data) {
        BufferedImage image = read(data);
        if (image == null) {
            return null;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            if (!ImageIO.write(image, "png", stream)) {
                return null;
            }
        }
        catch (IOException e) {
            return null;
        }
        return stream.toByteArray();
    }

    private static @Nullable BufferedImage read(ImageReaderSpi readerSpi, byte[] data) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            // a reader looks at the start of the stream and resets it - the contract of canDecodeInput
            if (input == null || !readerSpi.canDecodeInput(input)) {
                return null;
            }

            ImageReader reader = readerSpi.createReaderInstance();
            try {
                reader.setInput(input, true, true);
                return reader.read(reader.getMinIndex(), reader.getDefaultReadParam());
            }
            finally {
                reader.dispose();
            }
        }
        catch (IOException | RuntimeException e) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private static List<ClassLoader> getPluginClassLoaders() {
        // images are read before the application exists as well - the icons of the start - and plugins are of no
        // help there anyway
        Application application = ApplicationManager.getApplication();
        if (application == null) {
            return List.of();
        }

        return application.getExtensionPoint(SPIClassLoaderExtension.class).collectMapped(extension -> {
            if (extension.getTargetClass() != ImageIO.class) {
                return null;
            }
            PluginDescriptor plugin = PluginManager.getPlugin(extension.getClass());
            return plugin == null ? null : plugin.getPluginClassLoader();
        });
    }
}
