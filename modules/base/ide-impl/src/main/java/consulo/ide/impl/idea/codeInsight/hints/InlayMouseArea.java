// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.language.editor.inlay.InlayActionData;

import java.util.ArrayList;
import java.util.List;

public class InlayMouseArea {
    private final InlayActionData actionData;
    private final List<InlayPresentationEntry> entries = new ArrayList<>();

    public InlayMouseArea(InlayActionData actionData) {
        this.actionData = actionData;
    }

    public InlayActionData getActionData() {
        return actionData;
    }

    public List<InlayPresentationEntry> getEntries() {
        return entries;
    }
}