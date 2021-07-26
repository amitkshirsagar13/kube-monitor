### PromSQL Resource Queries

- Get Namespaces with memory:

  ```
  topk(5, sort_desc(avg_over_time( sum(container_memory_usage_bytes{namespace=~"kube-monitor|echo"}) by (namespace)[5m:])/1000000))
  ```
- Get Pods with memory usage top 5:

  ```
  topk(5, sort_desc(sum(label_replace(container_memory_working_set_bytes{namespace=~"kube-monitor|echo"}, "podName", "$1", "pod", "(.*)-.{10}-.{5}")) by (namespace,podName)/1000000))

  ```
- Get Pods with memory top 5:

  ```
  topk(5, sort_desc(sum(label_replace(container_spec_memory_reservation_limit_bytes{namespace=~"kube-monitor|echo"}, "podName", "$1", "pod", "(.*)-.{10}-.{5}")) by (namespace,podName)/1000000))
  ```
- Get Pods with cpu top 5:

  ```
  topk(5, sort_desc(sum(label_replace(container_cpu_usage_seconds_total{namespace=~"kube-monitor|echo"}, "podName", "$1", "pod", "(.*)-.{10}-.{5}")) by (namespace,podName)/1000000))
  ```
  