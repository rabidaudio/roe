package audio.rabid.roe.abstractquery;

/**
 * Created by  charles  on 3/30/16.
 */
public interface IWhereBuilder {

    WhereBuilder and(IWhereBuilder otherWhere);

    WhereBuilder or(IWhereBuilder otherWhere);

    WhereBuilder eq(String column, Object value);

    WhereBuilder lt(String column, Object value);

    WhereBuilder lte(String column, Object value);

    WhereBuilder gt(String column, Object value);

    WhereBuilder gte(String column, Object value);

    WhereBuilder like(String column, String value);

    WhereBuilder contains(String column, String value);
}
