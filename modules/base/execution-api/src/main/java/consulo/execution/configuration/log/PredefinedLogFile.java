/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.execution.configuration.log;

import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.JDOMExternalizable;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class PredefinedLogFile implements JDOMExternalizable {
  @NonNls private static final String ID_ATTRIBUTE = "id";
  @NonNls private static final String ENABLED_ATTRIBUTE = "enabled";
  private String myId;
  private boolean myEnabled;

  public PredefinedLogFile() {
  }


  public PredefinedLogFile(PredefinedLogFile logFile) {
    myEnabled = logFile.myEnabled;
    myId = logFile.myId;
  }

  public PredefinedLogFile(@Nonnull @NonNls String id, boolean enabled) {
    myEnabled = enabled;
    myId = id;
  }

  public boolean isEnabled() {
    return myEnabled;
  }

  public void setEnabled(boolean enabled) {
    myEnabled = enabled;
  }

  public String getId() {
    return myId;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PredefinedLogFile that = (PredefinedLogFile)o;
    return myId.equals(that.myId);
  }

  public int hashCode() {
    return myId.hashCode();
  }


  @Override
  public void readExternal(Element element) throws InvalidDataException {
    myId = element.getAttributeValue(ID_ATTRIBUTE);
    myEnabled = Boolean.parseBoolean(element.getAttributeValue(ENABLED_ATTRIBUTE));
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    element.setAttribute(ID_ATTRIBUTE, myId);
    element.setAttribute(ENABLED_ATTRIBUTE, String.valueOf(myEnabled));
  }
}
