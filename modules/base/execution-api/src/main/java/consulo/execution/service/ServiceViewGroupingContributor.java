// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.service;


import java.util.List;

public interface ServiceViewGroupingContributor<T, G> extends ServiceViewContributor<T> {
  
  List<G> getGroups(T service);

  
  ServiceViewDescriptor getGroupDescriptor(G group);
}
