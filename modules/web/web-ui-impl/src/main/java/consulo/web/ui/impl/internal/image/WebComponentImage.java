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
package consulo.web.ui.impl.internal.image;

import com.vaadin.flow.component.Component;
import consulo.ui.image.Image;

/**
 * An image which builds its own element instead of going through an url. A component carrying
 * {@link #FILL_CELL_CLASS} spans the full height of the list, tree or table cell it is shown in.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public interface WebComponentImage extends Image {
    String FILL_CELL_CLASS = "web-image-fill-cell";

    Component createComponent();
}
