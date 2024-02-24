package centwong.utility.response;

import lombok.*;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ServiceData<T>{
    private T data;
    private HttpResponse.Pagination pg;
    private Object metadata;
}
