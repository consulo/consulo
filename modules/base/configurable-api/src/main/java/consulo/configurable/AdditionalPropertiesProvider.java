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
package consulo.configurable;

import consulo.ui.Component;

import jakarta.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * Base extension interface for extending {@link SimpleConfigurableByProperties}, but it's depends to each implementation
 *
 * @author VISTALL
 * @since 17-Jul-22
 */
public interface AdditionalPropertiesProvider {
  void fillProperties(@Nonnull SimpleConfigurableByProperties.PropertyBuilder builder, Consumer<Component> layoutBuilder);
}
