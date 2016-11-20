package com.blueapron.marinator.processor;

import com.blueapron.marinator.Injector;
import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

/**
 * Processor for the Injector annotation.
 */
public final class InjectorProcessor extends BaseProcessor {

    private static final String ROOT_PACKAGE = "com.blueapron.marinator";
    private static final String PACKAGE_NAME = ROOT_PACKAGE + ".generated";
    private static final String INSTANCE_NAME = "sInstance";

    private static final String INJECTORS_CLASS_NAME = "Marinator";
    private static final String GENERATED_CLASS_NAME = "MarinadeHelper";

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> set = new HashSet<>();
        set.add(Injector.class.getCanonicalName());
        return set;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> injectMethods = roundEnv.getElementsAnnotatedWith(Injector.class);
        if (injectMethods.size() == 0) {
            return true;
        }

        // First find all the methods that are registered as @Injector. This will allow us to map
        // which class injects which types.
        Map<TypeElement, List<TypeMirror>> injectorMap = new LinkedHashMap<>();
        for (ExecutableElement method : ElementFilter.methodsIn(injectMethods)) {
            List<? extends VariableElement> vars = method.getParameters();
            Preconditions.checkArgument(vars.size() == 1, "Injectors can only take in one object!");
            TypeElement varType = (TypeElement) mTypeUtils.asElement(vars.get(0).asType());

            // Insert this into the list for the parent type.
            TypeElement type = (TypeElement) method.getEnclosingElement();
            List<TypeMirror> injected = injectorMap.get(type);
            if (injected == null) {
                injected = new ArrayList<>();
                injectorMap.put(type, injected);
            }
            injected.add(varType.asType());
        }

        // Output the final class.
        JavaFile file = constructClass(injectorMap);
        writeFile(file, "Failed to generate " + GENERATED_CLASS_NAME);
        return true;
    }

    private JavaFile constructClass(Map<TypeElement, List<TypeMirror>> injectors) {
        ClassName outputType = ClassName.get(PACKAGE_NAME, GENERATED_CLASS_NAME);
        ClassName injectorsClass = ClassName.get(ROOT_PACKAGE, INJECTORS_CLASS_NAME);

        // Generate the instance field builder.
        FieldSpec instanceField = FieldSpec.builder(outputType, INSTANCE_NAME)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .build();

        // Generate the create method builder.
        List<FieldSpec> fields = new ArrayList<>(injectors.keySet().size());
        List<ParameterSpec> params = new ArrayList<>(injectors.keySet().size());
        MethodSpec.Builder createBuilder = MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        // Generate a private constructor builder.
        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE);

        // Generate the register method builder.
        MethodSpec.Builder registerBuilder = MethodSpec.methodBuilder("register")
                .addModifiers(Modifier.PRIVATE);

        // Generate the inject method builder.
        ParameterSpec objParam = ParameterSpec.builder(TypeName.OBJECT, "obj").build();
        MethodSpec.Builder injectBuilder = MethodSpec.methodBuilder("inject")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(objParam);

        boolean firstArg = true;
        for (TypeElement type : injectors.keySet()) {
            TypeName typeName = TypeName.get(type.asType());
            String rawName = type.getSimpleName().toString();
            String paramName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, rawName);

            // Create a field for the injector.
            FieldSpec field = FieldSpec.builder(typeName, "m" + rawName)
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .build();
            fields.add(field);

            // Assign the parameter to the field.
            ParameterSpec assign = ParameterSpec.builder(typeName, paramName).build();
            params.add(assign);
            createBuilder.addParameter(assign);
            constructorBuilder.addParameter(assign);
            constructorBuilder.addStatement("$1N = $2N", field, assign);

            // Now walk the relevant classes.
            for (TypeMirror mirror : injectors.get(type)) {
                TypeName mirrorName = TypeName.get(mirror);

                // Add the registration statement to the register method.
                registerBuilder.addStatement("$1T.registerInjector($2T.class, this)",
                        injectorsClass, mirrorName);

                // Now add the control statement to the inject method.
                if (firstArg) {
                    firstArg = false;
                    injectBuilder.beginControlFlow("if ($1N instanceof $2T)", objParam, mirrorName);
                } else {
                    injectBuilder.nextControlFlow("else if ($1N instanceof $2T)", objParam,
                            mirrorName);
                }
                injectBuilder.addStatement("$1N.inject(($2T) $3N)", field, mirrorName, objParam);
            }
        }

        // Finish generating the create method's call to the constructor. We need to know how
        // many params to pass in to the constructor.
        StringBuilder paramNames = new StringBuilder();
        for (ParameterSpec spec : params) {
            paramNames.append("$N,");
        }
        paramNames.deleteCharAt(paramNames.length() - 1);
        createBuilder.addStatement(String.format("%s = new %s(%s)", INSTANCE_NAME,
                GENERATED_CLASS_NAME, paramNames), params.toArray());

        // Finalize the methods.
        constructorBuilder.addStatement("register()");
        injectBuilder.nextControlFlow("else")
                .addStatement("$1T clazz = $2N.getClass()", TypeName.get(Class.class), objParam)
                .addStatement("throw new IllegalArgumentException($S + clazz.getSimpleName())",
                        "Cannot inject type ")
                .endControlFlow();

        // Generate the overall class and return the file.
        ClassName injectorInterface = ClassName.get(ROOT_PACKAGE, INJECTORS_CLASS_NAME, "Injector");
        TypeSpec helper = TypeSpec.classBuilder(outputType)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(injectorInterface)
                .addMethod(createBuilder.build())
                .addMethod(constructorBuilder.build())
                .addMethod(registerBuilder.build())
                .addMethod(injectBuilder.build())
                .addFields(fields)
                .addField(instanceField)
                .build();
        return JavaFile.builder(PACKAGE_NAME, helper).build();
    }
}
