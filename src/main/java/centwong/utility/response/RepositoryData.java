package centwong.utility.response;

import lombok.*;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RepositoryData<T>{
    private T data;
    private HttpResponse.Pagination pg;
}
