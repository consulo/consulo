/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.impl.util;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.component.messagebus.MessageBus;
import consulo.language.impl.DebugUtil;
import consulo.language.psi.PsiModificationTrackerListener;
import consulo.logging.Logger;
import consulo.virtualFileSystem.event.BulkFileListener;

import java.util.HashSet;
import java.util.Set;

/**
 * @author peter
 */
public class NoAccessDuringPsiEvents {
  private static final Logger LOG = Logger.getInstance(NoAccessDuringPsiEvents.class);
  private static final Set<String> ourReportedTraces = new HashSet<>();

  public static void checkCallContext() {
    if (isInsideEventProcessing() && ourReportedTraces.add(DebugUtil.currentStackTrace())) {
      LOG.error("It's prohibited to access index during event dispatching");
    }
  }

  public static boolean isInsideEventProcessing() {
    Application application = ApplicationManager.getApplication();
    if (!application.isWriteAccessAllowed()) return false;

    MessageBus bus = application.getMessageBus();
    return bus.hasUndeliveredEvents(BulkFileListener.class) || bus.hasUndeliveredEvents(PsiModificationTrackerListener.class);
  }
}
