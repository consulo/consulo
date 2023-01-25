/*
 * Copyright 2013-2022 consulo.io
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
package consulo.internal.injecting.binding;

import com.squareup.javapoet.*;
import consulo.annotation.component.*;
import jakarta.inject.Inject;

import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author VISTALL
 * @see https://github.com/google/auto/blob/master/service/processor/src/main/java/com/google/auto/service/processor/AutoServiceProcessor.java
 * @since 16-Jun-22
 */
@SupportedAnnotationTypes({InjectingBindingProcessor.SERVICE_IMPL, InjectingBindingProcessor.EXTENSION_IMPL, InjectingBindingProcessor.TOPIC_IMPL, InjectingBindingProcessor.ACTION_IMPL})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class InjectingBindingProcessor extends BindingProcessor {
  private static record AnnotationResolveInfo(Annotation annotation, TypeElement typeElement) {
  }

  public static final String SERVICE_IMPL = "consulo.annotation.component.ServiceImpl";
  public static final String EXTENSION_IMPL = "consulo.annotation.component.ExtensionImpl";
  public static final String TOPIC_IMPL = "consulo.annotation.component.TopicImpl";
  public static final String ACTION_IMPL = "consulo.annotation.component.ActionImpl";

  private Map<String, Class<? extends Annotation>> myApiAnnotations = new HashMap<>();

  public InjectingBindingProcessor() {
    myApiAnnotations.put(SERVICE_IMPL, ServiceAPI.class);
    myApiAnnotations.put(EXTENSION_IMPL, ExtensionAPI.class);
    myApiAnnotations.put(TOPIC_IMPL, TopicAPI.class);
    myApiAnnotations.put(ACTION_IMPL, ActionAPI.class);
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (annotations.isEmpty()) {
      return true;
    }

    AnnotationSpec suppressWarning = AnnotationSpec.builder(SuppressWarnings.class).addMember("value", CodeBlock.of("$S", "ALL")).build();

    Filer filer = processingEnv.getFiler();

    Map<String, Set<String>> providers = new HashMap<>();

    String injectingBindingClassName = "consulo.component.bind.InjectingBinding";
    ClassName injectingBindingClass = ClassName.bestGuess(injectingBindingClassName);

    for (TypeElement annotation : annotations) {
      Set<? extends Element> elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(annotation);

      Class<? extends Annotation> apiClass = myApiAnnotations.get(annotation.getQualifiedName().toString());
      if (apiClass == null) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@" + annotation.getQualifiedName() + " not supported");
      }

      Class<? extends Annotation> annotationClazz = null;
      try {
        annotationClazz = (Class<? extends Annotation>)Class.forName(annotation.getQualifiedName().toString());
      }
      catch (ClassNotFoundException e) {
        throw new Error(e);
      }

      for (Element element : elementsAnnotatedWith) {
        if (!(element instanceof TypeElement)) {
          continue;
        }

        TypeElement typeElement = (TypeElement)element;
        AnnotationResolveInfo apiInfo = findAnnotationInSuper(typeElement, apiClass);
        if (apiInfo == null) {
          processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Can't find single @" + apiClass + " annotation for: " + typeElement.getQualifiedName() + " in super classes.", typeElement);
          return false;
        }

        try {
          String bindingQualifiedName = typeElement.getQualifiedName() + "_Binding";
          JavaFileObject bindingObject = filer.createSourceFile(bindingQualifiedName);

          providers.computeIfAbsent(injectingBindingClassName, (c) -> new HashSet<>()).add(bindingQualifiedName);

          TypeSpec.Builder bindBuilder = TypeSpec.classBuilder(typeElement.getSimpleName().toString() + "_Binding");
          bindBuilder.addModifiers(Modifier.PUBLIC, Modifier.FINAL);
          bindBuilder.addAnnotation(suppressWarning);
          bindBuilder.addSuperinterface(injectingBindingClass);

          bindBuilder.addMethod(MethodSpec.methodBuilder("getApiClass").returns(Class.class).addModifiers(Modifier.PUBLIC)
                                        .addCode(CodeBlock.of("return $T.class;", ClassName.bestGuess(apiInfo.typeElement().getQualifiedName().toString()))).build());

          bindBuilder.addMethod(MethodSpec.methodBuilder("getApiClassName").returns(String.class).addModifiers(Modifier.PUBLIC)
                                        .addCode(CodeBlock.of("return $S;", apiInfo.typeElement().getQualifiedName().toString())).build());

          bindBuilder.addMethod(MethodSpec.methodBuilder("getImplClass").returns(Class.class).addModifiers(Modifier.PUBLIC).addCode(CodeBlock.of("return $T.class;", typeElement)).build());

          bindBuilder.addMethod(MethodSpec.methodBuilder("getComponentAnnotationClass").addModifiers(Modifier.PUBLIC).returns(Class.class)
                                        .addCode(CodeBlock.of("return $T.class;", apiInfo.annotation().annotationType())).build());

          ComponentScope scope;
          // use TopicImpl scope
          if (apiInfo.annotation() instanceof TopicAPI) {
            TopicImpl topicImpl = typeElement.getAnnotation(TopicImpl.class);
            scope = topicImpl.value();
          }
          else {
            scope = getScope(apiInfo.annotation());
          }

          bindBuilder.addMethod(
                  MethodSpec.methodBuilder("getComponentScope").addModifiers(Modifier.PUBLIC).returns(ComponentScope.class).addCode(CodeBlock.of("return $T.$L;", ComponentScope.class, scope.name()))
                          .build());

          if (!isLazy(apiInfo.annotation())) {
            bindBuilder.addMethod(MethodSpec.methodBuilder("isLazy").addModifiers(Modifier.PUBLIC).returns(boolean.class).addCode(CodeBlock.of("return false;")).build());
          }

          List<? extends VariableElement> injectParameters = null;

          List<? extends Element> allMembers = processingEnv.getElementUtils().getAllMembers(typeElement);
          for (Element member : allMembers) {
            if (member instanceof ExecutableElement) {
              Name simpleName = member.getSimpleName();
              if ("<init>".equals(simpleName.toString())) {
                List<? extends VariableElement> parameters = ((ExecutableElement)member).getParameters();

                Inject injectAnnotation = member.getAnnotation(Inject.class);
                if (injectAnnotation != null) {
                  injectParameters = parameters;
                  break;
                }
              }
            }
          }

          if (injectParameters == null) {
            for (Element member : allMembers) {
              if (member instanceof ExecutableElement) {
                Name simpleName = member.getSimpleName();
                if ("<init>".equals(simpleName.toString())) {
                  List<? extends VariableElement> parameters = ((ExecutableElement)member).getParameters();

                  // default constructor
                  if (parameters.size() == 0 && member.getModifiers().contains(Modifier.PUBLIC)) {
                    injectParameters = parameters;
                    break;
                  }
                }
              }
            }
          }

          if (injectParameters == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "There no public constructor or constructor with @Inject annotation. Injecting impossible", typeElement);
            return false;
          }

          Annotation implAnnotation = typeElement.getAnnotation(annotationClazz);
          int componentProfiles = getComponentProfiles(implAnnotation);

          bindBuilder.addMethod(MethodSpec.methodBuilder("getComponentProfiles").addModifiers(Modifier.PUBLIC).returns(int.class).addCode(CodeBlock.of("return $L;", componentProfiles)).build());

          bindBuilder.addMethod(MethodSpec.methodBuilder("getParametersCount").addModifiers(Modifier.PUBLIC).returns(int.class).addCode(CodeBlock.of("return $L;", injectParameters.size())).build());

          List<TypeName> paramTypes = new ArrayList<>();

          if (!injectParameters.isEmpty()) {
            AppendTypeResult types = appendTypes(injectParameters, paramTypes, "return ", ";", true);

            bindBuilder.addMethod(
                    MethodSpec.methodBuilder("getParameterTypes").addModifiers(Modifier.PUBLIC).returns(Type[].class).addCode(CodeBlock.of(types.result(), types.types().toArray())).build());
          }
          else {
            bindBuilder.addMethod(MethodSpec.methodBuilder("getParameterTypes").addModifiers(Modifier.PUBLIC).returns(Type[].class).addCode(CodeBlock.of("return EMPTY_TYPES;")).build());
          }

          List<TypeName> argsTypes = new ArrayList<>();
          argsTypes.add(toTypeName(typeElement));
          argsTypes.addAll(paramTypes);

          StringBuilder newCreationBuilder = new StringBuilder();
          newCreationBuilder.append("return new $T(");
          for (int i = 0; i < injectParameters.size(); i++) {
            if (i != 0) {
              newCreationBuilder.append(", ");
            }
            newCreationBuilder.append("($T) args[").append(i).append("]");
          }

          newCreationBuilder.append(");");

          bindBuilder.addMethod(MethodSpec.methodBuilder("create").addParameter(Object[].class, "args").addModifiers(Modifier.PUBLIC).returns(Object.class)
                                        .addCode(CodeBlock.of(newCreationBuilder.toString(), argsTypes.toArray())).build());

          TypeSpec bindClass = bindBuilder.build();

          PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(typeElement);

          JavaFile javaFile = JavaFile.builder(packageElement.getQualifiedName().toString(), bindClass).build();

          try (Writer writer = bindingObject.openWriter()) {
            javaFile.writeTo(writer);
          }
        }
        catch (IOException e) {
          processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), typeElement);
        }
      }
    }

    generateConfigFiles(providers);

    return true;
  }

  private static ComponentScope getScope(Annotation annotation) {
    if (annotation instanceof ServiceAPI) {
      return ((ServiceAPI)annotation).value();
    }

    if (annotation instanceof ExtensionAPI) {
      return ((ExtensionAPI)annotation).value();
    }

    if (annotation instanceof ActionAPI) {
      // actions always bind in application injecting scope
      return ComponentScope.APPLICATION;
    }

    throw new UnsupportedOperationException(annotation.getClass().getName());
  }

  private static int getComponentProfiles(Annotation annotation) {
    if (annotation instanceof ServiceImpl) {
      return ((ServiceImpl)annotation).profiles();
    }

    if (annotation instanceof ExtensionImpl) {
      return ((ExtensionImpl)annotation).profiles();
    }

    if (annotation instanceof TopicImpl) {
      return ((TopicImpl)annotation).profiles();
    }

    if (annotation instanceof ActionImpl) {
      return ((ActionImpl)annotation).profiles();
    }

    throw new UnsupportedOperationException(annotation.getClass().getName());
  }

  private static boolean isLazy(Annotation annotation) {
    if (annotation instanceof ServiceAPI) {
      return ((ServiceAPI)annotation).lazy();
    }
    return true;
  }

  // TODO cache it ? per super type
  private static <T extends Annotation> AnnotationResolveInfo findAnnotationInSuper(TypeElement typeElement, Class<T> annotationClass) {
    Set<AnnotationResolveInfo> targets = new LinkedHashSet<>();
    findAnnotationInSuper(typeElement, annotationClass, new HashSet<>(), targets);

    // FIXME [VISTALL] this is dirty hack since we have two api annotations
    if (annotationClass == ActionAPI.class) {
      AnnotationResolveInfo actionGroupInfo = null;
      AnnotationResolveInfo actionInfo = null;

      for (AnnotationResolveInfo target : targets) {
        String qName = target.typeElement().getQualifiedName().toString();
        if (qName.equals("consulo.ui.ex.action.AnAction")) {
          actionInfo = target;
        }
        else if (qName.equals("consulo.ui.ex.action.ActionGroup")) {
          actionGroupInfo = target;
        }
      }

      if (actionInfo != null && actionGroupInfo != null) {
        targets.remove(actionInfo);
      }
    }

    if (targets.isEmpty() || targets.size() != 1) {
      return null;
    }
    return targets.iterator().next();
  }

  private static <T extends Annotation> void findAnnotationInSuper(TypeElement typeElement, Class<T> annotationClass, Set<TypeElement> processed, Set<AnnotationResolveInfo> targets) {
    if (!processed.add(typeElement)) {
      return;
    }

    T annotation = typeElement.getAnnotation(annotationClass);
    if (annotation != null) {
      targets.add(new AnnotationResolveInfo(annotation, typeElement));
    }

    TypeMirror superclass = typeElement.getSuperclass();
    if (superclass != null) {
      if (superclass instanceof DeclaredType) {
        findAnnotationInSuper((TypeElement)((DeclaredType)superclass).asElement(), annotationClass, processed, targets);
      }
    }

    for (TypeMirror typeMirror : typeElement.getInterfaces()) {
      if (typeMirror instanceof DeclaredType) {
        findAnnotationInSuper((TypeElement)((DeclaredType)typeMirror).asElement(), annotationClass, processed, targets);
      }
    }
  }
}
