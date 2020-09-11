package consulo.web.gwt.client.ui.contextmenu;

import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.ui.VOverlay;

/**
 * FIXME: This is needed to somehow resolve the application connection issue. A
 * real solution is needed.
 */
public class MyVOverlay extends VOverlay {
    private static ApplicationConnection ac_static;

    @SuppressWarnings("deprecation")
    public MyVOverlay(boolean autoHide, boolean modal) {
        super(autoHide, modal);
    }

    public MyVOverlay() {
        super();
    }

    public static void setApplicationConnection2(ApplicationConnection ac) {
        // this.ac = ac;
        ac_static = ac;
    }

    @Override
    protected ApplicationConnection getApplicationConnection() {
        return ac_static;
    }
}
