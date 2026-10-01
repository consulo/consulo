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
package consulo.web.ui.impl.internal.image;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.parser.SVGLoader;
import consulo.ui.ex.awt.internal.image.ImageIODecoder;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * An image handed over as bytes rather than named by an id - the icon of a plugin read out of its own jar is
 * the one the platform asks for by this route, and there is no url the servlet could serve it back from.
 * <p/>
 * So it carries its own bytes to the browser as a data uri. Everything id-addressed keeps going through
 * {@link WebImageUrl} and the servlet, this is only for what has no id at all.
 *
 * @author VISTALL
 * @since 2026-08-08
 */
public class WebBytesImageImpl implements Image {
    private final byte[] myBytes;
    private final boolean mySvg;
    private final String myContentType;
    private final int myWidth;
    private final int myHeight;

    private WebBytesImageImpl(byte[] bytes, boolean svg, String contentType, int width, int height) {
        myBytes = bytes;
        mySvg = svg;
        myContentType = contentType;
        myWidth = width;
        myHeight = height;
    }

    /**
     * A format the browser shows by itself is sent as it is - which one it is, is read from the bytes rather than
     * taken from the type. Any other format is decoded here and sent as png, the browser never sees it.
     */
    public static WebBytesImageImpl create(ImageType type, byte[] bytes) throws IOException {
        if (type.vector()) {
            SVGDocument document = loadSvg(bytes);
            if (document == null) {
                throw new IOException("Not an svg image");
            }

            FloatSize size = document.size();
            return new WebBytesImageImpl(bytes, true, ImageType.SVG.mimeType(), (int) size.getWidth(), (int) size.getHeight());
        }

        WebImageHeader header = WebImageHeader.read(bytes);
        if (header != null) {
            return new WebBytesImageImpl(bytes, false, header.contentType(), header.width(), header.height());
        }

        byte[] png = ImageIODecoder.toPng(bytes);
        WebImageHeader pngHeader = png == null ? null : WebImageHeader.read(png);
        if (png == null || pngHeader == null) {
            throw new IOException("Unable to read " + type.id() + " image of " + bytes.length + " bytes");
        }
        return new WebBytesImageImpl(png, false, pngHeader.contentType(), pngHeader.width(), pngHeader.height());
    }

    private static @Nullable SVGDocument loadSvg(byte[] bytes) throws IOException {
        try {
            return new SVGLoader().load(new ByteArrayInputStream(bytes));
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    public WebRenderedImage toRendered() {
        return new WebRenderedImage(myBytes, mySvg, myContentType);
    }

    @Override
    public int getWidth() {
        return myWidth;
    }

    @Override
    public int getHeight() {
        return myHeight;
    }
}
