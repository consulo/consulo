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
    else {
      // TODO !!
      webImage = new com.vaadin.flow.component.html.Image();
    }

    ((HasSize)webImage).setHeight(image.getHeight(), Unit.PIXELS);
    ((HasSize)webImage).setWidth(image.getWidth(), Unit.PIXELS);

    return webImage;
  }
}
