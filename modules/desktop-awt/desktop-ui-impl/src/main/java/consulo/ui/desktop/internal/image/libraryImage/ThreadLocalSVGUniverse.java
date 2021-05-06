/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ui.desktop.internal.image.libraryImage;

import com.kitfox.svg.SVGUniverse;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * @author VISTALL
 * @since 2020-10-02
 */
public class ThreadLocalSVGUniverse extends SVGUniverse {
  private final ThreadLocal<XMLReader> myXMLReader = ThreadLocal.withInitial(() -> {
    SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    saxParserFactory.setNamespaceAware(true);
    try {
      SAXParser saxParser = saxParserFactory.newSAXParser();
      return saxParser.getXMLReader();
    }
    catch (ParserConfigurationException | SAXException e) {
      throw new RuntimeException(e);
    }
  });

  @Override
  public XMLReader getXMLReader() {
    return myXMLReader.get();
  }
}
