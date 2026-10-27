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
package consulo.versionControlSystem.distributed;

import consulo.component.persist.PersistentStateComponent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kirill Likhodedov
 */
public class DvcsRememberedInputs implements PersistentStateComponent<DvcsRememberedInputsState> {

    private DvcsRememberedInputsState myState = new DvcsRememberedInputsState();

    @Override
    public DvcsRememberedInputsState getState() {
        return myState;
    }

    @Override
    public void loadState(DvcsRememberedInputsState state) {
        myState = state;
    }

    public void addUrl(String url) {
        addUrl(url, "");
    }

    public void addUrl(String url, String userName) {
        for (DvcsRememberedInputUrlAndUserName visitedUrl : myState.visitedUrls) {
            if (visitedUrl.url.equalsIgnoreCase(url)) {  // don't add multiple entries for a single url
                if (!userName.isEmpty()) {                 // rewrite username, unless no username is specified
                    visitedUrl.userName = userName;
                }
                return;
            }
        }

        DvcsRememberedInputUrlAndUserName urlAndUserName = new DvcsRememberedInputUrlAndUserName();
        urlAndUserName.url = url;
        urlAndUserName.userName = userName;
        myState.visitedUrls.add(urlAndUserName);
    }

    public @Nullable String getUserNameForUrl(String url) {
        for (DvcsRememberedInputUrlAndUserName urlAndUserName : myState.visitedUrls) {
            if (urlAndUserName.url.equalsIgnoreCase(url)) {
                return urlAndUserName.userName;
            }
        }
        return null;
    }


    public List<String> getVisitedUrls() {
        List<String> urls = new ArrayList<>(myState.visitedUrls.size());
        for (DvcsRememberedInputUrlAndUserName urlAndUserName : myState.visitedUrls) {
            urls.add(urlAndUserName.url);
        }
        return urls;
    }

    public String getCloneParentDir() {
        return myState.cloneParentDir;
    }

    public void setCloneParentDir(String cloneParentDir) {
        myState.cloneParentDir = cloneParentDir;
    }
}
