package consulo.endpoint.impl.internal.view.example;

import consulo.endpoint.client.generator.ClientGenerator;
import consulo.endpoint.oas.OpenApiSpecification;

record EndpointExampleRequest(ClientGenerator generator, OpenApiSpecification openApiSpecification, EndpointExampleSettings settings) {
}
