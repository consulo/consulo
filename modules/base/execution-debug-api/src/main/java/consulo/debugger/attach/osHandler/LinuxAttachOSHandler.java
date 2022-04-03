// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.debugger.attach.osHandler;

import consulo.debugger.attach.EnvironmentAwareHost;
import javax.annotation.Nonnull;

class LinuxAttachOSHandler extends UnixAttachOSHandler {
  LinuxAttachOSHandler(@Nonnull EnvironmentAwareHost host) {
    super(host, AttachOSHandler.OSType.LINUX);
  }
}
