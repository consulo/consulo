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
package consulo.util.jdom;


import consulo.util.collection.ArrayUtil;
import consulo.util.io.CharSequenceReader;
import consulo.util.io.URLUtil;
import consulo.util.lang.CharArrayUtil;
import consulo.util.lang.StringUtil;
import org.jdom.*;
import org.jdom.filter.AbstractFilter;
import org.jdom.filter.ElementFilter;
import org.jdom.input.DOMBuilder;
import org.jdom.input.SAXBuilder;
import org.jdom.output.DOMOutputter;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.output.support.AbstractXMLOutputProcessor;
import org.jdom.output.support.FormatStack;
import org.jdom.output.support.Walker;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.*;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author mike
 */
@SuppressWarnings("unused")
public class JDOMUtil {
  private static class LoggerHolder {
    private static final Logger ourLogger = LoggerFactory.getLogger(JDOMUtil.class);
  }

  private static Logger getLogger() {
    return LoggerHolder.ourLogger;
  }

  private static final ThreadLocal<SoftReference<SAXBuilder>> ourSaxBuilder = new ThreadLocal<>();

  private JDOMUtil() {
  }

  @Nonnull
  public static List<Element> getChildren(@Nullable Element parent) {
    if (parent == null) {
      return Collections.emptyList();
    }
    else {
      return parent.getChildren();
    }
  }

  @Nonnull
  public static List<Element> getChildren(@Nullable Element parent, @Nonnull String name) {
    if (parent != null) {
      return parent.getChildren(name);
    }
    return Collections.emptyList();
  }

  public static boolean areElementsEqual(@Nullable Element e1, @Nullable Element e2) {
    if (e1 == null && e2 == null) return true;
    if (e1 == null || e2 == null) return false;

    return Objects.equals(e1.getName(), e2.getName()) && attListsEqual(e1.getAttributes(), e2.getAttributes()) && contentListsEqual(e1.getContent(CONTENT_FILTER), e2.getContent(CONTENT_FILTER));
  }

  private static final EmptyTextFilter CONTENT_FILTER = new EmptyTextFilter();

  public static int getTreeHash(@Nonnull Element root) {
    return getTreeHash(root, false);
  }

  public static int getTreeHash(@Nonnull Element root, boolean skipEmptyText) {
    return addToHash(0, root, skipEmptyText);
  }

  @SuppressWarnings("unused")
  @Deprecated
  public static int getTreeHash(@Nonnull Document document) {
    return getTreeHash(document.getRootElement());
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

  private static int addToHash(int i, @Nonnull String s) {
    return i * 31 + s.hashCode();
  }

  @SuppressWarnings("unused")
  @Nonnull
  @Deprecated
  /**
   * to remove in IDEA 15
   */ public static Object[] getChildNodesWithAttrs(@Nonnull Element e) {
    ArrayList<Object> result = new ArrayList<>();
    result.addAll(e.getContent());
    result.addAll(e.getAttributes());
    return ArrayUtil.toObjectArray(result);
  }

  @Nonnull
  public static Content[] getContent(@Nonnull Element m) {
    List<Content> list = m.getContent();
    return list.toArray(new Content[list.size()]);
  }

  @Nonnull
  public static Element[] getElements(@Nonnull Element m) {
    List<Element> list = m.getChildren();
    return list.toArray(new Element[list.size()]);
  }

  @Deprecated
  @SuppressWarnings("unused")
  @Nonnull
  public static String concatTextNodesValues(@Nonnull Object[] nodes) {
    StringBuilder result = new StringBuilder();
    for (Object node : nodes) {
      result.append(((Content)node).getValue());
    }
    return result.toString();
  }

  public static void addContent(@Nonnull Element targetElement, Object node) {
    if (node instanceof Content) {
      Content content = (Content)node;
      targetElement.addContent(content);
    }
    else if (node instanceof List) {
      //noinspection unchecked
      targetElement.addContent((List)node);
    }
    else {
      throw new IllegalArgumentException("Wrong node: " + node);
    }
  }

  /**
   * Replace all strings in JDOM {@code element} with their interned variants with the help of {@code interner} to reduce memory.
   * It's better to use {@link #internElement(Element)} though because the latter will intern the Element instances too.
   */
  //public static void internStringsInElement(@Nonnull Element element, @Nonnull StringInterner interner) {
  //  element.setName(intern(interner, element.getName()));
  //
  //  for (Attribute attr : element.getAttributes()) {
  //    attr.setName(intern(interner, attr.getName()));
  //    attr.setValue(intern(interner, attr.getValue()));
  //  }
  //
  //  for (Content o : element.getContent()) {
  //    if (o instanceof Element) {
  //      Element e = (Element)o;
  //      internStringsInElement(e, interner);
  //    }
  //    else if (o instanceof Text) {
  //      Text text = (Text)o;
  //      text.setText(intern(interner, text.getText()));
  //    }
  //    else if (o instanceof Comment) {
  //      Comment comment = (Comment)o;
  //      comment.setText(intern(interner, comment.getText()));
  //    }
  //    else {
  //      throw new IllegalArgumentException("Wrong node: " + o);
  //    }
  //  }
  //}
  //
  //@Nonnull
  //private static String intern(@Nonnull final StringInterner interner, @Nonnull final String s) {
  //  return interner.intern(s);
  //}
  @Nonnull
  public static String legalizeText(@Nonnull String str) {
    return legalizeChars(str).toString();
  }

  @Nonnull
  public static CharSequence legalizeChars(@Nonnull CharSequence str) {
    StringBuilder result = new StringBuilder(str.length());
    for (int i = 0, len = str.length(); i < len; i++) {
      appendLegalized(result, str.charAt(i));
    }
    return result;
  }

  public static void appendLegalized(@Nonnull StringBuilder sb, char each) {
    if (each == '<' || each == '>') {
      sb.append(each == '<' ? "&lt;" : "&gt;");
    }
    else if (!Verifier.isXMLCharacter(each)) {
      sb.append("0x").append(StringUtil.toUpperCase(Long.toHexString(each)));
    }
    else {
      sb.append(each);
    }
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

  private static boolean contentListsEqual(List c1, List c2) {
    if (c1 == null && c2 == null) return true;
    if (c1 == null || c2 == null) return false;

    Iterator l1 = c1.listIterator();
    Iterator l2 = c2.listIterator();
    while (l1.hasNext() && l2.hasNext()) {
      if (!contentsEqual((Content)l1.next(), (Content)l2.next())) {
        return false;
      }
    }

    return l1.hasNext() == l2.hasNext();
  }

  private static boolean contentsEqual(Content c1, Content c2) {
    if (!(c1 instanceof Element) && !(c2 instanceof Element)) {
      return c1.getValue().equals(c2.getValue());
    }

    return c1 instanceof Element && c2 instanceof Element && areElementsEqual((Element)c1, (Element)c2);
  }

  private static boolean attListsEqual(@Nonnull List a1, @Nonnull List a2) {
    if (a1.size() != a2.size()) return false;
    for (int i = 0; i < a1.size(); i++) {
      if (!attEqual((Attribute)a1.get(i), (Attribute)a2.get(i))) return false;
    }
    return true;
  }

  private static boolean attEqual(@Nonnull Attribute a1, @Nonnull Attribute a2) {
    return a1.getName().equals(a2.getName()) && a1.getValue().equals(a2.getValue());
  }

  public static boolean areDocumentsEqual(@Nonnull Document d1, @Nonnull Document d2) {
    if (d1.hasRootElement() != d2.hasRootElement()) return false;

    if (!d1.hasRootElement()) return true;

    CharArrayWriter w1 = new CharArrayWriter();
    CharArrayWriter w2 = new CharArrayWriter();

    try {
      writeDocument(d1, w1, "\n");
      writeDocument(d2, w2, "\n");
    }
    catch (IOException e) {
      getLogger().error(e.getMessage(), e);
    }

    return w1.size() == w2.size() && w1.toString().equals(w2.toString());
  }

  @Nonnull
  public static Document loadDocument(char[] chars, int length) throws IOException, JDOMException {
    return getSaxBuilder().build(new CharArrayReader(chars, 0, length));
  }

  @Nonnull
  public static Document loadDocument(byte[] bytes) throws IOException, JDOMException {
    return loadDocument(new ByteArrayInputStream(bytes));
  }

  private static SAXBuilder getSaxBuilder() {
    SoftReference<SAXBuilder> reference = ourSaxBuilder.get();
    SAXBuilder saxBuilder = consulo.util.lang.ref.SoftReference.dereference(reference);
    if (saxBuilder == null) {
      saxBuilder = new SAXBuilder();
      saxBuilder.setEntityResolver(new EntityResolver() {
        @Override
        @Nonnull
        public InputSource resolveEntity(String publicId, String systemId) {
          return new InputSource(new CharArrayReader(ArrayUtil.EMPTY_CHAR_ARRAY));
        }
      });
      ourSaxBuilder.set(new SoftReference<>(saxBuilder));
    }
    return saxBuilder;
  }

  @Nonnull
  public static Document loadDocument(@Nonnull CharSequence seq) throws IOException, JDOMException {
    return loadDocument(new CharSequenceReader(seq));
  }

  @Nonnull
  public static Document loadDocument(@Nonnull Reader reader) throws IOException, JDOMException {
    try {
      return getSaxBuilder().build(reader);
    }
    finally {
      reader.close();
    }
  }

  @Nonnull
  public static Document loadDocument(File file) throws JDOMException, IOException {
    return loadDocument(new BufferedInputStream(new FileInputStream(file)));
  }

  @Nonnull
  public static Element load(@Nonnull File file) throws JDOMException, IOException {
    return load(new BufferedInputStream(new FileInputStream(file)));
  }

  @Nonnull
  public static Document loadDocument(@Nonnull InputStream stream) throws JDOMException, IOException {
    return loadDocument(new InputStreamReader(stream, StandardCharsets.UTF_8));
  }

  @Contract("null -> null; !null -> !null")
  public static Element load(Reader reader) throws JDOMException, IOException {
    return reader == null ? null : loadDocument(reader).detachRootElement();
  }

  @Contract("null -> null; !null -> !null")
  public static Element load(URL url) throws JDOMException, IOException {
    return url == null ? null : loadDocument(url).detachRootElement();
  }

  @Contract("null -> null; !null -> !null")
  public static Element load(InputStream stream) throws JDOMException, IOException {
    return stream == null ? null : loadDocument(stream).detachRootElement();
  }

  @Nonnull
  public static Document loadDocument(@Nonnull Class clazz, String resource) throws JDOMException, IOException {
    InputStream stream = clazz.getResourceAsStream(resource);
    if (stream == null) {
      throw new FileNotFoundException(resource);
    }
    return loadDocument(stream);
  }

  @Nonnull
  public static Document loadDocument(URL url) throws JDOMException, IOException {
    return loadDocument(URLUtil.openStream(url));
  }

  @Nonnull
  public static Document loadResourceDocument(URL url) throws JDOMException, IOException {
    return loadDocument(URLUtil.openResourceStream(url));
  }

  public static void writeDocument(@Nonnull Document document, @Nonnull String filePath, String lineSeparator) throws IOException {
    try (OutputStream stream = new BufferedOutputStream(new FileOutputStream(filePath))) {
      writeDocument(document, stream, lineSeparator);
    }
  }

  public static void writeDocument(@Nonnull Document document, @Nonnull File file, String lineSeparator) throws IOException {
    writeParent(document, file, lineSeparator);
  }

  public static void writeParent(@Nonnull Parent element, @Nonnull File file, String lineSeparator) throws IOException {
    try (OutputStream stream = new BufferedOutputStream(new FileOutputStream(file))) {
      writeParent(element, stream, lineSeparator);
    }
  }

  public static void writeDocument(@Nonnull Document document, @Nonnull OutputStream stream, String lineSeparator) throws IOException {
    writeParent(document, stream, lineSeparator);
  }

  public static void writeParent(@Nonnull Parent element, @Nonnull OutputStream stream, @Nonnull String lineSeparator) throws IOException {
    try (OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
      if (element instanceof Document) {
        writeDocument((Document)element, writer, lineSeparator);
      }
      else {
        writeElement((Element)element, writer, lineSeparator);
      }
    }
  }

  @Nonnull
  public static byte[] printDocument(@Nonnull Document document, String lineSeparator) throws IOException {
    CharArrayWriter writer = new CharArrayWriter();
    writeDocument(document, writer, lineSeparator);
    return writer.toString().getBytes(StandardCharsets.UTF_8);
  }

  @Nonnull
  public static String writeDocument(@Nonnull Document document, String lineSeparator) {
    try {
      StringWriter writer = new StringWriter();
      writeDocument(document, writer, lineSeparator);
      return writer.toString();
    }
    catch (IOException ignored) {
      // Can't be
      return "";
    }
  }

  @Nonnull
  public static String writeParent(Parent element, String lineSeparator) {
    try {
      StringWriter writer = new StringWriter();
      writeParent(element, writer, lineSeparator);
      return writer.toString();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void writeParent(Parent element, Writer writer, String lineSeparator) throws IOException {
    if (element instanceof Element) {
      writeElement((Element)element, writer, lineSeparator);
    }
    else if (element instanceof Document) {
      writeDocument((Document)element, writer, lineSeparator);
    }
  }

  public static void writeElement(@Nonnull Element element, Writer writer, String lineSeparator) throws IOException {
    XMLOutputter xmlOutputter = createOutputter(lineSeparator);
    try {
      xmlOutputter.output(element, writer);
    }
    catch (NullPointerException ex) {
      getLogger().error(element.toString(), ex);
      printDiagnostics(element, "");
    }
  }

  @Nonnull
  public static String writeElement(@Nonnull Element element) {
    return writeElement(element, "\n");
  }

  @Nonnull
  public static String writeElement(@Nonnull Element element, String lineSeparator) {
    try {
      StringWriter writer = new StringWriter();
      writeElement(element, writer, lineSeparator);
      return writer.toString();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Nonnull
  public static String writeChildren(@Nullable Element element, @Nonnull String lineSeparator) throws IOException {
    StringWriter writer = new StringWriter();
    for (Element child : getChildren(element)) {
      writeElement(child, writer, lineSeparator);
      writer.append(lineSeparator);
    }
    return writer.toString();
  }

  public static void writeDocument(@Nonnull Document document, @Nonnull Writer writer, String lineSeparator) throws IOException {
    XMLOutputter xmlOutputter = createOutputter(lineSeparator);
    try {
      xmlOutputter.output(document, writer);
    }
    catch (NullPointerException ex) {
      getLogger().error(document.toString(), ex);
      printDiagnostics(document.getRootElement(), "");
    }
  }

  @Nonnull
  public static XMLOutputter createOutputter(String lineSeparator) {
    XMLOutputter xmlOutputter = newXmlOutputter();
    Format format = Format.getCompactFormat().
            setIndent("  ").
            setTextMode(Format.TextMode.TRIM).
            setEncoding(StandardCharsets.UTF_8.toString()).
            setOmitEncoding(false).
            setOmitDeclaration(false).
            setLineSeparator(lineSeparator);
    xmlOutputter.setFormat(format);
    return xmlOutputter;
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
    return escapeText(text, false, false);
  }

  @Nonnull
  public static String escapeText(@Nonnull String text, boolean escapeSpaces, boolean escapeLineEnds) {
    return escapeText(text, false, escapeSpaces, escapeLineEnds);
  }

  @Nonnull
  public static String escapeText(@Nonnull String text, boolean escapeApostrophes, boolean escapeSpaces, boolean escapeLineEnds) {
    StringBuilder buffer = null;
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);
      String quotation = escapeChar(ch, escapeApostrophes, escapeSpaces, escapeLineEnds);
      if (buffer == null) {
        if (quotation != null) {
          // An quotation occurred, so we'll have to use StringBuffer
          // (allocate room for it plus a few more entities).
          buffer = new StringBuilder(text.length() + 20);
          // Copy previous skipped characters and fall through
          // to pickup current character
          buffer.append(text, 0, i);
          buffer.append(quotation);
        }
      }
      else if (quotation == null) {
        buffer.append(ch);
      }
      else {
        buffer.append(quotation);
      }
    }
    // If there were any entities, return the escaped characters
    // that we put in the StringBuffer. Otherwise, just return
    // the unmodified input string.
    return buffer == null ? text : buffer.toString();
  }

  @SuppressWarnings("unused")
  @Deprecated
  @Nonnull
  public static List<Element> getChildrenFromAllNamespaces(@Nonnull Element element, @Nonnull String name) {
    List<Element> result = new ArrayList<>();
    for (Element child : element.getChildren()) {
      if (name.equals(child.getName())) {
        result.add(child);
      }
    }
    return result;
  }

  private static class XmlProcessor extends AbstractXMLOutputProcessor {
    @Override
    protected void attributeEscapedEntitiesFilter(Writer out, FormatStack fstack, String value) throws IOException {
      if (!fstack.getEscapeOutput()) {
        // no escaping...
        write(out, value);
        return;
      }

      write(out, escapeText(value, false, true));
    }

    @Override
    protected Walker buildWalker(FormatStack fstack, List<? extends Content> content, boolean escape) {
      if (fstack.getTextMode() != Format.TextMode.TRIM) {
        throw new IllegalArgumentException("not trim mode unsupported: " + fstack.getTextMode());
      }
      return new CustomWalker(content, fstack, escape);
    }
  }

  public static XMLOutputter newXmlOutputter() {
    return new XMLOutputter(new XmlProcessor());
  }

  private static void printDiagnostics(@Nonnull Element element, String prefix) {
    ElementInfo info = getElementInfo(element);
    prefix += "/" + info.name;
    if (info.hasNullAttributes) {
      //noinspection UseOfSystemOutOrSystemErr
      System.err.println(prefix);
    }

    List<Element> children = getChildren(element);
    for (Element child : children) {
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
    if(newFilePaths.length != newFileDocuments.length) {
      getLogger().error("newFilePaths != newFileDocuments " + Arrays.asList(newFilePaths) + " " + Arrays.asList(newFileDocuments));
    }

    ArrayList<String> writtenFilesPaths = new ArrayList<>();

    // check if files are writable
    for (String newFilePath : newFilePaths) {
      File file = new File(newFilePath);
      if (file.exists() && !file.canWrite()) {
        throw new IOException("File \"" + newFilePath + "\" is not writeable");
      }
    }
    for (File file : oldFiles) {
      if (file.exists() && !file.canWrite()) {
        throw new IOException("File \"" + file.getAbsolutePath() + "\" is not writeable");
      }
    }

    for (int i = 0; i < newFilePaths.length; i++) {
      String newFilePath = newFilePaths[i];

      writeDocument(newFileDocuments[i], newFilePath, lineSeparator);
      writtenFilesPaths.add(newFilePath);
    }

    // delete files if necessary

    outer:
    for (File oldFile : oldFiles) {
      String oldFilePath = oldFile.getAbsolutePath();
      for (String writtenFilesPath : writtenFilesPaths) {
        if (oldFilePath.equals(writtenFilesPath)) {
          continue outer;
        }
      }
      boolean result = oldFile.delete();
      if (!result) {
        throw new IOException("File \"" + oldFilePath + "\" was not deleted");
      }
    }
  }

  private static class ElementInfo {
    @Nonnull
    public String name = "";
    public boolean hasNullAttributes = false;
  }

  @SuppressWarnings("unused")
  @Deprecated
  public static org.w3c.dom.Element convertToDOM(@Nonnull Element e) {
    try {
      Document d = new Document();
      Element newRoot = new Element(e.getName());
      List attributes = e.getAttributes();

      for (Object o : attributes) {
        Attribute attr = (Attribute)o;
        newRoot.setAttribute(attr.getName(), attr.getValue(), attr.getNamespace());
      }

      d.addContent(newRoot);
      newRoot.addContent(e.cloneContent());

      return new DOMOutputter().output(d).getDocumentElement();
    }
    catch (JDOMException e1) {
      throw new RuntimeException(e1);
    }
  }

  @SuppressWarnings("unused")
  @Deprecated
  public static Element convertFromDOM(org.w3c.dom.Element e) {
    return new DOMBuilder().build(e);
  }

  public static String getValue(Object node) {
    if (node instanceof Content) {
      Content content = (Content)node;
      return content.getValue();
    }
    else if (node instanceof Attribute) {
      Attribute attribute = (Attribute)node;
      return attribute.getValue();
    }
    else {
      throw new IllegalArgumentException("Wrong node: " + node);
    }
  }

  @SuppressWarnings("unused")
  @Nullable
  @Deprecated
  public static Element cloneElement(@Nonnull Element element, @Nonnull ElementFilter elementFilter) {
    Element result = new Element(element.getName(), element.getNamespace());
    List<Attribute> attributes = element.getAttributes();
    if (!attributes.isEmpty()) {
      ArrayList<Attribute> list = new ArrayList<>(attributes.size());
      for (Attribute attribute : attributes) {
        list.add(attribute.clone());
      }
      result.setAttributes(list);
    }

    for (Namespace namespace : element.getAdditionalNamespaces()) {
      result.addNamespaceDeclaration(namespace);
    }

    boolean hasContent = false;
    for (Content content : element.getContent()) {
      if (content instanceof Element) {
        if (elementFilter.matches(content)) {
          hasContent = true;
        }
        else {
          continue;
        }
      }
      result.addContent(content.clone());
    }
    return hasContent ? result : null;
  }

  public static boolean isEmpty(@Nullable Element element) {
    return element == null || (element.getAttributes().isEmpty() && element.getContent().isEmpty());
  }

  //private static final JDOMInterner ourJDOMInterner = new JDOMInterner();

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
  //@Nonnull
  //public static Element internElement(@Nonnull Element element) {
  //  return ourJDOMInterner.internElement(element);
  //}

  /**
   * Not required if you use XmlSerializator.
   *
   * @see XmlStringUtil#escapeIllegalXmlChars(String)
   */
  @Nonnull
  public static String removeControlChars(@Nonnull String text) {
    StringBuilder result = null;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (!Verifier.isXMLCharacter(c)) {
        if (result == null) {
          result = new StringBuilder(text.length());
          result.append(text, 0, i);
        }
        continue;
      }

      if (result != null) {
        result.append(c);
      }
    }
    return result == null ? text : result.toString();
  }
}
