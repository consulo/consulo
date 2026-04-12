// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.compiler.impl.internal;

import consulo.application.internal.ProgressIndicatorEx;
import consulo.application.progress.ProgressIndicator;
import consulo.build.ui.*;
import consulo.build.ui.event.FailureResult;
import consulo.build.ui.event.FinishEvent;
import consulo.build.ui.event.MessageEvent;
import consulo.build.ui.impl.internal.event.FileNavigatable;
import consulo.build.ui.issue.BuildIssue;
import consulo.build.ui.localize.BuildLocalize;
import consulo.build.ui.progress.BuildProgress;
import consulo.build.ui.progress.BuildProgressDescriptor;
import consulo.compiler.CompilerMessage;
import consulo.compiler.CompilerMessageCategory;
import consulo.compiler.ExitStatus;
import consulo.compiler.action.ExcludeFromCompileAction;
import consulo.compiler.impl.internal.action.CompilerPropertiesAction;
import consulo.execution.ui.console.RegexpFilter;
import consulo.execution.ui.console.UrlFilter;
import consulo.language.editor.wolfAnalyzer.WolfTheProblemSolver;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.navigation.OpenFileDescriptor;
import consulo.project.Project;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * Based on consulo.ide.impl.idea.compiler.progress.BuildOutputService idea version but in our platform.
 *
 * @author VISTALL
 * @since 2021-11-28
 */
public class BuildViewServiceImpl {
    private static class ConsolePrinter {
        
        private final BuildProgress<BuildProgressDescriptor> progress;
        private volatile boolean isNewLinePosition = true;

        private ConsolePrinter(BuildProgress<BuildProgressDescriptor> progress) {
            this.progress = progress;
        }

        private void print(String message, MessageEvent.Kind kind) {
            String text = wrapWithAnsiColor(kind, message);
            if (!isNewLinePosition && !StringUtil.startsWithChar(message, '\r')) {
                text = '\n' + text;
            }
            isNewLinePosition = StringUtil.endsWithLineBreak(message);
            progress.output(text, kind != MessageEvent.Kind.ERROR);
        }

        private static String wrapWithAnsiColor(MessageEvent.Kind kind, String message) {
            if (kind == MessageEvent.Kind.SIMPLE) {
                return message;
            }
            String color;
            if (kind == MessageEvent.Kind.ERROR) {
                color = ANSI_RED;
            }
            else if (kind == MessageEvent.Kind.WARNING) {
                color = ANSI_YELLOW;
            }
            else {
                color = ANSI_BOLD;
            }
            String ansiReset = ANSI_RESET;
            return color + message + ansiReset;
        }
    }

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BOLD = "\u001b[1m";

    private final Project myProject;
    private final UUID mySessionId;
    private final String myContentName;

    private final BuildProgress<BuildProgressDescriptor> myBuildProgress;
    private final ConsolePrinter myConsolePrinter;

    public BuildViewServiceImpl(Project project, UUID sessionId, String contentName, CompileCounters counters) {
        myProject = project;
        mySessionId = sessionId;
        myContentName = contentName;
        myBuildProgress = BuildViewManager.getInstance(project).createBuildProgress();
        myConsolePrinter = new ConsolePrinter(myBuildProgress);

        myBuildProgress.addListener((buildId, event) -> {
            if (event instanceof MessageEvent messageEvent) {
                CompilerMessageCategory category = toCompilerMessageCategory(messageEvent.getKind());

                counters.inc(category);

                Navigatable navigatable = messageEvent.getNavigatable(project);
                if (navigatable instanceof OpenFileDescriptor openFileDescriptor) {
                    VirtualFile file = openFileDescriptor.getFile();

                    counters.addErrorFile(file);

                    WolfTheProblemSolver wolf = WolfTheProblemSolver.getInstance(myProject);

                    wolf.queue(file);
                }
            } else if (event instanceof FinishEvent finishEvent) {
                if (finishEvent.getResult() instanceof FailureResult) {
                    counters.inc(CompilerMessageCategory.ERROR);
                }
            }
        });
    }

    public void openBuildToolWindow() {
        BuildContentManager contentManager = BuildContentManager.getInstance(myProject);

        myProject.getUIAccess().execute(() -> {
            ToolWindow toolWindow = contentManager.getOrCreateToolWindow();

            toolWindow.activate(null);
        });
    }

    private CompilerMessageCategory toCompilerMessageCategory(MessageEvent.Kind kind) {
        return switch (kind) {
            case ERROR -> CompilerMessageCategory.ERROR;
            case WARNING -> CompilerMessageCategory.WARNING;
            case INFO, SIMPLE -> CompilerMessageCategory.INFORMATION;
            case STATISTICS -> CompilerMessageCategory.STATISTICS;
        };
    }

    public BuildProgress<BuildProgressDescriptor> getBuildProgress() {
        return myBuildProgress;
    }

    public void onStart(Object sessionId, long startCompilationStamp, Runnable restartWork, ProgressIndicator indicator) {
        List<AnAction> contextActions = getContextActions();

        DefaultBuildDescriptor buildDescriptor =
            new DefaultBuildDescriptor(mySessionId, myContentName, StringUtil.notNullize(myProject.getBasePath()), startCompilationStamp)
                //.withRestartActions(restartActions.toArray(AnAction.EMPTY_ARRAY))
                .withAction(new CompilerPropertiesAction()).withExecutionFilter(new ModuleLinkFilter(myProject))
                .withExecutionFilter(new RegexpFilter(
                    myProject,
                    RegexpFilter.FILE_PATH_MACROS + ":" + RegexpFilter.LINE_MACROS + ":" + RegexpFilter.COLUMN_MACROS
                ))
                .withExecutionFilter(new UrlFilter(myProject))
                .withContextAction(node -> new ExcludeFromCompileAction(myProject) {
                    @Override
                    protected @Nullable VirtualFile getFile() {
                        List<Navigatable> navigatables = node.getNavigatables();
                        if (navigatables.size() != 1) {
                            return null;
                        }
                        Navigatable navigatable = navigatables.get(0);
                        if (navigatable instanceof OpenFileDescriptor openFileDescriptor) {
                            return openFileDescriptor.getFile();
                        }
                        else if (navigatable instanceof FileNavigatable fileNavigatable) {
                            OpenFileDescriptor fileDescriptor = fileNavigatable.getFileDescriptor();
                            return fileDescriptor != null ? fileDescriptor.getFile() : null;
                        }
                        return null;
                    }
                })
                .withContextActions(contextActions.toArray(AnAction.EMPTY_ARRAY));

        myBuildProgress.start(new BuildProgressDescriptor() {
            
            @Override
            public String getTitle() {
                return buildDescriptor.getTitle();
            }

            @Override
            public
            
            BuildDescriptor getBuildDescriptor() {
                return buildDescriptor;
            }
        });

        addIndicatorDelegate(indicator);
    }

    private void addIndicatorDelegate(ProgressIndicator indicator) {
        if (!(indicator instanceof ProgressIndicatorEx progressIndicatorEx)) {
            return;
        }
        progressIndicatorEx.addStateDelegate(new DummyProgressIndicator() {
            private final Map<String, Set<String>> mySeenMessages = new HashMap<>();
            private LocalizeValue lastMessage = LocalizeValue.empty();
            private Stack<LocalizeValue> myTextStack;

            @Override
            public void setText(LocalizeValue text) {
                addIndicatorNewMessagesAsBuildOutput(text);
            }

            @Override
            public void pushState() {
                getTextStack().push(indicator.getText());
            }

            @Override
            public void setFraction(double fraction) {
                myBuildProgress.progress(lastMessage.get(), 100, (long) (fraction * 100), "%");
            }

            private Stack<LocalizeValue> getTextStack() {
                Stack<LocalizeValue> stack = myTextStack;
                if (stack == null) {
                    myTextStack = stack = new Stack<>();
                }
                return stack;
            }

            private void addIndicatorNewMessagesAsBuildOutput(LocalizeValue msg) {
                Stack<LocalizeValue> textStack = getTextStack();
                if (!textStack.isEmpty() && msg.equals(textStack.peek())) {
                    textStack.pop();
                    return;
                }
                if (StringUtil.isEmptyOrSpaces(msg.getValue()) || msg.equals(lastMessage)) {
                    return;
                }
                lastMessage = msg;

                String msgText = msg.get();
                int start = msgText.indexOf("[");
                if (start >= 1) {
                    int end = msgText.indexOf(']', start + 1);
                    if (end != -1) {
                        String buildTargetNameCandidate = msgText.substring(start + 1, end);
                        Set<String> targets = mySeenMessages.computeIfAbsent(buildTargetNameCandidate, unused -> new HashSet<>());
                        boolean isSeenMessage = !targets.add(msgText.substring(0, start));
                        if (isSeenMessage) {
                            return;
                        }
                    }
                }
                myConsolePrinter.print(msgText, MessageEvent.Kind.SIMPLE);
            }
        });
    }

    public void onEnd(Object sessionId, ExitStatus exitStatus, long endBuildStamp) {
        LocalizeValue message;
        if (exitStatus == ExitStatus.ERRORS) {
            message = BuildLocalize.buildMessagesFailed(StringUtil.wordsToBeginFromLowerCase(myContentName));
            myBuildProgress.fail(endBuildStamp, message.get());
        }
        else if (exitStatus == ExitStatus.CANCELLED) {
            message = BuildLocalize.buildMessagesCancelled(StringUtil.wordsToBeginFromLowerCase(myContentName));
            myBuildProgress.cancel(endBuildStamp, message.get());
        }
        else {
            boolean isUpToDate = exitStatus == ExitStatus.UP_TO_DATE;
            message = BuildLocalize.buildMessagesFinished(StringUtil.wordsToBeginFromLowerCase(myContentName));
            myBuildProgress.finish(endBuildStamp, isUpToDate, message.get());
        }
    }

    public void addMessage(Object sessionId, CompilerMessage compilerMessage) {
        MessageEvent.Kind kind = convertCategory(compilerMessage.getCategory());
        VirtualFile virtualFile = compilerMessage.getVirtualFile();
        Navigatable navigatable = compilerMessage.getNavigatable();
        String title = getMessageTitle(compilerMessage);
        BuildIssue issue = buildIssue(
            compilerMessage.getModuleNames(),
            title,
            compilerMessage.getMessage(),
            kind,
            virtualFile,
            navigatable
        );
        if (issue != null) {
            myBuildProgress.buildIssue(issue, kind);
        }
        else if (virtualFile != null) {
            File file = VirtualFileUtil.virtualToIoFile(virtualFile);
            FilePosition filePosition;
            if (navigatable instanceof OpenFileDescriptor fileDescriptor) {
                int column = fileDescriptor.getColumn();
                int line = fileDescriptor.getLine();
                filePosition = new FilePosition(file, line, column);
            }
            else {
                filePosition = new FilePosition(file, 0, 0);
            }

            myBuildProgress.fileMessage(title, compilerMessage.getMessage(), kind, filePosition);
        }
        else {
            if (kind == MessageEvent.Kind.ERROR || kind == MessageEvent.Kind.WARNING) {
                myBuildProgress.message(title, compilerMessage.getMessage(), kind, navigatable);
            }
            myConsolePrinter.print(compilerMessage.getMessage(), kind);
        }
    }

    private static String getMessageTitle(CompilerMessage compilerMessage) {
        String message = null;
        String[] messages = StringUtil.splitByLines(compilerMessage.getMessage());
        if (messages.length > 1) {
            String line0 = messages[0];
            String line1 = messages[1];
            int colonIndex = line1.indexOf(':');
            if (colonIndex > 0) {
                String part1 = line1.substring(0, colonIndex).trim();
                // extract symbol information from the compiler message of the following template:
                // java: cannot find symbol
                //  symbol:   class AClass
                //  location: class AnotherClass
                if ("symbol".equals(part1)) {
                    String symbol = line1.substring(colonIndex + 1).trim();
                    message = line0 + " " + symbol;
                }
            }
        }
        if (message == null) {
            message = messages[0];
        }
        return StringUtil.trimEnd(StringUtil.trimStart(message, "java: "), '.'); //NON-NLS
    }

    private @Nullable BuildIssue buildIssue(
        Collection<String> moduleNames,
        String title,
        String message,
        MessageEvent.Kind kind,
        @Nullable VirtualFile virtualFile,
        @Nullable Navigatable navigatable
    ) {
        // TODO [VISTALL] use ep? like in idea
        return null;
    }

    private static List<AnAction> getContextActions() {
        List<AnAction> contextActions = new ArrayList<>();
        ActionGroup compilerErrorsViewPopupGroup =
            (ActionGroup) ActionManager.getInstance().getAction(IdeActions.GROUP_COMPILER_ERROR_VIEW_POPUP);
        if (compilerErrorsViewPopupGroup != null) {
            Collections.addAll(contextActions, compilerErrorsViewPopupGroup.getChildren(null));
        }
        return contextActions;
    }

    
    private static MessageEvent.Kind convertCategory(CompilerMessageCategory category) {
        return switch (category) {
            case ERROR -> MessageEvent.Kind.ERROR;
            case WARNING -> MessageEvent.Kind.WARNING;
            case INFORMATION -> MessageEvent.Kind.INFO;
            case STATISTICS -> MessageEvent.Kind.STATISTICS;
        };
    }
}
