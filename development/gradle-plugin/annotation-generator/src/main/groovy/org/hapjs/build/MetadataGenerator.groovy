/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.build

import com.sun.codemodel.*
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.hapjs.build.generator.Dependency
import org.hapjs.build.generator.EventTarget
import org.hapjs.build.generator.Extension
import org.hapjs.build.generator.ExtensionMetaData
import org.hapjs.build.generator.MetaData
import org.hapjs.build.generator.Method
import org.hapjs.build.generator.Type
import org.hapjs.build.generator.Widget

import static com.sun.codemodel.JMod.*

class MetadataGenerator {
    private final JCodeModel codeModel
    private final JDefinedClass metaDataSetClass
    private final JDefinedClass eventTargetMetaDataSetClass
    private final JDefinedClass dependencyManagerClass
    private final JClass extensionType
    private final JClass mapStringType
    private final JClass setStringType
    private final JClass eventTargetType
    private final JClass listEventTargetType
    private final JClass mapListEventTargetType
    private final JClass dependencyType
    private final JClass mapDependencyType
    private final JClass modeType
    private final JClass typeType
    private final JClass accessType
    private final JClass normalizeType
    private final JClass multipleType

    private final MetaData<Extension> featureExtensions
    private final MetaData<Extension> moduleExtensions
    private final MetaData<Extension> widgetExtensions
    private final MetaData<Widget> widgets
    private final MetaData<EventTarget> eventTargets
    private final MetaData<Dependency> dependencies
    private final File javaOutputDir
    private final File cardJsonFile

    private Object cardBlackListJSON

    MetadataGenerator(
            MetaData<Extension> featureExtensions,
            MetaData<Extension> moduleExtensions,
            MetaData<Extension> widgetExtensions,
            MetaData<Widget> widgets,
            MetaData<EventTarget> eventTargets,
            MetaData<Dependency> dependencies,
            File javaOutputDir, File cardJsonFile) {
        this.featureExtensions = featureExtensions;
        this.moduleExtensions = moduleExtensions
        this.widgetExtensions = widgetExtensions
        this.widgets = widgets
        this.eventTargets = eventTargets
        this.dependencies = dependencies
        this.javaOutputDir = javaOutputDir
        this.cardJsonFile = cardJsonFile

        codeModel = new JCodeModel()
        metaDataSetClass = codeModel._class(
                PUBLIC | FINAL,
                "org.hapjs.bridge.MetaDataSetImpl",
                ClassType.CLASS)
        metaDataSetClass._extends(codeModel.directClass('org.hapjs.bridge.MetaDataSet'))
        extensionType = codeModel.directClass("org.hapjs.bridge.ExtensionMetaData")
        mapStringType = codeModel.ref(Map.class).narrow(codeModel.ref(String.class), extensionType)
        setStringType = codeModel.ref(Set.class).narrow(codeModel.ref(String.class))
        modeType = codeModel.directClass("org.hapjs.bridge.Extension.Mode")
        typeType = codeModel.directClass("org.hapjs.bridge.Extension.Type")
        accessType = codeModel.directClass("org.hapjs.bridge.Extension.Access")
        normalizeType = codeModel.directClass("org.hapjs.bridge.Extension.Normalize")
        multipleType = codeModel.directClass("org.hapjs.bridge.Extension.Multiple")

        eventTargetMetaDataSetClass = codeModel._class(
                PUBLIC | FINAL,
                "org.hapjs.event.EventTargetDataSetImpl",
                ClassType.CLASS)
        eventTargetMetaDataSetClass._extends(codeModel.directClass('org.hapjs.event.EventTargetDataSet'))
        eventTargetType = codeModel.directClass("org.hapjs.event.EventTargetMetaData")
        listEventTargetType = codeModel.ref(List.class).narrow(eventTargetType)
        mapListEventTargetType = codeModel.ref(Map.class).narrow(codeModel.ref(String.class), listEventTargetType)

        dependencyManagerClass = codeModel._class(
                PUBLIC | FINAL,
                "org.hapjs.bridge.DependencyManagerImpl",
                ClassType.CLASS)
        dependencyManagerClass._extends(codeModel.directClass('org.hapjs.bridge.DependencyManager'))
        dependencyType = codeModel.directClass("org.hapjs.bridge.DependencyManager.Dependency")
        mapDependencyType = codeModel.ref(Map.class).narrow(codeModel.ref(String.class), dependencyType)
        initCardBlackList()
    }

    void generate() {
        generateExtensionMetaDataJSONStringMethod(featureExtensions, "getFeatureMetaDataJSONString")
        generateExtensionMetaData(
                featureExtensions,
                'FEATURE_META_DATA_MAP',
                'initFeatureMetaData',
                'getFeatureMetaData',
                'getFeatureMetaDataMap'
        )
        generateResidentSet(featureExtensions)
        generateExtensionMetaDataJSONStringMethod(moduleExtensions, "getModuleMetaDataJSONString")
        generateExtensionMetaData(
                moduleExtensions,
                'MODULE_META_DATA_MAP',
                'initModuleMetaData',
                'getModuleMetaData',
                'getModuleMetaDataMap'
        )
        generateExtensionMetaDataJSONStringMethod(widgetExtensions, "getWidgetMetaDataJSONString")
        generateExtensionMetaData(
                widgetExtensions,
                'WIDGET_META_DATA_MAP',
                'initWidgetMetaData',
                'getWidgetMetaData',
                'getWidgetMetaDataMap'
        )
        generateWidgetMetaDataJSONString()
        generateWidgetMetaData()
        generateEventTargets(
                eventTargets,
                'EVENT_TARGET_META_DATA_MAP',
                'initEventTargetMetaData',
                'getEventTargetMetaDataList',
                'getEventTargetMetaDataMap'
        )
        generateDependencies(
                dependencies,
                'DEPENDENCY_META_DATA_MAP',
                'initDependencyMetaData',
                'getDependency'
        )

        if (!javaOutputDir.isDirectory() && !javaOutputDir.mkdirs()) {
            throw new IOException('Could not create directory: ' + javaOutputDir)
        }
        codeModel.build(javaOutputDir)
    }

    private void generateExtensionMetaData(
            MetaData<Extension> metaData,
            String metaDataFieldName,
            String initMetaDataMethodName,
            String getMetaDataMethodName,
            String getMetaDataMapMethodName) {
        def initMetaDataMapMethod = metaDataSetClass.method(
                PRIVATE | STATIC, mapStringType, initMetaDataMethodName)
        def initBlock = initMetaDataMapMethod.body()
        def mapVar = initBlock.decl(mapStringType, "map")
        mapVar.init(JExpr._new(codeModel.ref(HashMap.class)))
        def extensionVar = initBlock.decl(extensionType, "extension")
        def extensionInited = false
        for (Extension extension : metaData.elements) {
            if (extensionInited) {
                initBlock.assign(extensionVar, JExpr._new(extensionType).arg(extension.name).arg(extension.classname))
            } else {
                extensionVar.init(JExpr._new(extensionType).arg(extension.name).arg(extension.classname))
                extensionInited = true
            }
            for (Method method : extension.methods) {
                JInvocation methodInvocation = extensionVar
                        .invoke("addMethod")
                        .arg(method.name)
                if (method.instanceMethod != null) {
                    methodInvocation.arg(JExpr.lit(method.instanceMethod))
                } else {
                    methodInvocation.arg(JExpr.FALSE)
                }
                methodInvocation.arg(modeType.staticRef(method.mode.name()))
                if (method.type != null) {
                    methodInvocation.arg(typeType.staticRef(method.type.name()))
                } else {
                    methodInvocation.arg(JExpr._null())
                }
                if (method.access != null) {
                    methodInvocation.arg(accessType.staticRef(method.access.name()))
                } else {
                    methodInvocation.arg(JExpr._null())
                }
                if (method.normalize != null) {
                    methodInvocation.arg(normalizeType.staticRef(method.normalize.name()))
                } else {
                    methodInvocation.arg(JExpr._null())
                }
                if (method.multiple != null) {
                    methodInvocation.arg(multipleType.staticRef(method.multiple.name()))
                } else {
                    methodInvocation.arg(JExpr._null())
                }
                if (method.alias != null) {
                    methodInvocation.arg(method.alias)
                } else {
                    methodInvocation.arg(JExpr._null())
                }
                if (method.permissions != null && method.permissions.length > 0) {
                    JArray permissionsArray = JExpr.newArray(codeModel.ref(String.class))
                    for (String perm : method.permissions) {
                        permissionsArray.add(JExpr.lit(perm))
                    }
                    methodInvocation.arg(permissionsArray)
                } else {
                    methodInvocation.arg(JExpr._null())
                }
                if (method.subAttrs != null && method.subAttrs.length > 0) {
                    JArray subAttrsArray = JExpr.newArray(codeModel.ref(String.class))
                    for (String subAttr : method.subAttrs) {
                        subAttrsArray.add(JExpr.lit(subAttr))
                    }
                    methodInvocation.arg(subAttrsArray)
                } else {
                    methodInvocation.arg(JExpr._null())
                }
                initBlock.add(methodInvocation)
            }
            JInvocation methodInvocation = extensionVar.invoke("validate")
            initBlock.add(methodInvocation)
            initBlock.add(mapVar.invoke("put").arg(extension.name).arg(extensionVar))
        }
        def constant = codeModel
                .directClass(Collections.class.getName())
                .staticInvoke('unmodifiableMap')
                .arg(mapVar)
        initBlock._return(constant)

        def metaDataField = metaDataSetClass.field(PRIVATE | STATIC, mapStringType, metaDataFieldName)
        def getMetaDataMapMethod = metaDataSetClass.method(
                PUBLIC, mapStringType, getMetaDataMapMethodName)
        getMetaDataMapMethod.annotate(codeModel.ref(Override.class))
        getMetaDataMapMethod.body()._if(metaDataField.eq(JExpr._null()))._then().add(metaDataField.assign(JExpr.invoke(initMetaDataMapMethod)))
        getMetaDataMapMethod.body()._return(metaDataField)

        def getMetaDataMethod = metaDataSetClass.method(
                PUBLIC, extensionType, getMetaDataMethodName)
        getMetaDataMethod.annotate(codeModel.ref(Override.class))
        def nameParam = getMetaDataMethod.param(String.class, "name")
        getMetaDataMethod.body()._return(JExpr.invoke(getMetaDataMapMethod).invoke("get").arg(nameParam))
    }

    private void generateExtensionMetaDataJSONStringMethod(MetaData<Extension> metaData, String methodName) {
        def stringClass = codeModel.ref(String.class)
        def getExtensionMetaDataJSONStringMethod = metaDataSetClass.method(
                PUBLIC, stringClass, methodName)
        getExtensionMetaDataJSONStringMethod.annotate(codeModel.ref(Override.class))
        def isCardMode = getExtensionMetaDataJSONStringMethod.param(codeModel.parseType("boolean"), "isCardMode")
        def block = getExtensionMetaDataJSONStringMethod.body()
        def array = new ArrayList(metaData.elements.size())
        for (Extension extension : metaData.elements) {
            def instance = new ExtensionMetaData(extension.name, extension.classname)
            for (Method method : extension.methods) {
                def instanceMethod = method.instanceMethod != null ? method.instanceMethod : false
                def mode = method.mode.name()
                def type = method.type != null ? method.type.name() : null
                def access = method.access != null ? method.access.name() : null
                def normalize = method.normalize != null ? method.normalize.name() : null
                def multiple = method.multiple != null ? method.multiple.name() : null
                def alias = method.alias != null ? method.alias : null
                def permissionsArray = method.permissions != null && method.permissions.length > 0 ? method.permissions : null
                def subAttrsArray = method.subAttrs != null && method.subAttrs.length > 0 ? method.subAttrs : null
                instance.addMethod(method.name, instanceMethod, mode, type, access, normalize, multiple, alias, permissionsArray, subAttrsArray)
            }
            instance.validate()
            array.add(instance.toJSON())
        }

        // 先生成未过滤卡片黑名单的JSONString
        def json = JsonOutput.toJson(array)
        def str = codeModel.directClass("java.lang.String").staticInvoke('valueOf').arg(json)

        // 过滤卡片黑名单
        if (cardBlackListJSON != null) {
            def blacklist = null
            if (metaData == featureExtensions) {
                blacklist = cardBlackListJSON["featureBlacklist"]
            } else if (metaData == widgetExtensions) {
                blacklist = cardBlackListJSON["componentBlacklist"]
            }

            blacklist.each { blackItem ->
                def temp = null;
                array.each { item ->
                    if (item.name == blackItem["name"]) {
                        temp = item
                    }
                }
                if (temp != null) {
                    if (blackItem["methods"] != null && !blackItem["methods"].isEmpty()) {
                        def waitingForRemovingMethods = new ArrayList();
                        temp.methods.each { method ->
                            if (blackItem["methods"].contains(method.name)) {
                                waitingForRemovingMethods.add(method)
                            }
                        }
                        temp.methods.removeAll(waitingForRemovingMethods)
                    } else {
                        array.remove(temp)
                    }
                    temp = null
                }
            }
        }

        // 生成过滤卡片黑名单后的JSONString
        def cardJson = JsonOutput.toJson(array)
        def cardStr = codeModel.directClass("java.lang.String").staticInvoke('valueOf').arg(cardJson)

        def result = block.decl(codeModel.ref(String.class), "result")
        def ifIsCardMode = block._if(isCardMode)
        ifIsCardMode._then().block().add(result.assign(cardStr))
        ifIsCardMode._else().block().add(result.assign(str))
        block._return(result)
    }

    private void generateWidgetMetaData() {
        def widgetType = codeModel.directClass("org.hapjs.bridge.Widget")
        def widgetListType = codeModel.ref(List.class).narrow(widgetType)

        def initWidgetListMethod = metaDataSetClass.method(
                PRIVATE | STATIC, widgetListType, 'initWidgetList')
        def initBlock = initWidgetListMethod.body()
        def listVar = initBlock.decl(widgetListType, "list")
        listVar.init(JExpr._new(codeModel.ref(ArrayList.class)))
        def widgetVar = initBlock.decl(widgetType, "widget")
        def widgetInited = false
        for (Widget widget : widgets.elements) {
            def componentClass = codeModel.directClass(widget.classname)
            if (widgetInited) {
                initBlock.assign(widgetVar, JExpr._new(widgetType).arg(widget.name).arg(componentClass.dotclass()))
            } else {
                widgetVar.init(JExpr._new(widgetType).arg(widget.name).arg(componentClass.dotclass()))
                widgetInited = true
            }
            for (Type type : widget.types) {
                JInvocation typeInvocation = widgetVar
                        .invoke("addType")
                        .arg(type.name)
                        .arg(String.valueOf(type.isDefault))
                initBlock.add(typeInvocation)
            }
            for (String method : widget.methods) {
                JInvocation methodInvocation = widgetVar
                        .invoke("addMethod")
                        .arg(method)
                initBlock.add(methodInvocation)
            }
            initBlock.add(listVar.invoke("add").arg(widgetVar))
        }
        def constant = codeModel
                .directClass(Collections.class.getName())
                .staticInvoke('unmodifiableList')
                .arg(listVar)
        initBlock._return(constant)

        def widgetListField = metaDataSetClass.field(
                PRIVATE | STATIC, widgetListType, 'WIDGET_LIST')
        def getWidgetListMethod = metaDataSetClass.method(
                PUBLIC, widgetListType, "getWidgetList")
        getWidgetListMethod.annotate(codeModel.ref(Override.class))
        getWidgetListMethod.body()._if(widgetListField.eq(JExpr._null()))._then().add(widgetListField.assign(JExpr.invoke(initWidgetListMethod)))
        getWidgetListMethod.body()._return(widgetListField)
    }

    private void generateWidgetMetaDataJSONString() {
        def array = new ArrayList(widgets.elements.size())
        widgets.elements.each { element ->
            def widget = new Widget(element.name, element.classname)
            element.types.each { type ->
                widget.addType(new Type(type.name, type.isDefault()))
            }
            element.methods.each { method ->
                widget.addMethod(method)
            }
            array.add(widget.toJSON())
        }

        // 先生成未过滤卡片黑名单的JSONString
        def json = JsonOutput.toJson(array)
        def str = codeModel.directClass("java.lang.String").staticInvoke('valueOf').arg(json)

        // 过滤卡片黑名单
        if (cardBlackListJSON != null) {
            def blacklist = cardBlackListJSON["componentBlacklist"]
            blacklist.each { blackItem ->
                def temp = null;
                array.each { item ->
                    if (item.name == blackItem["name"]) {
                        temp = item
                    }
                }
                if (temp != null) {
                    if (blackItem["methods"] != null && !blackItem["methods"].isEmpty()) {
                        def waitingForRemovingMethods = new ArrayList();
                        temp.methods.each { method ->
                            if (blackItem["methods"].contains(method.name)) {
                                waitingForRemovingMethods.add(method)
                            }
                        }
                        temp.methods.removeAll(waitingForRemovingMethods)
                    } else {
                        array.remove(temp)
                    }
                    temp = null
                }
            }
        }

        def getWidgetListJSONStringMethod = metaDataSetClass.method(
                PUBLIC, codeModel.ref(java.lang.String.class), "getWidgetListJSONString")
        getWidgetListJSONStringMethod.annotate(codeModel.ref(Override.class))
        def isCardMode = getWidgetListJSONStringMethod.param(codeModel.parseType("boolean"), "isCardMode")

        // 生成过滤卡片黑名单后的JSONString
        def cardJson = JsonOutput.toJson(array)
        def cardStr = codeModel.directClass("java.lang.String").staticInvoke('valueOf').arg(cardJson)

        def block = getWidgetListJSONStringMethod.body()
        def result = block.decl(codeModel.ref(String.class), "result")
        def ifIsCardMode = block._if(isCardMode)
        ifIsCardMode._then().block().add(result.assign(cardStr))
        ifIsCardMode._else().block().add(result.assign(str))
        block._return(result)
    }

    private void generateResidentSet(MetaData<Extension> metaData) {
        def initResidentWhiteSetMethod = metaDataSetClass.method(PRIVATE | STATIC, setStringType, 'initResidentWhiteSet')
        def initResidentWhiteSetBlock = initResidentWhiteSetMethod.body()
        def residentWhiteSetVar = initResidentWhiteSetBlock.decl(setStringType, "residentWhiteSet")
        residentWhiteSetVar.init(JExpr._new(codeModel.ref(HashSet.class)))

        def initResidentNormalSetMethod = metaDataSetClass.method(PRIVATE | STATIC, setStringType, 'initResidentNormalSet')
        def initResidentNormalSetBlock = initResidentNormalSetMethod.body()
        def residentNormalSetVar = initResidentNormalSetBlock.decl(setStringType, "residentNormalSet")
        residentNormalSetVar.init(JExpr._new(codeModel.ref(HashSet.class)))

        def initResidentImportantSetMethod = metaDataSetClass.method(PRIVATE | STATIC, setStringType, 'initResidentImportantSet')
        def initResidentImportantSetBlock = initResidentImportantSetMethod.body()
        def residentImportantSetVar = initResidentImportantSetBlock.decl(setStringType, "residentImportantSet")
        residentImportantSetVar.init(JExpr._new(codeModel.ref(HashSet.class)))

        def initMethodResidentWhiteSetMethod = metaDataSetClass.method(PRIVATE | STATIC, setStringType, 'initMethodResidentWhiteSet')
        def initMethodResidentWhiteSetBlock = initMethodResidentWhiteSetMethod.body()
        def methodResidentWhiteSetVar = initMethodResidentWhiteSetBlock.decl(setStringType, "methodResidentWhiteSet")
        methodResidentWhiteSetVar.init(JExpr._new(codeModel.ref(HashSet.class)))

        for (Extension extension : metaData.elements) {
            if (extension.getResidentType() == 'USEABLE') {
                initResidentWhiteSetBlock.add(residentWhiteSetVar.invoke("add").arg(extension.name))
            } else if (extension.residentType == "RESIDENT_NORMAL") {
                initResidentNormalSetBlock.add(residentNormalSetVar.invoke("add").arg(extension.name))
            } else if (extension.residentType == "RESIDENT_IMPORTANT") {
                initResidentImportantSetBlock.add(residentImportantSetVar.invoke("add").arg(extension.name))
            }
            for (Method method : extension.getMethods()) {
                if (method.getResidentType().name() != 'NONE') {
                    initMethodResidentWhiteSetBlock.add(methodResidentWhiteSetVar.invoke("add").arg(extension.name + '_' + method.getName()))
                }
            }
        }

        // Resident White
        def constantWhite = codeModel
                .directClass(Collections.class.getName())
                .staticInvoke('unmodifiableSet')
                .arg(residentWhiteSetVar)
        initResidentWhiteSetBlock._return(constantWhite)

        def residentWhiteSetField = metaDataSetClass.field(PRIVATE | STATIC, setStringType, 'RESIDENT_WHITE_SET')
        def isInResidentWhiteSetMethod = metaDataSetClass.method(PUBLIC, boolean, 'isInResidentWhiteSet')
        def isInResidentWhiteSetParam = isInResidentWhiteSetMethod.param(String.class, "name")
        isInResidentWhiteSetMethod.body()._if(residentWhiteSetField.eq(JExpr._null()))._then().add(residentWhiteSetField.assign(JExpr.invoke(initResidentWhiteSetMethod)))
        isInResidentWhiteSetMethod.body()._return(residentWhiteSetField.invoke("contains").arg(isInResidentWhiteSetParam))

        // Resident Normal
        def constantNormal = codeModel
                .directClass(Collections.class.getName())
                .staticInvoke('unmodifiableSet')
                .arg(residentNormalSetVar)
        initResidentNormalSetBlock._return(constantNormal)

        def residentNormalSetField = metaDataSetClass.field(PRIVATE | STATIC, setStringType, 'RESIDENT_NORMAL_SET')
        def isInResidentNormalSetMethod = metaDataSetClass.method(PUBLIC, boolean, 'isInResidentNormalSet')
        def isInResidentNormalSetParam = isInResidentNormalSetMethod.param(String.class, "name")
        isInResidentNormalSetMethod.body()._if(residentNormalSetField.eq(JExpr._null()))._then().add(residentNormalSetField.assign(JExpr.invoke(initResidentNormalSetMethod)))
        isInResidentNormalSetMethod.body()._return(residentNormalSetField.invoke("contains").arg(isInResidentNormalSetParam))

        // Resident Important
        def constantImportant = codeModel
                .directClass(Collections.class.getName())
                .staticInvoke('unmodifiableSet')
                .arg(residentImportantSetVar)
        initResidentImportantSetBlock._return(constantImportant)
        def residentImportantSetField = metaDataSetClass.field(PRIVATE | STATIC, setStringType, 'RESIDENT_IMPORTANT_SET')
        def isInResidentImportantSetMethod = metaDataSetClass.method(PUBLIC, boolean, 'isInResidentImportantSet')
        def isInResidentImportantSetParam = isInResidentImportantSetMethod.param(String.class, "name")
        isInResidentImportantSetMethod.body()._if(residentImportantSetField.eq(JExpr._null()))._then().add(residentImportantSetField.assign(JExpr.invoke(initResidentImportantSetMethod)))
        isInResidentImportantSetMethod.body()._return(residentImportantSetField.invoke("contains").arg(isInResidentImportantSetParam))

        //Method Resident White
        def constantMethodWhiteSet = codeModel
                .directClass(Collections.class.getName())
                .staticInvoke('unmodifiableSet')
                .arg(methodResidentWhiteSetVar)
        initMethodResidentWhiteSetBlock._return(constantMethodWhiteSet)

        def methodResidentWhiteSetField = metaDataSetClass.field(PRIVATE | STATIC | FINAL, setStringType, 'METHOD_RESIDENT_WHITE_SET')
        def staticMethodResidentWhiteSetBlock = metaDataSetClass.init()
        staticMethodResidentWhiteSetBlock.add(methodResidentWhiteSetField.assign(JExpr.invoke(initMethodResidentWhiteSetMethod)))

        def isInMethodResidentWhiteSetMethod = metaDataSetClass.method(PUBLIC, boolean, 'isInMethodResidentWhiteSet')
        def isInMethodResidentWhiteSetParam = isInMethodResidentWhiteSetMethod.param(String.class, "name")
        isInMethodResidentWhiteSetMethod.body()._return(methodResidentWhiteSetField.invoke("contains").arg(isInMethodResidentWhiteSetParam))
    }

    private void generateEventTargets(
            MetaData<EventTarget> metaData,
            String metaDataFieldName,
            String initMetaDataMethodName,
            String getMetaDataListMethodName,
            String getMetaDataMapMethodName) {
        def initMetaDataSetMethod = eventTargetMetaDataSetClass.method(
                PRIVATE | STATIC, mapListEventTargetType, initMetaDataMethodName)
        def initBlock = initMetaDataSetMethod.body()
        def mapVar = initBlock.decl(mapListEventTargetType, "eventTargetMap")
        mapVar.init(JExpr._new(codeModel.ref(HashMap.class)))
        def eventTargetVar = initBlock.decl(eventTargetType, "eventTarget")
        def eventTargetInited = false
        def targetListVar = initBlock.decl(listEventTargetType, "eventTargetList")
        def targetListInited = false

        Map<String, List<EventTarget>> eventTargets = new HashMap<>()
        for (EventTarget eventTarget : metaData.elements) {
            for (String event : eventTarget.eventNames) {
                List<EventTarget> targets = eventTargets.get(event)
                if (targets == null) {
                    targets = new ArrayList<>()
                    eventTargets.put(event, targets)
                }
                targets.add(eventTarget)
            }
        }

        for (Map.Entry<String, List<EventTarget>> entry : eventTargets.entrySet()) {
            if (targetListInited) {
                initBlock.assign(targetListVar, JExpr._new(codeModel.ref(ArrayList.class)))
            } else {
                targetListVar.init(JExpr._new(codeModel.ref(ArrayList.class)))
                targetListInited = true
            }

            for (EventTarget eventTarget : entry.value) {
                JExpression eventNamesArray = JExpr._null()
                if (eventTarget.eventNames != null && eventTarget.eventNames.length > 0) {
                    eventNamesArray = JExpr.newArray(codeModel.ref(String.class))
                    for (String eventName : eventTarget.eventNames) {
                        eventNamesArray.add(JExpr.lit(eventName))
                    }
                }
                if (eventTargetInited) {
                    initBlock.assign(eventTargetVar, JExpr._new(eventTargetType).arg(eventNamesArray).arg(eventTarget.classname))
                } else {
                    eventTargetVar.init(JExpr._new(eventTargetType).arg(eventNamesArray).arg(eventTarget.classname))
                    eventTargetInited = true
                }
                initBlock.add(targetListVar.invoke("add").arg(eventTargetVar))
            }
            initBlock.add(mapVar.invoke("put").arg(entry.key).arg(targetListVar))
        }

        def constant = codeModel
                .directClass(Collections.class.getName())
                .staticInvoke('unmodifiableMap')
                .arg(mapVar)
        initBlock._return(constant)

        def metaDataField = eventTargetMetaDataSetClass.field(
                PRIVATE | STATIC | FINAL, mapListEventTargetType, metaDataFieldName)
        def staticBlock = eventTargetMetaDataSetClass.init()
        staticBlock.add(metaDataField.assign(JExpr.invoke(initMetaDataSetMethod)))

        def getMetaDataListMethod = eventTargetMetaDataSetClass.method(
                PUBLIC, listEventTargetType, getMetaDataListMethodName)
        getMetaDataListMethod.annotate(codeModel.ref(Override.class))
        def nameParam = getMetaDataListMethod.param(String.class, "name")
        getMetaDataListMethod.body()._return(metaDataField.invoke("get").arg(nameParam))

        def getMetaDataMapMethod = eventTargetMetaDataSetClass.method(
                PUBLIC, mapListEventTargetType, getMetaDataMapMethodName)
        getMetaDataMapMethod.annotate(codeModel.ref(Override.class))
        getMetaDataMapMethod.body()._return(metaDataField)
    }

    private void generateDependencies(
            MetaData<Dependency> metaData,
            String metaDataFieldName,
            String initMetaDataMethodName,
            String getMetaDataMethodName) {
        def initMetaDataSetMethod = dependencyManagerClass.method(
                PRIVATE | STATIC, mapDependencyType, initMetaDataMethodName)
        def initBlock = initMetaDataSetMethod.body()
        def mapVar = initBlock.decl(mapDependencyType, "dependencyMap")
        mapVar.init(JExpr._new(codeModel.ref(HashMap.class)))
        def dependencyVar = initBlock.decl(dependencyType, "dependency")
        def dependencyInited = false

        for (Dependency dependency : metaData.elements) {
            if (dependencyInited) {
                initBlock.assign(dependencyVar, JExpr._new(dependencyType).arg(dependency.getClassname()))
            } else {
                dependencyVar.init(JExpr._new(dependencyType).arg(dependency.getClassname()))
                dependencyInited = true
            }
            initBlock.add(mapVar.invoke("put").arg(dependency.key).arg(dependencyVar))
        }

        def constant = codeModel
                .directClass(Collections.class.getName())
                .staticInvoke('unmodifiableMap')
                .arg(mapVar)
        initBlock._return(constant)

        def metaDataField = dependencyManagerClass.field(
                PRIVATE | STATIC | FINAL, mapDependencyType, metaDataFieldName)
        def staticBlock = dependencyManagerClass.init()
        staticBlock.add(metaDataField.assign(JExpr.invoke(initMetaDataSetMethod)))

        def getMetaDataMethod = dependencyManagerClass.method(
                PUBLIC, dependencyType, getMetaDataMethodName)
        getMetaDataMethod.annotate(codeModel.ref(Override.class))
        def nameParam = getMetaDataMethod.param(String.class, "key")
        getMetaDataMethod.body()._return(metaDataField.invoke("get").arg(nameParam))
    }

    private void initCardBlackList() {
        if (cardJsonFile == null) {
            return
        }
        def cardBlackListStr = cardJsonFile.getText("utf-8")
        def slurper = new JsonSlurper()
        cardBlackListJSON = slurper.parseText(cardBlackListStr)
    }
}