/*
 * Copyright 2013-2017 consulo.io
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
package consulo.components.impl.stores;

import com.intellij.openapi.components.*;

import java.lang.annotation.Annotation;

/**
 * @author VISTALL
 * @since 27-Feb-17
 */
public class SimpleState implements State {
  private static class SimpleStorage implements Storage {
    private String myFileSpec;
    private RoamingType myType;

    private SimpleStorage(String file, RoamingType type) {
      myFileSpec = file;
      myType = type;
    }

    @Override
    public String value() {
      return null;
    }

    @Override
    public String file() {
      return myFileSpec;
    }

    @Override
    public boolean deprecated() {
      return false;
    }

    @Override
    public RoamingType roamingType() {
      return myType;
    }

    @Override
    public Class<? extends StateSplitterEx> stateSplitter() {
      return StateSplitterEx.class;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
      return SimpleStorage.class;
    }
  }

  private String myName;
  private Storage myStorage;

  public SimpleState(@Storage String name, @Storage String fileSpec, @Storage RoamingType type) {
    myName = name;
    myStorage = new SimpleStorage(fileSpec, type);
  }

  @Override
  public String name() {
    return myName;
  }

  @Override
  public Storage[] storages() {
    return new Storage[]{myStorage};
  }

  @Override
  public String additionalExportFile() {
    return null;
  }

  @Override
  public String defaultStateFilePath() {
    return "";
  }

  @Override
  public Class<? extends Annotation> annotationType() {
    return SimpleState.class;
  }
}
