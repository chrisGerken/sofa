sofa
====

Stack Overflow Access (java client with Elasticsearch cache)


A Java client for Stack Overflow.  Pure java classes (User, Question, Answer) provide cached client support for the corresponding Stack Overflow graph objects.  A local Elasticsearch cluster (assuming Elasticsearch installation defaults) is checked for previously retrieved data to avoid excessive calls to Stack Overflow.  Server calls are also reduced by automatic pagination. 
