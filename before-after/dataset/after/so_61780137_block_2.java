Java
// thread1
public void run() {
    // to wait
    lock.lock();
    try {
        condition.await();
    } finally {
        lock.unlock();
    }
}
//thread2
public void run() {
    // to notify
    lock.lock();
    try {
        condition.signal();
    } finally {
        lock.unlock();
    }
}
You can also use [`CyclicBarrier`][2], and maybe other types.
The second school of thought is to have a single work thread, which executes the other using [`ExecutorService`][3]:
Java
// thread2
public void run() {
    executorService.execute(new RunnableThread1());
}
This concept looks at the work done by *thread1* as a detached task which can be executed multiple times. So this may not be compatible with your program.
And the last option is to use [`Thread.interrupt`][4]:
Java
//thread1
public void run() {
    while (true) {
         try {
             Thread.sleep(sleepTime);
         } catch(InterruptedException e) {
             // signaled.
         }
    }
}
//thread 2
public void run() {
    thread1.interrupt();
}
This may be a bit problematic since the interrupt call is better used to stop threads and not to signal them.
  [1]: https://www.tutorialspoint.com/java_concurrency/concurrency_condition.htm
  [2]: https://www.baeldung.com/java-cyclic-barrier
  [3]: https://www.baeldung.com/java-executor-service-tutorial
  [4]: https://www.javatpoint.com/interrupting-a-thread