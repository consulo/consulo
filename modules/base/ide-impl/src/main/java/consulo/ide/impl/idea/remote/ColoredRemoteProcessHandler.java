/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import consulo.execution.process.AnsiEscapeDecoder;
import consulo.process.remote.BaseRemoteProcessHandler;
import consulo.process.remote.RemoteProcess;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.nio.charset.Charset;

/**
 * @author traff
 */
public class ColoredRemoteProcessHandler<T extends RemoteProcess> extends BaseRemoteProcessHandler<T> implements AnsiEscapeDecoder.ColoredTextAcceptor {
  private final AnsiEscapeDecoder myAnsiEscapeDecoder = new AnsiEscapeDecoder();

  public ColoredRemoteProcessHandler(@Nonnull T process, /*@NotNull*/ String commandLine, @Nullable Charset charset) {
    super(process, commandLine, charset);
  }

  @Override
  public final void notifyTextAvailable(String text, Key outputType) {
    myAnsiEscapeDecoder.escapeText(text, outputType, this);
  }

  @Override
  public void coloredTextAvailable(String text, Key attributes) {
    super.notifyTextAvailable(text, attributes);
  }
}