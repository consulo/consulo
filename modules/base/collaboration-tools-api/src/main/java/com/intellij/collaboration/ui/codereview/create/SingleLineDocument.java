// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.create;

import consulo.util.lang.StringUtil;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

@ApiStatus.Internal
class SingleLineDocument extends PlainDocument {
    @Override
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        // filter new lines
        String withoutNewLines = StringUtil.replace(str, "\n", "");
        super.insertString(offs, withoutNewLines, a);
    }
}
