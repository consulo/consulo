package consulo.web.internal.ui.image;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import consulo.ui.image.Image;
import org.vaadin.pekkam.Canvas;

/**
 * @author VISTALL
 * @since 29/05/2023
 */
public class WebImageConverter {
  public static Component getImageCanvas(Image image) {
    Canvas canvas = new Canvas(image.getWidth(), image.getHeight());

    WebCanvasRenderingContext2D context = new WebCanvasRenderingContext2D(canvas);

    processCanvas(image, context);

    return canvas;
  }

  public static void processCanvas(Image image, WebCanvasRenderingContext2D context) {
    if (image instanceof WebImageWithURL webImageWithURL) {
      String imageURL = webImageWithURL.getImageURL();

      context.drawImage(imageURL, 0, 0, image.getWidth(), image.getHeight());
    } else if (image instanceof WebImageCanvasDraw draw) {
      draw.drawCanvas(context);
    }
  }

  public static Component getImage(Image image) {
    Component webImage;
    if (image instanceof WebImageWithURL webImageWithURL) {
      webImage = new com.vaadin.flow.component.html.Image(webImageWithURL.getImageURL(), "");
    }
    else if (image instanceof WebEmptyImageImpl) {
      // just empty span;
      webImage = new Span();
    }
    else if (image instanceof WebResizeImageImpl resizeImage) {
      webImage = getImage(resizeImage.getOriginal());
    }
    else if (image instanceof WebTransparentImageImpl transparentImage) {
      webImage = getImage(transparentImage.getOriginal());
      webImage.getElement().getStyle().set("opacity", String.valueOf(transparentImage.getAlpha()));
    }
    else if (image instanceof WebLayeredImageImpl layeredImage) {
      // the platform layers a badge over a base icon and expects one image out of it. the awt side paints them
      // onto a single graphics, here they are stacked in one box - a canvas would need the layers loaded before
      // it can draw, and it has nothing to redraw itself on when they arrive
      Span stack = new Span();
      stack.getStyle().set("position", "relative").set("display", "inline-block");

      for (Image layer : layeredImage.getImages()) {
        Component layerComponent = getImage(layer);
        layerComponent.getElement().getStyle()
          .set("position", "absolute")
          .set("inset", "0")
          .set("width", "100%")
          .set("height", "100%")
          .set("object-fit", "contain");

        stack.add(layerComponent);
      }

      webImage = stack;
    }
    else {
      webImage = new Span();
    }

    ((HasSize)webImage).setHeight(image.getHeight(), Unit.PIXELS);
    ((HasSize)webImage).setWidth(image.getWidth(), Unit.PIXELS);

    return webImage;
  }
}
