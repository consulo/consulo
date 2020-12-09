/*
 * Copyright 2013-2020 consulo.io
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
package consulo.execution;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import consulo.util.dataholder.Key;

/**
 * @author VISTALL
 * @since 09/12/2020
 */
public interface ExecutionDataKeys {
  Key<RunProfile> RUN_PROFILE = Key.create("runProfile");
  Key<ExecutionEnvironment> EXECUTION_ENVIRONMENT = Key.create("executionEnvironment");
  Key<RunContentDescriptor> RUN_CONTENT_DESCRIPTOR = Key.create("RUN_CONTENT_DESCRIPTOR");
}
