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
package consulo.component.internal.inject;

import consulo.annotation.component.*;

import java.util.Map;

/**
 * @author VISTALL
 * @since 13-Jun-22
 */
public record InjectingBindingLoader(Map<ComponentScope, InjectingBindingHolder> services,
                                     Map<ComponentScope, InjectingBindingHolder> extensions,
                                     Map<ComponentScope, InjectingBindingHolder> topics,
                                     InjectingBindingHolder actions) {

    public InjectingBindingHolder getHolder(Class<?> annotationClass, ComponentScope componentScope) {
        if (annotationClass == ServiceAPI.class) {
            return services().getOrDefault(componentScope, InjectingBindingHolder.EMPTY);
        }
        else if (annotationClass == ExtensionAPI.class) {
            return extensions().getOrDefault(componentScope, InjectingBindingHolder.EMPTY);
        }
        else if (annotationClass == TopicAPI.class) {
            return topics().getOrDefault(componentScope, InjectingBindingHolder.EMPTY);
        }
        else if (annotationClass == ActionAPI.class) {
            return actions();
        }
        throw new UnsupportedOperationException("Unknown annotation: " + annotationClass);
    }
}
