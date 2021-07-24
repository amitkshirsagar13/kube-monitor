package io.k8cluster.monitor.tracer;

import io.k8cluster.monitor.tracer.common.V1Status;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Pod;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static io.k8cluster.monitor.tracer.values.EVENT_TYPE.SPAN;
import static io.k8cluster.monitor.tracer.values.SPAN.POD;

@Slf4j
@Service
public class PodTracer extends SpanProvider {
    public void tracePodEvent(V1Pod event) {
        final String clusterName = event.getMetadata().getClusterName();
        final String nameSpace = event.getMetadata().getNamespace();
        final String podName = event.getMetadata().getName();

        final String spanContextId = getSpanContextId(nameSpace, podName);
        final SpanContext spanContext = buildOrGetSpanContext(SPAN, POD, spanContextId);
        Tracer tracer = GlobalTracer.get();
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(POD.name());
        spanBuilder.asChildOf(spanContext);
        Span podStatus = spanBuilder.start();
        podStatus.setBaggageItem("clusterName", clusterName);

        final String resourceVersion = event.getMetadata().getResourceVersion();
        final List<String> imageList = event.getSpec().getContainers().stream().map(V1Container::getImage).collect(Collectors.toList());
        podStatus.setBaggageItem("Resource_Version", resourceVersion);
        imageList.stream().forEach(image->podStatus.setBaggageItem("Image", image));

        final List<V1Status> statusList = event.getStatus().getConditions().stream()
                .map(v1PodCondition -> getModelMapper().map(v1PodCondition, V1Status.class))
                .collect(Collectors.toList());

        statusList.stream().forEach(status -> {
            Tracer tracerStatus = GlobalTracer.get();
            Tracer.SpanBuilder spanBuilderStatus = tracerStatus.buildSpan(POD.name());
            spanBuilderStatus.asChildOf(podStatus);
            Span statusCondition = spanBuilder.start();
            statusCondition.log(String.format("TransitionTime: %s", getDisplayValue(status.getLastTransitionTime())));
            statusCondition.log(String.format("Reason: %s", getDisplayValue(status.getReason())));
            statusCondition.log(String.format("Message: %s", getDisplayValue(status.getMessage())));
            statusCondition.log(String.format("Status: %s", getDisplayValue(status.getStatus().toString())));
            statusCondition.finish();
        });
        podStatus.finish();
    }

}
