package io.k8cluster.monitors;

import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeCondition;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Watch;
import io.kubernetes.client.util.Watch.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Amit Kshirsagar
 *
 */
@Slf4j
@Order(1)
@Component
public class NodeMonitor extends EventMonitor {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {

        ApiClient client  = Config.defaultClient();
        CoreV1Api api = new CoreV1Api(client);
        NodeMonitor nodeMonitor = new NodeMonitor();
        nodeMonitor.monitor(api);
    }

    public String monitor(CoreV1Api api) throws IOException {
        String resourceVersion = this.getResourceVersion();
        ApiClient client = api.getApiClient();
        try {
            log.info("[I59] Creating watch: resourceVersion={}", resourceVersion);
            try (Watch<V1Node> watch = Watch.createWatch(
                    client,
                    api.listNodeCall(null, null, null, null,
                                    null, null, resourceVersion, null,
                                    10, true, null),
                    new TypeToken<Response<V1Node>>(){}.getType())) {

                log.info("[I65] Receiving events:");
                for (Response<V1Node> item : watch) {
                    V1Node node = item.object;
                    resourceVersion = node.getMetadata().getResourceVersion();
                    final Map<String, V1NodeCondition> nodeConditionMap = node.getStatus().getConditions()
                            .stream()
                            .collect(Collectors.toMap(V1NodeCondition::getType, Function.identity()));
                    log.info("Address: {}", node.getStatus().getAddresses().stream().filter(address->address.getType().equals("Hostname")).findFirst().get().getAddress());
                    final List<String> allocatable = node.getStatus().getAllocatable().entrySet().stream().map(entry -> {
                        String key = entry.getKey();
                        String value = entry.getValue().getNumber().toString();
                        return String.format("%s: %s", key, value);
                    }).collect(Collectors.toList());
                    log.info("Allocatable: {}", String.join("|", allocatable));
                    for (CONDITION_TYPE type : CONDITION_TYPE.values()) {
                        getCondition(type.name(), nodeConditionMap);
                    }
                    log.info("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
                }
            }
        } catch (ApiException ex) {
            if ( ex.getCode() == 504 || ex.getCode() == 410 ) {
                resourceVersion = extractResourceVersionFromException(ex);
            }
            else {
                resourceVersion = null;
            }
        }
        return resourceVersion;
    }

    public String resourceVersion(CoreV1Api api) {
        String resourceVersion = null;
        try {
            resourceVersion = monitor(api);
        } catch (IOException e) {
            log.error("Failed to get resource version!!!");
        }
        return resourceVersion;
    }

    private static void getCondition(String conditionType, Map<String, V1NodeCondition> v1NodeConditionMap) {
        if(Objects.nonNull(v1NodeConditionMap.get(conditionType))) {
            log.info(" - Type: {}| Reason: {}| Status: {}", v1NodeConditionMap.get(conditionType).getType(), v1NodeConditionMap.get(conditionType).getReason(), v1NodeConditionMap.get(conditionType).getStatus());
        }
    }

    private enum CONDITION_TYPE {
        DiskPressure, MemoryPressure, PIDPressure, KubeletReady
    }

}