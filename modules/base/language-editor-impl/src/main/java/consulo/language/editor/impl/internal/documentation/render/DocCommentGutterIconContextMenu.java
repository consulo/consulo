// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.documentation.render;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.ui.ex.action.DefaultActionGroup;

@ActionImpl(id = "DocCommentGutterIconContextMenu", children = {
    @ActionRef(type = ToggleRenderAllDocs.class)
})
public class DocCommentGutterIconContextMenu extends DefaultActionGroup {
}
