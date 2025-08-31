/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.psi.stub;

import jakarta.annotation.Nonnull;
import java.io.IOException;

/**
 * @author Dmitry Avdeev
 * @since 2012-08-02
 */
public interface ObjectStubSerializer<T extends Stub, P extends Stub> {
  @Nonnull
  String getExternalId();

  void serialize(@Nonnull T stub, @Nonnull StubOutputStream dataStream) throws IOException;
  @Nonnull
  T deserialize(@Nonnull StubInputStream dataStream, P parentStub) throws IOException;

  void indexStub(@Nonnull T stub, @Nonnull IndexSink sink);
}
