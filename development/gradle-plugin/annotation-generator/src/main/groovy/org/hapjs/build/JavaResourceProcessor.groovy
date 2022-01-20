/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.build

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.hapjs.build.generator.Dependency
import org.hapjs.build.generator.EventTarget
import org.hapjs.build.generator.Extension
import org.hapjs.build.generator.Inherited
import org.hapjs.build.generator.MetaData
import org.hapjs.build.generator.Widget

class JavaResourceProcessor {
    private def featureExtensions = new MetaData<Extension>()
    private def moduleExtensions = new MetaData<Extension>()
    private def widgetExtensions = new MetaData<Extension>()
    private def widgets = new MetaData<Widget>()
    private def inheriteds = new MetaData<Inherited>()
    private def eventTargets = new MetaData<EventTarget>()
    private def dependencies = new MetaData<Dependency>()
    private def mapper = new ObjectMapper()
    private final File mergedAssetsDir
    private final File javaOutputDir
    private final File cardJsonFile

    JavaResourceProcessor(File mergedAssetsDir, File javaOutputDir, File cardJsonFile) {
        this.mergedAssetsDir = mergedAssetsDir
        this.javaOutputDir = javaOutputDir
        this.cardJsonFile = cardJsonFile
    }

    void process() {
        parseAssets()
        resolveInherited()
        removeParentExtensions(featureExtensions)
        removeParentExtensions(moduleExtensions)
        removeParentExtensions(widgetExtensions)
        removeParentWidget()
        removeParentEventTarget()
        removeParentDependency()
        generateJavaSource()
    }

    private void parseAssets() {
        if (!mergedAssetsDir.exists()) {
            return
        }

        walkMetaFile(mergedAssetsDir)
    }

    private void walkMetaFile(File dir) {
        dir.listFiles(new FileFilter() {
            @Override
            boolean accept(File file) {
                if (file.isDirectory()) {
                    walkMetaFile(file)
                } else {
                    processMetaFile(file)
                }
                return false
            }
        })
    }

    private void processMetaFile(File metaFile) {
        switch (metaFile.name) {
            case 'feature_extension.json':
                def subFeatureExtensions = mapper.readValue(
                        metaFile,
                        new TypeReference<MetaData<Extension>>() {})
                featureExtensions.addAllElement(subFeatureExtensions)
                break
            case 'module_extension.json':
                def subModuleExtensions = mapper.readValue(
                        metaFile,
                        new TypeReference<MetaData<Extension>>() {})
                moduleExtensions.addAllElement(subModuleExtensions)
                break
            case 'widget_extension.json':
                def subWidgetExtensions = mapper.readValue(
                        metaFile,
                        new TypeReference<MetaData<Extension>>() {})
                widgetExtensions.addAllElement(subWidgetExtensions)
                break
            case 'widget.json':
                def subWidgets = mapper.readValue(
                        metaFile,
                        new TypeReference<MetaData<Widget>>() {})
                widgets.addAllElement(subWidgets)
                break
            case 'inherited.json':
                def subInheriteds = mapper.readValue(
                        metaFile,
                        new TypeReference<MetaData<Inherited>>() {})
                inheriteds.addAllElement(subInheriteds)
                break
            case 'event_target.json':
                def subEventTargets = mapper.readValue(
                        metaFile,
                        new TypeReference<MetaData<EventTarget>>() {})
                eventTargets.addAllElement(subEventTargets)
                break
            case 'dependency.json':
                def subDependencies = mapper.readValue(
                        metaFile,
                        new TypeReference<MetaData<Dependency>>() {})
                dependencies.addAllElement(subDependencies)
                break
            default:
                break
        }
    }

    private void removeParentExtensions(MetaData<Extension> extensions) {
        def final SIZE = extensions.elements.size()
        for (int i = 0; i < SIZE - 1; i++) {
            def item = extensions.elements.get(i)
            if (item == null) {
                continue
            }
            for (int j = i + 1; j < SIZE; j++) {
                def other = extensions.elements.get(j)
                if (other == null || item.name != other.name) {
                    continue
                }
                if (item.superClasses.contains(other.classname)) {
                    extensions.elements.set(j, null)
                } else if (other.superClasses.contains(item.classname)) {
                    extensions.elements.set(i, null)
                    break
                }
            }
        }
        for (int i = SIZE - 1; i >= 0; i--) {
            if (extensions.elements.get(i) == null) {
                extensions.elements.removeAt(i)
            }
        }
    }

    private void removeParentWidget() {
        def final SIZE = widgets.elements.size()
        for (int i = 0; i < SIZE - 1; i++) {
            def item = widgets.elements.get(i)
            if (item == null) {
                continue
            }
            for (int j = i + 1; j < SIZE; j++) {
                def other = widgets.elements.get(j)
                if (other == null || item.name != other.name) {
                    continue
                }
                if (item.superClasses.contains(other.classname) && item.needDeleteSuperClasses) {
                    widgets.elements.set(j, null)
                } else if (other.superClasses.contains(item.classname) && other.needDeleteSuperClasses) {
                    widgets.elements.set(i, null)
                    break
                }
            }
        }
        for (int i = SIZE - 1; i >= 0; i--) {
            if (widgets.elements.get(i) == null) {
                widgets.elements.removeAt(i)
            }
        }
    }

    private void removeParentEventTarget() {
        def final SIZE = eventTargets.elements.size()
        for (int i = 0; i < SIZE - 1; i++) {
            def item = eventTargets.elements.get(i)
            if (item == null) {
                continue
            }
            for (int j = i + 1; j < SIZE; j++) {
                def other = eventTargets.elements.get(j)
                if (other == null) {
                    continue
                }
                if (item.superClasses.contains(other.classname)) {
                    eventTargets.elements.set(j, null)
                } else if (other.superClasses.contains(item.classname)) {
                    eventTargets.elements.set(i, null)
                    break
                }
            }
        }
        for (int i = SIZE - 1; i >= 0; i--) {
            if (eventTargets.elements.get(i) == null) {
                eventTargets.elements.removeAt(i)
            }
        }
    }

    private void removeParentDependency() {
        def final SIZE = dependencies.elements.size()
        for (int i = 0; i < SIZE - 1; i++) {
            def item = dependencies.elements.get(i)
            if (item == null) {
                continue
            }
            for (int j = i + 1; j < SIZE; j++) {
                def other = dependencies.elements.get(j)
                if (other == null) {
                    continue
                }
                if (item.superClasses.contains(other.classname)) {
                    dependencies.elements.set(j, null)
                } else if (other.superClasses.contains(item.classname)) {
                    dependencies.elements.set(i, null)
                    break
                }
            }
        }
        for (int i = SIZE - 1; i >= 0; i--) {
            if (dependencies.elements.get(i) == null) {
                dependencies.elements.removeAt(i)
            }
        }
    }

    private void resolveInherited() {
        def extensionMetadatas = [featureExtensions, moduleExtensions, widgetExtensions]
        for (Inherited inherited : inheriteds.elements) {
            boolean resolved = false
            for (MetaData<Extension> metadata : extensionMetadatas) {
                Extension[] result = inherited.resolveExtension(metadata.elements)
                if (result != null) {
                    resolved = true
                    metadata.addElement(result[1])
                    break
                }
            }
            if (!resolved) {
                Widget[] result = inherited.resolveWidget(widgets.elements)
                if (result != null) {
                    resolved = true
                    widgets.addElement(result[1])
                }
            }
            if (!resolved) {
                throw new RuntimeException("Fail to resolve inherited: " + inherited.classname)
            }
        }
    }

    private void generateJavaSource() {
        def generator = new MetadataGenerator(
                featureExtensions, moduleExtensions, widgetExtensions, widgets, eventTargets, dependencies, javaOutputDir, cardJsonFile)
        generator.generate()
    }
}