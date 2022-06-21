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
package consulo.ide.impl.idea.ide;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Topic;
import consulo.component.messagebus.TopicImpl;
import consulo.annotation.DeprecationInfo;

/**
 * @author yole
 */
@Topic(ComponentScope.APPLICATION)
public interface AppLifecycleListener {
  TopicImpl<AppLifecycleListener> TOPIC = TopicImpl.create("Application lifecycle notifications", AppLifecycleListener.class);

  default void projectFrameClosed() {
  }

  default void projectOpenFailed() {
  }

  /**
   * Fired before saving settings and before final 'can exit?' check. App may end up not closing if some of the 'can exit?' listeners
   * return false.
   */
  default void appClosing() {
  }

  @Deprecated
  @DeprecationInfo("Use 'consulo.ide.impl.idea.ide.AppLifecycleListener' instead")
  abstract class Adapter implements AppLifecycleListener {

    @Override
    public void projectFrameClosed() {
    }

    @Override
    public void projectOpenFailed() {
    }

    @Override
    public void appClosing() {
    }
  }
}