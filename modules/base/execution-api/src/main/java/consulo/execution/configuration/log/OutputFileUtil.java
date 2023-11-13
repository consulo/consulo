/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.execution.configuration.log;

import consulo.application.ApplicationManager;
import consulo.execution.CommonProgramRunConfigurationParameters;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.ui.ExecutionConsole;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.Filter;
import consulo.execution.ui.console.HyperlinkInfo;
import consulo.fileEditor.FileEditorManager;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.process.ProcessHandler;
import consulo.process.ProcessOutputTypes;
import consulo.process.event.ProcessAdapter;
import consulo.process.event.ProcessEvent;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.function.Supplier;

/**
 * User: anna
 * Date: 10/20/11
 */
public class OutputFileUtil {
  private static final String CONSOLE_OUTPUT_FILE_MESSAGE = "Console output is saving to: ";

  private OutputFileUtil() {
  }

  public static File getOutputFile(@Nonnull final RunConfigurationBase configuration) {
    String outputFilePath = configuration.getOutputFilePath();
    if (outputFilePath != null) {
      final String filePath = FileUtil.toSystemDependentName(outputFilePath);
      File file = new File(filePath);
      if (configuration instanceof CommonProgramRunConfigurationParameters && !FileUtil.isAbsolute(filePath)) {
        String directory = ((CommonProgramRunConfigurationParameters)configuration).getWorkingDirectory();
        if (directory != null) {
          file = new File(new File(directory), filePath);
        }
      }
      return file;
    }
    return null;
  }

  public static void attachDumpListener(final RunConfigurationBase base, final ProcessHandler startedProcess, ExecutionConsole console) {
    if (base.isSaveOutputToFile()) {
      final String outputFilePath = base.getOutputFilePath();
      if (outputFilePath != null) {
        final String filePath = FileUtil.toSystemDependentName(outputFilePath);
        startedProcess.addProcessListener(new ProcessAdapter() {
          private PrintStream myOutput;

          @Override
          public void onTextAvailable(ProcessEvent event, Key outputType) {
            if (base.collectOutputFromProcessHandler() && myOutput != null && outputType != ProcessOutputTypes.SYSTEM) {
              myOutput.print(event.getText());
            }
          }

          @Override
          public void startNotified(ProcessEvent event) {
            try {
              myOutput = new PrintStream(new FileOutputStream(new File(filePath)));
            }
            catch (FileNotFoundException ignored) {
            }
            startedProcess.notifyTextAvailable(CONSOLE_OUTPUT_FILE_MESSAGE + filePath + "\n", ProcessOutputTypes.SYSTEM);
          }

          @Override
          public void processTerminated(ProcessEvent event) {
            startedProcess.removeProcessListener(this);
            if (myOutput != null) {
              myOutput.close();
            }
          }
        });
        if (console instanceof ConsoleView) {
          ((ConsoleView)console).addMessageFilter(new ShowOutputFileFilter());
        }
      }
    }
  }

  private static class ShowOutputFileFilter implements Filter {
    @Override
    public Result applyFilter(String line, int entireLength) {
      if (line.startsWith(CONSOLE_OUTPUT_FILE_MESSAGE)) {
        final String filePath = StringUtil.trimEnd(line.substring(CONSOLE_OUTPUT_FILE_MESSAGE.length()), "\n");

        return new Result(entireLength - filePath.length() - 1, entireLength, new HyperlinkInfo() {
          @RequiredUIAccess
          @Override
          public void navigate(final Project project) {
            final VirtualFile file = ApplicationManager.getApplication().runWriteAction(new Supplier<VirtualFile>() {
              @Nullable
              @Override
              public VirtualFile get() {
                return LocalFileSystem.getInstance().refreshAndFindFileByPath(FileUtil.toSystemIndependentName(filePath));
              }
            });

            if (file != null) {
              file.refresh(false, false);
              ApplicationManager.getApplication().runReadAction(new Runnable() {
                @Override
                public void run() {
                  FileEditorManager.getInstance(project).openTextEditor(OpenFileDescriptorFactory.getInstance(project).builder(file).build(), true);
                }
              });
            }
          }
        });
      }
      return null;
    }
  }
}
