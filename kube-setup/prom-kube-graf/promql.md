## PromSQL Resource Queries

### Namespace Resource Stats
- Get Namespaces with memory:
  ```
  topk(5, sort_desc(sum(avg_over_time(container_memory_working_set_bytes{container="", namespace=~"kube-monitor|echo"}[1m])) by (namespace)))
  ```
- Get Namespaces with cpu:
  ```
  topk(5, sort_desc(sum(rate(container_cpu_usage_seconds_total{app_kubernetes_io_name="", namespace=~"kube-monitor|echo"}[1m])) by (namespace)))
  ```

### Pods Resource Request Stats
- Get Pods with memory request top 5:
  ```
  topk(5, sort_desc(sum(label_replace(kube_pod_container_resource_requests{app_kubernetes_io_name="", namespace=~"kube-monitor|echo", resource="memory"}, "podName", "$1$3", "pod", "(.*)(-.{9,10}-.{5})|(.*)")) by (namespace, podName)))
  ```
- Get Pods with cpu request top 5:
  ```
  topk(5, sort_desc(sum(label_replace(kube_pod_container_resource_requests{app_kubernetes_io_name="", namespace=~"kube-monitor|echo", resource="cpu"}, "podName", "$1$3", "pod", "(.*)(-.{9,10}-.{5})|(.*)")) by (namespace, podName)))
  ```

### Pods Resource Usage Stats
- Get Pods with memory usage top 5:
  ```
  topk(5, sort_desc(sum(label_replace(container_memory_working_set_bytes{container="", namespace=~"kube-monitor|echo"}, "podName",  "$1$3", "pod", "(.*)(-.{9,10}-.{5})|(.*)")) by (namespace, podName)))
  ```
- Get Pods with cpu usage top 5:
  ```
  topk(5, sort_desc(sum(label_replace(rate(container_cpu_usage_seconds_total{app_kubernetes_io_name="", namespace=~"kube-monitor|echo"}[1m]), "podName",  "$1$3", "pod", "(.*)(-.{9,10}-.{5})|(.*)")) by (namespace, podName)))
  ```


### Pods Restart Stats
- Get Pods with restarts top 5:
  ```
  topk(5,sum(sort_desc(label_replace(kube_pod_container_status_restarts_total{app_kubernetes_io_name="", namespace=~"kube-monitor|echo|kube-system"}, "podName", "$1$3", "pod", "(.*)(-.{9,10}-.{5})|(.*)"))) by (namespace, podName))
  ```
- Get Pods with restarts top 5:
  ```
  topk(5,avg(sort_desc(label_replace(kube_pod_container_status_waiting_reason{app_kubernetes_io_name="", namespace=~"kube-monitor|echo|kube-system"}, "podName", "$1$3", "pod", "(.*)(-.{9,10}-.{5})|(.*)"))) by (namespace, podName, reason))
  ```


### Pods PV Stats:
- Get Pods with Volume request top 5:
  ```
  topk(5, sort_desc(max(kube_persistentvolume_capacity_bytes) by (persistentvolume)))
  ```
### Pod Log Storage:
 - Log Storage for Pods
  ```
  topk(5,avg(sort_desc(label_replace(kubelet_container_log_filesystem_used_bytes{namespace=~"kube-monitor|echo|kube-system"}, "podName", "$1$3", "pod", "(.*)(-.{9,10}-.{5})|(.*)"))) by (namespace, podName))
  ```

