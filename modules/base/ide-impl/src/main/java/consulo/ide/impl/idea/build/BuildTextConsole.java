// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build;

import consulo.build.ui.BuildConsoleView;
import consulo.build.ui.FilePosition;
import consulo.build.ui.event.*;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.execution.ui.console.LazyFileHyperlinkInfo;
import consulo.localize.LocalizeValue;
import consulo.process.ProcessOutputTypes;
import consulo.process.util.AnsiEscapeDecoder;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

/**
 * What a build says written out as text, whatever console the text lands in - the events are turned into lines
 * here and the console only has to print them.
 *
 * @author Vladislav.Soroka
 */
public interface BuildTextConsole extends ConsoleView, BuildConsoleView, AnsiEscapeDecoder.ColoredTextAcceptor {
    Project getProject();

    AnsiEscapeDecoder getAnsiEscapeDecoder();

    @Override
    default void onEvent(Object buildId, BuildEvent event) {
        switch (event) {
            case BuildIssueEvent bie -> BuildConsoleUtils.print(this, bie.getGroup(), bie.getIssue());
            case FileMessageEvent fme -> {
                boolean isStdOut = fme.getResult().getKind() != MessageEvent.Kind.ERROR;
                LocalizeValue description = fme.getDescription();
                if (description.isNotEmpty()) {
                    append(description, isStdOut);
                }
                else {
                    FilePosition position = fme.getFilePosition();
                    StringBuilder fileLink = new StringBuilder();
                    fileLink.append(position.getFile().getName());
                    if (position.startLine() > 0) {
                        fileLink.append(":").append(position.startLine() + 1);
                    }
                    if (position.startColumn() > 0) {
                        fileLink.append(":").append(position.startColumn() + 1);
                    }
                    printHyperlink(
                        fileLink.toString(),
                        new LazyFileHyperlinkInfo(
                            getProject(),
                            position.getFile().getPath(),
                            position.startLine(),
                            position.startColumn()
                        )
                    );
                    print(": ", ConsoleViewContentType.NORMAL_OUTPUT);
                    append(fme.getMessage(), isStdOut);
                }
            }
            case MessageEvent me -> appendEventResult(me.getResult());
            case FinishEvent fe -> appendEventResult(fe.getResult());
            case OutputBuildEvent obe -> onEvent(obe);
            default -> append(event.getDescription().orIfEmpty(event.getMessage()), true);
        }
    }

    default void onEvent(OutputBuildEvent event) {
        append(event.getMessage(), event.isStdOut());
    }

    default boolean appendEventResult(@Nullable EventResult eventResult) {
        if (eventResult == null) {
            return false;
        }
        boolean hasChanged = false;
        if (eventResult instanceof FailureResult failureResult) {
            List<? extends Failure> failures = failureResult.getFailures();
            if (failures.isEmpty()) {
                return false;
            }
            for (Iterator<? extends Failure> iterator = failures.iterator(); iterator.hasNext(); ) {
                Failure failure = iterator.next();
                if (append(failure)) {
                    hasChanged = true;
                }
                if (iterator.hasNext()) {
                    print("\n\n", ConsoleViewContentType.NORMAL_OUTPUT);
                }
            }
        }
        else if (eventResult instanceof MessageEventResult messageEventResult) {
            LocalizeValue details = messageEventResult.getDetails();
            if (details.isEmpty()) {
                return false;
            }
            BuildConsoleUtils.printDetails(this, null, details);
            hasChanged = true;
        }
        return hasChanged;
    }

    default boolean append(Failure failure) {
        LocalizeValue text = failure.getDescription().orIfEmpty(failure.getMessage());
        if (text.isEmpty() && failure.getError() != null) {
            text = LocalizeValue.ofNullable(failure.getError().getMessage());
        }
        if (text.isEmpty()) {
            return false;
        }
        BuildConsoleUtils.printDetails(this, failure, text);
        return true;
    }

    default void append(LocalizeValue text, boolean isStdOut) {
        append(text.get(), isStdOut);
    }

    default void append(String text, boolean isStdOut) {
        Key outputType = !isStdOut ? ProcessOutputTypes.STDERR : ProcessOutputTypes.STDOUT;
        getAnsiEscapeDecoder().escapeText(text, outputType, this);
    }

    @Override
    default void coloredTextAvailable(String text, Key attributes) {
        print(text, ConsoleViewContentType.getConsoleViewType(attributes));
    }
}
