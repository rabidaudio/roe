//package audio.rabid.dev.roe.testobjects;
//
//import java.sql.SQLException;
//
//import audio.rabid.dev.roe.models.NetworkSource;
//import audio.rabid.dev.roe.models.SimplePermissionsManager;
//
///**
// * Created by charles on 11/5/15.
// */
//public class DummyObjectSource extends NetworkSource<DummyObject, Integer, Integer> {
//
//    private static DummyObjectSource instance;
//    public static DummyObjectSource getInstance(){
//        if(instance == null){
//            try{
//                instance = new DummyObjectSource();
//            }catch (SQLException e){
//                throw new RuntimeException(e);
//            }
//        }
//        return instance;
//    }
//
//    private DummyObjectSource() throws SQLException {
//        super(DummyObjectMockServer.getInstance(), GenericDatabase.getInstance(), DummyObject.class,
//                null, new SimplePermissionsManager<DummyObject>().all(), null);
//    }
//
//    public void clearCache(){
//        getResourceCache().clear();
//    }
//
//    private boolean updateCompleted = false;
//
//    @Override
//    protected void onAfterUpdated(DummyObject updated){
//        super.onAfterUpdated(updated);
//
//        //dirty hack to detect when background thread completes
//        Thread[] allthreads = new Thread[15];
//        int threadCount = Thread.enumerate(allthreads);
//        for (int i = 0; i < threadCount; i++) {
//            final Thread t = allthreads[i];
//            if (t.getName().equals("NetworkUpdate:" + updated.getClass().getName() + ":" + updated.hashCode())) {
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            t.join(0);
//                            updateCompleted = true;
//                        } catch (InterruptedException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
//                }).start();
//                return;
//            }
//        }
//        //if we get here, thread must have already finished
//        updateCompleted = true;
//    }
//
//    public boolean wasUpdateCompleted(){
//        return updateCompleted;
//    }
//
//    public void clearUpdateCompleted(){
//        updateCompleted = false;
//    }
//
//    private boolean createCompleted = false;
//
//    @Override
//    protected void onAfterCreated(DummyObject created){
//        super.onAfterCreated(created);
//
//        //dirty hack to detect when background thread completes
//        Thread[] allthreads = new Thread[15];
//        int threadCount = Thread.enumerate(allthreads);
//        for (int i = 0; i < threadCount; i++) {
//            final Thread t = allthreads[i];
//            if (t.getName().equals("NetworkCreate:" + created.getClass().getName() + ":" + created.hashCode())) {
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            t.join();
//                            createCompleted = true;
//                        } catch (InterruptedException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
//                }).start();
//                return;
//            }
//        }
//        //if we get here, thread must have already finished
//        createCompleted = true;
//    }
//
//    public boolean wasCreateCompleted(){
//        return createCompleted;
//    }
//
//    public void clearCreateCompleted(){
//        createCompleted = false;
//    }
//
//    private boolean deleteCompleted = false;
//
//    @Override
//    protected void onAfterDeleted(DummyObject deleted){
//        super.onAfterDeleted(deleted);
//
//        //dirty hack to detect when background thread completes
//        Thread[] allthreads = new Thread[15];
//        int threadCount = Thread.enumerate(allthreads);
//        for (int i = 0; i < threadCount; i++) {
//            final Thread t = allthreads[i];
//            if (t.getName().equals("NetworkDelete:" + deleted.getClass().getName() + ":" + deleted.hashCode())) {
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            t.join(0);
//                            deleteCompleted = true;
//                        } catch (InterruptedException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
//                }).start();
//                return;
//            }
//        }
//        //if we get here, thread must have already finished
//        deleteCompleted = true;
//    }
//
//    public boolean wasDeleteCompleted(){
//        return deleteCompleted;
//    }
//
//    public void clearDeleteCompleted(){
//        deleteCompleted = false;
//    }
//}
