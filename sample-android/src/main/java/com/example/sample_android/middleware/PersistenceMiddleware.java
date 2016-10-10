package com.example.sample_android.middleware;

import com.example.sample_android.Datastore;
import com.example.sample_android.state.TodoList;

import me.tatarka.redux.Store;
import me.tatarka.redux.middleware.Middleware;

public class PersistenceMiddleware implements Middleware<Object, TodoList> {

    private final Datastore datastore;
    private Store<Object, TodoList> store;

    public PersistenceMiddleware(Datastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public void create(final Store<Object, TodoList> store) {
        this.store = store;
    }

    @Override
    public void dispatch(Next<Object> next, Object action) {
        next.next(action);
        datastore.store(store.state().items());
    }
}
