/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change.ui;

import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.LocalChangeList;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AlienLocalChangeList extends LocalChangeList {
    private final List<Change> myChanges;
    private String myName;
    private String myComment;

    public AlienLocalChangeList(List<Change> changes, String name) {
        myChanges = changes;
        myName = name;
        myComment = "";
    }

    @Override
    public Collection<Change> getChanges() {
        return myChanges;
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public void setName(String name) {
        myName = name;
    }

    @Override
    public String getComment() {
        return myComment;
    }

    @Override
    public void setComment(String comment) {
        myComment = comment;
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public void setReadOnly(boolean isReadOnly) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable Object getData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public LocalChangeList copy() {
        throw new UnsupportedOperationException();
    }

    public static final AlienLocalChangeList DEFAULT_ALIEN = new AlienLocalChangeList(Collections.<Change>emptyList(), "Default") {
        @Override
        public boolean isDefault() {
            return true;
        }
    };
}
