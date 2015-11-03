package audio.rabid.dev.roe.models;

import audio.rabid.dev.roe.models.rails.Op;

/**
 * Created by charles on 10/25/15.
 * <p/>
 * This class is for keeping track of which CRUD operations are allowed on a particular object.
 * For a basic manager, use {@link SimplePermissionsManager}, which checks for permissions independent
 * of a particular instance. For finer control, create your own {@link PermissionsManager}.
 *
 * @see SimplePermissionsManager
 * @see Source#getPermissions()
 * @see Source.SingleSourceOperation#getPermissions()
 * @see Source.MultipleSourceOperation#getPermissions()
 */
public abstract class PermissionsManager<T> {

    public boolean canRead() {
        return can(Op.READ, null);
    }

    public boolean canCreate() {
        return can(Op.CREATE, null);
    }

    public boolean canUpdate(T object) {
        return can(Op.UPDATE, object);
    }

    public boolean canDelete(T object) {
        return can(Op.DELETE, object);
    }

    /**
     * @param permission the operation requested
     * @param object the object to act on. Will be null if the Op is {@link Op#CREATE} or {@link Op#READ}.
     * @return true if the operation is allowed
     */
    public abstract boolean can(Op permission, T object);
}
