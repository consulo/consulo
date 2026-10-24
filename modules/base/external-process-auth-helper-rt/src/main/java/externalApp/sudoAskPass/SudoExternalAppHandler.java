// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package externalApp.sudoAskPass;

import externalApp.ExternalAppHandler;

public interface SudoExternalAppHandler extends ExternalAppHandler {
    String IJ_SUDO_ASK_PASS_HANDLER_ENV = "INTELLIJ_SUDO_ASKPASS_HANDLER";
    String IJ_SUDO_ASK_PASS_PORT_ENV = "INTELLIJ_SUDO_ASKPASS_PORT";
    String ENTRY_POINT_NAME = "sudoAskPass";

    String handleInput(String description);
}
