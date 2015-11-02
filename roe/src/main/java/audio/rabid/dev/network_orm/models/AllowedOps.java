package audio.rabid.dev.network_orm.models;

import java.util.Arrays;

/**
 * Created by charles on 10/25/15.
 * <p/>
 * TODO allow for instance-specific permissions
 */
public class AllowedOps {

    public enum Op {
        READ,
        CREATE,
        UPDATE,
        DELETE
    }

    private Op[] list;

    public AllowedOps(Op... allowed) {
        list = allowed;
    }

    public boolean canRead() {
        return can(Op.READ);
    }

    public boolean canCreate() {
        return can(Op.CREATE);
    }

    public boolean canUpdate() {
        return can(Op.UPDATE);
    }

    public boolean canDelete() {
        return can(Op.DELETE);
    }

    public boolean can(Op permission) {
        return Arrays.asList(list).contains(permission);
    }

    public static AllowedOps READ_ONLY = new AllowedOps(Op.READ);

    public static AllowedOps ALL = new AllowedOps(Op.READ, Op.CREATE, Op.UPDATE, Op.DELETE);
}
