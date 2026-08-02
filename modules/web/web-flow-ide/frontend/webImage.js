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
 * The image effects of the platform as custom elements. The servlet stays a plain resource endpoint - it hands
 * out the bytes of one icon and knows nothing about composition - and every effect is applied here, where the
 * browser can do it for free.
 *
 * A composed image is a tree of these tags, so the same markup works wherever an icon is shown: appended to a
 * vaadin component, or pushed into the editor as a string.
 */
(() => {
    const SIZE_ATTRIBUTES = ['width', 'height'];

    /*
     * The layout is a stylesheet rather than something the elements apply to themselves: flow attaches a parent
     * before it appends the children, so anything an element does to its children on connect runs while it still
     * has none - which left every layer of a composition standing in normal flow beside the others.
     */
    const style = document.createElement('style');
    style.textContent = `
        web-image, web-image-empty, web-image-layered, web-image-colorize, web-image-transparent,
        web-image-grayed, web-image-append, web-image-text {
            display: inline-block;
            position: relative;
            line-height: 0;

            /* the own size is what the platform asked for and is written inline, so nothing in a
               stylesheet could outweigh it - a host fitting an icon into a box of its own sets
               --web-image-width instead, which inherits into every layer of a composition */
            width: var(--web-image-width, var(--web-image-own-width));
            height: var(--web-image-height, var(--web-image-own-height));
        }

        /* a layer keeps the size it was given and is drawn from the top left, the way the platform paints them */
        web-image-layered > * {
            position: absolute;
            top: 0;
            left: 0;
        }

        web-image > img {
            width: 100%;
            height: 100%;
            /* a non square icon must not be stretched into a square box */
            object-fit: contain;
        }

        /* the two sides stand beside each other and are centered against the taller one, the way the desktop
           frontend rows them up */
        web-image-append {
            display: inline-flex;
            align-items: center;
        }

        /* a side keeps the size the platform gave it - a host fitting the pair into a box of its own sizes
           the pair, not each half of it */
        web-image-append > * {
            flex: none;
            --web-image-width: var(--web-image-own-width);
            --web-image-height: var(--web-image-own-height);
        }

        /* the badge sits in the corner of the icon and is allowed to be wider than it, since the platform
           sizes a texted image by the icon alone */
        web-image-text > span {
            position: absolute;
            right: 0;
            bottom: 0;
            line-height: 1;
            font-size: 6px;
            white-space: nowrap;
            color: var(--lumo-body-text-color, #000);
            /* the desktop frontend paints the text over a halo of the background colour so that it stays
               readable wherever it falls on the icon */
            text-shadow:
                1px 0 var(--lumo-base-color, #fff),
                -1px 0 var(--lumo-base-color, #fff),
                0 1px var(--lumo-base-color, #fff),
                0 -1px var(--lumo-base-color, #fff);
        }
    `;
    document.head.appendChild(style);

    class WebImageBase extends HTMLElement {
        static get observedAttributes() {
            return SIZE_ATTRIBUTES;
        }

        connectedCallback() {
            this.applySize();
            this.render();

            // the children arrive after the element is attached, and an effect reading them has to run again.
            // only src is watched - rendering writes styles, and observing those would call it right back
            if (this.watchesChildren()) {
                this.myObserver = new MutationObserver(() => this.render());
                this.myObserver.observe(this, { childList: true, subtree: true, attributeFilter: ['src'] });
            }
        }

        disconnectedCallback() {
            if (this.myObserver) {
                this.myObserver.disconnect();
                this.myObserver = null;
            }
        }

        attributeChangedCallback() {
            if (this.isConnected) {
                this.applySize();
                this.render();
            }
        }

        watchesChildren() {
            return false;
        }

        applySize() {
            for (const name of SIZE_ATTRIBUTES) {
                const value = this.getAttribute(name);
                if (value) {
                    this.style.setProperty('--web-image-own-' + name, value + 'px');
                }
            }
        }

        render() {
        }
    }

    /**
     * A leaf - the only tag that reaches the servlet. The intrinsic size of what comes back is irrelevant, a
     * two times bigger asset is served on purpose and the box is what decides how large it is drawn.
     */
    class WebImage extends WebImageBase {
        static get observedAttributes() {
            return [...SIZE_ATTRIBUTES, 'src'];
        }

        render() {
            const src = this.getAttribute('src');

            let image = this.firstElementChild;
            if (!image) {
                image = document.createElement('img');
                image.alt = '';
                this.appendChild(image);
            }

            if (src && image.getAttribute('src') !== src) {
                image.setAttribute('src', src);
            }
        }
    }

    /**
     * Holds the box open without drawing anything - the platform uses an empty image as a spacer so that
     * labels with and without an icon line up.
     */
    class WebImageEmpty extends WebImageBase {
    }

    /**
     * The children are drawn on top of each other, in the order the platform layered them. The stacking is in
     * the stylesheet, so nothing has to run once the children arrive.
     */
    class WebImageLayered extends WebImageBase {
    }

    /**
     * Recolours whatever the child draws. The icon is used as a mask rather than an image, so its own colours
     * are discarded and its alpha decides what the fill reaches - which works for an svg and for a raster
     * alike, and needs neither the source to be recognisable nor the server to decode it.
     */
    class WebImageColorize extends WebImageBase {
        static get observedAttributes() {
            return [...SIZE_ATTRIBUTES, 'color'];
        }

        watchesChildren() {
            return true;
        }

        render() {
            const color = this.getAttribute('color');
            const source = this.querySelector('web-image');
            const src = source && source.getAttribute('src');

            if (!color || !src) {
                return;
            }

            // the child only carries the shape now, the fill of this box is what is seen
            source.style.visibility = 'hidden';

            this.style.backgroundColor = color;
            this.style.webkitMaskImage = 'url("' + src + '")';
            this.style.maskImage = 'url("' + src + '")';

            for (const property of ['webkitMaskSize', 'maskSize']) {
                this.style[property] = 'contain';
            }
            for (const property of ['webkitMaskRepeat', 'maskRepeat']) {
                this.style[property] = 'no-repeat';
            }
            for (const property of ['webkitMaskPosition', 'maskPosition']) {
                this.style[property] = 'center';
            }
        }
    }

    class WebImageTransparent extends WebImageBase {
        static get observedAttributes() {
            return [...SIZE_ATTRIBUTES, 'alpha'];
        }

        render() {
            const alpha = this.getAttribute('alpha');
            if (alpha) {
                this.style.opacity = alpha;
            }
        }
    }

    const GRAY_FILTERS = new Set();

    /*
     * The gray filter of the desktop frontend spends the colour of a pixel down to a third of its luminance
     * and then pulls the result back towards white by the percentage it was built with. That is a linear map
     * of the source channels, so a colour matrix says the same thing to the browser in one pass, for an svg
     * and for a raster alike.
     */
    function grayFilterId(percent) {
        const id = 'web-image-gray-' + percent;
        if (GRAY_FILTERS.has(id)) {
            return id;
        }
        GRAY_FILTERS.add(id);

        const rest = (100 - parseInt(percent, 10)) / 100;
        const scale = rest / 3;
        const row = [0.30 * scale, 0.59 * scale, 0.11 * scale, 0, 1 - rest];
        const values = [...row, ...row, ...row, 0, 0, 0, 1, 0].join(' ');

        const namespace = 'http://www.w3.org/2000/svg';

        const matrix = document.createElementNS(namespace, 'feColorMatrix');
        matrix.setAttribute('type', 'matrix');
        matrix.setAttribute('values', values);

        const filter = document.createElementNS(namespace, 'filter');
        filter.setAttribute('id', id);
        // without this the matrix would be applied to linearised channels and the icon would come out darker
        filter.setAttribute('color-interpolation-filters', 'sRGB');
        filter.appendChild(matrix);

        const svg = document.createElementNS(namespace, 'svg');
        svg.setAttribute('width', '0');
        svg.setAttribute('height', '0');
        svg.style.position = 'absolute';
        svg.appendChild(filter);

        document.body.appendChild(svg);
        return id;
    }

    class WebImageGrayed extends WebImageBase {
        static get observedAttributes() {
            return [...SIZE_ATTRIBUTES, 'percent'];
        }

        render() {
            const percent = this.getAttribute('percent');
            if (percent) {
                this.style.filter = 'url(#' + grayFilterId(percent) + ')';
            }
        }
    }

    /**
     * The two sides are laid out by the stylesheet, so nothing has to run once the children arrive.
     */
    class WebImageAppend extends WebImageBase {
    }

    class WebImageText extends WebImageBase {
        static get observedAttributes() {
            return [...SIZE_ATTRIBUTES, 'text'];
        }

        render() {
            let label = this.querySelector(':scope > span');
            if (!label) {
                label = document.createElement('span');
                this.appendChild(label);
            }

            label.textContent = this.getAttribute('text') || '';
        }
    }

    const TAGS = {
        'web-image': WebImage,
        'web-image-empty': WebImageEmpty,
        'web-image-layered': WebImageLayered,
        'web-image-colorize': WebImageColorize,
        'web-image-transparent': WebImageTransparent,
        'web-image-grayed': WebImageGrayed,
        'web-image-append': WebImageAppend,
        'web-image-text': WebImageText
    };

    for (const [tag, type] of Object.entries(TAGS)) {
        if (!customElements.get(tag)) {
            customElements.define(tag, type);
        }
    }
})();
