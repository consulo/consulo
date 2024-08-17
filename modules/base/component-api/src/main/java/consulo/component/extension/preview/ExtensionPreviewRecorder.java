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
package consulo.component.extension.preview;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionExtender;
import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

/**
 * This API allow extract extension info which will be stored at plugin repository,
 * and any plugin can access it for suggesting installing new plugins.
 * <p>
 * This code will never call in IDE instance
 * <p>
 * This extension not allowed to extend via {@link ExtensionExtender}
 *
 * @author VISTALL
 * @since 22/01/2023
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ExtensionPreviewRecorder<T> {
    void analyze(@Nonnull Consumer<ExtensionPreview> recorder);
}
