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
package consulo.language.impl.internal.util;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.internal.NoAccessDuringPsiEventsService;
import consulo.component.messagebus.MessageBus;
import consulo.language.impl.DebugUtil;
import consulo.language.psi.PsiModificationTrackerListener;
import consulo.logging.Logger;
import consulo.virtualFileSystem.event.BulkFileListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.HashSet;
import java.util.Set;

/**
 * @author peter
 */
@ServiceImpl
@Singleton
public class NoAccessDuringPsiEvents implements NoAccessDuringPsiEventsService {
    private static final Logger LOG = Logger.getInstance(NoAccessDuringPsiEvents.class);
    private final Set<String> myReportedTraces = new HashSet<>();

    private final Application myApplication;

    @Inject
    public NoAccessDuringPsiEvents(Application application) {
        myApplication = application;
    }

    @Override
    public void checkCallContext() {
        if (isInsideEventProcessing() && myReportedTraces.add(DebugUtil.currentStackTrace())) {
            LOG.error("It's prohibited to access index during event dispatching");
        }
    }

    @Override
    public boolean isInsideEventProcessing() {
        if (!myApplication.isWriteAccessAllowed()) {
            return false;
        }

        MessageBus bus = myApplication.getMessageBus();
        return bus.hasUndeliveredEvents(BulkFileListener.class) || bus.hasUndeliveredEvents(PsiModificationTrackerListener.class);
    }
}
