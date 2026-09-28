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

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.stream.Stream;

/**
 * A test of a {@link HttpRequestTestCase}, run once for each executor of the local platform.
 *
 * @author VISTALL
 * @since 2026-09-28
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@TestTemplate
@ExtendWith(EachExecutorTest.Executors.class)
public @interface EachExecutorTest {
    class Executors implements TestTemplateInvocationContextProvider {
        @Override
        public boolean supportsTestTemplate(ExtensionContext context) {
            return true;
        }

        @Override
        public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
            return Stream.of(invocation("url-connection", false), invocation("http-client", true));
        }

        private static TestTemplateInvocationContext invocation(String name, boolean httpClient) {
            return new TestTemplateInvocationContext() {
                @Override
                public String getDisplayName(int invocationIndex) {
                    return name;
                }

                @Override
                public List<Extension> getAdditionalExtensions() {
                    return List.of((BeforeEachCallback) context ->
                        ((HttpRequestTestCase) context.getRequiredTestInstance()).setHttpClient(httpClient));
                }
            };
        }
    }
}
