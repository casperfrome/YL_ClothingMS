package com.yl.clothing;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Guard the layer boundaries without an additional dependency. */
class ArchitectureTest {
    private static final Path SOURCES = Path.of("src/main/java/com/yl/clothing");

    private List<Path> sources(String layer) throws Exception {
        try (var paths = Files.walk(SOURCES.resolve(layer))) {
            return paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    private Class<?> type(Path source) throws Exception {
        String name = SOURCES.relativize(source).toString().replace('\\', '.').replace('/', '.').replace(".java", "");
        return Class.forName("com.yl.clothing." + name);
    }

    @Test void controllersDependOnServicesAndUseTypedResponses() throws Exception {
        for (Path source : sources("controller")) {
            assertThat(Files.readString(source)).doesNotContain("com.yl.clothing.mapper", "com.yl.clothing.entity");
            for (var field : type(source).getDeclaredFields()) {
                assertThat(field.getType().getPackageName()).isEqualTo("com.yl.clothing.service");
            }
            for (var method : type(source).getDeclaredMethods()) assertTyped(method.getGenericReturnType());
        }
    }

    @Test void servicesDoNotDependOnControllersOrResponseEnvelopes() throws Exception {
        for (Path source : sources("service")) {
            assertThat(Files.readString(source)).doesNotContain("com.yl.clothing.controller", "ApiResponse", "ResponseEntity");
            for (var method : type(source).getDeclaredMethods()) assertTyped(method.getGenericReturnType());
        }
    }

    @Test void mappersUseTypedModelsAndExplicitMappings() throws Exception {
        for (Path source : sources("mapper")) {
            assertThat(Files.readString(source)).doesNotContain("com.yl.clothing.controller", "com.yl.clothing.service", "com.yl.clothing.vo");
            for (var method : type(source).getDeclaredMethods()) {
                assertTyped(method.getGenericReturnType());
                for (Type parameter : method.getGenericParameterTypes()) assertTyped(parameter);
            }
            Path xml = Path.of("src/main/resources/mapper", type(source).getSimpleName() + ".xml");
            assertThat(Files.readString(xml)).contains("namespace=\"" + type(source).getName() + "\"")
                    .doesNotContain("resultType=\"map\"");
        }
    }

    private void assertTyped(Type type) {
        if (type instanceof Class<?> clazz) {
            assertThat(clazz).isNotEqualTo(Object.class);
            assertThat(Map.class.isAssignableFrom(clazz)).isFalse();
            if (clazz.isArray()) assertTyped(clazz.getComponentType());
        } else if (type instanceof ParameterizedType parameterized) {
            assertTyped(parameterized.getRawType());
            for (Type argument : parameterized.getActualTypeArguments()) assertTyped(argument);
        } else {
            throw new AssertionError("Unresolved business type: " + type);
        }
    }
}
