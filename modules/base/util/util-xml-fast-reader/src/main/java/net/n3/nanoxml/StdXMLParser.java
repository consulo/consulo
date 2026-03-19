/* StdXMLParser.java                                               NanoXML/Java
 *
 * $Revision: 1.5 $
 * $Date: 2002/03/24 11:37:00 $
 * $Name: RELEASE_2_2_1 $
 *
 * This file is part of NanoXML 2 for Java.
 * Copyright (C) 2000-2002 Marc De Scheemaecker, All Rights Reserved.
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from the
 * use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 *  1. The origin of this software must not be misrepresented; you must not
 *     claim that you wrote the original software. If you use this software in
 *     a product, an acknowledgment in the product documentation would be
 *     appreciated but is not required.
 *
 *  2. Altered source versions must be plainly marked as such, and must not be
 *     misrepresented as being the original software.
 *
 *  3. This notice may not be removed or altered from any source distribution.
 */
package net.n3.nanoxml;

import org.jspecify.annotations.Nullable;

import java.io.Reader;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Properties;
import java.util.Vector;


/**
 * StdXMLParser is the core parser of NanoXML.
 *
 * @author Marc De Scheemaecker
 * @version $Name: RELEASE_2_2_1 $, $Revision: 1.5 $
 */
public class StdXMLParser implements IXMLParser
{

   /**
    * The builder which creates the logical structure of the XML data.
    */
   private @Nullable IXMLBuilder builder = null;


   /**
    * The reader from which the parser retrieves its data.
    */
   private @Nullable IXMLReader reader = null;


   /**
    * The entity resolver.
    */
   private IXMLEntityResolver entityResolver = new XMLEntityResolver();


   /**
    * The validator that will process entity references and validate the XML
    * data.
    */
   private @Nullable IXMLValidator validator = null;


   /**
    * Sets the builder which creates the logical structure of the XML data.
    *
    * @param builder the non-null builder
    */
   public void setBuilder(IXMLBuilder builder)
   {
      this.builder = builder;
   }


   /**
    * Returns the builder which creates the logical structure of the XML data.
    *
    * @return the builder
    */
   public @Nullable IXMLBuilder getBuilder()
   {
      return this.builder;
   }


   /**
    * Sets the validator that validates the XML data.
    *
    * @param validator the non-null validator
    */
   public void setValidator(IXMLValidator validator)
   {
      this.validator = validator;
   }


   /**
    * Returns the validator that validates the XML data.
    *
    * @return the validator
    */
   public @Nullable IXMLValidator getValidator()
   {
      return this.validator;
   }


   /**
    * Sets the entity resolver.
    *
    * @param resolver the non-null resolver
    */
   public void setResolver(IXMLEntityResolver resolver)
   {
      this.entityResolver = resolver;
   }


   /**
    * Returns the entity resolver.
    *
    * @return the non-null resolver
    */
   public IXMLEntityResolver getResolver()
   {
      return this.entityResolver;
   }


   /**
    * Sets the reader from which the parser retrieves its data.
    *
    * @param reader the reader
    */
   public void setReader(IXMLReader reader)
   {
      this.reader = reader;
   }


   /**
    * Returns the reader from which the parser retrieves its data.
    *
    * @return the reader
    */
   public @Nullable IXMLReader getReader()
   {
      return this.reader;
   }


   /**
    * Parses the data and lets the builder create the logical data structure.
    *
    * @return the logical structure built by the builder
    *
    * @throws net.n3.nanoxml.XMLException
    *		if an error occurred reading or parsing the data
    */
   public @Nullable Object parse()
      throws XMLException
   {
      try {
         IXMLBuilder builder = Objects.requireNonNull(this.builder);
         IXMLReader reader = Objects.requireNonNull(this.reader);
         builder.startBuilding(reader.getSystemID(), reader.getLineNr());
         this.scanData();
         return builder.getResult();
      } catch (XMLException e) {
         throw e;
      } catch (Exception e) {
         throw new XMLException(e);
      }
   }


   /**
    * Scans the XML data for elements.
    *
    * @throws java.lang.Exception
    *     if something went wrong
    */
   protected void scanData()
      throws Exception
   {
      IXMLReader reader = Objects.requireNonNull(this.reader);
      IXMLBuilder builder = Objects.requireNonNull(this.builder);
      while (!reader.atEOF() && builder.getResult() == null) {
         String str = XMLUtil.read(reader, '&');
         char ch = str.charAt(0);
         if (ch == '&') {
            XMLUtil.processEntity(str, reader, this.entityResolver);
            continue;
         }

         switch (ch) {
            case '<':
               this.scanSomeTag(false, // don't allow CDATA
                                null,  // no default namespace
                                new Properties());
               break;

            case ' ':
            case '\t':
            case '\r':
            case '\n':
               // skip whitespace
               break;

            default:
               XMLUtil.errorInvalidInput(
                   reader.getSystemID(),
                   reader.getLineNr(),
                   "`" + ch + "' (0x" + Integer.toHexString((int) ch) + ')'
               );
         }
      }
   }


   /**
    * Scans an XML tag.
    *
    * @param allowCDATA true if CDATA sections are allowed at this point
    * @param defaultNamespace the default namespace URI (or null)
    * @param namespaces list of defined namespaces
    *
    * @throws java.lang.Exception
    *     if something went wrong
    */
   protected void scanSomeTag(boolean    allowCDATA,
                              @Nullable String defaultNamespace,
                              Properties namespaces)
      throws Exception
   {
      IXMLReader reader = Objects.requireNonNull(this.reader);
      String str = XMLUtil.read(reader, '&');
      char ch = str.charAt(0);

      if (ch == '&') {
         XMLUtil.errorUnexpectedEntity(reader.getSystemID(), reader.getLineNr(), str);
      }

      switch (ch) {
         case '?':
            this.processPI();
            break;

         case '!':
            this.processSpecialTag(allowCDATA);
            break;

         default:
            reader.unread(ch);
            this.processElement(defaultNamespace, namespaces);
      }
   }


   /**
    * Processes a "processing instruction".
    *
    * @throws java.lang.Exception
    *     if something went wrong
    */
   protected void processPI()
      throws Exception
   {
      IXMLReader reader = Objects.requireNonNull(this.reader);
      XMLUtil.skipWhitespace(reader, null);
      String target = XMLUtil.scanIdentifier(reader);
      XMLUtil.skipWhitespace(reader, null);
      Reader readerStream = new PIReader(reader);

      if (! target.equalsIgnoreCase("xml")) {
         Objects.requireNonNull(this.builder).newProcessingInstruction(target, readerStream);
      }

      readerStream.close();
   }


   /**
    * Processes a tag that starts with a bang (&lt;!...&gt;).
    *
    * @param allowCDATA true if CDATA sections are allowed at this point
    *
    * @throws java.lang.Exception
    *     if something went wrong
    */
   protected void processSpecialTag(boolean allowCDATA)
      throws Exception
   {
      IXMLReader reader = Objects.requireNonNull(this.reader);
      String str = XMLUtil.read(reader, '&');
      char ch = str.charAt(0);

      if (ch == '&') {
         XMLUtil.errorUnexpectedEntity(reader.getSystemID(), reader.getLineNr(), str);
      }

      switch (ch) {
         case '[':
            if (allowCDATA) {
               this.processCDATA();
            } else {
               XMLUtil.errorUnexpectedCDATA(reader.getSystemID(), reader.getLineNr());
            }

            return;

         case 'D':
            this.processDocType();
            return;

         case '-':
            XMLUtil.skipComment(reader);
            return;
      }
   }


   /**
    * Processes a CDATA section.
    *
    * @throws java.lang.Exception
    *     if something went wrong
    */
   protected void processCDATA()
      throws Exception
   {
      @Nullable IXMLReader reader = Objects.requireNonNull(this.reader);
      if (! XMLUtil.checkLiteral(reader, "CDATA[")) {
         XMLUtil.errorExpectedInput(reader.getSystemID(), reader.getLineNr(), "<![[CDATA[");
      }

      Objects.requireNonNull(this.validator).PCDataAdded(reader.getSystemID(), reader.getLineNr());
      Reader readerStream = new CDATAReader(reader);
      Objects.requireNonNull(this.builder).addPCData(readerStream, reader.getSystemID(), reader.getLineNr());
      readerStream.close();
   }


   /**
    * Processes a document type declaration.
    *
    * @throws java.lang.Exception
    *		if an error occurred reading or parsing the data
    */
   protected void processDocType()
      throws Exception
   {
      IXMLReader reader = Objects.requireNonNull(this.reader);
      IXMLValidator validator = Objects.requireNonNull(this.validator);
      if (! XMLUtil.checkLiteral(reader, "OCTYPE")) {
         XMLUtil.errorExpectedInput(reader.getSystemID(), reader.getLineNr(), "<!DOCTYPE");
         return;
      }

      XMLUtil.skipWhitespace(reader, null);
      String systemID = null;
      StringBuffer publicID = new StringBuffer();
      String rootElement = XMLUtil.scanIdentifier(reader);
      XMLUtil.skipWhitespace(reader, null);
      char ch = reader.read();

      if (ch == 'P') {
         systemID = XMLUtil.scanPublicID(publicID, reader);
         XMLUtil.skipWhitespace(reader, null);
         ch = reader.read();
      } else if (ch == 'S') {
         systemID = XMLUtil.scanSystemID(reader);
         XMLUtil.skipWhitespace(reader, null);
         ch = reader.read();
      }

      if (ch == '[') {
         validator.parseDTD(publicID.toString(), reader, this.entityResolver, false);
         XMLUtil.skipWhitespace(reader, null);
         ch = reader.read();
      }

      if (ch != '>') {
         XMLUtil.errorExpectedInput(reader.getSystemID(), reader.getLineNr(), "`>'");
      }

      if (systemID != null) {
         Reader readerStream = reader.openStream(publicID.toString(), systemID);
         reader.startNewStream(readerStream);
         reader.setSystemID(systemID);
         reader.setPublicID(publicID.toString());
         validator.parseDTD(publicID.toString(), reader, this.entityResolver, true);
      }
   }


   /**
    * Processes a regular element.
    *
    * @param defaultNamespace the default namespace URI (or null)
    * @param namespaces list of defined namespaces
    *
    * @throws java.lang.Exception
    *     if something went wrong
    */
   protected void processElement(@Nullable String defaultNamespace,
                                 Properties namespaces)
      throws Exception
   {
      IXMLReader reader = Objects.requireNonNull(this.reader);
      IXMLBuilder builder = Objects.requireNonNull(this.builder);
      IXMLValidator validator = Objects.requireNonNull(this.validator);

      String fullName = XMLUtil.scanIdentifier(reader);
      String name = fullName;
      XMLUtil.skipWhitespace(reader, null);
      String prefix = null;
      int colonIndex = name.indexOf(':');

      if (colonIndex > 0) {
         prefix = name.substring(0, colonIndex);
         name = name.substring(colonIndex + 1);
      }

      Vector attrNames = new Vector();
      Vector attrValues = new Vector();
      Vector attrTypes = new Vector();

      validator.elementStarted(fullName, reader.getSystemID(), reader.getLineNr());
      char ch;

      for (;;) {
         ch = reader.read();

         if ((ch == '/') || (ch == '>')) {
            break;
         }

         reader.unread(ch);
         this.processAttribute(attrNames, attrValues, attrTypes);
         XMLUtil.skipWhitespace(reader, null);
      }

      Properties extraAttributes = new Properties();
      validator.elementAttributesProcessed(fullName, extraAttributes, reader.getSystemID(), reader.getLineNr());
      Enumeration<?> keys = extraAttributes.keys();
//      Enumeration enum = extraAttributes.keys();

      while (keys.hasMoreElements()) {
         String key = (String) keys.nextElement();
         String value = extraAttributes.getProperty(key);
         attrNames.addElement(key);
         attrValues.addElement(value);
         attrTypes.addElement("CDATA");
      }

      for (int i = 0; i < attrNames.size(); i++) {
         String key = (String) attrNames.elementAt(i);
         String value = (String) attrValues.elementAt(i);
         String type = (String) attrTypes.elementAt(i);

         if (key.equals("xmlns")) {
            defaultNamespace = value;
         } else if (key.startsWith("xmlns:")) {
            namespaces.put(key.substring(6), value);
         }
      }

      if (prefix == null) {
         builder.startElement(name, prefix, defaultNamespace, reader.getSystemID(), reader.getLineNr());
      } else {
         builder.startElement(name, prefix, namespaces.getProperty(prefix), reader.getSystemID(), reader.getLineNr());
      }

      for (int i = 0; i < attrNames.size(); i++) {
         String key = (String) attrNames.elementAt(i);

         if (key.startsWith("xmlns")) {
            continue;
         }

         String value = (String) attrValues.elementAt(i);
         String type = (String) attrTypes.elementAt(i);
         colonIndex = key.indexOf(':');

         if (colonIndex > 0) {
            String attPrefix = key.substring(0, colonIndex);
            key = key.substring(colonIndex + 1);
            builder.addAttribute(key, attPrefix, namespaces.getProperty(attPrefix), value, type);
         } else {
            builder.addAttribute(key, null, null, value, type);
         }
      }

      if (prefix == null) {
         builder.elementAttributesProcessed(name, prefix, defaultNamespace);
      } else {
         builder.elementAttributesProcessed(name, prefix, namespaces.getProperty(prefix));
      }

      if (ch == '/') {
         if (reader.read() != '>') {
            XMLUtil.errorExpectedInput(reader.getSystemID(), reader.getLineNr(), "`>'");
         }

         validator.elementEnded(name, reader.getSystemID(), reader.getLineNr());

         if (prefix == null) {
            builder.endElement(name, prefix, defaultNamespace);
         } else {
            builder.endElement(name, prefix, namespaces.getProperty(prefix));
         }

         return;
      }

      StringBuffer buffer = new StringBuffer(16);

      for (;;) {
         buffer.setLength(0);
         String str;

         for (;;) {
            XMLUtil.skipWhitespace(reader, buffer);
            str = XMLUtil.read(reader, '&');

            if ((str.charAt(0) == '&') && (str.charAt(1) != '#')) {
               XMLUtil.processEntity(str, reader, this.entityResolver);
            } else {
               break;
            }
         }

         if (str.charAt(0) == '<') {
            str = XMLUtil.read(reader, '\0');

            if (str.charAt(0) == '/') {
               XMLUtil.skipWhitespace(reader, null);
               str = XMLUtil.scanIdentifier(reader);

               if (! str.equals(fullName)) {
                  XMLUtil.errorWrongClosingTag(reader.getSystemID(), reader.getLineNr(), name, str);
               }

               XMLUtil.skipWhitespace(reader, null);

               if (reader.read() != '>') {
                  XMLUtil.errorClosingTagNotEmpty(reader.getSystemID(), reader.getLineNr());
               }

               validator.elementEnded(fullName, reader.getSystemID(), reader.getLineNr());
               if (prefix == null) {
                   builder.endElement(name, prefix, defaultNamespace);
               } else {
                   builder.endElement(name, prefix, namespaces.getProperty(prefix));
               }
               break;
            } else { // <[^/]
               reader.unread(str.charAt(0));
               this.scanSomeTag(true, //CDATA allowed
                                defaultNamespace,
                                (Properties) namespaces.clone());
            }
         } else { // [^<]
            if (str.charAt(0) == '&') {
               ch = XMLUtil.processCharLiteral(str);
               buffer.append(ch);
            } else {
               reader.unread(str.charAt(0));
            }
            validator.PCDataAdded(reader.getSystemID(), reader.getLineNr());
            Reader r = new ContentReader(reader, this.entityResolver, buffer.toString());
            builder.addPCData(r, reader.getSystemID(), reader.getLineNr());
            r.close();
         }
      }
   }


   /**
    * Processes an attribute of an element.
    *
    * @param attrNames contains the names of the attributes.
    * @param attrValues contains the values of the attributes.
    * @param attrTypes contains the types of the attributes.
    *
    * @throws java.lang.Exception
    *     if something went wrong
    */
   protected void processAttribute(Vector attrNames,
                                   Vector attrValues,
                                   Vector attrTypes)
      throws Exception
   {
      IXMLReader reader = Objects.requireNonNull(this.reader);
      String key = XMLUtil.scanIdentifier(reader);
      XMLUtil.skipWhitespace(reader, null);

      if (! XMLUtil.read(reader, '&').equals("=")) {
         XMLUtil.errorExpectedInput(reader.getSystemID(), reader.getLineNr(), "`='");
      }

      XMLUtil.skipWhitespace(reader, null);
      String value = XMLUtil.scanString(reader, '&', this.entityResolver);
      attrNames.addElement(key);
      attrValues.addElement(value);
      attrTypes.addElement("CDATA");
      Objects.requireNonNull(this.validator).attributeAdded(key, value, reader.getSystemID(), reader.getLineNr());
   }
}
