package io.k8cluster.monitors;

import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PersistentVolumeList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Watch;
import io.kubernetes.client.util.Watch.Response;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Order(2)
@Component
public class PodMonitor extends EventMonitor {

    public static void main(String[] args) throws Exception {

        ApiClient client = Config.defaultClient();

        // Optional, put helpful during tests: disable client timeout and enable
        // HTTP wire-level logs
        OkHttpClient newClient = client.getHttpClient()
                .newBuilder()
                .readTimeout(0, TimeUnit.SECONDS)
                .build();

        client.setHttpClient(newClient);
        CoreV1Api api = new CoreV1Api(client);
        PodMonitor podMonitor = new PodMonitor();
        podMonitor.monitor(api);
    }

    public String resourceVersion(CoreV1Api api) throws ApiException {
        V1PersistentVolumeList itemList = api.listPersistentVolume(null, null, null, null, null, null, null, null, null, null);
        String resourceVersion = itemList.getMetadata().getResourceVersion();
        return resourceVersion;
    }

    public String monitor(CoreV1Api api) throws IOException {
        String resourceVersion = this.getResourceVersion();
        ApiClient client = api.getApiClient();
        try {
            log.info("[I59] Creating watch: resourceVersion={}", resourceVersion);
            try (Watch<V1Pod> watch = Watch.createWatch(
                    client,
                    api.listPodForAllNamespacesCall(null, null, null, null, null, null, resourceVersion, null, 60, true, null),
                    new TypeToken<Response<V1Pod>>(){}.getType())) {

                log.info("[I65] Receiving events:");
                for (Response<V1Pod> event : watch) {
                    V1Pod pod = event.object;
                    V1ObjectMeta meta = pod.getMetadata();
                    resourceVersion = meta.getResourceVersion();
                    log.info(pod.toString());
                    switch (event.type) {
                        case "ADDED":
                        case "MODIFIED":
                        case "DELETED":
                            log.info("event: type={}, namespace={}, name={}, message={}",
                                    event.type,
                                    meta.getNamespace(),
                                    meta.getName(), String.join("|", getPodMessage(pod)));
                            break;
                        default:
                            log.warn("[W76] Unknown event type: {}", event.type);
                    }
                }
            }
        } catch (ApiException ex) {
            if ( ex.getCode() == 504 || ex.getCode() == 410 ) {
                resourceVersion = extractResourceVersionFromException(ex);
            }
            else {
                // Reset resource version
                resourceVersion = null;
            }
        }
        return resourceVersion;
    }

    private List<String> getPodMessage(V1Pod pod) {
        return CollectionUtils.isEmpty(pod.getStatus().getConditions()) ? Collections.emptyList():
                pod.getStatus().getConditions().stream().filter(v1PodCondition -> StringUtils.isNotEmpty(v1PodCondition.getMessage()))
                .map(v1PodCondition -> String.format("Condition:%s | Message:%s", v1PodCondition.getType(), v1PodCondition.getMessage()))
                .collect(Collectors.toList());
    }
}