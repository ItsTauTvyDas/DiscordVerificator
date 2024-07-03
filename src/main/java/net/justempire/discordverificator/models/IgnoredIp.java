package net.justempire.discordverificator.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class IgnoredIp {
    @JsonProperty("ipToIgnore")
    private String ipToIgnore;

    @JsonProperty("ignoreUntil")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date ignoreUntil;

    public IgnoredIp() { }

    public IgnoredIp(String ipToIgnore, Date ignoreUntil) {
        this.ipToIgnore = ipToIgnore;
        this.ignoreUntil = ignoreUntil;
    }

    public String getIpToIgnore() {
        return ipToIgnore;
    }

    public Date getIgnoreUntil() {
        return ignoreUntil;
    }
}
