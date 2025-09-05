/*
 * Copyright 2013-2025 consulo.io
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
package consulo.desktop.awt.uiOld.content;

import consulo.annotation.component.ServiceImpl;
import consulo.ui.ex.awt.dnd.ContentWithDnDFactory;
import consulo.ui.ex.awt.dnd.DnDEvent;
import consulo.ui.ex.content.Content;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2025-09-04
 */
@ServiceImpl
@Singleton
public class ContentWithDnDFactoryImpl implements ContentWithDnDFactory {
    @Nonnull
    @Override
    public Content createContentWithDnd(JComponent component,
                                        String displayName,
                                        boolean isLockable,
                                        Consumer<DnDEvent> dropHandler,
                                        Predicate<DnDEvent> updateHandler) {
        return new DnDTargetContentAdapter(component, displayName, isLockable, dropHandler, updateHandler);
    }
}
