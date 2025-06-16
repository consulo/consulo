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
package consulo.execution.test.sm.runner.state;

import consulo.execution.test.CompositePrintable;
import consulo.execution.test.Printer;
import consulo.execution.test.sm.localize.SMTestLocalize;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.localize.LocalizeValue;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nullable;

/**
 * @author Roman Chernyatchik
 */
public class TestIgnoredState extends AbstractState {
    private final LocalizeValue myText;
    private final String myStacktrace;

    public TestIgnoredState(String ignoredComment, @Nullable String stackTrace) {
        LocalizeValue ignoredMsg = StringUtil.isEmpty(ignoredComment)
            ? SMTestLocalize.smTestRunnerStatesTestIsIgnored()
            : LocalizeValue.of(ignoredComment);
        myText = ignoredMsg.map((localizeManager, value) -> CompositePrintable.NEW_LINE + value);
        myStacktrace = stackTrace == null ? null : stackTrace + CompositePrintable.NEW_LINE;
    }

    @Override
    public boolean isInProgress() {
        return false;
    }

    @Override
    public boolean isDefect() {
        return true;
    }

    @Override
    public boolean wasLaunched() {
        return true;
    }

    @Override
    public boolean isFinal() {
        return true;
    }

    @Override
    public boolean wasTerminated() {
        return false;
    }

    @Override
    public Magnitude getMagnitude() {
        return Magnitude.IGNORED_INDEX;
    }

    @Override
    public void printOn(Printer printer) {
        super.printOn(printer);

        printer.print(myText.get(), ConsoleViewContentType.SYSTEM_OUTPUT);
        if (StringUtil.isEmptyOrSpaces(myStacktrace)) {
            printer.print(CompositePrintable.NEW_LINE, ConsoleViewContentType.SYSTEM_OUTPUT);
        }
        else {
            printer.print(CompositePrintable.NEW_LINE, ConsoleViewContentType.ERROR_OUTPUT);
            printer.mark();
            printer.print(myStacktrace, ConsoleViewContentType.ERROR_OUTPUT);
        }
    }


    @Override
    public String toString() {
        //noinspection HardCodedStringLiteral
        return "TEST IGNORED";
    }
}