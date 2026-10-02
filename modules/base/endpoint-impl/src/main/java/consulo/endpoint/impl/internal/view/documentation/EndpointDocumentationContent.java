package consulo.endpoint.impl.internal.view.documentation;

sealed interface EndpointDocumentationContent
    permits EndpointCustomDocumentation, EndpointElementDocumentation, EndpointNoDocumentation {
}
