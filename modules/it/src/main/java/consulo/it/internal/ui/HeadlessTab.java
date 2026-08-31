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
package consulo.it.internal.ui;

import consulo.ui.Component;
import consulo.ui.Tab;
import consulo.ui.TextItemPresentation;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * Dummy-but-creatable headless {@link Tab}.
 *
 * @author VISTALL
 */
public class HeadlessTab implements Tab {
    @Override
    public void setRenderer(BiConsumer<Tab, TextItemPresentation> renderer) {
    }

    @Override
    public void setCloseHandler(@Nullable BiConsumer<Tab, Component> closeHandler) {
    }

    @Override
    public void update() {
    }

    @Override
    public void select() {
    }
}
