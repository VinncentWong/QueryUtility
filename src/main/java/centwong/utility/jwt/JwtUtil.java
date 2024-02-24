package centwong.utility.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import centwong.utility.constant.JwtConstant;

import java.util.Base64;

@Slf4j
public class JwtUtil {

    public static String generateJwtToken(String secretToken, String role, Object id){
        return Jwts
                .builder()
                .claim(JwtConstant.ID, id)
                .claim(JwtConstant.ROLE, role)
                .signWith(SignatureAlgorithm.HS256, Base64.getEncoder().encode(secretToken.getBytes()))
                .compact();
    }

    public static JwtToken getTokenData(String secretToken, String token){
        try{
            var claims = Jwts
                    .parserBuilder()
                    .setSigningKey(Base64.getEncoder().encode(secretToken.getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            var jwtToken = JwtToken
                    .builder();
            jwtToken.id(claims.get(JwtConstant.ID));
            var role = claims.get(JwtConstant.ROLE);
            if(role != null){
                jwtToken.role((String)role);
            }
            return jwtToken.build();
        } catch(Exception e){
            log.error("error on JwtUtil: {}", e.getMessage());
            throw e;
        }
    }
}
