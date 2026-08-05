/**
 * The browser half of consulo.web.internal.ui.editor.ArquillEditorElement, built over the
 * window.arquillEditor.createEditor(options) the compiled arquill bundle exposes.
 *
 * install(element, contents, readonly) hangs $arquillApi onto the host element - the only thing the server ever
 * reaches for - and dispatches the custom events the @DomEvent classes bind to by name.
 */
(function () {
    'use strict';

    // orion hardcodes 8 lines per wheel notch off the legacy wheelDelta, which on a high resolution
    // wheel lands near 10. overriding _handleMouseWheel does not stick because orion already bound
    // it, so the event is taken in the capture phase and orion never sees it
    const LINES_PER_NOTCH = 3;

    const BAND_WIDTH = 3;
    const DELETION_HEIGHT = 4;
    const FOLDING_ANNOTATION_TYPE = 'orion.annotation.folding';
    const STRIPE_ANNOTATION_TYPE = 'arquill.annotation.errorStripe';

    // the overview ruler paints a mark 3px per line its annotation spans, and Ruler._mergeStyle
    // keeps its own height over the one an overviewStyle asks for - a range covering the file
    // would grow into a block over the whole ruler
    const MAX_MARK_LINES = 5;

    // what the marker column keeps to while nothing needs more - orion ships its annotation ruler at 16px,
    // which is not even one icon of a normal editor font
    const MIN_MARK_COLUMN_WIDTH = 32;

    // breathing room on each side of a marker, so two of them on one line do not read as a single wide glyph
    const MARK_GAP = 2;

    const install = (element, contents, readonly) => {
        if (element.$arquillEditor) {
            return;
        }

        // noComputeSize keeps the editor from shrinking the host down to the text height
        element.$arquillEditor = window.arquillEditor.createEditor({
            parent: element,
            contents: contents,
            readonly: readonly,
            noComputeSize: true
        });
        element.style.height = '100%';

        const textView = element.$arquillEditor.getTextView();

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

        // where the caret is on screen, measured against the host rather than the viewport - what anchors a popup
        // to the caret is a consulo.ui component, and the platform places one inside another, not on the page.
        // carried by the caret event because the server has to know it before it opens anything, and asking only
        // then would cost a round trip the popup would be waiting on
        const caretDetail = (baseOffset, viewOffset) => {
            // a caret with nothing selected is a range of no width over it, so every report carries a selection and
            // the ones that have none say so rather than leaving the field out - an absent field reaches the server
            // as a zero, which reads as a selection from the top of the document
            const detail = {
                offset: baseOffset,
                selectionStart: baseOffset,
                selectionEnd: baseOffset,
                caretX: 0,
                caretY: 0,
                caretHeight: 0,
                textY: 0,
                rectOnly: false
            };

            try {
                const location = textView.getLocationAtOffset(viewOffset);
                // the y of the line rather than the y the location reports: inside text that one is the top of the
                // glyph run, which sits below the top of the row by the leading - a caret drawn there hangs low,
                // and only on an empty line, where there is no run to measure, did the two agree
                const lineTop = textView.getLinePixel(viewModel.getLineAtOffset(viewOffset));
                // orion's page space is the viewport one - offsetAt turns a raw clientX/clientY straight into it -
                // so a host rect, which is viewport based as well, is the right thing to measure these against
                const point = textView.convert({ x: location.x, y: lineTop }, 'document', 'page');
                const host = element.getBoundingClientRect();

                detail.caretX = Math.round(point.x - host.left);
                detail.caretY = Math.round(point.y - host.top);
                detail.caretHeight = Math.round(textView.getLineHeight());

                // the top of the glyph box rather than of the row - what an inline background is drawn from, so
                // it is where a caret standing next to the selection has to start
                const textPoint = textView.convert({ x: location.x, y: location.y }, 'document', 'page');
                detail.textY = Math.round(textPoint.y - host.top);
            }
            catch (e) {
                // a caret inside a collapsed region has nowhere on screen to be, and the offset still matters
            }

            return detail;
        };

        /*
         * The caret of the platform, drawn rather than left to the browser - it has a width, a colour and a blink
         * period, and a contenteditable caret answers to none of them.
         */
        // the height an inline background covers - what the selection and the brace highlight are drawn at, and so
        // what the caret has to match. measured, not derived from the font size: only the browser knows the box a
        // face is laid out in
        let textBoxHeight = 0;
        let lineSpacing = 1;

        const caretElement = document.createElement('div');
        caretElement.className = 'arquill-caret arquill-caret-blinking arquill-caret-hidden';
        element.appendChild(caretElement);

        const placeCaret = () => {
            const selection = textView.getSelection();
            if (!selection || selection.start !== selection.end) {
                // a caret is where there is nothing selected, and a selection draws itself
                caretElement.classList.add('arquill-caret-hidden');
                return;
            }

            const detail = caretDetail(toBaseOffset(selection.start), selection.start);
            if (detail.caretHeight <= 0) {
                caretElement.classList.add('arquill-caret-hidden');
                return;
            }

            // the same box the selection is: an inline background covers the glyph box, and it starts where the
            // glyphs do rather than where the row does. deriving the one from the other left the two a couple of
            // pixels apart, which is the leading the row carries and the background does not
            const height = textBoxHeight > 0 ? Math.min(detail.caretHeight, textBoxHeight) : detail.caretHeight;

            caretElement.style.left = detail.caretX + 'px';
            caretElement.style.top = (detail.textY > 0 ? detail.textY : detail.caretY) + 'px';
            caretElement.style.height = height + 'px';
            caretElement.classList.remove('arquill-caret-hidden');

            // restarting the animation puts the caret back on solid, so it does not vanish while it is moved
            caretElement.style.animation = 'none';
            void caretElement.offsetWidth;
            caretElement.style.animation = '';
        };

        /*
         * The size of a character cell, which only the browser can measure. The platform maps a position to a point
         * and back again - remembering a column as an x while the caret moves down a short line and onto a long one
         * - and an editor which cannot answer stops the arrow keys dead.
         *
         * A span in the same font rather than orion's own metrics: what it caches is private, and a run of glyphs
         * divided by its length is exact for the monospace face an editor is drawn with.
         */
        const reportMetrics = () => {
            try {
                const content = element.querySelector('.textviewContent') || element;
                const style = window.getComputedStyle(content);

                // line-height normal, so the box measured is the one the face is laid out in rather than the one
                // the row was already set to - everything else is derived from this, and deriving it from a guess
                // at the font size is what left the caret, the selection and the glyphs disagreeing
                const ruler = document.createElement('span');
                ruler.style.cssText = 'position:absolute;visibility:hidden;white-space:pre;line-height:normal;';
                ruler.style.font = style.font || (style.fontSize + ' ' + style.fontFamily);
                ruler.textContent = 'MMMMMMMMMM';

                content.appendChild(ruler);
                const rulerBox = ruler.getBoundingClientRect();
                const charWidth = rulerBox.width / 10;
                textBoxHeight = Math.round(rulerBox.height);
                ruler.remove();

                // the row is the box of the face plus the leading the scheme asks for, so the glyphs sit in the
                // middle of it the way an inline box always does
                if (textBoxHeight > 0) {
                    element.style.setProperty('--arquill-editor-line-height',
                        Math.round(textBoxHeight * (lineSpacing > 0 ? lineSpacing : 1)) + 'px');
                }

                const lineHeight = textView.getLineHeight();
                if (charWidth > 0 && lineHeight > 0) {
                    element.dispatchEvent(new CustomEvent('arquill-metrics', {
                        detail: { charWidth: Math.max(1, Math.round(charWidth)), lineHeight: Math.round(lineHeight) }
                    }));
                }
            }
            catch (e) {
                // the platform keeps whatever it had - a wrong cell is better than an editor which cannot move
            }
        };

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

        let hoverOffset;

        const fireHover = offset => {
            if (hoverOffset === offset) {
                return;
            }

            hoverOffset = offset;
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

        let tooltipTimer = 0;

        const hideTooltip = () => {
            clearTimeout(tooltipTimer);
            tooltip.style.display = 'none';
        };

        const showTooltip = (html, x, y) => {
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

            const modifier = domEvent.ctrlKey || domEvent.metaKey;

            // the class only says the modifier is down - the stylesheet leaves it to :hover to pick out the run
            // actually aimed at, which the pointer knows better than an offset does
            element.classList.toggle('arquill-inlay-hover', modifier);

            // a run of a hint the server already said reaches somewhere needs nothing asked about it, unlike an
            // offset in the code which it has to resolve first
            const overInlayAction = modifier && inlayActionAt(domEvent) >= 0;

            fireHover(modifier && !overInlayAction ? offset : -1);

            const html = tooltipAt(offset);
            if (html) {
                showTooltip(html, domEvent.clientX, domEvent.clientY);
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
                // the pointer may not move again, so the link look of the hints has to be dropped from here too
                element.classList.remove('arquill-inlay-hover');

                fireHover(-1);
            }
        };

        // the modifier can be released while the focus is elsewhere, so the listener is global and
        // has to be dropped by hand on detach
        document.addEventListener('keyup', onKeyUp);

        // the run of an inlay a pointer is over, if it is one which reaches an action
        const inlayActionAt = domEvent => {
            const span = domEvent.target instanceof Element
                ? domEvent.target.closest('[data-arquill-inlay-click]')
                : null;

            return span && element.contains(span) ? Number(span.getAttribute('data-arquill-inlay-click')) : -1;
        };

        element.addEventListener('mousedown', domEvent => {
            const wanted = domEvent.button === 1 || (domEvent.button === 0 && (domEvent.ctrlKey || domEvent.metaKey));
            if (!wanted) {
                return;
            }

            // an inlay stands in no document offset, so it is answered for by what it is rather than by where -
            // and it has to be taken before the offset path below, which would resolve to the code beside it
            const inlayClick = inlayActionAt(domEvent);
            if (inlayClick >= 0) {
                domEvent.preventDefault();
                fireHover(-1);
                element.dispatchEvent(new CustomEvent('arquill-inlay-click', {
                    detail: { id: inlayClick, controlDown: domEvent.ctrlKey || domEvent.metaKey }
                }));
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
                element.dispatchEvent(new CustomEvent('arquill-caret', { detail: caretDetail(offset, toViewOffset(offset)) }));
            }
        }, { capture: true });

        // a typed character is handed to the platform instead of going straight into the model. typing in an
        // editor runs the typed action chain - the completion lookup grows its prefix from it, a brace or a quote
        // closes itself, a tag mirrors into the one that closes it - and none of that happens when the editor puts
        // the character in on its own. what comes back is a document change like any other
        //
        // keypress rather than beforeinput: orion drives its own edits off the key events and writes into the model
        // itself, so the browser never raises an input event for a character typed here and there is nothing to
        // cancel. taking the keypress first is what stops orion from inserting
        let composing = false;
        element.addEventListener('compositionstart', () => composing = true);
        element.addEventListener('compositionend', () => composing = false);

        element.addEventListener('keypress', domEvent => {
            // a modifier makes it a shortcut, and the keymap has already had its say by here
            if (composing || domEvent.ctrlKey || domEvent.metaKey || domEvent.altKey) {
                return;
            }

            // one printable character. return, tab and the rest name themselves and are editor actions, not typing
            const typed = domEvent.key;
            if (typeof typed !== 'string' || typed.length !== 1) {
                return;
            }

            const selection = textView.getSelection();
            // replacing a selection is not typing, and the platform has no single character for it
            if (!selection || selection.start !== selection.end) {
                return;
            }

            domEvent.preventDefault();
            domEvent.stopImmediatePropagation();

            // only the character. the caret is the server's - this one does not move, because the keystroke stops
            // here, and sending it would put every character of a run back at the same place
            element.dispatchEvent(new CustomEvent('arquill-typed', { detail: { text: typed } }));
        }, { capture: true });

        baseModel.addEventListener('Changed', event => {
            // any edit moves the pixels an offset lands on, so the rect the server holds is stale either way -
            // the next caret push has to measure and report again, suppressed edits included
            element.$arquillLastRectOffset = -1;

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
            // orion moves its own caret while the platform's text is written in, and the move it reports is a
            // consequence of that edit rather than the user going anywhere. taking it moved the platform caret to
            // wherever the replaced range left orion - the front of the document after a template was inserted -
            // and the next character typed went in there
            if (element.$arquillSuppressChange) {
                return;
            }

            if (!event.newValue) {
                return;
            }

            // the whole range, not just where it starts. a drag holds its start still and moves only its end, so a
            // report of the start alone said nothing had happened and the platform kept a selection of its own that
            // had never matched what was on screen - which is what delete then cut against
            const viewStart = Math.min(event.newValue.start, event.newValue.end);
            const viewEnd = Math.max(event.newValue.start, event.newValue.end);
            const viewCaret = textView.getCaretOffset();

            const start = toBaseOffset(viewStart);
            const end = toBaseOffset(viewEnd);
            const offset = toBaseOffset(viewCaret);
            if (start < 0 || end < 0 || offset < 0) {
                // a caret standing inside projected text answers for no document offset, so it is moved off it
                // and the move that follows is what the server hears about
                if (viewStart === viewEnd) {
                    snapCaretOutOfProjection(viewCaret);
                }
                return;
            }

            // the edge a later snap decides its direction from
            element.$arquillLastViewCaret = viewCaret;

            if (offset === element.$arquillCaretOffset
                && start === element.$arquillSelectionStart
                && end === element.$arquillSelectionEnd) {
                return;
            }

            element.$arquillCaretOffset = offset;
            element.$arquillSelectionStart = start;
            element.$arquillSelectionEnd = end;
            // the event just carried the rect of this offset, so the echo the server answers with has
            // nothing left to report
            element.$arquillLastRectOffset = offset;
            element.dispatchEvent(new CustomEvent('arquill-caret', {
                detail: Object.assign(caretDetail(offset, viewCaret), { selectionStart: start, selectionEnd: end })
            }));
            placeCaret();
        });

        // the fold placeholders are not in the document, so their ranges are built by the folding code
        // further down rather than pushed by the server
        let foldPlaceholderStyles = null;

        // the inlays are not in the document either, and their ranges are built the same way further down
        let inlayStyles = null;

        // the bundle hands the ranges straight to the LineStyle event, which reports view offsets,
        // so what the server pushed in document offsets has to be mapped first
        // the end of a range is exclusive, so it may sit exactly on the start of a collapsed region -
        // an offset inside a projection maps to nothing, and the tag name of a folded <build...> would
        // lose its colour along with the text the fold hides. the last character it covers still maps
        // taken from the last character the range covers rather than from the end itself. mapping the end lands
        // past everything projected at that offset, so a range ending where an inlay is anchored would reach over
        // the inlay - the highlight of an identifier would colour the hint that follows it, and the two ranges
        // would overlap, which the bundle does not define an answer for
        const toViewRangeEnd = offset => {
            const last = toViewOffset(offset - 1);
            if (last >= 0) {
                return last + 1;
            }

            return toViewOffset(offset);
        };

        const pushStyleRanges = () => {
            const mapped = [];

            for (const range of element.$arquillStyleRanges || []) {
                const start = toViewOffset(range.start);
                const end = toViewRangeEnd(range.end);

                // hidden inside a collapsed region, there is nothing left to style
                if (start < 0 || end < 0) {
                    continue;
                }

                mapped.push({ start: start, end: end, style: range.style });
            }

            const projected = (foldPlaceholderStyles ? foldPlaceholderStyles() : [])
                .concat(inlayStyles ? inlayStyles() : []);
            if (projected.length) {
                for (const style of projected) {
                    mapped.push(style);
                }

                // the bundle binary searches the array for the first range of a line, which only
                // answers while it is ordered
                mapped.sort((left, right) => left.start - right.start);
            }

            element.$arquillEditor.setStyleRanges(mapped);
        };

        // the gutter icons are drawn as an overlay column tracking the ruler, orion has no api to put
        // an arbitrary image into its annotation ruler
        const gutter = document.createElement('div');
        gutter.className = 'arquill-gutter';
        element.appendChild(gutter);

        // the widest group of markers the column has been sized for, so it is only resized when it has to be -
        // widening it makes orion measure the whole view again
        let markColumnWidth = 0;

        const renderGutterMarks = () => {
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

            const lineHeight = textView.getLineHeight();

            // the box spans the whole row so it cannot read as sitting between two of them. the icon
            // inside it follows the font rather than the row - the row is twice the font height, and
            // an icon drawn at that size towers over the text it belongs to
            const contentDiv = element.querySelector('.textviewContent');
            const fontHeight = contentDiv ? Math.round(parseFloat(getComputedStyle(contentDiv).fontSize)) : 0;
            const size = fontHeight > 0 ? fontHeight : 12;

            gutter.style.left = (rulerRect.left - hostRect.left) + 'px';
            gutter.style.width = rulerRect.width + 'px';
            gutter.style.setProperty('--arquill-gutter-icon-size', size + 'px');

            // several markers can sit on one line - implemented interfaces and overridden methods both
            // land on a declaration - and they have to stand next to each other, not on top
            const perLine = {};

            // the group of a line is centered in the column as a whole, so its width has to be known
            // before the first of its marks is placed
            const countPerLine = {};

            let widest = 0;
            for (const mark of marks) {
                if (mark.line >= 0 && mark.line < baseModel.getLineCount()) {
                    const count = (countPerLine[mark.line] || 0) + 1;
                    countPerLine[mark.line] = count;

                    widest = Math.max(widest, count);
                }
            }

            // the awt gutter grows to hold the widest group of markers - three of them land on a declaration
            // which is both an override and an implementation. orion sizes its left ruler once, off the dom and
            // while the view is created, so the column is widened here and the view is made to measure again -
            // without that the third icon is drawn past the column and the overlay clips it away
            // a marker takes its own width plus the gap on either side, and the column has to hold as many of
            // those slots as the busiest line asks for
            const slot = size + MARK_GAP * 2;

            const wantedColumn = Math.max(MIN_MARK_COLUMN_WIDTH, widest * slot);
            if (markColumnWidth !== wantedColumn) {
                markColumnWidth = wantedColumn;
                element.style.setProperty('--arquill-gutter-marks-width', wantedColumn + 'px');

                // update(true) is the way back into _calculateMetrics, the same one a font change goes through
                textView.update(true);

                // everything below is placed at pixels the view reported, and all of them just moved
                renderGutterMarks();
                return;
            }

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

                // layering, resizing and colorizing are composed before the image reaches here, so a marker is
                // a single url however many images the platform built it from
                const icon = document.createElement('span');
                icon.className = 'arquill-gutter-mark';

                if (mark.iconHtml) {
                    icon.innerHTML = mark.iconHtml;
                }

                if (mark.tooltip) {
                    // the host listener would hide the tooltip again on the way up, the pointer is not
                    // over any text range while it is over the icon
                    icon.addEventListener('mousemove', domEvent => {
                        domEvent.stopPropagation();
                        showTooltip(mark.tooltip, domEvent.clientX, domEvent.clientY);
                    });
                    icon.addEventListener('mouseleave', hideTooltip);
                }
                icon.style.width = size + 'px';
                icon.style.height = lineHeight + 'px';
                icon.style.top = top + 'px';
                // a group wider than the column keeps to the left edge, half of it would be clipped away. the
                // gap is inside the slot, so the icon sits at its centre rather than against its neighbour
                icon.style.left = Math.max(
                    MARK_GAP,
                    (rulerRect.width - countPerLine[mark.line] * slot) / 2 + column * slot + MARK_GAP
                ) + 'px';
                icon.addEventListener('click', () => {
                    hideTooltip();
                    element.dispatchEvent(new CustomEvent('arquill-gutter-click', { detail: { id: mark.id } }));
                });
                gutter.appendChild(icon);
            }
        };

        // the vcs change bars, a strip of its own - the awt gutter paints them in the free painters
        // area right of the icons, so they must not land in the marker column
        // the line marker presentations, one absolutely positioned layer per gutter area. the areas the
        // browser gutter can tell apart are the strip left of the annotation ruler, the icons column and the
        // strip right of the folding ruler - the wider awt bands all resolve onto the whole gutter
        const gutterBands = document.createElement('div');
        gutterBands.className = 'arquill-gutter-bands';
        // the geometry of a band is computed here anyway, so the structural part of it is set here too - the
        // theme stylesheet only carries what is a matter of looks
        gutterBands.style.cssText = 'position:absolute;inset:0;overflow:hidden;pointer-events:none;z-index:1';
        element.appendChild(gutterBands);

        const bandBounds = (area, hostRect) => {
            const annotationRuler = element.querySelector('.textviewLeftRuler .ruler.annotations');
            const foldingRuler = element.querySelector('.textviewLeftRuler .ruler.folding');
            if (!annotationRuler || !foldingRuler) {
                return null;
            }

            const left = annotationRuler.getBoundingClientRect();
            const right = foldingRuler.getBoundingClientRect();

            switch (area) {
                case 'LEFT_FREE_PAINTERS':
                    return { left: left.left - hostRect.left, width: BAND_WIDTH };
                case 'RIGHT_FREE_PAINTERS':
                    return { left: right.right - hostRect.left - BAND_WIDTH - 1, width: BAND_WIDTH };
                case 'ICONS':
                    return { left: left.left - hostRect.left, width: left.width };
                default:
                    return { left: left.left - hostRect.left, width: right.right - left.left };
            }
        };

        const renderGutterBands = () => {
            gutterBands.textContent = '';

            const bands = element.$arquillGutterBands || [];
            if (!bands.length) {
                return;
            }

            const hostRect = element.getBoundingClientRect();

            for (const band of bands) {
                const bounds = bandBounds(band.area, hostRect);
                if (!bounds) {
                    continue;
                }

                // a marker whose range is empty sits on the boundary between the two surviving lines,
                // which is how a deletion is expressed - there are no lines left for it to cover
                const boundary = band.startLine === band.endLine;

                const startLine = toViewLine(band.startLine);
                if (startLine < 0) {
                    continue;
                }

                const top = pageY(startLine) - hostRect.top;

                let height;
                if (boundary) {
                    height = DELETION_HEIGHT;
                }
                else {
                    const endLine = toViewLine(band.endLine);
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
                bar.className = 'arquill-gutter-band';
                bar.style.position = 'absolute';
                bar.style.boxSizing = 'border-box';
                bar.style.left = bounds.left + 'px';
                bar.style.width = bounds.width + 'px';
                bar.style.top = (boundary ? top - DELETION_HEIGHT / 2 : top) + 'px';
                bar.style.height = height + 'px';

                if (band.color) {
                    bar.style.backgroundColor = band.color;
                }
                if (band.borderColor) {
                    bar.style.border = '1px ' + (band.dotted ? 'dotted ' : 'solid ') + band.borderColor;
                }

                gutterBands.appendChild(bar);
            }
        };

        // folding is driven through orion's own machinery: a FoldingAnnotation over the base model is
        // what the folding ruler draws and toggles, and a hand rolled annotation would have to
        // reimplement both. only its projection is taken over - orion hides whole lines and
        // substitutes nothing, its projection starting at the line after the region, while the
        // platform replaces the region itself with the placeholder of the fold
        const annotationModel = element.$arquillEditor.getAnnotationModel();
        const foldAnnotations = new Map();

        const placeholderOf = region => region && region.placeholder ? region.placeholder : '';

        const collapseToPlaceholder = function (checkOverlapping) {
            if (!this._collapse()) {
                return;
            }

            // projections may not overlap, so a region nested in the one being collapsed has to give
            // its own up - orion marks it and puts it back once the outer one is expanded again
            if (checkOverlapping) {
                this._forEachOverlaping(function (other) {
                    if (!other.expanded) {
                        other._expandImpl(false);
                        other._recollapse = true;
                    }
                });
            }

            this._projection = {
                annotation: this,
                start: this.start,
                end: this.end,
                text: placeholderOf(this.$arquillFold)
            };

            this._projectionModel.addProjection(this._projection);
        };

        // the placeholder stands where the region was, so the first line of a collapsed region is inside
        // its own projection, and every offset of a projection maps to nothing. the folding ruler then
        // finds no view line to draw the marker on and no annotation under a click - stock orion never
        // meets this because its own projection starts at the line after the region, leaving that first
        // line visible. both answers are taken from the row the placeholder ended up on
        const placeholderLine = annotation => {
            const projection = annotation._projection;
            if (annotation.expanded || !projection) {
                return -1;
            }

            const end = toViewOffset(projection.end);

            return end < 0 ? -1 : viewModel.getLineAtOffset(end - projection.text.length);
        };

        const foldingRuler = element.$arquillEditor.getFoldingRuler();
        if (foldingRuler) {
            const rulerAnnotations = foldingRuler.getAnnotations.bind(foldingRuler);

            foldingRuler.getAnnotations = (startLine, endLine) => {
                const result = rulerAnnotations(startLine, endLine);

                for (const annotation of foldAnnotations.values()) {
                    const line = placeholderLine(annotation);
                    if (line >= startLine && line < endLine) {
                        result[line] = annotation;
                    }
                }

                return result;
            };

            const rulerClick = foldingRuler.onClick.bind(foldingRuler);

            foldingRuler.onClick = (lineIndex, domEvent) => {
                for (const annotation of foldAnnotations.values()) {
                    if (placeholderLine(annotation) === lineIndex) {
                        annotation.expand();
                        return;
                    }
                }

                rulerClick(lineIndex, domEvent);
            };
        }

        // the placeholder stands where the region was, so its view range is what lies right before the
        // end of the projection - the document offsets it covers all map outside the view
        foldPlaceholderStyles = () => {
            const styles = [];

            for (const annotation of foldAnnotations.values()) {
                const region = annotation.$arquillFold;
                if (annotation.expanded || !region || !region.style || !placeholderOf(region)) {
                    continue;
                }

                const end = toViewOffset(annotation._projection.end);
                if (end < 0) {
                    continue;
                }

                styles.push({
                    start: end - annotation._projection.text.length,
                    end: end,
                    style: region.style
                });
            }

            return styles;
        };

        // an inlay is text standing in the view without being in the document - what a fold placeholder already
        // is, so it is the same mechanism: a projection of no width, which inserts its text and hides nothing.
        // the server keeps to document offsets and the model maps them, exactly as it does across a collapse
        const inlayProjections = new Map();

        const inlayText = inlay => {
            let text = '';
            for (const segment of inlay.segments || []) {
                text += (segment.text || '') + (segment.br ? '\n' : '');
            }
            return text;
        };

        // where a projection ended up in the view. a document offset maps past everything the projection
        // inserted, so its end is what answers and the text reaches back from there
        const projectionViewRange = projection => {
            const end = viewModel.mapOffset(projection.end, true);
            if (end < 0) {
                return null;
            }

            return { start: end - projection._model.getCharCount(), end: end };
        };

        // projections may not overlap, and a region about to collapse takes in whatever stood inside it. so the
        // inlays step aside before the fold regions are reconciled, and renderInlays puts back the ones that
        // still have somewhere to stand once the collapse has happened
        const dropInlayProjections = () => {
            for (const held of inlayProjections.values()) {
                viewModel.removeProjection(held.projection);
            }
            inlayProjections.clear();
        };

        // a block inlay takes a view line of its own, and the bundle numbers that line by mapping its start back
        // to the document - which answers nothing for a line that is only projected text, so it numbered them
        // "0". they carry no number at all, the way a line an inlay owns has none in the awt editor either
        const lineNumberRuler = element.$arquillEditor.getLineNumberRuler();
        if (lineNumberRuler) {
            const rulerAnnotations = lineNumberRuler.getAnnotations.bind(lineNumberRuler);

            lineNumberRuler.getAnnotations = (startLine, endLine) => {
                const result = rulerAnnotations(startLine, endLine);

                for (let line = startLine; line < endLine; line++) {
                    if (result[line] && toBaseOffset(viewModel.getLineStart(line)) < 0) {
                        result[line].html = '';
                    }
                }

                return result;
            };
        }

        const renderInlays = () => {
            const wanted = new Map();
            for (const inlay of element.$arquillInlays ? JSON.parse(element.$arquillInlays) : []) {
                wanted.set(inlay.offset, inlay);
            }

            // reconciled rather than rebuilt, as the fold regions are - dropping and readding every projection
            // would move the view out from under the caret on each round trip
            textView.setRedraw(false);
            try {
                for (const [offset, held] of Array.from(inlayProjections)) {
                    const inlay = wanted.get(offset);

                    // kept while the anchor is still wanted, still says the same thing, and is still somewhere a
                    // projection can stand - a region collapsed over it takes the last of those away
                    if (inlay && inlayText(inlay) === held.text && toViewOffset(offset) >= 0) {
                        held.segments = inlay.segments;
                        continue;
                    }

                    viewModel.removeProjection(held.projection);
                    inlayProjections.delete(offset);
                }

                for (const [offset, inlay] of wanted) {
                    if (inlayProjections.has(offset)) {
                        continue;
                    }

                    // projections may not overlap, and an offset inside a collapsed one maps to nothing
                    if (toViewOffset(offset) < 0) {
                        continue;
                    }

                    const text = inlayText(inlay);
                    if (!text) {
                        continue;
                    }

                    const projection = { start: offset, end: offset, text: text };
                    viewModel.addProjection(projection);
                    inlayProjections.set(offset, { projection: projection, text: text, segments: inlay.segments });
                }
            }
            finally {
                textView.setRedraw(true);
            }
        };

        inlayStyles = () => {
            const styles = [];

            for (const held of inlayProjections.values()) {
                const range = projectionViewRange(held.projection);
                if (!range) {
                    continue;
                }

                // the runs are laid out in the order they were concatenated into the projection, and the break a
                // block inlay ends on is a character of the projection like any other
                let start = range.start;
                for (const segment of held.segments || []) {
                    const text = segment.text || '';
                    if (text && segment.style) {
                        // a run which reaches an action is marked on the span itself - the click is answered by
                        // what it stands for and not by where it is, which is nowhere the document knows about
                        const style = segment.click === undefined
                            ? segment.style
                            : Object.assign({}, segment.style, {
                                styleClass: (segment.style.styleClass || '') + ' arquill-inlay-action',
                                attributes: { 'data-arquill-inlay-click': String(segment.click) }
                            });

                        styles.push({ start: start, end: start + text.length, style: style });
                    }

                    start += text.length + (segment.br ? 1 : 0);
                }
            }

            return styles;
        };

        // orion will put the caret inside a projection, and an offset in there maps to no document offset at all -
        // the platform would keep the caret it had and quietly disagree with what is on screen. neither an inlay
        // nor a fold placeholder is somewhere the user can stand, so the caret steps over it instead
        let snappingCaret = false;

        const snapCaretOutOfProjection = viewOffset => {
            if (snappingCaret) {
                return false;
            }

            for (const projection of viewModel.getProjections()) {
                const range = projectionViewRange(projection);
                if (!range || viewOffset <= range.start || viewOffset >= range.end) {
                    continue;
                }

                // whichever edge the caret was not already on, so arrowing through moves past rather than sticking
                const previous = element.$arquillLastViewCaret;
                const target = typeof previous !== 'number' || previous <= range.start ? range.end : range.start;

                snappingCaret = true;
                try {
                    textView.setSelection(target, target, true);
                }
                finally {
                    snappingCaret = false;
                }
                return true;
            }

            return false;
        };

        const renderFoldRegions = () => {
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
                dropInlayProjections();

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
                        annotation._collapseImpl = collapseToPlaceholder;
                        foldAnnotations.set(key, annotation);
                    }

                    annotation.$arquillFold = region;

                    // the projection holds a copy of the placeholder, a new one only reaches the view
                    // by being built again
                    if (!annotation.expanded && annotation._projection.text !== placeholderOf(region)) {
                        annotation.expand();
                        annotation.collapse();
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

            // a region that just opened or closed decides whether the inlays inside it have anywhere to stand
            renderInlays();

            onProjectionChanged();
        };

        let redrawErrorStripe = null;

        // collapsing shifts every view offset after the region, so everything that was pushed in
        // document offsets has to be placed again
        const onProjectionChanged = () => {
            pushStyleRanges();
            renderGutterMarks();
            renderGutterBands();

            // the stripe is built after this, and the first fold pass already runs through here
            if (redrawErrorStripe) {
                redrawErrorStripe();
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
                    Promise.resolve().then(() => onProjectionChanged());
                }
            });
        }

        const onViewChanged = () => {
            renderGutterMarks();
            renderGutterBands();
        };

        textView.addEventListener('Scroll', () => {
            onViewChanged();
            placeCaret();
        });
        textView.addEventListener('Resize', onViewChanged);

        renderFoldRegions();

        onProjectionChanged();

        // the analyze status of the whole file, the counterpart of the awt status panel that sits over
        // the top right corner of the editor
        const status = document.createElement('div');
        status.className = 'arquill-status';
        element.appendChild(status);

        let statusTooltip = '';

        // the panel floats over the text, and without stopping the pointer here the editor wide
        // handlers answer for the line hidden behind it - its tooltip, its underline, its offset
        status.addEventListener('mousemove', domEvent => {
            domEvent.stopPropagation();
            fireHover(-1);

            if (statusTooltip) {
                showTooltip(statusTooltip, domEvent.clientX, domEvent.clientY);
            }
            else {
                hideTooltip();
            }
        });

        status.addEventListener('mouseleave', hideTooltip);

        status.addEventListener('mousedown', domEvent => domEvent.stopPropagation());

        const renderAnalyzeStatus = () => {
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

                if (item.iconHtml) {
                    const icon = document.createElement('span');
                    icon.className = 'arquill-status-icon';
                    icon.innerHTML = item.iconHtml;

                    cell.appendChild(icon);
                }

                cell.appendChild(document.createTextNode(item.text || ''));
                status.appendChild(cell);
            }

            statusTooltip = model.tooltip || '';
        };

        renderAnalyzeStatus();

        // the error stripe is orion's own overview ruler, the marks the awt DesktopEditorErrorPanel
        // paints are annotations in the model the folding already uses. the ruler squeezes the
        // document into its own height and scrolls to a mark on click by itself
        element.$arquillEditor.setOverviewRulerVisible(true);

        const overviewRuler = element.$arquillEditor.getOverviewRuler();

        if (overviewRuler) {
            // the ruler has room for one mark per line and keeps the annotation of the type it was
            // given first, so the marks are put in front of the current line annotation orion
            // registers on it - the caret row is not wanted in the stripe
            overviewRuler.addAnnotationType(STRIPE_ANNOTATION_TYPE, 1);
        }

        const stripeAnnotations = new Map();

        // holds everything the mark is built from, so an unchanged one is left in the model
        const stripeKey = slot =>
            JSON.stringify([slot.start, slot.end, slot.color, slot.thin, slot.title]);

        redrawErrorStripe = () => {
            // a document ruler is only rebuilt when it is asked to. the annotations are in document
            // offsets while the ruler lays them out in view lines, so folding moves every mark
            if (overviewRuler) {
                textView.redrawLines(0, undefined, overviewRuler);
            }
        };

        const renderErrorStripe = () => {
            if (!annotationModel) {
                return;
            }

            const model = element.$arquillErrorStripeMarks
                ? JSON.parse(element.$arquillErrorStripeMarks)
                : {};

            const lineCount = baseModel.getLineCount();
            const wanted = new Map();

            for (const mark of model.visible === false ? [] : model.marks || []) {
                if (mark.line < 0 || mark.line >= lineCount) {
                    continue;
                }

                let slot = wanted.get(mark.line);
                if (!slot) {
                    slot = { layer: mark.layer, tooltips: [] };
                    wanted.set(mark.line, slot);
                }

                // the awt panel keeps one stripe per layer and lets the topmost one paint over the
                // ones below, here the topmost is the only one there is room for
                if (slot.color === undefined || mark.layer >= slot.layer) {
                    const lastLine = Math.min(lineCount - 1, mark.line + MAX_MARK_LINES - 1);

                    slot.layer = mark.layer;
                    slot.color = mark.color;
                    slot.thin = mark.thin;
                    slot.start = mark.start;
                    slot.end = Math.min(mark.end, baseModel.getLineEnd(lastLine));
                }

                if (mark.tooltip && slot.tooltips.indexOf(mark.tooltip) < 0) {
                    slot.tooltips.push(mark.tooltip);
                }
            }

            for (const slot of wanted.values()) {
                slot.title = slot.tooltips.map(text => '<div>' + text + '</div>').join('');
            }

            // the annotations are reconciled rather than rebuilt - every add and every remove
            // notifies the model and redraws the ruler, and the daemon pushes the whole set again
            // on each of its passes
            element.$arquillSuppressFold = true;
            try {
                for (const [line, annotation] of Array.from(stripeAnnotations)) {
                    const slot = wanted.get(line);
                    if (slot && annotation.$arquillKey === stripeKey(slot)) {
                        continue;
                    }

                    annotationModel.removeAnnotation(annotation);
                    stripeAnnotations.delete(line);
                }

                for (const [line, slot] of wanted) {
                    if (stripeAnnotations.has(line)) {
                        continue;
                    }

                    // the ruler paints an annotation through its overviewStyle alone, the color of
                    // the highlighter comes from the platform scheme rather than from the type
                    const annotation = {
                        type: STRIPE_ANNOTATION_TYPE,
                        start: slot.start,
                        end: slot.end,
                        title: slot.title,
                        $arquillKey: stripeKey(slot),
                        overviewStyle: {
                            styleClass: 'annotationOverview arquillStripe'
                                + (slot.thin ? ' arquillStripeThin' : ''),
                            style: { backgroundColor: slot.color }
                        }
                    };

                    annotationModel.addAnnotation(annotation);
                    stripeAnnotations.set(line, annotation);
                }
            }
            finally {
                element.$arquillSuppressFold = false;
            }
        };

        // the tooltip of the ruler is dead in this bundle - Ruler._getTooltipInfo has no caller and
        // Editor.getTooltip answers null - so the marks are hovered through the floating element the
        // text ranges use, which the html of the daemon messages needs anyway
        if (overviewRuler && overviewRuler.node) {
            overviewRuler.node.addEventListener('mousemove', domEvent => {
                // the host listener would hide the tooltip again on the way up, the pointer is over
                // no text range while it is over the ruler
                domEvent.stopPropagation();

                // orion hangs the annotation it drew a row from onto the row itself
                const annotation = domEvent.target.annotation;
                if (annotation && annotation.title) {
                    showTooltip(annotation.title, domEvent.clientX, domEvent.clientY);
                }
                else {
                    hideTooltip();
                }
            });

            overviewRuler.node.addEventListener('mouseleave', hideTooltip);
            overviewRuler.node.addEventListener('click', hideTooltip);
        }

        renderErrorStripe();

        element.$arquillApi = {
            setText: text => {
                if (element.$arquillEditor) {
                    // orion reports our own edit through the model, the flag keeps it from bouncing back to the server
                    element.$arquillSuppressChange = true;
                    try {
                        // contentsSaved must stay false, orion skips the text when it is set
                        element.$arquillEditor.setInput(null, null, text, false, true);
                    }
                    finally {
                        element.$arquillSuppressChange = false;
                    }
                }
            },

            replaceText: (start, end, text) => {
                if (element.$arquillEditor) {
                    // orion reports our own edit through the model, the flag keeps it from bouncing back to the server
                    element.$arquillSuppressChange = true;
                    try {
                        element.$arquillEditor.getModel().setText(text, start, end);
                    }
                    finally {
                        element.$arquillSuppressChange = false;
                    }
                }
            },

            setCaretStyle: (width, blinkPeriod) => {
                if (width > 0) {
                    element.style.setProperty('--arquill-editor-caret-width', width + 'px');
                }

                // a period of zero is a caret which does not blink at all
                if (blinkPeriod > 0) {
                    element.style.setProperty('--arquill-editor-caret-blink-period', blinkPeriod + 'ms');
                    caretElement.classList.add('arquill-caret-blinking');
                }
                else {
                    caretElement.classList.remove('arquill-caret-blinking');
                }

                placeCaret();
            },

            setReadOnly: readOnly => {
                if (element.$arquillEditor) {
                    element.$arquillEditor.getTextView().setOptions({ readonly: readOnly });
                }
            },

            setCaretOffset: offset => element.$arquillApi.setSelection(offset, offset),

            /**
             * Where the platform's caret is and what it has selected, as one thing. A caret is a range of no width,
             * so there is one channel and not two - two would race, and whichever of them arrived last would win:
             * a selection push followed by a caret push collapses the range that was just drawn.
             */
            setSelection: (start, end) => {
                // the caret is at the end of the range - that is the edge that moves as it grows. remembered first,
                // otherwise orion fires Selection back and the server sees its own move
                element.$arquillCaretOffset = end;
                element.$arquillSelectionStart = Math.min(start, end);
                element.$arquillSelectionEnd = Math.max(start, end);

                if (!element.$arquillEditor) {
                    return;
                }

                if (start !== end) {
                    element.$arquillSuppressChange = true;
                    try {
                        element.$arquillEditor.getTextView().setSelection(toViewOffset(start), toViewOffset(end), true);
                    }
                    finally {
                        element.$arquillSuppressChange = false;
                    }

                    // a collapsed push landing on the same offset afterwards has a range to clear, so it must not be
                    // taken for a repeat of one already applied
                    element.$arquillLastRectOffset = -1;
                    placeCaret();
                    return;
                }

                // the server pushes the caret from more than one of its listeners per edit, and every push
                // answered here costs it a round trip - the same offset over an unchanged layout has the
                // same rect the server already holds
                if (element.$arquillLastRectOffset === end) {
                    return;
                }

                element.$arquillEditor.setCaretOffset(end, true);
                element.$arquillLastRectOffset = end;

                // the offset is the server's own and does not have to come back, but where it landed on screen
                // is only measurable here. suppressing the whole event left the server holding the rect from
                // wherever the caret was last put by hand, and a popup anchored to the caret opened there -
                // pressing return and asking for completion put the list beside the old line
                // rect only: the offset is the server's own and moving the platform caret to where it
                // already is counts as a caret move, which is what a lookup goes away on
                element.dispatchEvent(new CustomEvent('arquill-caret', {
                    detail: Object.assign(caretDetail(end, toViewOffset(end)), {
                        rectOnly: true,
                        selectionStart: end,
                        selectionEnd: end
                    })
                }));
                placeCaret();
            },

            setLinkHovered: hovered => element.classList.toggle('arquill-ctrl-hover', hovered),

            // the theme reads both properties, the bundled .textview rule would otherwise win over an inline
            // style set on the host
            setFont: (fontName, fontSize, lineSpacingValue) => {
                const quoted = fontName ? JSON.stringify(fontName) : '';

                if (quoted) {
                    // the browser falls back per glyph, so a family that never loaded still renders as text
                    element.style.setProperty('--arquill-editor-font-family', quoted + ', monospace');
                }

                if (fontSize > 0) {
                    element.style.setProperty('--arquill-editor-font-size', fontSize + 'px');
                }

                if (lineSpacingValue > 0) {
                    lineSpacing = lineSpacingValue;
                }

                // orion measures its metrics while the view is created and lays every line out against what
                // it cached - update(true) is the only way back into _calculateMetrics, and the overlays are
                // placed at pixels the view reported, all of which just moved
                const remeasure = () => {
                    // the row is published first: orion caches what it measures, and updating before the height
                    // is set leaves every line laid out against the previous one
                    reportMetrics();
                    textView.update(true);
                    onProjectionChanged();
                    placeCaret();
                };

                remeasure();

                // the face arrives over http, so the first measurement above ran against whatever the
                // fallback is - it has to be taken again once the real one is in
                if (quoted && document.fonts) {
                    document.fonts.load((fontSize > 0 ? fontSize : 12) + 'px ' + quoted).then(remeasure, remeasure);
                }
            },

            setColors: (background, foreground, selectionBackground, caretRowBackground) => {
                const set = (name, value) => {
                    if (value) {
                        element.style.setProperty(name, value);
                    }
                    else {
                        element.style.removeProperty(name);
                    }
                };

                set('--arquill-editor-background', background);
                set('--arquill-editor-foreground', foreground);
                set('--arquill-editor-selection-background', selectionBackground);
                set('--arquill-editor-caret-row-background', caretRowBackground);
            },

            setGutterMarks: marksJson => {
                element.$arquillGutterMarks = marksJson;
                renderGutterMarks();
            },

            setAnalyzeStatus: statusJson => {
                element.$arquillAnalyzeStatus = statusJson;
                renderAnalyzeStatus();
            },

            // parsed once here, the lookup runs on every pointer move
            setTooltipRanges: rangesJson => {
                element.$arquillTooltipRanges = JSON.parse(rangesJson);
            },

            // kept parsed - folding has to remap every range into view offsets again on each collapse and expand
            setStyleRanges: rangesJson => {
                element.$arquillStyleRanges = JSON.parse(rangesJson);
                pushStyleRanges();
            },

            setSchemeStyles: css => {
                let style = element.$arquillSchemeStyle;
                if (!style) {
                    style = document.createElement('style');
                    element.$arquillSchemeStyle = style;
                    element.appendChild(style);
                }

                if (style.textContent !== css) {
                    style.textContent = css;
                }
            },

            setFoldRegions: regionsJson => {
                element.$arquillFoldRegions = regionsJson;
                renderFoldRegions();
            },

            // adding a projection shifts every view offset after it, so the pushes keyed by offset are placed again
            setInlays: inlaysJson => {
                element.$arquillInlays = inlaysJson;
                renderInlays();
                onProjectionChanged();
            },

            setGutterBands: bands => {
                element.$arquillGutterBands = bands;
                renderGutterBands();
            },

            setErrorStripeMarks: marksJson => {
                element.$arquillErrorStripeMarks = marksJson;
                renderErrorStripe();
            },

            destroy: () => {
                // the tooltip lives on the body and the modifier listener on the document, neither goes away with
                // the element
                hideTooltip();
                tooltip.remove();

                document.removeEventListener('keyup', onKeyUp);

                if (element.$arquillEditor) {
                    element.$arquillEditor.uninstall();
                    element.$arquillEditor = null;
                }
            }
        };

        // the server pushes as soon as the editor is opened, long before the bundle is in - the stub the element
        // was given on creation collected those calls, and this is the first moment they can be answered
        const pending = element.$arquillPending || [];
        element.$arquillPending = null;

        for (const [name, args] of pending) {
            element.$arquillApi[name](...args);
        }
    };

    window.arquillEditorElement = { install: install };
})();
