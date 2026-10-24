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
package consulo.web.ui.impl.internal;

import consulo.ui.Length;
import consulo.ui.Space;
import consulo.ui.layout.TileLayout;
import consulo.web.ui.impl.internal.vaadin.WebLength;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public class WebTileLayoutImpl extends WebTileLayoutBase implements TileLayout {
    public WebTileLayoutImpl(Length minTileWidth, Space gap) {
        super("web-tile-layout", gap);
        toVaadinComponent().getStyle().set("grid-template-columns", "repeat(auto-fit, minmax(min(" + WebLength.toCss(minTileWidth) + ", 100%), 1fr))");
    }
}
