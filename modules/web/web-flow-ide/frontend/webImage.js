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
        web-image, web-image-empty, web-image-layered, web-image-colorize, web-image-transparent {
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

    const TAGS = {
        'web-image': WebImage,
        'web-image-empty': WebImageEmpty,
        'web-image-layered': WebImageLayered,
        'web-image-colorize': WebImageColorize,
        'web-image-transparent': WebImageTransparent
    };

    for (const [tag, type] of Object.entries(TAGS)) {
        if (!customElements.get(tag)) {
            customElements.define(tag, type);
        }
    }
})();
