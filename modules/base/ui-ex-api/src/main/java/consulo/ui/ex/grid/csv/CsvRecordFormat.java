// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid.csv;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class CsvRecordFormat {
    public final String prefix;
    public final String suffix;
    public final @Nullable String nullText;
    public final List<Quotes> quotes;
    public final QuotationPolicy quotationPolicy;
    public final String valueSeparator;
    public final String recordSeparator;
    public final boolean trimWhitespace;

    public CsvRecordFormat(String prefix,
                           String suffix,
                           @Nullable String nullText,
                           List<Quotes> quotes,
                           QuotationPolicy quotationPolicy,
                           String valueSeparator,
                           String recordSeparator,
                           boolean trimWhitespace) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.nullText = nullText;
        this.quotes = Collections.unmodifiableList(quotes);
        this.quotationPolicy = quotationPolicy;
        this.valueSeparator = valueSeparator;
        this.recordSeparator = recordSeparator;
        this.trimWhitespace = trimWhitespace;
    }

    public CsvRecordFormat withTrimWhitespaces(boolean trimWhitespaces) {
        if (trimWhitespace == trimWhitespaces) {
            return this;
        }
        return new CsvRecordFormat(
            prefix, suffix, nullText,
            quotes, quotationPolicy,
            valueSeparator, recordSeparator,
            trimWhitespaces
        );
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CsvRecordFormat format)) {
            return false;
        }

        if (trimWhitespace != format.trimWhitespace) {
            return false;
        }
        if (!prefix.equals(format.prefix)) {
            return false;
        }
        if (!suffix.equals(format.suffix)) {
            return false;
        }
        if (!Objects.equals(nullText, format.nullText)) {
            return false;
        }
        if (!quotes.equals(format.quotes)) {
            return false;
        }
        if (quotationPolicy != format.quotationPolicy) {
            return false;
        }
        if (!valueSeparator.equals(format.valueSeparator)) {
            return false;
        }
        if (!recordSeparator.equals(format.recordSeparator)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = prefix.hashCode();
        result = 31 * result + suffix.hashCode();
        result = 31 * result + Objects.hashCode(nullText);
        result = 31 * result + quotes.hashCode();
        result = 31 * result + quotationPolicy.hashCode();
        result = 31 * result + valueSeparator.hashCode();
        result = 31 * result + recordSeparator.hashCode();
        result = 31 * result + (trimWhitespace ? 1 : 0);
        return result;
    }


    public static final class Quotes {
        public final String leftQuote;
        public final String rightQuote;
        public final String leftQuoteEscaped;
        public final String rightQuoteEscaped;

        public Quotes(String leftQuote,
                      String rightQuote,
                      String leftQuoteEscaped,
                      String rightQuoteEscaped) {
            this.leftQuote = leftQuote;
            this.rightQuote = rightQuote;
            this.leftQuoteEscaped = leftQuoteEscaped;
            this.rightQuoteEscaped = rightQuoteEscaped;
        }

        public boolean isQuoted(String s) {
            if (leftQuote.isEmpty() || rightQuote.isEmpty()) {
                return false;
            }
            return s.length() >= leftQuote.length() + rightQuote.length() &&
                s.startsWith(leftQuote) && s.endsWith(rightQuote);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Quotes quotes)) {
                return false;
            }

            if (!leftQuote.equals(quotes.leftQuote)) {
                return false;
            }
            if (!rightQuote.equals(quotes.rightQuote)) {
                return false;
            }
            if (!leftQuoteEscaped.equals(quotes.leftQuoteEscaped)) {
                return false;
            }
            if (!rightQuoteEscaped.equals(quotes.rightQuoteEscaped)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = leftQuote.hashCode();
            result = 31 * result + rightQuote.hashCode();
            result = 31 * result + leftQuoteEscaped.hashCode();
            result = 31 * result + rightQuoteEscaped.hashCode();
            return result;
        }
    }

    public enum QuotationPolicy {
        ALWAYS,
        AS_NEEDED,
        NEVER
    }
}
