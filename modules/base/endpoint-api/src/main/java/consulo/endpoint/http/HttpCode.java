// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.http;

import consulo.endpoint.localize.EndpointLocalize;
import consulo.localize.LocalizeValue;

public enum HttpCode {
    SWITCHING_PROTOCOLS(101, EndpointLocalize.httpCodeDescription101()),
    PROCESSING(102, EndpointLocalize.httpCodeDescription102()),
    OK(200, EndpointLocalize.httpCodeDescription200()),
    CREATED(201, EndpointLocalize.httpCodeDescription201()),
    ACCEPTED(202, EndpointLocalize.httpCodeDescription202()),
    NON_AUTHORITATIVE_INFORMATION(203, EndpointLocalize.httpCodeDescription203()),
    NO_CONTENT(204, EndpointLocalize.httpCodeDescription204()),
    RESET_CONTENT(205, EndpointLocalize.httpCodeDescription205()),
    PARTIAL_CONTENT(206, EndpointLocalize.httpCodeDescription206()),
    MULTI_STATUS(207, EndpointLocalize.httpCodeDescription207()),
    MULTIPLE_CHOICES(300, EndpointLocalize.httpCodeDescription300()),
    MOVED_PERMANENTLY(301, EndpointLocalize.httpCodeDescription301()),
    FOUND(302, EndpointLocalize.httpCodeDescription302()),
    SEE_OTHER(303, EndpointLocalize.httpCodeDescription303()),
    NOT_MODIFIED(304, EndpointLocalize.httpCodeDescription304()),
    USE_PROXY(305, EndpointLocalize.httpCodeDescription305()),
    TEMPORARY_REDIRECT(307, EndpointLocalize.httpCodeDescription307()),
    PERMANENT_REDIRECT(308, EndpointLocalize.httpCodeDescription308()),
    BAD_REQUEST(400, EndpointLocalize.httpCodeDescription400()),
    UNAUTHORIZED(401, EndpointLocalize.httpCodeDescription401()),
    PAYMENT_REQUIRED(402, EndpointLocalize.httpCodeDescription402()),
    FORBIDDEN(403, EndpointLocalize.httpCodeDescription403()),
    NOT_FOUND(404, EndpointLocalize.httpCodeDescription404()),
    METHOD_NOT_ALLOWED(405, EndpointLocalize.httpCodeDescription405()),
    NOT_ACCEPTABLE(406, EndpointLocalize.httpCodeDescription406()),
    PROXY_AUTHENTICATION_REQUIRED(407, EndpointLocalize.httpCodeDescription407()),
    REQUEST_TIMEOUT(408, EndpointLocalize.httpCodeDescription408()),
    CONFLICT(409, EndpointLocalize.httpCodeDescription409()),
    GONE(410, EndpointLocalize.httpCodeDescription410()),
    LENGTH_REQUIRED(411, EndpointLocalize.httpCodeDescription411()),
    PRECONDITION_FAILED(412, EndpointLocalize.httpCodeDescription412()),
    REQUEST_ENTITY_TOO_LARGE(413, EndpointLocalize.httpCodeDescription413()),
    REQUEST_URI_TOO_LONG(414, EndpointLocalize.httpCodeDescription414()),
    UNSUPPORTED_MEDIA_TYPE(415, EndpointLocalize.httpCodeDescription415()),
    REQUESTED_RANGE_NOT_SATISFIABLE(416, EndpointLocalize.httpCodeDescription416()),
    EXPECTATION_FAILED(417, EndpointLocalize.httpCodeDescription417()),
    MISDIRECTED_REQUEST(421, EndpointLocalize.httpCodeDescription421()),
    UNPROCESSABLE_ENTITY(422, EndpointLocalize.httpCodeDescription422()),
    LOCKED(423, EndpointLocalize.httpCodeDescription423()),
    FAILED_DEPENDENCY(424, EndpointLocalize.httpCodeDescription424()),
    UNORDERED_COLLECTION(425, EndpointLocalize.httpCodeDescription425()),
    UPGRADE_REQUIRED(426, EndpointLocalize.httpCodeDescription426()),
    PRECONDITION_REQUIRED(428, EndpointLocalize.httpCodeDescription428()),
    TOO_MANY_REQUESTS(429, EndpointLocalize.httpCodeDescription429()),
    REQUEST_HEADER_FIELDS_TOO_LARGE(431, EndpointLocalize.httpCodeDescription431()),
    INTERNAL_SERVER_ERROR(500, EndpointLocalize.httpCodeDescription500()),
    NOT_IMPLEMENTED(501, EndpointLocalize.httpCodeDescription501()),
    BAD_GATEWAY(502, EndpointLocalize.httpCodeDescription502()),
    SERVICE_UNAVAILABLE(503, EndpointLocalize.httpCodeDescription503()),
    GATEWAY_TIMEOUT(504, EndpointLocalize.httpCodeDescription504()),
    HTTP_VERSION_NOT_SUPPORTED(505, EndpointLocalize.httpCodeDescription505()),
    VARIANT_ALSO_NEGOTIATES(506, EndpointLocalize.httpCodeDescription506()),
    INSUFFICIENT_STORAGE(507, EndpointLocalize.httpCodeDescription507()),
    NOT_EXTENDED(510, EndpointLocalize.httpCodeDescription510()),
    NETWORK_AUTHENTICATION_REQUIRED(511, EndpointLocalize.httpCodeDescription511());

    private final int myStatusCode;
    private final LocalizeValue myShortDescription;

    HttpCode(int statusCode, LocalizeValue shortDescription) {
        myStatusCode = statusCode;
        myShortDescription = shortDescription;
    }

    public int getStatusCode() {
        return myStatusCode;
    }

    public LocalizeValue getShortDescription() {
        return myShortDescription;
    }
}
