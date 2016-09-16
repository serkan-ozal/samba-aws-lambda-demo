1. What is this demo?
==============
AWS Lambda Demo for [Samba](https://github.com/serkan-ozal/samba) (Statefull AWS Lambda ) contains demo AWS Lambda applications by using **Samba** to access and share state.

2. Demos
==============

2.1. Database Access Handler
--------------
This handler is an AWS Lamda function that reuses database connections if they are available on the local through **Samba**. 

Creating database connection each time while accessing to database doesn't make sense because creating connection is not cheap operation and increases latency. By this behaviour, mostly database connections are reused according to background logic of AWS lambda under the hood as mentioned 
[here](https://aws.amazon.com/blogs/compute/container-reuse-in-lambda/) and [here](https://www.linkedin.com/pulse/aws-lambda-container-lifetime-config-refresh-frederik-willaert).

2.2. Request Counter Handler
--------------
This handler is an AWS Lamda function that shares and processes a request counter atomically and globally through **Samba**. 

In this handler, a long typed field (request counter) is shared globally via `TIERED` cache backed **Samba** field. Each lambda function invocation can increase this shared field value atomically and can retrieve its value with **eventual consistency** guarantee (means that every field access might not return fresh value but it will returns fresh value eventually).
