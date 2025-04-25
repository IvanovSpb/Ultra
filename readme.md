
# Rx-ultra

Rx-ultra — это легковесная библиотека для работы с потоками данных в реактивном стиле, реализующая основные операторы для трансформации, фильтрации и объединения потоков. Библиотека построена по паттерну "Наблюдатель" (Observer Pattern) и предоставляет удобный API для обработки асинхронных событий с использованием реактивных примитивов.

---

## Архитектура

Библиотека включает следующие ключевые компоненты:

- **Observable**  
  Класс, представляющий поток данных. Он позволяет создавать новые потоки с помощью статического метода `create` и поддерживает базовые операторы:
  - `map` — трансформация каждого элемента потока.
  - `filter` — фильтрация элементов по условию.
  - `flatMap` — преобразование каждого элемента в новый Observable с последующим объединением потоков.
  - `limit` — ограничение количества элементов.
  
  При подписке (`subscribe`) Observable оборачивает переданного `Observer` в наблюдателя, который проверяет флаг отмены подписки, позволяя корректно прекращать передачу элементов.

- **Observer**  
  Интерфейс с методами:
  - `onNext(T item)` — получение следующего элемента.
  - `onError(Throwable t)` — обработка ошибок.
  - `onComplete()` — уведомление об окончании передачи данных.
  
  Пользователь может реализовать этот интерфейс для получения данных из потока.

- **Disposable**  
  Интерфейс для управления жизненным циклом подписки. Через него возможно отменить подписку и предотвратить дальнейшую передачу элементов.

- **Schedulers**  
  Для управления потоками выполнения используются Schedulers. Они позволяют:
  - **`subscribeOn(Scheduler scheduler)`**: указать, на каком потоке будет происходить подписка на Observable.
  - **`observeOn(Scheduler scheduler)`**: указать, на каком потоке будут обрабатываться вызовы методов Observer.
  
  Schedulers реализованы через интерфейс `Scheduler`, который принимает `Runnable` задачу для выполнения.

---

## Schedulers: принципы работы, различия и области применения

В библиотеке реализовано несколько типов Schedulers:

- **ComputationScheduler**  
  - **Что делает:**  
    Создаёт пул потоков фиксированного размера, равного количеству ядер процессора, что хорошо подходит для вычислительно интенсивных задач.
  - **Область применения:**  
    Подходит для задач, не связанных с операциями ввода-вывода, где требуется стабильное количество потоков для параллельных вычислений.

- **IOThreadScheduler**  
  - **Что делает:**  
    Использует кэшированный пул потоков (`Executors.newCachedThreadPool()`), что делает его пригодным для большого количества кратковременных задач ввода-вывода.
  - **Область применения:**  
    Идеален для операций, связанных с вводом-выводом (сетевые запросы, операции чтения/записи на диск), где количество одновременно выполняемых задач может существенно варьироваться.

- **SingleScheduler**  
  - **Что делает:**  
    Обеспечивает выполнение задач в одном потоке (`Executors.newSingleThreadExecutor()`).
  - **Область применения:**  
    Полезен для последовательного выполнения задач, когда необходим строго последовательный порядок обработки событий.

Применение методов `subscribeOn` и `observeOn` даёт возможность гибко управлять тем, где и как будут запускаться операции подписки и обработки элементов, что позволяет снизить накладные расходы и избежать блокировки основных потоков.

---

## Процесс тестирования и основные сценарии

Тестирование библиотеки организовано с использованием JUnit и Mockito. Основные сценарии тестирования включают:

- **Получение значений.**  
  Проверяется, что Observable корректно эмитирует значения (метод `onNext`), завершает поток (`onComplete`) и обрабатывает ошибки (`onError`).

- **Трансформация элементов.**  
  Использование метода `map` для преобразования значений, а также проверка корректности реализации цепочки операторов.

- **Фильтрация элементов.**  
  Тесты для оператора `filter` проверяют, что в итоговый поток попадают только элементы, удовлетворяющие заданному условию.

- **Ограничение потока.**  
  Метод `limit` тестируется на количество эмитированных элементов, чтобы убедиться, что поток содержит только заданное число элементов.

- **Обработка вложенных потоков.**  
  Оператор `flatMap` тестируется на примере объединения нескольких Observable в один поток.

- **Асинхронное выполнение.**  
  Тесты для методов `subscribeOn` и `observeOn` используют mock-объекты для Scheduler и `CountDownLatch` для ожидания выполнения задач, чтобы проверить корректное распределение вызовов по потокам.

- **Управление подпиской (Disposable).**  
  Проверяется, что после вызова метода `dispose()` поток прекращает передачу значений.

---

## Примеры использования

### Пример 1. Базовое создание и подписка на Observable

```java
import com.vladimir.ultra.Observable;
import interfaces.com.vladimir.ultra.Observer;

public class BasicExample {
  public static void main(String[] args) {
    Observable<Integer> observable = Observable.create(observer -> {
      observer.onNext(1);
      observer.onNext(2);
      observer.onNext(3);
      observer.onComplete();
    });

    observable.subscribe(new Observer<>() {
      @Override
      public void onNext(Integer item) {
        System.out.println("Получено: " + item);
      }

      @Override
      public void onError(Throwable t) {
        System.err.println("Ошибка: " + t.getMessage());
      }

      @Override
      public void onComplete() {
        System.out.println("Завершено");
      }
    });
  }
}
```

### Пример 2. Использование операторов `map`, `filter` и `limit`

```java
import com.vladimir.ultra.Observable;
import interfaces.com.vladimir.ultra.Observer;

public class OperatorsExample {
  public static void main(String[] args) {
    Observable<Integer> observable = Observable.create(observer -> {
      for (int i = 1; i <= 10; i++) {
        observer.onNext(i);
      }
      observer.onComplete();
    });

    observable
            .filter(value -> value % 2 == 0) // пропускаем только чётные числа
            .map(value -> "Число: " + value) // преобразуем число в строку
            .limit(3)                     // ограничиваем количество эмитируемых элементов до 3
            .subscribe(new Observer<>() {
              @Override
              public void onNext(Object item) {
                System.out.println(item);
              }

              @Override
              public void onError(Throwable t) {
                t.printStackTrace();
              }

              @Override
              public void onComplete() {
                System.out.println("Поток завершен");
              }
            });
  }
}
```

### Пример 3. Асинхронная подписка с использованием Scheduler

```java
import com.vladimir.ultra.Observable;
import interfaces.com.vladimir.ultra.Observer;
import scheduler.com.vladimir.ultra.IOThreadScheduler;
import scheduler.com.vladimir.ultra.SingleScheduler;

public class AsyncExample {
  public static void main(String[] args) {
    // Создаем Observable с данными
    Observable<Integer> observable = Observable.create(observer -> {
      observer.onNext(42);
      observer.onComplete();
    });

    // Настраиваем подписку и обработку на разных потоках
    observable
            .subscribeOn(new IOThreadScheduler())     // подписка выполняется в пуле потоков для ввода-вывода
            .observeOn(new SingleScheduler())           // обработка событий происходит в одном потоке
            .subscribe(new Observer<>() {
              @Override
              public void onNext(Integer item) {
                System.out.println("Асинхронно получено: " + item);
              }

              @Override
              public void onError(Throwable t) {
                t.printStackTrace();
              }

              @Override
              public void onComplete() {
                System.out.println("Асинхронный поток завершен");
              }
            });
  }
}
```

---

## Заключение

Rx-ultra предоставляет простой и понятный API для работы с реактивными потоками, позволяющий обрабатывать асинхронные события с помощью цепочек операторов. Благодаря поддержке различных Scheduler'ов можно гибко настраивать выполнение подписок и обработки элементов в нужных потоках, а комплексный набор тестов гарантирует корректность работы основных сценариев.

Надеемся, что данная библиотека будет полезна для изучения принципов реактивного программирования и разработки высокопроизводительных приложений.
```