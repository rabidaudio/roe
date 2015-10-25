package audio.rabid.dev.network_orm;

/**
 * Created by charles on 10/25/15.
 */
public class AllowedOps {

    public enum Op {
        CREATE,
        UPDATE,
        DELETE
    }

    private boolean create, update, delete = false;

    public AllowedOps(Op... allowed){
        for(Op o : allowed){
            switch (o){
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

    public boolean canCreate(){
        return create;
    }

    public boolean canUpdate(){
        return update;
    }

    public boolean canDelete(){
        return delete;
    }

    public static AllowedOps READ_ONLY = new AllowedOps();

    public static AllowedOps ALL = new AllowedOps(Op.CREATE, Op.UPDATE, Op.DELETE);
}
