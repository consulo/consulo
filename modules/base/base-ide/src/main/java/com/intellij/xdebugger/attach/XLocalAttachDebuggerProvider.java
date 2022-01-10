// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.xdebugger.attach;

import com.intellij.execution.process.ProcessInfo;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.UserDataHolder;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated use {@link XAttachDebuggerProvider} instead
 */
@Deprecated
public interface XLocalAttachDebuggerProvider extends XAttachDebuggerProvider {
  ExtensionPointName<XAttachDebuggerProvider> EP = ExtensionPointName.create("com.intellij.xdebugger.localAttachDebuggerProvider");

  /**
   * @deprecated use {@link XAttachDebuggerProvider#getAvailableDebuggers(Project, XAttachHost, ProcessInfo, UserDataHolder)} instead
   */
  @Deprecated
  List<XLocalAttachDebugger> getAvailableDebuggers(@Nonnull Project project, @Nonnull ProcessInfo process, @Nonnull UserDataHolder contextHolder);

  /**
   * @deprecated use {@link XAttachDebuggerProvider#getPresentationGroup()} instead
   */
  @Deprecated
  @Nonnull
  default XAttachPresentationGroup<ProcessInfo> getAttachGroup() {
    return XDefaultLocalAttachGroup.INSTANCE;
  }

  @Nonnull
  @Override
  default XAttachPresentationGroup<ProcessInfo> getPresentationGroup() {
    return getAttachGroup();
  }

  @Override
  default boolean isAttachHostApplicable(@Nonnull XAttachHost attachHost) {
    return attachHost instanceof LocalAttachHost;
  }

  @Nonnull
  @Override
  default List<XAttachDebugger> getAvailableDebuggers(@Nonnull Project project, @Nonnull XAttachHost hostInfo, @Nonnull ProcessInfo process, @Nonnull UserDataHolder contextHolder) {
    assert hostInfo instanceof LocalAttachHost;

    return new ArrayList<>(getAvailableDebuggers(project, process, contextHolder));
  }
}
