package audio.rabid.dev.network_orm.models;

import java.util.Arrays;

/**
 * Created by charles on 11/2/15.
 */
public class SimplePermissionsManager<T> extends PermissionsManager<T> {

    private Op[] list;

    public SimplePermissionsManager(Op... allowed) {
        list = allowed;
    }

    public boolean can(Op permission, T object) {
        return Arrays.asList(list).contains(permission);
    }

    public static SimplePermissionsManager READ_ONLY = new SimplePermissionsManager(Op.READ);

    public static SimplePermissionsManager ALL = new SimplePermissionsManager(
            Op.READ, Op.CREATE, Op.UPDATE, Op.DELETE);

}
