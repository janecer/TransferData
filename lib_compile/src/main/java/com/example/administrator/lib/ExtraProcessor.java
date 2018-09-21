package com.example.administrator.lib;

import com.example.administrator.lib.Util.BundleGetBuilder;
import com.example.administrator.lib.Util.BundleSaveBuilder;
import com.example.administrator.lib.Util.Consts;
import com.example.administrator.lib.Util.IntentGetBuilder;
import com.example.administrator.lib.Util.Utils;
import com.example.admistrator.Extra;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import static javax.lang.model.element.Modifier.PUBLIC;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes({"com.example.admistrator.Extra"})
public class ExtraProcessor extends AbstractProcessor {

    private Map<TypeElement,List<Element>> parentAndChilds = new HashMap<>();
    private Elements elementsUtil ;
    private Types typeUtil ;
    private Filer filer ;
    private Messager log;

    BundleGetBuilder mBundleGetBuilder ;
    BundleSaveBuilder mBundleSaveBuilder ;
    IntentGetBuilder mIntentGetBuilder ;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        elementsUtil = processingEnvironment.getElementUtils() ;
        typeUtil = processingEnvironment.getTypeUtils() ;
        filer = processingEnvironment.getFiler() ;
        log = processingEnvironment.getMessager() ;
        mBundleGetBuilder = new BundleGetBuilder(elementsUtil,typeUtil,log) ;
        mBundleSaveBuilder = new BundleSaveBuilder(elementsUtil,typeUtil,log) ;
        mIntentGetBuilder = new IntentGetBuilder(elementsUtil,typeUtil,log) ;
        log.printMessage(Diagnostic.Kind.NOTE,"__init");
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        log.printMessage(Diagnostic.Kind.NOTE,"__process");
        if(null == set || set.isEmpty()){
            return false ;
        }
        Set<? extends Element> extraElements = roundEnvironment.getElementsAnnotatedWith(Extra.class) ;
        if(null == extraElements || extraElements.isEmpty()){
            return false ;
        }
        collecExtraInfo(extraElements);
        try {
            return makeJavaFile() ;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false ;
    }

    private boolean makeJavaFile() throws Exception {
        if(null == parentAndChilds || parentAndChilds.isEmpty()){
            return false;
        }
        TypeMirror typeActivity = elementsUtil.getTypeElement("android.app.Activity").asType() ;
        TypeMirror typeFragment = elementsUtil.getTypeElement("android.app.Fragment").asType() ;
        TypeMirror typeV4Fragment = elementsUtil.getTypeElement("android.support.v4.app.Fragment").asType() ;


        TypeElement typeExtra = elementsUtil.getTypeElement("com.example.lib_core.IExtra") ;
        ParameterSpec objectParamSpec = ParameterSpec.builder(TypeName.OBJECT, "target").build();
        ParameterSpec bundleParamSpec = ParameterSpec.builder(ClassName.get("android.os","Bundle"),"bundle").build();

        for (Map.Entry<TypeElement,List<Element>> entrys : parentAndChilds.entrySet()){
            TypeElement typeElement = entrys.getKey() ;


            List<Element> elements = parentAndChilds.get(typeElement) ;

            if(elements != null && !elements.isEmpty()){

                //添加获取bundle数据
                MethodSpec.Builder loadExtra = MethodSpec.methodBuilder(Consts
                        .METHOD_LOAD_EXTRA)
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(objectParamSpec)
                        .addParameter(bundleParamSpec);

                loadExtra.addStatement("$L t = ($L)target",typeElement.getSimpleName(),typeElement.getSimpleName()) ;

                if(typeUtil.isSubtype(typeElement.asType(),typeActivity)){
                    //构建activity的获取extra代码
                    makeActivityMethod(elements, loadExtra);
                } else if(typeUtil.isSubtype(typeElement.asType(),typeFragment) || typeUtil.isSubtype(typeElement.asType(),typeV4Fragment)) {
                    //构建fragment的获取extra代码
                    loadExtra.addStatement("bundle = bundle == null ? t.getArguments() : bundle") ;
                    for (Element element : elements){
                        mBundleGetBuilder.buildGetTransfDataStatement(element,loadExtra);
                    }
                } else {
                    throw new RuntimeException("only support type {android.app.Activity,android.app.Fragment}" + typeElement);
                }
                //添加保存bundle方法
                MethodSpec.Builder saveExtra = MethodSpec.methodBuilder(Consts
                        .METHOD_SAVE_EXTRA)
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(objectParamSpec)
                        .addParameter(bundleParamSpec);

                saveExtra.addStatement("$L t = ($L)target",typeElement.getSimpleName(),typeElement.getSimpleName()) ;
                for (Element element : elements){
                    mBundleSaveBuilder.buildSaveTransfDataStatement(element,saveExtra);
                }

                // 生成java类名
                String extraClassName = typeElement.getSimpleName() + Consts.NAME_OF_EXTRA;
                // 生成 XX$$Autowired
                JavaFile.builder( ClassName.get(typeElement).packageName(), TypeSpec.classBuilder(extraClassName)
                        .addSuperinterface(ClassName.get(typeExtra))
                        .addModifiers(PUBLIC)
                        .addMethod(loadExtra.build()).addMethod(saveExtra.build()).build())
                        .build().writeTo(filer);
            }
        }
        return true ;
    }

    private void makeActivityMethod( List<Element> elements, MethodSpec.Builder loadExtra) {

        loadExtra.beginControlFlow("if(bundle == null)") ;
        for (Element element : elements){
            mIntentGetBuilder.buildGetTransfDataStatement(element,loadExtra);
        }
        loadExtra.nextControlFlow("else") ;
        for (Element element : elements){
            mBundleGetBuilder.buildGetTransfDataStatement(element,loadExtra);
        }
        loadExtra.endControlFlow() ;
    }

    /**
     * 将该类中相关有Extra的属性存储到集合中，集中key为相关类
     * @param elements
     */
    private void collecExtraInfo(Set<? extends Element> elements) {
        for(Element element : elements) {
            TypeElement parentElement = (TypeElement) element.getEnclosingElement();
            if(parentAndChilds.containsKey(parentElement)){
                parentAndChilds.get(parentElement).add(element) ;
            } else {
                ArrayList<Element> elementChilds = new ArrayList<>() ;
                elementChilds.add(element) ;
                parentAndChilds.put(parentElement,elementChilds) ;
            }
        }
    }



}
