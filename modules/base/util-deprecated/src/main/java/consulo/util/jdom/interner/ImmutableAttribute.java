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

import org.jdom.*;

import javax.annotation.Nonnull;

class ImmutableAttribute extends Attribute {
  ImmutableAttribute(@Nonnull String name, @Nonnull String value, AttributeType type, @Nonnull Namespace namespace) {
    super.setName(name);
    super.setValue(value);
    super.setNamespace(namespace);
    super.setAttributeType(type);
  }

  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @Override
  public Attribute clone() {
    return new Attribute(name, value, type, namespace);
  }

  @Override
  public Element getParent() {
    throw ImmutableElement.immutableError(this);
  }

  /////////////////////////////////////////////////////////
  @Override
  protected Attribute setParent(Element parent) {
    throw ImmutableElement.immutableError(this);
    //return null; // to be able to add this to the other element
  }

  @Override
  public Attribute detach() {
    throw ImmutableElement.immutableError(this);
  }

  @Override
  public Attribute setName(@Nonnull String name) {
    throw ImmutableElement.immutableError(this);
  }

  @Override
  public Attribute setNamespace(Namespace namespace) {
    throw ImmutableElement.immutableError(this);
  }

  @Override
  public Attribute setValue(@Nonnull String value) {
    throw ImmutableElement.immutableError(this);
  }

  @Override
  public Attribute setAttributeType(AttributeType type) {
    throw ImmutableElement.immutableError(this);
  }
}
