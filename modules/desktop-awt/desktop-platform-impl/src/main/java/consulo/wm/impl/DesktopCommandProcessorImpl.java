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
package consulo.wm.impl;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.impl.DesktopApplicationImpl;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.wm.impl.CommandProcessorBase;
import consulo.ui.impl.ModalityPerProjectEAPDescriptor;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 13-Oct-17
 */
public class DesktopCommandProcessorImpl extends CommandProcessorBase {
  @Nonnull
  @Override
  protected AsyncResult<Void> invokeLater(@Nonnull Runnable command, @Nonnull Condition<?> expire) {
    DesktopApplicationImpl application = (DesktopApplicationImpl)Application.get();

    ModalityState modalityState = ModalityPerProjectEAPDescriptor.is() ? ModalityState.current() : ModalityState.NON_MODAL;
    return application.getInvokator().invokeLater(command, modalityState, expire);
  }
}
