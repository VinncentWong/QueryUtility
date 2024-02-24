package centwong.utility.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Slf4j
public class BcryptUtil {

    private static final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    public static String encode(String rawPassword){
        return bcrypt.encode(rawPassword);
    }

    public static boolean isMatch(String rawPassword, String encodedPassword){
        log.info("comparing: {} with {}", rawPassword, encodedPassword);
        return bcrypt.matches(rawPassword, encodedPassword);
    }
}
