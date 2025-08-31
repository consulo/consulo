// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.util.xml.fastReader;

import consulo.util.io.CharSequenceReader;
import consulo.util.io.StreamUtil;
import consulo.util.lang.StringUtil;
import net.n3.nanoxml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Properties;

/**
 * @author mike
 */
public class NanoXmlUtil {
  private static final Logger LOG = LoggerFactory.getLogger(NanoXmlUtil.class);

  private NanoXmlUtil() {
  }


  public static void parse(InputStream is, IXMLBuilder builder) {
    try {
      parse(new MyXMLReader(is), builder);
    }
    catch (IOException e) {
      LOG.error(e.getMessage(), e);
    }
    finally {

      try {
        is.close();
      }
      catch (IOException ignore) {

      }
    }
  }

  public static void parse(CharSequence text, IXMLBuilder builder) {
    parse(text, builder, null);
  }

  public static void parse(Reader reader, IXMLBuilder builder) {
    parse(reader, builder, null);
  }

  public static void parse(@Nonnull CharSequence text, @Nonnull IXMLBuilder builder, @Nullable IXMLValidator validator) {
    parse(new CharSequenceReader(text), builder, validator);
  }

  public static void parse(@Nonnull Reader reader, @Nonnull IXMLBuilder builder, @Nullable IXMLValidator validator) {
    try {
      parse(new MyXMLReader(reader), builder, validator);
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
    finally {
      try {
        reader.close();
      }
      catch (IOException ignore) {

      }
    }
  }

  public static void parse(IXMLReader r, IXMLBuilder builder) {
    parse(r, builder, null);
  }

  public static void parse(IXMLReader r, IXMLBuilder builder, @Nullable IXMLValidator validator) {
    try {
      IXMLParser parser = XMLParserFactory.createDefaultXMLParser();
      parser.setReader(r);
      parser.setBuilder(builder);
      parser.setValidator(validator == null ? new EmptyValidator() : validator);
      parser.setResolver(new EmptyEntityResolver());
      try {
        parser.parse();
      }
      catch (ParserStoppedXmlException ignore) {
      }
      catch (XMLException e) {
        LOG.debug(e.getMessage(), e);
      }
    }
    catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  @Nonnull
  public static XmlFileHeader parseHeaderWithException(CharSequence text) {
    return parseHeader(new MyXMLReader(new CharSequenceReader(text)));
  }

  @Nonnull
  public static XmlFileHeader parseHeaderWithException(Reader reader) {
    return parseHeader(new MyXMLReader(reader));
  }

  @Nonnull
  public static XmlFileHeader parseHeaderWithException(InputStream inputStream) throws IOException {
    return parseHeader(new MyXMLReader(inputStream));
  }

  @Nonnull
  public static XmlFileHeader parseHeader(Reader reader) {
    return parseHeader(new MyXMLReader(reader));
  }

  @Nonnull
  private static XmlFileHeader parseHeader(MyXMLReader r) {
    RootTagInfoBuilder builder = new RootTagInfoBuilder();
    parse(r, builder);
    return new XmlFileHeader(builder.getRootTagName(), builder.getNamespace(), r.publicId, r.systemId);
  }

  public static String createLocation(String... tagNames) {
    StringBuilder result = new StringBuilder();
    for (String tagName : tagNames) {
      result.append(".");
      result.append(tagName);
    }

    return result.toString();
  }

  /**
   * @deprecated left for API compatibility
   */
  @Deprecated
  public static abstract class IXMLBuilderAdapter implements NanoXmlBuilder {

    /**
     * @deprecated left for API compatibility
     */
    @Deprecated
    protected static void stop() throws ParserStoppedXmlException {
      throw ParserStoppedXmlException.INSTANCE;
    }
  }

  public static class BaseXmlBuilder implements NanoXmlBuilder {
    private final Deque<String> myLocation = new ArrayDeque<>();

    @Override
    public void startBuilding(String systemID, int lineNr) {
      myLocation.push("");
    }

    @Override
    public void startElement(String name, String nsPrefix, String nsURI, String systemID, int lineNr) throws Exception {
      myLocation.push(myLocation.getFirst() + "." + name);
    }

    @Override
    public void endElement(String name, String nsPrefix, String nsURI) throws Exception {
      myLocation.pop();
    }

    protected static String readText(Reader reader) throws IOException {
      return new String(StreamUtil.readTextAndConvertSeparators(reader));
    }

    protected String getLocation() {
      return myLocation.getFirst();
    }
  }

  public static class EmptyValidator extends NonValidator {
    private IXMLEntityResolver myParameterEntityResolver;

    @Override
    public void setParameterEntityResolver(IXMLEntityResolver resolver) {
      myParameterEntityResolver = resolver;
    }

    @Override
    public IXMLEntityResolver getParameterEntityResolver() {
      return myParameterEntityResolver;
    }

    @Override
    public void parseDTD(String publicID, IXMLReader reader, IXMLEntityResolver entityResolver, boolean external) throws Exception {
      if (!external) {
        //super.parseDTD(publicID, reader, entityResolver, external);
        int cnt = 1;
        for (char ch = reader.read(); !(ch == ']' && --cnt == 0); ch = reader.read()) {
          if (ch == '[') cnt++;
        }
      }
      else {
        int origLevel = reader.getStreamLevel();

        while (true) {
          char ch = reader.read();

          if (reader.getStreamLevel() < origLevel) {
            reader.unread(ch);
            return; // end external DTD
          }
        }
      }
    }

    @Override
    public void elementStarted(String name, String systemId, int lineNr) {
    }

    @Override
    public void elementEnded(String name, String systemId, int lineNr) {
    }

    @Override
    public void attributeAdded(String key, String value, String systemId, int lineNr) {
    }

    @Override
    public void elementAttributesProcessed(String name, Properties extraAttributes, String systemId, int lineNr) {
    }

    @Override
    public void PCDataAdded(String systemId, int lineNr) {
    }
  }

  private static class EmptyEntityResolver implements IXMLEntityResolver {
    @Override
    public void addInternalEntity(String name, String value) {
    }

    @Override
    public void addExternalEntity(String name, String publicID, String systemID) {
    }

    @Override
    public Reader getEntity(IXMLReader xmlReader, String name) {
      return new StringReader("");
    }

    @Override
    public boolean isExternalEntity(String name) {
      return false;
    }
  }

  private static class MyXMLReader extends StdXMLReader {
    private String publicId;
    private String systemId;

    MyXMLReader(@Nonnull Reader documentReader) {
      super(documentReader);
    }

    MyXMLReader(InputStream stream) throws IOException {
      super(stream);
    }

    @Override
    public Reader openStream(String publicId, String systemId) {
      this.publicId = StringUtil.isEmpty(publicId) ? null : publicId;
      this.systemId = StringUtil.isEmpty(systemId) ? null : systemId;

      return new StringReader(" ");
    }
  }

  public static class ParserStoppedXmlException extends XMLException {
    public static final ParserStoppedXmlException INSTANCE = new ParserStoppedXmlException();

    private ParserStoppedXmlException() {
      super("Parsing stopped");
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
      return this;
    }
  }

  private static class RootTagInfoBuilder implements IXMLBuilder {
    private String myRootTagName;
    private String myNamespace;

    @Override
    public void startBuilding(String systemID, int lineNr) {
    }

    @Override
    public void newProcessingInstruction(String target, Reader reader) {
    }

    @Override
    public void startElement(String name, String nsPrefix, String nsURI, String systemID, int lineNr) throws Exception {
      myRootTagName = name;
      myNamespace = nsURI;
      throw ParserStoppedXmlException.INSTANCE;
    }

    @Override
    public void addAttribute(String key, String nsPrefix, String nsURI, String value, String type) {
    }

    @Override
    public void elementAttributesProcessed(String name, String nsPrefix, String nsURI) {
    }

    @Override
    public void endElement(String name, String nsPrefix, String nsURI) {
    }

    @Override
    public void addPCData(Reader reader, String systemID, int lineNr) {
    }

    public String getNamespace() {
      return myNamespace;
    }

    public String getRootTagName() {
      return myRootTagName;
    }

    @Override
    public String getResult() {
      return myRootTagName;
    }
  }
}
