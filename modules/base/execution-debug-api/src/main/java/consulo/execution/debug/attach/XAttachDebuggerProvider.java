// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.attach;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.platform.ProcessInfo;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.UserDataHolder;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * This provider used to obtain debuggers, which can debug the process with specified {@link XAttachHost} and {@link ProcessInfo}
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface XAttachDebuggerProvider {
  ExtensionPointName<XAttachDebuggerProvider> EP = ExtensionPointName.create(XAttachDebuggerProvider.class);

  /**
   * will be removed in 2020.1, right after {@link XLocalAttachDebuggerProvider}
   */
  @Deprecated
  //@ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  @Nonnull
  static List<XAttachDebuggerProvider> getAttachDebuggerProviders() {
    return ContainerUtil.concat(new ArrayList<>(EP.getExtensionList()), new ArrayList<>(XLocalAttachDebuggerProvider.EP.getExtensionList()));
  }

  /**
   * @return a group in which the supported processes should be visually organized.
   * Return {@link XDefaultLocalAttachGroup} for a common process group.
   */
  @Nonnull
  default XAttachPresentationGroup<ProcessInfo> getPresentationGroup() {
    return XDefaultLocalAttachGroup.INSTANCE;
  }


  /**
   * @return if this XAttachDebuggerProvider is able to interact with this host
   */
  boolean isAttachHostApplicable(@Nonnull XAttachHost attachHost);

  /**
   * Attach to Process action invokes {@link #getAvailableDebuggers} method for every running process on the given host.
   * The host is either local or selected by user among available ones (see {@link XAttachHostProvider})
   * <p>
   * If there are several debuggers that can attach to a process, the user will have a choice between them.
   *
   * @param contextHolder use this data holder if you need to store temporary data during debuggers collection.
   *                      Lifetime of the data is restricted by a single Attach to Process action invocation.
   * @param hostInfo      host (environment) on which the process is being run
   * @param process       process to attach to
   * @return a list of the debuggers that can attach and debug a given process
   */
  @Nonnull
  List<XAttachDebugger> getAvailableDebuggers(@Nonnull Project project, @Nonnull XAttachHost hostInfo, @Nonnull ProcessInfo process, @Nonnull UserDataHolder contextHolder);
}
