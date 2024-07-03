package net.justempire.discordverificator.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class LatestVerificationFromIp {
    @JsonProperty("ip")
    private String ip;

    @JsonProperty("sent")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date sent;

    public LatestVerificationFromIp() { }

    public LatestVerificationFromIp(String ip, Date sent) {
        this.ip = ip;
        this.sent = sent;
    }

    public String getIp() {
        return ip;
    }

    public Date getSentDate() {
        return sent;
    }

    public void setSentDate(Date sent) {
        this.sent = sent;
    }
}
