package dev.magno.fiberhome.models;

import java.util.EnumSet;
import java.util.Set;

/**
 * Represents the FiberHome OLT version and its certified/tested capabilities.
 */
public enum FiberHomeVersion {
    /**
     * RP0700 version. Supports retrieving fixed ONU information.
     */
    RP0700(EnumSet.of(
        Capability.GET_ONUS_FIXED_INFO
    )),

    /**
     * RP1000 version. Fully supported, including raw commands and fixed ONU info retrieval.
     */
    RP1000(EnumSet.of(
        Capability.GET_ONUS_FIXED_INFO,
        Capability.EXECUTE_RAW_TL1
    )),

    /**
     * RP1200 version. Currently not certified by this SDK version.
     */
    RP1200(EnumSet.noneOf(Capability.class)),

    /**
     * Unknown version. No capabilities are guaranteed.
     */
    UNKNOWN(EnumSet.noneOf(Capability.class));

    private final Set<Capability> supportedCapabilities;

    FiberHomeVersion(Set<Capability> supportedCapabilities) {
        this.supportedCapabilities = supportedCapabilities;
    }

    /**
     * Checks if this version officially supports the specified capability.
     *
     * @param capability The capability to check.
     * @return true if supported, false otherwise.
     */
    public boolean supports(Capability capability) {
        return this.supportedCapabilities.contains(capability);
    }
}
