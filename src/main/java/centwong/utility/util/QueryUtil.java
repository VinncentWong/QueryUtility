package centwong.utility.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import centwong.utility.annotation.ParamColumn;
import centwong.utility.exception.QueryException;
import centwong.utility.response.HttpResponse;
import org.springframework.data.domain.Sort;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
public class QueryUtil {

    @Setter
    @Getter
    @Builder
    private static class PgChecker {
        private Integer offset;
        private Integer limit;
    }

    @Setter
    @Getter
    @Builder
    private static class ProcessParamResult<T> {
        private CriteriaQuery<T> query;
        private PgChecker checker;
    }

    @SneakyThrows
    public static <T> TypedQuery<T> generateQuery(
            EntityManager em,

            // target class is a class that define what type of data you want to fetch
            Class<T> targetClass,
            Object param
    ) {

        log.info("accepting param: {} with targetClass: {}", param, targetClass);

        if (em == null) {
            throw new QueryException("em should not null");
        }

        // you can get CriteriaBuilder from EntityManager class
        var cb = em.getCriteriaBuilder();

        log.info("before processing param select");

        var res = processParamSelect(cb, targetClass, param);

        log.info("after processing param select");

        var limit = res.checker.getLimit();
        var offset = res.checker.getOffset();

        TypedQuery<T> typedQuery;

        if (offset != -1 && limit != -1) {
            typedQuery = em.createQuery(res.query)
                    .setFirstResult(offset)
                    .setMaxResults(limit);
        } else if(limit != -1){
            typedQuery = em.createQuery(res.query)
                    .setMaxResults(limit);
        } else {
            typedQuery = em.createQuery(res.getQuery());
        }

        return typedQuery;
    }

    @SneakyThrows
    // clazz is Param Class<?>
    private static <T> ProcessParamResult<T> processParamSelect(CriteriaBuilder cb, Class<T> targetClass, Object param) {
        var clazz = param.getClass();
        var fields = clazz.getDeclaredFields();

        var query = cb.createQuery(targetClass);
        var root = query.from(targetClass);

        query = query
                .select(root);

        var limit = -1;
        var offset = -1;

        log.info("iterating each field");
        for (var field : fields) {
            try {
                // set the access modifier become public
                field.setAccessible(true);

                var value = field.get(param);

                var type = field.getType();

                var columnName = field.getName();
                if (Collection.class.isAssignableFrom(type)) {

                    var paramColumnExist = field.isAnnotationPresent(ParamColumn.class);

                    if(!paramColumnExist){
                        throw new QueryException(String.format("column %s should has @ParamColumn to define column IN target", columnName));
                    }

                    columnName = field.getAnnotation(ParamColumn.class).name();

                    List list = null;

                    if(List.class.isAssignableFrom(type)){
                        list = (List) field.get(param);
                    }

                    // it means the type is instance of Collection
                    if(value != null){
                        query
                                .where(root.get(columnName).in(list));
                    }
                } else if (type == HttpResponse.PaginationParam.class) {

                    // skip if the value is null
                    if(value == null){
                        continue;
                    }

                    // process pg
                    var pgParam = (HttpResponse.PaginationParam) field.get(param);

                    if (pgParam.getOffset() != null && pgParam.getLimit() != null) {
                        limit = pgParam.getLimit();
                        offset = pgParam.getOffset().intValue();
                    } else if(pgParam.getLimit() != null){
                        limit = pgParam.getLimit();
                    }

                    var pgParamParam = pgParam.getParam();
                    if (pgParamParam != null) {
                        var sort = pgParamParam.getSort();
                        if(sort == null){
                            continue;
                        }

                        var orderColumnName = sort.getColumnName();
                        var position = sort.getPosition();

                        if (orderColumnName == null || position == null) {
                            throw new QueryException("make sure to define the value of the position(asc/desc) and the order column name");
                        }

                        if (
                                position
                                        .name()
                                        .equalsIgnoreCase(
                                                HttpResponse.QueryParam.Sort.SortPosition.ASC.name()
                                        )
                        ) {
                            query.orderBy(
                                    cb.asc(root.get(orderColumnName))
                            );
                        } else {
                            query.orderBy(
                                    cb.desc(root.get(orderColumnName))
                            );
                        }
                    }
                } else {

                    if(value != null){
                        query
                                .where(cb.equal(root.get(columnName), value));
                    }
                }

                log.info("field {} success with value {}", field.getName(), value);
            } catch(Exception e) {
                log.error("catch error on processing select: {}", e.getMessage());
                throw new QueryException(e.getMessage());
            } finally {
                // set the access modifier become private
                field.setAccessible(false);
            }
        }

        log.info("processing select query finished");

        return ProcessParamResult.
                <T>builder()
                .checker(
                        PgChecker
                                .builder()
                                .limit(limit)
                                .offset(offset)
                                .build()
                )
                .query(query)
                .build();
    }

    @SneakyThrows
    public static <T> TypedQuery<Long> generateQueryCount(
            EntityManager em,
            Class<T> targetClass,
            Object param
    ) {

        log.info("accepting param count: {}", param);

        if (em == null) {
            throw new QueryException("em should not null");
        }

        // you can get CriteriaBuilder from EntityManager class
        var cb = em.getCriteriaBuilder();

        var res = processParamCount(cb, targetClass, param);

        return em.createQuery(res.getQuery());
    }

    @SneakyThrows
    // clazz is Param Class<?>
    private static <T> ProcessParamResult<Long> processParamCount(CriteriaBuilder cb, Class<T> targetClass, Object param) {
        var clazz = param.getClass();
        var fields = clazz.getDeclaredFields();

        var query = cb.createQuery(Long.class);
        var root = query.from(targetClass);

        query = query
                .select(cb.count(root));

        for (var field : fields) {
            try {
                // set the access modifier become public
                field.setAccessible(true);

                var type = field.getType();

                var value = field.get(param);

                var columnName = field.getName();
                if (Collection.class.isAssignableFrom(type)) {

                    var paramColumnExist = field.isAnnotationPresent(ParamColumn.class);

                    if(!paramColumnExist){
                        throw new QueryException(String.format("column %s should has @ParamColumn to define column IN target", columnName));
                    }

                    columnName = field.getAnnotation(ParamColumn.class).name();

                    var list = (List) value;

                    // it means the type is instance of Iterable
                    if(field.get(param) != null){
                        query
                                .where(root.get(columnName).in(list));
                    }
                } else {

                    if(field.getType() == HttpResponse.PaginationParam.class){
                        continue;
                    }

                    if(field.get(param) != null){
                        query
                                .where(cb.equal(root.get(columnName), field.get(param)));
                    }
                }
            } finally {
                // set the access modifier become private
                field.setAccessible(false);
            }

            log.info("field {} on count success", field.getName());
        }

        return ProcessParamResult.
                <Long>builder()
                .query(query)
                .build();

    }

    public static Query generateReactiveQuery(Object param, boolean isCountOperation){
        if(param == null){
            throw new QueryException("param can't be null");
        }

        var limit = -1;
        var offset = -1;

        HttpResponse.QueryParam.Sort.SortPosition sortPosition = null;
        String sortColumnName = null;

        var criterias = new ArrayList<Criteria>();
        var clazz = param.getClass();
        var fields = clazz.getDeclaredFields();
        for(var field: fields){
            try{
                field.setAccessible(true);

                var value = field.get(param);

                var isColumnExist = field.isAnnotationPresent(Column.class);
                String columnName;
                if(isColumnExist){
                    columnName = field.getAnnotation(Column.class)
                            .value();
                } else {
                    columnName = field.getName();
                }

                if(Collection.class.isAssignableFrom(field.getType())){
                    // is Collection type or sub-type
                    if(value != null){
                        var list = (ArrayList) value;
                        criterias.add(
                                Criteria
                                        .where(columnName)
                                        .in(list)
                        );
                    }
                } else if(field.getType() == HttpResponse.PaginationParam.class){
                    if(value != null && !isCountOperation){
                        var pgParam = (HttpResponse.PaginationParam) value;

                        limit = pgParam.getLimit();
                        offset = pgParam.getOffset().intValue();

                        var pgParamParam = pgParam.getParam();
                        if (pgParamParam != null){
                            var sort = pgParamParam.getSort();
                            if(sort != null){
                                sortPosition = sort.getPosition();
                                columnName = sort.getColumnName();
                            }
                        }
                    }
                } else {
                    if(value != null){
                        criterias.add(
                                Criteria.where(columnName)
                                        .is(value)
                        );
                    }
                }
            } catch(Exception e){
                log.error("error on generateReactiveSelectQUery with message: {}", e.getMessage());
                throw new QueryException(e.getMessage());
            } finally{
                field.setAccessible(false);
            }
        }

        var query = Query
                .query(Criteria.from(criterias));

        if(limit != -1 && offset != -1){
            query = query
                    .limit(limit)
                    .offset(offset);
        }

        if(sortPosition != null && sortColumnName != null){
            Sort.Order order;
            switch (sortPosition){
                case ASC:
                    order = Sort.Order.asc(sortColumnName);
                    break;
                case DESC:
                    order = Sort.Order.desc(sortColumnName);
                    break;
                default:
                    order = null;
            };
            query = query
                    .sort(Sort.by(
                            order
                    ));
        }

        return query;
    }
}