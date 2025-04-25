package com.vladimir.ultra;


import com.vladimir.ultra.interfaces.Disposable;
import com.vladimir.ultra.interfaces.Observer;
import com.vladimir.ultra.interfaces.Scheduler;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ObservableTest {

    @Test
    void observableShouldEmitValues() {
        Observer<Integer> observer = mock(Observer.class);

        Observable<Integer> observable = Observable.create(obs -> {
            obs.onNext(1);
            obs.onNext(2);
            obs.onComplete();
        });

        observable.subscribe(observer);

        verify(observer).onNext(1);
        verify(observer).onNext(2);
        verify(observer).onComplete();
        verify(observer, never()).onError(any());
    }

    @Test
    void mapShouldTransformValues() {
        Observer<String> observer = mock(Observer.class);

        Observable<Integer> observable = Observable.create(obs -> {
            obs.onNext(5);
            obs.onComplete();
        });

        observable
                .map(value -> "Number: " + value)
                .subscribe(observer);

        verify(observer).onNext("Number: 5");
        verify(observer).onComplete();
    }

    @Test
    void filterShouldEmitOnlyMatchingValues() {
        Observer<Integer> observer = mock(Observer.class);

        Observable<Integer> observable = Observable.create(obs -> {
            obs.onNext(1);
            obs.onNext(2);
            obs.onNext(3);
            obs.onComplete();
        });

        observable
                .filter(value -> value % 2 == 1)
                .subscribe(observer);

        verify(observer).onNext(1);
        verify(observer).onNext(3);
        verify(observer, times(2)).onNext(anyInt());
        verify(observer).onComplete();
    }

    @Test
    void limitShouldEmitOnlyLimitedNumberOfItems() {
        Observer<Integer> observer = mock(Observer.class);

        Observable<Integer> observable = Observable.create(obs -> {
            for (int i = 1; i <= 10; i++) {
                obs.onNext(i);
            }
            obs.onComplete();
        });

        observable
                .limit(3)
                .subscribe(observer);

        verify(observer, times(3)).onNext(anyInt());
        verify(observer).onNext(1);
        verify(observer).onNext(2);
        verify(observer).onNext(3);
        verify(observer).onComplete();
    }

    @Test
    void flatMapShouldFlattenObservables() {
        Observer<Object> observer = mock(Observer.class);

        Observable<Integer> observable = Observable.create(obs -> {
            obs.onNext(1);
            obs.onNext(2);
            obs.onComplete();
        });

        observable
                .flatMap(value -> Observable.create(innerObs -> {
                    innerObs.onNext("Value: " + value);
                    innerObs.onComplete();
                }))
                .subscribe(observer);

        verify(observer).onNext("Value: 1");
        verify(observer).onNext("Value: 2");
        verify(observer, times(2)).onNext(anyString());
        verify(observer).onComplete();
    }

    @Test
    void observableShouldHandleErrors() {
        Observer<Integer> observer = mock(Observer.class);
        Throwable throwable = new RuntimeException("Test Error");

        Observable<Integer> observable = Observable.create(obs -> {
            obs.onNext(1);
            obs.onError(throwable);
        });

        observable.subscribe(observer);

        verify(observer).onNext(1);
        verify(observer).onError(throwable);
        verify(observer, never()).onComplete();
    }

    @Test
    void subscribeOnShouldRunSubscriptionOnScheduler() throws InterruptedException {
        Scheduler scheduler = mock(Scheduler.class);
        CountDownLatch latch = new CountDownLatch(1);

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            new Thread(() -> {
                task.run();
                latch.countDown();
            }).start();
            return null;
        }).when(scheduler).execute(any(Runnable.class));

        Observer<Integer> observer = mock(Observer.class);

        Observable<Integer> observable = Observable.create(obs -> {
            obs.onNext(42);
            obs.onComplete();
        });

        observable.subscribeOn(scheduler).subscribe(observer);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        verify(observer).onNext(42);
        verify(observer).onComplete();
        verify(scheduler).execute(any(Runnable.class));
    }

    @Test
    void observeOnShouldEmitItemsOnScheduler() throws InterruptedException {
        Scheduler scheduler = mock(Scheduler.class);
        CountDownLatch latch = new CountDownLatch(2);

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            new Thread(() -> {
                task.run();
                latch.countDown();
            }).start();
            return null;
        }).when(scheduler).execute(any(Runnable.class));

        Observer<Integer> observer = mock(Observer.class);

        Observable<Integer> observable = Observable.create(obs -> {
            obs.onNext(100);
            obs.onComplete();
        });

        observable.observeOn(scheduler).subscribe(observer);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        verify(observer).onNext(100);
        verify(observer).onComplete();
        verify(scheduler, times(2)).execute(any(Runnable.class));
    }

    @Test
    void disposableShouldCancelSubscription() {
        Observer<Integer> observer = mock(Observer.class);

        Observable<Integer> observable = Observable.create(obs -> {
            obs.onNext(1);
            obs.onNext(2);
            obs.onNext(3);
            obs.onComplete();
        });

        Disposable disposable = observable.subscribe(observer);
        disposable.dispose();

        assertTrue(disposable.isDisposed());
    }
}
