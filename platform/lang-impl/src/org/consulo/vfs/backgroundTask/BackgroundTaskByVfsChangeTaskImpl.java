/*
 * Copyright 2013 Consulo.org
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
package org.consulo.vfs.backgroundTask;

import com.intellij.application.options.ReplacePathToMacroMap;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.components.ExpandMacroToPathMap;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.MessageView;
import com.intellij.util.ui.UIUtil;
import org.consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author VISTALL
 * @since 23:16/06.10.13
 */
@Logger
public class BackgroundTaskByVfsChangeTaskImpl implements BackgroundTaskByVfsChangeTask {
  private final Project myProject;
  private final String myProviderName;
  private final BackgroundTaskByVfsParameters myParameters;
  private final VirtualFilePointer myVirtualFilePointer;
  private final VirtualFileAdapter myListener;
  private final AtomicBoolean myProgress = new AtomicBoolean(false);

  public BackgroundTaskByVfsChangeTaskImpl(Project project,
                                           VirtualFile virtualFile,
                                           BackgroundTaskByVfsChangeManagerImpl manager,
                                           String provider,
                                           BackgroundTaskByVfsParameters parameters) {
    this(project, VirtualFilePointerManager.getInstance().create(virtualFile, manager, null), provider, parameters);
  }

  public BackgroundTaskByVfsChangeTaskImpl(Project project,
                                           VirtualFilePointer pointer,
                                           String provider,
                                           BackgroundTaskByVfsParameters parameters) {
    myProject = project;
    myProviderName = provider;
    myParameters = parameters;
    myVirtualFilePointer = pointer;

    myListener = new VirtualFileAdapter() {
      @Override
      public void contentsChanged(VirtualFileEvent event) {
        if(!myVirtualFilePointer.isValid()) {
          return;
        }
        VirtualFile file = myVirtualFilePointer.getFile();
        if (file == null) {
          return;
        }
        if (file.equals(event.getFile())) {
          if(!myProgress.getAndSet(true)) {
            start();
          }
        }
      }
    };
    VirtualFileManager.getInstance().addVirtualFileListener(myListener);
  }

  public void start() {
    Task.Backgroundable backgroundTask = new Task.Backgroundable(myProject, "Processing: " + myVirtualFilePointer.getFileName()) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        try {
          final ExpandMacroToPathMap expandMacroToPathMap = createExpandMacroToPathMap();

          GeneralCommandLine commandLine = new GeneralCommandLine();
          commandLine.setExePath(myParameters.getExePath());
          String programParameters = myParameters.getProgramParameters();
          if (programParameters != null) {
            commandLine.addParameters(StringUtil.split(expandMacroToPathMap.substitute(programParameters, false), " "));
          }

          commandLine.setWorkDirectory(expandMacroToPathMap.substitute(myParameters.getWorkingDirectory(), false));
          commandLine.setPassParentEnvironment(myParameters.isPassParentEnvs());
          commandLine.getEnvironment().putAll(myParameters.getEnvs());


          final Ref<Boolean> b = new Ref<Boolean>(false);

          final TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(myProject);
          OSProcessHandler processHandler = new OSProcessHandler(commandLine.createProcess());
          consoleBuilder.getConsole().attachToProcess(processHandler);
          processHandler.addProcessListener(new ProcessAdapter() {
            @Override
            public void processTerminated(ProcessEvent event) {
              myProgress.set(false);

              String outPath = myParameters.getOutPath();
              if (outPath == null) {
                return;
              }
              String substitute = expandMacroToPathMap.substitute(outPath, false);

              final VirtualFile fileByPath = LocalFileSystem.getInstance().findFileByPath(substitute);
              if (fileByPath != null) {
                new WriteAction<Object>() {
                  @Override
                  protected void run(Result<Object> result) throws Throwable {
                    fileByPath.refresh(false, true);
                  }
                }.execute();
              }
            }

            @Override
            public void onTextAvailable(ProcessEvent event, Key outputType) {
              if (outputType == ProcessOutputTypes.STDERR && !b.get()) {
                UIUtil.invokeLaterIfNeeded(new Runnable() {
                  @Override
                  public void run() {
                    ContentManager contentManager = MessageView.SERVICE.getInstance(myProject).getContentManager();
                    Content content = ContentFactory.SERVICE.getInstance()
                      .createContent(consoleBuilder.getConsole().getComponent(), myProviderName, false);
                    contentManager.addContent(content);
                  }
                });
              }
            }
          });
          processHandler.startNotify();
        }
        catch (ExecutionException e) {
          LOGGER.error(e);
        }
      }

    };

    backgroundTask.queue();
  }

  public ExpandMacroToPathMap createExpandMacroToPathMap() {
    final ExpandMacroToPathMap expandMacroToPathMap = new ExpandMacroToPathMap();
    expandMacroToPathMap.addMacroExpand("FileName", myVirtualFilePointer.getFileName());
    expandMacroToPathMap.addMacroExpand("FilePath", myVirtualFilePointer.getPresentableUrl());

    File parentFile = FileUtilRt.getParentFile(new File(myVirtualFilePointer.getPresentableUrl()));
    expandMacroToPathMap.addMacroExpand("FileParentPath", parentFile.getAbsolutePath());
    return expandMacroToPathMap;
  }

  public ReplacePathToMacroMap createReplaceMacroToPathMap() {
    final ReplacePathToMacroMap replacePathToMacroMap = new ReplacePathToMacroMap();
    replacePathToMacroMap.put(myVirtualFilePointer.getFileName(), "$FileName$");
    replacePathToMacroMap.addMacroReplacement(myVirtualFilePointer.getPresentableUrl(), "FilePath");

    File parentFile = FileUtilRt.getParentFile(new File(myVirtualFilePointer.getPresentableUrl()));
    replacePathToMacroMap.addMacroReplacement(parentFile.getAbsolutePath(), "FileParentPath");
    return replacePathToMacroMap;
  }

  @NotNull
  @Override
  public String getProviderName() {
    return myProviderName;
  }

  @NotNull
  @Override
  public VirtualFilePointer getVirtualFilePointer() {
    return myVirtualFilePointer;
  }

  @NotNull
  @Override
  public BackgroundTaskByVfsParameters getParameters() {
    return myParameters;
  }

  @NotNull
  @Override
  public Project getProject() {
    return myProject;
  }

  @Override
  public void dispose() {
    VirtualFileManager.getInstance().removeVirtualFileListener(myListener);
  }
}
