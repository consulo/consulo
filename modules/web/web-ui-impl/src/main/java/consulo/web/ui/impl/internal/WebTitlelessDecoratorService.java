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

import consulo.annotation.component.ServiceImpl;
import consulo.ui.ex.TitlelessDecorator;
import consulo.ui.ex.TitlelessDecoratorService;
import jakarta.inject.Singleton;

/**
 * The browser draws no window decoration of its own to replace, so no window of the web frontend is titleless.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
@Singleton
@ServiceImpl
public class WebTitlelessDecoratorService implements TitlelessDecoratorService {
    @Override
    public TitlelessDecorator of(Object pane, String windowId) {
        return TitlelessDecorator.NOTHING;
    }
}
