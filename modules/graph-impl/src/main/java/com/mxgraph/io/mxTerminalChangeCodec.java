/**
 * $Id: mxTerminalChangeCodec.java,v 1.1 2012/11/15 13:26:47 gaudenz Exp $
 * Copyright (c) 2006, Gaudenz Alder
 */
package com.mxgraph.io;

import com.mxgraph.model.mxGraphModel.mxTerminalChange;
import org.w3c.dom.Node;

import java.util.Map;

/**
 * Codec for mxChildChanges. This class is created and registered
 * dynamically at load time and used implicitely via mxCodec
 * and the mxCodecRegistry.
 */
public class mxTerminalChangeCodec extends mxObjectCodec {

  /**
   * Constructs a new model codec.
   */
  public mxTerminalChangeCodec() {
    this(new mxTerminalChange(), new String[]{"model", "previous"}, new String[]{"cell", "terminal"}, null);
  }

  /**
   * Constructs a new model codec for the given arguments.
   */
  public mxTerminalChangeCodec(Object template, String[] exclude, String[] idrefs, Map<String, String> mapping) {
    super(template, exclude, idrefs, mapping);
  }

  /* (non-Javadoc)
   * @see com.mxgraph.io.mxObjectCodec#afterDecode(com.mxgraph.io.mxCodec, org.w3c.dom.Node, java.lang.Object)
   */
  @Override
  public Object afterDecode(mxCodec dec, Node node, Object obj) {
    if (obj instanceof mxTerminalChange) {
      mxTerminalChange change = (mxTerminalChange)obj;

      change.setPrevious(change.getTerminal());
    }

    return obj;
  }

}
