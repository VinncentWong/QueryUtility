package centwong.utility.jwt;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@Builder
@ToString
public class JwtToken {

    private Object id;

    private String role;
}
