package e2e;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Loads .env once for the whole test run (mirrors python-dotenv's load_dotenv()).
 */
public final class EnvConfig {

    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();

    private EnvConfig() {
    }

    public static String get(String key) {
        return DOTENV.get(key);
    }
}
