package audio.rabid.roe.abstractquery;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.util.Size;
import android.util.SizeF;
import android.util.SparseArray;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by  charles  on 3/31/16.
 *
 * Helper for creating Bundles in a more concise way
 */
public class BundleBuilder {

    private Bundle bundle = new Bundle();

    public BundleBuilder putBoolean(@Nullable String key, boolean value) {
        bundle.putBoolean(key, value);
        return this;
    }

    public BundleBuilder putInt(@Nullable String key, int value) {
        bundle.putInt(key, value);
        return this;
    }

    public BundleBuilder putLong(@Nullable String key, long value) {
        bundle.putLong(key, value);
        return this;
    }

    public BundleBuilder putDouble(@Nullable String key, double value) {
        bundle.putDouble(key, value);
        return this;
    }

    public BundleBuilder putString(@Nullable String key, @Nullable String value) {
        bundle.putString(key, value);
        return this;
    }

    public BundleBuilder putBooleanArray(@Nullable String key, @Nullable boolean[] value) {
        bundle.putBooleanArray(key, value);
        return this;
    }

    public BundleBuilder putIntArray(@Nullable String key, @Nullable int[] value) {
        bundle.putIntArray(key, value);
        return this;
    }

    public BundleBuilder putLongArray(@Nullable String key, @Nullable long[] value) {
        bundle.putLongArray(key, value);
        return this;
    }

    public BundleBuilder putDoubleArray(@Nullable String key, @Nullable double[] value) {
        bundle.putDoubleArray(key, value);
        return this;
    }

    public BundleBuilder putStringArray(@Nullable String key, @Nullable String[] value) {
        bundle.putStringArray(key, value);
        return this;
    }

    public BundleBuilder putAll(Bundle bundle) {
        bundle.putAll(bundle);
        return this;
    }

    public BundleBuilder putByte(@Nullable String key, byte value) {
        bundle.putByte(key, value);
        return this;
    }

    public BundleBuilder putChar(@Nullable String key, char value) {
        bundle.putChar(key, value);
        return this;
    }

    public BundleBuilder putShort(@Nullable String key, short value) {
        bundle.putShort(key, value);
        return this;
    }

    public BundleBuilder putFloat(@Nullable String key, float value) {
        bundle.putFloat(key, value);
        return this;
    }

    public BundleBuilder putCharSequence(@Nullable String key, @Nullable CharSequence value) {
        bundle.putCharSequence(key, value);
        return this;
    }

    public BundleBuilder putParcelable(@Nullable String key, @Nullable Parcelable value) {
        bundle.putParcelable(key, value);
        return this;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BundleBuilder putSize(@Nullable String key, @Nullable Size value) {
        bundle.putSize(key, value);
        return this;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BundleBuilder putSizeF(@Nullable String key, @Nullable SizeF value) {
        bundle.putSizeF(key, value);
        return this;
    }

    public BundleBuilder putParcelableArray(@Nullable String key, @Nullable Parcelable[] value) {
        bundle.putParcelableArray(key, value);
        return this;
    }

    public BundleBuilder putParcelableArrayList(@Nullable String key, @Nullable ArrayList<? extends Parcelable> value) {
        bundle.putParcelableArrayList(key, value);
        return this;
    }

    public BundleBuilder putSparseParcelableArray(@Nullable String key, @Nullable SparseArray<? extends Parcelable> value) {
        bundle.putSparseParcelableArray(key, value);
        return this;
    }

    public BundleBuilder putIntegerArrayList(@Nullable String key, @Nullable ArrayList<Integer> value) {
        bundle.putIntegerArrayList(key, value);
        return this;
    }

    public BundleBuilder putStringArrayList(@Nullable String key, @Nullable ArrayList<String> value) {
        bundle.putStringArrayList(key, value);
        return this;
    }

    public BundleBuilder putCharSequenceArrayList(@Nullable String key, @Nullable ArrayList<CharSequence> value) {
        bundle.putCharSequenceArrayList(key, value);
        return this;
    }

    public BundleBuilder putSerializable(@Nullable String key, @Nullable Serializable value) {
        bundle.putSerializable(key, value);
        return this;
    }

    public BundleBuilder putByteArray(@Nullable String key, @Nullable byte[] value) {
        bundle.putByteArray(key, value);
        return this;
    }

    public BundleBuilder putShortArray(@Nullable String key, @Nullable short[] value) {
        bundle.putShortArray(key, value);
        return this;
    }

    public BundleBuilder putCharArray(@Nullable String key, @Nullable char[] value) {
        bundle.putCharArray(key, value);
        return this;
    }

    public BundleBuilder putFloatArray(@Nullable String key, @Nullable float[] value) {
        bundle.putFloatArray(key, value);
        return this;
    }

    public BundleBuilder putCharSequenceArray(@Nullable String key, @Nullable CharSequence[] value) {
        bundle.putCharSequenceArray(key, value);
        return this;
    }

    public BundleBuilder putBundle(@Nullable String key, @Nullable Bundle value) {
        bundle.putBundle(key, value);
        return this;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public BundleBuilder putBinder(@Nullable String key, @Nullable IBinder value) {
        bundle.putBinder(key, value);
        return this;
    }

    public Bundle build(){
        return bundle;
    }
}
