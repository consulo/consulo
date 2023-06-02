// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.compiler.build;

import consulo.application.progress.ProgressIndicator;
import consulo.application.internal.ProgressIndicatorEx;
import consulo.build.ui.*;
import consulo.build.ui.event.MessageEvent;
import consulo.build.ui.issue.BuildIssue;
import consulo.build.ui.progress.BuildProgress;
import consulo.build.ui.progress.BuildProgressDescriptor;
import consulo.compiler.CompilerMessage;
import consulo.compiler.CompilerMessageCategory;
import consulo.compiler.ExitStatus;
import consulo.execution.ui.console.RegexpFilter;
import consulo.execution.ui.console.UrlFilter;
import consulo.ide.impl.idea.build.FileNavigatable;
import consulo.ide.impl.idea.compiler.impl.CompilerPropertiesAction;
import consulo.ide.impl.idea.compiler.impl.ExcludeFromCompileAction;
import consulo.ide.impl.idea.compiler.progress.BuildViewService;
import consulo.ide.impl.idea.compiler.progress.ModuleLinkFilter;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.navigation.OpenFileDescriptor;
import consulo.project.Project;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.IdeActions;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.util.*;

/**
 * @author VISTALL
 * @since 28/11/2021
 * <p>
 * Based on consulo.ide.impl.idea.compiler.progress.BuildOutputService idea version but in our platform
 */
public class BuildViewServiceImpl implements BuildViewService {
  private static class ConsolePrinter {
    @Nonnull
    private final BuildProgress<BuildProgressDescriptor> progress;
    private volatile boolean isNewLinePosition = true;

    private ConsolePrinter(@Nonnull BuildProgress<BuildProgressDescriptor> progress) {
      this.progress = progress;
    }

    private void print(@Nonnull @Nls String message, @Nonnull MessageEvent.Kind kind) {
      String text = wrapWithAnsiColor(kind, message);
      if (!isNewLinePosition && !consulo.ide.impl.idea.openapi.util.text.StringUtil.startsWithChar(message, '\r')) {
        text = '\n' + text;
      }
      isNewLinePosition = consulo.ide.impl.idea.openapi.util.text.StringUtil.endsWithLineBreak(message);
      progress.output(text, kind != MessageEvent.Kind.ERROR);
    }

    @Nls
    private static String wrapWithAnsiColor(MessageEvent.Kind kind, @Nls String message) {
      if (kind == MessageEvent.Kind.SIMPLE) return message;
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
      final String ansiReset = ANSI_RESET;
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

  public BuildViewServiceImpl(Project project, UUID sessionId, String contentName) {
    myProject = project;
    mySessionId = sessionId;
    myContentName = contentName;
    myBuildProgress = BuildViewManager.getInstance(project).createBuildProgress();
    myConsolePrinter = new ConsolePrinter(myBuildProgress);
  }

  @Override
  public void onStart(Object sessionId, long startCompilationStamp, Runnable restartWork, ProgressIndicator indicator) {
    List<AnAction> contextActions = getContextActions();

    DefaultBuildDescriptor buildDescriptor = new DefaultBuildDescriptor(mySessionId, myContentName, StringUtil.notNullize(myProject.getBasePath()), startCompilationStamp)
            //.withRestartActions(restartActions.toArray(AnAction.EMPTY_ARRAY))
            .withAction(new CompilerPropertiesAction()).withExecutionFilter(new ModuleLinkFilter(myProject))
            .withExecutionFilter(new RegexpFilter(myProject, RegexpFilter.FILE_PATH_MACROS + ":" + RegexpFilter.LINE_MACROS + ":" + RegexpFilter.COLUMN_MACROS))
            .withExecutionFilter(new UrlFilter(myProject)).withContextAction(node -> {
              return new ExcludeFromCompileAction(myProject) {
                @Override
                @Nullable
                protected VirtualFile getFile() {
                  List<Navigatable> navigatables = node.getNavigatables();
                  if (navigatables.size() != 1) return null;
                  Navigatable navigatable = navigatables.get(0);
                  if (navigatable instanceof OpenFileDescriptor) {
                    return ((OpenFileDescriptor)navigatable).getFile();
                  }
                  else if (navigatable instanceof FileNavigatable) {
                    OpenFileDescriptor fileDescriptor = ((FileNavigatable)navigatable).getFileDescriptor();
                    return fileDescriptor != null ? fileDescriptor.getFile() : null;
                  }
                  return null;
                }
              };
            }).withContextActions(contextActions.toArray(AnAction.EMPTY_ARRAY));

    myBuildProgress.start(new BuildProgressDescriptor() {
      @Nonnull
      @Override
      public String getTitle() {
        return buildDescriptor.getTitle();
      }

      @Override
      public
      @Nonnull
      BuildDescriptor getBuildDescriptor() {
        return buildDescriptor;
      }
    });

    addIndicatorDelegate(indicator);
  }

  private void addIndicatorDelegate(ProgressIndicator indicator) {
    if (!(indicator instanceof ProgressIndicatorEx)) {
      return;
    }
    ((ProgressIndicatorEx)indicator).addStateDelegate(new DummyProgressIndicator() {
      private final Map<String, Set<String>> mySeenMessages = new HashMap<>();
      private LocalizeValue lastMessage = LocalizeValue.empty();
      private Stack<LocalizeValue> myTextStack;

      @Override
      public void setTextValue(@Nonnull LocalizeValue text) {
        addIndicatorNewMessagesAsBuildOutput(text);
      }

      @Override
      public void pushState() {
        getTextStack().push(indicator.getTextValue());
      }

      @Override
      public void setFraction(double fraction) {
        myBuildProgress.progress(lastMessage.get(), 100, (long)(fraction * 100), "%");
      }

      @Nonnull
      private Stack<LocalizeValue> getTextStack() {
        Stack<LocalizeValue> stack = myTextStack;
        if (stack == null) myTextStack = stack = new Stack<>();
        return stack;
      }

      private void addIndicatorNewMessagesAsBuildOutput(@Nls LocalizeValue msg) {
        Stack<LocalizeValue> textStack = getTextStack();
        if (!textStack.isEmpty() && msg.equals(textStack.peek())) {
          textStack.pop();
          return;
        }
        if (StringUtil.isEmptyOrSpaces(msg.getValue()) || msg.equals(lastMessage)) return;
        lastMessage = msg;

        String msgText = msg.get();
        int start = msgText.indexOf("[");
        if (start >= 1) {
          int end = msgText.indexOf(']', start + 1);
          if (end != -1) {
            String buildTargetNameCandidate = msgText.substring(start + 1, end);
            Set<String> targets = mySeenMessages.computeIfAbsent(buildTargetNameCandidate, unused -> new HashSet<>());
            boolean isSeenMessage = !targets.add(msgText.substring(0, start));
            if (isSeenMessage) return;
          }
        }
        myConsolePrinter.print(msgText, MessageEvent.Kind.SIMPLE);
      }
    });
  }

  @Override
  public void onEnd(Object sessionId, ExitStatus exitStatus, long endBuildStamp) {
    String message;
    if (exitStatus == ExitStatus.ERRORS) {
      message = BuildBundle.message("build.messages.failed", consulo.ide.impl.idea.openapi.util.text.StringUtil.wordsToBeginFromLowerCase(myContentName));
      myBuildProgress.fail(endBuildStamp, message);
    }
    else if (exitStatus == ExitStatus.CANCELLED) {
      message = BuildBundle.message("build.messages.cancelled", consulo.ide.impl.idea.openapi.util.text.StringUtil.wordsToBeginFromLowerCase(myContentName));
      myBuildProgress.cancel(endBuildStamp, message);
    }
    else {
      boolean isUpToDate = exitStatus == ExitStatus.UP_TO_DATE;
      //if (CompilerBundle.message("classes.up.to.date.check").equals(myContentName)) {
      //  if (isUpToDate) {
      //    myConsolePrinter.print(CompilerBundle.message("status.all.up.to.date"), MessageEvent.Kind.SIMPLE);
      //  }
      //  else {
      //    myConsolePrinter.print(CompilerBundle.message("compiler.build.messages.classes.check.outdated"), MessageEvent.Kind.SIMPLE);
      //  }
      //}
      message = BuildBundle.message("build.messages.finished", consulo.ide.impl.idea.openapi.util.text.StringUtil.wordsToBeginFromLowerCase(myContentName));
      myBuildProgress.finish(endBuildStamp, isUpToDate, message);
    }
  }

  @Override
  public void addMessage(Object sessionId, CompilerMessage compilerMessage) {
    MessageEvent.Kind kind = convertCategory(compilerMessage.getCategory());
    VirtualFile virtualFile = compilerMessage.getVirtualFile();
    Navigatable navigatable = compilerMessage.getNavigatable();
    String title = getMessageTitle(compilerMessage);
    BuildIssue issue = buildIssue(compilerMessage.getModuleNames(), title, compilerMessage.getMessage(), kind, virtualFile, navigatable);
    if (issue != null) {
      myBuildProgress.buildIssue(issue, kind);
    }
    else if (virtualFile != null) {
      File file = VfsUtilCore.virtualToIoFile(virtualFile);
      FilePosition filePosition;
      if (navigatable instanceof OpenFileDescriptorImpl) {
        OpenFileDescriptorImpl fileDescriptor = (OpenFileDescriptorImpl)navigatable;
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

  private static String getMessageTitle(@Nonnull CompilerMessage compilerMessage) {
    String message = null;
    String[] messages = consulo.ide.impl.idea.openapi.util.text.StringUtil.splitByLines(compilerMessage.getMessage());
    if (messages.length > 1) {
      final String line0 = messages[0];
      final String line1 = messages[1];
      final int colonIndex = line1.indexOf(':');
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
    return consulo.ide.impl.idea.openapi.util.text.StringUtil.trimEnd(consulo.ide.impl.idea.openapi.util.text.StringUtil.trimStart(message, "java: "), '.'); //NON-NLS
  }

  @Nullable
  private BuildIssue buildIssue(@Nonnull Collection<String> moduleNames,
                                @Nonnull String title,
                                @Nonnull String message,
                                @Nonnull MessageEvent.Kind kind,
                                @Nullable VirtualFile virtualFile,
                                @Nullable Navigatable navigatable) {
    // TODO [VISTALL] use ep? like in idea
    return null;
  }

  @NotNull
  private static List<AnAction> getContextActions() {
    List<AnAction> contextActions = new ArrayList<>();
    ActionGroup compilerErrorsViewPopupGroup = (ActionGroup)ActionManager.getInstance().getAction(IdeActions.GROUP_COMPILER_ERROR_VIEW_POPUP);
    if (compilerErrorsViewPopupGroup != null) {
      Collections.addAll(contextActions, compilerErrorsViewPopupGroup.getChildren(null));
    }
    return contextActions;
  }

  @Nonnull
  private static MessageEvent.Kind convertCategory(@Nonnull CompilerMessageCategory category) {
    switch (category) {
      case ERROR:
        return MessageEvent.Kind.ERROR;
      case WARNING:
        return MessageEvent.Kind.WARNING;
      case INFORMATION:
        return MessageEvent.Kind.INFO;
      case STATISTICS:
        return MessageEvent.Kind.STATISTICS;
      default:
        return MessageEvent.Kind.SIMPLE;
    }
  }

  @Override
  public void onProgressChange(Object sessionId, ProgressIndicator indicator) {

  }

  @Override
  public void registerCloseAction(Runnable onClose) {

  }
}
