/*
 * Copyright 2013 must-be.org
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
import com.intellij.execution.RunContentExecutor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.components.ExpandMacroToPathMap;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ui.UIUtil;
import lombok.val;
import org.consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 23:16/06.10.13
 */
@Logger
public class BackgroundTaskByVfsChangeTaskImpl implements BackgroundTaskByVfsChangeTask {
  private final Project myProject;
  private final BackgroundTaskByVfsParameters myParameters;
  private final VirtualFilePointer myVirtualFilePointer;
  private final String myProviderName;
  private final String myName;
  private boolean myEnabled;
  private final BackgroundTaskByVfsChangeProvider myProvider;
  private final BackgroundTaskByVfsChangeManagerImpl myManager;

  private String[] myGeneratedFilePaths;

  public BackgroundTaskByVfsChangeTaskImpl(@NotNull Project project,
                                           @NotNull VirtualFilePointer pointer,
                                           @NotNull BackgroundTaskByVfsParameters parameters,
                                           @NotNull String providerName,
                                           @NotNull String name,
                                           @Nullable BackgroundTaskByVfsChangeProvider provider,
                                           @NotNull BackgroundTaskByVfsChangeManagerImpl manager) {
    myProject = project;
    myParameters = parameters;
    myVirtualFilePointer = pointer;

    myProviderName = providerName;
    myName = name;
    myProvider = provider;
    myManager = manager;
  }

  public BackgroundTaskByVfsChangeTaskImpl(@NotNull Project project,
                                           @NotNull VirtualFile virtualFile,
                                           @NotNull BackgroundTaskByVfsChangeManagerImpl manager,
                                           @NotNull BackgroundTaskByVfsChangeProvider provider,
                                           @NotNull String name,
                                           @NotNull BackgroundTaskByVfsParameters parameters) {
    this(project, VirtualFilePointerManager.getInstance().create(virtualFile, manager, null), parameters, provider.getTemplateName(), name, provider, manager);

  }

  public BackgroundTaskByVfsChangeTaskImpl(@NotNull Project project,
                                           @NotNull VirtualFilePointer pointer,
                                           @NotNull String provider,
                                           @NotNull String name,
                                           @NotNull BackgroundTaskByVfsParameters parameters,
                                           @NotNull BackgroundTaskByVfsChangeManagerImpl manager) {
    this(project, pointer, parameters, provider, name, findProviderByName(provider), manager);
  }

  @Nullable
  private static BackgroundTaskByVfsChangeProvider findProviderByName(String name) {
    BackgroundTaskByVfsChangeProvider temp = null;
    for (val backgroundTaskByVfsChangeProvider : BackgroundTaskByVfsChangeProvider.EP_NAME.getExtensions()) {
      if (Comparing.equal(name, backgroundTaskByVfsChangeProvider.getTemplateName())) {
        temp = backgroundTaskByVfsChangeProvider;
        break;
      }
    }
    return temp;
  }

  public void run(@NotNull final ActionCallback actionCallback) {
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

        CapturingProcessHandler processHandler = new CapturingProcessHandler(commandLine.createProcess());
        processHandler.addProcessListener(new ProcessAdapter() {
          @Override
          public void processTerminated(ProcessEvent event) {
            actionCallback.setDone();

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
        });

        final RunContentExecutor contentExecutor =
                new RunContentExecutor(BackgroundTaskByVfsChangeTaskImpl.this.myProject, processHandler).withTitle(myProviderName)
                        .withActivateToolWindow(false);
        UIUtil.invokeLaterIfNeeded(new Runnable() {
          @Override
          public void run() {
            contentExecutor.run();
          }
        });
      }
      catch (ExecutionException e) {
        actionCallback.setRejected();
        LOGGER.error(e);
      }
  }

  public ExpandMacroToPathMap createExpandMacroToPathMap() {
    final ExpandMacroToPathMap expandMacroToPathMap = new ExpandMacroToPathMap();
    expandMacroToPathMap.addMacroExpand("FileName", myVirtualFilePointer.getFileName());
    expandMacroToPathMap.addMacroExpand("FilePath", myVirtualFilePointer.getPresentableUrl());

    File parentFile = FileUtilRt.getParentFile(new File(myVirtualFilePointer.getPresentableUrl()));
    expandMacroToPathMap.addMacroExpand("FileParentPath", parentFile.getAbsolutePath());
    return expandMacroToPathMap;
  }

  private ExpandMacroToPathMap createExpandOutMacroToPathMap() {
    final ExpandMacroToPathMap expandMacroToPathMap = new ExpandMacroToPathMap();
    expandMacroToPathMap.addMacroExpand("FileName", myVirtualFilePointer.getFileName());
    expandMacroToPathMap.addMacroExpand("FilePath", myVirtualFilePointer.getPresentableUrl());
    expandMacroToPathMap.addMacroExpand("OutPath", myParameters.getOutPath());

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

  @Override
  public boolean isEnabled() {
    return myEnabled;
  }

  @Override
  public void setEnabled(boolean enabled) {
    myEnabled = enabled;
  }

  @NotNull
  @Override
  public String getProviderName() {
    return myProviderName;
  }

  @NotNull
  @Override
  public String getName() {
    return myName;
  }

  @Nullable
  @Override
  public BackgroundTaskByVfsChangeProvider getProvider() {
    return myProvider;
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
  public String[] getGeneratedFilePaths() {
    if (myGeneratedFilePaths == null) {
      myGeneratedFilePaths = generateFilePaths();
    }
    return myGeneratedFilePaths;
  }

  @NotNull
  @Override
  public VirtualFile[] getGeneratedFiles() {
    String[] generatedFilePaths = getGeneratedFilePaths();
    if (generatedFilePaths.length == 0) {
      return VirtualFile.EMPTY_ARRAY;
    }
    List<VirtualFile> list = new ArrayList<VirtualFile>();
    for (String generatedFilePath : generatedFilePaths) {
      VirtualFile fileByPath = LocalFileSystem.getInstance().findFileByPath(generatedFilePath);
      if (fileByPath != null) {
        list.add(fileByPath);
      }
    }
    return VfsUtilCore.toVirtualFileArray(list);
  }

  @NotNull
  private String[] generateFilePaths() {
    if (myProvider == null) {
      return ArrayUtil.EMPTY_STRING_ARRAY;
    }
    VirtualFile file = myVirtualFilePointer.getFile();
    if (file == null) {
      return ArrayUtil.EMPTY_STRING_ARRAY;
    }
    String[] generatedFiles = myProvider.getGeneratedFiles(myProject, file);
    if (generatedFiles.length == 0) {
      return ArrayUtil.EMPTY_STRING_ARRAY;
    }

    ExpandMacroToPathMap expandOutMacroToPathMap = createExpandOutMacroToPathMap();

    String[] allPaths = new String[generatedFiles.length];
    for (int i = 0; i < generatedFiles.length; i++) {
      String generatedFile = generatedFiles[i];
      String expanded = expandOutMacroToPathMap.substitute(generatedFile, SystemInfo.isFileSystemCaseSensitive);
      allPaths[i] = expanded;
    }
    return allPaths;
  }

  @NotNull
  @Override
  public Project getProject() {
    return myProject;
  }

  @NotNull
  @Override
  public BackgroundTaskByVfsChangeTask clone() {
    val task = new BackgroundTaskByVfsChangeTaskImpl(myProject, myVirtualFilePointer, myParameters, myProviderName, myName, myProvider, myManager);
    task.setEnabled(isEnabled());
    return task;
  }

  public void parameterUpdated() {
    myGeneratedFilePaths = null;
    myManager.taskChanged(this);
  }
}
