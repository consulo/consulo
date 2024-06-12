// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.xdebugger.impl.actions;

import consulo.execution.debug.attach.XAttachDebuggerProvider;
import consulo.execution.debug.attach.XAttachHostProvider;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.platform.base.icon.PlatformIconGroup;

public class AttachToProcessAction extends AttachToProcessActionBase {
  public AttachToProcessAction() {
    super(
      XDebuggerLocalize.xdebuggerAttachAction(),
      XDebuggerLocalize.xdebuggerAttachActionDescription(),
      PlatformIconGroup.debuggerAttachtoprocess(),
      () -> XAttachDebuggerProvider.getAttachDebuggerProviders(),
      () -> XAttachHostProvider.EP.getExtensionList(),
      XDebuggerLocalize.xdebuggerAttachPopupSelectdebuggerTitle().get()
    );
  }
}
