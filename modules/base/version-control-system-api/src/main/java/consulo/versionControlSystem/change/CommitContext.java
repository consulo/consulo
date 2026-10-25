/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import consulo.util.dataholder.Key;
import consulo.util.dataholder.UnprotectedUserDataHolder;
import consulo.util.dataholder.UserDataHolder;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Contains specific commit data used in the commit flow.
 * <p>
 * It can be used to pass data between
 * {@link CommitExecutor} or {@link consulo.versionControlSystem.checkin.CheckinHandler} and
 * {@link CommitSession} or {@link consulo.versionControlSystem.checkin.CheckinEnvironment}.
 * <p>
 * For example, if "commit and push" was invoked and so push should start right after commit.
 *
 * @author Irina.Chernushina
 * @since 2011-09-15
 */
public class CommitContext extends UnprotectedUserDataHolder implements UserDataHolder {
    private static final Key<UserDataHolder> COMMIT_EXECUTOR_PROPERTY_MAP = Key.create("Vcs.Commit.ExecutorPropertyMap");
    private static final Key<Set<VirtualFile>> FRESH_UNHOSTED_ROOTS_KEY = Key.create("Git.Commit.FreshUnhostedRoots");

    public CommitContext() {
    }

    public <T> @Nullable T getExecutorProperty(Key<T> key) {
        UserDataHolder map = getUserData(COMMIT_EXECUTOR_PROPERTY_MAP);
        return map == null ? null : map.getUserData(key);
    }

    public <T> T getExecutorProperty(Key<T> key, T defaultValue) {
        T value = getExecutorProperty(key);
        return value == null ? defaultValue : value;
    }

    public <T> void putExecutorProperty(Key<T> key, @Nullable T value) {
        UserDataHolder map = getUserData(COMMIT_EXECUTOR_PROPERTY_MAP);
        if (map == null) {
            map = new UnprotectedUserDataHolder();
            putUserData(COMMIT_EXECUTOR_PROPERTY_MAP, map);
        }
        map.putUserData(key, value);
    }

    /**
     * Roots of repositories without a remote that get their first commit - the commit result offers to share them.
     */
    public @Nullable Set<VirtualFile> getFreshUnhostedRoots() {
        return getUserData(FRESH_UNHOSTED_ROOTS_KEY);
    }

    public void setFreshUnhostedRoots(@Nullable Set<VirtualFile> roots) {
        putUserData(FRESH_UNHOSTED_ROOTS_KEY, roots);
    }

    public void cleanExecutorProperties() {
        putUserData(COMMIT_EXECUTOR_PROPERTY_MAP, null);
    }
}
