// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.execution.debug.attach.XAttachDebuggerProvider;
import consulo.execution.debug.attach.XAttachHostProvider;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.execution.debug.localize.XDebuggerLocalize;
import jakarta.inject.Inject;

@ActionImpl(id = "XDebugger.AttachToProcess")
public class AttachToProcessAction extends AttachToProcessActionBase {
    @Inject
    public AttachToProcessAction(Application application) {
        super(
            XDebuggerLocalize.xdebuggerAttachAction(),
            XDebuggerLocalize.xdebuggerAttachActionDescription(),
            ExecutionDebugIconGroup.actionAttachtoprocess(),
            XAttachDebuggerProvider::getAttachDebuggerProviders,
            () -> application.getExtensionList(XAttachHostProvider.class),
            XDebuggerLocalize.xdebuggerAttachPopupSelectdebuggerTitle().get()
        );
    }
}
