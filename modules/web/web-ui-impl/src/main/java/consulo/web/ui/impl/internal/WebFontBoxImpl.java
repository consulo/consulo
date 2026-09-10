/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.server.VaadinSession;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.FontBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.font.FontManager;
import consulo.ui.font.Typeface;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The browser owns font rasterisation, so a row previews itself by asking the document for the family it names.
 * Which families are installed is the one thing it will not say without being asked, and asking is a row of the
 * list rather than a control beside it - a settings row is laid out one field wide, and the chooser is still
 * usable while the asking is only ever offered.
 *
 * @author VISTALL
 * @since 2026-09-03
 */
public class WebFontBoxImpl extends VaadinComponentDelegate<WebFontBoxImpl.Vaadin> implements FontBox {
    private static final Logger LOG = Logger.getInstance(WebFontBoxImpl.class);

    /**
     * The row which reads the installed families instead of choosing one. It is an item rather than a family so
     * that no family name can ever be mistaken for it.
     */
    private static final Object REQUEST_MORE = new Object() {
        @Override
        public String toString() {
            return UILocalize.fontBoxRequestFonts().get();
        }
    };

    public class Vaadin extends Select<Object> implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebFontBoxImpl.this;
        }
    }

    private List<Typeface> myTypefaces = List.of();

    private boolean myMonospacedOnly;

    /**
     * Whether the browser would still hand over more families than are being offered, which is the only reason
     * to carry the row that asks.
     */
    private boolean myCanAsk;

    /**
     * Rebuilding the list resets the selection, and that reset reports itself as a change - without this the
     * page would read a rebuild as the user picking a font.
     */
    private boolean myFireListeners = true;

    /**
     * What the caller asked for. It is kept rather than read back off the widget because the families can be
     * replaced underneath it, and a family this environment does not have is still the stored value.
     */
    private @Nullable String myRequestedValue;

    @RequiredUIAccess
    public WebFontBoxImpl() {
        Vaadin component = toVaadinComponent();

        component.setRenderer(new ComponentRenderer<>(item -> {
            Span span = new Span(item.toString());
            if (item instanceof String family) {
                // a row off the screen is not laid out, so the family it names is not instantiated until it
                // is scrolled to - the list holds every row at once and a machine can have hundreds
                span.getStyle()
                    .set("font-family", toCssFamily(family))
                    .set("content-visibility", "auto")
                    .set("contain-intrinsic-size", "auto 12em auto 1.5em");
            }
            else {
                span.getStyle().set("font-style", "italic");
            }
            return span;
        }));

        component.addValueChangeListener(event -> {
            if (event.getValue() == REQUEST_MORE) {
                // asking is not a choice of font - the row that was chosen goes back and the browser is asked
                pushItemsWhenLocked();
                loadTypefaces(UIAccess.current());
                return;
            }

            if (!myFireListeners) {
                return;
            }

            myRequestedValue = event.getValue() instanceof String family ? family : null;

            fireListeners(myRequestedValue);
        });

        myTypefaces = WebFontManagerImpl.getBundledTypefaces();

        // whatever was set before the field reached the document is pushed the moment it does, which is a point
        // the session is certainly held
        component.addAttachListener(event -> pushItems());
        pushItemsWhenLocked();

        UIAccess uiAccess = UIAccess.current();
        if (FontManager.get().isRequiredPermission()) {
            readPermission(uiAccess, true);
        }
        else {
            loadTypefaces(uiAccess);
        }
    }

    /**
     * A family name comes from the user's machine and is an arbitrary string, so it is written as a css string
     * rather than dropped in raw - a comma in one would otherwise read as a whole font stack, and a quote would
     * throw the declaration away and preview the row in the wrong face.
     */
    private static String toCssFamily(String family) {
        return '"' + family.replace("\\", "\\\\").replace("\"", "\\\"") + "\", sans-serif";
    }

    /**
     * Refusing the prompt is not an error - the browser answers with no families at all, which is
     * indistinguishable from a grant that found nothing. So whether the asking row stays is taken from the
     * permission rather than from the request appearing to have worked, and a refusal leaves it there for
     * whoever changes their mind in site settings.
     */
    @RequiredUIAccess
    private void readPermission(UIAccess uiAccess, boolean loadWhenGranted) {
        WebFontManagerImpl.ourInstance.getLocalFontsPermissionAsync(uiAccess)
            .whenCompleteAsync((permission, e) -> {
                if (e != null) {
                    LOG.error("Failed to read the local font permission", e);
                    return;
                }

                myCanAsk = permission == WebFontManagerImpl.LocalFontsPermission.CAN_ASK;

                if (loadWhenGranted && permission == WebFontManagerImpl.LocalFontsPermission.GRANTED) {
                    loadTypefaces(uiAccess);
                }
                else {
                    pushItems();
                }
            }, uiAccess);
    }

    @RequiredUIAccess
    private void loadTypefaces(UIAccess uiAccess) {
        FontManager.get().getAvailableTypefacesAsync(uiAccess)
            .whenCompleteAsync((typefaces, e) -> {
                if (e != null) {
                    LOG.error("Failed to read the installed font families", e);
                    return;
                }

                myTypefaces = typefaces;

                readPermission(uiAccess, false);
            }, uiAccess);
    }

    /**
     * Rebuilding the list is a structural change to the document, and flow refuses one made without the session
     * lock. A configurable is reset on whichever thread built it, and that thread holds no lock even though
     * vaadin left a current ui on it - so asking {@link UIAccess#isUIThread()} answers yes and is not the
     * question. The session itself is asked instead, and a push that cannot be made here is handed to the ui to
     * make when it can.
     */
    private void pushItemsWhenLocked() {
        UI ui = toVaadinComponent().getUI().orElse(null);
        if (ui == null) {
            // not in a document yet, so there is no shared state to be holding a lock on
            pushItems();
            return;
        }

        VaadinSession session = ui.getSession();
        if (session != null && session.hasLock()) {
            pushItems();
        }
        else {
            // through the access of the ui rather than UI#access directly, which vaadin drains on the calling
            // thread and would let a queued write block against it
            getUIAccess(ui).give(this::pushItems);
        }
    }

    @RequiredUIAccess
    private void pushItems() {
        Vaadin component = toVaadinComponent();

        List<Object> items = new ArrayList<>();
        for (Typeface typeface : myTypefaces) {
            if (!myMonospacedOnly || typeface.isMonospaced()) {
                items.add(typeface.getName());
            }
        }

        // the stored family is offered even when this environment has no such font, so that rebuilding the list
        // cannot quietly drop what the user chose
        String requested = myRequestedValue;
        if (requested != null && !items.contains(requested)) {
            items.add(requested);
        }

        if (myCanAsk) {
            items.add(REQUEST_MORE);
        }

        myFireListeners = false;
        try {
            component.setItems(items);
            component.setValue(requested);
        }
        finally {
            myFireListeners = true;
        }
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public @Nullable String getValue() {
        Object value = toVaadinComponent().getValue();
        return value instanceof String family ? family : myRequestedValue;
    }

    @RequiredUIAccess
    @Override
    public void setValue(@Nullable String value, boolean fireListeners) {
        myRequestedValue = value;

        pushItemsWhenLocked();

        if (fireListeners) {
            // the widget write may still be queued, so what is reported is what was stored rather than what
            // the widget would answer, which is still the family before this call
            fireListeners(value);
        }
    }

    @Override
    public void setMonospacedOnly(boolean monospacedOnly) {
        if (myMonospacedOnly == monospacedOnly) {
            return;
        }

        myMonospacedOnly = monospacedOnly;

        pushItemsWhenLocked();
    }

    @Override
    public boolean isMonospacedOnly() {
        return myMonospacedOnly;
    }

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    private void fireListeners(@Nullable String value) {
        getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, value));
    }
}
