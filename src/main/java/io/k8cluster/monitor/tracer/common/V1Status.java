package io.k8cluster.monitor.tracer.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class V1Status {
    private String lastProbeTime;
    private String lastTransitionTime;
    private String message;
    private String reason;
    private Boolean status;
    private String type;
}
