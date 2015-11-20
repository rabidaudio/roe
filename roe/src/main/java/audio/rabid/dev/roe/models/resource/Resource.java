package audio.rabid.dev.roe.models.resource;

import audio.rabid.dev.roe.models.NetworkSyncableDao;
import audio.rabid.dev.roe.models.json.JSONifyable;

/**
 * Created by charles on 11/11/15.
 *
 * An item stored both locally and remotely, for use with {@link NetworkSyncableDao}.
 */
public interface Resource<LK, SK> extends JSONifyable {

    SK getServerId();

    boolean hasServerId();
}
