/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.execution.test.sm.runner;

import consulo.disposer.Disposer;
import consulo.execution.test.Printer;
import consulo.execution.test.ui.BaseTestsOutputConsoleView;
import consulo.execution.test.ui.TestsOutputConsolePrinter;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.execution.ui.console.Filter;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.Condition;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public final class TestProxyPrinterProvider {
    private final TestProxyFilterProvider myFilterProvider;
    private final BaseTestsOutputConsoleView myTestOutputConsoleView;

    public TestProxyPrinterProvider(
        @Nonnull BaseTestsOutputConsoleView testsOutputConsoleView,
        @Nonnull TestProxyFilterProvider filterProvider
    ) {
        myTestOutputConsoleView = testsOutputConsoleView;
        myFilterProvider = filterProvider;
    }

    @Nullable
    public Printer getPrinterByType(@Nonnull String nodeType, @Nonnull String nodeName, @Nullable String nodeArguments) {
        Filter filter = myFilterProvider.getFilter(nodeType, nodeName, nodeArguments);
        if (filter != null && !Disposer.isDisposed(myTestOutputConsoleView)) {
            return new HyperlinkPrinter(myTestOutputConsoleView, HyperlinkPrinter.ERROR_CONTENT_TYPE, filter);
        }
        return null;
    }

    private static class HyperlinkPrinter extends TestsOutputConsolePrinter {
        public static final Condition<ConsoleViewContentType> ERROR_CONTENT_TYPE =
            contentType -> ConsoleViewContentType.ERROR_OUTPUT == contentType;
        private static final String NL = "\n";

        private final Condition<? super ConsoleViewContentType> myContentTypeCondition;
        private final Filter myFilter;

        HyperlinkPrinter(
            @Nonnull BaseTestsOutputConsoleView testsOutputConsoleView,
            @Nonnull Condition<? super ConsoleViewContentType> contentTypeCondition,
            @Nonnull Filter filter
        ) {
            super(testsOutputConsoleView, testsOutputConsoleView.getProperties(), null);
            myContentTypeCondition = contentTypeCondition;
            myFilter = filter;
        }

        @Override
        public void print(String text, ConsoleViewContentType contentType) {
            if (contentType == null || !myContentTypeCondition.value(contentType)) {
                defaultPrint(text, contentType);
                return;
            }
            text = StringUtil.replace(text, "\r\n", NL, false);
            StringTokenizer tokenizer = new StringTokenizer(text, NL, true);
            while (tokenizer.hasMoreTokens()) {
                String line = tokenizer.nextToken();
                if (NL.equals(line)) {
                    defaultPrint(line, contentType);
                }
                else {
                    printLine(line, contentType);
                }
            }
        }

        private void defaultPrint(String text, ConsoleViewContentType contentType) {
            super.print(text, contentType);
        }

        private void printLine(@Nonnull String line, @Nonnull ConsoleViewContentType contentType) {
            Filter.Result result;
            try {
                result = myFilter.applyFilter(line, line.length());
            }
            catch (Throwable t) {
                throw new RuntimeException("Error while applying " + myFilter + " to '" + line + "'", t);
            }
            if (result != null) {
                List<Filter.ResultItem> items = sort(result.getResultItems());
                int lastOffset = 0;
                for (Filter.ResultItem item : items) {
                    defaultPrint(line.substring(lastOffset, item.getHighlightStartOffset()), contentType);
                    String linkText = line.substring(item.getHighlightStartOffset(), item.getHighlightEndOffset());
                    printHyperlink(linkText, item.getHyperlinkInfo());
                    lastOffset = item.getHighlightEndOffset();
                }
                defaultPrint(line.substring(lastOffset), contentType);
            }
            else {
                defaultPrint(line, contentType);
            }
        }

        @Nonnull
        private static List<Filter.ResultItem> sort(@Nonnull List<Filter.ResultItem> items) {
            if (items.size() <= 1) {
                return items;
            }
            List<Filter.ResultItem> copy = new ArrayList<>(items);
            Collections.sort(copy, Comparator.comparingInt(Filter.ResultItem::getHighlightStartOffset));
            return copy;
        }
    }
}
