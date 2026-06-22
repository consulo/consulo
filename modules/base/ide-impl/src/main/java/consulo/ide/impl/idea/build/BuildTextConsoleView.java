// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build;

import consulo.build.ui.BuildConsoleView;
import consulo.build.ui.FilePosition;
import consulo.build.ui.event.*;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.execution.ui.console.Filter;
import consulo.execution.ui.console.LazyFileHyperlinkInfo;
import consulo.ide.impl.idea.execution.impl.ConsoleViewImpl;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.localize.LocalizeValue;
import consulo.process.ProcessOutputTypes;
import consulo.process.util.AnsiEscapeDecoder;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class BuildTextConsoleView extends ConsoleViewImpl implements BuildConsoleView, AnsiEscapeDecoder.ColoredTextAcceptor {
    private final AnsiEscapeDecoder myAnsiEscapeDecoder = new AnsiEscapeDecoder();

    public BuildTextConsoleView(Project project, boolean viewer, List<Filter> executionFilters) {
        super(project, GlobalSearchScope.allScope(project), viewer, true);
        executionFilters.forEach(this::addMessageFilter);
    }

    @Override
    public void onEvent(Object buildId, BuildEvent event) {
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
                    if (position.getStartLine() > 0) {
                        fileLink.append(":").append(position.getStartLine() + 1);
                    }
                    if (position.getStartColumn() > 0) {
                        fileLink.append(":").append(position.getStartColumn() + 1);
                    }
                    print(fileLink.toString(), ConsoleViewContentType.NORMAL_OUTPUT,
                        new LazyFileHyperlinkInfo(
                            getProject(),
                            position.getFile().getPath(),
                            position.getStartLine(),
                            position.getStartColumn()
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

    public void onEvent(OutputBuildEvent event) {
        append(event.getMessage(), event.isStdOut());
    }

    public boolean appendEventResult(@Nullable EventResult eventResult) {
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
            if (details.isEmpty() || details.isEmpty()) {
                return false;
            }
            BuildConsoleUtils.printDetails(this, null, details);
            hasChanged = true;
        }
        return hasChanged;
    }

    public boolean append(Failure failure) {
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

    public void append(LocalizeValue text, boolean isStdOut) {
        append(text.get(), isStdOut);
    }

    public void append(String text, boolean isStdOut) {
        Key outputType = !isStdOut ? ProcessOutputTypes.STDERR : ProcessOutputTypes.STDOUT;
        myAnsiEscapeDecoder.escapeText(text, outputType, this);
    }

    @Override
    public void coloredTextAvailable(String text, Key attributes) {
        print(text, ConsoleViewContentType.getConsoleViewType(attributes));
    }
}

