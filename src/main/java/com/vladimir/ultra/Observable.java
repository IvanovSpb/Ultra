package com.vladimir.ultra;

import com.vladimir.ultra.interfaces.Disposable;
import com.vladimir.ultra.interfaces.Observer;
import com.vladimir.ultra.interfaces.Scheduler;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Класс Observable для работы с потоком данных в реактивном стиле.
 *
 * @param <T> тип данных, испускаемых Observable
 */
public class Observable<T> {

    /**
     * Интерфейс для подписки на источник данных.
     *
     * @param <T> тип данных, которые будут переданы подписчику
     */
    public interface OnSubscribe<T> {
        void subscribe(Observer<T> observer);
    }

    private final OnSubscribe<T> subscriptionAction;

    /**
     * Конструктор Observable.
     *
     * @param subscriptionAction действие, выполняемое при подписке
     */
    public Observable(OnSubscribe<T> subscriptionAction) {
        this.subscriptionAction = subscriptionAction;
    }

    /**
     * Создаёт новый Observable из источника данных.
     *
     * @param source источник данных
     * @param <T> тип элементов потока
     * @return новый экземпляр Observable
     */
    public static <T> Observable<T> create(OnSubscribe<T> source) {
        return new Observable<>(source);
    }

    /**
     * Подписывает наблюдателя на этот Observable.
     *
     * @param observer наблюдатель, принимающий элементы потока
     * @return Disposable-объект для отмены подписки
     */
    public Disposable subscribe(Observer<T> observer) {
        AtomicBoolean disposed = new AtomicBoolean(false);

        Observer<T> safeObserver = new Observer<T>() {
            @Override
            public void onNext(T value) {
                if (!disposed.get()) observer.onNext(value);
            }

            @Override
            public void onError(Throwable error) {
                if (!disposed.get()) observer.onError(error);
            }

            @Override
            public void onComplete() {
                if (!disposed.get()) observer.onComplete();
            }
        };

        subscriptionAction.subscribe(safeObserver);

        return new Disposable() {
            @Override
            public void dispose() {
                disposed.set(true);
            }

            @Override
            public boolean isDisposed() {
                return disposed.get();
            }
        };
    }

    /**
     * Осуществляет трансформацию элементов потока с помощью функции mapper.
     *
     * @param mapper функция для трансформации элементов
     * @param <R> новый тип элементов после применения функции mapper
     * @return новый Observable с трансформированными элементами
     */
    public <R> Observable<R> map(Function<T, R> mapper) {
        return Observable.create(observer ->
                this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        try {
                            R transformedValue = mapper.apply(value);
                            observer.onNext(transformedValue);
                        } catch (Throwable throwable) {
                            observer.onError(throwable);
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }

                    @Override
                    public void onComplete() {
                        observer.onComplete();
                    }
                })
        );
    }

    /**
     * Ограничивает количество элементов, передаваемых подписчику потока.
     * Сделал дополнительно, без задания
     * @param maxItems максимальное количество элементов
     * @return новый Observable, ограниченный заданным количеством элементов
     */
    public Observable<T> limit(int maxItems) {
        return Observable.create(observer ->
                this.subscribe(new Observer<T>() {
                    private int emittedCount = 0;

                    @Override
                    public void onNext(T value) {
                        if (emittedCount < maxItems) {
                            observer.onNext(value);
                        }
                        emittedCount++;
                    }

                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }

                    @Override
                    public void onComplete() {
                        observer.onComplete();
                    }
                })
        );
    }

    /**
     * Фильтрует элементы потока согласно заданному предикату.
     *
     * @param condition условие фильтрации элементов
     * @return новый Observable, содержащий только элементы, удовлетворяющие предикату
     */
    public Observable<T> filter(Predicate<T> condition) {
        return Observable.create(observer ->
                this.subscribe(new Observer<>() {
                    @Override
                    public void onNext(T value) {
                        try {
                            if (condition.test(value)) {
                                observer.onNext(value);
                            }
                        } catch (Throwable throwable) {
                            observer.onError(throwable);
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }

                    @Override
                    public void onComplete() {
                        observer.onComplete();
                    }
                })
        );
    }

    /**
     * Преобразует каждый элемент в новый Observable и объединяет результаты.
     *
     * @param mapper функция, преобразующая элемент в Observable
     * @param <R> тип элементов результирующего потока
     * @return новый Observable после преобразования и объединения элементов
     */
    public <R> Observable<R> flatMap(Function<T, Observable<R>> mapper) {
        return Observable.create(observer ->
                this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        try {
                            Observable<R> innerObservable = mapper.apply(value);
                            innerObservable.subscribe(new Observer<R>() {
                                @Override
                                public void onNext(R innerValue) {
                                    observer.onNext(innerValue);
                                }

                                @Override
                                public void onError(Throwable error) {
                                    observer.onError(error);
                                }

                                @Override
                                public void onComplete() {
                                    // intentionally empty
                                }
                            });
                        } catch (Throwable throwable) {
                            observer.onError(throwable);
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }

                    @Override
                    public void onComplete() {
                        observer.onComplete();
                    }
                })
        );
    }

    /**
     * Указывает Scheduler, на котором будет происходить подписка на Observable.
     *
     * @param scheduler планировщик, управляющий потоком выполнения
     * @return Observable, подписка на который будет выполняться на заданном Scheduler
     */
    public Observable<T> subscribeOn(Scheduler scheduler) {
        return Observable.create(observer ->
                scheduler.execute(() -> Observable.this.subscribe(observer))
        );
    }

    /**
     * Указывает Scheduler, на котором будут обрабатываться вызовы Observer.
     *
     * @param scheduler планировщик, на котором будут обрабатываться события
     * @return Observable, события которого будут обрабатываться на заданном Scheduler
     */
    public Observable<T> observeOn(Scheduler scheduler) {
        return Observable.create(observer ->
                this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T value) {
                        scheduler.execute(() -> observer.onNext(value));
                    }

                    @Override
                    public void onError(Throwable error) {
                        scheduler.execute(() -> observer.onError(error));
                    }

                    @Override
                    public void onComplete() {
                        scheduler.execute(observer::onComplete);
                    }
                })
        );
    }
}