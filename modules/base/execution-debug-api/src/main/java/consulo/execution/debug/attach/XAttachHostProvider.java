/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.execution.debug.attach;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import java.util.List;

/**
 * This interface describes the provider to enumerate different hosts/environments capable of attaching to with the debugger.
 * The examples are local machine environment (see {@link LocalAttachHost}) or SSH connection to a remote server.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface XAttachHostProvider<T extends XAttachHost> {
  ExtensionPointName<XAttachHostProvider> EP = ExtensionPointName.create(XAttachHostProvider.class);

  /**
   * @return the group to which all connections provided by this provider belong
   */
  @Nonnull
  XAttachPresentationGroup<? extends XAttachHost> getPresentationGroup();

  /**
   * @return a list of connections of this type, which is characterized by the provider
   */
  @Nonnull
  List<T> getAvailableHosts(@Nullable Project project);
}
