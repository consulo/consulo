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
package consulo.it.internal;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ServiceImpl;
import consulo.ui.ex.ExpandableItemsHandler;
import consulo.ui.ex.TableCell;
import consulo.ui.ex.awt.ExpandableItemsHandlerFactory;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 2026-09-23
 */
@ServiceImpl
@Singleton
@Deprecated
@DeprecationInfo("AWT dep - must removed")
public class HeadlessExpandableItemsHandlerFactory extends ExpandableItemsHandlerFactory {
    private <T> ExpandableItemsHandler<T> stub() {
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
                return null;
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
