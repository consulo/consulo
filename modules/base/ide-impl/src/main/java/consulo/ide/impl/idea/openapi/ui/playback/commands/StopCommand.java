/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.ui.playback.commands;

import consulo.ide.impl.idea.openapi.ui.playback.PlaybackContext;
import consulo.util.concurrent.ActionCallback;

public class StopCommand extends AbstractCommand {

  public static String PREFIX = CMD_PREFIX + "stop";

  public StopCommand(String text, int line) {
    super(text, line);
  }

  protected ActionCallback _execute(PlaybackContext context) {
    context.message("Stopped", getLine());
    return new ActionCallback.Done();
  }

  @Override
  public boolean canGoFurther() {
    return false;
  }
}