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
import consulo.component.util.ModificationTracker;
import consulo.endpoint.EndpointFilter;
import consulo.endpoint.EndpointType;
import consulo.endpoint.EndpointUrlTargetProvider;
import consulo.endpoint.ExternalEndpointFilter;
import consulo.endpoint.FrameworkPresentation;
import consulo.endpoint.presentation.HttpMethodPresentation;
import consulo.endpoint.url.Authority;
import consulo.endpoint.url.UrlTargetInfo;
import consulo.navigation.ItemPresentation;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Set;

@ExtensionImpl
public final class SandExternalEndpointProvider implements EndpointUrlTargetProvider<String, SandExternalEndpoint> {
    public static final String PETSTORE_GROUP = "petstore";
    public static final String PETSTORE_AUTHORITY = "petstore.swagger.io";

    private static final FrameworkPresentation PRESENTATION =
        new FrameworkPresentation("Sand-OpenAPI", "Sand OpenAPI", PlatformIconGroup.nodesStatic());

    private static final List<SandExternalEndpoint> PETSTORE_ENDPOINTS = List.of(
        new SandExternalEndpoint("GET", "/v2/pet/{petId}"),
        new SandExternalEndpoint("POST", "/v2/pet"),
        new SandExternalEndpoint("GET", "/v2/pet/findByStatus"),
        new SandExternalEndpoint("DELETE", "/v2/store/order/{orderId}")
    );

    private final Project myProject;

    @Inject
    public SandExternalEndpointProvider(Project project) {
        myProject = project;
    }

    @Override
    public EndpointType getEndpointType() {
        return EndpointType.API_DEFINITION_TYPE;
    }

    @Override
    public FrameworkPresentation getPresentation() {
        return PRESENTATION;
    }

    @Override
    public Status getStatus() {
        return SandEndpointSearch.getStatus(myProject);
    }

    @Override
    public Iterable<String> getEndpointGroups(EndpointFilter filter) {
        return filter instanceof ExternalEndpointFilter ? List.of(PETSTORE_GROUP) : List.of();
    }

    @Override
    public Iterable<SandExternalEndpoint> getEndpoints(String group) {
        return PETSTORE_GROUP.equals(group) ? PETSTORE_ENDPOINTS : List.of();
    }

    @Override
    public boolean isValidEndpoint(String group, SandExternalEndpoint endpoint) {
        return PETSTORE_GROUP.equals(group) && PETSTORE_ENDPOINTS.contains(endpoint);
    }

    @Override
    public ItemPresentation getEndpointPresentation(String group, SandExternalEndpoint endpoint) {
        return new HttpMethodPresentation(endpoint.path(), endpoint.method(), group, PRESENTATION.getIcon());
    }

    @Override
    public ModificationTracker getModificationTracker() {
        return ModificationTracker.NEVER_CHANGED;
    }

    @Override
    public Iterable<UrlTargetInfo> getUrlTargetInfo(String group, SandExternalEndpoint endpoint) {
        return List.of(new SandUrlTargetInfo(
            List.of("https://"),
            List.of(new Authority.Exact(PETSTORE_AUTHORITY)),
            SandUrlTargetInfo.parsePath(endpoint.path()),
            Set.of(endpoint.method()),
            false,
            group,
            null
        ));
    }
}
