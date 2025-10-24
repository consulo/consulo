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

/**
 * @author VISTALL
 * @since 2025-10-20
 */
public class RunConfigurationStartHistoryState {
    public LinkedHashSet<String> myHistory = new LinkedHashSet<>();

    public LinkedHashSet<String> myPinned = new LinkedHashSet<>();

    public boolean myAllConfigurationsExpanded;

    public LinkedHashSet<String> getHistory() {
        return myHistory;
    }

    public void setHistory(LinkedHashSet<String> history) {
        myHistory = history;
    }

    public LinkedHashSet<String> getPinned() {
        return myPinned;
    }

    public void setPinned(LinkedHashSet<String> pinned) {
        myPinned = pinned;
    }

    public boolean isAllConfigurationsExpanded() {
        return myAllConfigurationsExpanded;
    }

    public void setAllConfigurationsExpanded(boolean allConfigurationsExpanded) {
        myAllConfigurationsExpanded = allConfigurationsExpanded;
    }
}
