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
package consulo.versionControlSystem.internal;

import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ContentRevision;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public class ChangesBrowserUtil {
    /**
     * Zips changes by removing duplicates (changes in the same file) and compounding the diff.
     * <b>NB:</b> changes must be given in the time-ascending order, i.e the first change in the list should be the oldest one.
     */
    @Nonnull
    public static List<Change> zipChanges(@Nonnull List<Change> changes) {
        List<Change> result = new ArrayList<>();
        for (Change change : changes) {
            addOrReplaceChange(result, change);
        }
        return result;
    }

    public static void addOrReplaceChange(List<Change> changes, Change c) {
        ContentRevision beforeRev = c.getBeforeRevision();
        // todo!!! further improvements needed
        if (beforeRev != null) {
            String beforeName = beforeRev.getFile().getName();
            String beforeAbsolutePath = beforeRev.getFile().getIOFile().getAbsolutePath();
            for (Change oldChange : changes) {
                ContentRevision rev = oldChange.getAfterRevision();
                // first compare name, which is many times faster - to remove 99% not matching
                if (rev != null && (rev.getFile().getName().equals(beforeName))
                    && rev.getFile().getIOFile().getAbsolutePath().equals(beforeAbsolutePath)) {
                    changes.remove(oldChange);
                    if (oldChange.getBeforeRevision() != null || c.getAfterRevision() != null) {
                        changes.add(new Change(oldChange.getBeforeRevision(), c.getAfterRevision()));
                    }
                    return;
                }
            }
        }
        changes.add(c);
    }
}
