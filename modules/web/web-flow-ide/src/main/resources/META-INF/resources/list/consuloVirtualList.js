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

/*
 * A list which scrolls over a count rather than over its rows.
 *
 * The rows it holds are a fixed pool - as many as fit on screen and a few more - and the server rebinds them as the
 * window moves. Nothing is ever added or removed, so a list whose items changed costs the properties that actually
 * differ and not a tree of components per visible line, which is what a completion popup was paying on every typed
 * character.
 *
 * What makes the scrollbar right without any of the rows existing is the count: the scroll area is
 * itemCount * rowHeight, set as one number, and a model of five items or five hundred is the same single property.
 */
(() => {
    if (customElements.get('consulo-virtual-list')) {
        return;
    }

    /* rows kept above and below the viewport, so a small scroll shows something already bound */
    const OVERSCAN = 4;

    class ConsuloVirtualList extends HTMLElement {
        constructor() {
            super();
            this.$itemCount = 0;
            this.$rowHeight = 24;
            this.$first = -1;
            this.$count = -1;
        }

        connectedCallback() {
            this.style.display = 'block';
            this.style.position = 'relative';
            this.style.overflowY = 'auto';
            this.style.overflowX = 'hidden';

            if (!this.$scrollListener) {
                this.$scrollListener = () => this.$report();
                this.addEventListener('scroll', this.$scrollListener, { passive: true });
            }

            // the element is measured to know how many rows fit, and on attach it may not have been laid out yet
            if (!this.$resizeObserver && typeof ResizeObserver !== 'undefined') {
                this.$resizeObserver = new ResizeObserver(() => this.$report());
                this.$resizeObserver.observe(this);
            }

            this.$report();
        }

        disconnectedCallback() {
            if (this.$resizeObserver) {
                this.$resizeObserver.disconnect();
                this.$resizeObserver = null;
            }
        }

        /**
         * How many items the model holds. The only thing that has to travel when the list changes length - the rows
         * themselves are the pool, and the server rebinds them.
         */
        setItemCount(count) {
            this.$itemCount = count < 0 ? 0 : count;
            this.$applyScrollHeight();
            this.$report();
        }

        setRowHeight(height) {
            this.$rowHeight = height > 0 ? height : 24;
            this.$applyScrollHeight();
            this.$report();
        }

        /**
         * Puts a pooled row at the item it currently stands for. Called per bound row, and it is a style on a node
         * which already exists rather than a node being made.
         */
        placeRow(row, index) {
            if (!row) {
                return;
            }

            row.style.position = 'absolute';
            row.style.left = '0';
            row.style.right = '0';
            row.style.height = this.$rowHeight + 'px';
            row.style.top = (index * this.$rowHeight) + 'px';
        }

        /**
         * Brings an item into view, moving as little as it can - the row the user is on should stay where it is
         * unless it would leave the window.
         */
        scrollToIndex(index) {
            if (index < 0 || this.$rowHeight <= 0) {
                return;
            }

            const top = index * this.$rowHeight;
            const bottom = top + this.$rowHeight;

            if (top < this.scrollTop) {
                this.scrollTop = top;
            }
            else if (bottom > this.scrollTop + this.clientHeight) {
                this.scrollTop = bottom - this.clientHeight;
            }
        }

        $applyScrollHeight() {
            // a pseudo element carries it, so the scroll area needs no child of its own competing with the rows
            this.style.setProperty('--consulo-vlist-scroll-height', (this.$itemCount * this.$rowHeight) + 'px');
        }

        /**
         * Tells the server which rows it should be showing, and only when that changed - a scroll inside the rows
         * already bound is the browser's own business and costs nothing.
         */
        $report() {
            if (!this.isConnected || this.$rowHeight <= 0) {
                return;
            }

            const viewport = this.clientHeight || (this.$rowHeight * 10);
            let first = Math.floor(this.scrollTop / this.$rowHeight) - OVERSCAN;
            if (first < 0) {
                first = 0;
            }

            let count = Math.ceil(viewport / this.$rowHeight) + OVERSCAN * 2;
            if (first + count > this.$itemCount) {
                count = this.$itemCount - first;
            }
            if (count < 0) {
                count = 0;
            }

            if (first === this.$first && count === this.$count) {
                return;
            }

            this.$first = first;
            this.$count = count;

            this.dispatchEvent(new CustomEvent('consulo-range', { detail: { first: first, count: count } }));
        }
    }

    customElements.define('consulo-virtual-list', ConsuloVirtualList);
})();
