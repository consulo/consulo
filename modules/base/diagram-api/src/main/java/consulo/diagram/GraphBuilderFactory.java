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
package consulo.diagram;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 22:36/15.10.13
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface GraphBuilderFactory {
    @Nonnull
    public static GraphBuilderFactory getInstance() {
        return Application.get().getInstance(GraphBuilderFactory.class);
    }

    GraphBuilder createBuilder();
}
