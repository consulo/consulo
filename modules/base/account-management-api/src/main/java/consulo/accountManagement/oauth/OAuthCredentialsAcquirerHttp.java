// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.accountManagement.oauth;

import consulo.util.io.Url;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.BiFunction;

public final class OAuthCredentialsAcquirerHttp {
    public static <T extends OAuthCredentials> AcquireCredentialsResult<T> requestToken(
        Url url,
        BiFunction<String, HttpHeaders, T> credentialsProvider
    ) {
        HttpResponse<String> response;
        try {
            response = requestToken(url);
        }
        catch (IOException e) {
            return new ErrorAcquireCredentialsResult<>("Cannot exchange token: " + e.getMessage());
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ErrorAcquireCredentialsResult<>("Cannot exchange token: " + e.getMessage());
        }
        return convertToAcquireCredentialsResult(response, credentialsProvider);
    }

    // used externally
    public static HttpResponse<String> requestToken(Url url) throws IOException, InterruptedException {
        String tokenUrl = url.toExternalForm();
        HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(tokenUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static <T extends OAuthCredentials> AcquireCredentialsResult<T> convertToAcquireCredentialsResult(
        HttpResponse<String> httpResponse,
        BiFunction<String, HttpHeaders, T> credentialsProvider
    ) {
        if (httpResponse.statusCode() == 200) {
            T creds = credentialsProvider.apply(httpResponse.body(), httpResponse.headers());
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
