package com.dongxm.handler;

import com.dongxm.annotation.ProxyGetMapping;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import com.sun.tools.javac.code.Symbol;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.dongxm.annotation.ProxyRestController"})
public class ProxyRestControllerProcessor extends AbstractProcessor {
    private Messager messager;
    private Filer filer;
    private Elements elementUtils;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        elementUtils = processingEnv.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement element : annotations) {
            Set<? extends Element> elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(element);
            for (Element element1 : elementsAnnotatedWith) {
                messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, element1.getSimpleName().toString() + " element.getKind(): " + element1.getKind(), element1);
                try {
                    addMethod(element1);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return true;
    }

    private void addMethod(Element element) throws ClassNotFoundException {
        String simpleName = element.getSimpleName().toString();
        String packageName = elementUtils.getPackageOf(element).getQualifiedName().toString();
        List<MethodSpec> methodSpecList = new ArrayList<>();
        List<? extends Element> enclosedElements = element.getEnclosedElements();
        ClassName objectMapper = ClassName.get("com.fasterxml.jackson.databind", "ObjectMapper");
        for (Element enclosedElement : enclosedElements) {
            ProxyGetMapping annotation = enclosedElement.getAnnotation(ProxyGetMapping.class);
            if (Objects.nonNull(annotation)) {
                Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) enclosedElement;
                messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, "enclosedElement.getEnclosingElement(): " + enclosedElement.getClass(), element);
                messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, "methodSymbol.getReturnType(): " + methodSymbol.getReturnType(), element);

                MethodSpec sampleMethod = MethodSpec.methodBuilder(methodSymbol.getSimpleName().toString()+"Proxy")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(AnnotationSpec.builder(GetMapping.class)
                                .addMember("value", "$S", (Object[]) annotation.value())
                                .build())
                        .addParameter(ParameterSpec.builder(ClassName.get("java.lang","String"),"name").build())
                        .addStatement("System.out.println($S)", "Hello, JavaPoet!12")
                        .addStatement("Object result = super."+methodSymbol.getSimpleName()+"(name)")
                        .addStatement("return new $T().writeValueAsString(result)",objectMapper)
                        .returns(String.class)
                        .addException(JsonProcessingException.class)
                        .build();
                methodSpecList.add(sampleMethod);
            }
        }

        // 创建类
        TypeSpec typeSpec = TypeSpec.classBuilder(simpleName + "Proxy")
                .superclass(ClassName.get(packageName, simpleName))
                .addAnnotation(RestController.class)
                .addModifiers(Modifier.PUBLIC) // 设置类修饰符为 public
                .addMethods(methodSpecList) // 添加刚创建的方法
                //.addField(FieldSpec.builder(ClassName.get(packageName,simpleName),"originObject").build())
                .build();
        // 生成Java文件
        JavaFile javaFile = JavaFile.builder(packageName, typeSpec)
                .build();
        // 输出生成的代码
        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}

