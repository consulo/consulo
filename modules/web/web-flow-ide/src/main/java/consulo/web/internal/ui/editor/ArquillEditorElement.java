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
import com.vaadin.flow.dom.DebouncePhase;
import com.vaadin.flow.shared.Registration;

import consulo.web.internal.ui.editor.gutter.GutterBand;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Wraps the compiled arquill editor bundle, which exposes window.arquillEditor.createEditor(options).
 *
 * @author VISTALL
 */
// served straight from META-INF/resources, the vite bundle skips rebuilding when only annotations change
@Tag("pre")
@StyleSheet("/arquill/arquillEditor.css")
public class ArquillEditorElement extends Component implements HasSize {
    private static final int VIEWPORT_THROTTLE_MS = 50;

    private static final int HOVER_THROTTLE_MS = 60;

    /**
     * Fired when the user moves the caret in the browser, by clicking or by keyboard navigation.
     */
    /**
     * The size of a character cell, measured in the browser when the font is applied and again once the real face
     * has loaded. The platform maps positions to points and back with it - moving the caret up and down keeps the
     * column it started from as an x, and an editor which cannot answer cannot move its caret at all.
     */
    @DomEvent("arquill-metrics")
    public static class ArquillMetricsEvent extends ComponentEvent<ArquillEditorElement> {
        private final int myCharWidth;
        private final int myLineHeight;

        public ArquillMetricsEvent(
            ArquillEditorElement source,
            boolean fromClient,
            @EventData("event.detail.charWidth") int charWidth,
            @EventData("event.detail.lineHeight") int lineHeight
        ) {
            super(source, fromClient);
            myCharWidth = charWidth;
            myLineHeight = lineHeight;
        }

        public int getCharWidth() {
            return myCharWidth;
        }

        public int getLineHeight() {
            return myLineHeight;
        }
    }

    @DomEvent("arquill-caret")
    public static class ArquillCaretEvent extends ComponentEvent<ArquillEditorElement> {
        private final int myOffset;
        private final int mySelectionStart;
        private final int mySelectionEnd;
        private final int myCaretX;
        private final int myCaretY;
        private final int myCaretHeight;
        private final int myTextX;
        private final boolean myRectOnly;

        public ArquillCaretEvent(
            ArquillEditorElement source,
            boolean fromClient,
            @EventData("event.detail.offset") int offset,
            @EventData("event.detail.selectionStart") int selectionStart,
            @EventData("event.detail.selectionEnd") int selectionEnd,
            @EventData("event.detail.caretX") int caretX,
            @EventData("event.detail.caretY") int caretY,
            @EventData("event.detail.caretHeight") int caretHeight,
            @EventData("event.detail.textX") int textX,
            @EventData("event.detail.rectOnly") boolean rectOnly
        ) {
            super(source, fromClient);
            myRectOnly = rectOnly;
            myOffset = offset;
            mySelectionStart = selectionStart;
            mySelectionEnd = selectionEnd;
            myCaretX = caretX;
            myCaretY = caretY;
            myCaretHeight = caretHeight;
            myTextX = textX;
        }

        public int getOffset() {
            return myOffset;
        }

        /**
         * Start of what is selected, equal to {@link #getSelectionEnd()} when nothing is. A drag holds this still and
         * moves only the end, so a report of the caret alone left the platform with a selection it had never been
         * told about.
         */
        public int getSelectionStart() {
            return mySelectionStart;
        }

        public int getSelectionEnd() {
            return mySelectionEnd;
        }

        /**
         * Pixels from the left of the editor component. Where anything anchored to the caret goes - the completion
         * popup above all - and it rides on this event because the server cannot measure the browser.
         */
        public int getCaretX() {
            return myCaretX;
        }

        /**
         * Pixels from the top of the editor component, at the top of the caret line.
         */
        public int getCaretY() {
            return myCaretY;
        }

        /**
         * Height of the caret line, so a popup with no room below it can go above the line rather than over it.
         */
        public int getCaretHeight() {
            return myCaretHeight;
        }

        public int getTextX() {
            return myTextX;
        }

        /**
         * The caret did not move - the platform moved it and this only says where it ended up on screen. Acting on
         * it as a move would put the caret where it already is, and anything watching the caret would see one.
         */
        public boolean isRectOnly() {
            return myRectOnly;
        }
    }

    /**
     * Fired when the user types a character. The browser has been stopped from inserting it - the platform puts it
     * in instead, so the typed action chain runs and the edit comes back as a document change.
     */
    @DomEvent("arquill-typed")
    public static class ArquillTypedEvent extends ComponentEvent<ArquillEditorElement> {
        private final String myText;

        public ArquillTypedEvent(
            ArquillEditorElement source,
            boolean fromClient,
            @EventData("event.detail.text") String text
        ) {
            super(source, fromClient);
            myText = text;
        }

        /**
         * Always one character - anything else is left to the editor and arrives as a change instead.
         * <p/>
         * Where it goes is not carried with it: the keystroke was stopped before the editor could act on it, so the
         * caret there has not moved and would report the same offset for every character of a run.
         */
        public String getText() {
            return myText;
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
     * Fired when a run of an inlay is clicked with ctrl/cmd held. The id is the {@code click} the run carried in
     * the array last passed to {@link #setInlays(String)} - an inlay stands in no document offset, so the
     * {@code arquill-ctrl-click} channel, which reports one, cannot answer for it.
     */
    @DomEvent("arquill-inlay-click")
    public static class ArquillInlayClickEvent extends ComponentEvent<ArquillEditorElement> {
        private final int myId;
        private final boolean myControlDown;

        public ArquillInlayClickEvent(
            ArquillEditorElement source,
            boolean fromClient,
            @EventData("event.detail.id") int id,
            @EventData("event.detail.controlDown") boolean controlDown
        ) {
            super(source, fromClient);
            myId = id;
            myControlDown = controlDown;
        }

        public int getId() {
            return myId;
        }

        public boolean isControlDown() {
            return myControlDown;
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

    @DomEvent("arquill-gutter-context-menu")
    public static class ArquillGutterContextMenuEvent extends ComponentEvent<ArquillEditorElement> {
        private final int myLine;
        private final int myMarkId;
        private final int myAnnotationColumn;

        public ArquillGutterContextMenuEvent(
            ArquillEditorElement source,
            boolean fromClient,
            @EventData("event.detail.line") int line,
            @EventData("event.detail.markId") int markId,
            @EventData("event.detail.annotationColumn") int annotationColumn
        ) {
            super(source, fromClient);
            myLine = line;
            myMarkId = markId;
            myAnnotationColumn = annotationColumn;
        }

        public int getLine() {
            return myLine;
        }

        public int getMarkId() {
            return myMarkId;
        }

        public int getAnnotationColumn() {
            return myAnnotationColumn;
        }
    }

    @DomEvent("arquill-annotation-hover")
    public static class ArquillAnnotationHoverEvent extends ComponentEvent<ArquillEditorElement> {
        private final int myLine;

        public ArquillAnnotationHoverEvent(
            ArquillEditorElement source,
            boolean fromClient,
            @EventData("event.detail.line") int line
        ) {
            super(source, fromClient);
            myLine = line;
        }

        public int getLine() {
            return myLine;
        }
    }

    @DomEvent("arquill-annotation-click")
    public static class ArquillAnnotationClickEvent extends ComponentEvent<ArquillEditorElement> {
        private final int myLine;
        private final int myColumn;

        public ArquillAnnotationClickEvent(
            ArquillEditorElement source,
            boolean fromClient,
            @EventData("event.detail.line") int line,
            @EventData("event.detail.column") int column
        ) {
            super(source, fromClient);
            myLine = line;
            myColumn = column;
        }

        public int getLine() {
            return myLine;
        }

        public int getColumn() {
            return myColumn;
        }
    }

    @DomEvent("arquill-gutter-hover")
    public static class ArquillGutterHoverEvent extends ComponentEvent<ArquillEditorElement> {
        private final int myLine;

        public ArquillGutterHoverEvent(
            ArquillEditorElement source,
            boolean fromClient,
            @EventData("event.detail.line") int line
        ) {
            super(source, fromClient);
            myLine = line;
        }

        public int getLine() {
            return myLine;
        }
    }

    @DomEvent("arquill-gutter-line-click")
    public static class ArquillGutterLineClickEvent extends ComponentEvent<ArquillEditorElement> {
        private final int myLine;
        private final boolean myAltKey;
        private final boolean myShiftKey;
        private final boolean myCtrlKey;
        private final boolean myMetaKey;

        public ArquillGutterLineClickEvent(
            ArquillEditorElement source,
            boolean fromClient,
            @EventData("event.detail.line") int line,
            @EventData("event.detail.altKey") boolean altKey,
            @EventData("event.detail.shiftKey") boolean shiftKey,
            @EventData("event.detail.ctrlKey") boolean ctrlKey,
            @EventData("event.detail.metaKey") boolean metaKey
        ) {
            super(source, fromClient);
            myLine = line;
            myAltKey = altKey;
            myShiftKey = shiftKey;
            myCtrlKey = ctrlKey;
            myMetaKey = metaKey;
        }

        public int getLine() {
            return myLine;
        }

        public boolean isAltKey() {
            return myAltKey;
        }

        public boolean isShiftKey() {
            return myShiftKey;
        }

        public boolean isCtrlKey() {
            return myCtrlKey;
        }

        public boolean isMetaKey() {
            return myMetaKey;
        }
    }

    @DomEvent("arquill-viewport")
    public static class ArquillViewportEvent extends ComponentEvent<ArquillEditorElement> {
        private final int myX;
        private final int myY;
        private final int myWidth;
        private final int myHeight;

        public ArquillViewportEvent(
            ArquillEditorElement source,
            boolean fromClient,
            @EventData("event.detail.x") int x,
            @EventData("event.detail.y") int y,
            @EventData("event.detail.width") int width,
            @EventData("event.detail.height") int height
        ) {
            super(source, fromClient);
            myX = x;
            myY = y;
            myWidth = width;
            myHeight = height;
        }

        public int getX() {
            return myX;
        }

        public int getY() {
            return myY;
        }

        public int getWidth() {
            return myWidth;
        }

        public int getHeight() {
            return myHeight;
        }
    }

    private String myText;

    private boolean myReadOnly;

    /**
     * What each decoration channel last sent, keyed by the api call. The platform rebuilds every channel on
     * every pass - a typed character runs the pass from its own document listener and again when the daemon
     * finishes, and most channels come out byte for byte the same. A push that repeats the last one draws
     * nothing new, so it does not travel. Cleared on attach: a reloaded browser holds none of it.
     */
    private final Map<String, Object> myLastPushed = new HashMap<>();

    private boolean isUnchanged(String channel, Object value) {
        if (Objects.equals(myLastPushed.get(channel), value)) {
            return true;
        }
        myLastPushed.put(channel, value);
        return false;
    }

    public ArquillEditorElement(String text) {
        myText = text;

        // the gutter overlay and the status panel are placed against this element, the class makes it their
        // containing block
        getElement().getClassList().add("arquill-editor");
        getElement().getStyle().set("margin", "0").set("padding", "0");

        // registering here also installs the dom listener - flow only forwards the event while the server listens
        addListener(ArquillTextChangeEvent.class, event -> applyToCache(event.getStart(), event.getStart() + event.getRemovedCharCount(), event.getText()));
    }

    /**
     * The server pushes into the editor while the scripts are still loading, so the api it calls has to exist
     * before any of them. The stub collects those calls and install replays them once it takes over.
     * <p>
     * It belongs to attaching rather than to the constructor: a browser reload builds a new ui and a new dom
     * for the very same server side editor, and the constructor does not run a second time - the stub of the
     * old document is gone and everything pushed while the bundle loads would be thrown away.
     */
    private void installApiStub() {
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
    /**
     * How the caret is drawn. The browser draws its own one pixel wide, in the colour of the text it stands in and
     * blinking at a rate of its own, so the editor draws one instead and is told what it should look like.
     *
     * @param blinkPeriod milliseconds, or {@code 0} for a caret which does not blink
     */
    public void setCaretStyle(int width, int blinkPeriod) {
        getElement().executeJs("this.$arquillApi.setCaretStyle($0, $1);", width, blinkPeriod);
    }

    public void setFont(String fontName, int fontSize, double lineSpacing) {
        getElement().executeJs("this.$arquillApi.setFont($0, $1, $2);", fontName, fontSize, lineSpacing);
    }

    /**
     * Default colors of the scheme. Nothing of the scheme is readable in the browser, so they travel the same
     * way the font does, and the stylesheet reads them instead of carrying colors of its own.
     */
    public void setColors(
        String background,
        String foreground,
        @Nullable String selectionBackground,
        @Nullable String selectionForeground,
        @Nullable String caretRowBackground
    ) {
        getElement().executeJs(
            "this.$arquillApi.setColors($0, $1, $2, $3, $4);",
            background,
            foreground,
            selectionBackground,
            selectionForeground,
            caretRowBackground
        );
    }

    public void setGutterColors(@Nullable String background, @Nullable String separator) {
        setStyleProperty("--arquill-editor-gutter-background", background);
        setStyleProperty("--arquill-editor-gutter-separator", separator);
    }

    /**
     * The line number colours of the scheme, which the awt gutter paints itself. Set as custom properties rather
     * than pushed through the editor api - the bundled orion stylesheet gives the ruler a colour of its own, and
     * the sheet of the ide only has to outweigh it.
     */
    public void setLineNumberColors(@Nullable String color, @Nullable String caretRowColor) {
        setStyleProperty("--arquill-editor-line-number-color", color);
        setStyleProperty("--arquill-editor-line-number-caret-row-color", caretRowColor);
    }

    private void setStyleProperty(String name, @Nullable String value) {
        if (value == null) {
            getElement().getStyle().remove(name);
        }
        else {
            getElement().getStyle().set(name, value);
        }
    }

    /**
     * The stylesheet of the attribute keys the style ranges name - one rule per key, replaced as a whole when
     * the scheme changes.
     */
    public void setSchemeStyles(String css) {
        if (isUnchanged("schemeStyles", css)) {
            return;
        }
        getElement().executeJs("this.$arquillApi.setSchemeStyles($0);", css);
    }

    public Registration addTextChangeListener(ComponentEventListener<ArquillTextChangeEvent> listener) {
        return addListener(ArquillTextChangeEvent.class, listener);
    }

    public void setCaretOffset(int offset) {
        setSelection(offset, offset);
    }

    /**
     * Where the caret is and what is selected, as one push. The caret is at {@code end} - the edge a growing range
     * moves - and a caret with nothing selected is a range of no width.
     * <p>
     * One channel rather than two on purpose: a caret push and a selection push would race, and a caret arriving
     * after a selection collapses the range that was just drawn. Whoever moved either of them says what both are.
     */
    public void setSelection(int start, int end) {
        getElement().executeJs("this.$arquillApi.setSelection($0, $1);", start, end);
    }

    public Registration addTypedListener(ComponentEventListener<ArquillTypedEvent> listener) {
        return addListener(ArquillTypedEvent.class, listener);
    }

    public Registration addCaretListener(ComponentEventListener<ArquillCaretEvent> listener) {
        return addListener(ArquillCaretEvent.class, listener);
    }

    public Registration addMetricsListener(ComponentEventListener<ArquillMetricsEvent> listener) {
        return addListener(ArquillMetricsEvent.class, listener);
    }

    public Registration addCtrlHoverListener(ComponentEventListener<ArquillCtrlHoverEvent> listener) {
        return addListener(ArquillCtrlHoverEvent.class, listener);
    }

    public Registration addCtrlClickListener(ComponentEventListener<ArquillCtrlClickEvent> listener) {
        return addListener(ArquillCtrlClickEvent.class, listener);
    }

    public Registration addInlayClickListener(ComponentEventListener<ArquillInlayClickEvent> listener) {
        return addListener(ArquillInlayClickEvent.class, listener);
    }

    public Registration addGutterClickListener(ComponentEventListener<ArquillGutterClickEvent> listener) {
        return addListener(ArquillGutterClickEvent.class, listener);
    }

    public Registration addGutterLineClickListener(ComponentEventListener<ArquillGutterLineClickEvent> listener) {
        return addListener(ArquillGutterLineClickEvent.class, listener);
    }

    public Registration addGutterContextMenuListener(ComponentEventListener<ArquillGutterContextMenuEvent> listener) {
        return addListener(ArquillGutterContextMenuEvent.class, listener);
    }

    public Registration addGutterHoverListener(ComponentEventListener<ArquillGutterHoverEvent> listener) {
        return getEventBus().addListener(
            ArquillGutterHoverEvent.class,
            listener,
            registration -> registration.debounce(HOVER_THROTTLE_MS, DebouncePhase.LEADING, DebouncePhase.TRAILING)
        );
    }

    public Registration addAnnotationHoverListener(ComponentEventListener<ArquillAnnotationHoverEvent> listener) {
        return getEventBus().addListener(
            ArquillAnnotationHoverEvent.class,
            listener,
            registration -> registration.debounce(HOVER_THROTTLE_MS, DebouncePhase.LEADING, DebouncePhase.TRAILING)
        );
    }

    public Registration addAnnotationClickListener(ComponentEventListener<ArquillAnnotationClickEvent> listener) {
        return addListener(ArquillAnnotationClickEvent.class, listener);
    }

    public Registration addFoldListener(ComponentEventListener<ArquillFoldEvent> listener) {
        return addListener(ArquillFoldEvent.class, listener);
    }

    public Registration addViewportListener(ComponentEventListener<ArquillViewportEvent> listener) {
        return getEventBus().addListener(
            ArquillViewportEvent.class,
            listener,
            registration -> registration.debounce(
                VIEWPORT_THROTTLE_MS,
                DebouncePhase.LEADING,
                DebouncePhase.INTERMEDIATE,
                DebouncePhase.TRAILING
            )
        );
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
    public void setGutterHoverMark(String markJson) {
        if (isUnchanged("gutterHoverMark", markJson)) {
            return;
        }
        getElement().executeJs("this.$arquillApi.setGutterHoverMark($0);", markJson);
    }

    public void setGutterMarks(String marksJson) {
        if (isUnchanged("gutterMarks", marksJson)) {
            return;
        }
        getElement().executeJs("this.$arquillApi.setGutterMarks($0);", marksJson);
    }

    public void setTextAnnotations(String annotationsJson) {
        if (isUnchanged("textAnnotations", annotationsJson)) {
            return;
        }
        getElement().executeJs("this.$arquillApi.setTextAnnotations($0);", annotationsJson);
    }

    public void setAnnotationTooltip(String tooltipJson) {
        if (isUnchanged("annotationTooltip", tooltipJson)) {
            return;
        }
        getElement().executeJs("this.$arquillApi.setAnnotationTooltip($0);", tooltipJson);
    }

    /**
     * @param statusJson {items:[{text, iconUrls}], tooltip, analyzing} - the daemon analyze status of the whole
     *                   file, where {@code tooltip} is the html shown while the pointer is over the panel
     */
    public void setAnalyzeStatus(String statusJson) {
        if (isUnchanged("analyzeStatus", statusJson)) {
            return;
        }
        getElement().executeJs("this.$arquillApi.setAnalyzeStatus($0);", statusJson);
    }

    /**
     * @param rangesJson array of {start, end, html} in absolute document offsets - the message of every highlight
     *                   the daemon produced, shown while the pointer rests over the range
     */
    public void setTooltipRanges(String rangesJson) {
        if (isUnchanged("tooltipRanges", rangesJson)) {
            return;
        }
        getElement().executeJs("this.$arquillApi.setTooltipRanges($0);", rangesJson);
    }

    /**
     * @param rangesJson array of {start, end, style} in absolute document offsets, sorted by start
     */
    public void setStyleRanges(String rangesJson) {
        if (isUnchanged("styleRanges", rangesJson)) {
            return;
        }
        getElement().executeJs("this.$arquillApi.setStyleRanges($0);", rangesJson);
    }

    /**
     * @param regionsJson array of {start, end, collapsed} in absolute document offsets - the fold regions the
     *                    platform folding model holds
     */
    public void setFoldRegions(String regionsJson) {
        if (isUnchanged("foldRegions", regionsJson)) {
            return;
        }
        getElement().executeJs("this.$arquillApi.setFoldRegions($0);", regionsJson);
    }

    /**
     * Drops what the browser is known to hold, so the next push of a channel goes out even when it carries the
     * same value. What the browser has can differ from the last push - orion cannot express two folded regions
     * one inside the other - and then only a push it does not skip brings the two back together.
     */
    public void invalidatePushed(String channel) {
        myLastPushed.remove(channel);
    }

    /**
     * The markup the folding ruler draws its anchors with. The editor owns no icons, the platform ones are
     * handed over as the markup of a live image - a tag which reloads itself when the style of the ide changes,
     * which a url written into the ruler could not do.
     *
     * @param expandedHtml       head of a region which is open, on the row it begins on
     * @param collapsedHtml      anchor of a region which is folded
     * @param expandedBottomHtml foot of a region which is open, on the row it ends on - the lower end of the
     *                           bracket the awt gutter draws around what an open region holds
     */
    public void setFoldingAnchors(String expandedHtml, String collapsedHtml, String expandedBottomHtml) {
        String anchors = expandedHtml + "\u0000" + collapsedHtml + "\u0000" + expandedBottomHtml;
        if (isUnchanged("foldingAnchors", anchors)) {
            return;
        }
        getElement().executeJs(
            "this.$arquillApi.setFoldingAnchors($0, $1, $2);",
            expandedHtml,
            collapsedHtml,
            expandedBottomHtml
        );
    }

    /**
     * The inlays of the document. Each anchor becomes a zero width projection of the orion model - text standing in
     * the view without being in the document, which is what a fold placeholder already is.
     *
     * @param inlaysJson array of {offset, segments:[{text, style}]} in absolute document offsets, one entry per
     *                   anchor. The segments of an anchor are laid out in the order given, and a segment whose text
     *                   ends in a newline is what puts a block inlay on a line of its own
     */
    public void setInlays(String inlaysJson) {
        if (isUnchanged("inlays", inlaysJson)) {
            return;
        }
        getElement().executeJs("this.$arquillApi.setInlays($0);", inlaysJson);
    }

    /**
     * @param marksJson {visible, marks:[{line, start, end, layer, thin, color, tooltip}]} - the highlighters
     *                  carrying an error stripe color. They become annotations of the orion overview ruler,
     *                  which is the error stripe, so the offsets are the document ones the annotation model
     *                  works in and the line is only there to tell apart the marks sharing a row of the ruler
     */
    public void setErrorStripeMarks(String marksJson) {
        if (isUnchanged("errorStripeMarks", marksJson)) {
            return;
        }
        getElement().executeJs("this.$arquillApi.setErrorStripeMarks($0);", marksJson);
    }

    /**
     * The line marker presentations of the document, already resolved to bands. Handed over as a property
     * rather than an argument of the call - a list crosses as itself that way, and the browser is not left
     * parsing a string the server just printed.
     */
    public void setGutterBands(List<GutterBand> bands) {
        if (isUnchanged("gutterBands", List.copyOf(bands))) {
            return;
        }
        getElement().setPropertyList("gutterBands", bands);
        getElement().executeJs("this.$arquillApi.setGutterBands(this.gutterBands);");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // a fresh dom holds none of the decoration channels, whatever this instance sent to the previous one
        myLastPushed.clear();

        // ahead of the loader below, so that a push arriving while the scripts are on their way is collected
        installApiStub();

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
