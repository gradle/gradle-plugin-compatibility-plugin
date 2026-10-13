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

import kotlin.Metadata;
import kotlin.metadata.Attributes;
import kotlin.metadata.KmClassifier;
import kotlin.metadata.KmFunction;
import kotlin.metadata.KmPackage;
import kotlin.metadata.KmType;
import kotlin.metadata.KmTypeProjection;
import kotlin.metadata.KmValueParameter;
import kotlin.metadata.KmVariance;
import kotlin.metadata.Modality;
import kotlin.metadata.Visibility;
import kotlin.metadata.jvm.JvmExtensionsKt;
import kotlin.metadata.jvm.JvmMetadataVersion;
import kotlin.metadata.jvm.JvmMethodSignature;
import kotlin.metadata.jvm.KmModule;
import kotlin.metadata.jvm.KmPackageParts;
import kotlin.metadata.jvm.KotlinClassMetadata;
import kotlin.metadata.jvm.KotlinModuleMetadata;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates the Kotlin DSL entry point {@code PluginDeclaration.compatibility} without running a Kotlin compiler.
 * <p>
 * Kotlin treats a Java class as a file facade holding top-level functions when the class carries a
 * {@code @kotlin.Metadata} annotation describing them and the jar's {@code META-INF/*.kotlin_module} index lists the
 * class. This task derives both from the declaration below using the Kotlin metadata library, emits a Java source
 * with the annotation and the implementation, and emits the equivalent Kotlin source for the sources jar.
 */
@CacheableTask
public abstract class GenerateKotlinFacade extends DefaultTask {
    static final String PACKAGE_NAME = "org.gradle.plugin.compatibility";
    static final String FILE_NAME = "SupportedFeaturesExtensions";
    static final String FACADE_CLASS_NAME = FILE_NAME + "Kt";

    private static final String FUNCTION_NAME = "compatibility";
    private static final String RECEIVER_TYPE = "org/gradle/plugin/devel/PluginDeclaration";
    private static final String PARAMETER_NAME = "action";
    private static final String PARAMETER_TYPE = "org/gradle/api/Action";
    private static final String PARAMETER_TYPE_ARGUMENT = "org/gradle/plugin/compatibility/CompatibilityExtension";
    private static final String JVM_DESCRIPTOR = "(L" + RECEIVER_TYPE + ";L" + PARAMETER_TYPE + ";)V";

    // kotlin.Metadata.extraInt bits 4 and 5: compiled with the IR backend, metadata and ABI are stable
    private static final int EXTRA_INT = 48;

    private static final String LICENSE_HEADER = """
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
        """;

    private static final String KOTLIN_SOURCE = LICENSE_HEADER + """

        package org.gradle.plugin.compatibility

        import org.gradle.api.Action
        import org.gradle.plugin.compatibility.internal.CompatibilityStrategy
        import org.gradle.plugin.devel.PluginDeclaration

        /**
         * Applies compatibility configuration defined in `action` to the `PluginDeclaration`.
         *
         * @since 1.0.0
         */
        fun PluginDeclaration.compatibility(action: Action<in CompatibilityExtension>) {
            CompatibilityStrategy.getInstance().configure(this, action)
        }
        """;

    private static final String JAVA_SOURCE_TEMPLATE = LICENSE_HEADER + """

        package org.gradle.plugin.compatibility;

        import org.gradle.api.Action;
        import org.gradle.plugin.compatibility.internal.CompatibilityStrategy;
        import org.gradle.plugin.devel.PluginDeclaration;

        /**
         * Kotlin file facade of {@code SupportedFeaturesExtensions.kt}, generated by the build.
         */
        @kotlin.Metadata(
            mv = {%s},
            k = %d,
            xi = %d,
            d1 = {%s},
            d2 = {%s}
        )
        public final class SupportedFeaturesExtensionsKt {
            private SupportedFeaturesExtensionsKt() {
            }

            public static void compatibility(PluginDeclaration declaration, Action<? super CompatibilityExtension> action) {
                CompatibilityStrategy.getInstance().configure(declaration, action);
            }
        }
        """;

    /**
     * Name of the Kotlin module, which names the {@code .kotlin_module} file and is recorded in the metadata.
     */
    @Input
    public abstract Property<String> getModuleName();

    /**
     * Version of the metadata format to write, as major, minor and patch.
     */
    @Input
    public abstract ListProperty<Integer> getMetadataVersion();

    @OutputDirectory
    public abstract DirectoryProperty getJavaOutputDirectory();

    @OutputDirectory
    public abstract DirectoryProperty getKotlinOutputDirectory();

    @OutputDirectory
    public abstract DirectoryProperty getResourcesOutputDirectory();

    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();

    @TaskAction
    public void generate() throws IOException {
        List<Integer> versionNumbers = getMetadataVersion().get();
        if (versionNumbers.size() != 3) {
            throw new IllegalArgumentException("metadataVersion must have three components, got " + versionNumbers);
        }
        JvmMetadataVersion version = new JvmMetadataVersion(versionNumbers.get(0), versionNumbers.get(1), versionNumbers.get(2));
        String moduleName = getModuleName().get();

        Metadata metadata = facadeMetadata(version, moduleName);
        byte[] moduleIndex = moduleIndex(version);

        Path packagePath = Path.of(PACKAGE_NAME.replace('.', '/'));
        Path javaDir = cleanOutputDirectory(getJavaOutputDirectory());
        Path kotlinDir = cleanOutputDirectory(getKotlinOutputDirectory());
        Path resourcesDir = cleanOutputDirectory(getResourcesOutputDirectory());

        writeText(javaDir.resolve(packagePath).resolve(FACADE_CLASS_NAME + ".java"), javaSource(metadata));
        writeText(kotlinDir.resolve(packagePath).resolve(FILE_NAME + ".kt"), KOTLIN_SOURCE);
        writeBytes(resourcesDir.resolve("META-INF").resolve(moduleName + ".kotlin_module"), moduleIndex);
    }

    private static Metadata facadeMetadata(JvmMetadataVersion version, String moduleName) {
        KmType actionType = classType(PARAMETER_TYPE);
        actionType.getArguments().add(new KmTypeProjection(KmVariance.IN, classType(PARAMETER_TYPE_ARGUMENT)));

        KmValueParameter parameter = new KmValueParameter(PARAMETER_NAME);
        parameter.setType(actionType);

        KmFunction function = new KmFunction(FUNCTION_NAME);
        Attributes.setVisibility(function, Visibility.PUBLIC);
        Attributes.setModality(function, Modality.FINAL);
        function.setReceiverParameterType(classType(RECEIVER_TYPE));
        function.setReturnType(classType("kotlin/Unit"));
        function.getValueParameters().add(parameter);

        KmPackage kmPackage = new KmPackage();
        kmPackage.getFunctions().add(function);
        JvmExtensionsKt.setModuleName(kmPackage, moduleName);

        Metadata metadata = new KotlinClassMetadata.FileFacade(kmPackage, version, EXTRA_INT).write();
        verifyMatchesJavaMethod(metadata);
        return metadata;
    }

    /**
     * The metadata does not spell out the JVM signature, Kotlin derives it. Reading the metadata back the way the
     * Kotlin compiler does confirms that derivation lands on the Java method the facade declares.
     */
    private static void verifyMatchesJavaMethod(Metadata metadata) {
        KotlinClassMetadata.FileFacade facade = (KotlinClassMetadata.FileFacade) KotlinClassMetadata.Companion.readStrict(metadata);
        List<KmFunction> functions = facade.getKmPackage().getFunctions();
        if (functions.size() != 1) {
            throw new IllegalStateException("Expected exactly one function in the facade metadata, got " + functions.size());
        }
        JvmMethodSignature signature = JvmExtensionsKt.getSignature(functions.get(0));
        if (signature == null || !signature.getName().equals(FUNCTION_NAME) || !signature.getDescriptor().equals(JVM_DESCRIPTOR)) {
            throw new IllegalStateException("Facade metadata resolves to " + signature + ", expected " + FUNCTION_NAME + JVM_DESCRIPTOR);
        }
    }

    private static byte[] moduleIndex(JvmMetadataVersion version) {
        List<String> fileFacades = new ArrayList<>(List.of(PACKAGE_NAME.replace('.', '/') + "/" + FACADE_CLASS_NAME));
        KmModule module = new KmModule();
        module.getPackageParts().put(PACKAGE_NAME, new KmPackageParts(fileFacades, new LinkedHashMap<>()));
        return new KotlinModuleMetadata(module, version).write();
    }

    private static String javaSource(Metadata metadata) {
        return JAVA_SOURCE_TEMPLATE.formatted(
            Arrays.stream(metadata.mv()).mapToObj(Integer::toString).collect(Collectors.joining(", ")),
            metadata.k(),
            metadata.xi(),
            Arrays.stream(metadata.d1()).map(GenerateKotlinFacade::javaStringLiteral).collect(Collectors.joining(", ")),
            Arrays.stream(metadata.d2()).map(GenerateKotlinFacade::javaStringLiteral).collect(Collectors.joining(", "))
        );
    }

    /**
     * Encodes a string as a Java literal. Control characters use octal escapes, because javac expands {@code \\u}
     * escapes before tokenizing and an escaped line terminator or quote would break the literal.
     */
    private static String javaStringLiteral(String value) {
        StringBuilder literal = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            if (c == '"' || c == '\\') {
                literal.append('\\').append(c);
            } else if (c >= 0x20 && c < 0x7f) {
                literal.append(c);
            } else if (c < 0x100) {
                literal.append(String.format("\\%03o", (int) c));
            } else {
                literal.append(String.format("\\u%04x", (int) c));
            }
        }
        return literal.append('"').toString();
    }

    private static KmType classType(String name) {
        KmType type = new KmType();
        type.setClassifier(new KmClassifier.Class(name));
        return type;
    }

    private Path cleanOutputDirectory(DirectoryProperty directory) throws IOException {
        Path path = directory.get().getAsFile().toPath();
        getFileSystemOperations().delete(spec -> spec.delete(path));
        return Files.createDirectories(path);
    }

    private static void writeText(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private static void writeBytes(Path file, byte[] content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, content);
    }
}
