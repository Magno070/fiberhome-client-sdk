package dev.magno.fiberhome.api;

import dev.magno.fiberhome.models.Onu;
import dev.magno.fiberhome.models.Capability;
import dev.magno.fiberhome.models.FiberHomeVersion;
import dev.magno.fiberhome.connection.Tl1Connection;
import dev.magno.fiberhome.connection.SnmpConnection;
import dev.magno.fiberhome.parser.OnuParser;

import java.util.List;
import java.util.logging.Logger;

public class FiberHomeClient implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(FiberHomeClient.class.getName());

    private final Tl1Connection tl1Connection;
    private final SnmpConnection snmpConnection;
    private final FiberHomeVersion oltVersion;

    // Package-private constructor, forcing Builder usage
    FiberHomeClient(Tl1Connection tl1, SnmpConnection snmp, FiberHomeVersion oltVersion) {
        this.tl1Connection = tl1;
        this.snmpConnection = snmp;
        this.oltVersion = oltVersion;
    }

    public void connect() {
        if (tl1Connection != null) tl1Connection.connect();
        if (snmpConnection != null) snmpConnection.connect();
    }

    /**
     * Checks if the configured OLT version supports the specified capability.
     * Logs a warning if the capability is not certified or supported.
     */
    private void ensureCapability(Capability capability) {
        if (oltVersion != null && !oltVersion.supports(capability)) {
            LOGGER.warning(String.format(
                "WARNING: Capability '%s' is not officially certified/tested on OLT version '%s'. " +
                "This action might behave unexpectedly or fail.",
                capability, oltVersion
            ));
        }
    }

    // High-Level API Layer (Only visible to the user)
    public List<Onu> getOnusFixedInfo() {
        ensureCapability(Capability.GET_ONUS_FIXED_INFO);

        if (tl1Connection == null) {
            throw new IllegalStateException("TL1 Connection is not configured.");
        }
        // 1. Execute raw command on transport
        String rawTl1Response = tl1Connection.execute("LST-ONU::OLTID=1;");

        // 2. Pass to Parser to convert text to typed objects
        return OnuParser.parseList(rawTl1Response);
    }

    // Escape method (Required by requirements)
    public String executeRawTl1(String command) {
        ensureCapability(Capability.EXECUTE_RAW_TL1);

        if (tl1Connection == null) {
            throw new IllegalStateException("TL1 Connection is not configured.");
        }
        return tl1Connection.execute(command);
    }

    public Tl1Connection getTl1Connection() {
        return tl1Connection;
    }

    public SnmpConnection getSnmpConnection() {
        return snmpConnection;
    }

    public FiberHomeVersion getOltVersion() {
        return oltVersion;
    }

    @Override
    public void close() {
        if (tl1Connection != null) {
            try {
                tl1Connection.disconnect();
            } catch (Exception ignored) {}
        }
        if (snmpConnection != null) {
            try {
                snmpConnection.disconnect();
            } catch (Exception ignored) {}
        }
    }

    // Builder Structure
    public static class Builder {
        private Tl1Connection.Builder tl1Builder;
        private SnmpConnection.Builder snmpBuilder;
        private FiberHomeVersion oltVersion = FiberHomeVersion.UNKNOWN;

        public Builder oltVersion(FiberHomeVersion version) {
            this.oltVersion = version;
            return this;
        }

        public Builder tl1(String host, int port, String username, String password) {
            this.tl1Builder = new Tl1Connection.Builder()
                    .host(host)
                    .port(port)
                    .username(username)
                    .password(password);
            return this;
        }

        public Builder tl1(Tl1Connection.Builder builder) {
            this.tl1Builder = builder;
            return this;
        }

        public Builder snmp(String host, int port, String community) {
            this.snmpBuilder = new SnmpConnection.Builder()
                    .host(host)
                    .port(port)
                    .community(community);
            return this;
        }

        public Builder snmp(SnmpConnection.Builder builder) {
            this.snmpBuilder = builder;
            return this;
        }

        public FiberHomeClient build() {
            Tl1Connection tl1 = (tl1Builder != null) ? tl1Builder.build() : null;
            SnmpConnection snmp = (snmpBuilder != null) ? snmpBuilder.build() : null;
            if (tl1 == null && snmp == null) {
                throw new IllegalStateException("At least one connection (TL1 or SNMP) must be configured.");
            }
            return new FiberHomeClient(tl1, snmp, oltVersion);
        }
    }
}