/*
 * Copyright 2013-2026 consulo.io
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
package consulo.sandboxPlugin.ide.endpoint;

import consulo.annotation.component.ExtensionImpl;
import consulo.endpoint.EndpointType;
import consulo.endpoint.FrameworkPresentation;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import jakarta.inject.Inject;

@ExtensionImpl
public final class SandEndpointProvider extends SandHttpEndpointProviderBase {
    private static final FrameworkPresentation PRESENTATION =
        new FrameworkPresentation("Sand-Web", "Sand Web", PlatformIconGroup.nodesStatic());

    @Inject
    public SandEndpointProvider(Project project) {
        super(project, false);
    }

    @Override
    public EndpointType getEndpointType() {
        return EndpointType.HTTP_SERVER_TYPE;
    }

    @Override
    public FrameworkPresentation getPresentation() {
        return PRESENTATION;
    }
}
