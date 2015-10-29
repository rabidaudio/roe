package audio.rabid.dev.network_orm;

/**
 * Created by charles on 10/25/15.
 */
public class AllowedOps {

    public enum Op {
        READ,
        CREATE,
        UPDATE,
        DELETE
    }

    private boolean read, create, update, delete = false;

    public AllowedOps(Op... allowed){
        for(Op o : allowed){
            switch (o){
                case READ:
                    read = true;
                    break;
                case CREATE:
                    create = true;
                    break;
                case UPDATE:
                    update = true;
                    break;
                case DELETE:
                    delete = true;
                    break;
            }
        }
    }

    public boolean canRead(){
        return read;
    }

    public boolean canCreate(){
        return create;
    }

    public boolean canUpdate(){
        return update;
    }

    public boolean canDelete(){
        return delete;
    }

    public boolean can(Op permission){
        switch (permission){
            case READ:
                return read;
            case CREATE:
                return create;
            case UPDATE:
                return update;
            case DELETE:
                return delete;
            default:
                return false;
        }
    }

    public static AllowedOps READ_ONLY = new AllowedOps(Op.READ);

    public static AllowedOps ALL = new AllowedOps(Op.READ, Op.CREATE, Op.UPDATE, Op.DELETE);
}
