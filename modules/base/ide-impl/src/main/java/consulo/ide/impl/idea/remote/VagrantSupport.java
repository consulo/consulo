/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.remote;

import com.google.common.util.concurrent.ListenableFuture;
import consulo.process.ExecutionException;
import consulo.ide.ServiceManager;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * @author traff
 */
public abstract class VagrantSupport {
  @Nullable
  public static VagrantSupport getInstance() {
    return ServiceManager.getService(VagrantSupport.class);
  }
  public abstract ListenableFuture<RemoteCredentials> computeVagrantSettings(@Nullable Project project,
                                                                             @Nonnull String vagrantFolder,
                                                                             @Nullable String machineName);

  public static void showMissingVagrantSupportMessage(final @Nullable Project project) {
    UIUtil.invokeLaterIfNeeded(() -> Messages.showErrorDialog(project, "Enable Vagrant Support plugin",
                                                          "Vagrant Support Disabled"));
  }

  @Nonnull
  public abstract RemoteCredentials getCredentials(@Nonnull String vagrantFolder, @Nullable String machineName) throws IOException;

  public abstract boolean checkVagrantRunning(@Nonnull String vagrantFolder, @Nullable String machineName, boolean askToRunIfDown);

  public abstract void runVagrant(@Nonnull String vagrantFolder, @Nullable String machineName) throws ExecutionException;

  public abstract Collection<? extends RemoteConnector> getVagrantInstancesConnectors(@Nonnull Project project);

  public abstract boolean isVagrantInstance(VirtualFile dir);

  public abstract List<String> getMachineNames(@Nonnull String instanceFolder);

  public boolean isNotReadyForSsh(Throwable t) {
    return isNotReadyForSsh(t.getMessage());
  }

  public static boolean isNotReadyForSsh(@Nonnull String errorMessage) {
    return errorMessage.contains("not yet ready for SSH");
  }

  @Nullable
  public abstract String findVagrantFolder(@Nonnull Project project);


  public static class MultipleMachinesException extends Exception {
  }
}
