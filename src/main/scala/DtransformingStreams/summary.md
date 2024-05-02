# Summary

- fs2 streams are pulled based (only emit when asked to)
- Streams emit values in chunks (think about bus example from aws summit)
- Pulls represent processes
  - useful when transforming streams
- Pipes are functions from stream to streams
  - the `through` operation can be used to transfrom a stream to a pipe