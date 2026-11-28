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
package consulo.externalService.impl.internal;

import consulo.logging.Logger;
import consulo.util.lang.function.ThrowableFunction;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sends a request to the other host of the {@link WebServiceApi} - the same api, see
 * {@link WebServiceApi#buildFallbackUrl(String)} - where the host of the url can not be reached. This is not a proxy:
 * the request gets another host in its url, nothing else.
 * <p>
 * Where the api can not be reached - a country where it answers with a {@link SocketTimeoutException} - a request pays
 * the connect timeout once and goes on with the host which answers. That host is the one the next request is sent to
 * first, so the timeout is paid once per run.
 *
 * @author VISTALL
 * @since 2026-11-25
 */
public final class WebServiceApiFallback {
    private static final Logger LOG = Logger.getInstance(WebServiceApiFallback.class);

    /**
     * The other host answered last - the next request is sent to it first.
     */
    private static final AtomicBoolean ourFallbackFirst = new AtomicBoolean();

    private WebServiceApiFallback() {
    }

    /**
     * Sends the request, with the other api host where the default one can not be reached.
     *
     * @param api    the api the url was built with - it knows its other host
     * @param url    the url built by {@link WebServiceApi#buildUrl(String)}
     * @param sender sends the request to the url it is given - the url of the other host on the second try
     * @throws IOException what the last try threw, the first one suppressed
     */
    public static <T> T request(WebServiceApi api, String url, ThrowableFunction<String, T, IOException> sender) throws IOException {
        String fallbackUrl = api.buildFallbackUrl(url);
        if (fallbackUrl == null) {
            return sender.apply(url);
        }

        List<String> urls = ourFallbackFirst.get() ? List.of(fallbackUrl, url) : List.of(url, fallbackUrl);

        IOException error = null;
        for (String next : urls) {
            try {
                T result = sender.apply(next);
                ourFallbackFirst.set(next.equals(fallbackUrl));
                return result;
            }
            catch (IOException e) {
                if (error != null) {
                    e.addSuppressed(error);
                }

                if (!isUnreachable(e)) {
                    throw e;
                }

                LOG.warn("The api host of " + next + " can not be reached", e);
                error = e;
            }
        }

        // both tries have thrown
        throw error;
    }

    /**
     * A host which can not be reached - not an answer the api gave.
     */
    private static boolean isUnreachable(Throwable error) {
        for (Throwable e = error; e != null; e = e.getCause()) {
            if (e instanceof SocketTimeoutException
                || e instanceof UnknownHostException
                || e instanceof SocketException
                || e instanceof SSLException) {
                return true;
            }

            if (e.getCause() == e) {
                break;
            }
        }

        return false;
    }
}
