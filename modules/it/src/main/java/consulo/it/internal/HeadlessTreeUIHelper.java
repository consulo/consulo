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
import consulo.ui.ex.awt.tree.TreeUIHelper;
import jakarta.inject.Singleton;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-09-23
 */
@ServiceImpl
@Singleton
@Deprecated
@DeprecationInfo("AWT dep - must removed")
public class HeadlessTreeUIHelper extends TreeUIHelper {
    @Override
    public void installToolTipHandler(JTree tree) {
        
    }

    @Override
    public void installToolTipHandler(JTable table) {

    }

    @Override
    public void installToolTipHandler(JList list) {

    }

    @Override
    public void installEditSourceOnDoubleClick(JTree tree) {

    }

    @Override
    public void installTreeSpeedSearch(JTree tree) {

    }

    @Override
    public void installListSpeedSearch(JList list) {

    }

    @Override
    public void installTreeSpeedSearch(JTree tree, Function<TreePath, String> converter, boolean canExpand) {

    }

    @Override
    public void installListSpeedSearch(JList list, Function<Object, String> converter) {

    }

    @Override
    public void installEditSourceOnEnterKeyHandler(JTree tree) {

    }

    @Override
    public void installSmartExpander(JTree tree) {

    }

    @Override
    public void installSelectionSaver(JTree tree) {

    }
}
