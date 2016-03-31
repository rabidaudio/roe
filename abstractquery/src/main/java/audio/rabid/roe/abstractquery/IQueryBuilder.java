package audio.rabid.roe.abstractquery;

import android.os.Bundle;

/**
 * Created by  charles  on 3/30/16.
 */
public interface IQueryBuilder {

    QueryBuilder where(WhereBuilder where);

    QueryBuilder orderAsc(String column);

    QueryBuilder orderDesc(String column);

    QueryBuilder limit(int max);

    QueryBuilder limit(int min, int max);

    String buildSQLClause();

    String buildHTTPQueryString();
}
