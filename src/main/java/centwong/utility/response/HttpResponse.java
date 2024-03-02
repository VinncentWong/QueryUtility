package centwong.utility.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import centwong.utility.constant.ContextConstant;

import reactor.util.context.Context;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Builder(access = AccessLevel.PRIVATE)
@Setter
@Getter
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class HttpResponse {

    private HttpMessage metadata;

    private Object data;

    private Pagination pagination;

    @Builder
    @Setter
    @Getter
    @ToString
    static class HttpMessage{
        private String title;
        private String message;
        private Boolean success;
        private Meta meta;
    }

    @Builder
    @Setter
    @Getter
    @ToString
    static class Meta{
        private String path;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private LocalDateTime timestamp;
        private Long status;
        private String timeElapsed;
        private Object data;
    }

    @Builder
    @Setter
    @Getter
    @ToString
    public static class Pagination{
        private Long currentPage;
        private Long currentElements;
        private Long totalPage;
        private Long totalElements;
    }

    @Builder
    @Setter
    @Getter
    @ToString
    public static class PaginationParam{
        private Long offset;
        private Integer limit;
        private QueryParam param;
    }

    @Builder
    @Setter
    @Getter
    @ToString
    public static class QueryParam{

        private Sort sort;

        @Builder
        @Setter
        @Getter
        public static class Sort{
            private String columnName;
            private SortPosition position;

            public enum SortPosition{
                ASC, DESC
            }
        }
    }

    public static HttpResponse sendSuccessResponse(
            Context context,
            HttpStatus statusCode,
            String message,
            Object data,
            Pagination pagination,
            Object metadata
    ){
        var initialTime = context.<LocalDateTime>get(ContextConstant.TIME_START);
        var now = LocalDateTime.now();

        log.info("initialTime: {}, now: {}", initialTime, now);

        var differenceMinute = ChronoUnit.MINUTES.between(now, initialTime);
        var differenceSecond = differenceMinute != 0 ? differenceMinute % 60 : ChronoUnit.SECONDS.between(now, initialTime);

        log.info("differenceMinute: {}, differenceSecond: {}", differenceMinute, differenceSecond);

        return HttpResponse
                .builder()
                .metadata(
                        HttpMessage
                                .builder()
                                .title("Request berhasil")
                                .message(message)
                                .meta(
                                        Meta
                                                .builder()
                                                .path(
                                                        context.get(ContextConstant.REQUEST_PATH)
                                                )
                                                .data(metadata)
                                                .status((long)statusCode.value())
                                                .timestamp(LocalDateTime.now())
                                                .timeElapsed(String.format("%dm %ds", differenceMinute, differenceSecond))
                                                .build()
                                )
                                .success(true)
                                .build()
                )
                .data(data)
                .pagination(pagination)
                .build();
    }

    public static HttpResponse sendErrorResponse(
            String message,
            Boolean success
    ){
        return HttpResponse
                .builder()
                .metadata(
                        HttpMessage
                                .builder()
                                .title("Terjadi kesalahan pada saat mengolah request")
                                .message(message)
                                .success(success)
                                .build()
                )
                .build();
    }
}
