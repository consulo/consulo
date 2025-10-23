package com.mxgraph.reader;

import com.mxgraph.canvas.mxICanvas2D;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Hashtable;
import java.util.Map;

/**
 * XMLReader reader = SAXParserFactory.newInstance().newSAXParser()
 * .getXMLReader();
 * reader.setContentHandler(new mxSaxExportHandler(
 * new mxGraphicsExportCanvas(g2)));
 * reader.parse(new InputSource(new StringReader(xml)));
 */
public class mxSaxOutputHandler extends DefaultHandler {
  /**
   *
   */
  protected mxICanvas2D canvas;

  /**
   *
   */
  protected transient Map<String, IElementHandler> handlers = new Hashtable<String, IElementHandler>();

  /**
   *
   */
  public mxSaxOutputHandler(mxICanvas2D canvas) {
    setCanvas(canvas);
    initHandlers();
  }

  /**
   * Sets the canvas for rendering.
   */
  public void setCanvas(mxICanvas2D value) {
    canvas = value;
  }

  /**
   * Returns the canvas for rendering.
   */
  public mxICanvas2D getCanvas() {
    return canvas;
  }

  /**
   *
   */
  public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
    IElementHandler handler = handlers.get(qName.toLowerCase());

    if (handler != null) {
      handler.parseElement(attrs);
    }
  }

  /**
   *
   */
  protected void initHandlers() {
    handlers.put("save", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.save();
      }
    });

    handlers.put("restore", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.restore();
      }
    });

    handlers.put("scale", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.scale(Double.parseDouble(attrs.getValue("scale")));
      }
    });

    handlers.put("translate", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.translate(Double.parseDouble(attrs.getValue("dx")), Double.parseDouble(attrs.getValue("dy")));
      }
    });

    handlers.put("rotate", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.rotate(Double.parseDouble(attrs.getValue("theta")), attrs.getValue("flipH").equals("1"), attrs.getValue("flipV").equals("1"),
                      Double.parseDouble(attrs.getValue("cx")), Double.parseDouble(attrs.getValue("cy")));
      }
    });

    handlers.put("strokewidth", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.setStrokeWidth(Double.parseDouble(attrs.getValue("width")));
      }
    });

    handlers.put("strokecolor", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.setStrokeColor(attrs.getValue("color"));
      }
    });

    handlers.put("dashed", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.setDashed(attrs.getValue("dashed").equals("1"));
      }
    });

    handlers.put("dashpattern", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.setDashPattern(attrs.getValue("pattern"));
      }
    });

    handlers.put("linecap", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.setLineCap(attrs.getValue("cap"));
      }
    });

    handlers.put("linejoin", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.setLineJoin(attrs.getValue("join"));
      }
    });

    handlers.put("miterlimit", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.setMiterLimit(Double.parseDouble(attrs.getValue("limit")));
      }
    });

    handlers.put("fontsize", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.setFontSize(Double.parseDouble(attrs.getValue("size")));
      }
    });

    handlers.put("fontcolor", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.setFontColor(attrs.getValue("color"));
      }
    });

    handlers.put("fontbackgroundcolor", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.setFontBackgroundColor(attrs.getValue("color"));
      }
    });

    handlers.put("fontbordercolor", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.setFontBorderColor(attrs.getValue("color"));
      }
    });

    handlers.put("fontfamily", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.setFontFamily(attrs.getValue("family"));
      }
    });

    handlers.put("fontstyle", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.setFontStyle(Integer.parseInt(attrs.getValue("style")));
      }
    });

    handlers.put("alpha", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.setAlpha(Double.parseDouble(attrs.getValue("alpha")));
      }
    });

    handlers.put("fillcolor", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.setFillColor(attrs.getValue("color"));
      }
    });

    handlers.put("shadowcolor", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.setShadowColor(attrs.getValue("color"));
      }
    });

    handlers.put("shadowalpha", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.setShadowAlpha(Double.parseDouble(attrs.getValue("alpha")));
      }
    });

    handlers.put("shadowoffset", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.setShadowOffset(Double.parseDouble(attrs.getValue("dx")), Double.parseDouble(attrs.getValue("dy")));
      }
    });

    handlers.put("shadow", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.setShadow(getValue(attrs, "enabled", "1").equals("1"));
      }
    });

    handlers.put("gradient", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.setGradient(attrs.getValue("c1"), attrs.getValue("c2"), Double.parseDouble(attrs.getValue("x")),
                           Double.parseDouble(attrs.getValue("y")), Double.parseDouble(attrs.getValue("w")),
                           Double.parseDouble(attrs.getValue("h")), attrs.getValue("direction"),
                           Double.parseDouble(getValue(attrs, "alpha1", "1")), Double.parseDouble(getValue(attrs, "alpha2", "1")));
      }
    });

    handlers.put("rect", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.rect(Double.parseDouble(attrs.getValue("x")), Double.parseDouble(attrs.getValue("y")), Double.parseDouble(attrs.getValue("w")),
                    Double.parseDouble(attrs.getValue("h")));
      }
    });

    handlers.put("roundrect", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas
          .roundrect(Double.parseDouble(attrs.getValue("x")), Double.parseDouble(attrs.getValue("y")), Double.parseDouble(attrs.getValue("w")),
                     Double.parseDouble(attrs.getValue("h")), Double.parseDouble(attrs.getValue("dx")),
                     Double.parseDouble(attrs.getValue("dy")));
      }
    });

    handlers.put("ellipse", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas
          .ellipse(Double.parseDouble(attrs.getValue("x")), Double.parseDouble(attrs.getValue("y")), Double.parseDouble(attrs.getValue("w")),
                   Double.parseDouble(attrs.getValue("h")));
      }
    });

    handlers.put("image", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.image(Double.parseDouble(attrs.getValue("x")), Double.parseDouble(attrs.getValue("y")), Double.parseDouble(attrs.getValue("w")),
                     Double.parseDouble(attrs.getValue("h")), attrs.getValue("src"), attrs.getValue("aspect").equals("1"),
                     attrs.getValue("flipH").equals("1"), attrs.getValue("flipV").equals("1"));
      }
    });

    handlers.put("text", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.text(Double.parseDouble(attrs.getValue("x")), Double.parseDouble(attrs.getValue("y")), Double.parseDouble(attrs.getValue("w")),
                    Double.parseDouble(attrs.getValue("h")), attrs.getValue("str"), attrs.getValue("align"), attrs.getValue("valign"),
                    getValue(attrs, "wrap", "").equals("1"), attrs.getValue("format"), attrs.getValue("overflow"),
                    getValue(attrs, "clip", "").equals("1"), Double.parseDouble(getValue(attrs, "rotation", "0")));
      }
    });

    handlers.put("begin", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.begin();
      }
    });

    handlers.put("move", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.moveTo(Double.parseDouble(attrs.getValue("x")), Double.parseDouble(attrs.getValue("y")));
      }
    });

    handlers.put("line", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.lineTo(Double.parseDouble(attrs.getValue("x")), Double.parseDouble(attrs.getValue("y")));
      }
    });

    handlers.put("quad", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas
          .quadTo(Double.parseDouble(attrs.getValue("x1")), Double.parseDouble(attrs.getValue("y1")), Double.parseDouble(attrs.getValue("x2")),
                  Double.parseDouble(attrs.getValue("y2")));
      }
    });

    handlers.put("curve", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.curveTo(Double.parseDouble(attrs.getValue("x1")), Double.parseDouble(attrs.getValue("y1")),
                       Double.parseDouble(attrs.getValue("x2")), Double.parseDouble(attrs.getValue("y2")),
                       Double.parseDouble(attrs.getValue("x3")), Double.parseDouble(attrs.getValue("y3")));
      }
    });

    handlers.put("close", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.close();
      }
    });

    handlers.put("stroke", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.stroke();
      }
    });

    handlers.put("fill", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.fill();
      }
    });

    handlers.put("fillstroke", new IElementHandler() {
      public void parseElement(Attributes attrs) {
        canvas.fillAndStroke();
      }
    });
  }

  /**
   * Returns the given attribute value or an empty string.
   */
  protected String getValue(Attributes attrs, String name, String defaultValue) {
    String value = attrs.getValue(name);

    if (value == null) {
      value = defaultValue;
    }

    return value;
  }

  ;

  /**
   *
   */
  protected interface IElementHandler {
    void parseElement(Attributes attrs);
  }
}
