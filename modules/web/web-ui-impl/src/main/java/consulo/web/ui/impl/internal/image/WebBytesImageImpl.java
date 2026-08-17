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

import ar.com.hjg.pngj.PngReader;
import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.parser.SVGLoader;
import consulo.ui.image.Image;

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

    private final int myWidth;
    private final int myHeight;

    public WebBytesImageImpl(ImageType type, byte[] bytes) throws IOException {
        myBytes = bytes;
        mySvg = type == ImageType.SVG;

        try {
            if (mySvg) {
                SVGDocument document = new SVGLoader().load(new ByteArrayInputStream(bytes));
                if (document == null) {
                    throw new IOException("Not an svg image");
                }
                FloatSize size = document.size();
                myWidth = (int) size.getWidth();
                myHeight = (int) size.getHeight();
            }
            else {
                try (ByteArrayInputStream stream = new ByteArrayInputStream(bytes)) {
                    PngReader reader = new PngReader(stream);
                    try {
                        myWidth = reader.imgInfo.cols;
                        myHeight = reader.imgInfo.rows;
                    }
                    finally {
                        reader.close();
                    }
                }
            }
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    public WebRenderedImage toRendered() {
        return new WebRenderedImage(myBytes, mySvg);
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
