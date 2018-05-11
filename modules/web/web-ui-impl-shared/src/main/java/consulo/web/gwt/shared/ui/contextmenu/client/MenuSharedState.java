package consulo.web.gwt.shared.ui.contextmenu.client;

import com.vaadin.shared.AbstractComponentState;
import com.vaadin.shared.communication.URLReference;

import java.io.Serializable;
import java.util.List;

// FIXME: TabIndex? MenuBar's state needs to extend a TabIndex state, ContextMenu's state doesn't need it, what's the solution?
@SuppressWarnings("serial")
public class MenuSharedState extends AbstractComponentState {
    // public class MenuBarState extends TabIndexState {
    // {
    // primaryStyleName = "v-menubar";
    // }
    // }

    public List<MenuItemState> menuItems;
    public boolean htmlContentAllowed;

    public static class MenuItemState implements Serializable {
        public int id;
        public boolean separator;
        public String text;
        public boolean command;
        public URLReference icon;
        public boolean enabled;
        public String description;
        public boolean checkable;
        public boolean checked;
        public List<MenuItemState> childItems;
        public String styleName;
    }
}
