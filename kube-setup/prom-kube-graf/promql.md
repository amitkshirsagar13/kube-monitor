## PromSQL Resource Queries


### Namespace Resource Stats
- Get Namespaces with cpu:
  ```
  topk(5, sort_desc(avg_over_time( sum(container_cpu_usage_seconds_total{namespace=~"kube-monitor|echo"}) by (namespace)[5m:])))
  ```
- Get Namespaces with memory:
  ```
  topk(5, sort_desc(avg_over_time( sum(container_memory_usage_bytes{namespace=~"kube-monitor|echo"}) by (namespace)[5m:])/1000000))
  ```

### Pods Resource Usage Stats
- Get Pods with memory usage top 5:
  ```
  topk(5, sort_desc(avg_over_time(sum(label_replace(container_memory_working_set_bytes{namespace=~"kube-monitor|echo"}, "podName", "$1$3", "pod", "(.*)(-.{9,10}-.{5})|(.*)")) by (namespace, podName)[5m:])/1000000))
  ```
- Get Pods with cpu usage top 5:
  ```
  topk(5, sort_desc(sum(label_replace(container_cpu_usage_seconds_total{namespace=~"kube-monitor|echo"}, "podName",  "$1$3", "pod", "(.*)(-.{9,10}-.{5})|(.*)")) by (namespace, podName)))
  ```

### Pods Resource Request Stats
- Get Pods with memory request top 5:
  ```
  topk(5, sort_desc(avg_over_time(sum(label_replace(kube_pod_container_resource_requests{namespace=~"kube-monitor|echo", resource="memory"}, "podName", "$1$3", "pod", "(.*)(-.{9,10}-.{5})|(.*)")) by (namespace, podName)[15m:])/1000000))
  ```
- Get Pods with memory request top 5:
  ```
  topk(5, sort_desc(avg_over_time(sum(label_replace(kube_pod_container_resource_requests{namespace=~"kube-monitor|echo", resource="cpu"}, "podName", "$1$3", "pod", "(.*)(-.{9,10}-.{5})|(.*)")) by (namespace, podName)[15m:])))
  ```


### Pods Restart Stats
- Get Pods with restarts top 5:
  ```
  topk(5,avg(sort_desc(label_replace(kube_pod_container_status_restarts_total{namespace=~"kube-monitor|echo|kube-system"}, "podName", "$1$3", "pod", "(.*)(-.{9,10}-.{5})|(.*)"))) by (namespace, podName))
  ```

- Get Pods with restarts top 5:
  ```
  topk(5,avg(sort_desc(label_replace(kube_pod_container_status_waiting_reason{namespace=~"kube-monitor|echo|kube-system"}, "podName", "$1$3", "pod", "(.*)(-.{9,10}-.{5})|(.*)"))) by (namespace, podName, reason))
  ```


### Pods PV Stats:
- Get Pods with Volume request top 5:
  ```
  topk(5, sort_desc(max(kube_persistentvolume_capacity_bytes) by (persistentvolume)))
  ```

count(label_replace(kube_pod_container_status_waiting_reason{namespace=~"kube-monitor|echo"}, "podName", "$1$3", "pod", "(.*)(-.{9,10}-.{5})|(.*)")) by (namespace, podName, reason)