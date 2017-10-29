Run `prototype.MultiNodeMain`, which starts 3 nodes in single JVM. 
Each node has HTTP endpoint and Akka cluster member. 

HTTP ports are: 8080,8081,8082.

Run
```
curl 'localhost:8080/getChain' | python -m json.tool
```
on all nodes to confirm all nodes have same genesis block.
Run
```
curl localhost:8080/getBalance?address=Alice
```
to confirm that Alice has 100 coins.

Run
```
curl 'localhost:8080/send?from=Alice&to=Bob&amount=1'
```
To send 1 coin to Bob. Check balance and chain on all nodes.

To add 4th node, run `prototype.SingleNodeMain`. It should join the cluster and copy the chain, 
HTTP server is at port 8083. Note that as long as at least one JVM is up, the state will not disappear.

TODO
====
Identity is currently based on plain string names. This should be changed to public/private keys.
Mutiple transactions per block are not supported.
Transactions should be gossipped so that each node can do proof of work.
Nodes are not rewarded for mining.
Test coverage, logging, exception handling is minimal.
