// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.memory;

import java.util.List;

public interface TypeInfo {
    
    String name();

    
    List<ReferenceInfo> getInstances(int limit);

    boolean canGetInstanceInfo();
}
