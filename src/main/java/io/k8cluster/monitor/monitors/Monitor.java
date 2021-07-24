package io.k8cluster.monitor.monitors;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;

import java.io.IOException;

public interface Monitor {
    String resourceVersion(CoreV1Api api) throws ApiException;
    String monitor(CoreV1Api api) throws IOException, ApiException;
}
