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
    if (window.consuloSpeedSearch) {
        return;
    }

    const ENABLED_ATTRIBUTE = 'consulo-speed-search';
    const EVENT = 'consulo-speed-search';
    const OWN_KEYS_ATTRIBUTE = 'consulo-own-keys';
    const OWN_KEYS = ['27', '8', '38', '40', '36', '35'];
    const PUNCTUATION = '*_-"\'/.#$>:,;?!@%^&';
    const IDLE_MS = 5000;
    const STYLE_ID = 'consulo-speed-search-style';

    const NAVIGATION = {
        ArrowUp: 'previous',
        ArrowDown: 'next',
        Home: 'first',
        End: 'last'
    };

    const CLOSING = new Set(['Enter', 'Tab', 'PageUp', 'PageDown', 'ArrowLeft', 'ArrowRight']);

    const ensureStyle = () => {
        if (document.getElementById(STYLE_ID)) {
            return;
        }

        const link = document.createElement('link');
        link.id = STYLE_ID;
        link.rel = 'stylesheet';
        link.href = '/speedSearch/speedSearch.css';
        document.head.appendChild(link);
    };

    const isEditable = node => !!node
        && (node.tagName === 'INPUT' || node.tagName === 'TEXTAREA' || node.isContentEditable === true);

    const startsSearch = key => /^[\p{L}\p{N}]$/u.test(key) || PUNCTUATION.includes(key);

    const install = host => {
        if (host.$consuloSpeedSearch) {
            return;
        }

        ensureStyle();

        let text = null;
        let hint = null;
        let label = null;
        let timer = 0;
        let savedOwnKeys = null;

        const send = action => {
            host.dispatchEvent(new CustomEvent(EVENT, {
                detail: {
                    action: action,
                    text: text === null ? '' : text
                }
            }));
        };

        const place = () => {
            const bounds = host.getBoundingClientRect();
            const top = bounds.top - hint.offsetHeight;
            hint.style.left = Math.max(0, bounds.left) + 'px';
            hint.style.top = (top < 0 ? Math.max(0, bounds.top) : top) + 'px';
        };

        const show = () => {
            if (!hint) {
                hint = document.createElement('div');
                hint.className = 'consulo-speed-search-hint';
                label = document.createElement('span');
                label.className = 'consulo-speed-search-hint-text';
                hint.appendChild(label);
                if (typeof hint.showPopover === 'function') {
                    hint.popover = 'manual';
                }
                document.body.appendChild(hint);
                if (hint.popover) {
                    hint.showPopover();
                }
            }

            label.textContent = text;
            place();
        };

        const close = notify => {
            if (text === null) {
                return;
            }

            text = null;
            clearTimeout(timer);

            if (savedOwnKeys === null) {
                host.removeAttribute(OWN_KEYS_ATTRIBUTE);
            }
            else {
                host.setAttribute(OWN_KEYS_ATTRIBUTE, savedOwnKeys);
            }
            savedOwnKeys = null;

            if (hint) {
                hint.remove();
                hint = null;
                label = null;
            }

            if (notify) {
                send('close');
            }
        };

        const update = action => {
            if (!host.isConnected) {
                close(true);
                return;
            }

            show();

            clearTimeout(timer);
            timer = setTimeout(() => close(true), IDLE_MS);

            send(action);
        };

        const open = initial => {
            text = initial;

            savedOwnKeys = host.getAttribute(OWN_KEYS_ATTRIBUTE);
            const own = savedOwnKeys ? savedOwnKeys.split('\n') : [];
            host.setAttribute(OWN_KEYS_ATTRIBUTE, own.concat(OWN_KEYS.filter(key => !own.includes(key))).join('\n'));

            update('type');
        };

        const actionOf = event => {
            if (!host.hasAttribute(ENABLED_ATTRIBUTE) || event.isComposing || event.altKey || event.ctrlKey || event.metaKey) {
                return null;
            }

            if (isEditable(event.composedPath()[0])) {
                return null;
            }

            const key = event.key;
            if (text === null) {
                return key.length === 1 && startsSearch(key) ? 'open' : null;
            }

            if (key === 'Escape') {
                return 'escape';
            }

            if (key === 'Backspace') {
                return 'backspace';
            }

            if (NAVIGATION[key]) {
                return event.shiftKey ? null : NAVIGATION[key];
            }

            if (CLOSING.has(key)) {
                return 'close';
            }

            return key.length === 1 ? 'append' : null;
        };

        const consumes = event => {
            const action = actionOf(event);
            return action !== null && action !== 'close';
        };

        host.addEventListener('keydown', event => {
            const action = actionOf(event);
            if (action === null) {
                return;
            }

            if (action === 'close') {
                close(true);
                return;
            }

            event.preventDefault();
            event.stopPropagation();

            if (action === 'open') {
                open(event.key);
            }
            else if (action === 'escape') {
                close(true);
            }
            else if (action === 'backspace') {
                text = text.slice(0, -1);
                update('type');
            }
            else if (action === 'append') {
                text += event.key;
                update('type');
            }
            else {
                update(action);
            }
        }, true);

        host.addEventListener('focusout', event => {
            const next = event.relatedTarget;
            if (next && (next === host || host.contains(next))) {
                return;
            }
            close(true);
        });

        host.$consuloSpeedSearch = {
            found: (forText, found) => {
                if (hint && text === forText) {
                    hint.toggleAttribute('mismatch', !found);
                }
            },
            close: () => close(false),
            consumes: consumes
        };
    };

    window.consuloSpeedSearch = { install: install };
})();
