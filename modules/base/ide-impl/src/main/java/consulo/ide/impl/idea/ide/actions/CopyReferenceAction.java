// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.ide.action.CopyReferenceActionBase;
import consulo.platform.base.localize.ActionLocalize;

/**
 * @author Alexey
 */
@ActionImpl(
    id = "CopyReference",
    parents = {
        @ActionParentRef(
            value = @ActionRef(type = CopyReferencePopup.class),
            anchor = ActionRefAnchor.AFTER,
            relatedToAction = @ActionRef(type = CopyExternalReferenceGroup.class)
        ),
        @ActionParentRef(
            value = @ActionRef(type = EditorTabPopupMenuGroup.class),
            anchor = ActionRefAnchor.AFTER,
            relatedToAction = @ActionRef(type = CopyPathsAction.class)
        )
    }
)
public class CopyReferenceAction extends CopyReferenceActionBase {
    public CopyReferenceAction() {
        super(ActionLocalize.actionCopyreferenceText(), ActionLocalize.actionCopyreferenceDescription(), null);
    }
}
