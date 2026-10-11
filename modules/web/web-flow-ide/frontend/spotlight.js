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
(() => {
    const TARGET_ATTRIBUTE = 'data-consulo-spotlight';
    const LIT_ATTRIBUTE = 'data-consulo-spotlight-lit';
    const SHAPE_ATTRIBUTE = 'data-consulo-spotlight-shape';
    const SVG_NAMESPACE = 'http://www.w3.org/2000/svg';

    const HORIZONTAL_INSET = 8;
    const VERTICAL_INSET = 6;
    const SECTION_VERTICAL_INSET = 2;
    const TAB_INSET = 1;
    const MAX_ARC = 30;
    const STROKE = 2;

    let nextMaskId = 0;

    const insetsOf = element => {
        const shape = element.getAttribute(SHAPE_ATTRIBUTE);
        if (shape === 'tab') {
            return { horizontal: TAB_INSET, vertical: TAB_INSET };
        }

        if (shape === 'section') {
            return { horizontal: HORIZONTAL_INSET, vertical: SECTION_VERTICAL_INSET };
        }

        return { horizontal: HORIZONTAL_INSET, vertical: VERTICAL_INSET };
    };

    const hasToken = (element, attribute, token) => (element.getAttribute(attribute) || '').split(' ').includes(token);

    const flatParent = node => {
        if (node.assignedSlot) {
            return node.assignedSlot;
        }

        if (node.parentElement) {
            return node.parentElement;
        }

        const parent = node.parentNode;
        return parent instanceof ShadowRoot ? parent.host : null;
    };

    const paddingBox = element => {
        const rect = element.getBoundingClientRect();
        return {
            left: rect.left + element.clientLeft,
            top: rect.top + element.clientTop,
            width: element.clientWidth || rect.width,
            height: element.clientHeight || rect.height
        };
    };

    const intersect = (a, b) => {
        const left = Math.max(a.left, b.left);
        const top = Math.max(a.top, b.top);
        const right = Math.min(a.left + a.width, b.left + b.width);
        const bottom = Math.min(a.top + a.height, b.top + b.height);
        return { left: left, top: top, width: Math.max(0, right - left), height: Math.max(0, bottom - top) };
    };

    const isShown = element => {
        if (!element.isConnected || element.getClientRects().length === 0) {
            return false;
        }

        return typeof element.checkVisibility !== 'function' || element.checkVisibility({ visibilityProperty: true });
    };

    const clipsContent = element => {
        const style = getComputedStyle(element);
        return style.overflowX !== 'visible' || style.overflowY !== 'visible';
    };

    const roundedRect = (hole, grow, fill) => {
        const x = hole.left - grow;
        const y = hole.top - grow;
        const width = Math.max(0, hole.width + grow * 2);
        const height = Math.max(0, hole.height + grow * 2);
        const radius = Math.max(0, hole.radius + grow);
        const rect = `<rect x="${x}" y="${y}" width="${width}" height="${height}" rx="${radius}" ry="${radius}" fill="${fill}"/>`;
        if (!hole.clip) {
            return rect;
        }

        const clip = hole.clip;
        return `<svg x="${clip.left}" y="${clip.top}" width="${clip.width}" height="${clip.height}" overflow="hidden">`
            + `<g transform="translate(${-clip.left} ${-clip.top})">${rect}</g></svg>`;
    };

    class Spotlight {
        constructor(host, id) {
            this.host = host;
            this.id = id;
            this.frame = 0;
            this.rendered = '';
            this.observedTargets = new Set();
            this.scrollers = new Set();
            this.positioned = false;

            const maskId = ++nextMaskId;
            this.dimMaskId = `consulo-spotlight-dim-${maskId}`;
            this.borderMaskId = `consulo-spotlight-border-${maskId}`;

            this.overlay = document.createElement('div');
            this.overlay.className = 'consulo-spotlight';
            this.overlay.style.display = 'none';

            this.svg = document.createElementNS(SVG_NAMESPACE, 'svg');
            this.overlay.appendChild(this.svg);

            this.schedule = () => {
                if (this.frame) {
                    return;
                }

                this.frame = requestAnimationFrame(() => {
                    this.frame = 0;
                    this.update();
                });
            };

            this.resizeObserver = new ResizeObserver(this.schedule);

            this.mutationObserver = new MutationObserver(records => {
                const foreign = records.some(record => {
                    if (this.overlay.contains(record.target)) {
                        return false;
                    }

                    if (record.type !== 'childList') {
                        return true;
                    }

                    const nodes = [...record.addedNodes, ...record.removedNodes];
                    return nodes.length === 0 || nodes.some(node => node !== this.overlay);
                });

                if (foreign) {
                    this.schedule();
                }
            });
        }

        connect() {
            if (getComputedStyle(this.host).position === 'static') {
                this.host.style.position = 'relative';
                this.positioned = true;
            }

            this.host.appendChild(this.overlay);

            this.resizeObserver.observe(this.host);
            this.mutationObserver.observe(this.host, {
                subtree: true,
                childList: true,
                characterData: true,
                attributes: true,
                attributeFilter: [TARGET_ATTRIBUTE, LIT_ATTRIBUTE, SHAPE_ATTRIBUTE, 'style', 'class', 'hidden', 'selected']
            });

            document.addEventListener('scroll', this.schedule, { capture: true, passive: true });
            window.addEventListener('resize', this.schedule, { passive: true });

            this.schedule();
        }

        disconnect() {
            if (this.frame) {
                cancelAnimationFrame(this.frame);
                this.frame = 0;
            }

            this.resizeObserver.disconnect();
            this.mutationObserver.disconnect();
            this.observedTargets.clear();
            this.listenToScrollers(new Set());

            document.removeEventListener('scroll', this.schedule, { capture: true });
            window.removeEventListener('resize', this.schedule);

            this.overlay.remove();

            if (this.positioned) {
                this.host.style.position = '';
                this.positioned = false;
            }
        }

        observeTargets(targets) {
            const next = new Set(targets);
            for (const target of this.observedTargets) {
                if (!next.has(target)) {
                    this.resizeObserver.unobserve(target);
                }
            }

            for (const target of next) {
                if (!this.observedTargets.has(target)) {
                    this.resizeObserver.observe(target);
                }
            }

            this.observedTargets = next;
        }

        listenToScrollers(scrollers) {
            for (const scroller of this.scrollers) {
                if (!scrollers.has(scroller)) {
                    scroller.removeEventListener('scroll', this.schedule);
                }
            }

            for (const scroller of scrollers) {
                if (!this.scrollers.has(scroller)) {
                    scroller.addEventListener('scroll', this.schedule, { passive: true });
                }
            }

            this.scrollers = scrollers;
        }

        update() {
            if (!this.host.isConnected) {
                return;
            }

            if (this.overlay.parentNode !== this.host) {
                this.host.appendChild(this.overlay);
            }

            if (!hasToken(this.host, LIT_ATTRIBUTE, this.id)) {
                this.observeTargets([]);
                this.listenToScrollers(new Set());
                this.overlay.style.display = 'none';
                this.rendered = '';
                return;
            }

            const box = paddingBox(this.host);
            this.place(box);

            const targets = [...this.host.querySelectorAll(`[${TARGET_ATTRIBUTE}~="${this.id}"]`)];
            this.observeTargets(targets);

            const scrollers = new Set();
            const holes = [];
            for (const target of targets) {
                const hole = this.holeOf(target, box, scrollers);
                if (hole) {
                    holes.push(hole);
                }
            }

            this.listenToScrollers(scrollers);
            this.render(box, holes);
        }

        place(box) {
            const style = this.overlay.style;
            style.display = '';
            style.width = `${box.width}px`;
            style.height = `${box.height}px`;

            const current = this.overlay.getBoundingClientRect();
            const left = (parseFloat(style.left) || 0) + box.left - current.left;
            const top = (parseFloat(style.top) || 0) + box.top - current.top;
            style.left = `${Math.round(left * 100) / 100}px`;
            style.top = `${Math.round(top * 100) / 100}px`;
        }

        holeOf(target, box, scrollers) {
            if (target === this.host || !isShown(target)) {
                return null;
            }

            const rect = target.getBoundingClientRect();
            const insets = insetsOf(target);
            const height = rect.height + insets.vertical * 2;
            const hole = {
                left: rect.left - box.left - insets.horizontal,
                top: rect.top - box.top - insets.vertical,
                width: rect.width + insets.horizontal * 2,
                height: height,
                radius: Math.min(height, MAX_ARC) / 2,
                clip: null
            };

            let clip = null;
            for (let node = flatParent(target); node && node !== this.host; node = flatParent(node)) {
                if (node instanceof Element && clipsContent(node)) {
                    scrollers.add(node);

                    const area = paddingBox(node);
                    const local = { left: area.left - box.left, top: area.top - box.top, width: area.width, height: area.height };
                    clip = clip ? intersect(clip, local) : local;
                }
            }

            hole.clip = clip;
            return hole;
        }

        render(box, holes) {
            const width = box.width;
            const height = box.height;
            const half = STROKE / 2;

            const markup = `<defs>`
                + `<mask id="${this.dimMaskId}" maskUnits="userSpaceOnUse" x="0" y="0" width="${width}" height="${height}">`
                + `<rect x="0" y="0" width="${width}" height="${height}" fill="white"/>`
                + holes.map(hole => roundedRect(hole, 0, 'black')).join('')
                + `</mask>`
                + `<mask id="${this.borderMaskId}" maskUnits="userSpaceOnUse" x="0" y="0" width="${width}" height="${height}">`
                + holes.map(hole => roundedRect(hole, half, 'white')).join('')
                + holes.map(hole => roundedRect(hole, -half, 'black')).join('')
                + `</mask>`
                + `</defs>`
                + `<rect class="consulo-spotlight-dim" x="0" y="0" width="${width}" height="${height}" mask="url(#${this.dimMaskId})"/>`
                + `<rect class="consulo-spotlight-border" x="0" y="0" width="${width}" height="${height}" mask="url(#${this.borderMaskId})"/>`;

            if (markup === this.rendered) {
                return;
            }

            this.rendered = markup;
            this.svg.setAttribute('width', `${width}`);
            this.svg.setAttribute('height', `${height}`);
            this.svg.setAttribute('viewBox', `0 0 ${width} ${height}`);
            this.svg.innerHTML = markup;
        }
    }

    const spotlightsOf = host => {
        if (!host.$consuloSpotlights) {
            host.$consuloSpotlights = new Map();
        }
        return host.$consuloSpotlights;
    };

    const attach = (host, id) => {
        const spotlights = spotlightsOf(host);
        const existing = spotlights.get(id);
        if (existing) {
            existing.schedule();
            return;
        }

        const spotlight = new Spotlight(host, id);
        spotlights.set(id, spotlight);
        spotlight.connect();
    };

    const detach = (host, id) => {
        const spotlights = spotlightsOf(host);
        const spotlight = spotlights.get(id);
        if (!spotlight) {
            return;
        }

        spotlights.delete(id);
        spotlight.disconnect();

        for (const other of spotlights.values()) {
            if (other.positioned || getComputedStyle(host).position !== 'static') {
                continue;
            }

            host.style.position = 'relative';
            other.positioned = true;
        }
    };

    window.consuloSpotlight = { attach: attach, detach: detach };
})();
