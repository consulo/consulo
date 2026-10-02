package consulo.ui.event;

import consulo.ui.HtmlView;
import consulo.ui.event.details.InputDetails;

public final class HtmlViewDoubleClickEvent extends ComponentEvent<HtmlView> {
    public HtmlViewDoubleClickEvent(HtmlView component, InputDetails inputDetails) {
        super(component, inputDetails);
    }
}
