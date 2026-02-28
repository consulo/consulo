/*
 * Copyright 2013-2025 consulo.io
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
package consulo.execution.impl.internal;

import java.util.LinkedHashSet;
import java.util.SequencedSet;

/**
 * @author VISTALL
 * @since 2025-10-20
 */
public class RunConfigurationStartHistoryState {
    public SequencedSet<String> myHistory = new LinkedHashSet<>();

    public SequencedSet<String> myPinned = new LinkedHashSet<>();

    public boolean myAllConfigurationsExpanded;

    public SequencedSet<String> getHistory() {
        return myHistory;
    }

    public void setHistory(SequencedSet<String> history) {
        myHistory = history;
    }

    public SequencedSet<String> getPinned() {
        return myPinned;
    }

    public void setPinned(SequencedSet<String> pinned) {
        myPinned = pinned;
    }

    public boolean isAllConfigurationsExpanded() {
        return myAllConfigurationsExpanded;
    }

    public void setAllConfigurationsExpanded(boolean allConfigurationsExpanded) {
        myAllConfigurationsExpanded = allConfigurationsExpanded;
    }
}
