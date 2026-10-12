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

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import consulo.ui.clipboard.DataTransfer;
import org.jspecify.annotations.Nullable;

/**
 * The payload of the drag running in a browser tab. The browser only carries the marker type, the payload stays on
 * the server, so a drop target of another component reads it here.
 *
 * @author VISTALL
 * @since 2026-10-12
 */
public final class WebDragSession {
    public static final String DRAG_TYPE = "application/x-consulo-drag";

    private record Active(Object source, DataTransfer transfer) {
    }

    private WebDragSession() {
    }

    public static void start(UI ui, Object source, @Nullable DataTransfer transfer) {
        ComponentUtil.setData(ui, Active.class, transfer == null ? null : new Active(source, transfer));
    }

    public static void end(UI ui, Object source) {
        Active active = ComponentUtil.getData(ui, Active.class);
        if (active != null && active.source() == source) {
            ComponentUtil.setData(ui, Active.class, null);
        }
    }

    public static @Nullable Object getSource(UI ui) {
        Active active = ComponentUtil.getData(ui, Active.class);
        return active == null ? null : active.source();
    }

    public static DataTransfer getTransfer(UI ui) {
        Active active = ComponentUtil.getData(ui, Active.class);
        return active == null ? DataTransfer.EMPTY : active.transfer();
    }
}
