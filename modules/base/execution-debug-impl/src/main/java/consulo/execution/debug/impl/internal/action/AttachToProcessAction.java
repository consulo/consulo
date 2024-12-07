// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.impl.internal.action;

import consulo.execution.debug.attach.XAttachDebuggerProvider;
import consulo.execution.debug.attach.XAttachHostProvider;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.execution.debug.localize.XDebuggerLocalize;

public class AttachToProcessAction extends AttachToProcessActionBase {
    public AttachToProcessAction() {
        super(
            XDebuggerLocalize.xdebuggerAttachAction(),
            XDebuggerLocalize.xdebuggerAttachActionDescription(),
            ExecutionDebugIconGroup.actionAttachtoprocess(),
            () -> XAttachDebuggerProvider.getAttachDebuggerProviders(),
            () -> XAttachHostProvider.EP.getExtensionList(),
            XDebuggerLocalize.xdebuggerAttachPopupSelectdebuggerTitle().get()
        );
    }
}
