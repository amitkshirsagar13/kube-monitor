package io.k8cluster.monitor;

import io.k8cluster.monitor.monitors.EventMonitor;
import io.k8cluster.monitor.monitors.NodeMonitor;
import io.k8cluster.monitor.monitors.PodMonitor;
import io.k8cluster.monitor.monitors.PvMonitor;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootApplication
public class ClusterMonitorApplication {
    @Autowired
    private NodeMonitor nodeMonitor;
    @Autowired
    private PvMonitor pvMonitor;
    @Autowired
    private PodMonitor podMonitor;

    @SuppressWarnings("resource")
    public static void main(final String[] args) {
        SpringApplication.run(ClusterMonitorApplication.class, args);
    }

    private List<EventMonitor> eventMonitorList;

    @Autowired
    private ApplicationAvailability applicationAvailability;

    @EventListener(ApplicationReadyEvent.class)
    public void startMonitoringAfterStartup() throws IOException {
        availability();

        ApiClient client  = Config.defaultClient();
        OkHttpClient newClient = client.getHttpClient()
                .newBuilder()
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        client.setHttpClient(newClient);
        CoreV1Api api = new CoreV1Api(client);

        initMap(api);

        while (true) {
            eventMonitorList.parallelStream().forEach(eventMonitor->{
                try {
                    final String resourceVersion = eventMonitor.monitor(api);
                    eventMonitor.setResourceVersion(resourceVersion);
                } catch (IOException | ApiException e) {
                    log.error("Failed getting Events for eventMonitor : {}", eventMonitor.getCreatedClass());
                }
            });
        }
    }

    private void availability() {
        // Available as a component in the application context
        ApplicationAvailability availability;
        LivenessState livenessState = applicationAvailability.getLivenessState();
        ReadinessState readinessState = applicationAvailability.getReadinessState();
    }

    private void initMap(CoreV1Api api) {
        eventMonitorList = Arrays.asList(podMonitor);
        eventMonitorList.parallelStream().forEach(eventMonitor-> {
            try {
                eventMonitor.setResourceVersion(eventMonitor.resourceVersion(api));
            } catch (ApiException e) {
                log.error("Failed getting Resource version for eventMonitor : {}", eventMonitor.getCreatedClass());
            }
        });
    }
}
