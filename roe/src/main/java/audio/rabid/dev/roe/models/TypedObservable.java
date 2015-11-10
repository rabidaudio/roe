/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package audio.rabid.dev.roe.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Observer;

/**
 * Observable is used to notify a group of Observer objects when a change
 * occurs. On creation, the set of observers is empty. After a change occurred,
 * the application can call the {@link #notifyObservers()} method. This will
 * cause the invocation of the {@code update()} method of all registered
 * Observers. The order of invocation is not specified. This implementation will
 * call the Observers in the order they registered. Subclasses are completely
 * free in what order they call the update methods.
 *
 * @see Observer
 */
public class TypedObservable<T> {

    List<TypedObserver<T>> observers = new ArrayList<>();

    boolean changed = false;

    /**
     * Constructs a new {@code Observable} object.
     */
    public TypedObservable() {
    }

    /**
     * Adds the specified observer to the list of observers. If it is already
     * registered, it is not added a second time.
     *
     * @param observer the Observer to add.
     */
    public void addObserver(TypedObserver<T> observer) {
        if (observer == null) {
            throw new NullPointerException("observer == null");
        }
        synchronized (this) {
            if (!observers.contains(observer))
                observers.add(observer);
        }
    }

    /**
     * Clears the changed flag for this {@code Observable}. After calling
     * {@code clearChanged()}, {@code hasChanged()} will return {@code false}.
     */
    protected void clearChanged() {
        changed = false;
    }

    /**
     * Returns the number of observers registered to this {@code Observable}.
     *
     * @return the number of observers.
     */
    public int countObservers() {
        return observers.size();
    }

    /**
     * Removes the specified observer from the list of observers. Passing null
     * won't do anything.
     *
     * @param observer the observer to remove.
     */
    public synchronized void deleteObserver(TypedObserver<T> observer) {
        observers.remove(observer);
    }

    /**
     * Removes all observers from the list of observers.
     */
    public synchronized void deleteObservers() {
        observers.clear();
    }

    /**
     * Returns the changed flag for this {@code Observable}.
     *
     * @return {@code true} when the changed flag for this {@code Observable} is
     * set, {@code false} otherwise.
     */
    public boolean hasChanged() {
        return changed;
    }

    /**
     * If {@code hasChanged()} returns {@code true}, calls the {@code update()}
     * method for every Observer in the list of observers using the specified
     * argument. Afterwards calls {@code clearChanged()}..
     */
    @SuppressWarnings("unchecked")
    public void notifyObservers(T item, boolean deleted) {
        int size = 0;
        TypedObserver<T>[] arrays = null;
        synchronized (this) {
            if (hasChanged()) {
                clearChanged();
                size = observers.size();
                arrays = new TypedObserver[size];
                observers.toArray(arrays);
            }
        }
        if (arrays != null) {
            for (TypedObserver<T> observer : arrays) {
                observer.update(item, deleted);
            }
        }
    }

    /**
     * Sets the changed flag for this {@code Observable}. After calling
     * {@code setChanged()}, {@code hasChanged()} will return {@code true}.
     */
    protected void setChanged() {
        changed = true;
    }
}
