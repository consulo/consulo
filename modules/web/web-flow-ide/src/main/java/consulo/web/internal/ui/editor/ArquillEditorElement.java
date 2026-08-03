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
package consulo.web.internal.ui.editor;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.shared.Registration;

import consulo.web.internal.ui.editor.gutter.GutterBand;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Wraps the compiled arquill editor bundle, which exposes window.arquillEditor.createEditor(options).
 *
 * @author VISTALL
 */
// served straight from META-INF/resources, the vite bundle skips rebuilding when only annotations change
@Tag("pre")
@StyleSheet("/arquill/arquillEditor.css")
public class ArquillEditorElement extends Component implements HasSize {
    /**
     * Fired when the user moves the caret in the browser, by clicking or by keyboard navigation.
     */
    @DomEvent("arquill-caret")
    public static class ArquillCaretEvent extends ComponentEvent<ArquillEditorElement> {
        private final int myOffset;

        public ArquillCaretEvent(ArquillEditorElement source, boolean fromClient, @EventData("event.detail.offset") int offset) {
            super(source, fromClient);
            myOffset = offset;
        }

        public int getOffset() {
            return myOffset;
        }
    }

    /**
     * Fired when the user edits the text in the browser. Offsets are in document coordinates, and refer to the
     * text as it was before the edit, so they can be fed straight into the platform document.
     */
    @DomEvent("arquill-change")
    public static class ArquillTextChangeEvent extends ComponentEvent<ArquillEditorElement> {
        private final int myStart;
        private final int myRemovedCharCount;
        private final String myText;

        public ArquillTextChangeEvent(
            ArquillEditorElement source,
            boolean fromClient,
            @EventData("event.detail.start") int start,
            @EventData("event.detail.removedCharCount") int removedCharCount,
            @EventData("event.detail.text") String text
        ) {
            super(source, fromClient);
            myStart = start;
            myRemovedCharCount = removedCharCount;
            myText = text;
        }

        public int getStart() {
            return myStart;
        }

        public int getRemovedCharCount() {
            return myRemovedCharCount;
        }

        public String getText() {
            return myText;
        }
    }

    /**
     * Fired while the pointer moves over the text with ctrl/cmd held, and with offset -1 once the modifier is
     * released, so the server can drop the link highlight.
     */
    @DomEvent("arquill-ctrl-hover")
    public static class ArquillCtrlHoverEvent extends ComponentEvent<ArquillEditorElement> {
        private final int myOffset;

        public ArquillCtrlHoverEvent(ArquillEditorElement source, boolean fromClient, @EventData("event.detail.offset") int offset) {
            super(source, fromClient);
            myOffset = offset;
        }

        public int getOffset() {
            return myOffset;
        }
    }

    /**
     * Fired on ctrl/cmd + left click and on middle click - the two mouse shortcuts bound to go to declaration.
     */
    @DomEvent("arquill-ctrl-click")
    public static class ArquillCtrlClickEvent extends ComponentEvent<ArquillEditorElement> {
        private final int myOffset;

        public ArquillCtrlClickEvent(ArquillEditorElement source, boolean fromClient, @EventData("event.detail.offset") int offset) {
            super(source, fromClient);
            myOffset = offset;
        }

        public int getOffset() {
            return myOffset;
        }
    }

    /**
     * Fired when a gutter icon is left clicked. The id is the index the mark had in the array last passed to
     * {@link #setGutterMarks(String)} - a line is not enough, several markers can share one.
     */
    @DomEvent("arquill-gutter-click")
    public static class ArquillGutterClickEvent extends ComponentEvent<ArquillEditorElement> {
        private final int myId;

        public ArquillGutterClickEvent(ArquillEditorElement source, boolean fromClient, @EventData("event.detail.id") int id) {
            super(source, fromClient);
            myId = id;
        }

        public int getId() {
            return myId;
        }
    }

    /**
     * Fired when the user collapses or expands a region through the orion folding ruler. The offset is the
     * start of the region in document coordinates, which is what {@link #setFoldRegions(String)} sent.
     */
    @DomEvent("arquill-fold")
    public static class ArquillFoldEvent extends ComponentEvent<ArquillEditorElement> {
        private final int myStart;
        private final boolean myCollapsed;

        public ArquillFoldEvent(
            ArquillEditorElement source,
            boolean fromClient,
            @EventData("event.detail.start") int start,
            @EventData("event.detail.collapsed") boolean collapsed
        ) {
            super(source, fromClient);
            myStart = start;
            myCollapsed = collapsed;
        }

        public int getStart() {
            return myStart;
        }

        public boolean isCollapsed() {
            return myCollapsed;
        }
    }

    private String myText;

    private boolean myReadOnly;

    public ArquillEditorElement(String text) {
        myText = text;

        // the gutter overlay and the status panel are placed against this element, the class makes it their
        // containing block
        getElement().getClassList().add("arquill-editor");
        getElement().getStyle().set("margin", "0").set("padding", "0");

        // registering here also installs the dom listener - flow only forwards the event while the server listens
        addListener(ArquillTextChangeEvent.class, event -> applyToCache(event.getStart(), event.getStart() + event.getRemovedCharCount(), event.getText()));

        // the server pushes into the editor while the scripts are still loading, so the api it calls has to
        // exist before any of them. the stub collects the calls, install replays them once it takes over -
        // this runs first, it is registered ahead of every other script of this element
        getElement().executeJs("""
            const pending = this.$arquillPending = [];
            this.$arquillApi = new Proxy({}, {
                get: (target, name) => (...args) => pending.push([name, args])
            });
            """);
    }

    public void setText(String text) {
        myText = text;

        getElement().executeJs("this.$arquillApi.setText($0);", text);
    }

    /**
     * Replaces {@code [start, end)} without resending the whole document.
     */
    public void replaceText(int start, int end, String text) {
        applyToCache(start, end, text);

        getElement().executeJs("this.$arquillApi.replaceText($0, $1, $2);", start, end, text);
    }

    private void applyToCache(int start, int end, String text) {
        if (start >= 0 && start <= end && end <= myText.length()) {
            myText = myText.substring(0, start) + text + myText.substring(end);
        }
    }

    public String getText() {
        return myText;
    }

    public void setReadOnly(boolean readOnly) {
        myReadOnly = readOnly;

        getElement().executeJs("this.$arquillApi.setReadOnly($0);", readOnly);
    }

    /**
     * The editor font of the color scheme. Orion measures its line metrics once, while the view is being
     * created, and lays the whole view out against what it measured - so the client cannot merely restyle,
     * it has to make orion measure again.
     *
     * @param fontName family name, the one the scheme stores
     * @param fontSize size in points, applied as css pixels
     */
    public void setFont(String fontName, int fontSize) {
        getElement().executeJs("this.$arquillApi.setFont($0, $1);", fontName, fontSize);
    }

    /**
     * Default colors of the scheme. Nothing of the scheme is readable in the browser, so they travel the same
     * way the font does, and the stylesheet reads them instead of carrying colors of its own.
     */
    public void setColors(
        String background,
        String foreground,
        @Nullable String selectionBackground,
        @Nullable String caretRowBackground
    ) {
        getElement().executeJs(
            "this.$arquillApi.setColors($0, $1, $2, $3);",
            background,
            foreground,
            selectionBackground,
            caretRowBackground
        );
    }

    /**
     * The stylesheet of the attribute keys the style ranges name - one rule per key, replaced as a whole when
     * the scheme changes.
     */
    public void setSchemeStyles(String css) {
        getElement().executeJs("this.$arquillApi.setSchemeStyles($0);", css);
    }

    public Registration addTextChangeListener(ComponentEventListener<ArquillTextChangeEvent> listener) {
        return addListener(ArquillTextChangeEvent.class, listener);
    }

    public void setCaretOffset(int offset) {
        getElement().executeJs("this.$arquillApi.setCaretOffset($0);", offset);
    }

    public Registration addCaretListener(ComponentEventListener<ArquillCaretEvent> listener) {
        return addListener(ArquillCaretEvent.class, listener);
    }

    public Registration addCtrlHoverListener(ComponentEventListener<ArquillCtrlHoverEvent> listener) {
        return addListener(ArquillCtrlHoverEvent.class, listener);
    }

    public Registration addCtrlClickListener(ComponentEventListener<ArquillCtrlClickEvent> listener) {
        return addListener(ArquillCtrlClickEvent.class, listener);
    }

    public Registration addGutterClickListener(ComponentEventListener<ArquillGutterClickEvent> listener) {
        return addListener(ArquillGutterClickEvent.class, listener);
    }

    public Registration addFoldListener(ComponentEventListener<ArquillFoldEvent> listener) {
        return addListener(ArquillFoldEvent.class, listener);
    }

    /**
     * Turns the pointer into a hand while ctrl/cmd is held over a reference. Only the server knows whether the
     * offset under the pointer resolves to anything, so the client cannot decide this on its own.
     */
    public void setLinkHovered(boolean hovered) {
        getElement().executeJs("this.$arquillApi.setLinkHovered($0);", hovered);
    }

    /**
     * @param marksJson array of {id, line, iconUrl, tooltip} - gutter icons produced by the line marker pass
     */
    public void setGutterMarks(String marksJson) {
        getElement().executeJs("this.$arquillApi.setGutterMarks($0);", marksJson);
    }

    /**
     * @param statusJson {items:[{text, iconUrls}], tooltip, analyzing} - the daemon analyze status of the whole
     *                   file, where {@code tooltip} is the html shown while the pointer is over the panel
     */
    public void setAnalyzeStatus(String statusJson) {
        getElement().executeJs("this.$arquillApi.setAnalyzeStatus($0);", statusJson);
    }

    /**
     * @param rangesJson array of {start, end, html} in absolute document offsets - the message of every highlight
     *                   the daemon produced, shown while the pointer rests over the range
     */
    public void setTooltipRanges(String rangesJson) {
        getElement().executeJs("this.$arquillApi.setTooltipRanges($0);", rangesJson);
    }

    /**
     * @param rangesJson array of {start, end, style} in absolute document offsets, sorted by start
     */
    public void setStyleRanges(String rangesJson) {
        getElement().executeJs("this.$arquillApi.setStyleRanges($0);", rangesJson);
    }

    /**
     * @param regionsJson array of {start, end, collapsed} in absolute document offsets - the fold regions the
     *                    platform folding model holds
     */
    public void setFoldRegions(String regionsJson) {
        getElement().executeJs("this.$arquillApi.setFoldRegions($0);", regionsJson);
    }

    /**
     * @param marksJson {visible, marks:[{line, start, end, layer, thin, color, tooltip}]} - the highlighters
     *                  carrying an error stripe color. They become annotations of the orion overview ruler,
     *                  which is the error stripe, so the offsets are the document ones the annotation model
     *                  works in and the line is only there to tell apart the marks sharing a row of the ruler
     */
    public void setErrorStripeMarks(String marksJson) {
        getElement().executeJs("this.$arquillApi.setErrorStripeMarks($0);", marksJson);
    }

    /**
     * The line marker presentations of the document, already resolved to bands. Handed over as a property
     * rather than an argument of the call - a list crosses as itself that way, and the browser is not left
     * parsing a string the server just printed.
     */
    public void setGutterBands(List<GutterBand> bands) {
        getElement().setPropertyList("gutterBands", bands);
        getElement().executeJs("this.$arquillApi.setGutterBands(this.gutterBands);");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // the bundle measures the parent, so the editor can only be created once the element is in the dom.
        // the scripts are injected by hand - flow drops @JavaScript pointing at a context absolute path
        getElement().executeJs("""
            const element = this;

            const load = (id, src, loaded) => loaded() ? Promise.resolve() : new Promise(resolve => {
                let script = document.getElementById(id);
                if (!script) {
                    script = document.createElement('script');
                    script.id = id;
                    script.src = src;
                    document.head.appendChild(script);
                }
                script.addEventListener('load', resolve);
            });

            // the element script reaches into the bundle from install alone, so the two may load side by side
            // as long as nothing installs before both are in
            Promise.all([
                load('arquill-editor-script', '/arquill/arquillEditor.js', () => window.arquillEditor),
                load('arquill-editor-element-script', '/arquill/arquillEditorElement.js', () => window.arquillEditorElement)
            ]).then(() => window.arquillEditorElement.install(element, $0, $1));
            """, myText, myReadOnly);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        getElement().executeJs("this.$arquillApi.destroy();");

        super.onDetach(detachEvent);
    }
}
