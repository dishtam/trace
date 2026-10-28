package trace.demoapp;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Activation for seeded evaluation bugs (S0-6). Each bug Bxx is active only
 * under the Spring profile named after it in lower case (e.g. b04); the
 * default profile is always the clean app, so fixtures never interfere.
 * Ground truth per bug: eval/fixtures/<id>/fixture.md.
 */
@Component
public class SeededBugs {
    private final Environment environment;

    public SeededBugs(Environment environment) { this.environment = environment; }

    public boolean active(String bugId) {
        return environment.acceptsProfiles(Profiles.of(bugId.toLowerCase()));
    }
}
