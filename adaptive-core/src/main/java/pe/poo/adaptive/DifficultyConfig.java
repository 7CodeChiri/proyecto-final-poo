package pe.poo.adaptive;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** External thresholds read once when the application starts. */
public record DifficultyConfig(int recoveryDamage, int challengeSeconds, String balancedClass,
        String recoveryClass, String challengeClass) {
    public DifficultyConfig(int recoveryDamage, int challengeSeconds) {
        this(recoveryDamage, challengeSeconds, null, null, null);
    }

    public DifficultyConfig {
        if (recoveryDamage < 1 || challengeSeconds < 1) {
            throw new IllegalArgumentException("Los dos umbrales deben ser positivos");
        }
        balancedClass = plugin(balancedClass, "BalancedGenerator");
        recoveryClass = plugin(recoveryClass, "RecoveryGenerator");
        challengeClass = plugin(challengeClass, "ChallengeGenerator");
    }

    private static String plugin(String value, String fallback) {
        return value == null ? "pe.poo.plugins." + fallback : value;
    }

    public static DifficultyConfig load(Path override) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        if (Files.exists(override)) {
            try (var input = Files.newInputStream(override)) {
                return mapper.readValue(input, DifficultyConfig.class);
            }
        }
        try (var input = DifficultyConfig.class.getResourceAsStream("/difficulty.json")) {
            if (input == null) throw new IOException("Falta difficulty.json");
            return mapper.readValue(input, DifficultyConfig.class);
        }
    }
}
