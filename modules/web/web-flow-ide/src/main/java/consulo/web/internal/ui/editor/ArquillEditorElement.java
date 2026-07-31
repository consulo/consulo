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
     * Fired when a mark of the error stripe is clicked. The offset is the start of the highlighter the mark was
     * built from, in document coordinates.
     */
    @DomEvent("arquill-stripe-click")
    public static class ArquillErrorStripeClickEvent extends ComponentEvent<ArquillEditorElement> {
        private final int myOffset;

        public ArquillErrorStripeClickEvent(ArquillEditorElement source, boolean fromClient, @EventData("event.detail.offset") int offset) {
            super(source, fromClient);
            myOffset = offset;
        }

        public int getOffset() {
            return myOffset;
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
    }

    public void setText(String text) {
        myText = text;

        getElement().executeJs("""
            if (this.$arquillEditor) {
                // orion reports our own edit through the model, the flag keeps it from bouncing back to the server
                this.$arquillSuppressChange = true;
                try {
                    // contentsSaved must stay false, orion skips the text when it is set
                    this.$arquillEditor.setInput(null, null, $0, false, true);
                }
                finally {
                    this.$arquillSuppressChange = false;
                }
            }
            """, text);
    }

    /**
     * Replaces {@code [start, end)} without resending the whole document.
     */
    public void replaceText(int start, int end, String text) {
        applyToCache(start, end, text);

        getElement().executeJs("""
            if (this.$arquillEditor) {
                // orion reports our own edit through the model, the flag keeps it from bouncing back to the server
                this.$arquillSuppressChange = true;
                try {
                    this.$arquillEditor.getModel().setText($2, $0, $1);
                }
                finally {
                    this.$arquillSuppressChange = false;
                }
            }
            """, start, end, text);
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

        getElement().executeJs("""
            if (this.$arquillEditor) {
                this.$arquillEditor.getTextView().setOptions({ readonly: $0 });
            }
            """, readOnly);
    }

    public Registration addTextChangeListener(ComponentEventListener<ArquillTextChangeEvent> listener) {
        return addListener(ArquillTextChangeEvent.class, listener);
    }

    public void setCaretOffset(int offset) {
        // remember the offset first, otherwise orion fires Selection back and the server sees its own move
        getElement().executeJs("""
            this.$arquillCaretOffset = $0;
            if (this.$arquillEditor) {
                this.$arquillEditor.setCaretOffset($0, true);
            }
            """, offset);
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

    public Registration addErrorStripeClickListener(ComponentEventListener<ArquillErrorStripeClickEvent> listener) {
        return addListener(ArquillErrorStripeClickEvent.class, listener);
    }

    /**
     * Turns the pointer into a hand while ctrl/cmd is held over a reference. Only the server knows whether the
     * offset under the pointer resolves to anything, so the client cannot decide this on its own.
     */
    public void setLinkHovered(boolean hovered) {
        getElement().executeJs("this.classList.toggle('arquill-ctrl-hover', $0);", hovered);
    }

    /**
     * @param marksJson array of {id, line, iconUrl, tooltip} - gutter icons produced by the line marker pass
     */
    public void setGutterMarks(String marksJson) {
        getElement().executeJs("""
            this.$arquillGutterMarks = $0;
            if (this.$arquillRenderGutterMarks) {
                this.$arquillRenderGutterMarks();
            }
            """, marksJson);
    }

    /**
     * @param statusJson {items:[{text, iconUrls}], tooltip, analyzing} - the daemon analyze status of the whole
     *                   file, where {@code tooltip} is the html shown while the pointer is over the panel
     */
    public void setAnalyzeStatus(String statusJson) {
        getElement().executeJs("""
            this.$arquillAnalyzeStatus = $0;
            if (this.$arquillRenderAnalyzeStatus) {
                this.$arquillRenderAnalyzeStatus();
            }
            """, statusJson);
    }

    /**
     * @param rangesJson array of {start, end, html} in absolute document offsets - the message of every highlight
     *                   the daemon produced, shown while the pointer rests over the range
     */
    public void setTooltipRanges(String rangesJson) {
        // parsed once here, the lookup runs on every pointer move
        getElement().executeJs("""
            this.$arquillTooltipRanges = JSON.parse($0);
            """, rangesJson);
    }

    /**
     * @param rangesJson array of {start, end, style} in absolute document offsets, sorted by start
     */
    public void setStyleRanges(String rangesJson) {
        // kept parsed - folding has to remap every range into view offsets again on each collapse and expand
        getElement().executeJs("""
            this.$arquillStyleRanges = JSON.parse($0);
            if (this.$arquillPushStyleRanges) {
                this.$arquillPushStyleRanges();
            }
            """, rangesJson);
    }

    /**
     * @param regionsJson array of {start, end, collapsed} in absolute document offsets - the fold regions the
     *                    platform folding model holds
     */
    public void setFoldRegions(String regionsJson) {
        getElement().executeJs("""
            this.$arquillFoldRegions = $0;
            if (this.$arquillRenderFoldRegions) {
                this.$arquillRenderFoldRegions();
            }
            """, regionsJson);
    }

    /**
     * @param marksJson {visible, minMarkHeight, marks:[{offset, line1, line2, layer, thin, color, tooltip}]} - the
     *                  highlighters carrying an error stripe color, in document lines. The mapping onto the strip
     *                  is done in the browser, the line height and the strip height are only known there
     */
    public void setErrorStripeMarks(String marksJson) {
        getElement().executeJs("""
            this.$arquillErrorStripeMarks = $0;
            if (this.$arquillRenderErrorStripe) {
                this.$arquillRenderErrorStripe();
            }
            """, marksJson);
    }

    /**
     * @param bandsJson array of {line1, line2, type, color} - the vcs line status ranges, where {@code line2}
     *                  is exclusive and equal to {@code line1} for a deletion
     */
    public void setChangeBands(String bandsJson) {
        getElement().executeJs("""
            this.$arquillChangeBands = $0;
            if (this.$arquillRenderChangeBands) {
                this.$arquillRenderChangeBands();
            }
            """, bandsJson);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // the bundle measures the parent, so the editor can only be created once the element is in the dom.
        // the script is injected by hand - flow drops @JavaScript pointing at a context absolute path
        getElement().executeJs("""
            const element = this;
            const create = () => {
                if (!element.$arquillEditor) {
                    // the error stripe is a column of its own right of the text rather than an overlay over it,
                    // the way the awt error panel sits next to the editor. the host reserves it here, before the
                    // view exists - orion lays its root out as a plain block of full width, so it measures what
                    // the padding leaves. twice the width of the awt panel, as asked for -
                    // DesktopEditorErrorPanelUI.getPreferredSize is scale(6) + HighlightDisplayLevel icon
                    const STRIPE_WIDTH = 40;
                    element.style.paddingRight = STRIPE_WIDTH + 'px';

                    // the analyze counters are placed against the same edge and have to clear the column, so the
                    // width is published rather than repeated in the stylesheet
                    element.style.setProperty('--arquill-error-stripe-width', STRIPE_WIDTH + 'px');

                    // noComputeSize keeps the editor from shrinking the host down to the text height
                    element.$arquillEditor = window.arquillEditor.createEditor({
                        parent: element,
                        contents: $0,
                        readonly: $1,
                        noComputeSize: true
                    });
                    element.style.height = '100%';

                    // orion hardcodes 8 lines per wheel notch off the legacy wheelDelta, which on a high resolution
                    // wheel lands near 10. overriding _handleMouseWheel does not stick because orion already bound
                    // it, so the event is taken in the capture phase and orion never sees it
                    const textView = element.$arquillEditor.getTextView();
                    const LINES_PER_NOTCH = 3;

                    const BAND_WIDTH = 3;
                    const DELETION_HEIGHT = 4;
                    const FOLDING_ANNOTATION_TYPE = 'orion.annotation.folding';

                    const onWheel = domEvent => {
                        const lineHeight = textView._getLineHeight();
                        const delta = domEvent.deltaY !== undefined ? domEvent.deltaY : -domEvent.wheelDeltaY;

                        domEvent.preventDefault();
                        domEvent.stopImmediatePropagation();

                        textView._scrollView(0, Math.sign(delta) * LINES_PER_NOTCH * lineHeight);
                    };

                    element.addEventListener('wheel', onWheel, { capture: true, passive: false });
                    element.addEventListener('mousewheel', onWheel, { capture: true, passive: false });

                    // the view is laid out over a projection of the base model, so there are two offset
                    // spaces. the base model matches the platform document one to one and is the only one the
                    // server ever sees, while everything the view reports - hit testing, the caret, line
                    // pixels - is in the projected space that folding shortens. the two are the same until
                    // the first region is collapsed, which is why nothing here converted before
                    const baseModel = element.$arquillEditor.getModel();
                    const viewModel = textView.getModel();

                    const toBaseOffset = offset => offset < 0 ? -1 : viewModel.mapOffset(offset);

                    const toViewOffset = offset => offset < 0 ? -1 : viewModel.mapOffset(offset, true);

                    // -1 while the line is hidden inside a collapsed region. the line count itself maps to the
                    // end of the view, so a range ending past the last line still has a bottom
                    const toViewLine = documentLine => {
                        if (documentLine >= baseModel.getLineCount()) {
                            return viewModel.getLineCount();
                        }

                        const offset = toViewOffset(baseModel.getLineStart(documentLine));

                        return offset < 0 ? -1 : viewModel.getLineAtOffset(offset);
                    };

                    // orion places the rulers and the text against the host, but the overlays are absolutely
                    // positioned inside it, so every y has to come back through the page
                    const pageY = viewLine =>
                        textView.convert({ y: textView.getLinePixel(viewLine) }, 'document', 'page').y;

                    // go to declaration is bound to ctrl/cmd click and to middle click, and the underline follows
                    // the pointer while the modifier is held
                    const offsetAt = domEvent => {
                        try {
                            // getOffsetAtLocation wants document coordinates, a raw clientX/clientY is off by the
                            // scroll position and by the view padding
                            const point = textView.convert({ x: domEvent.clientX, y: domEvent.clientY }, 'page', 'document');

                            // getOffsetAtLocation clamps to the closest character, so past the end of a line it
                            // still reports the last one - that would light up the whole right margin
                            if (!textView.isValidTextPosition(point.x, point.y)) {
                                return -1;
                            }

                            return toBaseOffset(textView.getOffsetAtLocation(point.x, point.y));
                        }
                        catch (e) {
                            return -1;
                        }
                    };

                    const fireHover = offset => {
                        if (element.$arquillHoverOffset === offset) {
                            return;
                        }

                        element.$arquillHoverOffset = offset;
                        // the hand cursor is put back on by the server once it knows the offset resolves, but
                        // dropping it has to be immediate - the modifier may already be released
                        if (offset < 0) {
                            element.classList.remove('arquill-ctrl-hover');
                        }
                        element.dispatchEvent(new CustomEvent('arquill-ctrl-hover', { detail: { offset: offset } }));
                    };

                    // the daemon messages are html, the native title attribute would print the markup, so the
                    // tooltip is a floating element of our own. it lives on the body because the editor host is
                    // clipped by orion, a tooltip inside it would be cut off
                    const tooltip = document.createElement('div');
                    tooltip.className = 'arquill-tooltip';
                    tooltip.style.display = 'none';
                    document.body.appendChild(tooltip);
                    element.$arquillTooltipElement = tooltip;

                    let tooltipTimer = 0;

                    const hideTooltip = () => {
                        clearTimeout(tooltipTimer);
                        tooltip.style.display = 'none';
                    };

                    element.$arquillShowTooltip = (html, x, y) => {
                        clearTimeout(tooltipTimer);
                        tooltipTimer = setTimeout(() => {
                            tooltip.innerHTML = html;
                            tooltip.style.display = 'block';
                            // placed only after it is laid out, its size is unknown while it is hidden
                            const box = tooltip.getBoundingClientRect();
                            const left = Math.min(x, window.innerWidth - box.width - 4);
                            const below = y + 20;
                            tooltip.style.left = Math.max(0, left) + 'px';
                            tooltip.style.top = (below + box.height > window.innerHeight ? y - box.height - 4 : below) + 'px';
                        }, 300);
                    };

                    element.$arquillHideTooltip = hideTooltip;

                    const tooltipAt = offset => {
                        if (offset < 0 || !element.$arquillTooltipRanges) {
                            return null;
                        }

                        // ranges nest - an error inside an annotated declaration - and the innermost one is the
                        // message the pointer is actually over
                        let found = null;
                        for (const range of element.$arquillTooltipRanges) {
                            if (offset >= range.start && offset < range.end
                                && (found === null || range.end - range.start < found.end - found.start)) {
                                found = range;
                            }
                        }

                        return found === null ? null : found.html;
                    };

                    element.addEventListener('mousemove', domEvent => {
                        const offset = offsetAt(domEvent);

                        fireHover(domEvent.ctrlKey || domEvent.metaKey ? offset : -1);

                        const html = tooltipAt(offset);
                        if (html) {
                            element.$arquillShowTooltip(html, domEvent.clientX, domEvent.clientY);
                        }
                        else {
                            hideTooltip();
                        }
                    });

                    element.addEventListener('mouseleave', () => {
                        fireHover(-1);
                        hideTooltip();
                    });

                    const onKeyUp = domEvent => {
                        if (domEvent.key === 'Control' || domEvent.key === 'Meta') {
                            fireHover(-1);
                        }
                    };

                    // the modifier can be released while the focus is elsewhere, so the listener is global and
                    // has to be dropped by hand on detach
                    document.addEventListener('keyup', onKeyUp);
                    element.$arquillKeyUpListener = onKeyUp;

                    element.addEventListener('mousedown', domEvent => {
                        const wanted = domEvent.button === 1 || (domEvent.button === 0 && (domEvent.ctrlKey || domEvent.metaKey));
                        if (!wanted) {
                            return;
                        }

                        const offset = offsetAt(domEvent);
                        if (offset < 0) {
                            return;
                        }

                        domEvent.preventDefault();
                        fireHover(-1);
                        element.dispatchEvent(new CustomEvent('arquill-ctrl-click', { detail: { offset: offset } }));
                    });

                    // the awt editor moves the caret to the clicked position before showing its menu, and the group
                    // is expanded against the caret - without this every item answers for wherever the caret was
                    // left. the popup itself is opened by the context menu, which takes the event in the capture
                    // phase on this same element, so this listener has to be a capture one too - orion swallows
                    // the event on the inner text divs on the way down
                    element.addEventListener('contextmenu', domEvent => {
                        hideTooltip();

                        const offset = offsetAt(domEvent);
                        if (offset >= 0 && offset !== element.$arquillCaretOffset) {
                            element.$arquillCaretOffset = offset;
                            element.dispatchEvent(new CustomEvent('arquill-caret', { detail: { offset: offset } }));
                        }
                    }, { capture: true });

                    baseModel.addEventListener('Changed', event => {
                        if (element.$arquillSuppressChange) {
                            return;
                        }

                        const added = event.addedCharCount > 0
                            ? baseModel.getText(event.start, event.start + event.addedCharCount)
                            : '';

                        element.dispatchEvent(new CustomEvent('arquill-change', {
                            detail: {
                                start: event.start,
                                removedCharCount: event.removedCharCount,
                                text: added
                            }
                        }));
                    });

                    textView.addEventListener('Selection', event => {
                        const offset = event.newValue ? toBaseOffset(event.newValue.start) : -1;
                        if (offset < 0 || offset === element.$arquillCaretOffset) {
                            return;
                        }

                        element.$arquillCaretOffset = offset;
                        element.dispatchEvent(new CustomEvent('arquill-caret', { detail: { offset: offset } }));
                    });

                    // the bundle hands the ranges straight to the LineStyle event, which reports view offsets,
                    // so what the server pushed in document offsets has to be mapped first
                    element.$arquillPushStyleRanges = () => {
                        const ranges = element.$arquillStyleRanges;
                        if (!ranges) {
                            return;
                        }

                        const mapped = [];
                        for (const range of ranges) {
                            const start = toViewOffset(range.start);
                            const end = toViewOffset(range.end);

                            // hidden inside a collapsed region, there is nothing left to style
                            if (start < 0 || end < 0) {
                                continue;
                            }

                            mapped.push({ start: start, end: end, style: range.style });
                        }

                        element.$arquillEditor.setStyleRanges(mapped);
                    };

                    // the gutter icons are drawn as an overlay column tracking the ruler, orion has no api to put
                    // an arbitrary image into its annotation ruler
                    const gutter = document.createElement('div');
                    gutter.className = 'arquill-gutter';
                    element.appendChild(gutter);

                    element.$arquillRenderGutterMarks = () => {
                        gutter.textContent = '';

                        const marks = element.$arquillGutterMarks ? JSON.parse(element.$arquillGutterMarks) : [];
                        if (!marks.length) {
                            return;
                        }

                        // orion keeps the annotation ruler left of the line numbers - it is added at index 0 among
                        // the left rulers - and that is the only free column, anchoring the overlay anywhere else
                        // draws the icons over the digits
                        const annotationRuler = element.querySelector('.textviewLeftRuler .ruler.annotations');
                        if (!annotationRuler) {
                            return;
                        }

                        const hostRect = element.getBoundingClientRect();
                        const rulerRect = annotationRuler.getBoundingClientRect();

                        gutter.style.left = (rulerRect.left - hostRect.left) + 'px';
                        gutter.style.width = rulerRect.width + 'px';

                        const lineHeight = textView.getLineHeight();
                        const size = Math.min(16, lineHeight);

                        // several markers can sit on one line - implemented interfaces and overridden methods both
                        // land on a declaration - and they have to stand next to each other, not on top
                        const perLine = {};

                        for (const mark of marks) {
                            if (mark.line < 0 || mark.line >= baseModel.getLineCount()) {
                                continue;
                            }

                            // the mark carries a document line, the view is laid out over the projected model
                            const viewLine = toViewLine(mark.line);
                            if (viewLine < 0) {
                                continue;
                            }

                            const column = perLine[mark.line] || 0;
                            perLine[mark.line] = column + 1;

                            // asking orion where the line actually is beats computing line * lineHeight - it stays
                            // correct while scrolling and while the view padding changes
                            const top = pageY(viewLine) - hostRect.top;

                            // a mark outside the visible area would otherwise pile up at the clipping edge
                            if (top < 0 || top > element.clientHeight) {
                                continue;
                            }

                            // the platform layers markers out of several images, the awt gutter paints them onto
                            // one graphics - here they are stacked inside one box instead
                            const icon = document.createElement('span');
                            icon.className = 'arquill-gutter-mark';

                            for (const url of mark.iconUrls || []) {
                                const layer = document.createElement('img');
                                layer.src = url;
                                icon.appendChild(layer);
                            }

                            if (mark.tooltip) {
                                // the host listener would hide the tooltip again on the way up, the pointer is not
                                // over any text range while it is over the icon
                                icon.addEventListener('mousemove', domEvent => {
                                    domEvent.stopPropagation();
                                    element.$arquillShowTooltip(mark.tooltip, domEvent.clientX, domEvent.clientY);
                                });
                                icon.addEventListener('mouseleave', () => element.$arquillHideTooltip());
                            }
                            icon.style.width = size + 'px';
                            icon.style.height = size + 'px';
                            icon.style.top = (top + (lineHeight - size) / 2) + 'px';
                            icon.style.left = (column * size) + 'px';
                            icon.addEventListener('click', () => {
                                element.$arquillHideTooltip();
                                element.dispatchEvent(new CustomEvent('arquill-gutter-click', { detail: { id: mark.id } }));
                            });
                            gutter.appendChild(icon);
                        }
                    };

                    // the vcs change bars, a strip of its own - the awt gutter paints them in the free painters
                    // area right of the icons, so they must not land in the marker column
                    const changeBands = document.createElement('div');
                    changeBands.className = 'arquill-change-bands';
                    element.appendChild(changeBands);

                    element.$arquillRenderChangeBands = () => {
                        changeBands.textContent = '';

                        const bands = element.$arquillChangeBands ? JSON.parse(element.$arquillChangeBands) : [];
                        if (!bands.length) {
                            return;
                        }

                        // the folding ruler is added last among the left rulers, so its right edge is where the
                        // gutter ends and the text begins - the same place the awt bars sit
                        const foldingRuler = element.querySelector('.textviewLeftRuler .ruler.folding');
                        if (!foldingRuler) {
                            return;
                        }

                        const hostRect = element.getBoundingClientRect();
                        const rulerRect = foldingRuler.getBoundingClientRect();

                        changeBands.style.left = (rulerRect.right - hostRect.left - BAND_WIDTH - 1) + 'px';
                        changeBands.style.width = BAND_WIDTH + 'px';

                        for (const band of bands) {
                            const deleted = band.line1 === band.line2;

                            const startLine = toViewLine(band.line1);
                            if (startLine < 0) {
                                continue;
                            }

                            const top = pageY(startLine) - hostRect.top;

                            let height;
                            if (deleted) {
                                // a deletion has no lines left to cover, the awt gutter marks the boundary
                                // between the two surviving lines instead
                                height = DELETION_HEIGHT;
                            }
                            else {
                                const endLine = toViewLine(band.line2);
                                // the whole range collapsed into a single fold, nothing of it is on screen
                                if (endLine < 0) {
                                    continue;
                                }
                                height = pageY(endLine) - hostRect.top - top;
                            }

                            if (height <= 0 || top + height < 0 || top > element.clientHeight) {
                                continue;
                            }

                            const bar = document.createElement('div');
                            bar.className = 'arquill-change-band';
                            bar.style.top = (deleted ? top - DELETION_HEIGHT / 2 : top) + 'px';
                            bar.style.height = height + 'px';
                            bar.style.backgroundColor = band.color;
                            changeBands.appendChild(bar);
                        }
                    };

                    // folding is driven through orion's own machinery: a FoldingAnnotation over the base model
                    // owns the projection that hides the lines, and the folding ruler already draws and toggles
                    // it. hand rolled projections would have to reimplement both
                    const annotationModel = element.$arquillEditor.getAnnotationModel();
                    const foldAnnotations = new Map();

                    element.$arquillRenderFoldRegions = () => {
                        if (!annotationModel) {
                            return;
                        }

                        const wanted = new Map();
                        for (const region of element.$arquillFoldRegions ? JSON.parse(element.$arquillFoldRegions) : []) {
                            wanted.set(region.start + ':' + region.end, region);
                        }

                        // the annotations are reconciled rather than rebuilt - recreating them would drop and
                        // readd every projection, and the view would jump on each round trip
                        element.$arquillSuppressFold = true;
                        textView.setRedraw(false);
                        try {
                            for (const [key, annotation] of Array.from(foldAnnotations)) {
                                if (wanted.has(key)) {
                                    continue;
                                }

                                // the annotation model does not touch the projection model, so a collapsed
                                // annotation has to give its projection back before it is dropped
                                if (!annotation.expanded) {
                                    annotation.expand();
                                }

                                annotationModel.removeAnnotation(annotation);
                                foldAnnotations.delete(key);
                            }

                            for (const [key, region] of wanted) {
                                let annotation = foldAnnotations.get(key);
                                if (!annotation) {
                                    annotation = element.$arquillEditor.addFoldingAnnotation(region.start, region.end);
                                    if (!annotation) {
                                        continue;
                                    }
                                    foldAnnotations.set(key, annotation);
                                }

                                if (region.collapsed === annotation.expanded) {
                                    if (region.collapsed) {
                                        annotation.collapse();
                                    }
                                    else {
                                        annotation.expand();
                                    }
                                }
                            }
                        }
                        finally {
                            textView.setRedraw(true);
                            element.$arquillSuppressFold = false;
                        }

                        element.$arquillOnProjectionChanged();
                    };

                    // collapsing shifts every view offset after the region, so everything that was pushed in
                    // document offsets has to be placed again
                    element.$arquillOnProjectionChanged = () => {
                        element.$arquillPushStyleRanges();
                        element.$arquillRenderGutterMarks();
                        element.$arquillRenderChangeBands();

                        // the strip is built after this, and the first fold pass already runs through here
                        if (element.$arquillRenderErrorStripe) {
                            element.$arquillRenderErrorStripe();
                        }
                    };

                    if (annotationModel) {
                        annotationModel.addEventListener('Changed', event => {
                            // a text edit shifts every annotation and reports all of them as changed, a real
                            // collapse or expand goes through modifyAnnotation with no document event attached
                            if (element.$arquillSuppressFold || event.textModelChangedEvent) {
                                return;
                            }

                            let folded = false;
                            for (const annotation of event.changed || []) {
                                if (annotation.type !== FOLDING_ANNOTATION_TYPE) {
                                    continue;
                                }

                                folded = true;
                                element.dispatchEvent(new CustomEvent('arquill-fold', {
                                    detail: { start: annotation.start, collapsed: !annotation.expanded }
                                }));
                            }

                            // the annotation tells the model it changed before it adds or drops its
                            // projection, so the view offsets are still the old ones right here
                            if (folded) {
                                Promise.resolve().then(() => element.$arquillOnProjectionChanged());
                            }
                        });
                    }

                    const onViewChanged = () => {
                        element.$arquillRenderGutterMarks();
                        element.$arquillRenderChangeBands();
                    };

                    textView.addEventListener('Scroll', onViewChanged);
                    textView.addEventListener('Resize', onViewChanged);

                    element.$arquillRenderFoldRegions();

                    element.$arquillOnProjectionChanged();

                    // the analyze status of the whole file, the counterpart of the awt status panel that sits over
                    // the top right corner of the editor
                    const status = document.createElement('div');
                    status.className = 'arquill-status';
                    element.appendChild(status);

                    // the panel floats over the text, and without stopping the pointer here the editor wide
                    // handlers answer for the line hidden behind it - its tooltip, its underline, its offset
                    status.addEventListener('mousemove', domEvent => {
                        domEvent.stopPropagation();
                        fireHover(-1);

                        if (element.$arquillStatusTooltip) {
                            element.$arquillShowTooltip(element.$arquillStatusTooltip, domEvent.clientX, domEvent.clientY);
                        }
                        else {
                            hideTooltip();
                        }
                    });

                    status.addEventListener('mouseleave', hideTooltip);

                    status.addEventListener('mousedown', domEvent => domEvent.stopPropagation());

                    element.$arquillRenderAnalyzeStatus = () => {
                        status.textContent = '';

                        const model = element.$arquillAnalyzeStatus ? JSON.parse(element.$arquillAnalyzeStatus) : {};
                        const items = model.items || [];

                        status.style.display = items.length ? 'flex' : 'none';
                        // the awt panel dims the whole widget while the daemon is still running, so a stale count
                        // does not read as the final one
                        status.classList.toggle('arquill-status-analyzing', !!model.analyzing);

                        for (const item of items) {
                            const cell = document.createElement('span');
                            cell.className = 'arquill-status-item';

                            if (item.iconUrls && item.iconUrls.length) {
                                const icon = document.createElement('span');
                                icon.className = 'arquill-status-icon';

                                for (const url of item.iconUrls) {
                                    const layer = document.createElement('img');
                                    layer.src = url;
                                    icon.appendChild(layer);
                                }

                                cell.appendChild(icon);
                            }

                            cell.appendChild(document.createTextNode(item.text || ''));
                            status.appendChild(cell);
                        }

                        element.$arquillStatusTooltip = model.tooltip || '';

                        // the strip keeps its top free for the widget, so it has to be measured again once the
                        // widget itself changed - a clean file shows one icon, a broken one three counters
                        if (element.$arquillRenderErrorStripe) {
                            element.$arquillRenderErrorStripe();
                        }
                    };

                    element.$arquillRenderAnalyzeStatus();

                    // the error stripe, one mark per highlighter over the whole document - what the awt
                    // DesktopEditorErrorPanel paints. it sits in the column the host padding reserved, so it
                    // covers neither the text nor the orion scrollbar
                    const stripe = document.createElement('div');
                    stripe.className = 'arquill-error-stripe';
                    stripe.style.width = STRIPE_WIDTH + 'px';
                    element.appendChild(stripe);

                    element.$arquillRenderErrorStripe = () => {
                        stripe.textContent = '';

                        const model = element.$arquillErrorStripeMarks
                            ? JSON.parse(element.$arquillErrorStripeMarks)
                            : {};

                        stripe.style.display = model.visible === false ? 'none' : 'block';

                        const marks = model.marks || [];
                        if (!marks.length) {
                            return;
                        }

                        // the traffic light of the awt panel is the status widget here, and it floats over the
                        // top of the column - the same space getIconPanelSize() keeps free
                        const stripeTop = status.offsetHeight ? status.offsetHeight + 4 : 2;

                        // the view is laid out over the projection, so a collapsed region shortens the document
                        // the strip stands for, exactly like the awt panel measuring in visual lines
                        const lineCount = viewModel.getLineCount();
                        const lineHeight = textView.getLineHeight();
                        const sourceHeight = lineCount * lineHeight;
                        const targetHeight = stripe.clientHeight - stripeTop;

                        if (lineCount <= 0 || targetHeight <= 0) {
                            return;
                        }

                        // the whole document is squeezed into the column - unless it is short enough to fit as
                        // it is, then the strip follows the text line for line
                        const lineToY = viewLine => stripeTop + Math.round(sourceHeight < targetHeight
                            ? viewLine * lineHeight
                            : viewLine / lineCount * targetHeight);

                        const minHeight = model.minMarkHeight || 2;
                        // measured rather than taken from the constant, the border of the column is part of it
                        const width = stripe.clientWidth || STRIPE_WIDTH;
                        const thinWidth = Math.max(2, Math.round(width / 6));

                        // the awt panel keeps one stripe per layer and lets the topmost one hide the ones below,
                        // here the marks are drawn in that order instead. everything landing on the same pixel
                        // row shares a single element - a squeezed document puts hundreds of them on one y, and
                        // their messages have to end up in one tooltip rather than on top of each other
                        const slots = new Map();

                        for (const mark of marks.slice().sort((left, right) => left.layer - right.layer)) {
                            const startLine = toViewLine(mark.line1);
                            // hidden inside a collapsed region
                            if (startLine < 0) {
                                continue;
                            }

                            const y = lineToY(startLine);

                            let height = 0;
                            if (mark.line2 > mark.line1) {
                                const endLine = toViewLine(mark.line2);
                                if (endLine >= 0) {
                                    height = lineToY(endLine) - y;
                                }
                            }

                            height = Math.max(minHeight, height);

                            const key = (mark.thin ? 't' : 'w') + ':' + y + ':' + height;

                            let slot = slots.get(key);
                            if (!slot) {
                                slot = { y: y, height: height, thin: mark.thin, tooltips: [] };
                                slots.set(key, slot);
                            }

                            slot.color = mark.color;
                            slot.offset = mark.offset;

                            if (mark.tooltip && slot.tooltips.indexOf(mark.tooltip) < 0) {
                                slot.tooltips.push(mark.tooltip);
                            }
                        }

                        for (const slot of slots.values()) {
                            const bar = document.createElement('div');
                            bar.className = 'arquill-error-stripe-mark';
                            // a thin mark gets a narrow column of its own, so it stays visible next to the wide
                            // ones covering the same lines
                            bar.style.left = (slot.thin ? 0 : thinWidth) + 'px';
                            bar.style.width = (slot.thin ? thinWidth : width - thinWidth) + 'px';
                            bar.style.top = slot.y + 'px';
                            bar.style.height = slot.height + 'px';
                            bar.style.backgroundColor = slot.color;

                            if (slot.tooltips.length) {
                                const html = slot.tooltips.map(text => '<div>' + text + '</div>').join('');

                                // the host listener would hide the tooltip again on the way up, the pointer is
                                // over no text range while it is over the strip
                                bar.addEventListener('mousemove', domEvent => {
                                    domEvent.stopPropagation();
                                    element.$arquillShowTooltip(html, domEvent.clientX, domEvent.clientY);
                                });
                                bar.addEventListener('mouseleave', () => element.$arquillHideTooltip());
                            }

                            bar.addEventListener('click', () => {
                                element.$arquillHideTooltip();
                                element.dispatchEvent(new CustomEvent('arquill-stripe-click', {
                                    detail: { offset: slot.offset }
                                }));
                            });

                            stripe.appendChild(bar);
                        }
                    };

                    // scrolling leaves the strip alone, it stands for the whole document - only its own height
                    // changes under it
                    textView.addEventListener('Resize', () => element.$arquillRenderErrorStripe());

                    element.$arquillRenderErrorStripe();
                }
            };

            if (window.arquillEditor) {
                create();
                return;
            }

            let script = document.getElementById('arquill-editor-script');
            if (!script) {
                script = document.createElement('script');
                script.id = 'arquill-editor-script';
                script.src = '/arquill/arquillEditor.js';
                document.head.appendChild(script);
            }

            script.addEventListener('load', create);
            """, myText, myReadOnly);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        getElement().executeJs("""
            // the tooltip lives on the body and the modifier listener on the document, neither goes away with
            // the element
            if (this.$arquillTooltipElement) {
                this.$arquillTooltipElement.remove();
                this.$arquillTooltipElement = null;
            }

            if (this.$arquillKeyUpListener) {
                document.removeEventListener('keyup', this.$arquillKeyUpListener);
                this.$arquillKeyUpListener = null;
            }

            if (this.$arquillEditor) {
                this.$arquillEditor.uninstall();
                this.$arquillEditor = null;
            }
            """);

        super.onDetach(detachEvent);
    }
}
