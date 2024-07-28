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
package consulo.container;

import consulo.container.plugin.PluginId;

/**
 * <p>Represents an internal error caused by a plugin. It may happen if the plugin's code fails with an exception,
 * or if the plugin violates some contract of Consulo Platform. If such exceptions are thrown or logged
 * via {@link Logger#error(Throwable)} method and reported by user, they may be automatically attributed
 * to corresponding plugins.</p>
 *
 * <p>If the problem is caused by a class, use {@link PluginExceptionUtil#createByClass} to create
 * an instance</p>
 */
public class PluginException extends RuntimeException {
  private final PluginId myPluginId;

  public PluginException(String message, Throwable cause, PluginId pluginId) {
    super(message, cause);
    myPluginId = pluginId;
  }

  public PluginException(Throwable e, PluginId pluginId) {
    super (e.getMessage(), e);
    myPluginId = pluginId;
  }

  public PluginException(final String message, final PluginId pluginId) {
    super(message);
    myPluginId = pluginId;
  }

  public PluginId getPluginId() {
    return myPluginId;
  }

  @Override
  public String getMessage() {
    String message = super.getMessage();

    if (message == null) {
      message = "";
    }

    message += " [Plugin: " + myPluginId.toString() + "]";
    return message;
  }
}
