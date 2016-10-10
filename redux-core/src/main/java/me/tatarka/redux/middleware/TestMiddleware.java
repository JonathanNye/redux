package me.tatarka.redux.middleware;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.tatarka.redux.Store;

/**
 * A middleware that records all actions and states, allowing you to easily check them in tests.
 *
 * @param <A> the action type.
 * @param <S> the state type.
 */
public class TestMiddleware<A, S> implements Middleware<A, S> {

    private Store<A, S> store;
    private List<A> actions = new ArrayList<>();
    private List<S> states = new ArrayList<>();

    @Override
    public void create(final Store<A, S> store) {
        this.store = store;
        states.add(store.state());
    }

    @Override
    public void dispatch(Next<A> next, A action) {
        actions.add(action);
        next.next(action);
        states.add(store.state());
    }

    /**
     * Returns all actions that have been dispatched.
     */
    public List<A> actions() {
        return Collections.unmodifiableList(actions);
    }

    /**
     * Returns all states. The first state is before any action has been dispatched. Each
     * subsequent
     * state is the state after each action.
     */
    public List<S> states() {
        return Collections.unmodifiableList(states);
    }

}
