package consulo.component.bind;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * @author peter
 */
public class ParameterizedTypeImpl implements ParameterizedType {
  private final Type myRawType;
  private final Type[] myArguments;

  public ParameterizedTypeImpl(Type rawType, Type... arguments) {
    myRawType = rawType;
    myArguments = arguments;
  }

  @Override
  public Type[] getActualTypeArguments() {
    return myArguments;
  }

  @Override
  public Type getRawType() {
    return myRawType;
  }

  @Nullable
  @Override
  public Type getOwnerType() {
    return null;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    return this == o
        || o instanceof ParameterizedTypeImpl that
        && Arrays.equals(myArguments, that.myArguments)
        && myRawType.equals(that.myRawType);
  }

  @Override
  public int hashCode() {
    return 31 * myRawType.hashCode() +
        Arrays.hashCode(myArguments);
  }
}
