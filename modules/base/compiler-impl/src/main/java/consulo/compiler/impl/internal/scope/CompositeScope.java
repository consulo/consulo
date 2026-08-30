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
package consulo.compiler.impl.internal.scope;

import consulo.compiler.scope.CompileScope;
import consulo.compiler.util.ExportableUserDataHolderBase;
import consulo.module.Module;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.fileType.FileType;

import java.nio.file.Path;
import java.util.*;

/**
 * @author Eugene Zhuravlev
 * @since 2003-02-05
 */
public class CompositeScope extends ExportableUserDataHolderBase implements CompileScope {
    private final List<CompileScope> myScopes = new ArrayList<>();

    public CompositeScope(CompileScope scope1, CompileScope scope2) {
        addScope(scope1);
        addScope(scope2);
    }

    public CompositeScope(CompileScope[] scopes) {
        for (CompileScope scope : scopes) {
            addScope(scope);
        }
    }

    public CompositeScope(Collection<? extends CompileScope> scopes) {
        for (CompileScope scope : scopes) {
            addScope(scope);
        }
    }

    private void addScope(CompileScope scope) {
        if (scope instanceof CompositeScope compositeScope) {
            for (CompileScope childScope : compositeScope.myScopes) {
                addScope(childScope);
            }
        }
        else {
            myScopes.add(scope);
        }

        Map<Key, Object> map = scope.exportUserData();
        for (Map.Entry<Key, Object> entry : map.entrySet()) {
            putUserData(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Collection<Path> getFiles(FileType fileType) {
        Set<Path> allFiles = new HashSet<>();
        for (CompileScope scope : myScopes) {
            allFiles.addAll(scope.getFiles(fileType));
        }
        return allFiles;
    }

    @Override
    public boolean belongs(Path path) {
        for (CompileScope scope : myScopes) {
            if (scope.belongs(path)) {
                return true;
            }
        }
        return false;
    }

    
    @Override
    public Module[] getAffectedModules() {
        Set<Module> modules = new HashSet<>();
        for (CompileScope compileScope : myScopes) {
            ContainerUtil.addAll(modules, compileScope.getAffectedModules());
        }
        return modules.toArray(new Module[modules.size()]);
    }

    @Override
    public <T> T getUserData(Key<T> key) {
        for (CompileScope compileScope : myScopes) {
            T userData = compileScope.getUserData(key);
            if (userData != null) {
                return userData;
            }
        }
        return super.getUserData(key);
    }

    @Override
    public boolean includeTestScope() {
        for (CompileScope scope : myScopes) {
            if (scope.includeTestScope()) {
                return true;
            }
        }
        return false;
    }

    public Collection<CompileScope> getScopes() {
        return Collections.unmodifiableList(myScopes);
    }
}
