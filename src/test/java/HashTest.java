import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class HashTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder e = new BCryptPasswordEncoder();
        System.out.println("COINCIDE ADMIN: " + e.matches("admin", "$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2"));
        System.out.println("NUEVO HASH: " + e.encode("admin"));
    }
}
