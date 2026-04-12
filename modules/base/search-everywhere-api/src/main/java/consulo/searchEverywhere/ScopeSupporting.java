// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.searchEverywhere;

import consulo.content.scope.ScopeDescriptor;

import java.util.List;

/**
 * Interface for search everywhere contributors that support scope selection.
 */
public interface ScopeSupporting {

    ScopeDescriptor getScope();

    void setScope(ScopeDescriptor scope);

    List<ScopeDescriptor> getSupportedScopes();
}
