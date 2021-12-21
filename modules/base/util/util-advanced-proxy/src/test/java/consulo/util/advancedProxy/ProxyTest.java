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
package consulo.util.advancedProxy;

import consulo.util.advandedProxy.AdvancedProxyBuilder;
import consulo.util.advandedProxy.internal.impl.AdvancedProxyTesting;
import org.junit.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author peter
 */
public class ProxyTest extends Assert {
  @Before
  public void before() {
    AdvancedProxyTesting.IS_INSIDE_TEST = true;
  }

  @After
  public void after() {
    AdvancedProxyTesting.IS_INSIDE_TEST = false;
  }

  @Test
  @Ignore
  // disable test since it will can provide SOFE due getField will override in impl class
  public void testExtendClass() throws Throwable {
    final List<String> invocations = new ArrayList<>();
    Implementation implementation = AdvancedProxyBuilder.create(Implementation.class).withInterfaces(Interface3.class).withInvocationHandler(new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        invocations.add(method.getName());
        if (Object.class.equals(method.getDeclaringClass())) {
          return method.invoke(this, args);
        }
        return Implementation.class.getMethod("getField").invoke(proxy);
      }
    }).withSuperConstructorArguments("239").build();

    implementation.hashCode();
    implementation.method();
    assertEquals("239", implementation.getFoo());
    implementation.setField("42");
    assertEquals("42", implementation.getBar());
    assertEquals("42", implementation.toString());
    assertEquals(Arrays.asList("hashCode", "getFoo", "getFoo", "getBar"), invocations);

    assertEquals("42", Interface1.class.getMethod("getFoo").invoke(implementation));

    assertEquals("42", Interface3.class.getMethod("bar").invoke(implementation));

    assertEquals("42", Interface1.class.getMethod("foo").invoke(implementation));
    assertEquals("42", Interface2.class.getMethod("foo").invoke(implementation));
    assertEquals("42", Interface2.class.getMethod("foo").invoke(implementation));
    assertEquals("42", Implementation.class.getMethod("foo").invoke(implementation));
  }

  public interface Interface1 {
    Object getFoo();

    Object foo();
  }

  public interface Interface2 extends Interface1 {
    @Override
    CharSequence getFoo();

    @Override
    CharSequence foo();
  }

  public interface Interface3 extends Interface1 {
    @Override
    String foo();

    String bar();
  }

  private abstract static class Implementation implements Interface2 {
    private String myField;

    protected Implementation(final String field) {
      myField = field;
    }

    public String getField() {
      return myField;
    }

    public void setField(final String field) {
      myField = field;
    }

    public void method() {
      getFoo();
    }

    public String toString() {
      return myField;
    }

    @Override
    public abstract String getFoo();

    public abstract String getBar();

  }

  @Test
  @Ignore
  public void testAddInterfaces() throws Throwable {
    final BaseImpl proxy = AdvancedProxyBuilder.create(BaseImpl.class).withInterfaces(BaseIEx.class).withInvocationHandler((proxy1, method, args) -> "a").build();
    assertEquals(proxy.sayA(), "a");
    assertEquals(((BaseI)proxy).sayA(), "a");
    assertEquals(((BaseIEx)proxy).sayA(), "a");
  }

  public interface BaseI {
    Object sayA();
  }

  public interface BaseIEx extends BaseI {
    @Override
    CharSequence sayA();
  }

  public static abstract class BaseImpl implements BaseI {
    @Override
    public String sayA() {
      return "a";
    }
  }

  public static abstract class AbstractBase implements BaseI {
    @Override
    public abstract String sayA();

    public abstract static class AbstractBaseImpl extends AbstractBase {
    }
  }

  @Test
  public void testCovariantFromInterface() throws Throwable {
    final AbstractBase.AbstractBaseImpl proxy =
            AdvancedProxyBuilder.<AbstractBase.AbstractBaseImpl>create(AbstractBase.AbstractBaseImpl.class).withInvocationHandler((proxy1, method, args) -> "a").build();

    assertEquals(proxy.sayA(), "a");
    assertEquals(((AbstractBase)proxy).sayA(), "a");
    assertEquals(((BaseI)proxy).sayA(), "a");
  }

  public static class CovariantFromBaseClassTest {
    public static interface Intf {
      String sayA();
    }

    public static class Base {
      public Object sayA() {
        return "beeee";
      }
    }

    public abstract static class Impl extends Base implements Intf {
      @Override
      public abstract String sayA();
    }
  }

  @Test
  @Ignore
  public void testCovariantFromBaseClass() throws Throwable {
    final CovariantFromBaseClassTest.Impl proxy =
            AdvancedProxyBuilder.<CovariantFromBaseClassTest.Impl>create(CovariantFromBaseClassTest.Impl.class).withInvocationHandler((proxy1, method, args) -> "a").withInterceptObjectMethods(false)
                    .build();

    assertEquals(proxy.sayA(), "a");
    assertEquals(((CovariantFromBaseClassTest.Base)proxy).sayA(), "a");
    assertEquals(((CovariantFromBaseClassTest.Intf)proxy).sayA(), "a");
  }

  public static interface InterfaceWithDefaultMethods {
    default void test() {
    }

    default String foo() {
      return "foo";
    }
  }

  public static class DefaultImpl {

  }

  @Test
  public void testDefaultInterfaceMethods() {
    DefaultImpl defaultImpl = AdvancedProxyBuilder.create(DefaultImpl.class).withInterfaces(InterfaceWithDefaultMethods.class).withInvocationHandler((proxy, method, args) -> {
      throw new IllegalArgumentException("should not never called. Method: " + method);
    }).build();

    ((InterfaceWithDefaultMethods)defaultImpl).test();

    assertEquals("foo", ((InterfaceWithDefaultMethods)defaultImpl).foo());
  }

  public static class SomeBaseClassWithArguments {
    private final String myA;
    private final String myB;

    public SomeBaseClassWithArguments(String a, String b) {
      myA = a;
      myB = b;
    }

    public SomeBaseClassWithArguments(String a) {
      myA = a;
      myB = "error";
    }
  }

  @Test
  public void testArgumentsCall() {
    SomeBaseClassWithArguments proxy = AdvancedProxyBuilder.create(SomeBaseClassWithArguments.class).withSuperConstructorArguments("a", "b").withInvocationHandler((o, method, args) -> null).build();

    assertEquals(proxy.myA, "a");
    assertEquals(proxy.myB, "b");
  }
}
