/*
 * Copyright 2013-2019 consulo.io
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
package consulo.sandboxPlugin.ide.debugger.attach;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessInfo;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.UserDataHolder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.xdebugger.attach.*;
import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-10-26
 */
public class SandAttachToProcessProvider implements XAttachDebuggerProvider {
  @Override
  public boolean isAttachHostApplicable(@Nonnull XAttachHost attachHost) {
    return attachHost instanceof LocalAttachHost;
  }

  @Nonnull
  @Override
  public List<XAttachDebugger> getAvailableDebuggers(@Nonnull Project project, @Nonnull XAttachHost hostInfo, @Nonnull ProcessInfo process, @Nonnull UserDataHolder contextHolder) {
    if(StringUtil.endsWith(process.getExecutableDisplayName(), "java")) {
      return Collections.singletonList(new XLocalAttachDebugger() {
        @Nonnull
        @Override
        public String getDebuggerDisplayName() {
          return "Test";
        }

        @Override
        public void attachDebugSession(@Nonnull Project project, @Nonnull ProcessInfo info) throws ExecutionException {

        }
      });
    }
    return Collections.emptyList();
  }
}
