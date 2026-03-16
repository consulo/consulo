/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal;

import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.log.VcsCommitMetadata;
import consulo.versionControlSystem.log.VcsLogUserFilter;
import consulo.versionControlSystem.log.VcsUser;
import consulo.versionControlSystem.log.util.VcsUserUtil;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VcsLogUserFilterImpl implements VcsLogUserFilter {
  private static final Logger LOG = Logger.getInstance(VcsLogUserFilterImpl.class);

  
  public static final String ME = "me";

  
  private final Collection<String> myUsers;
  
  private final Map<VirtualFile, VcsUser> myData;
  
  private final MultiMap<String, VcsUser> myAllUsersByNames = MultiMap.create();
  
  private final MultiMap<String, VcsUser> myAllUsersByEmails = MultiMap.create();

  public VcsLogUserFilterImpl(Collection<String> users,
                              Map<VirtualFile, VcsUser> meData,
                              Set<VcsUser> allUsers) {
    myUsers = users;
    myData = meData;

    for (VcsUser user : allUsers) {
      String name = user.getName();
      if (!name.isEmpty()) {
        myAllUsersByNames.putValue(VcsUserUtil.getNameInStandardForm(name), user);
      }
      String email = user.getEmail();
      String nameFromEmail = VcsUserUtil.getNameFromEmail(email);
      if (nameFromEmail != null) {
        myAllUsersByEmails.putValue(VcsUserUtil.getNameInStandardForm(nameFromEmail), user);
      }
    }
  }

  @Override
  
  public Collection<VcsUser> getUsers(VirtualFile root) {
    Set<VcsUser> result = new HashSet<>();
    for (String user : myUsers) {
      result.addAll(getUsers(root, user));
    }
    return result;
  }

  
  private Set<VcsUser> getUsers(VirtualFile root, String name) {
    Set<VcsUser> users = new HashSet<>();
    if (ME.equals(name)) {
      VcsUser vcsUser = myData.get(root);
      if (vcsUser != null) {
        users.addAll(getUsers(vcsUser.getName())); // do not just add vcsUser, also add synonyms
        String emailNamePart = VcsUserUtil.getNameFromEmail(vcsUser.getEmail());
        if (emailNamePart != null) {
          users.addAll(getUsers(emailNamePart));
        }
      }
    }
    else {
      users.addAll(getUsers(name));
    }
    return users;
  }

  
  public Collection<String> getUserNamesForPresentation() {
    return myUsers;
  }

  @Override
  public boolean matches(VcsCommitMetadata commit) {
    return ContainerUtil.exists(myUsers, name -> {
      Set<VcsUser> users = getUsers(commit.getRoot(), name);
      if (!users.isEmpty()) {
        return users.contains(commit.getAuthor());
      }
      else if (!name.equals(ME)) {
        String lowerUser = VcsUserUtil.nameToLowerCase(name);
        boolean result = VcsUserUtil.nameToLowerCase(commit.getAuthor().getName()).equals(lowerUser) ||
                         VcsUserUtil.emailToLowerCase(commit.getAuthor().getEmail()).startsWith(lowerUser + "@");
        if (result) {
          LOG.warn("Unregistered author " + commit.getAuthor() + " for commit " + commit.getId().asString() + "; search pattern " + name);
        }
        return result;
      }
      return false;
    });
  }

  private Set<VcsUser> getUsers(String name) {
    Set<VcsUser> result = new HashSet<>();

    result.addAll(myAllUsersByNames.get(VcsUserUtil.getNameInStandardForm(name)));
    result.addAll(myAllUsersByEmails.get(VcsUserUtil.getNameInStandardForm(name)));

    return result;
  }

  @Override
  public String toString() {
    return "author: " + StringUtil.join(myUsers, ", ");
  }
}