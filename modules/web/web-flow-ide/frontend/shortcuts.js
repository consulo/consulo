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
            const modifier = MODIFIERS[event.code];

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
    };

    window.consuloShortcuts = { install: install };
})();
