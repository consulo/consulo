// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.accountManagement.oauth;

import consulo.application.Application;
import consulo.http.HttpMethod;
import consulo.http.HttpRequestBuilderFactory;
import consulo.http.HttpVersion;
import consulo.http.client.HttpApiResponse;
import consulo.util.io.Url;

import java.io.IOException;
import java.util.function.Function;

public final class OAuthCredentialsAcquirerHttp {
    public static <T extends OAuthCredentials> AcquireCredentialsResult<T> requestToken(
        Url url,
        Function<HttpApiResponse<String>, T> credentialsProvider
    ) {
        HttpApiResponse<String> response;
        try {
            response = requestToken(url);
        }
        catch (IOException e) {
            return new ErrorAcquireCredentialsResult<>("Cannot exchange token: " + e.getMessage());
        }
        return convertToAcquireCredentialsResult(response, credentialsProvider);
    }

    // used externally
    public static HttpApiResponse<String> requestToken(Url url) throws IOException {
        HttpRequestBuilderFactory factory = Application.get().getInstance(HttpRequestBuilderFactory.class);
        return factory.newBuilder(url.toExternalForm(), HttpMethod.POST)
            .version(HttpVersion.HTTP_1_1)
            .header("Content-Type", "application/json")
            .body(new byte[0])
            .allowErrorCodes(true)
            .connect(request -> new HttpApiResponse<>(
                request.getURL(),
                request.statusCode(),
                request.responseHeaders(),
                request.readString(null)
            ));
    }

    public static <T extends OAuthCredentials> AcquireCredentialsResult<T> convertToAcquireCredentialsResult(
        HttpApiResponse<String> httpResponse,
        Function<HttpApiResponse<String>, T> credentialsProvider
    ) {
        if (httpResponse.statusCode() == 200) {
            T creds = credentialsProvider.apply(httpResponse);
            return new SuccessAcquireCredentialsResult<>(creds);
        }
        else {
            String body = httpResponse.body();
            return new ErrorAcquireCredentialsResult<>(body.isEmpty() ? "No token provided" : body);
        }
    }

    private OAuthCredentialsAcquirerHttp() {
    }
}
