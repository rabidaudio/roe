package audio.rabid.dev.roe.testobjects;

import com.alibaba.fastjson.annotation.JSONField;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import audio.rabid.dev.roe.models.NetworkRoeDao;

/**
 * Created by charles on 10/30/15.
 */
@DatabaseTable(tableName = "dummies", daoClass = NetworkRoeDao.class)
public class DummyObject {

    public DummyObject() {
    }

    public DummyObject(String name, int age, DummyChild child) {
        this.name = name;
        this.age = age;
        this.child = child;
    }

    @JSONField
    @DatabaseField(generatedId = true)
    private int id = -1;

    @JSONField
    @DatabaseField(index = true)
    private int serverId = -1;

    @JSONField
    @DatabaseField
    protected String name;

    @JSONField
    @DatabaseField
    protected int age = 0;

    @JSONField
    @DatabaseField(foreign = true)
    public DummyChild child;

    public int getId(){
        return id;
    }

    public boolean isNew(){
        return id == -1;
    }

    public int getServerId(){
        return serverId;
    }

    public boolean hasServerId(){
        return serverId != -1;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age){
        this.age = age;
    }

    public DummyChild getChild() {
        return child;
    }
}
