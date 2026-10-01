/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui.image;

import consulo.annotation.DeprecationInfo;
import consulo.ui.Size2D;
import consulo.ui.internal.UIInternal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2016-06-13
 */
public interface Image {
    Image[] EMPTY_ARRAY = new Image[0];

    int DEFAULT_ICON_SIZE = 16;

    /**
     * Format of the bytes an image is made of. It is a hint only: a frontend which finds the bytes to be of another
     * format reads them as what they are, and a frontend which cannot show a format by itself decodes it on the jvm
     * side - so whatever is readable at all shows on every frontend.
     *
     * @param id       name of the format as image readers know it - {@code png}, {@code jpeg}, {@code psd}
     * @param mimeType media type of the bytes
     * @param vector   the bytes are a svg document, drawn anew at every size instead of having its pixels scaled
     */
    record ImageType(String id, String mimeType, boolean vector) {
        public static final ImageType PNG = new ImageType("png", "image/png", false);
        public static final ImageType SVG = new ImageType("svg", "image/svg+xml", true);
        public static final ImageType JPEG = new ImageType("jpeg", "image/jpeg", false);
        public static final ImageType GIF = new ImageType("gif", "image/gif", false);
        public static final ImageType BMP = new ImageType("bmp", "image/bmp", false);
        public static final ImageType ICO = new ImageType("ico", "image/x-icon", false);
        public static final ImageType WEBP = new ImageType("webp", "image/webp", false);
        public static final ImageType TIFF = new ImageType("tiff", "image/tiff", false);

        private static final Map<String, ImageType> BY_EXTENSION = Map.ofEntries(
            Map.entry("png", PNG),
            Map.entry("svg", SVG),
            Map.entry("jpg", JPEG),
            Map.entry("jpeg", JPEG),
            Map.entry("jpe", JPEG),
            Map.entry("gif", GIF),
            Map.entry("bmp", BMP),
            Map.entry("ico", ICO),
            Map.entry("webp", WEBP),
            Map.entry("tif", TIFF),
            Map.entry("tiff", TIFF)
        );

        /**
         * The type of a file extension - {@code jpg} gives {@link #JPEG}. An extension no frontend knows by itself, like
         * {@code psd}, gives a type of that name, which is decoded on the jvm side wherever it is shown.
         */
        public static ImageType fromExtension(String extension) {
            String id = extension.toLowerCase(Locale.ROOT);
            ImageType known = BY_EXTENSION.get(id);
            return known != null ? known : new ImageType(id, "image/" + id, false);
        }

        /**
         * The type of a file name or a path by its extension, {@link #PNG} when there is none.
         */
        public static ImageType fromFileName(String fileName) {
            int dot = fileName.lastIndexOf('.');
            if (dot < 0 || dot < fileName.lastIndexOf('/') || dot == fileName.length() - 1) {
                return PNG;
            }
            return fromExtension(fileName.substring(dot + 1));
        }
    }

    @Deprecated
    static Image create(URL url) throws IOException {
        return fromUrl(url);
    }

    static Image fromUrl(URL url) throws IOException {
        return UIInternal.get()._Image_fromUrl(url);
    }

    /**
     * Return image from bytes. JPG, PNG only
     */
    @Deprecated
    static Image fromBytes(byte[] bytes, int width, int height) throws IOException {
        return fromBytes(ImageType.PNG, bytes);
    }

    @Deprecated
    @DeprecationInfo("Image#fromBytes(imageType, bytes) - width&height ignored")
    static Image fromBytes(ImageType imageType, byte[] bytes, int width, int height) throws IOException {
        return fromBytes(imageType, bytes);
    }

    /**
     * @param imageType what the bytes are expected to be - see {@link ImageType} for how loosely it is taken
     * @throws IOException when the bytes are not an image anything can read
     */
    static Image fromBytes(ImageType imageType, byte[] bytes) throws IOException {
        return fromStream(imageType, new ByteArrayInputStream(bytes));
    }

    /**
     * @see #fromBytes(ImageType, byte[])
     */
    static Image fromStream(ImageType imageType, InputStream stream) throws IOException {
        return UIInternal.get()._Image_fromStream(imageType, stream);
    }

    static Image lazy(Supplier<Image> imageSupplier) {
        return UIInternal.get()._Image_lazy(imageSupplier);
    }

    static <S> Image stated(ImageState<S> state, Function<S, Image> funcCall) {
        return UIInternal.get()._Image_stated(state, funcCall);
    }

    /**
     * Work is going on where this is shown, and its end is not known.
     * <p>
     * Square, {@code widthAndHeight} on each side. {@link ImageEffects#resize} gives a busy image of the new size, drawn
     * again at that size and never stretched; when width and height differ it is drawn in the centred square of the
     * smaller side. How it moves and its colour are decided by whoever draws it; where it cannot move it is shown still.
     * A component showing it, alone or inside another image other than a colorized one, keeps it moving by itself while
     * it is shown. It never stops by itself: show another image when the work is done.
     */
    static Image busy(int widthAndHeight) {
        return UIInternal.get()._Image_busy(widthAndHeight);
    }

    /**
     * {@link #busy(int)} of the icon size.
     */
    static Image busy() {
        return busy(DEFAULT_ICON_SIZE);
    }

    static EmptyImage empty() {
        return empty(0);
    }

    static EmptyImage empty(int widthAndHeight) {
        return UIInternal.get()._ImageEffects_empty(widthAndHeight, widthAndHeight);
    }

    static EmptyImage empty(int width, int height) {
        return UIInternal.get()._ImageEffects_empty(width, height);
    }

    int getHeight();

    int getWidth();

    default Size2D getSize() {
        return new Size2D(getWidth(), getHeight());
    }
}
