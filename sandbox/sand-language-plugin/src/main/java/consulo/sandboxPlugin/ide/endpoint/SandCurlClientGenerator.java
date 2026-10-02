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
import consulo.endpoint.client.generator.AvailableClientSettings;
import consulo.endpoint.client.generator.ClientExample;
import consulo.endpoint.client.generator.ClientGenerator;
import consulo.endpoint.oas.OasEndpointPath;
import consulo.endpoint.oas.OasHttpMethod;
import consulo.endpoint.oas.OasOperation;
import consulo.endpoint.oas.OpenApiSpecification;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

@ExtensionImpl
public final class SandCurlClientGenerator implements ClientGenerator {
    private static final String DEFAULT_BASE_URL = "http://localhost:8080";

    private final SandCurlClientSettings mySettings = new SandCurlClientSettings();

    @Override
    public LocalizeValue getTitle() {
        return LocalizeValue.localizeTODO("Sand cURL");
    }

    @Override
    public AvailableClientSettings getAvailableClientSettings() {
        return mySettings;
    }

    @Override
    public @Nullable ClientExample generate(OpenApiSpecification openApiSpecification) {
        if (openApiSpecification.isEmpty()) {
            return null;
        }

        boolean boilerplate = mySettings.getActualClientSettings().getBoilerplate();
        String baseUrl = boilerplate ? "$BASE_URL" : DEFAULT_BASE_URL;

        StringBuilder builder = new StringBuilder();
        if (boilerplate) {
            builder.append("#!/usr/bin/env bash\n");
            builder.append("set -euo pipefail\n\n");
            builder.append("BASE_URL=\"${BASE_URL:-").append(DEFAULT_BASE_URL).append("}\"\n\n");
        }

        for (OasEndpointPath path : openApiSpecification.getPaths()) {
            for (OasOperation operation : path.getOperations()) {
                appendCurl(builder, operation, baseUrl + path.getAbsolutePath(), boilerplate);
            }
        }
        return ClientExample.fromFileExtension(builder.toString(), "sh");
    }

    private static void appendCurl(StringBuilder builder, OasOperation operation, String url, boolean boilerplate) {
        String method = operation.getMethod().getMethodName().toUpperCase(Locale.ROOT);
        builder.append("curl --globoff -X ").append(method).append(" \"").append(url).append('"');
        builder.append(" -H 'Accept: application/json'");
        OasHttpMethod httpMethod = operation.getMethod();
        if (httpMethod == OasHttpMethod.POST || httpMethod == OasHttpMethod.PUT || httpMethod == OasHttpMethod.PATCH) {
            builder.append(" -H 'Content-Type: application/json' -d '{}'");
        }
        if (boilerplate) {
            builder.append(" --fail --silent --show-error");
        }
        builder.append('\n');
    }
}
