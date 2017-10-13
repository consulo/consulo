/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ui.ex;

import com.intellij.openapi.wm.impl.WindowInfoImpl;
import com.intellij.openapi.wm.impl.commands.FinalizableCommand;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

/**
 * @author VISTALL
 * @since 12-Oct-17
 * <p>
 * TODO [VISTALL] implement {@link consulo.ui.Component}
 */
public interface ToolWindowPanel {
  @NotNull
  FinalizableCommand createAddButtonCmd(final ToolWindowStripeButton button, @NotNull WindowInfoImpl info, @NotNull Comparator<ToolWindowStripeButton> comparator, @NotNull Runnable finishCallBack);

  FinalizableCommand createUpdateButtonPositionCmd(@NotNull String id, @NotNull Runnable finishCallback);

  @NotNull
  FinalizableCommand createRemoveButtonCmd(@NotNull String id, @NotNull Runnable finishCallBack);
}
