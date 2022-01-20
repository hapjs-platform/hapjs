/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.build

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project

class AnnotationExecutorPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def generatedAssetsDir = new File(project.buildDir, "generated/hap/src/main/assets")
        def generatedMetadataDir = new File(generatedAssetsDir, "hap/" + project.name)

        project.android.defaultConfig.javaCompileOptions.annotationProcessorOptions.arguments =
                [ outputDir :  generatedMetadataDir.absolutePath]
        project.android.compileOptions.sourceCompatibility = JavaVersion.VERSION_1_8
        project.android.compileOptions.targetCompatibility = JavaVersion.VERSION_1_8
        project.android.sourceSets.main.assets.srcDirs += generatedAssetsDir
    }
}