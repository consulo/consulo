/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.psi.impl.file.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.component.messagebus.MessageBusConnection;
import consulo.document.event.FileDocumentManagerListener;
import consulo.language.file.event.FileTypeEvent;
import consulo.language.file.event.FileTypeListener;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;

import jakarta.annotation.Nonnull;

@ExtensionImpl(order = "first")
public class PsiVFSListenerStartUpActivity implements PostStartupActivity, DumbAware {

  @Override
  public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    PsiVFSListener psiVFSListener = project.getInstance(PsiVFSListener.class);

    MessageBusConnection connection = project.getMessageBus().connect();
    connection.subscribe(ModuleRootListener.class, psiVFSListener.new MyModuleRootListener());
    connection.subscribe(FileTypeListener.class, new FileTypeListener() {
      @Override
      public void fileTypesChanged(@Nonnull FileTypeEvent e) {
        psiVFSListener.myFileManager.processFileTypesChangedAsync(e.getRemovedFileType() != null);
      }
    });
    connection.subscribe(FileDocumentManagerListener.class, psiVFSListener.new MyFileDocumentManagerAdapter());

    PsiVFSListener.installGlobalListener();
  }
}
