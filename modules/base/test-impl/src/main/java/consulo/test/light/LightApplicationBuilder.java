/*
 * Copyright 2013-2018 consulo.io
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
package consulo.test.light;

import consulo.application.Application;
import consulo.component.impl.internal.ComponentBinding;
import consulo.component.internal.inject.InjectingBindingLoader;
import consulo.component.internal.inject.TopicBindingLoader;
import consulo.disposer.Disposable;
import consulo.test.light.impl.LightApplication;
import consulo.test.light.impl.LightExtensionRegistrator;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class LightApplicationBuilder {
  public static class DefaultRegistrator extends LightExtensionRegistrator {
  }

  @Nonnull
  public static LightApplicationBuilder create(@Nonnull Disposable rootDisposable) {
    return create(rootDisposable, new DefaultRegistrator());
  }

  @Nonnull
  public static LightApplicationBuilder create(@Nonnull Disposable rootDisposable, @Nonnull DefaultRegistrator registrator) {
    return new LightApplicationBuilder(rootDisposable, registrator);
  }

  private final Disposable myRootDisposable;
  private final LightExtensionRegistrator myRegistrator;

  private LightApplicationBuilder(Disposable rootDisposable, LightExtensionRegistrator registrator) {
    myRootDisposable = rootDisposable;
    myRegistrator = registrator;
  }

  @Nonnull
  public Application build() {
    InjectingBindingLoader bindingLoader = new InjectingBindingLoader();
    bindingLoader.analyzeBindings();
    return new LightApplication(myRootDisposable,
                                new ComponentBinding(bindingLoader, new TopicBindingLoader()),
                                myRegistrator);
  }
}
