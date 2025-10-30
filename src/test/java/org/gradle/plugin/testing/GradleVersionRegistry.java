package org.gradle.plugin.testing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gradle.util.GradleVersion;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GradleVersionRegistry {

    private static final String ALL_VERSIONS_JSON = "https://services.gradle.org/versions/all";

    private static List<GradleVersion> gradleVersions = null;

    public static List<GradleVersion> getVersions(@Nullable GradleVersion from) {
        // This is a simple memoization to avoid fetching the versions multiple times
        if (gradleVersions == null) {
            try {
                HttpClient client = HttpClient.newHttpClient();
                String responseBody = client.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(ALL_VERSIONS_JSON))
                                .build(),
                        HttpResponse.BodyHandlers.ofString()
                ).body();
                gradleVersions = Collections.unmodifiableList(parseVersionsFromJson(responseBody));
            } catch (Exception ex) {
                throw new RuntimeException("Failed to fetch Gradle versions", ex);
            }
        }

        return gradleVersions
                .stream()
                .filter(v -> v.compareTo(from) > 0)
                .sorted()
                .toList();
    }

    private static List<GradleVersion> parseVersionsFromJson(String json) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(json);

        List<GradleVersion> versions = new ArrayList<>();
        for (JsonNode versionNode : rootNode) {
            String versionString = versionNode.get("version").asText();
            boolean isMilestone = !versionNode.get("milestoneFor").asText().isEmpty();

            if (!isMilestone) {
                GradleVersion gradleVersion = GradleVersion.version(versionString);
                versions.add(gradleVersion);
            }
        }

        return versions;
    }

}
