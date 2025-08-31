// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.impl.internal;

import consulo.application.ApplicationManager;
import consulo.project.Project;
import consulo.ui.ex.awt.util.Alarm;
import consulo.virtualFileSystem.LocalFileSystem;
import jakarta.annotation.Nonnull;

public class FileWatchRequestsManager {
  private final FileWatchRequestModifier myModifier;
  private final Alarm myAlarm;

  public FileWatchRequestsManager(@Nonnull Project project, @Nonnull NewMappings newMappings) {
    this(project, newMappings, LocalFileSystem.getInstance());
  }

  public FileWatchRequestsManager(@Nonnull Project project, @Nonnull NewMappings newMappings, @Nonnull LocalFileSystem localFileSystem) {
    myModifier = new FileWatchRequestModifier(project, newMappings, localFileSystem);
    myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, newMappings);
  }

  public void ping() {
    if (myAlarm.isDisposed() || ApplicationManager.getApplication().isUnitTestMode()) {
      myModifier.run();
    }
    else {
      myAlarm.cancelAllRequests();
      myAlarm.addRequest(myModifier, 0);
    }
  }
}
