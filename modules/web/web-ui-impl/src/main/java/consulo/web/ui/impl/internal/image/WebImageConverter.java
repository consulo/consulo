package consulo.web.ui.impl.internal.image;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import consulo.ui.image.Image;

/**
 * @author VISTALL
 * @since 29/05/2023
 */
public class WebImageConverter {
    public static Component getImage(Image image) {
        // the composition is a tree of custom elements which apply the effects in the browser, so the tags
        // carry their own size and nothing has to be set on the component around them
        Element element = WebImageElement.toElement(image);
        if (element != null) {
            return new WebImageComponent(element);
        }

        // an image the effects cannot describe still has a url of its own - a canvas drawing, or a file the
        // servlet has no key for, both of which arrive as a data uri
        String url = WebImageUrl.toURL(image);

        Component webImage;
        if (url != null) {
            com.vaadin.flow.component.html.Image imageElement = new com.vaadin.flow.component.html.Image(url, "");
            imageElement.getStyle().set("object-fit", "contain");
            webImage = imageElement;
        }
        else {
            webImage = new Span();
        }

        ((HasSize)webImage).setHeight(size(image.getHeight()), Unit.PIXELS);
        ((HasSize)webImage).setWidth(size(image.getWidth()), Unit.PIXELS);

        return webImage;
    }

    /**
     * The tag differs per effect, so the component is built around the element it is given - a component with
     * a {@code @Tag} of its own can only be mapped onto an element carrying that very tag.
     */
    public static class WebImageComponent extends Component {
        private WebImageComponent(Element element) {
            super(element);
        }
    }

    /**
     * An {@link consulo.ui.image.ImageKey} may carry no size at all, and an element sized zero shows nothing.
     */
    private static int size(int value) {
        return value > 0 ? value : WebImageSpec.DEFAULT_SIZE;
    }

    private WebImageConverter() {
    }
}
