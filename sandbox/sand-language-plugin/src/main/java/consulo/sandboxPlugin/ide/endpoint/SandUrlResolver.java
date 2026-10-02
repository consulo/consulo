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

import consulo.annotation.access.RequiredReadAction;
import consulo.endpoint.url.Authority;
import consulo.endpoint.url.HttpUrlResolver;
import consulo.endpoint.url.UrlResolveRequest;
import consulo.endpoint.url.UrlTargetInfo;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class SandUrlResolver extends HttpUrlResolver {
    private final Project myProject;

    public SandUrlResolver(Project project) {
        myProject = project;
    }

    @Override
    @RequiredReadAction
    public Iterable<UrlTargetInfo> resolve(UrlResolveRequest request) {
        if (!isKnownAuthority(request.getAuthorityHint())) {
            return List.of();
        }

        String method = request.getMethod();
        List<UrlTargetInfo> result = new ArrayList<>();
        for (UrlTargetInfo target : getVariants()) {
            if (method != null && !target.getMethods().isEmpty() && !target.getMethods().contains(method)) {
                continue;
            }
            if (target.getPath().isCompatibleWith(request.getPath())) {
                result.add(target);
            }
        }
        return result;
    }

    @Override
    @RequiredReadAction
    public Iterable<UrlTargetInfo> getVariants() {
        return SandEndpointSearch.getServerTargets(myProject);
    }

    private static boolean isKnownAuthority(@Nullable String authority) {
        if (authority == null) {
            return true;
        }
        for (Authority.Exact known : HTTP_AUTHORITY) {
            if (known.getText().equals(authority)) {
                return true;
            }
        }
        return false;
    }
}
