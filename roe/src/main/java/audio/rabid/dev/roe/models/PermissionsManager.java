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
 */
public abstract class PermissionsManager<T extends Resource> {

    public boolean canRead(T object) {
        return can(Op.READ, object);
    }

    public boolean canCreate(T object) {
        return can(Op.CREATE, object);
    }

    public boolean canUpdate(T object) {
        return can(Op.UPDATE, object);
    }

    public boolean canDelete(T object) {
        return can(Op.DELETE, object);
    }

    public void checkPermission(Op permission, T object) throws PermissionDeniedException {
        if(!can(permission, object))
            throw new PermissionDeniedException(permission, object);
    }

    /**
     * @param permission the operation requested
     * @param object the object to act on. Will be null if the Op is {@link Op#CREATE} or {@link Op#READ}.
     * @return true if the operation is allowed
     */
    public abstract boolean can(Op permission, T object);

    public static class PermissionDeniedException extends IllegalStateException {
        private PermissionDeniedException(Op op, Resource resource){
            super("Permission "+op.name()+" denied to "+resource.toString());
        }
    }
}
