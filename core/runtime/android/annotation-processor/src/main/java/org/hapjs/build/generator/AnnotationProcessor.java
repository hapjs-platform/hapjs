/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.build.generator;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.tools.javac.code.Type;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.DependencyAnnotation;
import org.hapjs.bridge.annotation.EventTargetAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.bridge.annotation.InheritedAnnotation;
import org.hapjs.bridge.annotation.ModuleExtensionAnnotation;
import org.hapjs.bridge.annotation.TypeAnnotation;
import org.hapjs.bridge.annotation.WebFeatureExtensionAnnotation;
import org.hapjs.bridge.annotation.WebInheritedAnnotation;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.bridge.annotation.WidgetExtensionAnnotation;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AnnotationProcessor extends AbstractProcessor {
    private static final String FeatureExtensionAnnotationClassname
            = FeatureExtensionAnnotation.class.getName();
    private static final String ModuleExtensionAnnotationClassname
            = ModuleExtensionAnnotation.class.getName();
    private static final String WidgetExtensionAnnotationClassname
            = WidgetExtensionAnnotation.class.getName();
    private static final String WidgetAnnotationClassname
            = WidgetAnnotation.class.getName();
    private static final String InheritedAnnotationClassname
            = InheritedAnnotation.class.getName();
    private static final String EventTargetAnnotationClassname
            = EventTargetAnnotation.class.getName();
    private static final String DependencyAnnotationClassname
            = DependencyAnnotation.class.getName();
    private static final String WebFeatureExtensionAnnotationClassname
            = WebFeatureExtensionAnnotation.class.getName();
    private static final String WebInheritedAnnotationClassname
            = WebInheritedAnnotation.class.getName();
    private String mOutputDir;
    private MetaData<Extension> mFeatureExtensions;
    private MetaData<Extension> mModuleExtensions;
    private MetaData<Extension> mWidgetExtensions;
    private MetaData<Widget> mWidgets;
    private MetaData<Inherited> mInheriteds;
    private MetaData<EventTarget> mEventTargets;
    private MetaData<Dependency> mDependencies;
    private Map<String, Class<? extends Annotation>> mAnnotationClasses;
    private MetaData<Extension> mWebFeatureExtensions;
    private MetaData<Inherited> mWebInheriteds;

    public AnnotationProcessor() {
        mFeatureExtensions = new MetaData<>();
        mModuleExtensions = new MetaData<>();
        mWidgetExtensions = new MetaData<>();
        mWidgets = new MetaData<>();
        mInheriteds = new MetaData<>();
        mEventTargets = new MetaData<>();
        mAnnotationClasses = new HashMap<>();
        mWebFeatureExtensions = new MetaData<>();
        mWebInheriteds = new MetaData<>();
        mDependencies = new MetaData<>();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        Map<String, String> options = processingEnvironment.getOptions();
        mOutputDir = options.get("outputDir");
        if (mOutputDir == null) {
            throw new IllegalArgumentException("No outputDir option");
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> set = new HashSet<>();
        set.add(FeatureExtensionAnnotationClassname);
        set.add(WebFeatureExtensionAnnotationClassname);
        set.add(WebInheritedAnnotationClassname);
        set.add(ModuleExtensionAnnotationClassname);
        set.add(WidgetExtensionAnnotationClassname);
        set.add(WidgetAnnotationClassname);
        set.add(InheritedAnnotationClassname);
        set.add(EventTargetAnnotationClassname);
        set.add(DependencyAnnotationClassname);
        return set;
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        for (TypeElement typeElement : set) {
            for (Element annotatedElement : roundEnvironment
                    .getElementsAnnotatedWith(typeElement)) {
                processAnnotation(typeElement, (TypeElement) annotatedElement);
            }
        }

        saveMetadata(mFeatureExtensions, "feature_extension.json");
        saveMetadata(mModuleExtensions, "module_extension.json");
        saveMetadata(mWidgetExtensions, "widget_extension.json");
        saveMetadata(mWidgets, "widget.json");
        saveMetadata(mInheriteds, "inherited.json");
        saveMetadata(mEventTargets, "event_target.json");
        saveMetadata(mWebInheriteds, "web_inherited.json");
        saveMetadata(mWebFeatureExtensions, "web_feature_extension.json");
        saveMetadata(mDependencies, "dependency.json");

        return true;
    }

    private void processAnnotation(
            TypeElement annotationTypeElement,
            TypeElement annotatedTypeElement) {
        String qualifiedName = annotationTypeElement.getQualifiedName().toString();
        if (FeatureExtensionAnnotationClassname.equals(qualifiedName)) {
            processExtensionAnnotation(
                    annotationTypeElement,
                    annotatedTypeElement,
                    mFeatureExtensions);
        } else if (ModuleExtensionAnnotationClassname.equals(qualifiedName)) {
            processExtensionAnnotation(
                    annotationTypeElement,
                    annotatedTypeElement,
                    mModuleExtensions);
        } else if (WidgetExtensionAnnotationClassname.equals(qualifiedName)) {
            processExtensionAnnotation(
                    annotationTypeElement,
                    annotatedTypeElement,
                    mWidgetExtensions);
        } else if (WidgetAnnotationClassname.equals(qualifiedName)) {
            processWidgetAnnotation(annotatedTypeElement);
        } else if (InheritedAnnotationClassname.equals(qualifiedName)) {
            processInheritedAnnotation(annotatedTypeElement, false);
        } else if (EventTargetAnnotationClassname.equals(qualifiedName)) {
            processEventTargetAnnotation(annotatedTypeElement);
        } else if (WebInheritedAnnotationClassname.equals(qualifiedName)) {
            processInheritedAnnotation(annotatedTypeElement, true);
        } else if (WebFeatureExtensionAnnotationClassname.equals(qualifiedName)) {
            processExtensionAnnotation(
                    annotationTypeElement,
                    annotatedTypeElement,
                    mWebFeatureExtensions);
        } else if (DependencyAnnotationClassname.equals(qualifiedName)) {
            processDependencyAnnotation(annotatedTypeElement);
        } else {
            throw new IllegalStateException("Unknown annotation: " + qualifiedName);
        }
    }

    private void processExtensionAnnotation(
            TypeElement annotationTypeElement,
            TypeElement annotatedTypeElement,
            MetaData<Extension> metadata) {
        Class<? extends Annotation> annotationClass = getAnnotationClass(annotationTypeElement);
        Annotation annotation = annotatedTypeElement.getAnnotation(annotationClass);
        Extension extension = parseExtension(annotatedTypeElement, annotation);
        metadata.addElement(extension);
        System.out.println("Found extension: " + extension.getName());
    }

    private Extension parseExtension(TypeElement annotatedTypeElement, Annotation annotation) {
        try {
            java.lang.reflect.Method nameField = annotation.getClass().getMethod("name");
            java.lang.reflect.Method actionsField = annotation.getClass().getMethod("actions");
            String name = (String) nameField.invoke(annotation);
            ActionAnnotation[] actions = (ActionAnnotation[]) actionsField.invoke(annotation);

            Extension extension = new Extension();
            extension.setName(name);
            extension.setClassname(annotatedTypeElement.getQualifiedName().toString());
            extension.setSuperClasses(processSuperClasses(annotatedTypeElement.getSuperclass()));

            try {
                java.lang.reflect.Method residentTypeField =
                        annotation.getClass().getMethod("residentType");
                if (null != residentTypeField) {
                    FeatureExtensionAnnotation.ResidentType residentType =
                            (FeatureExtensionAnnotation.ResidentType) residentTypeField
                                    .invoke(annotation);
                    extension.setResidentType(residentType.name());
                }
            } catch (NoSuchMethodException e) {
                System.out.println("parse extension error:" + e.getMessage());
            }

            for (ActionAnnotation action : actions) {
                Method method = new Method();
                method.setName(action.name());
                method.setInstanceMethod(action.instanceMethod());
                method.setMode(action.mode());
                method.setType(action.type());
                method.setAccess(action.access());
                method.setNormalize(action.normalize());
                method.setMultiple(action.multiple());
                method.setAlias(action.alias());
                method.setPermissions(action.permissions());
                method.setSubAttrs(action.subAttrs());
                method.setResidentType(action.residentType());
                extension.addMethod(method);
            }
            return extension;
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException("Fail to parse extension", e);
        }
    }

    private void processWidgetAnnotation(TypeElement annotatedTypeElement) {
        WidgetAnnotation annotation = annotatedTypeElement.getAnnotation(WidgetAnnotation.class);
        String name = annotation.name();
        String classname = annotatedTypeElement.getQualifiedName().toString();
        Widget widget = new Widget();
        widget.setName(name);
        widget.setClassname(classname);
        widget.setNeedDeleteSuperClasses(annotation.needDeleteSuperClasses());
        widget.setSuperClasses(processSuperClasses(annotatedTypeElement.getSuperclass()));
        for (TypeAnnotation typeAnnotation : annotation.types()) {
            org.hapjs.build.generator.Type type = new org.hapjs.build.generator.Type();
            type.setName(typeAnnotation.name());
            type.setDefault(typeAnnotation.isDefault());
            widget.addType(type);
        }
        for (String method : annotation.methods()) {
            widget.addMethod(method);
        }
        mWidgets.addElement(widget);
        System.out.println("Found widget: " + widget.getName());
    }

    private void processInheritedAnnotation(TypeElement annotatedTypeElement,
                                            boolean isWebAnnotation) {
        String classname = annotatedTypeElement.getQualifiedName().toString();
        Inherited inherited = new Inherited();
        inherited.setClassname(classname);
        inherited.setSuperClasses(processSuperClasses(annotatedTypeElement.getSuperclass()));
        if (isWebAnnotation) {
            mWebInheriteds.addElement(inherited);
        } else {
            mInheriteds.addElement(inherited);
        }
        System.out.println("Found inherited: " + inherited.getClassname());
    }

    private void processEventTargetAnnotation(TypeElement annotatedTypeElement) {
        EventTargetAnnotation annotation =
                annotatedTypeElement.getAnnotation(EventTargetAnnotation.class);
        String classname = annotatedTypeElement.getQualifiedName().toString();
        String[] eventNames = annotation.eventNames();
        EventTarget eventTarget = new EventTarget();
        eventTarget.setEventNames(eventNames);
        eventTarget.setClassname(classname);
        eventTarget.setSuperClasses(processSuperClasses(annotatedTypeElement.getSuperclass()));
        mEventTargets.addElement(eventTarget);
        System.out.println("Found event target: " + eventTarget.getClassname());
    }

    private void processDependencyAnnotation(TypeElement annotatedTypeElement) {
        DependencyAnnotation annotation =
                annotatedTypeElement.getAnnotation(DependencyAnnotation.class);
        String classname = annotatedTypeElement.getQualifiedName().toString();
        String key = annotation.key();

        Dependency dependency = new Dependency();
        dependency.setKey(key);
        dependency.setClassname(classname);
        dependency.setSuperClasses(processSuperClasses(annotatedTypeElement.getSuperclass()));

        mDependencies.addElement(dependency);
        System.out.println("Found dependency: " + dependency.getClassname());
    }

    private List<String> processSuperClasses(TypeMirror superClassType) {
        List<String> superClassesName = new ArrayList<>();
        while (superClassType instanceof Type.ClassType) {
            Type.ClassType classType = (Type.ClassType) superClassType;
            superClassesName.add(classType.tsym.getQualifiedName().toString());
            superClassType = classType.supertype_field;
            if (superClassType == null && classType.tsym.type instanceof Type.ClassType) {
                classType = (Type.ClassType) classType.tsym.type;
                superClassType = classType.supertype_field;
            }
        }
        return superClassesName;
    }

    private void saveMetadata(MetaData metadata, String filename) {
        if (!metadata.isEmpty()) {
            saveToFile(metadata, filename);
        }
    }

    private void saveToFile(Object obj, String filename) {
        File metadataFile = new File(mOutputDir, filename);
        File parentFile = metadataFile.getParentFile();
        if (parentFile != null) {
            parentFile.mkdirs();
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY);
        mapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY);
        try {
            mapper.writeValue(metadataFile, obj);
        } catch (IOException e) {
            throw new IllegalStateException("Fail to write json", e);
        }
    }

    private Class<? extends Annotation> getAnnotationClass(TypeElement annotationTypeElement) {
        String annotationClassname = annotationTypeElement.getQualifiedName().toString();
        return getAnnotationClass(annotationClassname);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Annotation> getAnnotationClass(String annotationClassname) {
        Class<? extends Annotation> klass = mAnnotationClasses.get(annotationClassname);
        if (klass == null) {
            try {
                klass = (Class<? extends Annotation>) Class.forName(annotationClassname);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Unknown annotation: " + annotationClassname, e);
            }
            mAnnotationClasses.put(annotationClassname, klass);
        }
        return klass;
    }
}
