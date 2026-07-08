
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashTest {
    @Test
    void testPasswordMatches() {
        BCryptPasswordEncoder e = new BCryptPasswordEncoder();
        String hash = e.encode("admin");
        assertTrue(e.matches("admin", hash));
    }
}
