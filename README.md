# Welcome to the blockingMap4j wiki!


### 1. What is it? <a name="what"></a>
Blocking map acts as a key-based synchronizer between producers and consumers in concurrent environment. It is a Map that additionally supports operations that wait for a key to be available when retrieving an element. There can be multiple producers populating & multiple consumers consuming from the same blocking map. BlockingMap is **thread-safe & highly-concurrent**.
> **Note:**
> - For now, only unbound map implementation supported
> - Maven GAV co-ordinates: <br/>
> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\<groupId\>com.github.sarveswaran-m\</groupId\><br/>
> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\<artifactId\>util.concurrent.blockingMap\</artifactId\>


### 2. Why blockingMap4j? <a name="why"></a>
In any concurrent environment with one or more producer(s) & consumer(s), synchronizer between the producers & consumers is required. A *Queue* is typically used as synchronizer. Java has many queue based synchronizers. The **BlockingQueue** interface with multiple implementations like *ArrayBlockingQueue, DelayQueue* etc are examples of queue based synchronizers in Java. 

When a blocking queue is used as synchronizer, consumers can wait for any arbitrary object to be available. But consumers cannot wait for a specific object to be available i.e If each consumer is interested in a specific object, they cannot do so on a *BlockingQueue*. **BlockingMap** addresses this need for consumers to wait on specific objects in a concurrent producer-consumer environment. 

### 3. How is it done? <a name="how"></a>
* BlockingMap is based on the concept of BlockingQueue. BlockingMap definition is consistent with blockingqueue.
* Effort has been made to avoid locking, since locking adversely affects performance. When locking becomes un-avoidable, **Re-entrant read & write locks** is used to improve throughput. Read locks do not drastically reduce through put since they can inter leave.
* BlockingMap abstraction & implementation separated. BlockingMap interface extends Map. There can be multiple BlockingMap implementations. There can be implementations that can be bound or unbound, ordered or unordered, etc.
* BlockingHashMap is an implementation of BlockingMap that is unbound & unordered.
Internally, it uses latches to block/unblock consumers.
* Since BlockingHashMap exhibits distinctively different behaviors based on the state it is in, **state
pattern** is employed. Any operation on BlockingHashMap is delegated to state based concrete
implementations.
* ActiveBlockingHashMap & PassiveBlockingHashMap are the concrete implementations. Any
operation on an instance BlockingHashMap that is not shut-down will eventually be carried out by
ActiveBlockingHashMap. PassiveBlockingHashMap is an **immutable singleton**. Any operation on any
instance of BlockedHashMap will land in PassiveBlockingHashMap.
> **Note:**
> - Refer to [Semantics of BlockingMap](#semantics) to understand the contract offered by BlockingMap

### 4. Testing times for BlockingMap <a name="test"></a>

#### 4.1 Test Strategy
- Idea is to first test the semantics of blocking map and then check its consistency by gradually
    increasing conflicts.
- Functionality and consistency of the map is first tested in a single thread environment. There
    will be no conflicts in this scenario.
- Functionality and consistency of the map is then tested in a single producer- single consumer
    environment. There will be some conflict in this environment.
- Functionality and consistency of the map is then tested in a multiple producers- multiple
    consumers environment. There will be maximum conflict in this environment.

### 4.2 Scenarios Considered
- Scenario 1: Single Thread – methods that return special values
- Scenario 2: Single Thread – methods that block
- Scenario 3: Single Thread – shut down & other operations
- Scenario 4: Single Producer & Single Consumer – methods that return special values
- Scenario 5: Single Producer & Single Consumer – methods that block
- Scenario 6: Single Producer & multiple Consumer s – special case
- Scenario 7: Single Producer & Single Consumer –shut down & other operations
- Scenario 8: Multiple producers & multiple consumers

All test cases can be found in the spreadsheet attached <a href="https://docs.google.com/spreadsheets/d/1ka62aFjnkFLfovXzvYipmF9rKbWS0k0hqvsN0u3P70A/edit#gid=2143733835" target="_blank">here.</a>



### 5. Future Enhancements <a name="future"></a>

- Current behaviour when multiple producers try to `offer(K)` & while multiple consumers try to `take(k)` on the same key should be further analysed & refined
- Concurrency improvements in Java 8 should be incorporated
- Conflicts & ambiguities on the blocking map arise only when operations on the same key
    interleave. Throughput can be increased to a big extent if there is a lock for each key.
- A bound implementation of blocking map.

### 6. Semantics of BlockingMap <a name="semantics"></a>

- Three types of primary operations is allowed on the blockingmap.
  - Insert
  - Remove
  - Examine
- Three flavors of each of these operations is supported.


Special|value    |Blocks    |Times Out
-------|---------|----------|----------
Insert |put(k,v) |offer(k,v)| offer(k,v,time,unit)
Remove |remove(k)|take(k)   |take(k,time,unit)
Examine| get(k)  |N.A       |N.A

- `put(k,v)` on an un-bound map should always be successful. `put(k,v)` returns null if there was no
    previous mapping for the given key. `put(k,v)` returns the value that was previously associated
    with the given key (if such a mapping already exists in the map)
- Bound blocking maps should return a special value to distinguish between successful & unsuccessful
    invocation of a `put(k,v)`
- `remove(k)` usually removes the mapping corresponding to the given key & returns the value
    corresponding to the given key.
- `remove(k)` on a key that does not exist in the map shouldreturn immediately with null.
- `get(k)` usually returns the value associated with the given key.
- `get(k)` on a key that does not exist in the map should return immediately with null.
- `offer(k,v)` should behave similar to `put(k,v)` under normal circumstances.
- `offer(k,v)` on a bound blocking map that is full should block till space is available on the map to
    put the specified mapping.
- `take(K)` usually returns with the value corresponding to the given key.
- `take(k)` on a key that does not exist will block, till that key becomes available. When the key
    becomes available, the corresponding mapping is removed from the map & value returned.
    i.e. an attempt by a consumer to consume the object corresponding to a key will block till a
    producer produces the object & puts it on the map. As soon as producer puts the product on the
    map, consumer takes it.
- If `take(k)` on a key that does not exist is invoked from multiple threads, all the thread should
    block till the key becomes available. When the key becomes available, all the blocked threads
    should be notified. Actual removal of the mapping can & should happen from only one of the
    threads. Therefore, the operation should be successful from only one thread. Hence, value
    should be returned on only one of the threads and not on all the threads. Null should be
    returned on threads on which the operation was not successful.
- When a consumer is blocked on `take(k)` waiting for a key that is not available yet, it can be
    interrupted. Consumer should return in this case with an Interrupted exception.
- `offer(k,v, time, unit)` should behave similar to `offer(k,v)` but time out when the specified amount
    of time elapses.
- `offer(k,v, time, unit)` should return null when it times out.
- Bound blocking map Implementations should return a special value to differentiate a successful
    invocation of `offer(k,v, time, unit)` from a timed out one.
- `take(k, time, unit)` should behave the same way as `take(k)` but time out after the specified time
    elapses.
- `take(k, time, unit)` should return null when it times out.
- The above specified operational constructs should hold well when multiple producers & consumers rendezvous with the map.
- An attempt to put a key that already exists in the map will be ignored. Since blocking map acts as a synchronizer, every product produced should either be consumed or removed explicitly. Over-write on a synchronizer is not meaningful.
- Map should exhibit highest level of concurrency characterized by minimal or no-locks.
- Though BlockingMap is designed to be used primarily for producer-consumer maps, it additionally support the Map interface. This is done to maintain consistency with the existing map hierarchy in **collection** framework.
- All methods in the map interface need not be supported. However, methods that are essential
    in a producer-consumer environment should be supported. In general, any method in Map
    interface that can be supported unambiguously without introducing additional complexities
    shall be supported. For detailed list of methods that are supported, **pleaserefer to <a href="http://blockingmapforj.sourceforge.net/" target="_blank">javadoc</a>**
- Null keys & values should not be accepted since null is used a special maker.Blocking map
    should throw a NullPointerException when an attempt is made to put a null key or value.
- A logical shutdown operation should be supported. On shutdown, all blocked consumers should
    be notified (to prevent the consumers from waiting for-ever).
- Any operation on a map that has been shut-down will throw an exception indicating the
    shutdown.

  
