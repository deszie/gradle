/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.ide.visualstudio.internal

import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.FileResolver
import org.gradle.internal.reflect.DirectInstantiator
import org.gradle.language.nativeplatform.HeaderExportingSourceSet
import org.gradle.nativeplatform.NativeComponentSpec
import spock.lang.Specification

import static org.gradle.ide.visualstudio.internal.DefaultVisualStudioProject.getUUID

class DefaultVisualStudioProjectTest extends Specification {
    private DirectInstantiator instantiator = DirectInstantiator.INSTANCE
    def component = Mock(NativeComponentSpec)
    def fileResolver = Mock(FileResolver)
    def vsProject = project("projectName")

    def project(String vsProjectName, NativeComponentSpec component = component) {
        new DefaultVisualStudioProject(vsProjectName, component.getName(), null, null, fileResolver, instantiator)
    }

    def "names"() {
        final projectFile = new File("project")
        final filtersFile = new File("filters")
        when:
        fileResolver.resolve("projectName.vcxproj") >> projectFile
        fileResolver.resolve("projectName.vcxproj.filters") >> filtersFile

        then:
        vsProject.name == "projectName"
        vsProject.projectFile.location == projectFile
        vsProject.filtersFile.location == filtersFile
    }

    def "includes source, resource, and header files from target binary"() {
        when:
        def sourcefile1 = new File("s1")
        def sourcefile2 = new File("s2")
        def sourcefile3 = new File("s3")
        def resourcefile1 = new File("r1")
        def resourcefile2 = new File("r2")
        def headerfile1 = new File("h1")
        def headerfile2 = new File("h2")
        def headerfile3 = new File("h3")

        vsProject.addConfiguration(targetBinary([sourcefile1, sourcefile2], [resourcefile1, resourcefile2], [headerfile1, headerfile2, headerfile3]), Mock(VisualStudioProjectConfiguration))
        vsProject.addSourceFile(sourcefile3)

        then:
        vsProject.sourceFiles == [sourcefile1, sourcefile2, sourcefile3] as Set
        vsProject.resourceFiles == [resourcefile1, resourcefile2] as Set
        vsProject.headerFiles == [headerfile1, headerfile2, headerfile3] as Set
    }

    def "has consistent uuid for same file"() {
        when:
        def file = new File("foo")
        def sameFile = new File("foo")
        def differentFile = new File("bar")

        then:
        getUUID(file) == getUUID(sameFile)
        getUUID(file) != getUUID(differentFile)
    }

    private VisualStudioTargetBinary targetBinary(List<File> sourceFiles, List<File> resourceFiles, List<File> headerFiles) {
        def binary = Mock(VisualStudioTargetBinary)
        2 * binary.sourceFiles >> fileCollection(sourceFiles)
        2 * binary.resourceFiles >> fileCollection(resourceFiles)
        2 * binary.headerFiles >> fileCollection(headerFiles)
        return binary
    }

    private FileCollection fileCollection(List<File> files) {
        return Stub(FileCollection) {
            getFiles() >> files
        }
    }

    private HeaderExportingSourceSet headerSourceSet(List<File> exportedHeaders, List<File> implicitHeaders = []) {
        def exportedHeaderFiles = exportedHeaders as Set
        def implicitHeaderFiles = implicitHeaders as Set
        def sourceSet = Mock(HeaderExportingSourceSet)
        def sourceDirs = Mock(SourceDirectorySet)
        1 * sourceSet.exportedHeaders >> sourceDirs
        1 * sourceDirs.files >> exportedHeaderFiles
        def implicitHeaderSet = Mock(SourceDirectorySet)
        1 * sourceSet.implicitHeaders >> implicitHeaderSet
        1 * implicitHeaderSet.files >> implicitHeaderFiles
        return sourceSet
    }
}
