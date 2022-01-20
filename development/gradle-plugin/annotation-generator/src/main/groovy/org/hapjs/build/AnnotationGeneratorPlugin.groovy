/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.build

import org.gradle.api.Plugin
import org.gradle.api.Project

class AnnotationGeneratorPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def buildDir = project.buildDir
        // 判断是否为 application project
        if (project.configurations.findByName('apk') != null) {
            def javaOutputDir = new File(buildDir, "generated/hap/src/main/java")
            project.android.sourceSets.main.java.srcDirs += javaOutputDir

            project.afterEvaluate {
                project.android.applicationVariants.all { variant ->
                    def buildType = variant.name
                    def capitalizeBuildType = buildType.capitalize()
                    def compileJavaTask = project.tasks.getByName(
                            "compile${capitalizeBuildType}JavaWithJavac")
                    def mergeAssetsTask = project.tasks.getByName(
                            "merge${capitalizeBuildType}Assets")
                    compileJavaTask.dependsOn(mergeAssetsTask)
                    compileJavaTask.doFirst {
                        def mergedAssetsDir = new File(buildDir,
                                "intermediates/merged_assets/${buildType}/out/hap")
                        if (!mergedAssetsDir.exists()) {
                            mergedAssetsDir = new File(buildDir,
                                    "intermediates/merged_assets/${buildType}/merge${capitalizeBuildType}Assets/out/hap")
                        }
                        def cardJsonFile = new File(buildDir, "intermediates/merged_assets/${buildType}/out/hap/card.json")
                        if (!cardJsonFile.exists()) {
                            cardJsonFile = null
                        }
                        def processor = new JavaResourceProcessor(mergedAssetsDir, javaOutputDir, cardJsonFile)
                        processor.process()
                    }
                }
            }
        } else {
            project.afterEvaluate {
                project.android.libraryVariants.all { variant ->
                    def buildType = variant.name
                    def capitalizeBuildType = buildType.capitalize()
                    def proguardType = project.properties['android.enableR8'] ==
                            'false' ? 'Proguard' : 'R8'
                    def proguardTask = project.tasks.findByName(
                            "transformClassesAndResourcesWith${proguardType}For${capitalizeBuildType}")
                    if (proguardTask != null) {
                        proguardTask.doFirst {
                            def proguardFile = new File(buildDir,
                                    "intermediates/proguard-rules/${buildType}/aapt_rules.txt")
                            if (proguardFile.exists()) {
                                def text = new StringBuilder()
                                proguardFile.withReader('UTF-8') { reader ->
                                    reader.eachLine {
                                        if (!it.contains("\${applicationId}")) {
                                            text.append(it).append('\n')
                                        }
                                    }
                                }
                                proguardFile.write(text.toString(), 'UTF-8')
                            }
                        }
                    }
                }
            }
        }
    }
}