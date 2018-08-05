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
import com.intellij.util.ui.JBUI;
import com.kitfox.svg.SVGCache;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGUniverse;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URI;


/**
 * @author kitfox
 */
class SVGIconImpl extends JBUI.CachingScalableJBIcon<SVGIconImpl> {
  public static final long serialVersionUID = 1;

  public static final String PROP_AUTOSIZE = "PROP_AUTOSIZE";

  private final PropertyChangeSupport changes = new PropertyChangeSupport(this);

  SVGUniverse svgUniverse = SVGCache.getSVGUniverse();

  private boolean clipToViewbox;

  URI svgURI;

  AffineTransform scaleXform = new AffineTransform();

  public static final int AUTOSIZE_NONE = 0;
  public static final int AUTOSIZE_HORIZ = 1;
  public static final int AUTOSIZE_VERT = 2;
  public static final int AUTOSIZE_BESTFIT = 3;
  public static final int AUTOSIZE_STRETCH = 4;
  private int autosize = AUTOSIZE_NONE;

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

  public void addPropertyChangeListener(PropertyChangeListener p) {
    changes.addPropertyChangeListener(p);
  }

  public void removePropertyChangeListener(PropertyChangeListener p) {
    changes.removePropertyChangeListener(p);
  }

  /**
   * @return height of this icon
   */
  public int getIconHeightIgnoreAutosize() {
    SVGDiagram diagram = svgUniverse.getDiagram(svgURI);
    if (diagram == null) {
      return 0;
    }
    return (int)diagram.getHeight();
  }

  /**
   * @return width of this icon
   */

  public int getIconWidthIgnoreAutosize() {

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
   * @param comp - Component to draw icon to.  This is ignored by SVGIcon, and can be set to null; only gg is used for drawing the icon
   * @param gg   - Graphics context to render SVG content to
   * @param x    - X coordinate to draw icon
   * @param y    - Y coordinate to draw icon
   */
  @Override
  public void paintIcon(Component comp, Graphics gg, int x, int y) {
    //Copy graphics object so that
    Graphics2D g = (Graphics2D)gg.create();
    GraphicsConfig config = GraphicsUtil.setupAAPainting(g);
    paintIcon(comp, g, x, y);
    config.restore();
    g.dispose();
  }

  private void paintIcon(Component comp, Graphics2D g, int x, int y) {
    SVGDiagram diagram = svgUniverse.getDiagram(svgURI);
    if (diagram == null) {
      return;
    }

    diagram.setDeviceViewport(new Rectangle(getIconWidth(), getIconHeight()));

    g.translate(x, y);
    diagram.setIgnoringClipHeuristic(!clipToViewbox);
    if (clipToViewbox) {
      g.setClip(new Rectangle2D.Float(0, 0, diagram.getWidth(), diagram.getHeight()));
    }

    final int width = getIconWidthIgnoreAutosize();
    final int height = getIconHeightIgnoreAutosize();

    if (width == 0 || height == 0) {
      return;
    }

    AffineTransform oldXform = g.getTransform();
    g.transform(scaleXform);

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
    SVGUniverse old = this.svgUniverse;
    this.svgUniverse = svgUniverse;
    changes.firePropertyChange("svgUniverse", old, svgUniverse);
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
    URI old = this.svgURI;
    this.svgURI = svgURI;

    SVGDiagram diagram = svgUniverse.getDiagram(svgURI);
    if (diagram != null) {
      Dimension size = getPreferredSize();
      if (size == null) {
        size = new Dimension((int)diagram.getRoot().getDeviceWidth(), (int)diagram.getRoot().getDeviceHeight());
      }
      diagram.setDeviceViewport(new Rectangle(0, 0, size.width, size.height));
    }

    changes.firePropertyChange("svgURI", old, svgURI);
  }

  /**
   * If this SVG document has a viewbox, if scaleToFit is set, will scale the viewbox to match the
   * preferred size of this icon
   *
   * @return
   * @deprecated
   */
  public boolean isScaleToFit() {
    return autosize == AUTOSIZE_STRETCH;
  }

  /**
   * @deprecated
   */
  public void setScaleToFit(boolean scaleToFit) {
    setAutosize(AUTOSIZE_STRETCH);
//        boolean old = this.scaleToFit;
//        this.scaleToFit = scaleToFit;
//        firePropertyChange("scaleToFit", old, scaleToFit);
  }

  public Dimension getPreferredSize() {
    SVGDiagram diagram = svgUniverse.getDiagram(svgURI);
    if (diagram != null) {
      //preferredSize = new Dimension((int)diagram.getWidth(), (int)diagram.getHeight());
      setPreferredSize(new Dimension((int)diagram.getWidth(), (int)diagram.getHeight()));
    }

    return new Dimension();
  }

  public void setPreferredSize(Dimension preferredSize) {

    SVGDiagram diagram = svgUniverse.getDiagram(svgURI);
    if (diagram != null) {
      diagram.setDeviceViewport(new Rectangle(0, 0, preferredSize.width, preferredSize.height));
    }
  }

  /**
   * clipToViewbox will set a clip box equivilant to the SVG's viewbox before
   * rendering.
   */
  public boolean isClipToViewbox() {
    return clipToViewbox;
  }

  public void setClipToViewbox(boolean clipToViewbox) {
    this.clipToViewbox = clipToViewbox;
  }

  /**
   * @return the autosize
   */
  public int getAutosize() {
    return autosize;
  }

  /**
   * @param autosize the autosize to set
   */
  public void setAutosize(int autosize) {
    int oldAutosize = this.autosize;
    this.autosize = autosize;
    changes.firePropertyChange(PROP_AUTOSIZE, oldAutosize, autosize);
  }

}
