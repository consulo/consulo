/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.plugins.whatsNew;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import consulo.ide.updateSettings.impl.UpdateHistory;
import consulo.platform.base.localize.IdeLocalize;
import consulo.project.startup.StartupActivity;
import consulo.ui.UIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author VISTALL
 * @since 21/11/2021
 */
public class WhatsNewStartupActivity implements StartupActivity.DumbAware {
  private final Application myApplication;
  private final Provider<UpdateHistory> myUpdateHistoryProvider;

  private final AtomicBoolean myAlreadyShow = new AtomicBoolean();

  @Inject
  public WhatsNewStartupActivity(Application application, Provider<UpdateHistory> updateHistoryProvider) {
    myApplication = application;
    myUpdateHistoryProvider = updateHistoryProvider;
  }

  @Override
  public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    if(myAlreadyShow.compareAndSet(false, true)) {
      UpdateHistory updateHistory = myUpdateHistoryProvider.get();

      if (updateHistory.isShowChangeLog()) {
        updateHistory.setShowChangeLog(false);

        uiAccess.give(() -> FileEditorManager.getInstance(project).openFile(new WhatsNewVirtualFile(IdeLocalize.whatsnewActionCustomText(myApplication.getName())), true));
      }
    }
  }
}
