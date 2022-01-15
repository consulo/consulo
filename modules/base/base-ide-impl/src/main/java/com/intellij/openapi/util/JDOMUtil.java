/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.util;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.Interner;
import com.intellij.util.text.CharArrayUtil;
import consulo.annotation.internal.MigratedExtensionsTo;
import consulo.logging.Logger;
import consulo.util.jdom.interner.JDOMInterner;
import org.jdom.*;
import org.jdom.filter.AbstractFilter;
import org.jdom.filter.ElementFilter;
import org.jdom.output.XMLOutputter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.List;

/**
 * @author mike
 */
@SuppressWarnings("MigratedExtensionsTo")
@MigratedExtensionsTo(consulo.util.jdom.JDOMUtil.class)
public class JDOMUtil {
  private static final String X = "x";
  private static final String Y = "y";
  private static final String WIDTH = "width";
  private static final String HEIGHT = "height";

  private JDOMUtil() {
  }

  @Nonnull
  public static List<Element> getChildren(@Nullable Element parent) {
    return consulo.util.jdom.JDOMUtil.getChildren(parent);
  }

  @Nonnull
  public static List<Element> getChildren(@Nullable Element parent, @Nonnull String name) {
    return consulo.util.jdom.JDOMUtil.getChildren(parent, name);
  }

  @SuppressWarnings("UtilityClassWithoutPrivateConstructor")
  private static class LoggerHolder {
    private static final Logger ourLogger = Logger.getInstance(JDOMUtil.class);
  }

  private static Logger getLogger() {
    return LoggerHolder.ourLogger;
  }

  public static boolean areElementsEqual(@Nullable Element e1, @Nullable Element e2) {
    return consulo.util.jdom.JDOMUtil.areElementsEqual(e1, e2);
  }

  public static int getTreeHash(@Nonnull Element root) {
    return consulo.util.jdom.JDOMUtil.getTreeHash(root);
  }

  public static int getTreeHash(@Nonnull Element root, boolean skipEmptyText) {
    return consulo.util.jdom.JDOMUtil.getTreeHash(root, skipEmptyText);
  }

  @SuppressWarnings("unused")
  @Deprecated
  public static int getTreeHash(@Nonnull Document document) {
    return consulo.util.jdom.JDOMUtil.getTreeHash(document);
  }

  private static int addToHash(int i, @Nonnull Element element, boolean skipEmptyText) {
    i = addToHash(i, element.getName());

    for (Attribute attribute : element.getAttributes()) {
      i = addToHash(i, attribute.getName());
      i = addToHash(i, attribute.getValue());
    }

    for (Content child : element.getContent()) {
      if (child instanceof Element) {
        i = addToHash(i, (Element)child, skipEmptyText);
      }
      else if (child instanceof Text) {
        String text = ((Text)child).getText();
        if (!skipEmptyText || !StringUtil.isEmptyOrSpaces(text)) {
          i = addToHash(i, text);
        }
      }
    }
    return i;
  }

  private static int addToHash(final int i, @Nonnull final String s) {
    return i * 31 + s.hashCode();
  }

  @SuppressWarnings("unused")
  @Nonnull
  @Deprecated
  /**
   * to remove in IDEA 15
   */ public static Object[] getChildNodesWithAttrs(@Nonnull Element e) {
    return consulo.util.jdom.JDOMUtil.getChildNodesWithAttrs(e);
  }

  @Nonnull
  public static Content[] getContent(@Nonnull Element m) {
    return consulo.util.jdom.JDOMUtil.getContent(m);
  }

  @Nonnull
  public static Element[] getElements(@Nonnull Element m) {
    return consulo.util.jdom.JDOMUtil.getElements(m);
  }

  @Deprecated
  @SuppressWarnings("unused")
  @Nonnull
  public static String concatTextNodesValues(@Nonnull final Object[] nodes) {
    return consulo.util.jdom.JDOMUtil.concatTextNodesValues(nodes);
  }

  public static void addContent(@Nonnull final Element targetElement, final Object node) {
    consulo.util.jdom.JDOMUtil.addContent(targetElement, node);
  }

  /**
   * Replace all strings in JDOM {@code element} with their interned variants with the help of {@code interner} to reduce memory.
   * It's better to use {@link #internElement(Element)} though because the latter will intern the Element instances too.
   */
  public static void internStringsInElement(@Nonnull Element element, @Nonnull Interner<String> interner) {
    element.setName(intern(interner, element.getName()));

    for (Attribute attr : element.getAttributes()) {
      attr.setName(intern(interner, attr.getName()));
      attr.setValue(intern(interner, attr.getValue()));
    }

    for (Content o : element.getContent()) {
      if (o instanceof Element) {
        Element e = (Element)o;
        internStringsInElement(e, interner);
      }
      else if (o instanceof Text) {
        Text text = (Text)o;
        text.setText(intern(interner, text.getText()));
      }
      else if (o instanceof Comment) {
        Comment comment = (Comment)o;
        comment.setText(intern(interner, comment.getText()));
      }
      else {
        throw new IllegalArgumentException("Wrong node: " + o);
      }
    }
  }

  @Nonnull
  private static String intern(@Nonnull final Interner<String> interner, @Nonnull final String s) {
    return interner.intern(s);
  }

  @Nonnull
  public static String legalizeText(@Nonnull String str) {
    return consulo.util.jdom.JDOMUtil.legalizeText(str);
  }

  @Nonnull
  public static CharSequence legalizeChars(@Nonnull CharSequence str) {
    return consulo.util.jdom.JDOMUtil.legalizeChars(str);
  }

  public static void appendLegalized(@Nonnull StringBuilder sb, char each) {
    consulo.util.jdom.JDOMUtil.appendLegalized(sb, each);
  }

  private static class EmptyTextFilter extends AbstractFilter<Content> {
    @Override
    public Content filter(Object obj) {
      if (obj instanceof Text && CharArrayUtil.containsOnlyWhiteSpaces(((Text)obj).getText())) {
        return null;
      }
      return (Content)obj;
    }
  }

  private static boolean attEqual(@Nonnull Attribute a1, @Nonnull Attribute a2) {
    return a1.getName().equals(a2.getName()) && a1.getValue().equals(a2.getValue());
  }

  public static boolean areDocumentsEqual(@Nonnull Document d1, @Nonnull Document d2) {
    return consulo.util.jdom.JDOMUtil.areDocumentsEqual(d1, d2);
  }

  @Nonnull
  public static Document loadDocument(char[] chars, int length) throws IOException, JDOMException {
    return consulo.util.jdom.JDOMUtil.loadDocument(chars, length);
  }

  @Nonnull
  public static Document loadDocument(byte[] bytes) throws IOException, JDOMException {
    return consulo.util.jdom.JDOMUtil.loadDocument(bytes);
  }
  @Nonnull
  public static Document loadDocument(@Nonnull CharSequence seq) throws IOException, JDOMException {
    return consulo.util.jdom.JDOMUtil.loadDocument(seq);
  }

  @Nonnull
  public static Document loadDocument(@Nonnull Reader reader) throws IOException, JDOMException {
    return consulo.util.jdom.JDOMUtil.loadDocument(reader);
  }

  @Nonnull
  public static Document loadDocument(File file) throws JDOMException, IOException {
    return consulo.util.jdom.JDOMUtil.loadDocument(file);
  }

  @Nonnull
  public static Element load(@Nonnull File file) throws JDOMException, IOException {
    return consulo.util.jdom.JDOMUtil.load(file);
  }

  @Nonnull
  public static Document loadDocument(@Nonnull InputStream stream) throws JDOMException, IOException {
    return consulo.util.jdom.JDOMUtil.loadDocument(stream);
  }

  @Contract("null -> null; !null -> !null")
  public static Element load(Reader reader) throws JDOMException, IOException {
    return consulo.util.jdom.JDOMUtil.load(reader);
  }

  @Contract("null -> null; !null -> !null")
  public static Element load(URL url) throws JDOMException, IOException {
    return consulo.util.jdom.JDOMUtil.load(url);
  }

  @Contract("null -> null; !null -> !null")
  public static Element load(InputStream stream) throws JDOMException, IOException {
    return consulo.util.jdom.JDOMUtil.load(stream);
  }

  @Nonnull
  public static Document loadDocument(@Nonnull Class clazz, String resource) throws JDOMException, IOException {
    return consulo.util.jdom.JDOMUtil.loadDocument(clazz, resource);
  }

  @Nonnull
  public static Document loadDocument(URL url) throws JDOMException, IOException {
    return consulo.util.jdom.JDOMUtil.loadDocument(url);
  }

  @Nonnull
  public static Document loadResourceDocument(URL url) throws JDOMException, IOException {
    return consulo.util.jdom.JDOMUtil.loadResourceDocument(url);
  }

  public static void writeDocument(@Nonnull Document document, @Nonnull String filePath, String lineSeparator) throws IOException {
    consulo.util.jdom.JDOMUtil.writeDocument(document, filePath, lineSeparator);
  }

  public static void writeDocument(@Nonnull Document document, @Nonnull File file, String lineSeparator) throws IOException {
    consulo.util.jdom.JDOMUtil.writeDocument(document, file, lineSeparator);
  }

  public static void writeParent(@Nonnull Parent element, @Nonnull File file, String lineSeparator) throws IOException {
    consulo.util.jdom.JDOMUtil.writeParent(element, file, lineSeparator);
  }

  public static void writeDocument(@Nonnull Document document, @Nonnull OutputStream stream, String lineSeparator) throws IOException {
    consulo.util.jdom.JDOMUtil.writeDocument(document, stream, lineSeparator);
  }

  public static void writeParent(@Nonnull Parent element, @Nonnull OutputStream stream, @Nonnull String lineSeparator) throws IOException {
    consulo.util.jdom.JDOMUtil.writeParent(element, stream, lineSeparator);
  }

  @Nonnull
  public static byte[] printDocument(@Nonnull Document document, String lineSeparator) throws IOException {
    return consulo.util.jdom.JDOMUtil.printDocument(document, lineSeparator);
  }

  @Nonnull
  public static String writeDocument(@Nonnull Document document, String lineSeparator) {
    return consulo.util.jdom.JDOMUtil.writeDocument(document, lineSeparator);
  }

  @Nonnull
  public static String writeParent(Parent element, String lineSeparator) {
    return consulo.util.jdom.JDOMUtil.writeParent(element, lineSeparator);
  }

  public static void writeParent(Parent element, Writer writer, String lineSeparator) throws IOException {
    consulo.util.jdom.JDOMUtil.writeParent(element, writer, lineSeparator);
  }

  public static void writeElement(@Nonnull Element element, Writer writer, String lineSeparator) throws IOException {
    consulo.util.jdom.JDOMUtil.writeElement(element, writer, lineSeparator);
  }

  @Nonnull
  public static String writeElement(@Nonnull Element element) {
    return consulo.util.jdom.JDOMUtil.writeElement(element);
  }

  @Nonnull
  public static String writeElement(@Nonnull Element element, String lineSeparator) {
    return consulo.util.jdom.JDOMUtil.writeElement(element, lineSeparator);
  }

  @Nonnull
  public static String writeChildren(@Nullable final Element element, @Nonnull final String lineSeparator) throws IOException {
    return consulo.util.jdom.JDOMUtil.writeChildren(element, lineSeparator);
  }

  public static void writeDocument(@Nonnull Document document, @Nonnull Writer writer, String lineSeparator) throws IOException {
    consulo.util.jdom.JDOMUtil.writeDocument(document, writer, lineSeparator);
  }

  @Nonnull
  public static XMLOutputter createOutputter(String lineSeparator) {
    return consulo.util.jdom.JDOMUtil.createOutputter(lineSeparator);
  }

  /**
   * Returns null if no escapement necessary.
   */
  @Nullable
  private static String escapeChar(char c, boolean escapeApostrophes, boolean escapeSpaces, boolean escapeLineEnds) {
    switch (c) {
      case '\n':
        return escapeLineEnds ? "&#10;" : null;
      case '\r':
        return escapeLineEnds ? "&#13;" : null;
      case '\t':
        return escapeLineEnds ? "&#9;" : null;
      case ' ':
        return escapeSpaces ? "&#20" : null;
      case '<':
        return "&lt;";
      case '>':
        return "&gt;";
      case '\"':
        return "&quot;";
      case '\'':
        return escapeApostrophes ? "&apos;" : null;
      case '&':
        return "&amp;";
    }
    return null;
  }

  @Nonnull
  public static String escapeText(@Nonnull String text) {
    return consulo.util.jdom.JDOMUtil.escapeText(text);
  }

  @Nonnull
  public static String escapeText(@Nonnull String text, boolean escapeSpaces, boolean escapeLineEnds) {
    return consulo.util.jdom.JDOMUtil.escapeText(text, escapeSpaces, escapeLineEnds);
  }

  @Nonnull
  public static String escapeText(@Nonnull String text, boolean escapeApostrophes, boolean escapeSpaces, boolean escapeLineEnds) {
    return consulo.util.jdom.JDOMUtil.escapeText(text, escapeApostrophes, escapeSpaces, escapeLineEnds);
  }

  @SuppressWarnings("unused")
  @Deprecated
  @Nonnull
  public static List<Element> getChildrenFromAllNamespaces(@Nonnull final Element element, @Nonnull @NonNls final String name) {
    return consulo.util.jdom.JDOMUtil.getChildrenFromAllNamespaces(element, name);
  }

  public static XMLOutputter newXmlOutputter() {
    return consulo.util.jdom.JDOMUtil.newXmlOutputter();
  }

  private static void printDiagnostics(@Nonnull Element element, String prefix) {
    ElementInfo info = getElementInfo(element);
    prefix += "/" + info.name;
    if (info.hasNullAttributes) {
      //noinspection UseOfSystemOutOrSystemErr
      System.err.println(prefix);
    }

    List<Element> children = getChildren(element);
    for (final Element child : children) {
      printDiagnostics(child, prefix);
    }
  }

  @Nonnull
  private static ElementInfo getElementInfo(@Nonnull Element element) {
    ElementInfo info = new ElementInfo();
    StringBuilder buf = new StringBuilder(element.getName());
    List attributes = element.getAttributes();
    if (attributes != null) {
      int length = attributes.size();
      if (length > 0) {
        buf.append("[");
        for (int idx = 0; idx < length; idx++) {
          Attribute attr = (Attribute)attributes.get(idx);
          if (idx != 0) {
            buf.append(";");
          }
          buf.append(attr.getName());
          buf.append("=");
          buf.append(attr.getValue());
          if (attr.getValue() == null) {
            info.hasNullAttributes = true;
          }
        }
        buf.append("]");
      }
    }
    info.name = buf.toString();
    return info;
  }

  public static void updateFileSet(@Nonnull File[] oldFiles, @Nonnull String[] newFilePaths, @Nonnull Document[] newFileDocuments, String lineSeparator) throws IOException {
    consulo.util.jdom.JDOMUtil.updateFileSet(oldFiles, newFilePaths, newFileDocuments, lineSeparator);
  }

  private static class ElementInfo {
    @Nonnull
    public String name = "";
    public boolean hasNullAttributes = false;
  }

  @SuppressWarnings("unused")
  @Deprecated
  public static org.w3c.dom.Element convertToDOM(@Nonnull Element e) {
    return consulo.util.jdom.JDOMUtil.convertToDOM(e);
  }

  @SuppressWarnings("unused")
  @Deprecated
  public static Element convertFromDOM(org.w3c.dom.Element e) {
    return consulo.util.jdom.JDOMUtil.convertFromDOM(e);
  }

  public static String getValue(Object node) {
    return consulo.util.jdom.JDOMUtil.getValue(node);
  }

  @SuppressWarnings("unused")
  @Nullable
  @Deprecated
  public static Element cloneElement(@Nonnull Element element, @Nonnull ElementFilter elementFilter) {
    return consulo.util.jdom.JDOMUtil.cloneElement(element, elementFilter);
  }

  public static boolean isEmpty(@Nullable Element element) {
    return consulo.util.jdom.JDOMUtil.isEmpty(element);
  }

  private static final JDOMInterner ourJDOMInterner = new JDOMInterner();

  /**
   * Interns {@code element} to reduce instance count of many identical Elements created after loading JDOM document to memory.
   * For example, after interning <pre>{@code
   * <app>
   *   <component load="true" isDefault="true" name="comp1"/>
   *   <component load="true" isDefault="true" name="comp2"/>
   * </app>}</pre>
   * <p>
   * there will be created just one XmlText("\n  ") instead of three for whitespaces between tags,
   * one Attribute("load=true") instead of two equivalent for each component tag etc.
   *
   * <p><h3>Intended usage:</h3>
   * - When you need to keep some part of JDOM tree in memory, use this method before save the element to some collection,
   * E.g.: <pre>{@code
   *   public void readExternal(final Element element) {
   *     myStoredElement = JDOMUtil.internElement(element);
   *   }
   *   }</pre>
   * - When you need to save interned element back to JDOM and/or modify/add it, use {@link ImmutableElement#clone()}
   * to obtain mutable modifiable Element.
   * E.g.: <pre>{@code
   *   void writeExternal(Element element) {
   *     for (Attribute a : myStoredElement.getAttributes()) {
   *       element.setAttribute(a.getName(), a.getValue()); // String getters work as before
   *     }
   *     for (Element child : myStoredElement.getChildren()) {
   *       element.addContent(child.clone()); // need to call clone() before modifying/adding
   *     }
   *   }
   *   }</pre>
   *
   * @return interned Element, i.e Element which<br/>
   * - is the same for equivalent parameters. E.g. two calls of internElement() with {@code <xxx/>} and the other {@code <xxx/>}
   * will return the same element {@code <xxx/>}<br/>
   * - getParent() method is not implemented (and will throw exception; interning would not make sense otherwise)<br/>
   * - is immutable (all modifications methods like setName(), setParent() etc will throw)<br/>
   * - has {@code clone()} method which will return modifiable org.jdom.Element copy.<br/>
   */
  @Nonnull
  public static Element internElement(@Nonnull Element element) {
    return ourJDOMInterner.internElement(element);
  }

  /**
   * Not required if you use XmlSerializator.
   *
   * @see XmlStringUtil#escapeIllegalXmlChars(String)
   */
  @Nonnull
  public static String removeControlChars(@Nonnull String text) {
    return consulo.util.jdom.JDOMUtil.removeControlChars(text);
  }


  @Nullable
  public static Point getLocation(@Nullable Element element) {
    return element == null ? null : getLocation(element, X, Y);
  }

  @Nullable
  public static Point getLocation(@Nonnull Element element, @Nonnull String x, @Nonnull String y) {
    String sX = element.getAttributeValue(x);
    if (sX == null) return null;
    String sY = element.getAttributeValue(y);
    if (sY == null) return null;
    try {
      return new Point(Integer.parseInt(sX), Integer.parseInt(sY));
    }
    catch (NumberFormatException ignored) {
      return null;
    }
  }

  @Nonnull
  public static Element setLocation(@Nonnull Element element, @Nonnull Point location) {
    return setLocation(element, X, Y, location);
  }

  @Nonnull
  public static Element setLocation(@Nonnull Element element, @Nonnull String x, @Nonnull String y, @Nonnull Point location) {
    return element.setAttribute(x, Integer.toString(location.x)).setAttribute(y, Integer.toString(location.y));
  }


  @Nullable
  public static Dimension getSize(@Nullable Element element) {
    return element == null ? null : getSize(element, WIDTH, HEIGHT);
  }

  @Nullable
  public static Dimension getSize(@Nonnull Element element, @Nonnull String width, @Nonnull String height) {
    String sWidth = element.getAttributeValue(width);
    if (sWidth == null) return null;
    String sHeight = element.getAttributeValue(height);
    if (sHeight == null) return null;
    try {
      int iWidth = Integer.parseInt(sWidth);
      if (iWidth <= 0) return null;
      int iHeight = Integer.parseInt(sHeight);
      if (iHeight <= 0) return null;
      return new Dimension(iWidth, iHeight);
    }
    catch (NumberFormatException ignored) {
      return null;
    }
  }

  @Nonnull
  public static Element setSize(@Nonnull Element element, @Nonnull Dimension size) {
    return setSize(element, WIDTH, HEIGHT, size);
  }

  @Nonnull
  public static Element setSize(@Nonnull Element element, @Nonnull String width, @Nonnull String height, @Nonnull Dimension size) {
    return element.setAttribute(width, Integer.toString(size.width)).setAttribute(height, Integer.toString(size.height));
  }


  @Nullable
  public static Rectangle getBounds(@Nullable Element element) {
    return element == null ? null : getBounds(element, X, Y, WIDTH, HEIGHT);
  }

  @Nullable
  public static Rectangle getBounds(@Nonnull Element element, @Nonnull String x, @Nonnull String y, @Nonnull String width, @Nonnull String height) {
    String sX = element.getAttributeValue(x);
    if (sX == null) return null;
    String sY = element.getAttributeValue(y);
    if (sY == null) return null;
    String sWidth = element.getAttributeValue(width);
    if (sWidth == null) return null;
    String sHeight = element.getAttributeValue(height);
    if (sHeight == null) return null;
    try {
      int iWidth = Integer.parseInt(sWidth);
      if (iWidth <= 0) return null;
      int iHeight = Integer.parseInt(sHeight);
      if (iHeight <= 0) return null;
      return new Rectangle(Integer.parseInt(sX), Integer.parseInt(sY), iWidth, iHeight);
    }
    catch (NumberFormatException ignored) {
      return null;
    }
  }

  @Nonnull
  public static Element setBounds(@Nonnull Element element, @Nonnull Rectangle bounds) {
    return setBounds(element, X, Y, WIDTH, HEIGHT, bounds);
  }

  @Nonnull
  public static Element setBounds(@Nonnull Element element, @Nonnull String x, @Nonnull String y, @Nonnull String width, @Nonnull String height, @Nonnull Rectangle bounds) {
    return element.setAttribute(x, Integer.toString(bounds.x)).setAttribute(y, Integer.toString(bounds.y)).setAttribute(width, Integer.toString(bounds.width))
            .setAttribute(height, Integer.toString(bounds.height));
  }
}
