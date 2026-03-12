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
package consulo.util.xml.fastReader;

import org.jspecify.annotations.Nullable;

/**
 * @author peter
 */
public class XmlFileHeader {
  public static final XmlFileHeader EMPTY = new XmlFileHeader(null, null, null, null);

  @Nullable
  private final String myRootTagLocalName;
  @Nullable
  private final String myRootTagNamespace;
  @Nullable
  private final String myPublicId;
  @Nullable
  private final String mySystemId;

  public XmlFileHeader(
    @Nullable String rootTagLocalName,
    @Nullable String rootTagNamespace,
    @Nullable String publicId,
    @Nullable String systemId
  ) {
    myPublicId = publicId;
    myRootTagLocalName = rootTagLocalName;
    myRootTagNamespace = rootTagNamespace;
    mySystemId = systemId;
  }

  @Nullable
  public String getPublicId() {
    return myPublicId;
  }

  @Nullable
  public String getRootTagLocalName() {
    return myRootTagLocalName;
  }

  @Nullable
  public String getRootTagNamespace() {
    return myRootTagNamespace;
  }

  @Nullable
  public String getSystemId() {
    return mySystemId;
  }

  @Override
  public String toString() {
    return "XmlFileHeader: name=" + myRootTagLocalName + "; namespace=" + myRootTagNamespace + "; publicId=" + myPublicId + "; systemId=" + mySystemId;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof XmlFileHeader)) return false;

    XmlFileHeader header = (XmlFileHeader)o;

    if (myPublicId != null ? !myPublicId.equals(header.myPublicId) : header.myPublicId != null) return false;
    if (myRootTagLocalName != null ? !myRootTagLocalName.equals(header.myRootTagLocalName) : header.myRootTagLocalName != null)
      return false;
    if (myRootTagNamespace != null ? !myRootTagNamespace.equals(header.myRootTagNamespace) : header.myRootTagNamespace != null)
      return false;
    if (mySystemId != null ? !mySystemId.equals(header.mySystemId) : header.mySystemId != null) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = (myRootTagLocalName != null ? myRootTagLocalName.hashCode() : 0);
    result = 31 * result + (myRootTagNamespace != null ? myRootTagNamespace.hashCode() : 0);
    result = 31 * result + (myPublicId != null ? myPublicId.hashCode() : 0);
    result = 31 * result + (mySystemId != null ? mySystemId.hashCode() : 0);
    return result;
  }
}
