
# roe.

> A More MVC-like Android framework

[![Build Status](https://travis-ci.org/rabidaudio/roe.svg?branch=master)](https://travis-ci.org/rabidaudio/roe)

----

Powerful model system built on ORMLite with automatic handling of network APIs

Activities are your controllers. They can manipulate simple views directly, but for more complicated views,
use ViewHolder. 


## TODO

- document methods
- document usages
- determine how to handle delete without network
- determine how to handle associations
- make API more obvious and idiotproof (annotations are cool)
- return from database before updating with server data for performance (this should be fine thanks to the observer pattern)
- separate factory into ResourceFactory and something to map server json to resource json (server?)
- allow custom date formats
- better way to create Sources (no unchecked, way to pass in programatically)
- 0verhaul permissions crap
- support ids that aren't integers
- allow for resorces without network apis

in sample:
- how to deal with syncing authors (related to associations issue)

Creating new threads seems to be about 15% faster than a single background looper thread when crunching a whole bunch of inserts. (`52220.965 ms` compared to `61390.46 ms`)
That may not be the most realistic test though. Also a single background thread greatly reduces the chance of simultaneous changes 


## Docs

Start by creating a Database for ORMLite. See [ORMLite docs](). This is where you can handle migrations to the local database.

```java
public class MyDatabase extends OrmLiteSqliteOpenHelper {


    private static final int VERSION = 1;

    private static MyDatabase instance;

    public static MyDatabase getInstance() {
        if (instance == null)
            throw new RuntimeException("Need to create an instance with context first");
        return instance;
    }

    public static MyDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new MyDatabase(context);
        }
        return instance;
    }

    private MyDatabase(Context context) {
        super(context, "mydatabase.db", null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            //TODO make tables
        } catch (SQLException e) {
            throw new RuntimeException("Problem creating database", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            switch (oldVersion) {
                case 1: //--> 2
                  // TODO migration
                case 2: //--> 3
                  //TODO migration
            }
        } catch (SQLException e) {
            throw new RuntimeException("Problem migrating database", e);
        }
    }
}
```

Then make your models. These should extend `Resource`. This gives you built-in fields:

    int id - local auto-incrementing primary key
    int serverId - the primary key of the item on the server
    boolean synced - this will be set to false if a change was made locally but failed to be saved to the network
    Date createdAt - the (local) create timestamp (independant of server create timestamp)
    Date updatedAt - the (local) update timestamp (independant of server update timestamp)

Use `@DatabaseTable` and `@DatabaseField` annotations as appropriate. Also override the 

```java
@DatabaseTable(tableName = "my_models")
public class MyModel extends Resource<MyModel> {
    
    ...

    @Override
    protected synchronized boolean updateFromJSON(JSONObject data) throws JSONException {
        boolean updated = super.updateFromJSON(data);
        ... update values ...
        return updated;
    }
}
```


```
    static {
        try {
            final Dao<Post, ?> adao = Database.getInstance().getDao(Post.class);
            adao.callBatchTasks(new Callable<Post>() {
                @Override
                public Post call() throws Exception {
                    for (Post a : adao.queryForAll()) {
                        a.synced = false;
                        a.serverId = null;
                        adao.update(a);
                    }
                    return null;
                }
            });
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
```