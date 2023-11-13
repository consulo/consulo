/*
 * Copyright 2013-2022 consulo.io
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
package consulo.desktop.progress;

import consulo.annotation.component.ServiceImpl;
import consulo.application.impl.internal.progress.ProgressActivityFactory;
import consulo.platform.Platform;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 23-Mar-22
 */
@Singleton
@ServiceImpl
public class DesktopProgressActivityFactory implements ProgressActivityFactory {
  private final boolean myShouldStartActivity;

  public DesktopProgressActivityFactory() {
    Platform platform = Platform.current();
    
    boolean isMac = platform.os().isMac();
    myShouldStartActivity = isMac && Boolean.parseBoolean(platform.jvm().getRuntimeProperty("consulo.mac.prevent.app.nap", "true"));
  }

  @Nullable
  @Override
  public Runnable createActivity() {
    if (myShouldStartActivity) {
      return MacActivityUtil.wakeUpNeo(this);
    }
    return null;
  }
}
