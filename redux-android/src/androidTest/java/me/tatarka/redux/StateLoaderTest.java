package me.tatarka.redux;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.content.Loader;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import rx.Emitter;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Cancellable;
import rx.observers.TestSubscriber;

@RunWith(AndroidJUnit4.class)
public class StateLoaderTest {

    @Test
    public void loader_delivers_initial_state() {
        ObservableStore<String> store = new ObservableStore<>("test", Reducers.<Object, String>id());
        StateLoader<String> loader = StateLoader.create(InstrumentationRegistry.getContext(), store);
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        fromLoader(loader).subscribe(testSubscriber);
        loader.startLoading();

        testSubscriber.awaitValueCount(1, 500, TimeUnit.MILLISECONDS);
        testSubscriber.assertValue("test");
    }

    @Test
    public void loader_delivers_changed_state() {
        Reducer<Object, String> reducer = new Reducer<Object, String>() {
            @Override
            public String reduce(Object action, String state) {
                return action.toString();
            }
        };
        ObservableStore<String> store = new ObservableStore<>("test1", reducer);
        StateLoader<String> loader = StateLoader.create(InstrumentationRegistry.getContext(), store);
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        fromLoader(loader).subscribe(testSubscriber);
        loader.startLoading();
        store.dispatch("test2");

        testSubscriber.awaitValueCount(2, 500, TimeUnit.MILLISECONDS);
        testSubscriber.assertValues("test1", "test2");
    }

    @Test
    public void loader_ignores_state_changes_when_stopped() {
        Reducer<Object, String> reducer = new Reducer<Object, String>() {
            @Override
            public String reduce(Object action, String state) {
                return action.toString();
            }
        };
        ObservableStore<String> store = new ObservableStore<>("test1", reducer);
        StateLoader<String> loader = StateLoader.create(InstrumentationRegistry.getContext(), store);
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        fromLoader(loader).subscribe(testSubscriber);
        loader.startLoading();
        loader.stopLoading();
        store.dispatch("test2");

        testSubscriber.awaitValueCount(1, 500, TimeUnit.MILLISECONDS);
        testSubscriber.assertValue("test1");
    }

    @Test
    // There isn't a clean way to catch the exception thrown by the main thread. But you can remove 
    // this ignore to see what the stacktrace looks like.
    @Ignore
    public void loader_includes_dispatched_stacktrace_on_failure() throws InterruptedException {
        Reducer<Object, String> reducer = new Reducer<Object, String>() {
            @Override
            public String reduce(Object action, String state) {
                return action.toString();
            }
        };
        ObservableStore<String> store = new ObservableStore<>("test1", reducer);
        StateLoader<String> loader = StateLoader.create(InstrumentationRegistry.getContext(), store);
        loader.debug(true);
        loader.registerListener(0, new Loader.OnLoadCompleteListener<String>() {
            @Override
            public void onLoadComplete(Loader<String> loader, String data) {
                if (data.equals("test2")) {
                    throw new RuntimeException("error");
                }
            }
        });
        loader.startLoading();
        store.dispatch("test2");
    }

    private static <T> Observable<T> fromLoader(final Loader<T> loader) {
        return Observable.fromEmitter(new Action1<Emitter<T>>() {
            @Override
            public void call(final Emitter<T> emitter) {
                final Loader.OnLoadCompleteListener<T> listener = new Loader.OnLoadCompleteListener<T>() {
                    @Override
                    public void onLoadComplete(Loader<T> loader, T data) {
                        emitter.onNext(data);
                    }
                };
                emitter.setCancellation(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        loader.unregisterListener(listener);
                        loader.stopLoading();
                    }
                });
                loader.registerListener(0, listener);
            }
        }, Emitter.BackpressureMode.BUFFER);
    }
}
