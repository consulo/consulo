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
package consulo.http.impl.internal;

import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.component.ProcessCanceledException;
import consulo.http.HttpCertificateManager;
import consulo.http.HttpMethod;
import consulo.http.HttpProxyManager;
import consulo.platform.Platform;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The {@link java.net.http.HttpClient} executor stops waiting for an answer when the progress of the thread is canceled
 * - {@link java.net.URLConnection} waits until its timeout.
 *
 * @author VISTALL
 * @since 2026-09-28
 */
public class HttpRequestCancelTest {
    @Test
    public void canceledProgressStopsTheWaitForTheAnswer() throws Exception {
        ProgressIndicator indicator = mock(ProgressIndicator.class);
        when(indicator.isCanceled()).thenReturn(true);
        doThrow(new ProcessCanceledException()).when(indicator).checkCanceled();

        ProgressIndicatorProvider provider = mock(ProgressIndicatorProvider.class);
        when(provider.getProgressIndicator()).thenReturn(indicator);

        Application application = mock(Application.class);
        when(application.getProgressManager()).thenReturn(provider);

        HttpRequestBuilderFactoryImpl factory =
            new HttpRequestBuilderFactoryImpl(application, mock(HttpProxyManager.class), mock(HttpCertificateManager.class), true);

        try (StubHttpServer server = new StubHttpServer(request -> {
            Thread.sleep(3000);
            return StubHttpServer.Response.text("late");
        })) {
            long start = System.nanoTime();

            assertThatExceptionOfType(ProcessCanceledException.class)
                .isThrownBy(() -> factory.newBuilder(server.url("/"), HttpMethod.GET).useProxy(false).readString(null));

            assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isLessThan(2000);
        }
    }

    @Test
    public void onlyTheLocalPlatformSends() {
        Platform remote = mock(Platform.class);
        when(remote.getId()).thenReturn("remote");

        HttpRequestBuilderFactoryImpl factory =
            new HttpRequestBuilderFactoryImpl(mock(Application.class), mock(HttpProxyManager.class), mock(HttpCertificateManager.class), false);

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> factory.newBuilder(remote, "http://127.0.0.1/", HttpMethod.GET));
    }
}
