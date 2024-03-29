/*****************************************************************************
 * Copyright (C) PicoContainer Organization. All rights reserved.            *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package consulo.component.impl.internal.inject;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * Exception thrown when some of the component's dependencies are not satisfiable.
 *
 * @author Aslak Helles&oslash;y
 * @author Mauro Talevi
 * @version $Revision: 2838 $
 */
class UnsatisfiableDependenciesException extends PicoIntrospectionException {

  private final ComponentAdapter instantiatingComponentAdapter;
  private final Set unsatisfiableDependencies;
  private final Type unsatisfiedDependencyType;
  private final InstanceContainer leafContainer;

  public UnsatisfiableDependenciesException(ComponentAdapter instantiatingComponentAdapter, Set unsatisfiableDependencies, InstanceContainer leafContainer) {
    super(instantiatingComponentAdapter.getComponentImplClass().getName() +
          " has unsatisfiable dependencies: " +
          unsatisfiableDependencies +
          " where " +
          leafContainer +
          " was the leaf container being asked for dependencies.");
    this.instantiatingComponentAdapter = instantiatingComponentAdapter;
    this.unsatisfiableDependencies = unsatisfiableDependencies;
    this.unsatisfiedDependencyType = null;
    this.leafContainer = leafContainer;
  }

  public UnsatisfiableDependenciesException(ComponentAdapter instantiatingComponentAdapter, Type unsatisfiedDependencyType, Set unsatisfiableDependencies, InstanceContainer leafContainer) {
    super(instantiatingComponentAdapter.getComponentImplClass().getName() +
          " has unsatisfied dependency: " +
          unsatisfiedDependencyType +
          " among unsatisfiable dependencies: " +
          unsatisfiableDependencies +
          " where " +
          leafContainer +
          " was the leaf container being asked for dependencies.");
    this.instantiatingComponentAdapter = instantiatingComponentAdapter;
    this.unsatisfiableDependencies = unsatisfiableDependencies;
    this.unsatisfiedDependencyType = unsatisfiedDependencyType;
    this.leafContainer = leafContainer;
  }

  public ComponentAdapter getUnsatisfiableComponentAdapter() {
    return instantiatingComponentAdapter;
  }

  public Set getUnsatisfiableDependencies() {
    return unsatisfiableDependencies;
  }

  public Type getUnsatisfiedDependencyType() {
    return unsatisfiedDependencyType;
  }

  public InstanceContainer getLeafContainer() {
    return leafContainer;
  }

}
