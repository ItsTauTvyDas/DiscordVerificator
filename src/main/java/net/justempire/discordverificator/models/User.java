package net.justempire.discordverificator.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.justempire.discordverificator.exceptions.MinecraftUsernameAlreadyLinkedException;
import net.justempire.discordverificator.exceptions.NoVerificationsFoundException;
import net.justempire.discordverificator.exceptions.NotFoundException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@JsonAutoDetect
public class User {
    @JsonProperty("discordId")
    private String discordId;

    @JsonProperty("linkedMinecraftUsernames")
    public List<String> linkedMinecraftUsernames;

    @JsonProperty("ignoredIps")
    private List<IgnoredIp> ignoredIps;

    @JsonProperty("latestVerificationsFromIps")
    private List<LatestVerificationFromIp> latestVerificationsFromIps;

    @JsonProperty("currentAllowedIp")
    private String currentAllowedIp;

    public User() { }

    public User(String discordUsername, List<String> minecraftUsernames, List<IgnoredIp> ignoredIps, List<LatestVerificationFromIp> latestVerificationsFromIps, String currentAllowedIp) {
        this.discordId = discordUsername;
        this.linkedMinecraftUsernames = minecraftUsernames;
        this.ignoredIps = ignoredIps;
        this.latestVerificationsFromIps = latestVerificationsFromIps;
        this.currentAllowedIp = currentAllowedIp;
    }

    public void setCurrentAllowedIp(String currentAllowedIp) {
        this.currentAllowedIp = currentAllowedIp;
    }

    public void updateLatestVerificationTimeFromIp(String ip) {
        if (latestVerificationsFromIps == null) latestVerificationsFromIps = new ArrayList<>();

        Date now = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        for (LatestVerificationFromIp verification : latestVerificationsFromIps) {
            if (!ip.equalsIgnoreCase(verification.getIp())) continue;

            verification.setSentDate(now);
            return;
        }

        // If not found, then create
        LatestVerificationFromIp verificationFromIp = new LatestVerificationFromIp(ip, now);
        latestVerificationsFromIps.add(verificationFromIp);
    }

    public Date getLatestVerificationTimeFromIp(String ip) throws NoVerificationsFoundException {
        for (LatestVerificationFromIp verification : latestVerificationsFromIps) {
            if (verification.getIp().equalsIgnoreCase(ip))
                return verification.getSentDate();
        }

        throw new NoVerificationsFoundException();
    }

    public String getDiscordId() {
        return discordId;
    }

    public String getCurrentAllowedIp() {
        return currentAllowedIp;
    }

    public boolean isIpIgnored(String ip) {
        if (ignoredIps == null) {
            ignoredIps = new ArrayList<>();
            return false;
        }

        Date now = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());

        for (IgnoredIp ignoredIp : ignoredIps) {
            if (!ignoredIp.getIpToIgnore().equalsIgnoreCase(ip)) continue;
            if (now.before(ignoredIp.getIgnoreUntil())) return true;
            else {
                ignoredIps.remove(ignoredIp);
                return false;
            }
        }

        return false;
    }

    public void ignoreIp(String ip, Date until) {
        if (ignoredIps == null) ignoredIps = new ArrayList<>();

        if (isIpIgnored(ip)) return;

        IgnoredIp ignoredIp = new IgnoredIp(ip, until);
        ignoredIps.add(ignoredIp);
    }

    public void unIgnoreIp(String ip) {
        for (int i = 0; i < ignoredIps.size(); i++)
        {
            IgnoredIp ignoredIp = ignoredIps.get(i);
            if (ignoredIp.getIpToIgnore().equalsIgnoreCase(ip))
            {
                ignoredIps.remove(ignoredIp);
                i--;
            }
        }
    }

    public boolean isMinecraftUsernameLinked(String username) {
        for (String linkedName : linkedMinecraftUsernames) {
            if (linkedName.equalsIgnoreCase(username)) return true;
        }

        return false;
    }

    public void linkMinecraftUsername(String username) throws MinecraftUsernameAlreadyLinkedException {
        for (String linkedName : linkedMinecraftUsernames) {
            if (linkedName.equalsIgnoreCase(username))
                throw new MinecraftUsernameAlreadyLinkedException();
        }

        linkedMinecraftUsernames.add(username);
    }

    public void unlinkMinecraftUsername(String username) throws NotFoundException {
        for (String linkedName : linkedMinecraftUsernames) {
            if (linkedName.equalsIgnoreCase(username)) {
                linkedMinecraftUsernames.remove(linkedName);
                return;
            }
        }

        throw new NotFoundException();
    }
}
