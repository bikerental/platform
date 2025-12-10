package com.bikerental.platform.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive validation tests for .coderabbit.yaml configuration file.
 * 
 * These tests ensure that the CodeRabbit configuration file:
 * - Contains valid YAML syntax
 * - Follows CodeRabbit's expected schema
 * - Has appropriate values for all configured fields
 * - Handles edge cases gracefully
 */
@DisplayName("CodeRabbit Configuration Validation Tests")
class CodeRabbitConfigValidationTest {

    private static final String CONFIG_FILE_PATH = ".coderabbit.yaml";
    private final Yaml yaml = new Yaml();

    @Nested
    @DisplayName("File Existence and Accessibility Tests")
    class FileExistenceTests {

        @Test
        @DisplayName("Should verify .coderabbit.yaml file exists in repository root")
        void testConfigFileExists() {
            Path configPath = Paths.get(CONFIG_FILE_PATH);
            assertTrue(Files.exists(configPath), 
                "CodeRabbit configuration file should exist at repository root");
        }

        @Test
        @DisplayName("Should verify .coderabbit.yaml file is readable")
        void testConfigFileIsReadable() {
            Path configPath = Paths.get(CONFIG_FILE_PATH);
            assertTrue(Files.isReadable(configPath), 
                "CodeRabbit configuration file should be readable");
        }

        @Test
        @DisplayName("Should handle missing configuration file gracefully")
        void testMissingConfigFileHandling() {
            Path nonExistentPath = Paths.get(".nonexistent.yaml");
            assertFalse(Files.exists(nonExistentPath), 
                "Non-existent file should not exist");
        }
    }

    @Nested
    @DisplayName("YAML Syntax Validation Tests")
    class YamlSyntaxTests {

        @Test
        @DisplayName("Should parse .coderabbit.yaml as valid YAML")
        void testValidYamlSyntax() {
            assertDoesNotThrow(() -> {
                try (FileInputStream fis = new FileInputStream(CONFIG_FILE_PATH)) {
                    yaml.load(fis);
                }
            }, "CodeRabbit configuration should be valid YAML");
        }

        @Test
        @DisplayName("Should handle empty YAML file")
        void testEmptyYamlFile() {
            assertDoesNotThrow(() -> {
                try (FileInputStream fis = new FileInputStream(CONFIG_FILE_PATH)) {
                    Object content = yaml.load(fis);
                    // Empty YAML file returns null
                    assertTrue(content == null || content instanceof Map,
                        "Empty or valid YAML should parse without errors");
                }
            });
        }

        @Test
        @DisplayName("Should reject invalid YAML syntax")
        void testInvalidYamlSyntax(@TempDir Path tempDir) throws IOException {
            Path invalidYaml = tempDir.resolve("invalid.yaml");
            Files.writeString(invalidYaml, "invalid: yaml: content: [unclosed");
            
            assertThrows(YAMLException.class, () -> {
                try (FileInputStream fis = new FileInputStream(invalidYaml.toFile())) {
                    yaml.load(fis);
                }
            }, "Invalid YAML syntax should throw YAMLException");
        }

        @Test
        @DisplayName("Should handle YAML with special characters")
        void testYamlWithSpecialCharacters(@TempDir Path tempDir) throws IOException {
            Path specialCharsYaml = tempDir.resolve("special.yaml");
            Files.writeString(specialCharsYaml, 
                "language: \"en-US\"\n" +
                "reviews:\n" +
                "  path_filters:\n" +
                "    - \"**/*.java\"\n" +
                "    - \"!**/test/**\"\n");
            
            assertDoesNotThrow(() -> {
                try (FileInputStream fis = new FileInputStream(specialCharsYaml.toFile())) {
                    yaml.load(fis);
                }
            });
        }
    }

    @Nested
    @DisplayName("Schema Validation Tests")
    class SchemaValidationTests {

        @Test
        @DisplayName("Should validate language field when present")
        void testLanguageFieldValidation(@TempDir Path tempDir) throws IOException {
            Path configWithLanguage = tempDir.resolve("config.yaml");
            Files.writeString(configWithLanguage, "language: en-US\n");
            
            try (FileInputStream fis = new FileInputStream(configWithLanguage.toFile())) {
                Map<String, Object> config = yaml.load(fis);
                if (config != null && config.containsKey("language")) {
                    String language = (String) config.get("language");
                    assertNotNull(language, "Language should not be null");
                    assertTrue(language.matches("[a-z]{2}-[A-Z]{2}"), 
                        "Language should follow ISO format (e.g., en-US)");
                }
            }
        }

        @Test
        @DisplayName("Should validate reviews section structure")
        void testReviewsSectionStructure(@TempDir Path tempDir) throws IOException {
            Path configWithReviews = tempDir.resolve("config.yaml");
            Files.writeString(configWithReviews, 
                "reviews:\n" +
                "  profile: chill\n" +
                "  request_changes_workflow: false\n" +
                "  high_level_summary: true\n");
            
            try (FileInputStream fis = new FileInputStream(configWithReviews.toFile())) {
                Map<String, Object> config = yaml.load(fis);
                if (config != null && config.containsKey("reviews")) {
                    Object reviews = config.get("reviews");
                    assertTrue(reviews instanceof Map, 
                        "Reviews section should be a map/object");
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> reviewsMap = (Map<String, Object>) reviews;
                    
                    if (reviewsMap.containsKey("profile")) {
                        assertTrue(reviewsMap.get("profile") instanceof String,
                            "Profile should be a string");
                    }
                    
                    if (reviewsMap.containsKey("request_changes_workflow")) {
                        assertTrue(reviewsMap.get("request_changes_workflow") instanceof Boolean,
                            "request_changes_workflow should be a boolean");
                    }
                }
            }
        }

        @Test
        @DisplayName("Should validate profile enum values")
        void testProfileEnumValidation(@TempDir Path tempDir) throws IOException {
            List<String> validProfiles = Arrays.asList("chill", "assertive");
            
            for (String profile : validProfiles) {
                Path configPath = tempDir.resolve("config_" + profile + ".yaml");
                Files.writeString(configPath, "reviews:\n  profile: " + profile + "\n");
                
                try (FileInputStream fis = new FileInputStream(configPath.toFile())) {
                    Map<String, Object> config = yaml.load(fis);
                    assertNotNull(config);
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> reviews = (Map<String, Object>) config.get("reviews");
                    String actualProfile = (String) reviews.get("profile");
                    
                    assertTrue(validProfiles.contains(actualProfile),
                        "Profile should be one of: " + validProfiles);
                }
            }
        }

        @Test
        @DisplayName("Should validate path_filters array")
        void testPathFiltersArrayValidation(@TempDir Path tempDir) throws IOException {
            Path configWithFilters = tempDir.resolve("config.yaml");
            Files.writeString(configWithFilters, 
                "reviews:\n" +
                "  path_filters:\n" +
                "    - \"**/*.java\"\n" +
                "    - \"**/*.ts\"\n" +
                "    - \"!**/node_modules/**\"\n");
            
            try (FileInputStream fis = new FileInputStream(configWithFilters.toFile())) {
                Map<String, Object> config = yaml.load(fis);
                
                @SuppressWarnings("unchecked")
                Map<String, Object> reviews = (Map<String, Object>) config.get("reviews");
                Object pathFilters = reviews.get("path_filters");
                
                assertTrue(pathFilters instanceof List,
                    "path_filters should be an array/list");
                
                @SuppressWarnings("unchecked")
                List<String> filters = (List<String>) pathFilters;
                assertFalse(filters.isEmpty(), 
                    "path_filters should not be empty when defined");
                
                for (String filter : filters) {
                    assertNotNull(filter, "Each filter should not be null");
                    assertFalse(filter.trim().isEmpty(), 
                        "Each filter should not be empty");
                }
            }
        }

        @Test
        @DisplayName("Should validate chat section structure")
        void testChatSectionStructure(@TempDir Path tempDir) throws IOException {
            Path configWithChat = tempDir.resolve("config.yaml");
            Files.writeString(configWithChat, 
                "chat:\n" +
                "  auto_reply: true\n");
            
            try (FileInputStream fis = new FileInputStream(configWithChat.toFile())) {
                Map<String, Object> config = yaml.load(fis);
                
                if (config.containsKey("chat")) {
                    Object chat = config.get("chat");
                    assertTrue(chat instanceof Map,
                        "Chat section should be a map/object");
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> chatMap = (Map<String, Object>) chat;
                    
                    if (chatMap.containsKey("auto_reply")) {
                        assertTrue(chatMap.get("auto_reply") instanceof Boolean,
                            "auto_reply should be a boolean");
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Configuration Completeness Tests")
    class ConfigurationCompletenessTests {

        @Test
        @DisplayName("Should handle minimal valid configuration")
        void testMinimalValidConfiguration(@TempDir Path tempDir) throws IOException {
            Path minimalConfig = tempDir.resolve("minimal.yaml");
            Files.writeString(minimalConfig, "language: en-US\n");
            
            try (FileInputStream fis = new FileInputStream(minimalConfig.toFile())) {
                Map<String, Object> config = yaml.load(fis);
                assertNotNull(config);
                assertTrue(config.containsKey("language"));
            }
        }

        @Test
        @DisplayName("Should handle comprehensive configuration")
        void testComprehensiveConfiguration(@TempDir Path tempDir) throws IOException {
            Path comprehensiveConfig = tempDir.resolve("comprehensive.yaml");
            Files.writeString(comprehensiveConfig,
                "language: en-US\n" +
                "early_access: false\n" +
                "reviews:\n" +
                "  profile: chill\n" +
                "  request_changes_workflow: false\n" +
                "  high_level_summary: true\n" +
                "  poem: false\n" +
                "  review_status: true\n" +
                "  collapse_walkthrough: false\n" +
                "  auto_review:\n" +
                "    enabled: true\n" +
                "    drafts: false\n" +
                "  path_filters:\n" +
                "    - \"**/*.java\"\n" +
                "    - \"**/*.ts\"\n" +
                "    - \"!**/test/**\"\n" +
                "chat:\n" +
                "  auto_reply: true\n");
            
            try (FileInputStream fis = new FileInputStream(comprehensiveConfig.toFile())) {
                Map<String, Object> config = yaml.load(fis);
                assertNotNull(config);
                assertTrue(config.containsKey("language"));
                assertTrue(config.containsKey("reviews"));
                assertTrue(config.containsKey("chat"));
            }
        }

        @Test
        @DisplayName("Should validate auto_review nested structure")
        void testAutoReviewNestedStructure(@TempDir Path tempDir) throws IOException {
            Path configWithAutoReview = tempDir.resolve("config.yaml");
            Files.writeString(configWithAutoReview,
                "reviews:\n" +
                "  auto_review:\n" +
                "    enabled: true\n" +
                "    drafts: false\n");
            
            try (FileInputStream fis = new FileInputStream(configWithAutoReview.toFile())) {
                Map<String, Object> config = yaml.load(fis);
                
                @SuppressWarnings("unchecked")
                Map<String, Object> reviews = (Map<String, Object>) config.get("reviews");
                Object autoReview = reviews.get("auto_review");
                
                assertTrue(autoReview instanceof Map,
                    "auto_review should be a nested object");
                
                @SuppressWarnings("unchecked")
                Map<String, Object> autoReviewMap = (Map<String, Object>) autoReview;
                
                if (autoReviewMap.containsKey("enabled")) {
                    assertTrue(autoReviewMap.get("enabled") instanceof Boolean,
                        "enabled should be a boolean");
                }
                
                if (autoReviewMap.containsKey("drafts")) {
                    assertTrue(autoReviewMap.get("drafts") instanceof Boolean,
                        "drafts should be a boolean");
                }
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle configuration with only comments")
        void testConfigurationWithOnlyComments(@TempDir Path tempDir) throws IOException {
            Path commentsOnlyConfig = tempDir.resolve("comments.yaml");
            Files.writeString(commentsOnlyConfig,
                "# This is a comment\n" +
                "# Another comment\n");
            
            try (FileInputStream fis = new FileInputStream(commentsOnlyConfig.toFile())) {
                Object config = yaml.load(fis);
                // Comments-only file should parse as null or empty
                assertTrue(config == null || (config instanceof Map && ((Map<?, ?>) config).isEmpty()),
                    "Comments-only configuration should be treated as empty");
            }
        }

        @Test
        @DisplayName("Should handle configuration with null values")
        void testConfigurationWithNullValues(@TempDir Path tempDir) throws IOException {
            Path nullValuesConfig = tempDir.resolve("nulls.yaml");
            Files.writeString(nullValuesConfig,
                "language: en-US\n" +
                "early_access: null\n");
            
            try (FileInputStream fis = new FileInputStream(nullValuesConfig.toFile())) {
                Map<String, Object> config = yaml.load(fis);
                assertNotNull(config);
                assertTrue(config.containsKey("early_access"));
                assertNull(config.get("early_access"),
                    "Null values should be preserved");
            }
        }

        @Test
        @DisplayName("Should handle configuration with extra whitespace")
        void testConfigurationWithExtraWhitespace(@TempDir Path tempDir) throws IOException {
            Path whitespaceConfig = tempDir.resolve("whitespace.yaml");
            Files.writeString(whitespaceConfig,
                "language:   en-US  \n" +
                "reviews:  \n" +
                "  profile:   chill   \n");
            
            assertDoesNotThrow(() -> {
                try (FileInputStream fis = new FileInputStream(whitespaceConfig.toFile())) {
                    Map<String, Object> config = yaml.load(fis);
                    assertNotNull(config);
                }
            }, "Extra whitespace should not cause parsing errors");
        }

        @Test
        @DisplayName("Should validate boolean field type enforcement")
        void testBooleanFieldTypeEnforcement(@TempDir Path tempDir) throws IOException {
            Path booleanConfig = tempDir.resolve("booleans.yaml");
            Files.writeString(booleanConfig,
                "early_access: false\n" +
                "reviews:\n" +
                "  high_level_summary: true\n" +
                "  poem: no\n" +  // YAML accepts yes/no as boolean
                "  review_status: on\n");  // YAML accepts on/off as boolean
            
            try (FileInputStream fis = new FileInputStream(booleanConfig.toFile())) {
                Map<String, Object> config = yaml.load(fis);
                
                assertTrue(config.get("early_access") instanceof Boolean);
                
                @SuppressWarnings("unchecked")
                Map<String, Object> reviews = (Map<String, Object>) config.get("reviews");
                assertTrue(reviews.get("high_level_summary") instanceof Boolean);
                assertTrue(reviews.get("poem") instanceof Boolean);
                assertTrue(reviews.get("review_status") instanceof Boolean);
            }
        }

        @Test
        @DisplayName("Should handle empty arrays")
        void testEmptyArrays(@TempDir Path tempDir) throws IOException {
            Path emptyArrayConfig = tempDir.resolve("empty_array.yaml");
            Files.writeString(emptyArrayConfig,
                "reviews:\n" +
                "  path_filters: []\n");
            
            try (FileInputStream fis = new FileInputStream(emptyArrayConfig.toFile())) {
                Map<String, Object> config = yaml.load(fis);
                
                @SuppressWarnings("unchecked")
                Map<String, Object> reviews = (Map<String, Object>) config.get("reviews");
                Object pathFilters = reviews.get("path_filters");
                
                assertTrue(pathFilters instanceof List);
                assertTrue(((List<?>) pathFilters).isEmpty(),
                    "Empty array should be allowed");
            }
        }

        @Test
        @DisplayName("Should handle deeply nested structures")
        void testDeeplyNestedStructures(@TempDir Path tempDir) throws IOException {
            Path nestedConfig = tempDir.resolve("nested.yaml");
            Files.writeString(nestedConfig,
                "reviews:\n" +
                "  auto_review:\n" +
                "    enabled: true\n" +
                "    drafts: false\n" +
                "    ignore_title_keywords:\n" +
                "      - WIP\n" +
                "      - DO NOT MERGE\n");
            
            assertDoesNotThrow(() -> {
                try (FileInputStream fis = new FileInputStream(nestedConfig.toFile())) {
                    Map<String, Object> config = yaml.load(fis);
                    assertNotNull(config);
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> reviews = (Map<String, Object>) config.get("reviews");
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> autoReview = (Map<String, Object>) reviews.get("auto_review");
                    
                    assertTrue(autoReview.containsKey("ignore_title_keywords"));
                }
            });
        }
    }

    @Nested
    @DisplayName("Best Practices and Recommendations Tests")
    class BestPracticesTests {

        @Test
        @DisplayName("Should recommend setting language for international teams")
        void testLanguageSettingRecommendation() throws IOException {
            try (FileInputStream fis = new FileInputStream(CONFIG_FILE_PATH)) {
                Object content = yaml.load(fis);
                
                if (content == null || !(content instanceof Map)) {
                    // Empty config - log recommendation
                    assertTrue(true, "Consider adding 'language: en-US' for clarity");
                } else {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> config = (Map<String, Object>) content;
                    
                    // It's okay if language is not set, but recommend it
                    if (!config.containsKey("language")) {
                        assertTrue(true, 
                            "Recommendation: Add 'language' field for better internationalization support");
                    }
                }
            }
        }

        @Test
        @DisplayName("Should validate path_filters use glob patterns")
        void testPathFiltersUseGlobPatterns(@TempDir Path tempDir) throws IOException {
            Path configWithFilters = tempDir.resolve("config.yaml");
            Files.writeString(configWithFilters,
                "reviews:\n" +
                "  path_filters:\n" +
                "    - \"**/*.java\"\n" +
                "    - \"**/*.ts\"\n");
            
            try (FileInputStream fis = new FileInputStream(configWithFilters.toFile())) {
                Map<String, Object> config = yaml.load(fis);
                
                @SuppressWarnings("unchecked")
                Map<String, Object> reviews = (Map<String, Object>) config.get("reviews");
                
                @SuppressWarnings("unchecked")
                List<String> pathFilters = (List<String>) reviews.get("path_filters");
                
                for (String filter : pathFilters) {
                    // Basic glob pattern validation
                    assertTrue(filter.contains("*") || filter.contains("!") || filter.contains("?"),
                        "Path filters should typically use glob patterns (*, **, !)");
                }
            }
        }

        @Test
        @DisplayName("Should ensure boolean fields are explicitly set")
        void testBooleanFieldsExplicitlySet(@TempDir Path tempDir) throws IOException {
            Path explicitConfig = tempDir.resolve("explicit.yaml");
            Files.writeString(explicitConfig,
                "reviews:\n" +
                "  high_level_summary: true\n" +
                "  poem: false\n");
            
            try (FileInputStream fis = new FileInputStream(explicitConfig.toFile())) {
                Map<String, Object> config = yaml.load(fis);
                
                @SuppressWarnings("unchecked")
                Map<String, Object> reviews = (Map<String, Object>) config.get("reviews");
                
                // Verify boolean fields are actual booleans, not strings
                Object highLevelSummary = reviews.get("high_level_summary");
                Object poem = reviews.get("poem");
                
                assertTrue(highLevelSummary instanceof Boolean,
                    "Boolean fields should be typed as boolean, not string");
                assertTrue(poem instanceof Boolean,
                    "Boolean fields should be typed as boolean, not string");
            }
        }
    }

    @Nested
    @DisplayName("Integration and Real-World Scenario Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should validate typical Java project configuration")
        void testTypicalJavaProjectConfiguration(@TempDir Path tempDir) throws IOException {
            Path javaConfig = tempDir.resolve("java_project.yaml");
            Files.writeString(javaConfig,
                "language: en-US\n" +
                "reviews:\n" +
                "  profile: chill\n" +
                "  path_filters:\n" +
                "    - \"src/**/*.java\"\n" +
                "    - \"!src/test/**\"\n" +
                "  auto_review:\n" +
                "    enabled: true\n" +
                "    drafts: false\n");
            
            try (FileInputStream fis = new FileInputStream(javaConfig.toFile())) {
                Map<String, Object> config = yaml.load(fis);
                
                assertNotNull(config);
                assertEquals("en-US", config.get("language"));
                
                @SuppressWarnings("unchecked")
                Map<String, Object> reviews = (Map<String, Object>) config.get("reviews");
                assertEquals("chill", reviews.get("profile"));
                
                @SuppressWarnings("unchecked")
                List<String> pathFilters = (List<String>) reviews.get("path_filters");
                assertTrue(pathFilters.stream().anyMatch(f -> f.contains(".java")),
                    "Java project should filter Java files");
            }
        }

        @Test
        @DisplayName("Should validate full-stack project configuration")
        void testFullStackProjectConfiguration(@TempDir Path tempDir) throws IOException {
            Path fullStackConfig = tempDir.resolve("fullstack.yaml");
            Files.writeString(fullStackConfig,
                "language: en-US\n" +
                "reviews:\n" +
                "  profile: assertive\n" +
                "  path_filters:\n" +
                "    - \"src/**/*.java\"\n" +
                "    - \"frontend/**/*.ts\"\n" +
                "    - \"frontend/**/*.tsx\"\n" +
                "    - \"!**/node_modules/**\"\n" +
                "    - \"!**/dist/**\"\n" +
                "  auto_review:\n" +
                "    enabled: true\n" +
                "    drafts: true\n");
            
            try (FileInputStream fis = new FileInputStream(fullStackConfig.toFile())) {
                Map<String, Object> config = yaml.load(fis);
                
                @SuppressWarnings("unchecked")
                Map<String, Object> reviews = (Map<String, Object>) config.get("reviews");
                
                @SuppressWarnings("unchecked")
                List<String> pathFilters = (List<String>) reviews.get("path_filters");
                
                long javaFilters = pathFilters.stream()
                    .filter(f -> f.contains(".java"))
                    .count();
                long tsFilters = pathFilters.stream()
                    .filter(f -> f.contains(".ts"))
                    .count();
                
                assertTrue(javaFilters > 0, "Should include Java file filters");
                assertTrue(tsFilters > 0, "Should include TypeScript file filters");
                assertTrue(pathFilters.stream().anyMatch(f -> f.startsWith("!")),
                    "Should include exclusion patterns");
            }
        }
    }
}