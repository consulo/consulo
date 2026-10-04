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

import consulo.util.lang.Comparing;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public class ColumnQueryData implements Comparable<ColumnQueryData>, ColumnDescriptor, SizeProvider {
    private final GridColumn myColumn;
    private final @Nullable Object myObject;

    public ColumnQueryData(GridColumn column, @Nullable Object object) {
        myColumn = column;
        myObject = object;
    }

    public GridColumn getColumn() {
        return myColumn;
    }

    public @Nullable Object getObject() {
        return myObject;
    }

    @Override
    public Set<Attribute> getAttributes() {
        return myColumn.getAttributes();
    }

    @Override
    public int compareTo(ColumnQueryData o) {
        return Integer.compare(myColumn.getColumnNumber(), o.myColumn.getColumnNumber());
    }

    @Override
    public GridDataType getType() {
        return myColumn.getType();
    }

    @Override
    public String getName() {
        return myColumn.getName();
    }

    @Override
    public int getSize() {
        return myColumn instanceof SizeProvider ? ((SizeProvider) myColumn).getSize() : -1;
    }

    @Override
    public int getScale() {
        return myColumn instanceof SizeProvider ? ((SizeProvider) myColumn).getScale() : -1;
    }

    @Override
    public int hashCode() {
        return Comparing.hashcode(myColumn, myObject);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof ColumnQueryData toCompare)) {
            return false;
        }
        return Comparing.equal(myColumn, toCompare.myColumn) && Comparing.equal(myObject, toCompare.myObject);
    }
}
