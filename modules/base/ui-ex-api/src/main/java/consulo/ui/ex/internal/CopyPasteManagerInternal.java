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
package consulo.ui.ex.internal;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.CopyPasteManager;
import consulo.ui.clipboard.DataTransfer;

import java.util.List;

/**
 * The platform side of {@link CopyPasteManager}. The history feeds the paste from history dialog, the
 * kill ring flag feeds adjacent kill merging - plugins are not supposed to read or edit either.
 *
 * @author VISTALL
 * @since 2026-08-07
 */
public interface CopyPasteManagerInternal extends CopyPasteManager {
    /**
     * Most recent first, what this process wrote itself.
     */
    @RequiredUIAccess
    List<DataTransfer> getHistory();

    @RequiredUIAccess
    void removeFromHistory(DataTransfer transfer);

    /**
     * Set by {@link consulo.ui.ex.CopyPasteManager#stopKillRings()} and cleared by the next write - the
     * kill ring reads it to decide whether an adjacent kill may still combine with the previous one.
     */
    boolean isKillRingBroken();
}
