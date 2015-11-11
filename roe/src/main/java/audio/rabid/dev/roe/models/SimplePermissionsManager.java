//package audio.rabid.dev.roe.models;
//
//import java.util.Arrays;
//
///**
// * Created by charles on 11/2/15.
// */
//public class SimplePermissionsManager<T extends Resource> extends PermissionsManager<T> {
//
//    private Op[] list;
//
//    public SimplePermissionsManager(Op... allowed) {
//        list = allowed;
//    }
//
//    public boolean can(Op permission, T object) {
//        return Arrays.asList(list).contains(permission);
//    }
//
//    public SimplePermissionsManager<T> readOnly() {
//        return new SimplePermissionsManager<>(Op.READ);
//    }
//
//    public PermissionsManager<T> all() {
//        return new SimplePermissionsManager<>(Op.READ, Op.CREATE, Op.UPDATE, Op.DELETE);
//    }
//}
