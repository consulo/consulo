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
package consulo.ui.internal.image;

import ar.com.hjg.pngj.PngReader;
import com.kitfox.svg.SVGCache;
import com.kitfox.svg.SVGDiagram;
import consulo.ui.image.Image;
import consulo.ui.internal.WGwtUIThreadLocal;
import consulo.ui.migration.SwingImageRef;
import consulo.web.gwt.shared.ui.state.image.ImageState;
import consulo.web.gwt.shared.ui.state.image.MultiImageState;
import consulo.web.servlet.ui.UIServlet;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author VISTALL
 * @since 13-Jun-16
 */
public class WGwtImageImpl implements Image, WGwtImageWithState, SwingImageRef {
  private int myURLHash, myHeight, myWidth;

  @SuppressWarnings("unchecked")
  public WGwtImageImpl(@Nonnull URL url) {
    myURLHash = WGwtImageUrlCache.hashCode(url);

    URL scaledImageUrl = url;
    String urlText = url.toString();
    if (urlText.endsWith(".png")) {
      urlText = urlText.replace(".png", "@2x.png");
      try {
        scaledImageUrl = new URL(urlText);
      }
      catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }

    try (InputStream ignored = scaledImageUrl.openStream()) {
      // if scaled image resolved - map it for better quality
      myURLHash = WGwtImageUrlCache.hashCode(scaledImageUrl);
    }
    catch (Throwable ignored) {
    }

    try {
      if (urlText.endsWith(".svg")) {
        SVGDiagram diagram = SVGCache.getSVGUniverse().getDiagram(url.toURI());
        Rectangle2D viewRect = diagram.getViewRect();
        myWidth = (int)viewRect.getWidth();
        myHeight = (int)viewRect.getHeight();
      }
      else {
        PngReader reader = null;
        try (InputStream stream = url.openStream()) {
          reader = new PngReader(stream);
          myWidth = reader.imgInfo.cols;
          myHeight = reader.imgInfo.rows;
        }
        finally {
          if (reader != null) {
            reader.close();
          }
        }
      }
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public int getHeight() {
    return myHeight;
  }

  @Override
  public int getWidth() {
    return myWidth;
  }

  @Override
  public void toState(MultiImageState m) {
    ImageState state = new ImageState();
    UIServlet.UIImpl current = (UIServlet.UIImpl)WGwtUIThreadLocal.getUI();
    state.myURL = WGwtImageUrlCache.createURL(myURLHash, current.getURLPrefix());

    m.myImageState = state;
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getIconWidth() {
    return getWidth();
  }

  @Override
  public int getIconHeight() {
    return getHeight();
  }
}
