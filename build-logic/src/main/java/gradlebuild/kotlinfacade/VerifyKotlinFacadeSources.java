/*
 * Copyright 2026 the original author or authors.
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

package gradlebuild.kotlinfacade;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Checks that the sources jar ships the generated Kotlin sources unchanged, so IDEs can navigate to the extension
 * function.
 */
@DisableCachingByDefault(because = "Not worth caching")
public abstract class VerifyKotlinFacadeSources extends DefaultTask {

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getSourcesJar();

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getKotlinSources();

    /**
     * Lists the verified entries, so the task is up to date while its inputs do not change.
     */
    @OutputFile
    public abstract RegularFileProperty getReportFile();

    @TaskAction
    public void verify() throws IOException {
        Path kotlinRoot = getKotlinSources().get().getAsFile().toPath();
        List<Path> sources;
        try (Stream<Path> files = Files.walk(kotlinRoot)) {
            sources = files.filter(Files::isRegularFile).sorted().collect(Collectors.toList());
        }

        File jarFile = getSourcesJar().get().getAsFile();
        List<String> verified = new ArrayList<>();
        try (ZipFile jar = new ZipFile(jarFile)) {
            for (Path source : sources) {
                String entryName = kotlinRoot.relativize(source).toString().replace(File.separatorChar, '/');
                ZipEntry entry = jar.getEntry(entryName);
                if (entry == null) {
                    throw new GradleException(jarFile + " does not contain " + entryName);
                }
                byte[] actual;
                try (InputStream content = jar.getInputStream(entry)) {
                    actual = content.readAllBytes();
                }
                if (!Arrays.equals(Files.readAllBytes(source), actual)) {
                    throw new GradleException(entryName + " in " + jarFile + " differs from the generated source");
                }
                verified.add(entryName);
            }
        }

        Path report = getReportFile().get().getAsFile().toPath();
        Files.createDirectories(report.getParent());
        Files.write(report, verified);
    }
}
