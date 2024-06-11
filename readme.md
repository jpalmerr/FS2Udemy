# FS2 Udemy course

Completed 

# Personal projects

[Queuing Mechanism](https://github.com/jpalmerr/FS2Udemy/tree/main/src/main/scala/scrapbook/queuingApi)
 
- Say you have a constant stream of traffic to a http call
- On success response, do nothing
- On error response, we want to kick off a process
  - real life example to compare to is invalidating a cache after a decoding error
- To avoid traffic building and performing the same function, potentially building up memory issues, instead we want to:
  - create queueing mechanism
  - when queue contains 1 message, kick off function. once this function is complete, flush the queue
  - when queue now contains more than 1 message, assume the process is still ongoing and do nothing
