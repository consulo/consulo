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
import consulo.component.internal.ComponentBinding;
import consulo.component.internal.inject.*;
import consulo.disposer.Disposable;
import consulo.test.light.impl.LightApplication;
import consulo.test.light.impl.LightExtensionRegistrator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class LightApplicationBuilder {
    public static class DefaultRegistrator extends LightExtensionRegistrator {
    }


    public static LightApplicationBuilder create(Disposable rootDisposable) {
        return create(rootDisposable, new DefaultRegistrator());
    }


    public static LightApplicationBuilder create(Disposable rootDisposable, DefaultRegistrator registrator) {
        return new LightApplicationBuilder(rootDisposable, registrator);
    }

    private final Disposable myRootDisposable;
    private final LightExtensionRegistrator myRegistrator;

    private LightApplicationBuilder(Disposable rootDisposable, LightExtensionRegistrator registrator) {
        myRootDisposable = rootDisposable;
        myRegistrator = registrator;
    }

    public Application build() {
        NewInjectingBindingCollector injectingBindingCollector = new NewInjectingBindingCollector();
        NewTopicBindingCollector topicBindingCollector = new NewTopicBindingCollector();
        NewBindingLoader loader = new NewBindingLoader(injectingBindingCollector, topicBindingCollector);

        List<Runnable> actions = new ArrayList<>();

        loader.init(actions);

        actions.parallelStream().forEach(Runnable::run);

        InjectingBindingLoader injectingBindingLoader = new InjectingBindingLoader(
            injectingBindingCollector.getServices(),
            injectingBindingCollector.getExtensions(),
            injectingBindingCollector.getTopics(),
            injectingBindingCollector.getActions()
        );

        TopicBindingLoader topicBindingLoader = new TopicBindingLoader(topicBindingCollector.getBindings());

        return new LightApplication(myRootDisposable, new ComponentBinding(injectingBindingLoader, topicBindingLoader), myRegistrator);
    }
}
