package audio.rabid.roe.abstractquery;

import android.os.Bundle;

/**
 * Created by  charles  on 3/30/16.
 */
public class QueryBuilder implements IQueryBuilder {

    @Override
    public QueryBuilder where(WhereBuilder where) {
        return null;
    }

    @Override
    public QueryBuilder orderAsc(String column) {
        return null;
    }

    @Override
    public QueryBuilder orderDesc(String column) {
        return null;
    }

    @Override
    public QueryBuilder limit(int max) {
        return null;
    }

    @Override
    public QueryBuilder limit(int min, int max) {
        return null;
    }

    @Override
    public String buildSQLClause() {
        return null;
    }

    @Override
    public String buildHTTPQueryString() {
        return null;
    }
}
