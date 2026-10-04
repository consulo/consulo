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
package consulo.grid.editor;

import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.grid.csv.CsvFormat;
import consulo.ui.ex.grid.csv.CsvFormatHookUp;
import consulo.ui.ex.grid.csv.CsvFormatParser;
import consulo.ui.ex.grid.csv.CsvFormatter;
import consulo.ui.ex.grid.csv.CsvImportUtil;
import consulo.ui.ex.grid.csv.CsvParserResult;
import consulo.ui.ex.grid.csv.CsvRecord;
import consulo.ui.ex.grid.csv.TypeMerger;
import consulo.ui.ex.grid.csv.ValueRange;
import consulo.ui.grid.ColumnDescriptor;
import consulo.ui.grid.ColumnQueryData;
import consulo.ui.grid.DataConsumer;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridRequestSource;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.NamedRow;
import consulo.ui.grid.ReservedCellValue;
import consulo.ui.grid.RowMutation;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.IntFunction;

import static consulo.grid.editor.DocumentDataHookUp.DataMarkup.BIG_INTEGER_MERGER;
import static consulo.grid.editor.DocumentDataHookUp.DataMarkup.BOOLEAN_MERGER;
import static consulo.grid.editor.DocumentDataHookUp.DataMarkup.DOUBLE_MERGER;
import static consulo.grid.editor.DocumentDataHookUp.DataMarkup.INTEGER_MERGER;
import static consulo.grid.editor.DocumentDataHookUp.DataMarkup.STRING_MERGER;
import static consulo.grid.editor.DocumentDataHookUp.DataMarkup.getType;

/**
 * A document data source of comma separated values - or of any {@link CsvFormat}.
 * <p/>
 * A subclass with its own parser overrides {@link #buildMarkup}, and returns a {@link CsvMarkup} built with its own
 * {@link CsvFormatter} and column names ({@link CsvMarkup#CsvMarkup(CsvParserResult, CsvFormatter, IntFunction)}).
 */
public class CsvDocumentDataHookUp extends DocumentDataHookUp implements CsvFormatHookUp {
    private static final IntFunction<String> DEFAULT_COLUMN_NAME = i -> "C" + (i + 1);
    /**
     * The attributes of a column the header does not name: its name follows its position.
     */
    private static final Set<ColumnDescriptor.Attribute> GENERATED_NAME_ATTRIBUTES = Set.of(ColumnDescriptor.Attribute.GENERATED_NAME);

    /**
     * Read by {@link #buildMarkup} on the pool thread.
     */
    private volatile CsvFormat myFormat;

    /**
     * @param project  the project the document undo and the read-only check belong to, or {@code null} for a document of no project
     * @param range    the part of the document which holds the data, or {@code null} for the whole document
     * @param uiAccess the UI thread of the grid, where the data source reports its answers
     */
    public CsvDocumentDataHookUp(@Nullable Project project,
                                 CsvFormat format,
                                 Document document,
                                 @Nullable TextRange range,
                                 UIAccess uiAccess) {
        super(project, document, range, uiAccess);
        myFormat = format;
    }

    @Override
    protected DocumentDataMutator createDataMutator() {
        return new CsvDocumentDataMutator();
    }

    @Override
    @RequiredUIAccess
    public void setFormat(CsvFormat format, GridRequestSource source) {
        myFormat = format;
        // a header record may come or go: the rows and columns get other indices
        markStructureChanged();
        getLoader().reloadCurrentPage(source);
    }

    @Override
    public CsvFormat getFormat() {
        return myFormat;
    }

    /**
     * Consulo: a request changed the format along with the text - renaming a column of a file without a header row writes one. Called
     * inside the command of the request, before the text is parsed again. The default takes the format as it is.
     */
    protected void formatChangedAfterUpdate(CsvFormat format) {
        myFormat = format;
    }

    @Override
    protected @Nullable CsvMarkup buildMarkup(CharSequence sequence, GridRequestSource source) {
        //TODO use a sliding window if copying whole document is unacceptable
        // using CharsSequence returned by myDocument.getCharsSequence is slow, use string instead
        String string = sequence.toString();
        CsvFormat format = myFormat;
        CsvParserResult result = new CsvFormatParser(format).parse(string);
        return result == null ? oneLineMarkup(format, string) : new CsvMarkup(result);
    }

    private static CsvMarkup oneLineMarkup(CsvFormat format, String string) {
        CsvFormatter formatter = new CsvFormatter(format);
        int length = string.length();
        TextRange range = new TextRange(0, length);
        CsvRecord record = new CsvRecord(range, Collections.singletonList(new ValueRange(0, length)), false);
        return new CsvMarkup(formatter, string, Collections.singletonList(record), null, false, record.values.size());
    }

    private class CsvDocumentDataMutator extends DocumentDataMutator {
        @Override
        protected UpdateSession createSession() {
            TextRange range = getRange();
            return new CsvUpdateSession(getDocument(), range != null ? range.getStartOffset() : 0);
        }

        @Override
        protected void finishSession(UpdateSession session, boolean success) {
            if (success && session instanceof CsvUpdateSession csvSession) {
                CsvFormat newFormat = csvSession.myNewFormat;
                if (newFormat != null) {
                    formatChangedAfterUpdate(newFormat);
                }
            }
        }
    }

    protected static class CsvUpdateSession extends UpdateSession {
        private @Nullable CsvFormat myNewFormat;

        private CsvUpdateSession(Document document, int rightShift) {
            super(document, rightShift);
        }
    }

    public static List<GridColumn> columnsFrom(CharSequence sequence,
                                               List<CsvRecord> records,
                                               @Nullable CsvRecord header,
                                               boolean rowNameColumn,
                                               int columnsCount,
                                               CsvFormat format) {
        return columnsFrom(sequence, records, header, rowNameColumn, columnsCount, format, DEFAULT_COLUMN_NAME);
    }

    /**
     * Consulo: the names of the columns the header does not name come from {@code columnName}.
     *
     * @param columnName the name of a column by its index, starting from 0
     */
    public static List<GridColumn> columnsFrom(CharSequence sequence,
                                               List<CsvRecord> records,
                                               @Nullable CsvRecord header,
                                               boolean rowNameColumn,
                                               int columnsCount,
                                               CsvFormat format,
                                               IntFunction<String> columnName) {
        int count = columnsCount > 0 && rowNameColumn ? columnsCount - 1 : columnsCount;
        List<@Nullable String> columnNames;
        if (header != null && rowNameColumn) {
            columnNames = CsvFormatParser.values(null, sequence, header.values.subList(1, header.values.size()));
        }
        else if (header != null) {
            columnNames = CsvFormatParser.values(null, sequence, header.values);
        }
        else {
            columnNames = null;
        }
        List<GridColumn> columns = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String headerName = columnNames == null || i >= columnNames.size() ? null : columnNames.get(i);
            String name = headerName != null ? headerName : columnName.apply(i);
            TypeMerger merger = determineColumnType(records, i, sequence, format);
            // Consulo: a made-up name is marked, so the grid does not take it for the identity of the column
            Set<ColumnDescriptor.Attribute> attributes = headerName != null ? Collections.emptySet() : GENERATED_NAME_ATTRIBUTES;
            columns.add(new DataConsumer.Column(i, name, getType(merger), -1, -1, attributes));
        }
        return columns;
    }

    private static TypeMerger determineColumnType(List<CsvRecord> records,
                                                  int i,
                                                  CharSequence sequence,
                                                  CsvFormat format) {
        List<@Nullable String> values = new ArrayList<>();
        for (CsvRecord row : records) {
            if (values.size() >= 200) {
                break;
            }
            values.add(i < row.values.size() ? nullize(format.dataRecord.nullText, row.values.get(i).value(sequence).toString()) : null);
        }
        return CsvImportUtil.getPreferredTypeMergerBasedOnContent(values, STRING_MERGER, INTEGER_MERGER, BIG_INTEGER_MERGER, DOUBLE_MERGER,
            BOOLEAN_MERGER);
    }

    private static @Nullable String nullize(@Nullable String nullText, String string) {
        return Objects.equals(nullText, string) ? null : string;
    }

    public static List<GridRow> rowsFrom(CsvFormat format,
                                         CharSequence sequence,
                                         List<CsvRecord> records,
                                         boolean named) {
        List<GridRow> rows = new ArrayList<>(records.size());
        for (int i = 0; i < records.size(); i++) {
            List<ValueRange> valuesList = records.get(i).values;
            GridRow row;
            if (named) {
                List<ValueRange> namedValues = valuesList.subList(1, valuesList.size());
                @Nullable Object[] values = CsvFormatParser.values(format.dataRecord, sequence, namedValues).toArray();
                String name = valuesList.get(0).value(sequence).toString();
                row = NamedRow.create(i, name, values);
            }
            else {
                row = DataConsumer.Row.create(i, CsvFormatParser.values(format.dataRecord, sequence, valuesList).toArray());
            }
            rows.add(row);
        }
        return rows;
    }

    /**
     * The markup of records parsed in a {@link CsvFormat}. Every change is a minimal change of the ranges it touches: other records, the
     * quoting of other values and the text between records - comment lines of a subclass parser, for one - stay as they are.
     * <p/>
     * Consulo: a record may hold fewer values than there are columns. Such a short record is padded only by a change which writes a value
     * beyond its end, once, up to the last value written; inserting, moving and deleting columns leave the values it does not have alone.
     * <p/>
     * A subclass changes how records are appended by overriding {@link #appendRecords}.
     */
    public static class CsvMarkup extends DocumentDataHookUp.DataMarkup {
        /**
         * Consulo: the parsed text, which a move of columns copies the values from.
         */
        private final CharSequence mySequence;
        private final List<CsvRecord> myRecords;
        private final @Nullable CsvRecord myHeader;
        private final CsvFormatter myFormatter;

        public CsvMarkup(CsvParserResult result) {
            this(result, new CsvFormatter(result.getFormat()), DEFAULT_COLUMN_NAME);
        }

        /**
         * Consulo: a markup of a parser with its own quoting rules and column names.
         *
         * @param formatter  writes the values, records and header values the changes need
         * @param columnName the name of a column the header does not name, by its index starting from 0
         */
        protected CsvMarkup(CsvParserResult result, CsvFormatter formatter, IntFunction<String> columnName) {
            super(
                columnsFrom(result.getSequence(), result.getRecords(), result.getHeader(), result.getFormat().rowNumbers,
                    result.getColumnsCount(), result.getFormat(), columnName),
                rowsFrom(result.getFormat(), result.getSequence(), result.getRecords(), result.getFormat().rowNumbers));
            myFormatter = formatter;
            myHeader = result.getHeader();
            myRecords = result.getRecords();
            mySequence = result.getSequence();
        }

        CsvMarkup(CsvFormatter formatter,
                  CharSequence sequence,
                  List<CsvRecord> records,
                  @Nullable CsvRecord header,
                  boolean firstRowIsHeader,
                  int columnsCount) {
            super(columnsFrom(sequence, records, header, firstRowIsHeader, columnsCount, formatter.getFormat()),
                rowsFrom(formatter.getFormat(), sequence, records, firstRowIsHeader));
            myFormatter = formatter;
            myHeader = header;
            myRecords = records;
            mySequence = sequence;
        }

        /**
         * Consulo: the data records, in the order of the rows.
         */
        protected List<CsvRecord> getRecords() {
            return myRecords;
        }

        /**
         * Consulo: the header record, or {@code null} when the format has none.
         */
        protected @Nullable CsvRecord getHeader() {
            return myHeader;
        }

        /**
         * Consulo: writes the values, records and header values of the changes.
         */
        protected CsvFormatter getFormatter() {
            return myFormatter;
        }

        /**
         * Consulo: the parsed text the ranges of the records point into.
         */
        protected CharSequence getSequence() {
            return mySequence;
        }

        @Override
        protected boolean deleteRows(UpdateSession session, List<GridRow> sortedRows) {
            for (GridRow row : sortedRows) {
                CsvRecord record = getRecord(row);
                if (record != null) {
                    session.delete(record.range);
                }
            }
            return true;
        }

        @Override
        protected boolean insertRow(UpdateSession session) {
            return appendRecords(session, Collections.singletonList(emptyValues()));
        }

        /**
         * Consulo: before the record of the row, or appended.
         */
        @Override
        protected boolean insertRow(UpdateSession session, @Nullable GridRow before) {
            return insertRows(session, before, 1);
        }

        /**
         * Consulo: the new records go in one piece before the record of the row, or are appended with {@link #appendRecords}.
         */
        @Override
        protected boolean insertRows(UpdateSession session, @Nullable GridRow before, int amount) {
            List<List<?>> records = new ArrayList<>(amount);
            for (int i = 0; i < amount; i++) {
                records.add(emptyValues());
            }
            CsvRecord beforeRecord = before != null ? getRecord(before) : null;
            if (beforeRecord == null) {
                return appendRecords(session, records);
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < records.size(); i++) {
                sb.append(myFormatter.formatRecord(withRecordName(records.get(i), i))).append(myFormatter.recordSeparator());
            }
            session.insert(sb, beforeRecord.range.getStartOffset());
            return true;
        }

        @Override
        protected boolean cloneRow(UpdateSession session, GridRow row) {
            return appendRecords(session, Collections.singletonList(Arrays.asList(GridRow.getValues(row))));
        }

        /**
         * Consulo: the columns go by ranges ({@link #deleteColumnRanges}); only when every column goes are the records written again.
         */
        @Override
        protected boolean deleteColumns(UpdateSession session, List<GridColumn> sortedColumns) {
            if (deleteColumnRanges(session, sortedColumns)) {
                return true;
            }

            List<GridColumn> columnsToLeave = getColumnsToLeave(sortedColumns);
            CsvRecord header = myHeader;
            if (header != null) {
                leaveColumns(session, columnsToLeave, header);
            }
            for (int i = 0; i < rows.size(); i++) {
                leaveColumns(session, columnsToLeave, myRecords.get(i), rows.get(i));
            }
            return true;
        }

        /**
         * Consulo: deletes the values of the columns from every record which has them, each run of adjacent values with one separator
         * next to it. A short record keeps the values it has.
         *
         * @return {@code false} - without any change made - when every column goes: the records themselves are deleted then
         */
        protected boolean deleteColumnRanges(UpdateSession session, List<GridColumn> sortedColumns) {
            if (getColumnsToLeave(sortedColumns).isEmpty()) {
                return false;
            }

            int shift = myFormatter.requiresRowNumbers() ? 1 : 0;
            int[] indices = new int[sortedColumns.size()];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = sortedColumns.get(i).getColumnNumber() + shift;
            }

            CsvRecord header = myHeader;
            if (header != null) {
                deleteValueRanges(session, header.values, indices);
            }
            for (CsvRecord record : myRecords) {
                deleteValueRanges(session, record.values, indices);
            }
            return true;
        }

        private static void deleteValueRanges(UpdateSession session, List<ValueRange> values, int[] sortedIndices) {
            int count = values.size();
            int i = 0;
            while (i < sortedIndices.length && sortedIndices[i] < count) {
                int first = sortedIndices[i];
                int last = first;
                while (i + 1 < sortedIndices.length && sortedIndices[i + 1] == last + 1 && sortedIndices[i + 1] < count) {
                    i++;
                    last++;
                }
                i++;

                if (last < count - 1) {
                    // the run and the separator after it
                    session.delete(new TextRange(values.get(first).getStartOffset(), values.get(last + 1).getStartOffset()));
                }
                else if (first > 0) {
                    // the run ends the record: the separator before it
                    session.delete(new TextRange(values.get(first - 1).getEndOffset(), values.get(last).getEndOffset()));
                }
                else {
                    // every value of a short record
                    session.delete(new TextRange(values.get(0).getStartOffset(), values.get(count - 1).getEndOffset()));
                }
            }
        }

        @Override
        protected boolean renameColumn(UpdateSession session,
                                       ModelIndex<GridColumn> column,
                                       String name) {
            if (column.asInteger() >= columns.size()) {
                return false;
            }
            String columnName = myFormatter.formatHeaderValue(name);
            CsvRecord header = myHeader;
            if (header == null) {
                generateHeaderRow(session, column, columnName);
                return true;
            }
            if (column.asInteger() >= header.values.size()) {
                generateMissingPartOfHeaderRow(session, header, column, columnName);
                return true;
            }
            ValueRange range = header.values.get(column.asInteger());
            session.replace(range, columnName);
            return true;
        }

        private void generateMissingPartOfHeaderRow(UpdateSession session,
                                                    CsvRecord header,
                                                    ModelIndex<GridColumn> column,
                                                    String columnName) {
            ValueRange lastValue = ContainerUtil.getLastItem(header.values);
            int offset = lastValue == null ? 0 : lastValue.getEndOffset();
            for (int i = header.values.size(); i < column.asInteger(); i++) {
                session.insert(myFormatter.valueSeparator(), offset);
                session.insert(columns.get(i).getName(), offset);
            }
            session.insert(myFormatter.valueSeparator(), offset);
            session.insert(columnName, offset);
        }

        private void generateHeaderRow(UpdateSession session,
                                       ModelIndex<GridColumn> column,
                                       String columnName) {
            if (session instanceof CsvUpdateSession csvSession) {
                csvSession.myNewFormat = CsvFormatter.setFirstRowIsHeader(myFormatter.getFormat(), true);
            }
            boolean emptyDoc = session.getText().isEmpty();
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (GridColumn c : columns) {
                if (first) {
                    first = false;
                }
                else {
                    sb.append(myFormatter.valueSeparator());
                }

                if (c.getColumnNumber() == column.asInteger()) {
                    sb.append(columnName);
                }
                else {
                    sb.append(myFormatter.formatHeaderValue(c.getName()));
                }
            }
            if (emptyDoc) {
                sb.append(myFormatter.recordSeparator());
            }
            session.insert(sb.append(myFormatter.recordSeparator()).toString(), 0);
        }

        /**
         * Consulo: appended with the rules of {@link #insertColumn(UpdateSession, GridColumn, String)}.
         */
        @Override
        protected boolean insertColumn(UpdateSession session, @Nullable String name) {
            return insertColumn(session, null, name);
        }

        /**
         * Consulo: for a column {@code k}, a record with more than {@code k} values gets the new value before its value {@code k}, a
         * record with exactly {@code k} values gets it appended, and a shorter record is left as it is. The header gets the name the
         * same way.
         */
        @Override
        protected boolean insertColumn(UpdateSession session, @Nullable GridColumn before, @Nullable String name) {
            int index = before != null && before.getColumnNumber() < columns.size() ? before.getColumnNumber() : columns.size();
            String columnName = name != null ? name : "column" + (columns.size() + 1);
            return doInsertColumn(session, index, null, columnName);
        }

        /**
         * Consulo: moves the values by ranges, in every record which has all the values between the two columns; a shorter record is
         * left as it is. The values keep their text, quoting included, and the separators between them stay where they are.
         */
        @Override
        protected boolean moveColumn(UpdateSession session, GridColumn fromColumn, ModelIndex<GridColumn> toColumn) {
            int shift = myFormatter.requiresRowNumbers() ? 1 : 0;
            int from = fromColumn.getColumnNumber() + shift;
            int to = toColumn.asInteger() + shift;
            if (from == to) {
                return true;
            }

            CsvRecord header = myHeader;
            if (header != null) {
                moveValues(session, header.values, from, to);
            }
            for (CsvRecord record : myRecords) {
                moveValues(session, record.values, from, to);
            }
            return true;
        }

        private void moveValues(UpdateSession session, List<ValueRange> values, int from, int to) {
            boolean fromBeforeTo = from < to; // i.e. moving to the right
            int left = Math.min(from, to);
            int right = Math.max(from, to);
            if (values.size() <= right) {
                return;
            }

            StringBuilder text = new StringBuilder();
            for (int i = left; i <= right; i++) {
                int oldPos = i == to ? from : fromBeforeTo ? i + 1 : i - 1;
                text.append(values.get(oldPos).subSequence(mySequence));
                if (i < right) {
                    text.append(mySequence, values.get(i).getEndOffset(), values.get(i + 1).getStartOffset());
                }
            }
            session.replace(new TextRange(values.get(left).getStartOffset(), values.get(right).getEndOffset()), text);
        }

        @Override
        protected String prepareMoveColumn(GridColumn fromColumn, ModelIndex<GridColumn> toColumn) {
            int to = toColumn.value;
            int from = fromColumn.getColumnNumber();
            boolean fromBeforeTo = from < to; // i.e. moving to the right
            int left = Math.min(from, to);
            int right = Math.max(from, to);

            StringBuilder text = new StringBuilder();

            if (myHeader != null) {
                for (int i = 0; i < columns.size(); i++) {
                    int newPos = (i < left || i > right) ? i :
                        i == to ? from :
                            fromBeforeTo ? i + 1 : i - 1;
                    text.append(myFormatter.formatHeaderValue(columns.get(newPos).getName()));
                    text.append(i < columns.size() - 1 ? myFormatter.headerValueSeparator() : myFormatter.recordSeparator());
                }
            }

            for (GridRow row : rows) {
                for (int i = 0; i < columns.size(); i++) {
                    int newPos = (i < left || i > right) ? i :
                        i == to ? from :
                            fromBeforeTo ? i + 1 : i - 1;
                    text.append(myFormatter.formatValue(toCsvValue(columns.get(newPos).getValue(row))));
                    text.append(i < columns.size() - 1 ? myFormatter.valueSeparator() : myFormatter.recordSeparator());
                }
            }

            return text.toString();
        }

        /**
         * Consulo: appended with the rules of {@link #insertColumn(UpdateSession, GridColumn, String)}, except that a short record which
         * has a value to copy is padded up to the new column.
         */
        @Override
        protected boolean cloneColumn(UpdateSession session, GridColumn column) {
            return doInsertColumn(session, columns.size(), column, column.getName());
        }

        /**
         * Consulo: a record which lacks values gets them once, up to the last column the mutation writes.
         */
        @Override
        protected boolean update(UpdateSession session, List<RowMutation> mutations) {
            for (RowMutation mutation : mutations) {
                GridRow row = mutation.getRow();
                CsvRecord record = getRecord(row);
                if (record == null) {
                    continue;
                }
                List<ValueRange> valueRanges = record.values;
                Map<Integer, String> missingValues = new TreeMap<>();
                for (ColumnQueryData data : mutation.getData()) {
                    String newValueText = myFormatter.formatValue(toCsvValue(data.getObject()));
                    GridColumn column = data.getColumn();
                    int rangeIdx = column.getColumnNumber() + (row instanceof NamedRow ? 1 : 0);
                    if (rangeIdx < valueRanges.size()) {
                        session.replace(valueRanges.get(rangeIdx), newValueText);
                    }
                    else {
                        missingValues.put(rangeIdx, newValueText);
                    }
                }
                if (!missingValues.isEmpty()) {
                    insertMissingRanges(session, record, missingValues);
                }
            }
            return true;
        }

        /**
         * Consulo: appends the values a record lacks up to the last one written - the written ones with their text, the others empty.
         */
        private void insertMissingRanges(UpdateSession session, CsvRecord record, Map<Integer, String> missingValues) {
            List<ValueRange> values = record.values;
            ValueRange lastValue = ContainerUtil.getLastItem(values);
            int endOffset = lastValue != null ? lastValue.getEndOffset() : record.range.getStartOffset();
            String nullValue = myFormatter.formatValue(null);
            int lastIndex = Collections.max(missingValues.keySet());

            StringBuilder sb = new StringBuilder();
            for (int i = values.size(); i <= lastIndex; i++) {
                if (i > 0) {
                    sb.append(myFormatter.valueSeparator());
                }
                String valueText = missingValues.get(i);
                sb.append(valueText != null ? valueText : nullValue);
            }
            session.insert(sb, endOffset);
        }

        /**
         * Consulo: inserts the column {@code index} with the name and the values of {@code column}, or empty values.
         */
        private boolean doInsertColumn(UpdateSession session, int index, @Nullable GridColumn column, String name) {
            int valueIndex = index + (myFormatter.requiresRowNumbers() ? 1 : 0);
            CsvRecord header = myHeader;
            if (header != null) {
                String columnName = myFormatter.formatHeaderValue(name);
                insertValue(session, header.values, valueIndex, columnName, myFormatter.headerValueSeparator(), false);
            }
            for (int i = 0; i < rows.size() && i < myRecords.size(); i++) {
                Object value = column != null ? toCsvValue(column.getValue(rows.get(i))) : null;
                String valueText = myFormatter.formatValue(value);
                insertValue(session, myRecords.get(i).values, valueIndex, valueText, myFormatter.valueSeparator(), value != null);
            }
            return true;
        }

        private void insertValue(UpdateSession session,
                                 List<ValueRange> values,
                                 int index,
                                 String valueText,
                                 String separator,
                                 boolean padShortRecord) {
            int count = values.size();
            if (count == 0) {
                // nothing to put the value after
                return;
            }

            if (count > index) {
                session.insert(valueText + separator, values.get(index).getStartOffset());
            }
            else if (count == index) {
                session.insert(separator + valueText, values.get(count - 1).getEndOffset());
            }
            else if (padShortRecord) {
                String nullValue = myFormatter.formatValue(null);
                StringBuilder sb = new StringBuilder();
                for (int i = count; i < index; i++) {
                    sb.append(separator).append(nullValue);
                }
                sb.append(separator).append(valueText);
                session.insert(sb, values.get(count - 1).getEndOffset());
            }
            // else: a short record which does not reach the column stays as it is
        }

        /**
         * Consulo: appends records after the last one, in one session. A subclass which wants other line endings around appended records -
         * a final line break kept, for one - overrides it.
         *
         * @param records the values of each new record, without the name of a row
         */
        protected boolean appendRecords(UpdateSession session, List<? extends List<?>> records) {
            CsvRecord lastRecord = myRecords.isEmpty() ? myHeader : myRecords.get(myRecords.size() - 1);
            int offset = lastRecord != null ? lastRecord.range.getEndOffset() : 0;
            if (lastRecord != null && !lastRecord.hasRecordSeparator) {
                session.insert(myFormatter.recordSeparator(), offset);
            }
            for (int i = 0; i < records.size(); i++) {
                String newRecord = myFormatter.formatRecord(withRecordName(records.get(i), i));
                session.insert(newRecord, offset);
                if (newRecord.isEmpty() || i < records.size() - 1) {
                    session.insert(myFormatter.recordSeparator(), offset);
                }
            }
            return true;
        }

        /**
         * @param shift the position of the record among the new ones
         */
        private List<?> withRecordName(List<?> values, int shift) {
            if (!myFormatter.requiresRowNumbers()) {
                return values;
            }
            String newRecordName = null;
            GridRow lastRow = ContainerUtil.getLastItem(rows);
            if (lastRow instanceof NamedRow namedRow) {
                try {
                    newRecordName = String.valueOf(Long.parseLong(namedRow.name) + 1 + shift);
                }
                catch (NumberFormatException ignore) {
                }
            }
            if (newRecordName == null) {
                newRecordName = String.valueOf(lastRow != null ? lastRow.getRowNum() + 1 + shift : 1 + shift);
            }
            List<@Nullable Object> named = new ArrayList<>(values.size() + 1);
            named.add(newRecordName);
            named.addAll(values);
            return named;
        }

        private List<?> emptyValues() {
            return Arrays.asList(new @Nullable Object[columns.size()]);
        }

        private @Nullable CsvRecord getRecord(GridRow row) {
            int index = GridRow.toRealIdx(row);
            return index >= 0 && index < myRecords.size() ? myRecords.get(index) : null;
        }

        /**
         * Consulo: a value which stands for a missing one - beyond the end of a short record, or a reserved value - is written as an empty
         * value.
         */
        private static @Nullable Object toCsvValue(@Nullable Object value) {
            return value instanceof ReservedCellValue ? null : value;
        }

        private void leaveColumns(UpdateSession session,
                                  List<GridColumn> columns,
                                  CsvRecord record,
                                  GridRow row) {
            if (columns.isEmpty()) {
                session.delete(record.range);
                return;
            }

            List<@Nullable Object> values = new ArrayList<>(columns.size() + 1);
            if (row instanceof NamedRow namedRow) {
                values.add(namedRow.name);
            }
            for (GridColumn column : columns) {
                values.add(toCsvValue(column.getValue(row)));
            }
            String recordText = myFormatter.formatRecord(values);

            session.replace(record.range, recordText);
            if (record.hasRecordSeparator) {
                session.insert(myFormatter.recordSeparator(), record.range.getEndOffset());
            }
        }

        private void leaveColumns(UpdateSession session,
                                  List<GridColumn> columns,
                                  CsvRecord headerRecord) {
            List<ValueRange> values = headerRecord.values;
            int valuesStart = values.get(myFormatter.requiresRowNumbers() ? 1 : 0).getStartOffset();
            int valuesEnd = values.get(values.size() - 1).getEndOffset();

            StringBuilder sb = new StringBuilder();
            for (GridColumn column : columns) {
                sb.append(myFormatter.formatHeaderValue(column.getName()))
                    .append(myFormatter.headerValueSeparator());
            }
            sb.setLength(!sb.isEmpty() ? sb.length() - myFormatter.headerValueSeparator().length() : 0);

            session.replace(TextRange.create(valuesStart, valuesEnd), sb.toString());
        }

        private List<GridColumn> getColumnsToLeave(List<GridColumn> orderedColumnsToDelete) {
            List<GridColumn> columnsToLeave = new ArrayList<>(Math.max(0, columns.size() - orderedColumnsToDelete.size()));

            Iterator<GridColumn> toDeleteIterator = orderedColumnsToDelete.iterator();
            Iterator<GridColumn> allColumnsIterator = columns.iterator();

            while (allColumnsIterator.hasNext()) {
                GridColumn toDelete = toDeleteIterator.hasNext() ? toDeleteIterator.next() : null;
                do {
                    GridColumn column = allColumnsIterator.next();
                    if (column.equals(toDelete)) {
                        break;
                    }

                    columnsToLeave.add(column);
                }
                while (allColumnsIterator.hasNext());
            }

            return columnsToLeave;
        }
    }
}
