/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.component.persist.scheme;

import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.Parent;
import javax.annotation.Nonnull;

import java.io.IOException;

public interface SchemeProcessor<T, E extends ExternalizableScheme> {
  E readScheme(@Nonnull Document schemeContent) throws InvalidDataException, IOException, JDOMException;

  Parent writeScheme(@Nonnull E scheme) throws WriteExternalException;

  boolean shouldBeSaved(@Nonnull E scheme);

  void initScheme(@Nonnull E scheme);

  void onSchemeAdded(@Nonnull E scheme);

  void onSchemeDeleted(@Nonnull E scheme);

  void onCurrentSchemeChanged(final E oldCurrentScheme);

  @Nonnull
  String getName(@Nonnull T immutableElement);
}
