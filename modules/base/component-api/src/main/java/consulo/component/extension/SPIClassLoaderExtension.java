/*
 * Copyright 2013-2025 consulo.io
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
package consulo.component.extension;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import jakarta.annotation.Nonnull;

/**
 * Extension for registering plugins for getting it for composite classloaders, for loading SPI providers from JDK
 * <p>
 * Possible variants of target classes {@link javax.imageio.ImageIO} and {@link  javax.sound.sampled.AudioSystem}
 *
 * @author VISTALL
 * @since 2025-04-01
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface SPIClassLoaderExtension {
    @Nonnull
    Class<?> getTargetClass();
}
