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
 * Reports the moment an element is first shown, as one `consulo-shown` event. The server side has no way of
 * knowing it: a tab that is not selected keeps its content attached and only hides it, so being attached says
 * nothing about being on screen.
 *
 * Shown means laid out, the way awt reads `Component#isShowing` - not scrolled into view, which is why this
 * watches the box of the element instead of using an IntersectionObserver. A hidden element has no box, and
 * the observer fires again once an ancestor gives it one.
 */
(() => {
    const shown = element => element.isConnected && element.getClientRects().length > 0;

    const once = element => {
        if (element.$consuloShowNotifier) {
            return;
        }
        element.$consuloShowNotifier = true;

        if (shown(element)) {
            element.dispatchEvent(new CustomEvent('consulo-shown'));
            return;
        }

        const observer = new ResizeObserver(() => {
            if (!shown(element)) {
                return;
            }

            observer.disconnect();
            element.dispatchEvent(new CustomEvent('consulo-shown'));
        });
        observer.observe(element);
    };

    window.consuloShowNotifier = { once: once };
})();
