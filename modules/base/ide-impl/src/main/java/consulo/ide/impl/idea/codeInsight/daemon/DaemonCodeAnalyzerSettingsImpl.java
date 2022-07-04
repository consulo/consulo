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

package consulo.ide.impl.idea.codeInsight.daemon;

import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.ide.impl.idea.codeInspection.ex.InspectionProfileImpl;
import consulo.ide.impl.idea.openapi.util.JDOMUtil;
import consulo.ide.impl.idea.profile.codeInspection.InspectionProfileManagerImpl;
import consulo.language.editor.DaemonCodeAnalyzerSettings;
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import consulo.logging.Logger;
import consulo.util.xml.serializer.DefaultJDOMExternalizer;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

@Singleton
@State(name = "DaemonCodeAnalyzerSettings", storages = {@Storage("editor.codeinsight.xml")})
@ServiceImpl
public class DaemonCodeAnalyzerSettingsImpl extends DaemonCodeAnalyzerSettings implements PersistentStateComponent<Element>, Cloneable {
  private static final Logger LOG = Logger.getInstance(DaemonCodeAnalyzerSettingsImpl.class);

  private static final String ROOT_TAG = "root";
  private static final String PROFILE_ATT = "profile";
  private final InspectionProfileManagerImpl myManager;

  @Inject
  public DaemonCodeAnalyzerSettingsImpl(InspectionProfileManager manager) {
    myManager = (InspectionProfileManagerImpl)manager;
  }

  @Override
  public boolean isCodeHighlightingChanged(DaemonCodeAnalyzerSettings oldSettings) {
    try {
      Element rootNew = new Element(ROOT_TAG);
      writeExternal(rootNew);
      Element rootOld = new Element(ROOT_TAG);
      ((DaemonCodeAnalyzerSettingsImpl)oldSettings).writeExternal(rootOld);

      return !JDOMUtil.areElementsEqual(rootOld, rootNew);
    }
    catch (WriteExternalException e) {
      LOG.error(e);
    }

    return false;
  }

  @Override
  public DaemonCodeAnalyzerSettingsImpl clone() {
    DaemonCodeAnalyzerSettingsImpl settings = new DaemonCodeAnalyzerSettingsImpl(myManager);
    settings.AUTOREPARSE_DELAY = AUTOREPARSE_DELAY;
    settings.SHOW_ADD_IMPORT_HINTS = SHOW_ADD_IMPORT_HINTS;
    settings.SHOW_METHOD_SEPARATORS = SHOW_METHOD_SEPARATORS;
    settings.NO_AUTO_IMPORT_PATTERN = NO_AUTO_IMPORT_PATTERN;
    return settings;
  }

  @Override
  public Element getState() {
    Element e = new Element("state");
    try {
      writeExternal(e);
    }
    catch (WriteExternalException ex) {
      LOG.error(ex);
    }
    return e;
  }

  @Override
  public void loadState(final Element state) {
    try {
      readExternal(state);
    }
    catch (InvalidDataException e) {
      LOG.error(e);
    }
  }

  private void readExternal(Element element) throws InvalidDataException {
    DefaultJDOMExternalizer.readExternal(this, element);
    myManager.getConverter().storeEditorHighlightingProfile(element, new InspectionProfileImpl(InspectionProfileConvertor.OLD_HIGHTLIGHTING_SETTINGS_PROFILE));
    myManager.setRootProfile(element.getAttributeValue(PROFILE_ATT));
  }

  private void writeExternal(Element element) throws WriteExternalException {
    DefaultJDOMExternalizer.writeExternal(this, element);
    element.setAttribute(PROFILE_ATT, myManager.getRootProfile().getName());
  }
}
