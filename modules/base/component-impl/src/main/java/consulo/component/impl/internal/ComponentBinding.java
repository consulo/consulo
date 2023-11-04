/*
 * Copyright 2013-2023 consulo.io
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
package consulo.component.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.component.internal.inject.InjectingBindingLoader;
import consulo.component.internal.inject.TopicBindingLoader;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2023-11-04
 */
@ServiceAPI(ComponentScope.APPLICATION)
// implementation will be autoregistered by application start
public record ComponentBinding(InjectingBindingLoader injectingBindingLoader, @Nonnull TopicBindingLoader topicBindingLoader) {
}
