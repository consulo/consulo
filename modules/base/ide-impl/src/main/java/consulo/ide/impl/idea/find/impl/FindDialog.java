// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.find.impl;

import consulo.find.FindSearchContext;

import javax.swing.*;

public class FindDialog {
    /**
     * @deprecated use {@link FindInProjectUtil#initFileFilter(JComboBox, JCheckBox)}
     */
    @Deprecated
    public static void initFileFilter(JComboBox<? super String> fileFilter, JCheckBox useFileFilter) {
        FindInProjectUtil.initFileFilter(fileFilter, useFileFilter);
    }

    /**
     * @deprecated use {@link FindSearchContext#getName()}
     */
    @Deprecated
    public static String getPresentableName(FindSearchContext searchContext) {
        return searchContext.getName().get();
    }
}

