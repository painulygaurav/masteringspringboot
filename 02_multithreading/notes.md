# Multithreading
When you see multiple programs like MS Word, Chrome, and Video player running simultaneously that is called multitasking.
When you see multiple processes being executed in a single application then that is called multithreaded application.
That's why threads are called light weight because they run within a single application.

## Methods
### Using Thread Class
If you are not extending any class then  you can use Thread class method and extend Thread.
Whatever logic you want in a thread you must right in the run method. Run is the starting point of a Thread.
```java
public void run() {
    
}
```

- **`run()` method** - Whenever you are extending Thread class then you must override run method.
Whenever you

### Using Runnable Interface
If you are already extending a class in your class then you can use Runnable Interface because in Java you can't extend more than one class.
Which means a sub(child) class cannot have multiple super (parent) class.

## Thread States
- **New** - The thread is created but not yet started.
- **Runnable(Ready)** - The thread is ready to run and waiting for CPU scheduling.
- **Running** - The thread is actively executing it's `run()` method.
- **Waiting/Blocked** - The thread is not eligible to run because it's waiting for a resource or a specific condition.
- **Timed waiting(sleeping)** - The thread is waiting for a specific amount of time before becoming runnable again.
- **Terminated** - The thread has finished executing.

## Thread Priorities
The threads are maintained in a `Ready Queue`. JVM's Scheduler maintains this `Ready Queue`, all the threads will eventually be executed by CPU.
All threads get equal amount of time slice of the CPU. If any thread is having higher priority (highest priority = 10), so now they won't get
equal time slice like in a round-robin rather the high priority thread will get more time and will be given priority in the queue.

- **Thread.MIN_PRIORITY = 1**
- **Thread.NORM_PRIORITY = 5** [DEFAULT]
- **Thread.MAX_PRIORITY = 10**

## Thread Class and it's methods

### Constructors
- **Thread()**
- **Thread(Runnable r)**
- **Thread(Runnable r, String name)**
- **Thread(ThreadGroup g, String name)**
- **Thread(String name)**

### getters and setters methods
- **long getID()**
- **String getName()**
- **int getPriority()**
- **ThreadState getState()**
- **ThreadGroup getThreadGroup()**
- **void setName(String name)**
- **void setPriority(int p)**
- **void setDaemon(boolean d)**

### Enquiry
- **boolean isAlive()**
- **boolean isDaemon()**
- **boolean isInterrupted()**

### Daemon Thread
It is a thread that runs in the background. Like garbage collector.