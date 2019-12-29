package com.mikerusoft.redirect.to.stream.utils;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("receiver")
// I can't use lombok here, since micronaut, seems, scans class before lombok generates getters and setters
public class UrlReceiverProperties {
    private int inactiveUrlExpireSec;
    private int globalFrequencyCount;
    private int globalStatus;
    private long globalDelayMs;
    private int maxSizeOfDelayedRequests;

    public int getInactiveUrlExpireSec() { return inactiveUrlExpireSec;}
    public void setInactiveUrlExpireSec(int inactiveUrlExpireSec) { this.inactiveUrlExpireSec = inactiveUrlExpireSec; }
    public int getGlobalFrequencyCount() { return globalFrequencyCount; }
    public void setGlobalFrequencyCount(int globalFrequencyCount) { this.globalFrequencyCount = globalFrequencyCount; }
    public int getGlobalStatus() { return globalStatus; }
    public void setGlobalStatus(int globalStatus) { this.globalStatus = globalStatus; }
    public long getGlobalDelayMs() { return globalDelayMs; }
    public void setGlobalDelayMs(long globalDelayMs) { this.globalDelayMs = globalDelayMs; }
    public int getMaxSizeOfDelayedRequests() { return maxSizeOfDelayedRequests; }
    public void setMaxSizeOfDelayedRequests(int maxSizeOfDelayedRequests) { this.maxSizeOfDelayedRequests = maxSizeOfDelayedRequests; }
}
