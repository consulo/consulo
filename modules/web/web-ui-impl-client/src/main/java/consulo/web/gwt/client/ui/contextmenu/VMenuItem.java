package consulo.web.gwt.client.ui.contextmenu;

import com.vaadin.client.ui.VMenuBar.CustomMenuItem;

import java.util.logging.Logger;

public class VMenuItem extends CustomMenuItem {

    public VMenuItem() {
        Logger.getLogger("VMenuItem").info("VMenuItem constructor");
    }

    public void setSeparator(boolean separator) {
        isSeparator = separator;
        updateStyleNames();
        if (!separator) {
            setEnabled(enabled);
        }
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
