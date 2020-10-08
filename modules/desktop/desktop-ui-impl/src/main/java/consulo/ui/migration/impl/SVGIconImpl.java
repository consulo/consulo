/*
 * SVG Salamander
 * Copyright (c) 2004, Mark McKay
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 *   - Redistributions of source code must retain the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer.
 *   - Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Mark McKay can be contacted at mark@kitfox.com.  Salamander and other
 * projects can be found at http://www.kitfox.com
 *
 * Created on April 21, 2005, 10:45 AM
 */

package consulo.ui.migration.impl;

import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBUI;
import com.kitfox.svg.SVGCache;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGUniverse;
import consulo.annotation.DeprecationInfo;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImageFilter;
import java.net.URI;
import java.util.function.Supplier;


/**
 * @author kitfox
 */
@Deprecated
@DeprecationInfo("Old svg icon implementation, used by old icon loader. Use ImageKey, or target DesktopSvgImageImpl impl")
class SVGIconImpl extends JBUI.CachingScalableJBIcon<SVGIconImpl> {
  public static final long serialVersionUID = 1;

  private SVGUniverse svgUniverse = SVGCache.getSVGUniverse();
  private URI svgURI;
  private Supplier<ImageFilter>[] myImageFilters;

  /**
   * Creates a new instance of SVGIcon
   */
  public SVGIconImpl() {
  }

  @Nonnull
  @Override
  protected SVGIconImpl copy() {
    SVGIconImpl svgIcon = new SVGIconImpl();
    svgIcon.setSvgUniverse(svgUniverse);
    svgIcon.setSvgURI(svgURI);
    return svgIcon;
  }

  /**
   * @return height of this icon
   */
  private int getIconHeightIgnoreAutosize() {
    SVGDiagram diagram = svgUniverse.getDiagram(svgURI);
    if (diagram == null) {
      return 0;
    }
    return (int)diagram.getHeight();
  }

  /**
   * @return width of this icon
   */

  private int getIconWidthIgnoreAutosize() {
    SVGDiagram diagram = svgUniverse.getDiagram(svgURI);
    if (diagram == null) {
      return 0;
    }
    return (int)diagram.getWidth();
  }

  @Override
  public int getIconWidth() {
    final SVGDiagram diagram = svgUniverse.getDiagram(svgURI);
    return (int)Math.ceil(scaleVal(diagram.getWidth()));
  }

  @Override
  public int getIconHeight() {
    final SVGDiagram diagram = svgUniverse.getDiagram(svgURI);
    return (int)Math.ceil(scaleVal(diagram.getHeight()));
  }


  /**
   * Draws the icon to the specified component.
   *
   * @param comp             - Component to draw icon to.  This is ignored by SVGIcon, and can be set to null; only gg is used for drawing the icon
   * @param originalGraphics - Graphics context to render SVG content to
   * @param x                - X coordinate to draw icon
   * @param y                - Y coordinate to draw icon
   */
  @Override
  public void paintIcon(Component comp, Graphics originalGraphics, int x, int y) {
    if (myImageFilters == null || myImageFilters.length == 0) {
      Graphics2D g = (Graphics2D)originalGraphics.create();
      GraphicsConfig config = GraphicsUtil.setupAAPainting(g);
      paintIcon(g, x, y);
      config.restore();
      g.dispose();

    }
    else {
      Image image = new BufferedImage(getIconWidthIgnoreAutosize(), getIconWidthIgnoreAutosize(), BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = ((BufferedImage)image).createGraphics();
      GraphicsConfig config = GraphicsUtil.setupAAPainting(g);
      paintIcon(g, 0, 0);
      for (Supplier<ImageFilter> filter : myImageFilters) {
        image = ImageUtil.filter(image, filter.get());
      }
      config.restore();
      g.dispose();

      originalGraphics.drawImage(image, x, y, null);
    }
  }

  private void paintIcon(Graphics2D g, int x, int y) {
    SVGDiagram diagram = svgUniverse.getDiagram(svgURI);
    if (diagram == null) {
      return;
    }

    diagram.setDeviceViewport(new Rectangle(getIconWidth(), getIconHeight()));

    g.translate(x, y);
    diagram.setIgnoringClipHeuristic(true);

    final int width = getIconWidthIgnoreAutosize();
    final int height = getIconHeightIgnoreAutosize();

    if (width == 0 || height == 0) {
      return;
    }

    AffineTransform oldXform = g.getTransform();
    g.transform(new AffineTransform());

    try {
      diagram.render(g);
    }
    catch (SVGException e) {
      throw new RuntimeException(e);
    }

    g.setTransform(oldXform);

    g.translate(-x, -y);
  }

  /**
   * @return the universe this icon draws it's SVGDiagrams from
   */
  public SVGUniverse getSvgUniverse() {
    return svgUniverse;
  }

  public void setSvgUniverse(SVGUniverse svgUniverse) {
    this.svgUniverse = svgUniverse;
  }

  /**
   * @return the uni of the document being displayed by this icon
   */
  public URI getSvgURI() {
    return svgURI;
  }

  /**
   * Loads an SVG document from a URI.
   *
   * @param svgURI - URI to load document from
   */
  public void setSvgURI(URI svgURI) {
    this.svgURI = svgURI;

    SVGDiagram diagram = svgUniverse.getDiagram(svgURI);
    if (diagram != null) {
      Dimension size = getPreferredSize();
      diagram.setDeviceViewport(new Rectangle(0, 0, size.width, size.height));
    }
  }

  private Dimension getPreferredSize() {
    SVGDiagram diagram = svgUniverse.getDiagram(svgURI);
    if (diagram != null) {
      //preferredSize = new Dimension((int)diagram.getWidth(), (int)diagram.getHeight());
      setPreferredSize(new Dimension((int)diagram.getWidth(), (int)diagram.getHeight()));
    }

    return new Dimension();
  }

  private void setPreferredSize(Dimension preferredSize) {
    SVGDiagram diagram = svgUniverse.getDiagram(svgURI);
    if (diagram != null) {
      diagram.setDeviceViewport(new Rectangle(0, 0, preferredSize.width, preferredSize.height));
    }
  }

  public void setImageFilters(Supplier<ImageFilter>[] imageFilters) {
    myImageFilters = imageFilters;
  }
}
