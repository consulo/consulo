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
import consulo.ui.ex.ExpandableItemsHandler;
import consulo.ui.ex.TableCell;
import consulo.ui.ex.awt.ExpandableItemsHandlerFactory;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

/**
 * Swing lists, trees and tables install an expand tip handler as they are constructed. This frontend never draws
 * them, so the handler does nothing - but it has to be bound, or constructing any of them fails, and with it every
 * view which still builds one (the frames of a debug session, for one).
 *
 * @author VISTALL
 * @since 2026-09-24
 */
@ServiceImpl
@Singleton
public class WebStubExpandableItemsHandlerFactory extends ExpandableItemsHandlerFactory {
    private static <T> ExpandableItemsHandler<T> stub() {
        return new ExpandableItemsHandler<>() {
            @Override
            public void setEnabled(boolean enabled) {
            }

            @Override
            public boolean isEnabled() {
                return false;
            }

            @Override
            public Collection<T> getExpandedItems() {
                return List.of();
            }
        };
    }

    @Override
    protected ExpandableItemsHandler<Integer> doInstall(JList list) {
        return stub();
    }

    @Override
    protected ExpandableItemsHandler<Integer> doInstall(JTree tree) {
        return stub();
    }

    @Override
    protected ExpandableItemsHandler<TableCell> doInstall(JTable table) {
        return stub();
    }
}
