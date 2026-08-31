/*-
 * #%L
 * Carousel Addon
 * %%
 * Copyright (C) 2018 - 2024 Flowing Code
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
package consulo.web.ui.impl.internal.vaadin.carousel;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;

/**
 * Vendored from <a href="https://github.com/FlowingCode/CarouselAddon">FlowingCode CarouselAddon</a>.
 */
@SuppressWarnings("serial")
@Tag("paper-slide")
public class Slide extends Component implements HasComponents {

    public Slide(Component... components) {
        this.add(components);
    }
}
