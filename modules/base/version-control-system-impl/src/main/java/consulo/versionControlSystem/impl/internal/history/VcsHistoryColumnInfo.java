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
package consulo.versionControlSystem.impl.internal.history;

import consulo.ui.ex.awt.ColumnInfo;
import consulo.versionControlSystem.history.VcsFileRevision;
import consulo.versionControlSystem.history.VcsHistoryColumn;

/**
 * Shows a {@link VcsHistoryColumn} in the Swing history tables.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public class VcsHistoryColumnInfo extends ColumnInfo<VcsFileRevision, String> {
    private final VcsHistoryColumn myColumn;

    public VcsHistoryColumnInfo(VcsHistoryColumn column) {
        super(column.getName());
        myColumn = column;
    }

    @Override
    public String valueOf(VcsFileRevision revision) {
        return myColumn.getValue(revision);
    }
}
