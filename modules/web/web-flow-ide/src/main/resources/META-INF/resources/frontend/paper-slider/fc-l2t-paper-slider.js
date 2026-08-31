/*-
 * #%L
 * Carousel Addon
 * %%
 * Copyright (C) 2024 Flowing Code
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

import './l2t-paper-slider.js';

import {ThemableMixin} from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';

class FCL2TPaperSlider extends ThemableMixin(customElements.get('l2t-paper-slider')) {
    static get is() { return 'fc-l2t-paper-slider'; }

    static get template() {
      return html`${super.template}`;
    }  
};

customElements.define(FCL2TPaperSlider.is, FCL2TPaperSlider);