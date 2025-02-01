/*
 * Copyright 2013-2016 consulo.io
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
package consulo.externalService.impl.internal.update;

import consulo.externalService.internal.PlatformOrPluginUpdateResultType;
import jakarta.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 10-Oct-16
 */
public class PlatformOrPluginUpdateResult {
  public static PlatformOrPluginUpdateResult CANCELED = new PlatformOrPluginUpdateResult(PlatformOrPluginUpdateResultType.CANCELED, Collections.emptyList());
  public static PlatformOrPluginUpdateResult NO_UPDATE = new PlatformOrPluginUpdateResult(PlatformOrPluginUpdateResultType.NO_UPDATE, Collections.emptyList());
  public static PlatformOrPluginUpdateResult RESTART_REQUIRED = new PlatformOrPluginUpdateResult(PlatformOrPluginUpdateResultType.RESTART_REQUIRED, Collections.emptyList());

    private final PlatformOrPluginUpdateResultType myType;
  private final List<PlatformOrPluginNode> myPlugins;

  public PlatformOrPluginUpdateResult(@Nonnull PlatformOrPluginUpdateResultType type, @Nonnull List<PlatformOrPluginNode> plugins) {
    myType = type;
    myPlugins = plugins;
  }

  @Nonnull
  public PlatformOrPluginUpdateResultType getType() {
    return myType;
  }

  @Nonnull
  public List<PlatformOrPluginNode> getPlugins() {
    return myPlugins;
  }
}
