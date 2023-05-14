/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;

/**
 * @author yole
 */
public abstract class BaseSchemeProcessor<T, E extends ExternalizableScheme> implements SchemeProcessor<T, E>, SchemeExtensionProvider {
  @Override
  public void initScheme(@Nonnull E scheme) {
  }

  @Override
  public void onSchemeAdded(@Nonnull E scheme) {
  }

  @Override
  public void onSchemeDeleted(@Nonnull E scheme) {
  }

  @Override
  public void onCurrentSchemeChanged(E newCurrentScheme) {
  }

  @Nullable
  public E readScheme(@Nonnull Element element) throws InvalidDataException, IOException, JDOMException {
    return readScheme(new Document((Element)element.detach()));
  }

  @Nullable
  /**
   * @param duringLoad If occurred during {@link SchemeManager#loadSchemes()} call
   */
  public E readScheme(@Nonnull Element element, boolean duringLoad) throws InvalidDataException, IOException, JDOMException {
    return readScheme(element);
  }

  @Override
  public E readScheme(@Nonnull Document schemeContent) throws InvalidDataException, IOException, JDOMException {
    throw new AbstractMethodError();
  }

  public enum State {
    UNCHANGED, NON_PERSISTENT, POSSIBLY_CHANGED
  }

  @Override
  public boolean shouldBeSaved(@Nonnull E scheme) {
    return true;
  }

  @Nonnull
  public State getState(@Nonnull E scheme) {
    return shouldBeSaved(scheme) ? State.POSSIBLY_CHANGED : State.NON_PERSISTENT;
  }

  @Override
  public boolean isUpgradeNeeded() {
    return false;
  }

  @Nonnull
  @Override
  public String getSchemeExtension() {
    return ".xml";
  }
}
