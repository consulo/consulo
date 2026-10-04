// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

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
package consulo.ui.grid;

import consulo.disposer.Disposable;

import java.util.EventListener;
import java.util.List;

/**
 * The filter of a data source as text: the current and the applied text, and the history. The model holds no editor document -
 * documents are not visible from {@code consulo.ui.api}; a data source which edits its filter in an editor adds the document on
 * its own model.
 */
public interface GridFilteringModel {

    String getFilterText();

    String getAppliedText();

    boolean isIgnoreCurrentText();

    void setIgnoreCurrentText(boolean ignore);

    void setFilterText(String text);

    void setHistory(List<String> history);

    List<String> getHistory();

    void addListener(Listener l, Disposable disposable);

    void applyCurrentText();

    interface Listener extends EventListener {
        default void onPsiUpdated() {
        }

        default void onPrefixUpdated() {
        }

        default void onApplicableUpdated() {
        }
    }
}
