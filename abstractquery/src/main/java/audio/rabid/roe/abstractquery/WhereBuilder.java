package audio.rabid.roe.abstractquery;

/**
 * Created by  charles  on 3/30/16.
 */
public class WhereBuilder implements IWhereBuilder {
    @Override
    public WhereBuilder and(IWhereBuilder otherWhere) {
        return null;
    }

    @Override
    public WhereBuilder or(IWhereBuilder otherWhere) {
        return null;
    }

    @Override
    public WhereBuilder eq(String column, Object value) {
        return null;
    }

    @Override
    public WhereBuilder lt(String column, Object value) {
        return null;
    }

    @Override
    public WhereBuilder lte(String column, Object value) {
        return null;
    }

    @Override
    public WhereBuilder gt(String column, Object value) {
        return null;
    }

    @Override
    public WhereBuilder gte(String column, Object value) {
        return null;
    }

    @Override
    public WhereBuilder like(String column, String value) {
        return null;
    }

    @Override
    public WhereBuilder contains(String column, String value) {
        return null;
    }
}
