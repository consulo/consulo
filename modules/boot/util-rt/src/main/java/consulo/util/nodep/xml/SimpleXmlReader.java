/*
 * Copyright 2013-2019 consulo.io
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
package consulo.util.nodep.xml;

import consulo.util.nodep.text.StringUtilRt;
import consulo.util.nodep.xml.node.SimpleXmlElement;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * @author VISTALL
 * @since 2019-07-17
 */
public class SimpleXmlReader {
  private static ThreadLocal<DocumentBuilder> ourDocumentBuilder = new ThreadLocal<DocumentBuilder>() {
    @Override
    protected DocumentBuilder initialValue() {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      try {
        return dbFactory.newDocumentBuilder();
      }
      catch (ParserConfigurationException e) {
        throw new RuntimeException(e);
      }
    }
  };

  @Nonnull
  public static SimpleXmlElement parse(@Nonnull URL url) throws SimpleXmlParsingException {
    try {
      DocumentBuilder builder = ourDocumentBuilder.get();
      builder.reset();

      Document doc = builder.parse(url.toExternalForm());
      return mapDocument(doc);
    }
    catch (SAXException e) {
      throw new SimpleXmlParsingException(e);
    }
    catch (IOException e) {
      throw new SimpleXmlParsingException(e);
    }
  }

  @Nonnull
  public static SimpleXmlElement parse(@Nonnull InputStream stream) throws SimpleXmlParsingException {
    try {
      DocumentBuilder builder = ourDocumentBuilder.get();
      builder.reset();
      
      Document doc = builder.parse(stream);
      return mapDocument(doc);
    }
    catch (SAXException e) {
      throw new SimpleXmlParsingException(e);
    }
    catch (IOException e) {
      throw new SimpleXmlParsingException(e);
    }
  }

  @Nonnull
  public static SimpleXmlElement parse(@Nonnull File file) throws SimpleXmlParsingException {
    try {
      DocumentBuilder builder = ourDocumentBuilder.get();
      builder.reset();

      Document doc = builder.parse(file);
      return mapDocument(doc);
    }
    catch (SAXException e) {
      throw new SimpleXmlParsingException(e);
    }
    catch (IOException e) {
      throw new SimpleXmlParsingException(e);
    }
  }

  private static SimpleXmlElement mapDocument(Document doc) {
    Element documentElement = doc.getDocumentElement();
    documentElement.normalize();

    List<SimpleXmlElement> children = new ArrayList<SimpleXmlElement>();

    fillElements(documentElement, children);

    return new SimpleXmlElement(documentElement.getTagName(), mapText(documentElement), children, mapAttributes(documentElement));
  }

  private static void fillElements(@Nonnull Element targetElement, @Nonnull List<SimpleXmlElement> children) {
    for (Node node = targetElement.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node instanceof Element) {
        String tagName = ((Element)node).getTagName();

        List<SimpleXmlElement> children2 = new ArrayList<SimpleXmlElement>();

        fillElements((Element)node, children2);

        SimpleXmlElement element = new SimpleXmlElement(tagName, mapText((Element)node), children2, mapAttributes((Element)node));
        children.add(element);
      }
    }
  }

  @Nullable
  private static String mapText(Element element) {
    Node firstChild = element.getFirstChild();
    Node lastChild = element.getLastChild();
    if (lastChild == firstChild && firstChild instanceof Text) {
      String wholeText = ((Text)firstChild).getWholeText();
      return StringUtilRt.isEmptyOrSpaces(wholeText) ? null : wholeText.trim();
    }
    return null;
  }

  @Nonnull
  private static Map<String, String> mapAttributes(Element element) {
    NamedNodeMap attributes = element.getAttributes();

    int length = attributes.getLength();
    if (length == 0) {
      return Collections.emptyMap();
    }

    Map<String, String> map = new HashMap<String, String>();

    for (int i = 0; i < length; i++) {
      Node namedItem = attributes.item(i);

      if (namedItem instanceof Attr) {
        map.put(((Attr)namedItem).getName(), ((Attr)namedItem).getValue());
      }
    }

    return map;
  }
}
