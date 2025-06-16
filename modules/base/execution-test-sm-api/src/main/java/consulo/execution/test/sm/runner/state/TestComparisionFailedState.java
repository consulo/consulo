/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.test.sm.runner.state;

import consulo.execution.test.CompositePrintable;
import consulo.execution.test.Printer;
import consulo.execution.test.stacktrace.DiffHyperlink;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.process.ProcessOutputTypes;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;

/**
 * @author Roman.Chernyatchik
 */
public class TestComparisionFailedState extends TestFailedState {
    private final String myErrorMsgPresentation;
    private final String myStacktracePresentation;
    private DiffHyperlink myHyperlink;
    private boolean myToDeleteExpectedFile;
    private boolean myToDeleteActualFile;

    public TestComparisionFailedState(
        @Nullable String localizedMessage,
        @Nullable String stackTrace,
        @Nonnull String actualText,
        @Nonnull String expectedText
    ) {
        this(localizedMessage, stackTrace, actualText, expectedText, null);
    }

    public TestComparisionFailedState(
        @Nullable String localizedMessage,
        @Nullable String stackTrace,
        @Nonnull String actualText,
        @Nonnull String expectedText,
        @Nullable String filePath
    ) {
        this(localizedMessage, stackTrace, actualText, expectedText, filePath, null);
    }

    public TestComparisionFailedState(
        @Nullable String localizedMessage,
        @Nullable String stackTrace,
        @Nonnull String actualText,
        @Nonnull String expectedText,
        @Nullable String expectedFilePath,
        @Nullable String actualFilePath
    ) {
        super(localizedMessage, stackTrace);
        myHyperlink = new DiffHyperlink(expectedText, actualText, expectedFilePath, actualFilePath, true);

        myErrorMsgPresentation = StringUtil.isEmptyOrSpaces(localizedMessage) ? "" : localizedMessage;
        myStacktracePresentation = StringUtil.isEmptyOrSpaces(stackTrace) ? "" : stackTrace;
    }

    @Override
    public void printOn(Printer printer) {
        printer.print(CompositePrintable.NEW_LINE, ConsoleViewContentType.ERROR_OUTPUT);
        printer.mark();

        // Error msg
        printer.printWithAnsiColoring(myErrorMsgPresentation, ProcessOutputTypes.STDERR);

        // Diff link
        myHyperlink.printOn(printer);

        // Stacktrace
        printer.print(CompositePrintable.NEW_LINE, ConsoleViewContentType.ERROR_OUTPUT);
        printer.printWithAnsiColoring(myStacktracePresentation, ProcessOutputTypes.STDERR);
        printer.print(CompositePrintable.NEW_LINE, ConsoleViewContentType.ERROR_OUTPUT);
    }

    @Nullable
    public DiffHyperlink getHyperlink() {
        return myHyperlink;
    }

    public void setToDeleteExpectedFile(boolean expectedTemp) {
        myToDeleteExpectedFile = expectedTemp;
    }

    public void setToDeleteActualFile(boolean actualTemp) {
        myToDeleteActualFile = actualTemp;
    }

    @Override
    public void dispose() {
        if (myToDeleteActualFile) {
            FileUtil.delete(new File(myHyperlink.getActualFilePath()));
        }
        if (myToDeleteExpectedFile) {
            FileUtil.delete(new File(myHyperlink.getFilePath()));
        }
    }
}
