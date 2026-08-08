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
 * Reports the keys the platform needs for the modifier double click - a double shift opens search everywhere -
 * as one custom event per key, which the server turns into the shared state machine of the platform.
 *
 * The machine measures real intervals, so nothing here may be debounced or batched. What keeps that from
 * meaning an round trip per character is the gate: a plain key is only worth reporting while a modifier
 * sequence is already in progress, and there is no sequence at all while the user is only typing.
 */
(() => {
    const MODIFIERS = {
        AltLeft: 'ALT',
        AltRight: 'ALT',
        ControlLeft: 'CTRL',
        ControlRight: 'CTRL',
        MetaLeft: 'META',
        MetaRight: 'META',
        ShiftLeft: 'SHIFT',
        ShiftRight: 'SHIFT'
    };

    /* the platform reads a key by its awt code, and only these few ever reach it from here */
    const KEY_CODES = {
        ALT: 0x12,
        CTRL: 0x11,
        META: 0x9D,
        SHIFT: 0x10,
        Escape: 0x1B,
        Tab: 0x09
    };

    /* past this a modifier press is too old to be the first half of anything */
    const SEQUENCE_MS = 600;

    /*
     * The combinations the keymap of the platform owns, as sent by the server. A key of the ide must not also
     * reach the browser - ctrl s belongs to the ide and would otherwise offer to save the page - so only the
     * ones on this list are taken, and everything else is left alone.
     */
    const shortcutsOf = element => element.$consuloShortcutSet || (element.$consuloShortcutSet = new Set());

    /*
     * The combinations which must reach the browser rather than being taken here. A page is handed the system
     * clipboard only inside the event the browser itself raises - cancelling the keydown of ctrl v cancels the
     * paste event with it, and then nothing on either side has the payload. So these keys are left alone, and
     * the shortcut of the platform is dispatched from the clipboard event instead, carrying what it holds.
     */
    const clipboardOf = element => element.$consuloClipboardSet || (element.$consuloClipboardSet = new Map());

    const comboForKind = (element, kind) => {
        for (const [combo, value] of clipboardOf(element)) {
            if (value === kind) {
                return combo;
            }
        }
        return null;
    };

    const setClipboardShortcuts = (element, spec) => {
        const map = new Map();
        (spec ? spec.split('\n') : []).forEach(line => {
            const at = line.lastIndexOf('=');
            if (at > 0) {
                map.set(line.substring(0, at), line.substring(at + 1));
            }
        });
        element.$consuloClipboardSet = map;
    };

    const comboOf = event => {
        const parts = [];
        if (event.altKey) {
            parts.push('ALT');
        }
        if (event.ctrlKey) {
            parts.push('CTRL');
        }
        if (event.metaKey) {
            parts.push('META');
        }
        if (event.shiftKey) {
            parts.push('SHIFT');
        }
        parts.push(event.keyCode);
        return parts.join('+');
    };

    const setShortcuts = (element, combos) => {
        element.$consuloShortcutSet = new Set(combos ? combos.split('\n') : []);
    };

    const install = element => {
        if (element.$consuloShortcuts) {
            return;
        }
        element.$consuloShortcuts = true;

        let sequenceUntil = 0;

        const modifiersOf = event => {
            const modifiers = [];
            if (event.altKey) {
                modifiers.push('ALT');
            }
            if (event.ctrlKey) {
                modifiers.push('CTRL');
            }
            if (event.metaKey) {
                modifiers.push('META');
            }
            if (event.shiftKey) {
                modifiers.push('SHIFT');
            }
            return modifiers;
        };

        const report = (event, keyCode, pressed) => {
            element.dispatchEvent(new CustomEvent('consulo-key', {
                detail: {
                    keyCode: keyCode,
                    modifiers: modifiersOf(event).join(','),
                    pressed: pressed,
                    // the double click is measured in the gaps between the keys, and every one of them reaches
                    // the server as a request of its own - by the time they arrive the gaps are the network's,
                    // not the user's, so the only clock that means anything is the one here
                    time: Date.now()
                }
            }));
        };

        const onKey = (event, pressed) => {
            // a terminal talks to a process which owns the keyboard - enter, tab and the control keys are input
            // for that process, so the keymap of the ide must not take them first
            const target = event.target;
            if (target && target.closest && target.closest('[consulo-keyboard-capture]')) {
                return;
            }

            const modifier = MODIFIERS[event.code];

            // left for the browser on purpose - see clipboardOf. the stroke still reaches the platform, but from
            // the clipboard event which the untouched default action raises, not from here
            if (pressed && !modifier && clipboardOf(element).has(comboOf(event))) {
                return;
            }

            if (pressed && !modifier && shortcutsOf(element).has(comboOf(event))) {
                event.preventDefault();
                event.stopPropagation();

                element.dispatchEvent(new CustomEvent('consulo-shortcut', {detail: {combo: comboOf(event)}}));
                return;
            }

            if (modifier) {
                // a repeat is the key being held, not pressed again - the machine would read it as a second click
                if (!event.repeat) {
                    sequenceUntil = Date.now() + SEQUENCE_MS;
                    report(event, KEY_CODES[modifier], pressed);
                }
                return;
            }

            // outside a sequence nothing here is of interest, and typing must not reach the server
            if (Date.now() > sequenceUntil) {
                return;
            }

            report(event, KEY_CODES[event.code] || 0, pressed);
        };

        // on the document rather than the frame: a listener only ever sees events targeted inside its own
        // element, and with nothing in the frame focused the browser targets the body - which is why a double
        // shift did nothing until something had been clicked. capture, so a key the platform owns is seen
        // before the editor or a text field acts on it
        document.addEventListener('keydown', event => onKey(event, true), true);
        document.addEventListener('keyup', event => onKey(event, false), true);

        /*
         * Capture, so the editor in the page does not also act on it - the orion client div is contenteditable
         * and would insert the text itself, on top of what the paste action of the platform inserts.
         */
        document.addEventListener('paste', event => {
            const combo = comboForKind(element, 'paste');
            if (!combo || !event.clipboardData) {
                return;
            }

            event.preventDefault();
            event.stopPropagation();

            element.dispatchEvent(new CustomEvent('consulo-shortcut', {
                detail: {
                    combo: combo,
                    // the payload travels with the stroke rather than being asked for afterwards - by the time a
                    // request of the server reached the page the activation of this gesture would be spent
                    pasteText: event.clipboardData.getData('text/plain') || '',
                    pasteHtml: event.clipboardData.getData('text/html') || ''
                }
            }));
        }, true);
    };

    window.consuloShortcuts = { install: install, setShortcuts: setShortcuts, setClipboardShortcuts: setClipboardShortcuts };
})();
