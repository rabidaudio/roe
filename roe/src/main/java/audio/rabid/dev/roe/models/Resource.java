package audio.rabid.dev.roe.models;

import com.j256.ormlite.field.DatabaseField;

import org.json.JSONObject;

/**
 * Created by charles on 10/23/15.
 *
 * The {@link Resource} is your base class that all of your models should subclass (passing their class
 * in for the generic <code>R</code>). It gives you a couple of default fields, including local and
 * remote public keys. It also gives you built-in ways to convert your models to and from {@link JSONObject}s.
 *
 * For each of your custom fields, use the {@link DatabaseField} annotation so ORMLite knows which to
 * store in the database. Also use the {@link JSONField} annotation to help roe convert your models
 * to JSON. You can also override {@link #toJSON()} and {@link #updateFromJSON(JSONObject)} if you
 * need finer control over the conversion (or for performance reasons).
 *
 * Because a single record is shared as a single instance (possibly to multiple threads), be sure to
 * synchronize where necessary.
 */
public interface Resource<LK> {

    LK getId();

    String localIdToString();

    /**
     * Returns true if the item has not yet been saved locally.
     */
    boolean isNew();

    /**
     * The {@link Source} is the way you manipulate {@link Resource}s. Use the source to query for
     * instances and do CRUD operations. The {@link Source} will handle all of the caching, updating
     * of database and network, etc. Since they are somewhat heavy, you should make a static instance
     * and use it across all of your {@link Resource} instances. You can either use the default
     * {@link Source} class or subclass it to add your own query methods.
     *
     * @see Source
     */
    Source<? extends Resource<LK>, LK> getSource();
}
