/*
 * Copyright 2013 Consulo.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.consulo.java.roots;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.newvfs.persistent.FSRecords;
import com.intellij.pom.java.LanguageLevel;
import org.consulo.java.platform.module.extension.JavaModuleExtensionImpl;
import org.consulo.module.extension.ModuleExtension;
import org.consulo.module.extension.ModuleExtensionChangeListener;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 23:33/10.09.13
 */
public class LanguageVersionKeyDropper {
  public LanguageVersionKeyDropper(final Project project) {
    if(ApplicationManager.getApplication().isInternal()) {
      return;
    }
    project.getMessageBus().connect().subscribe(ModuleExtension.CHANGE_TOPIC, new ModuleExtensionChangeListener() {
      @Override
      public void extensionChanged(@NotNull ModuleExtension<?> oldExtension, @NotNull ModuleExtension<?> newExtension) {
        if(!(oldExtension instanceof JavaModuleExtensionImpl)) {
          return;
        }

        LanguageLevel oldLevel = ((JavaModuleExtensionImpl)oldExtension).getLanguageLevel();
        LanguageLevel newLevel = ((JavaModuleExtensionImpl)newExtension).getLanguageLevel();

        if(oldLevel == newLevel) {
          return;
        }
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            showWarning(project);
          }
        }, ModalityState.NON_MODAL);
      }
    });
  }

  private static void showWarning(Project project) {
    final Application app = ApplicationManager.getApplication();
    final boolean mac = Messages.canShowMacSheetPanel();
    String[] options = new String[3];
    options[0] = app.isRestartCapable() ? "Invalidate and Restart" : "Invalidate and Exit";
    options[1] = mac ? "Cancel" : "Invalidate";
    options[2] = mac ? "Invalidate" : "Cancel";

    int result = Messages.showYesNoCancelDialog(project,
                                                "The caches will be invalidated and rebuilt on the next startup.\n" +
                                                "WARNING: Local History will be also cleared.\n\n" +
                                                "Would you like to continue?\n\n",
                                                "Invalidate Caches",
                                                options[0], options[1], options[2],
                                                Messages.getWarningIcon());

    if (result == -1 || result == (mac ? 1 : 2)) {
      return;
    }
    FSRecords.invalidateCaches();
    if (result == 0) app.restart();
  }
}
