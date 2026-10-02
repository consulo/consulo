// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

import consulo.annotation.access.RequiredReadAction;
import consulo.endpoint.EndpointElementItem;
import consulo.endpoint.EndpointListItem;
import consulo.endpoint.EndpointProvider;
import consulo.endpoint.EndpointUrlTargetProvider;
import consulo.endpoint.mime.MimeTypeConstants;
import consulo.endpoint.url.UrlPath;
import consulo.endpoint.url.UrlQueryParameter;
import consulo.endpoint.url.UrlTargetInfo;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class OasExportUtil {
    public static final OpenApiSpecification EMPTY_OPENAPI_SPECIFICATION = new OpenApiSpecification(List.of());

    private OasExportUtil() {
    }

    @RequiredReadAction
    public static OpenApiSpecification getSpecificationByUrls(Iterable<? extends UrlTargetInfo> urls) {
        List<OpenApiSpecification> fromProvider = new ArrayList<>();
        for (UrlTargetInfo url : urls) {
            OpenApiSpecification specification = OasSpecificationProvider.findOasSpecification(url);
            if (specification != null) {
                fromProvider.add(specification);
            }
        }
        if (!fromProvider.isEmpty()) {
            return squashOpenApiSpecifications(fromProvider);
        }

        List<OasEndpointPath> paths = new ArrayList<>();
        for (UrlTargetInfo urlTargetInfo : urls) {
            OasEndpointPath.Builder pathBuilder =
                new OasEndpointPath.Builder(urlTargetInfo.getPath().getPresentation(OasModelUtil.OPEN_API_PRESENTATION));
            paths.add(pathBuilder.build(builder -> {
                List<OasParameter> pathParams = new ArrayList<>();
                for (UrlPath.PathSegment segment : urlTargetInfo.getPath().getSegments()) {
                    if (segment instanceof UrlPath.PathSegment.Variable variable) {
                        String variableName = variable.getVariableName();
                        if (variableName != null) {
                            pathParams.add(new OasParameter.Builder(variableName, OasParameterIn.PATH).build());
                        }
                    }
                }

                List<OasParameter> queryParams = new ArrayList<>();
                for (UrlQueryParameter queryParameter : urlTargetInfo.getQueryParameters()) {
                    queryParams.add(new OasParameter.Builder(queryParameter.getName(), OasParameterIn.QUERY).build());
                }

                List<OasOperation> operations = new ArrayList<>();
                for (String method : urlTargetInfo.getMethods()) {
                    operations.add(new OasOperation.Builder(method).build(operation -> {
                        operation.setSummary(
                            method.toUpperCase(Locale.ROOT) + " " +
                                urlTargetInfo.getPath().getPresentation(UrlPath.FULL_PATH_VARIABLE_PRESENTATION)
                        );
                        operation.setDeprecated(urlTargetInfo.isDeprecated());
                        operation.setResponses(List.of(new OasResponse("200", "OK")));

                        List<OasParameter> parameters = new ArrayList<>(pathParams.size() + queryParams.size());
                        parameters.addAll(pathParams);
                        parameters.addAll(queryParams);
                        operation.setParameters(parameters);

                        Set<String> contentTypes = urlTargetInfo.getContentTypes();
                        if (!contentTypes.isEmpty()) {
                            Map<String, OasSchema> content = new LinkedHashMap<>();
                            for (String contentType : contentTypes) {
                                if (contentType.equalsIgnoreCase(MimeTypeConstants.APPLICATION_OCTET_STREAM)) {
                                    content.put(
                                        contentType,
                                        new OasSchema.Builder(OasSchemaType.STRING)
                                            .build(schema -> schema.setFormat(OasSchemaFormat.BINARY))
                                    );
                                }
                                else {
                                    content.put(
                                        contentType,
                                        new OasSchema.Builder(OasSchemaType.OBJECT)
                                            .build(schema -> schema.setReference(OasSchema.SCHEMA_DEFINITION_STUB_REFERENCE))
                                    );
                                }
                            }
                            operation.setRequestBody(new OasRequestBody(content, true));
                        }
                    }));
                }
                builder.setOperations(operations);
            }));
        }
        return new OpenApiSpecification(paths);
    }

    public static OpenApiSpecification squashOpenApiSpecifications(List<OpenApiSpecification> specifications) {
        if (specifications.size() <= 1) {
            return specifications.isEmpty() ? EMPTY_OPENAPI_SPECIFICATION : specifications.get(0);
        }

        Map<String, List<OasEndpointPath>> grouped = new LinkedHashMap<>();
        for (OpenApiSpecification specification : specifications) {
            for (OasEndpointPath path : specification.getPaths()) {
                grouped.computeIfAbsent(path.getPath(), key -> new ArrayList<>()).add(path);
            }
        }

        List<OasEndpointPath> list = new ArrayList<>();

        Set<OasTag> distinctTags = new LinkedHashSet<>();
        for (OpenApiSpecification specification : specifications) {
            List<OasTag> specificationTags = specification.getTags();
            if (specificationTags != null) {
                distinctTags.addAll(specificationTags);
            }
        }
        List<OasTag> tags = new ArrayList<>(distinctTags);

        for (Map.Entry<String, List<OasEndpointPath>> entry : grouped.entrySet()) {
            List<OasEndpointPath> items = entry.getValue();

            if (items.size() == 1) {
                list.addAll(items);
            }
            else {
                String path = entry.getKey();
                String summary = items.get(0).getSummary();

                List<OasOperation> operations = new ArrayList<>();
                for (OasEndpointPath item : items) {
                    operations.addAll(item.getOperations());
                }

                boolean sameSummary = true;
                for (OasEndpointPath item : items) {
                    if (!Objects.equals(item.getSummary(), summary)) {
                        sameSummary = false;
                        break;
                    }
                }

                Set<OasHttpMethod> distinctMethods = new HashSet<>();
                for (OasOperation operation : operations) {
                    distinctMethods.add(operation.getMethod());
                }

                if (sameSummary && distinctMethods.size() == operations.size()) {
                    list.add(new OasEndpointPath(path, summary, operations));
                }
                else {
                    list.addAll(entry.getValue());
                }
            }
        }

        Map<String, OasSchema> mergedSchemas = new LinkedHashMap<>();
        for (OpenApiSpecification specification : specifications) {
            OasComponents components = specification.getComponents();
            if (components != null) {
                mergedSchemas.putAll(components.getSchemas());
            }
        }
        OasComponents components = !mergedSchemas.isEmpty() ? new OasComponents(mergedSchemas) : null;

        return new OpenApiSpecification(list, components, tags);
    }

    @RequiredReadAction
    public static <G, E> @Nullable OpenApiSpecification getOpenApi(EndpointProvider<G, E> provider, G group, E endpoint) {
        if (provider instanceof EndpointUrlTargetProvider<G, E> urlTargetProvider && urlTargetProvider.shouldShowOpenApiPanel()) {
            OpenApiSpecification openApiFromProvider = urlTargetProvider.getOpenApiSpecification(group, endpoint);
            return openApiFromProvider != null
                ? openApiFromProvider
                : getSpecificationByUrls(urlTargetProvider.getUrlTargetInfo(group, endpoint));
        }
        else {
            return null;
        }
    }

    @RequiredReadAction
    public static @Nullable OpenApiSpecification getOpenApiSpecification(Collection<? extends EndpointListItem> endpointsList) {
        List<OpenApiSpecification> specifications = new ArrayList<>();
        for (EndpointListItem item : endpointsList) {
            if (item instanceof EndpointElementItem<?, ?> elementItem) {
                OpenApiSpecification specification = getOpenApi(elementItem);
                if (specification != null) {
                    specifications.add(specification);
                }
            }
        }

        if (specifications.isEmpty()) {
            return null;
        }

        return squashOpenApiSpecifications(specifications);
    }

    @RequiredReadAction
    private static <G, E> @Nullable OpenApiSpecification getOpenApi(EndpointElementItem<G, E> item) {
        return getOpenApi(item.getProvider(), item.getGroup(), item.getEndpoint());
    }
}
