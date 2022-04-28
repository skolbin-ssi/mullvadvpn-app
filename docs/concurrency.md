# Sharing data with _concurrency_
Since our client has to be responsive to changes whilst *doing things*, it must be able to do different things
concurrently. In the daemon, it's various different components are running concurrently, often in parallel.

## Actor model
To handle various concurrent computations within the daemon, it is comprised of lots of components that may act
concurrently and in parallel of each other, often interacting with one another via sending messages through channels.

### What makes an actor
- Owns some data
- Responding to messages, trying its best to not block itself
- In our case, an actor always runs in it's own execution context (thread or future)
- An actor may concurrently listen for messages whilst managing some other work

#### More specific examples
- The relay list updater is an actor that responds to *messages* from the daemon to eagerly download a relay list,
  whilst also waiting on a timer
- The account manager will respond to requests for account data whilst waiting on a timer to update the WireGuard keys.
- The daemon is an actor that will respond to RPCs and messages from the tunnel state machine, among other actors, and
    synchronize user data (settings, target state) to the


### What problems using an actor solves
- Dividing shared state out to an actor can relieve data dependencies
- Provides some strucutre to concurrency and allows
- Provides an asynchronous interface around some data that is bound to blocking operations, such as API calls and I/O
    operations.
- Using asynchronous actors allows for using less threads, which is good for efficiency.

### Guidelines around working with actors
- If servicing a message requires some blocking, one should evaluate if it can be done in an asynchronous fashion,
    either by spawning a new future, thread or by polling the future alongside the messages.
- Avoid sharing state via messages and locks
- Avoid using actors where they are not necessary - if a bit of data needs to be shared and operations requiring
    exclusive access to it are not blocking, one should heavily consider using a plain old `Arc<Mutex<_>>`.
- If an actor needs to be used by multiple other components, but it itself doesn't rely on sending messages or on
    blocking syscalls, maybe it can be shared via a mutex instead?


The actor model is just a fancy way of saying that a component that owns its own state.

