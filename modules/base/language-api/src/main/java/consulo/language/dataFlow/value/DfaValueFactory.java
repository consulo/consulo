// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package consulo.language.dataFlow.value;

import consulo.language.dataFlow.types.DfType;
import consulo.language.dataFlow.value.DfaControlTransferValue.TransferTarget;
import consulo.language.dataFlow.value.DfaControlTransferValue.Trap;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.language.psi.PsiElement;
import consulo.util.collection.FList;
import consulo.util.collection.FactoryMap;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DfaValueFactory {
  private final List<DfaValue> myValues = new ArrayList<>();
  private final List<Synthetic> mySynthetics = new ArrayList<>();
  private final Project myProject;
  private @Nullable PsiElement myContext;

  /**
   * @param project a project in which context the analysis is performed
   */
  public DfaValueFactory(Project project) {
    myProject = project;
    myValues.add(null);
    myVarFactory = new DfaVariableValue.Factory(this);
    myBoxedFactory = new DfaWrappedValue.Factory(this);
    myBinOpFactory = new DfaBinOpValue.Factory(this);
    myTypeValueFactory = new DfaTypeValue.Factory(this);
  }

  /**
   * @return context (can be used to determine variable initial values)
   */
  public @Nullable PsiElement getContext() {
    return myContext;
  }

  /**
   * Sets the analysis context used to determine variable initial values.
   * Must be set before interpretation start by {@link consulo.language.dataFlow.interpreter.DataFlowInterpreter}.
   * Has no meaning during flow building.
   * 
   * @param context current analysis context
   */
  public void setContext(PsiElement context) {
    myContext = context;
    for (DfaValue value : myValues) {
      if (value instanceof DfaVariableValue) {
        ((DfaVariableValue)value).resetInherentType();
      }
    }
  }

  int registerValue(DfaValue value) {
    myValues.add(value);
    return myValues.size() - 1;
  }

  public DfaValue getValue(int id) {
    return myValues.get(id);
  }

  /**
   * @param offset offset to search temporary variables from
   * @return temporary variables created at or after the given offset
   */
  public List<VariableDescriptor> getTempVariableDescriptorsFrom(int offset) {
    List<VariableDescriptor> result = new ArrayList<>();
    for (Synthetic synthetic : mySynthetics) {
      if (synthetic.myLocation >= offset) {
        result.add(synthetic);
      }
    }
    return result;
  }

  /**
   * Creates temporary variable
   * @param offset offset to create a temporary variable at
   * @param type type of the a temporary variable
   * @return created temporary variable
   */
  public DfaVariableValue createTempVariable(int offset, DfType type) {
    Synthetic descriptor = new Synthetic(offset, type);
    mySynthetics.add(descriptor);
    return myVarFactory.createVariableValue(descriptor);
  }

  /**
   * @param var variable to check
   * @return true if it's a temp variable
   */
  public static boolean isTempVariable(DfaVariableValue var) {
    return var.getDescriptor() instanceof Synthetic;
  }

  public DfaTypeValue getUnknown() {
    return fromDfType(DfType.TOP);
  }

  /**
   * @return a special sentinel value that never equals to anything else (even unknown value) and
   * sometimes pushed on the stack as control flow implementation detail.
   * It's never assigned to the variable or merged with any other value.
   */
  public DfaValue getSentinel() {
    return mySentinelValue;
  }

  public Project getProject() {
    return myProject;
  }

  public DfaTypeValue fromDfType(DfType dfType) {
    return myTypeValueFactory.create(dfType);
  }

  public Collection<DfaValue> getValues() {
    return Collections.unmodifiableCollection(myValues);
  }

  public DfaControlTransferValue controlTransfer(TransferTarget kind, FList<Trap> traps) {
    return Objects.requireNonNull(myControlTransfers.get(Pair.create(kind, traps)));
  }

  private final Map<Pair<TransferTarget, FList<Trap>>, DfaControlTransferValue> myControlTransfers =
    FactoryMap.create(p -> new DfaControlTransferValue(this, p.first, p.second));

  private final DfaVariableValue.Factory myVarFactory;
  private final DfaWrappedValue.Factory myBoxedFactory;
  private final DfaBinOpValue.Factory myBinOpFactory;
  private final DfaTypeValue.Factory myTypeValueFactory;
  private final DfaValue mySentinelValue = new DfaValue(this) {
    @Override
    public DfaValue bindToFactory(DfaValueFactory factory) {
      return factory.mySentinelValue;
    }

    @Override
    public String toString() {
      return "SENTINEL";
    }
  };

  public DfaVariableValue.Factory getVarFactory() {
    return myVarFactory;
  }

  public DfaWrappedValue.Factory getWrapperFactory() {
    return myBoxedFactory;
  }

  public DfaBinOpValue.Factory getBinOpFactory() {
    return myBinOpFactory;
  }

  private static final class Synthetic implements VariableDescriptor {
    private final int myLocation;
    private final DfType myType;

    private Synthetic(int location, DfType type) {
      myLocation = location;
      myType = type;
    }

    @Override
    public String toString() {
      return "tmp$" + myLocation;
    }

    @Override
    public DfType getDfType(@Nullable DfaVariableValue qualifier) {
      return myType;
    }

    @Override
    public boolean isStable() {
      return true;
    }
  }
}
