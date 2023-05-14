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
package consulo.ide.impl.idea.profile.codeInspection;

import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionToolRegistrar;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.ide.impl.idea.openapi.util.JDOMUtil;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.language.editor.inspection.scheme.Profile;
import consulo.language.editor.inspection.scheme.ProfileManager;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;

public class InspectionProfileLoadUtil {
  @NonNls private static final String PROFILE_NAME_TAG = "profile_name";
  @NonNls public static final String PROFILE_TAG = "profile";

  private static String getProfileName(@Nonnull File file, @Nonnull Element element) {
    String name = getRootElementAttribute(PROFILE_NAME_TAG, element);
    return name != null ? name : FileUtil.getNameWithoutExtension(file);
  }

  private static String getRootElementAttribute(@Nonnull Element element, @NonNls String name) {
    return element.getAttributeValue(name);
  }

  @Nullable
  private static String getRootElementAttribute(@NonNls String name, @Nonnull Element element) {
    return getRootElementAttribute(element, name);
  }

  @Nonnull
  public static String getProfileName(@Nonnull Element element) {
    String name = getRootElementAttribute(element, PROFILE_NAME_TAG);
    if (name != null) return name;
    return "unnamed";
  }

  @Nonnull
  public static Profile load(@Nonnull File file,
                             @Nonnull InspectionToolRegistrar registrar,
                             @Nonnull ProfileManager profileManager) throws JDOMException, IOException, InvalidDataException {
    Element element = JDOMUtil.loadDocument(file).getRootElement();
    InspectionProfileImpl profile = new InspectionProfileImpl(getProfileName(file, element), registrar, profileManager);
    final Element profileElement = element.getChild(PROFILE_TAG);
    if (profileElement != null) {
      element = profileElement;
    }
    profile.readExternal(element);
    return profile;
  }
}
