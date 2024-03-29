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
package consulo.versionControlSystem.change;

import java.util.Date;

public class LogicalLock {
  private final boolean myIsLocal;
  private final String myOwner;
  private final String myComment;
  private final Date myCreationDate;
  private final Date myExpirationDate;

  public LogicalLock(boolean isLocal, String owner, String comment, Date creationDate, Date expirationDate) {
    myIsLocal = isLocal;
    myOwner = owner;
    myComment = comment;
    myCreationDate = creationDate;
    myExpirationDate = expirationDate;
  }

  public String getOwner() {
    return myOwner;
  }

  public String getComment() {
    return myComment;
  }

  public Date getCreationDate() {
    return myCreationDate;
  }

  public Date getExpirationDate() {
    return myExpirationDate;
  }

  public boolean isIsLocal() {
    return myIsLocal;
  }
}
