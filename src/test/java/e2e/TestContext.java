package e2e;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds temporary session data during a scenario and resolves Gherkin placeholders, e.g:
 * - {{username}} -&gt; USERNAME from .env
 * - {{password}} -&gt; PASSWORD from .env
 * - {{base_url}} -&gt; BASE_URL from .env
 * - {{test_email}} -&gt; a dynamically generated unique email, stable for the whole scenario
 *
 * Shared across step classes within a scenario via Cucumber's PicoContainer injection.
 */
public class TestContext {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, String> state = new LinkedHashMap<>();

    public Map<String, String> getState() {
        return state;
    }

    public String resolve(String value) {
        if (value == null) {
            return null;
        }
        String result = value;

        if (result.contains("{{username}}")) {
            result = result.replace("{{username}}", require("USERNAME"));
        }
        if (result.contains("{{password}}")) {
            result = result.replace("{{password}}", require("PASSWORD"));
        }
        if (result.contains("{{base_url}}")) {
            result = result.replace("{{base_url}}", require("BASE_URL"));
        }
        if (result.contains("{{test_email}}")) {
            if (!state.containsKey("test_email")) {
                String randomStr = randomString(8);
                String template = require("EMAIL_TEMPLATE");
                state.put("test_email", template.replace("{{random}}", randomStr));
            }
            result = result.replace("{{test_email}}", state.get("test_email"));
        }

        for (Map.Entry<String, String> entry : state.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            if (result.contains(placeholder)) {
                result = result.replace(placeholder, entry.getValue());
            }
        }

        return result;
    }

    private String require(String key) {
        String value = EnvConfig.get(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Missing " + key + " in .env");
        }
        return value;
    }

    private static String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
