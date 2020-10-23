// Copyright 2000-2017 JetBrains s.r.o.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package consulo.util.jdom.interner;

import org.jdom.CDATA;
import org.jdom.Element;
import org.jdom.Parent;
import org.jdom.Text;

import javax.annotation.Nonnull;

class ImmutableCDATA extends CDATA {
  ImmutableCDATA(@Nonnull String str) {
    super.setText(str);
  }

  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @Override
  public CDATA clone() {
    return new CDATA(value);
  }

  @Override
  public Element getParent() {
    throw ImmutableElement.immutableError(this);
  }

  //////////////////////////////////////////////////////////////////////////
  @Override
  public CDATA setText(String str) {
    throw ImmutableElement.immutableError(this);
  }

  @Override
  public void append(String str) {
    throw ImmutableElement.immutableError(this);
  }

  @Override
  public void append(Text text) {
    throw ImmutableElement.immutableError(this);
  }

  @Override
  public CDATA detach() {
    throw ImmutableElement.immutableError(this);
  }

  @Override
  protected CDATA setParent(Parent parent) {
    throw ImmutableElement.immutableError(this);
    //return null; // to be able to add this to the other element
  }
}
