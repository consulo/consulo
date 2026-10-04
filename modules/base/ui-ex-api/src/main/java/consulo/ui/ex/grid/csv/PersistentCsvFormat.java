// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid.csv;

import consulo.util.collection.ContainerUtil;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Tag;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Tag("csv-format")
public class PersistentCsvFormat {
    @Attribute("name")
    public @Nullable String name;
    @Attribute("id")
    public String id = UUID.randomUUID().toString();
    @Tag("data")
    public @Nullable Record data;
    @Tag("header")
    public @Nullable Record header;
    @Attribute("row-numbers")
    public boolean rowNumbers;


    @SuppressWarnings("unused")
    public PersistentCsvFormat() {
        this("");
    }

    public PersistentCsvFormat(CsvFormat format) {
        this(format.id, format.name, format);
    }

    public PersistentCsvFormat(String id, String name, CsvFormat format) {
        this(name);
        this.id = id;
        this.data = new Record(format.dataRecord);
        this.header = format.headerRecord != null ? new Record(format.headerRecord) : null;
        this.rowNumbers = format.rowNumbers;
    }

    private PersistentCsvFormat(String name) {
        this.name = name;
    }

    public @Nullable CsvFormat immutable() {
        if (!isValid()) {
            return null;
        }

        String name = Objects.requireNonNull(this.name);
        CsvRecordFormat dataFormat = Objects.requireNonNull(Objects.requireNonNull(data).immutable());
        @Nullable CsvRecordFormat header = this.header != null ? Objects.requireNonNull(this.header.immutable()) : null;
        return new CsvFormat(name, dataFormat, header, id, rowNumbers);
    }

    private boolean isValid() {
        return name != null &&
            data != null && data.isValid() &&
            (header == null || header.isValid());
    }

    @Tag("record-format")
    public static class Record {
        private static final String QUOTATION_POLICY_NEVER = "never";
        private static final String QUOTATION_POLICY_ALWAYS = "always";
        private static final String QUOTATION_POLICY_AS_NEEDED = "as needed";

        @Attribute("prefix")
        public @Nullable String prefix;
        @Attribute("suffix")
        public @Nullable String suffix;
        @Attribute("nullText")
        public @Nullable String nullText;
        @Attribute("quotationPolicy")
        public @Nullable String quotationPolicy;
        @Attribute("valueSeparator")
        public @Nullable String valueSeparator;
        @Attribute("recordSeparator")
        public @Nullable String recordSeparator;
        @Attribute("trimWhitespace")
        public boolean trimWhitespace;

        @Tag("quotation")
        @AbstractCollection(surroundWithTag = false)
        public List<Quotes> quotes = new ArrayList<>();

        @SuppressWarnings("unused")
        public Record() {
        }

        public Record(CsvRecordFormat record) {
            prefix = record.prefix;
            suffix = record.suffix;
            nullText = record.nullText;
            quotationPolicy = valueOfQuotationPolicy(record.quotationPolicy);
            valueSeparator = record.valueSeparator;
            recordSeparator = record.recordSeparator;
            trimWhitespace = record.trimWhitespace;
            quotes = ContainerUtil.map(record.quotes, quotes1 -> new Quotes(quotes1));
        }

        private CsvRecordFormat immutable() {
            CsvRecordFormat.QuotationPolicy qp = Objects.requireNonNull(quotationPolicy(quotationPolicy));
            List<CsvRecordFormat.Quotes> immutableQuotes = ContainerUtil.map(
                quotes,
                quotes1 -> new CsvRecordFormat.Quotes(Objects.requireNonNull(quotes1.left),
                    Objects.requireNonNull(quotes1.right),
                    Objects.requireNonNull(quotes1.leftEscaped),
                    Objects.requireNonNull(quotes1.rightEscaped))
            );

            // only called on a valid record, whose parts are all set
            return new CsvRecordFormat(Objects.requireNonNull(prefix),
                Objects.requireNonNull(suffix),
                nullText,
                immutableQuotes,
                qp,
                Objects.requireNonNull(valueSeparator),
                Objects.requireNonNull(recordSeparator),
                trimWhitespace);
        }

        private boolean isValid() {
            return prefix != null &&
                suffix != null &&
                quotationPolicy(quotationPolicy) != null &&
                valueSeparator != null &&
                recordSeparator != null &&
                quotes != null &&
                ContainerUtil.find(quotes, quotes1 -> quotes1 == null || !quotes1.isValid()) == null;
        }

        private static CsvRecordFormat.@Nullable QuotationPolicy quotationPolicy(@Nullable String qp) {
            return QUOTATION_POLICY_AS_NEEDED.equals(qp) ? CsvRecordFormat.QuotationPolicy.AS_NEEDED :
                QUOTATION_POLICY_ALWAYS.equals(qp) ? CsvRecordFormat.QuotationPolicy.ALWAYS :
                    QUOTATION_POLICY_NEVER.equals(qp) ? CsvRecordFormat.QuotationPolicy.NEVER : null;
        }

        private static String valueOfQuotationPolicy(CsvRecordFormat.QuotationPolicy policy) {
            return switch (policy) {
                case ALWAYS -> QUOTATION_POLICY_ALWAYS;
                case AS_NEEDED -> QUOTATION_POLICY_AS_NEEDED;
                case NEVER -> QUOTATION_POLICY_NEVER;
            };
        }
    }

    @Tag("quotes")
    public static class Quotes {
        @Attribute("left")
        public @Nullable String left;
        @Attribute("right")
        public @Nullable String right;
        @Attribute("leftEscaped")
        public @Nullable String leftEscaped;
        @Attribute("rightEscaped")
        public @Nullable String rightEscaped;

        @SuppressWarnings("unused")
        public Quotes() {
        }

        public Quotes(CsvRecordFormat.Quotes quotes) {
            left = quotes.leftQuote;
            right = quotes.rightQuote;
            leftEscaped = quotes.leftQuoteEscaped;
            rightEscaped = quotes.rightQuoteEscaped;
        }

        private boolean isValid() {
            return left != null && right != null && leftEscaped != null && rightEscaped != null;
        }
    }
}
