package io.k8cluster.monitors;

import com.google.gson.Gson;
import io.kubernetes.client.openapi.ApiException;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Setter
public abstract class EventMonitor implements Monitor {
    private String resourceVersion;

    public Class<? extends EventMonitor> getCreatedClass() {
        return this.getClass();
    }

    public String extractResourceVersionFromException(ApiException ex) {
        String body = ex.getResponseBody();
        if (body == null) {
            return null;
        }
        Gson gson = new Gson();
        Map<?,?> st = gson.fromJson(body, Map.class);
        Pattern p = Pattern.compile("Timeout: Too large resource version: (\\d+), current: (\\d+)");
        String msg = (String)st.get("message");
        Matcher m = p.matcher(msg);
        if (!m.matches()) {
            return null;
        }

        return m.group(2);
    }
}
