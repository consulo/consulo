/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ide.impl.idea.usages.impl;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.usage.UsageViewPresentation;
import consulo.usage.internal.UsageViewEx;

/**
 * What the usage view manager and the search need from a usage view, whichever frontend draws it.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
public interface UsageViewInternal extends UsageViewEx {
    /**
     * Shows the view in the Find tool window.
     */
    @RequiredUIAccess
    Content addContent(UsageViewPresentation presentation);

    @RequiredUIAccess
    void expandRoot();

    /**
     * Flushes the usages appended so far into the view; called off the UI thread when the search ends.
     */
    void drainQueuedUsageNodes();

    /**
     * @return the number of usages the view shows
     */
    int getShownUsageCount();
}
