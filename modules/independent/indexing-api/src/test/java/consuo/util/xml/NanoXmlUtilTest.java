/*
 * Copyright 2013-2017 consulo.io
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
package consuo.util.xml;

import com.intellij.util.xml.NanoXmlUtil;
import com.intellij.util.xml.XmlFileHeader;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringReader;

/**
 * @author VISTALL
 * @since 06-Nov-17
 */
public class NanoXmlUtilTest extends Assert {
  @Test
  public void testDtdHeader() throws Exception {
    XmlFileHeader xmlFileHeader = NanoXmlUtil.parseHeaderWithException(new StringReader("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                                                                        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
                                                                                        "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
                                                                                        "<!-- the XHTML document body starts here-->\n" +
                                                                                        "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                                                                                        "</html>"));

    assertEquals("html", xmlFileHeader.getRootTagLocalName());
    assertEquals("http://www.w3.org/1999/xhtml", xmlFileHeader.getRootTagNamespace());
    assertEquals("-//W3C//DTD XHTML 1.0 Transitional//EN", xmlFileHeader.getPublicId());
    assertEquals("http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd", xmlFileHeader.getSystemId());
  }

  @Test
  public void testXsdHeader() throws Exception {
    XmlFileHeader xmlFileHeader = NanoXmlUtil.parseHeaderWithException(new StringReader("<?xml version=\"1.0\"?>\n" +
                                                                                        "\n" +
                                                                                        "<note\n" +
                                                                                        "xmlns=\"https://www.w3schools.com\"\n" +
                                                                                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                                                                        "xsi:schemaLocation=\"https://www.w3schools.com/xml/note.xsd\">\n" +
                                                                                        "  <to>Tove</to>\n" +
                                                                                        "  <from>Jani</from>\n" +
                                                                                        "  <heading>Reminder</heading>\n" +
                                                                                        "  <body>Don't forget me this weekend!</body>\n" +
                                                                                        "</note>"));

    assertEquals("note", xmlFileHeader.getRootTagLocalName());
    assertEquals("https://www.w3schools.com", xmlFileHeader.getRootTagNamespace());
  }
}
