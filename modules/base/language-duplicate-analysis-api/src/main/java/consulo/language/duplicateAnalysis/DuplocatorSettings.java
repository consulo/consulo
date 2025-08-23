// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.duplicateAnalysis;

import consulo.application.Application;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.annotation.Nonnull;

import java.util.HashSet;
import java.util.Set;

@State(name = "DuplocatorSettings", storages = @Storage("duplocatorSettings.xml"))
public class DuplocatorSettings implements PersistentStateComponent<DuplocatorSettings> {
  public boolean DISTINGUISH_VARIABLES = false;
  public boolean DISTINGUISH_FIELDS = false;
  public boolean DISTINGUISH_METHODS = true;
  public boolean DISTINGUISH_TYPES = true;
  public boolean DISTINGUISH_LITERALS = true;
  public boolean CHECK_VALIDITY = true;
  public int LOWER_BOUND = 10;
  public int  DISCARD_COST = 0;
  public Set<String> SELECTED_PROFILES = new HashSet<>();
  public String LAST_SELECTED_LANGUAGE = null;

  public static DuplocatorSettings getInstance() {
    return Application.get().getService(DuplocatorSettings.class);
  }

  @Override
  public DuplocatorSettings getState() {
    return this;
  }

  @Override
  public void loadState(@Nonnull DuplocatorSettings object) {
    XmlSerializerUtil.copyBean(object, this);
  }
}
