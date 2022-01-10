/*
 * Copyright 2013-2016 consulo.io
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
package consulo.backgroundTaskByVfsChange;

import com.intellij.application.options.ReplacePathToMacroMap;
import com.intellij.build.progress.BuildProgress;
import com.intellij.build.progress.BuildProgressDescriptor;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.ExpandMacroToPathMap;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import com.intellij.util.ArrayUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 23:16/06.10.13
 */
public class BackgroundTaskByVfsChangeTaskImpl implements BackgroundTaskByVfsChangeTask {
  private static final Logger LOGGER = Logger.getInstance(BackgroundTaskByVfsChangeTaskImpl.class);

  private final Project myProject;
  private final BackgroundTaskByVfsParameters myParameters;
  private final VirtualFilePointer myVirtualFilePointer;
  private final String myProviderName;
  private final String myName;
  private boolean myEnabled;
  private final BackgroundTaskByVfsChangeProvider myProvider;
  private final BackgroundTaskByVfsChangeManagerImpl myManager;

  private String[] myGeneratedFilePaths;

  public BackgroundTaskByVfsChangeTaskImpl(@Nonnull Project project,
                                           @Nonnull VirtualFilePointer pointer,
                                           @Nonnull BackgroundTaskByVfsParameters parameters,
                                           @Nonnull String providerName,
                                           @Nonnull String name,
                                           @Nullable BackgroundTaskByVfsChangeProvider provider,
                                           @Nonnull BackgroundTaskByVfsChangeManagerImpl manager) {
    myProject = project;
    myParameters = parameters;
    myVirtualFilePointer = pointer;

    myProviderName = providerName;
    myName = name;
    myProvider = provider;
    myManager = manager;
  }

  public BackgroundTaskByVfsChangeTaskImpl(@Nonnull Project project,
                                           @Nonnull VirtualFile virtualFile,
                                           @Nonnull BackgroundTaskByVfsChangeManagerImpl manager,
                                           @Nonnull BackgroundTaskByVfsChangeProvider provider,
                                           @Nonnull String name,
                                           @Nonnull BackgroundTaskByVfsParameters parameters) {
    this(project, VirtualFilePointerManager.getInstance().create(virtualFile, manager, null), parameters, provider.getTemplateName(), name, provider, manager);

  }

  public BackgroundTaskByVfsChangeTaskImpl(@Nonnull Project project,
                                           @Nonnull VirtualFilePointer pointer,
                                           @Nonnull String provider,
                                           @Nonnull String name,
                                           @Nonnull BackgroundTaskByVfsParameters parameters,
                                           @Nonnull BackgroundTaskByVfsChangeManagerImpl manager) {
    this(project, pointer, parameters, provider, name, findProviderByName(provider), manager);
  }

  @Nullable
  private static BackgroundTaskByVfsChangeProvider findProviderByName(String name) {
    BackgroundTaskByVfsChangeProvider temp = null;
    for (BackgroundTaskByVfsChangeProvider backgroundTaskByVfsChangeProvider : BackgroundTaskByVfsChangeProvider.EP_NAME.getExtensionList()) {
      if (Comparing.equal(name, backgroundTaskByVfsChangeProvider.getTemplateName())) {
        temp = backgroundTaskByVfsChangeProvider;
        break;
      }
    }
    return temp;
  }

  public void run(@Nonnull final AsyncResult<Void> actionCallback, BuildProgress<BuildProgressDescriptor> buildProgress) {
    try {
      UIAccess uiAccess = Application.get().getLastUIAccess();

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

      OSProcessHandler processHandler = ProcessHandlerFactory.getInstance().createProcessHandler(commandLine);
      processHandler.addProcessListener(new ProcessAdapter() {
        @Override
        public void onTextAvailable(ProcessEvent event, Key outputType) {
          buildProgress.output(event.getText(), outputType == ProcessOutputTypes.STDOUT || outputType == ProcessOutputTypes.SYSTEM);
        }

        @Override
        public void processTerminated(ProcessEvent event) {
          if (event.getExitCode() != 0) {
            actionCallback.setRejected();
            return;
          }

          actionCallback.setDone();

          String outPath = myParameters.getOutPath();
          if (outPath == null) {
            return;
          }
          String substitute = expandMacroToPathMap.substitute(outPath, false);

          final VirtualFile fileByPath = LocalFileSystem.getInstance().findFileByPath(substitute);
          if (fileByPath != null) {
            uiAccess.give(() -> ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> fileByPath.refresh(false, true), "Refreshing Files...", false, myProject));
          }
        }
      });

      processHandler.startNotify();
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

  @Nonnull
  @Override
  public String getProviderName() {
    return myProviderName;
  }

  @Nonnull
  @Override
  public String getName() {
    return myName;
  }

  @Nullable
  @Override
  public BackgroundTaskByVfsChangeProvider getProvider() {
    return myProvider;
  }

  @Nonnull
  @Override
  public VirtualFilePointer getVirtualFilePointer() {
    return myVirtualFilePointer;
  }

  @Nonnull
  @Override
  public BackgroundTaskByVfsParameters getParameters() {
    return myParameters;
  }

  @RequiredReadAction
  @Nonnull
  @Override
  public String[] getGeneratedFilePaths() {
    if (myGeneratedFilePaths == null) {
      myGeneratedFilePaths = generateFilePaths();
    }
    return myGeneratedFilePaths;
  }

  @RequiredReadAction
  @Nonnull
  @Override
  public VirtualFile[] getGeneratedFiles() {
    String[] generatedFilePaths = getGeneratedFilePaths();
    if (generatedFilePaths.length == 0) {
      return VirtualFile.EMPTY_ARRAY;
    }
    List<VirtualFile> list = new ArrayList<>();
    for (String generatedFilePath : generatedFilePaths) {
      VirtualFile fileByPath = LocalFileSystem.getInstance().findFileByPath(generatedFilePath);
      if (fileByPath != null) {
        list.add(fileByPath);
      }
    }
    return VfsUtilCore.toVirtualFileArray(list);
  }

  @Nonnull
  @RequiredReadAction
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

  @Nonnull
  @Override
  public Project getProject() {
    return myProject;
  }

  @Nonnull
  @Override
  public BackgroundTaskByVfsChangeTask clone() {
    BackgroundTaskByVfsChangeTaskImpl task =
            new BackgroundTaskByVfsChangeTaskImpl(myProject, myVirtualFilePointer, myParameters, myProviderName, myName, myProvider, myManager);
    task.setEnabled(isEnabled());
    return task;
  }

  public void parameterUpdated() {
    myGeneratedFilePaths = null;
  }
}
