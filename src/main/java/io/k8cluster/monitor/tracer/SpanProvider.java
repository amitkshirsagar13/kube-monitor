package io.k8cluster.monitor.tracer;

import io.k8cluster.monitor.tracer.values.EVENT_TYPE;
import io.k8cluster.monitor.tracer.values.SPAN;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public abstract class SpanProvider {
    private Map<String, SpanContext> spanContextMap;
    private ModelMapper modelMapper;

    @PostConstruct
    public void onStartUp() {
        spanContextMap = new HashMap<>();
        modelMapper = new ModelMapper();
    }

    public SpanContext buildOrGetSpanContext(EVENT_TYPE eventType, SPAN span, String spanContextId) {
        final SpanContext parentSpanContext = spanContextMap.computeIfAbsent(spanContextId, spanContext -> {
            Tracer tracer = GlobalTracer.get();
            Tracer.SpanBuilder spanBuilder = tracer.buildSpan(span.name())
                    .withTag(Tags.SPAN_KIND.getKey(), span.name());
            Span traceSpan = spanBuilder.start();
            Tags.COMPONENT.set(traceSpan, spanContextId);
            traceSpan.setTag("EVENT_TYPE", eventType.name());
            traceSpan.setTag("context", spanContextId);
            traceSpan.finish();
            return traceSpan.context();
        });
        return parentSpanContext;
    }

    public String getSpanContextId(String nameSpace, String podName) {
        return String.format("%s-%s", nameSpace, podName);
    }

    public ModelMapper getModelMapper() {
        return modelMapper;
    }

    public String getDisplayValue(String displayValue) {
        return StringUtils.isNotEmpty(displayValue) ? displayValue : "No Value";
    }
}
