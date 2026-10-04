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
package consulo.ui.ex.grid.csv;

import consulo.localize.LocalizeValue;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static consulo.ui.ex.grid.csv.CsvFormatsSettings.formatsSimilar;

/**
 * Writes values and records in a {@link CsvFormat}.
 * <p>
 * A data source with its own quoting rules subclasses it: {@link #formatValue(Object)}, {@link #formatHeaderValue(Object)},
 * {@link #formatRecord(List)} and {@link #formatHeader(List)} are the overridable entry points. The record methods do not call
 * {@link #formatValue(Object)}, so a subclass overrides all four.
 */
public class CsvFormatter {
    private final CsvFormat myFormat;

    public CsvFormatter(CsvFormat format) {
        myFormat = format;
    }

    public CsvFormat getFormat() {
        return myFormat;
    }

    public String formatValue(@Nullable Object value) {
        return formatValue(myFormat.dataRecord, value);
    }

    public String formatHeaderValue(@Nullable Object value) {
        return formatValue(ObjectUtil.notNull(myFormat.headerRecord, myFormat.dataRecord), value);
    }

    public String valueSeparator() {
        return valueSeparator(myFormat.dataRecord);
    }

    public String headerValueSeparator() {
        return valueSeparator(ObjectUtil.notNull(myFormat.headerRecord, myFormat.dataRecord));
    }

    public String recordSeparator() {
        return myFormat.dataRecord.recordSeparator;
    }

    public String formatRecord(List<?> values) {
        return formatRecord(myFormat.dataRecord, values);
    }

    public String formatHeader(List<?> values) {
        return formatRecord(ObjectUtil.notNull(myFormat.headerRecord, myFormat.dataRecord), values);
    }

    public boolean requiresRowNumbers() {
        return myFormat.rowNumbers;
    }

    protected String valueToRawText(@Nullable Object value) {
        return String.valueOf(value);
    }

    private String formatRecord(CsvRecordFormat recordTemplate, List<?> values) {
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtil.notNullize(recordTemplate.prefix));
        for (Object value : values) {
            sb.append(formatValue(recordTemplate, value)).append(valueSeparator(recordTemplate));
        }
        sb.setLength(values.isEmpty() ? sb.length() : sb.length() - valueSeparator(recordTemplate).length());
        sb.append(StringUtil.notNullize(recordTemplate.suffix));
        return sb.toString();
    }

    private String formatValue(CsvRecordFormat recordTemplate, @Nullable Object value) {
        String valueText = value == null ? StringUtil.notNullize(recordTemplate.nullText) : valueToRawText(value);
        valueText = recordTemplate.trimWhitespace ? StringUtil.trimLeading(StringUtil.trimTrailing(valueText)) : valueText;
        CsvRecordFormat.QuotationPolicy quotationPolicy = recordTemplate.quotationPolicy;
        CsvRecordFormat.@Nullable Quotes quotes = value == null ? null : getQuotes(recordTemplate, valueText, quotationPolicy);
        return quotes != null ? quotes.leftQuote + escapeQuotes(valueText, quotes) + quotes.rightQuote : valueText;
    }

    private static CsvRecordFormat.@Nullable Quotes getQuotes(CsvRecordFormat recordTemplate,
                                                              String valueText,
                                                              CsvRecordFormat.QuotationPolicy quotationPolicy) {
        CsvRecordFormat.@Nullable Quotes quote = ContainerUtil.getFirstItem(recordTemplate.quotes);
        if (quotationPolicy == CsvRecordFormat.QuotationPolicy.ALWAYS) {
            return quote;
        }
        else if (quotationPolicy == CsvRecordFormat.QuotationPolicy.AS_NEEDED) {
            return shouldQuote(recordTemplate, valueText, quote) ? quote : null;
        }
        else if (quotationPolicy == CsvRecordFormat.QuotationPolicy.NEVER) {
            return null;
        }
        throw new AssertionError("Unhandled quotation policy: " + quotationPolicy);
    }

    private static String escapeQuotes(CharSequence s, CsvRecordFormat.Quotes quotes) {
        boolean leftIsEmpty = quotes.leftQuote.isEmpty();
        boolean rightIsEmpty = quotes.rightQuote.isEmpty();
        List<String> escapedQuotes = ContainerUtil.filter(
            Arrays.asList(leftIsEmpty ? null : quotes.leftQuoteEscaped, rightIsEmpty ? null : quotes.rightQuoteEscaped),
            Objects::nonNull
        );
        List<String> unescapedQuotes = ContainerUtil.filter(
            Arrays.asList(leftIsEmpty ? null : quotes.leftQuote, rightIsEmpty ? null : quotes.rightQuote),
            Objects::nonNull
        );
        return StringUtil.replace(s.toString(), unescapedQuotes, escapedQuotes);
    }

    private static String valueSeparator(CsvRecordFormat template) {
        return template.valueSeparator;
    }

    private static boolean shouldQuote(CsvRecordFormat recordTemplate,
                                       String valueText,
                                       CsvRecordFormat.@Nullable Quotes quote) {
        return StringUtil.contains(valueText, recordTemplate.valueSeparator) ||
            StringUtil.contains(valueText, recordTemplate.recordSeparator) ||
            quote != null && StringUtil.contains(valueText, quote.rightQuote) ||
            quote != null && StringUtil.contains(valueText, quote.leftQuote) ||
            StringUtil.isNotEmpty(recordTemplate.prefix) && StringUtil.contains(valueText, recordTemplate.prefix) ||
            StringUtil.isNotEmpty(recordTemplate.suffix) && StringUtil.contains(valueText, recordTemplate.suffix) ||
            StringUtil.equals(valueText, recordTemplate.nullText);
    }

    public static CsvFormat setFirstRowIsHeader(CsvFormat currentFormat, boolean value) {
        String withHeader = " " + LocalizeValue.localizeTODO("with header").get();
        String withoutHeader = " " + LocalizeValue.localizeTODO("without header").get();
        List<CsvFormat> formats = CsvSettings.getSettings().getCsvFormats();
        boolean isTemp = ContainerUtil.find(formats, f -> f.id.equals(currentFormat.id)) == null;
        String newName = isTemp
            ? StringUtil.trimEnd(StringUtil.trimEnd(currentFormat.name, withHeader), withoutHeader)
            : currentFormat.name;
        newName += value ? withHeader : withoutHeader;
        CsvFormat newFormat =
            new CsvFormat(newName, currentFormat.dataRecord, value ? currentFormat.dataRecord : null, currentFormat.rowNumbers);
        @Nullable CsvFormat existingFormat = ContainerUtil.find(formats, f -> formatsSimilar(f, newFormat));
        return ObjectUtil.notNull(existingFormat, newFormat);
    }
}
