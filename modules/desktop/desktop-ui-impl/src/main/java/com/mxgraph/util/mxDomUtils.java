/**
 * $Id: mxDomUtils.java,v 1.1 2012/11/15 13:26:39 gaudenz Exp $
 * Copyright (c) 2007-2012, JGraph Ltd
 */
package com.mxgraph.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Contains various DOM API helper methods for use with mxGraph.
 */
public class mxDomUtils {
  /**
   * Returns a new, empty DOM document.
   *
   * @return Returns a new DOM document.
   */
  public static Document createDocument() {
    Document result = null;

    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder parser = factory.newDocumentBuilder();

      result = parser.newDocument();
    }
    catch (Exception e) {
      System.out.println(e.getMessage());
    }

    return result;
  }

  /**
   * Returns a document with a HTML node containing a HEAD and BODY node.
   */
  public static Document createHtmlDocument() {
    Document document = createDocument();

    Element root = document.createElement("html");

    document.appendChild(root);

    Element head = document.createElement("head");
    root.appendChild(head);

    Element body = document.createElement("body");
    root.appendChild(body);

    return document;
  }
}
