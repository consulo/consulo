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
package consulo.versionControlSystem.distributed.branch;

import consulo.localize.LocalizeValue;
import consulo.versionControlSystem.distributed.repository.Repository;

import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
public final class DvcsNewBranchOption<R extends Repository> {
    public static <R extends Repository> DvcsNewBranchOption<R> of(LocalizeValue text, boolean selected) {
        return new DvcsNewBranchOption<>(text, selected, input -> true);
    }

    public static <R extends Repository> DvcsNewBranchOption<R> of(LocalizeValue text,
                                                                  boolean selected,
                                                                  Predicate<DvcsNewBranchInput<R>> enabled) {
        return new DvcsNewBranchOption<>(text, selected, enabled);
    }

    private final LocalizeValue myText;
    private final boolean mySelected;
    private final Predicate<DvcsNewBranchInput<R>> myEnabled;

    private DvcsNewBranchOption(LocalizeValue text, boolean selected, Predicate<DvcsNewBranchInput<R>> enabled) {
        myText = text;
        mySelected = selected;
        myEnabled = enabled;
    }

    public LocalizeValue getText() {
        return myText;
    }

    public boolean isSelectedByDefault() {
        return mySelected;
    }

    public boolean isEnabled(DvcsNewBranchInput<R> input) {
        return myEnabled.test(input);
    }
}
