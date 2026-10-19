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
package consulo.web.editor.impl.internal.bridge;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import consulo.ui.Component;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Editors side by side with the strip of the bridge between each two of them. The strips are drawn by the browser
 * from the data the bridge pushes, against the editors next to them, so scrolling an editor never waits on the server.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public class WebEditorBridgeComponent extends VaadinComponentDelegate<WebEditorBridgeComponent.Vaadin> {
    public class Vaadin extends Div implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebEditorBridgeComponent.this;
        }

        @Override
        protected void onAttach(AttachEvent attachEvent) {
            super.onAttach(attachEvent);
            install();
        }
    }

    private static final String INSTALL = """
        const divider = $0, left = $1, right = $2, data = $3, version = $4;
        const run = attempt => {
            if (window.arquillBridge) {
                window.arquillBridge.install(divider, left, right);
                if (data && (divider.$arquillBridgeVersion || 0) <= version) {
                    divider.$arquillBridgeVersion = version;
                    window.arquillBridge.update(divider, data);
                }
            }
            else if (attempt < 400) {
                setTimeout(() => run(attempt + 1), 50);
            }
        };
        run(0);
        """;

    private final List<Div> myTitles = new ArrayList<>();
    private final List<Div> mySides = new ArrayList<>();
    private final List<Div> myDividers = new ArrayList<>();
    private final List<@Nullable String> myData = new ArrayList<>();
    private final List<Integer> myVersions = new ArrayList<>();
    private int myVersion;

    public WebEditorBridgeComponent(List<Component> editors) {
        Vaadin vaadin = getVaadinComponent();
        vaadin.setSizeFull();

        StringBuilder columns = new StringBuilder();
        for (int i = 0; i < editors.size(); i++) {
            if (i > 0) {
                columns.append(" 20px ");
            }
            columns.append("minmax(0, 1fr)");
        }
        vaadin.getStyle()
            .set("display", "grid")
            .set("grid-template-columns", columns.toString())
            .set("grid-template-rows", "auto minmax(0, 1fr)")
            .set("overflow", "hidden");

        for (int i = 0; i < editors.size(); i++) {
            String column = String.valueOf(i * 2 + 1);

            Div title = new Div();
            title.getStyle().set("grid-row", "1").set("grid-column", column).set("min-width", "0");
            myTitles.add(title);

            Div side = new Div();
            side.getStyle().set("grid-row", "2").set("grid-column", column).set("min-width", "0").set("min-height", "0").set("display", "flex");
            com.vaadin.flow.component.Component editor = TargetVaadin.to(editors.get(i));
            editor.getElement().getStyle().set("flex", "1 1 auto").set("min-width", "0");
            side.add(editor);
            mySides.add(side);

            vaadin.add(title, side);

            if (i > 0) {
                Div divider = new Div();
                divider.addClassName("arquill-bridge-divider");
                divider.getStyle().set("grid-row", "2").set("grid-column", String.valueOf(i * 2)).set("position", "relative").set("overflow", "hidden");
                myDividers.add(divider);
                myData.add(null);
                myVersions.add(0);
                vaadin.add(divider);
            }
        }
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    public void setTitles(List<? extends @Nullable Component> titles) {
        for (int i = 0; i < myTitles.size(); i++) {
            Div holder = myTitles.get(i);
            holder.removeAll();
            Component title = i < titles.size() ? titles.get(i) : null;
            if (title != null) {
                holder.add(TargetVaadin.to(title));
            }
        }
    }

    public void setData(int gap, String data) {
        int version = ++myVersion;
        myData.set(gap, data);
        myVersions.set(gap, version);
        myDividers.get(gap).getElement().executeJs("""
            const divider = this, data = $0, version = $1;
            const run = attempt => {
                if (window.arquillBridge && divider.$arquillBridge) {
                    if ((divider.$arquillBridgeVersion || 0) <= version) {
                        divider.$arquillBridgeVersion = version;
                        window.arquillBridge.update(divider, data);
                    }
                }
                else if (attempt < 400) {
                    setTimeout(() => run(attempt + 1), 50);
                }
            };
            run(0);
            """, data, version);
    }

    public void makeVisible(int masterIndex, int[] startLines, int[] endLines) {
        Element first = myDividers.get(0).getElement();
        Element second = myDividers.size() > 1 ? myDividers.get(1).getElement() : first;
        getVaadinComponent().getElement().executeJs("""
            if (window.arquillBridge) {
                const dividers = $3 === $4 ? [$3] : [$3, $4];
                window.arquillBridge.makeVisibleAll(dividers, $0, JSON.parse($1), JSON.parse($2));
            }
            """, masterIndex, toJson(startLines), toJson(endLines), first, second);
    }

    public void suspendSyncScroll() {
        for (Div divider : myDividers) {
            divider.getElement().executeJs("""
                if (window.arquillBridge && this.$arquillBridge) {
                    window.arquillBridge.suspend(this);
                }
                """);
        }
    }

    private void install() {
        for (int i = 0; i < myDividers.size(); i++) {
            getVaadinComponent().getElement().executeJs(
                INSTALL,
                myDividers.get(i).getElement(),
                mySides.get(i).getElement(),
                mySides.get(i + 1).getElement(),
                myData.get(i),
                myVersions.get(i)
            );
        }
    }

    private static String toJson(int[] values) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(values[i]);
        }
        return builder.append(']').toString();
    }
}
