package com.example.administrator.lib.Util;

import com.example.admistrator.Extra;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.List;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * Created by janecer on 2018/4/2 0002.
 * email:janecer@sina.cn
 */
public class BundleSaveBuilder {
    private Elements elementsUtil ;
    private Types typeUtil ;
    private Messager log;

    public BundleSaveBuilder(Elements elementsUtil, Types typeUtil,Messager log) {
        this.elementsUtil = elementsUtil;
        this.typeUtil = typeUtil;
        this.log = log;
    }


    public void buildSaveTransfDataStatement(Element element, MethodSpec.Builder builder) {
        TypeMirror typeMirror = element.asType();
        int type = typeMirror.getKind().ordinal();
        //属性名 String text 获得text
        String fieldName = element.getSimpleName().toString();

        //获得注解 name值
        String extraName = element.getAnnotation(Extra.class).name();
        log.printMessage(Diagnostic.Kind.NOTE,"filed:" + fieldName +" extraName:" + extraName);
        extraName = Utils.isEmpty(extraName) ? fieldName : extraName;
        String extvalue = "t." + fieldName;
        String statement = "bundle.";
        if (type == TypeKind.BOOLEAN.ordinal()) {
            statement += "putBoolean($S, " + extvalue + ")";
        } else if (type == TypeKind.BYTE.ordinal()) {
            statement += "putByte($S, " + extvalue + ")";
        } else if (type == TypeKind.SHORT.ordinal()) {
            statement += "putShort($S, " + extvalue + ")";
        } else if (type == TypeKind.INT.ordinal()) {
            statement += "putInt($S, " + extvalue + ")";
        } else if (type == TypeKind.LONG.ordinal()) {
            statement += "putLong($S, " + extvalue + ")";
        } else if (type == TypeKind.CHAR.ordinal()) {
            statement += "putChar($S, " + extvalue + ")";
        } else if (type == TypeKind.FLOAT.ordinal()) {
            statement += "putFloat($S, " + extvalue + ")";
        } else if (type == TypeKind.DOUBLE.ordinal()) {
            statement += "putDouble($S, " + extvalue + ")";
        } else {
            //数组类型
            if (type == TypeKind.ARRAY.ordinal()) {
                addArrayStatement(statement, fieldName, extraName, typeMirror, element,builder);
            } else {
                //Object
                addObjectStatement(statement, fieldName, extraName, typeMirror, element,builder);
            }
            return;
        }
        log.printMessage(Diagnostic.Kind.NOTE,"extraName: " + extraName);
        builder.addStatement(statement, extraName);
    }

    /**
     * 添加数组
     *
     * @param statement
     * @param fieldName
     * @param typeMirror
     * @param element
     */
    private void addArrayStatement(String statement, String fieldName, String extraName, TypeMirror
            typeMirror, Element element, MethodSpec.Builder builder) {
        String extValue = "t." + fieldName ;
        TypeMirror parcelableType = elementsUtil.getTypeElement(Consts.PARCELABLE).asType() ;
        //数组
        switch (typeMirror.toString()) {
            case Consts.BOOLEANARRAY:
                statement += "putBooleanArray($S," + extValue +")";
                break;
            case Consts.INTARRAY:
                statement += "putIntArray($S," + extValue +")";
                break;
            case Consts.SHORTARRAY:
                statement += "putShortArray($S," + extValue +")";
                break;
            case Consts.FLOATARRAY:
                statement += "putFloatArray($S," + extValue +")";
                break;
            case Consts.DOUBLEARRAY:
                statement += "putDoubleArray($S," + extValue +")";
                break;
            case Consts.BYTEARRAY:
                statement += "putByteArray($S," + extValue +")";
                break;
            case Consts.CHARARRAY:
                statement += "putCharArray($S," + extValue +")";
                break;
            case Consts.LONGARRAY:
                statement += "putLongArray($S," + extValue +")";
                break;
            case Consts.STRINGARRAY:
                statement += "putStringArray($S," + extValue +")";
                break;
            default:
                //Parcelable 数组
                //object数组 componentType获得object类型
                ArrayTypeName arrayTypeName = (ArrayTypeName) ClassName.get(typeMirror);
                TypeElement typeElement = elementsUtil.getTypeElement(arrayTypeName
                        .componentType.toString());
                //是否为 Parcelable 类型
                if (!typeUtil.isSubtype(typeElement.asType(), parcelableType)) {
                    throw new RuntimeException("不支持参数类型 " + typeMirror + " " +element);
                }
                statement = "bundle.putParcelableArray($S," + extValue + ")";
                builder.addStatement(statement, extraName);
                return;
        }
        builder.addStatement(statement, extraName);
    }


    /**
     * 添加对象 String/List/Parcelable
     *
     * @param statement
     * @param extraName
     * @param typeMirror
     * @param element
     */
    private void addObjectStatement(String statement, String fieldName, String extraName,
                                    TypeMirror typeMirror,
                                    Element element,MethodSpec.Builder builder) {
        String extValue = "t." + fieldName ;
        //Parcelable
        TypeMirror parcelableType = elementsUtil.getTypeElement(Consts.PARCELABLE).asType() ;
        if (typeUtil.isSubtype(typeMirror, parcelableType)) {
            statement += "putParcelable($S,"+ extValue +")";
        } else if (typeMirror.toString().equals(Consts.STRING)) {
            statement += "putString($S,"+ extValue +")";
        } else {
            //List
            TypeName typeName = ClassName.get(typeMirror);
            //泛型
            if (typeName instanceof ParameterizedTypeName) {

                //list 或 arraylist
                ClassName rawType = ((ParameterizedTypeName) typeName).rawType;
                //泛型类型
                List<TypeName> typeArguments = ((ParameterizedTypeName) typeName)
                        .typeArguments;
                if (!rawType.toString().equals(Consts.ARRAYLIST) && !rawType.toString()
                        .equals(Consts.LIST)) {
                    throw new RuntimeException("Not Support Inject Type:" + typeMirror + " " +
                            element);
                }
                if (typeArguments.isEmpty() || typeArguments.size() != 1) {
                    throw new RuntimeException("List Must Specify Generic Type:" + typeArguments);
                }
                TypeName typeArgumentName = typeArguments.get(0);
                TypeElement typeElement = elementsUtil.getTypeElement(typeArgumentName
                        .toString());
                // Parcelable 类型
                if (typeUtil.isSubtype(typeElement.asType(), parcelableType)) {
                    statement += "putParcelableArrayList($S,($L)"+extValue+")";
                } else if (typeElement.asType().toString().equals(Consts.STRING)) {
                    statement += "putStringArrayList($S,($L)"+extValue+")";
                } else if (typeElement.asType().toString().equals(Consts.INTEGER)) {
                    statement += "putIntegerArrayList($S,($L)"+extValue+")";
                } else {
                    throw new RuntimeException("Not Support Generic Type : " + typeMirror + " " +
                            element);
                }
                builder.addStatement(statement, extraName, Consts.ARRAYLIST);
                return;
            } else {
                throw new RuntimeException("Not Support Extra Type : " + typeMirror + " " +
                        element);
            }
        }
        builder.addStatement(statement, extraName);
    }

}
