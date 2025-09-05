/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ui.ex.awt.util;

import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFilePresentation;
import jakarta.annotation.Nonnull;

import javax.swing.*;

public class FileListRenderer extends ColoredListCellRenderer {
    @Override
    protected void customizeCellRenderer(@Nonnull JList list, Object value, int index, boolean selected, boolean hasFocus) {
        // paint selection only as a focus rectangle
        mySelected = false;
        setBackground(null);
        VirtualFile vf = (VirtualFile) value;
        setIcon(VirtualFilePresentation.getIcon(vf));
        append(vf.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        VirtualFile parent = vf.getParent();
        if (parent != null) {
            append(" (" + FileUtil.toSystemDependentName(parent.getPath()) + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
        }
    }
}
