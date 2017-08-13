/**
 * $Id: mxGraphTransferable.java,v 1.1 2012/11/15 13:26:46 gaudenz Exp $
 * Copyright (c) 2008, Gaudenz Alder
 */
package com.mxgraph.swing.util;

import com.mxgraph.util.mxRectangle;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.UIResource;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.RenderedImage;
import java.io.*;

/**
 *
 */
public class mxGraphTransferable implements Transferable, UIResource, Serializable {

  /**
   *
   */
  private static final long serialVersionUID = 5123819419918087664L;

  /**
   * Serialized Data Flavor. Use the following code to switch to local
   * reference flavor:
   * <code>
   * try
   * {
   * mxGraphTransferable.dataFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType
   * + "; class=com.mxgraph.swing.util.mxGraphTransferable");
   * }
   * catch (ClassNotFoundException cnfe)
   * {
   * // do nothing
   * }
   * </code>
   * <p/>
   * If you get a class not found exception, try the following instead:
   * <code>
   * mxGraphTransferable.dataFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType
   * + "; class=com.mxgraph.swing.util.mxGraphTransferable", null,
   * new com.mxgraph.swing.util.mxGraphTransferable(null, null).getClass().getClassLoader());
   * </code>
   */
  public static DataFlavor dataFlavor;

  /**
   *
   */
  private static DataFlavor[] htmlFlavors;

  /**
   *
   */
  private static DataFlavor[] stringFlavors;

  /**
   *
   */
  private static DataFlavor[] plainFlavors;

  /**
   *
   */
  private static DataFlavor[] imageFlavors;

  /**
   *
   */
  protected Object[] cells;

  /**
   *
   */
  protected mxRectangle bounds;

  /**
   *
   */
  protected ImageIcon image;

  /**
   *
   */
  public mxGraphTransferable(Object[] cells, mxRectangle bounds) {
    this(cells, bounds, null);
  }

  /**
   *
   */
  public mxGraphTransferable(Object[] cells, mxRectangle bounds, ImageIcon image) {
    this.cells = cells;
    this.bounds = bounds;
    this.image = image;
  }

  /**
   * @return Returns the cells.
   */
  public Object[] getCells() {
    return cells;
  }

  /**
   * Returns the unscaled, untranslated bounding box of the cells.
   */
  public mxRectangle getBounds() {
    return bounds;
  }

  /**
   *
   */
  public ImageIcon getImage() {
    return image;
  }

  /**
   *
   */
  public DataFlavor[] getTransferDataFlavors() {
    DataFlavor[] richerFlavors = getRicherFlavors();

    int nRicher = (richerFlavors != null) ? richerFlavors.length : 0;
    int nHtml = (isHtmlSupported()) ? htmlFlavors.length : 0;
    int nPlain = (isPlainSupported()) ? plainFlavors.length : 0;
    int nString = (isPlainSupported()) ? stringFlavors.length : 0;
    int nImage = (isImageSupported()) ? stringFlavors.length : 0;
    int nFlavors = nRicher + nHtml + nPlain + nString + nImage;

    DataFlavor[] flavors = new DataFlavor[nFlavors];

    // fill in the array
    int nDone = 0;

    if (nRicher > 0) {
      System.arraycopy(richerFlavors, 0, flavors, nDone, nRicher);
      nDone += nRicher;
    }

    if (nHtml > 0) {
      System.arraycopy(htmlFlavors, 0, flavors, nDone, nHtml);
      nDone += nHtml;
    }

    if (nPlain > 0) {
      System.arraycopy(plainFlavors, 0, flavors, nDone, nPlain);
      nDone += nPlain;
    }

    if (nString > 0) {
      System.arraycopy(stringFlavors, 0, flavors, nDone, nString);
      nDone += nString;
    }

    if (nImage > 0) {
      System.arraycopy(imageFlavors, 0, flavors, nDone, nImage);
      nDone += nImage;
    }

    return flavors;
  }

  /**
   * Some subclasses will have flavors that are more descriptive than HTML or
   * plain text. If this method returns a non-null value, it will be placed at
   * the start of the array of supported flavors.
   */
  protected DataFlavor[] getRicherFlavors() {
    return new DataFlavor[]{dataFlavor};
  }

  /**
   * Returns whether or not the specified data flavor is supported for this
   * object.
   *
   * @param flavor the requested flavor for the data
   * @return boolean indicating whether or not the data flavor is supported
   */
  public boolean isDataFlavorSupported(DataFlavor flavor) {
    DataFlavor[] flavors = getTransferDataFlavors();

    for (int i = 0; i < flavors.length; i++) {
      if (flavors[i] != null && flavors[i].equals(flavor)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns an object which represents the data to be transferred. The class
   * of the object returned is defined by the representation class of the
   * flavor.
   *
   * @param flavor the requested flavor for the data
   * @throws IOException                if the data is no longer available in the requested
   *                                    flavor.
   * @throws UnsupportedFlavorException if the requested data flavor is not supported.
   * @see DataFlavor#getRepresentationClass
   */
  public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
    if (isRicherFlavor(flavor)) {
      return getRicherData(flavor);
    }
    else if (isImageFlavor(flavor)) {
      if (image != null && image.getImage() instanceof RenderedImage) {
        if (flavor.equals(DataFlavor.imageFlavor)) {
          return image.getImage();
        }
        else {
          ByteArrayOutputStream stream = new ByteArrayOutputStream();
          ImageIO.write((RenderedImage)image.getImage(), "bmp", stream);

          return new ByteArrayInputStream(stream.toByteArray());
        }
      }
    }
    else if (isHtmlFlavor(flavor)) {
      String data = getHtmlData();
      data = (data == null) ? "" : data;

      if (String.class.equals(flavor.getRepresentationClass())) {
        return data;
      }
      else if (Reader.class.equals(flavor.getRepresentationClass())) {
        return new StringReader(data);
      }
      else if (InputStream.class.equals(flavor.getRepresentationClass())) {
        return new ByteArrayInputStream(data.getBytes());
      }
      // fall through to unsupported
    }
    else if (isPlainFlavor(flavor)) {
      String data = getPlainData();
      data = (data == null) ? "" : data;

      if (String.class.equals(flavor.getRepresentationClass())) {
        return data;
      }
      else if (Reader.class.equals(flavor.getRepresentationClass())) {
        return new StringReader(data);
      }
      else if (InputStream.class.equals(flavor.getRepresentationClass())) {
        return new ByteArrayInputStream(data.getBytes());
      }
      // fall through to unsupported

    }
    else if (isStringFlavor(flavor)) {
      String data = getPlainData();
      data = (data == null) ? "" : data;

      return data;
    }

    throw new UnsupportedFlavorException(flavor);
  }

  /**
   * @param flavor
   * @return Returns true if the given flavor is a richer flavor of this
   *         transferable.
   */
  protected boolean isRicherFlavor(DataFlavor flavor) {
    DataFlavor[] richerFlavors = getRicherFlavors();
    int nFlavors = (richerFlavors != null) ? richerFlavors.length : 0;

    for (int i = 0; i < nFlavors; i++) {
      if (richerFlavors[i].equals(flavor)) {
        return true;
      }
    }

    return false;
  }

  /**
   * @param flavor
   * @return the richer data flavor of this and the specified
   * @throws UnsupportedFlavorException
   */
  public Object getRicherData(DataFlavor flavor) throws UnsupportedFlavorException {
    if (flavor.equals(dataFlavor)) {
      return this;
    }
    else {
      throw new UnsupportedFlavorException(flavor);
    }
  }

  /**
   * Returns whether or not the specified data flavor is an HTML flavor that
   * is supported.
   *
   * @param flavor the requested flavor for the data
   * @return boolean indicating whether or not the data flavor is supported
   */
  protected boolean isHtmlFlavor(DataFlavor flavor) {
    DataFlavor[] flavors = htmlFlavors;

    for (int i = 0; i < flavors.length; i++) {
      if (flavors[i].equals(flavor)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Whether the HTML flavors are offered. If so, the method getHTMLData
   * should be implemented to provide something reasonable.
   */
  protected boolean isHtmlSupported() {
    return false;
  }

  /**
   * Fetch the data in a text/html format
   */
  protected String getHtmlData() {
    return null;
  }

  /**
   * @param flavor
   * @return Returns true if the given flavor is an image flavor of this
   *         transferable.
   */
  protected boolean isImageFlavor(DataFlavor flavor) {
    int nFlavors = (imageFlavors != null) ? imageFlavors.length : 0;

    for (int i = 0; i < nFlavors; i++) {
      if (imageFlavors[i].equals(flavor)) {
        return true;
      }
    }

    return false;
  }

  /**
   *
   */
  public boolean isImageSupported() {
    return image != null;
  }

  /**
   * Returns whether or not the specified data flavor is an plain flavor that
   * is supported.
   *
   * @param flavor the requested flavor for the data
   * @return boolean indicating whether or not the data flavor is supported
   */
  protected boolean isPlainFlavor(DataFlavor flavor) {
    DataFlavor[] flavors = plainFlavors;

    for (int i = 0; i < flavors.length; i++) {
      if (flavors[i].equals(flavor)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Whether the plain text flavors are offered. If so, the method
   * getPlainData should be implemented to provide something reasonable.
   */
  protected boolean isPlainSupported() {
    return false;
  }

  /**
   * Fetch the data in a text/plain format.
   */
  protected String getPlainData() {
    return null;
  }

  /**
   * Returns whether or not the specified data flavor is a String flavor that
   * is supported.
   *
   * @param flavor the requested flavor for the data
   * @return boolean indicating whether or not the data flavor is supported
   */
  protected boolean isStringFlavor(DataFlavor flavor) {
    DataFlavor[] flavors = stringFlavors;

    for (int i = 0; i < flavors.length; i++) {
      if (flavors[i].equals(flavor)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Local Machine Reference Data Flavor.
   */
  static {
    try {
      htmlFlavors = new DataFlavor[3];
      htmlFlavors[0] = new DataFlavor("text/html;class=java.lang.String");
      htmlFlavors[1] = new DataFlavor("text/html;class=java.io.Reader");
      htmlFlavors[2] = new DataFlavor("text/html;charset=unicode;class=java.io.InputStream");

      plainFlavors = new DataFlavor[3];
      plainFlavors[0] = new DataFlavor("text/plain;class=java.lang.String");
      plainFlavors[1] = new DataFlavor("text/plain;class=java.io.Reader");
      plainFlavors[2] = new DataFlavor("text/plain;charset=unicode;class=java.io.InputStream");

      stringFlavors = new DataFlavor[2];
      stringFlavors[0] = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=java.lang.String");
      stringFlavors[1] = DataFlavor.stringFlavor;

      imageFlavors = new DataFlavor[2];
      imageFlavors[0] = DataFlavor.imageFlavor;
      imageFlavors[1] = new DataFlavor("image/bmp");
    }
    catch (ClassNotFoundException cle) {
      System.err.println("error initializing javax.swing.plaf.basic.BasicTranserable");
    }

    try {
      dataFlavor = new DataFlavor(DataFlavor.javaSerializedObjectMimeType + "; class=com.mxgraph.swing.util.mxGraphTransferable");
    }
    catch (ClassNotFoundException cnfe) {
      // do nothing
    }
  }

}
