// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.attach;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.platform.ProcessInfo;
import consulo.project.Project;
import consulo.util.dataholder.UserDataHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated use {@link XAttachDebuggerProvider} instead
 */
@Deprecated
@ExtensionAPI(ComponentScope.APPLICATION)
public interface XLocalAttachDebuggerProvider extends XAttachDebuggerProvider {
  ExtensionPointName<XAttachDebuggerProvider> EP = ExtensionPointName.create(XLocalAttachDebuggerProvider.class);

  /**
   * @deprecated use {@link XAttachDebuggerProvider#getAvailableDebuggers(Project, XAttachHost, ProcessInfo, UserDataHolder)} instead
   */
  @Deprecated
  List<XLocalAttachDebugger> getAvailableDebuggers(Project project, ProcessInfo process, UserDataHolder contextHolder);

  /**
   * @deprecated use {@link XAttachDebuggerProvider#getPresentationGroup()} instead
   */
  @Deprecated
  
  default XAttachPresentationGroup<ProcessInfo> getAttachGroup() {
    return XDefaultLocalAttachGroup.INSTANCE;
  }

  
  @Override
  default XAttachPresentationGroup<ProcessInfo> getPresentationGroup() {
    return getAttachGroup();
  }

  @Override
  default boolean isAttachHostApplicable(XAttachHost attachHost) {
    return attachHost instanceof LocalAttachHost;
  }

  
  @Override
  default List<XAttachDebugger> getAvailableDebuggers(Project project, XAttachHost hostInfo, ProcessInfo process, UserDataHolder contextHolder) {
    assert hostInfo instanceof LocalAttachHost;

    return new ArrayList<>(getAvailableDebuggers(project, process, contextHolder));
  }
}
