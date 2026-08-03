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

            fireHover(domEvent.ctrlKey || domEvent.metaKey ? offset : -1);

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
                fireHover(-1);
            }
        };

        // the modifier can be released while the focus is elsewhere, so the listener is global and
        // has to be dropped by hand on detach
        document.addEventListener('keyup', onKeyUp);

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

        // the fold placeholders are not in the document, so their ranges are built by the folding code
        // further down rather than pushed by the server
        let foldPlaceholderStyles = null;

        // the bundle hands the ranges straight to the LineStyle event, which reports view offsets,
        // so what the server pushed in document offsets has to be mapped first
        // the end of a range is exclusive, so it may sit exactly on the start of a collapsed region -
        // an offset inside a projection maps to nothing, and the tag name of a folded <build...> would
        // lose its colour along with the text the fold hides. the last character it covers still maps
        const toViewRangeEnd = offset => {
            const end = toViewOffset(offset);
            if (end >= 0) {
                return end;
            }

            const last = toViewOffset(offset - 1);

            return last < 0 ? -1 : last + 1;
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

            const placeholders = foldPlaceholderStyles ? foldPlaceholderStyles() : [];
            if (placeholders.length) {
                for (const placeholder of placeholders) {
                    mapped.push(placeholder);
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

            for (const mark of marks) {
                if (mark.line >= 0 && mark.line < baseModel.getLineCount()) {
                    countPerLine[mark.line] = (countPerLine[mark.line] || 0) + 1;
                }
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
                // a group wider than the column keeps to the left edge, half of it would be clipped away
                icon.style.left =
                    Math.max(0, (rulerRect.width - countPerLine[mark.line] * size) / 2 + column * size) + 'px';
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

        textView.addEventListener('Scroll', onViewChanged);
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

            setReadOnly: readOnly => {
                if (element.$arquillEditor) {
                    element.$arquillEditor.getTextView().setOptions({ readonly: readOnly });
                }
            },

            setCaretOffset: offset => {
                // remember the offset first, otherwise orion fires Selection back and the server sees its own move
                element.$arquillCaretOffset = offset;
                if (element.$arquillEditor) {
                    element.$arquillEditor.setCaretOffset(offset, true);
                }
            },

            setLinkHovered: hovered => element.classList.toggle('arquill-ctrl-hover', hovered),

            // the theme reads both properties, the bundled .textview rule would otherwise win over an inline
            // style set on the host
            setFont: (fontName, fontSize) => {
                const quoted = fontName ? JSON.stringify(fontName) : '';

                if (quoted) {
                    // the browser falls back per glyph, so a family that never loaded still renders as text
                    element.style.setProperty('--arquill-editor-font-family', quoted + ', monospace');
                }

                if (fontSize > 0) {
                    element.style.setProperty('--arquill-editor-font-size', fontSize + 'px');
                }

                // orion measures its metrics while the view is created and lays every line out against what
                // it cached - update(true) is the only way back into _calculateMetrics, and the overlays are
                // placed at pixels the view reported, all of which just moved
                const remeasure = () => {
                    textView.update(true);
                    onProjectionChanged();
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
