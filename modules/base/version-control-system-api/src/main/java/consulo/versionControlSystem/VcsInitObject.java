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
package consulo.versionControlSystem;

public enum VcsInitObject {
  MAPPINGS(10),
  CHANGE_LIST_MANAGER(100),
  DIRTY_SCOPE_MANAGER(110),
  COMMITTED_CHANGES_CACHE(200),
  BRANCHES(250),
  REMOTE_REVISIONS_CACHE(300),
  OTHER_INITIALIZATION(350),
  AFTER_COMMON(400);

  private final int myOrder;

  VcsInitObject(final int order) {
    myOrder = order;
  }

  public int getOrder() {
    return myOrder;
  }
}
