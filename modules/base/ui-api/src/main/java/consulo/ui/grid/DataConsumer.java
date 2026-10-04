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

import consulo.util.lang.ObjectUtil;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author Gregory.Shrago
 */
public interface DataConsumer {

    default void setColumns(GridDataRequest.Context context, GridColumn[] columns) {
        setColumns(context, 0, 0, columns, 0);
    }

    default void setColumns(GridDataRequest.Context context, int subQueryIndex, int resultSetIndex,
                            GridColumn[] columns, int firstRowNum) {
    }

    default void setInReference(GridDataRequest.Context context, Object reference) {
    }

    default void updateColumns(GridDataRequest.Context context, GridColumn[] columns) {
    }

    default void setOutReferences(GridDataRequest.Context context, Set<Object> references) {
    }

    default void addRows(GridDataRequest.Context context, List<? extends GridRow> rows) {
    }

    default void afterLastRowAdded(GridDataRequest.Context context, int total) {
    }


    class Row implements GridRow {
        /**
         * @deprecated use {@code GridRow.getRowNum()} instead
         */
        @Deprecated(forRemoval = true)
        public final int rowNum;

        /**
         * @deprecated use {@code GridRow.getSize() and GridRow.getValue(int)} instead
         */
        @Deprecated(forRemoval = true)
        public final @Nullable Object[] values;

        protected Row(int rowNum, @Nullable Object[] values) {
            this.rowNum = rowNum;
            this.values = values;
        }

        public static Row create(int realIdx, @Nullable Object[] values) {
            return new Row(realIdx + 1, values);
        }

        @Override
        public void setValue(int i, @Nullable Object object) {
            values[i] = object;
        }

        @Override
        public @Nullable Object getValue(int columnNum) {
            return columnNum < values.length ? values[columnNum] : ReservedCellValue.UNSET;
        }

        @Override
        public int getSize() {
            return values.length;
        }

        @Override
        public int getRowNum() {
            return rowNum;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof GridRow)) {
                return false;
            }
            return GridRow.equals(this, (GridRow) o);
        }

        @Override
        public int hashCode() {
            int result = getRowNum();
            result = 31 * result + Arrays.hashCode(values);
            return result;
        }

        @Override
        public String toString() {
            return "Row{" +
                "rowNum=" + rowNum +
                ", values=" + Arrays.toString(values) +
                '}';
        }
    }

    /**
     * A column which knows nothing of where its data comes from. A data source which has more to say about a column -
     * the table it belongs to, the class of its values - extends it.
     */
    class Column implements GridColumn, SizeProvider {
        private final Set<Attribute> myAttributes;

        private final int myColumnNum;
        private final GridDataType myType;
        private final String myName;
        private final int myPrecision;
        private final int myScale;

        public Column(int columnNum, String name, GridDataType type) {
            this(columnNum, name, type, -1, -1);
        }

        public Column(int columnNum, String name, GridDataType type, int precision, int scale) {
            this(columnNum, name, type, precision, scale, Collections.emptySet());
        }

        public Column(int columnNum, String name, GridDataType type, int precision, int scale, Set<Attribute> attributes) {
            myColumnNum = columnNum;
            myName = name;
            myType = type;
            myPrecision = precision;
            myScale = scale;
            myAttributes = attributes;
        }

        @Override
        public int getScale() {
            return myScale;
        }

        @Override
        public String toString() {
            return "Column" + getColumnNumber() + "{" +
                "name='" + getName() + '\'' +
                ", type=" + getType() +
                '}';
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Column column = (Column) o;

            if (getColumnNumber() != column.getColumnNumber()) {
                return false;
            }
            if (myPrecision != column.myPrecision) {
                return false;
            }
            if (myScale != column.myScale) {
                return false;
            }
            if (!Objects.equals(getType(), column.getType())) {
                return false;
            }
            if (!Objects.equals(getName(), column.getName())) {
                return false;
            }
            return myAttributes.equals(column.myAttributes);
        }

        @Override
        public int hashCode() {
            int result = getColumnNumber();
            result = 31 * result + getType().hashCode();
            result = 31 * result + getName().hashCode();
            result = 31 * result + myPrecision;
            result = 31 * result + myScale;
            result = 31 * result + myAttributes.hashCode();
            return result;
        }

        @Override
        public GridDataType getType() {
            return myType;
        }

        @Override
        public String getName() {
            return myName;
        }

        @Override
        public int getSize() {
            return myPrecision;
        }

        @Override
        public int getColumnNumber() {
            return myColumnNum;
        }

        @Override
        public Set<Attribute> getAttributes() {
            return myAttributes;
        }

        public static Column copy(GridColumn column, int idx) {
            return copy(column, idx, column.getName(), column.getType());
        }

        public static Column copy(GridColumn column, int idx, String name, GridDataType type) {
            Column consumerColumn = ObjectUtil.tryCast(column, Column.class);
            return consumerColumn == null
                ? new Column(idx, name, type)
                : new Column(idx, name, type, consumerColumn.getSize(), consumerColumn.getScale(), consumerColumn.getAttributes());
        }
    }

    class Composite implements DataConsumer {
        private final List<DataConsumer> myDelegates;

        public Composite(List<DataConsumer> delegates) {
            myDelegates = delegates;
        }

        public Composite(DataConsumer... delegates) {
            myDelegates = Arrays.asList(delegates);
        }

        @Override
        public void setColumns(GridDataRequest.Context context, int subQueryIndex, int resultSetIndex,
                               GridColumn[] columns, int firstRowNum) {
            for (DataConsumer delegate : myDelegates) {
                delegate.setColumns(context, subQueryIndex, resultSetIndex, columns, firstRowNum);
            }
        }

        @Override
        public void setInReference(GridDataRequest.Context context, Object reference) {
            for (DataConsumer delegate : myDelegates) {
                delegate.setInReference(context, reference);
            }
        }

        @Override
        public void updateColumns(GridDataRequest.Context context, GridColumn[] columns) {
            for (DataConsumer delegate : myDelegates) {
                delegate.updateColumns(context, columns);
            }
        }

        @Override
        public void setOutReferences(GridDataRequest.Context context, Set<Object> references) {
            for (DataConsumer delegate : myDelegates) {
                delegate.setOutReferences(context, references);
            }
        }

        @Override
        public void addRows(GridDataRequest.Context context, List<? extends GridRow> rows) {
            for (DataConsumer delegate : myDelegates) {
                delegate.addRows(context, rows);
            }
        }

        @Override
        public void afterLastRowAdded(GridDataRequest.Context context, int total) {
            for (DataConsumer delegate : myDelegates) {
                delegate.afterLastRowAdded(context, total);
            }
        }
    }
}
