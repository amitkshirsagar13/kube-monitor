package io.k8cluster.monitors;


import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PersistentVolume;
import io.kubernetes.client.openapi.models.V1PersistentVolumeList;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Watch;
import io.kubernetes.client.util.Watch.Response;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Order(3)
@Component
public class PvMonitor extends EventMonitor {

    public static void main(String[] args) throws Exception {

        ApiClient client = Config.defaultClient();

        OkHttpClient newClient = client.getHttpClient()
                .newBuilder()
                .readTimeout(0, TimeUnit.SECONDS)
                .build();
        client.setHttpClient(newClient);
        CoreV1Api api = new CoreV1Api(client);
        PvMonitor monitor = new PvMonitor();
        monitor.monitor(api);
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
            try (Watch<V1PersistentVolume> watch = Watch.createWatch(
                    client,
                    api.listPersistentVolumeCall(null, null, null, null,
                            null, null, resourceVersion, null, 60, true, null),
                    new TypeToken<Response<V1PersistentVolume>>(){}.getType())) {

                log.info("[I65] Receiving events:");
                for (Response<V1PersistentVolume> event : watch) {
                    V1PersistentVolume item = event.object;
                    V1ObjectMeta meta = item.getMetadata();
                    resourceVersion = meta.getResourceVersion();
                    switch (event.type) {
                        case "ADDED":
                        case "MODIFIED":
                        case "DELETED":
                            log.info("event: type={}, name={}, phase={}, message={}, reason={}, created={}, deleted={}",
                                    event.type,
                                    meta.getName(),
                                    item.getStatus().getPhase(),
                                    item.getStatus().getMessage(),
                                    item.getStatus().getReason(),
                                    meta.getCreationTimestamp(),
                                    meta.getDeletionTimestamp());
                            break;
                        default:
                            log.warn("[W76] Unknown event type: {}", event.type);
                    }
                }
            }
        } catch (ApiException ex) {
            if ( ex.getCode() == 504 || ex.getCode() == 410 ) {
                resourceVersion = extractResourceVersionFromException(ex);
            } else {
                resourceVersion = null;
            }
        }
        return resourceVersion;
    }
}