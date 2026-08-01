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
 * vaadin-grid-tree-toggle flips its own expanded state for a click anywhere on it, and the label is inside it -
 * so clicking a name opened the node, and refusing the expand on the server alone left the chevron turned
 * while nothing opened. The ide trees only open a node from its handle; a click on the name selects it.
 *
 * The mixin binds its handler as `(e) => this._onClick(e)`, which resolves the method at call time, so one
 * patch of the prototype covers every toggle ever created.
 */
(() => {
    const patch = () => {
        const toggle = customElements.get('vaadin-grid-tree-toggle');
        if (!toggle || toggle.prototype.$consuloChevronOnly) {
            return !!toggle;
        }

        toggle.prototype.$consuloChevronOnly = true;
        toggle.prototype._onClick = function (event) {
            if (this.leaf) {
                return;
            }

            const onChevron = event.composedPath()
                .some(node => node.getAttribute && node.getAttribute('part') === 'toggle');
            if (!onChevron) {
                return;
            }

            event.preventDefault();
            this.expanded = !this.expanded;
        };
        return true;
    };

    if (!patch()) {
        customElements.whenDefined('vaadin-grid-tree-toggle').then(patch);
    }
})();
