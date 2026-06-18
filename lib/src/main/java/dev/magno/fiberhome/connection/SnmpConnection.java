package dev.magno.fiberhome.connection;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnmpConnection {
    private final String host;
    private final int port;
    private final String community;
    private final int snmpVersion;
    
    private Snmp snmp;
    private CommunityTarget<Address> target;
    private boolean connected;

    private SnmpConnection(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.community = builder.community;
        this.snmpVersion = builder.snmpVersion;
    }

    public synchronized void connect() {
        if (connected) {
            return;
        }
        try {
            TransportMapping<? extends Address> transport = new DefaultUdpTransportMapping();
            this.snmp = new Snmp(transport);
            transport.listen();

            Address targetAddress = GenericAddress.parse("udp:" + host + "/" + port);
            this.target = new CommunityTarget<>();
            target.setCommunity(new OctetString(community));
            target.setAddress(targetAddress);
            target.setRetries(2);
            target.setTimeout(1500);
            target.setVersion(snmpVersion);

            this.connected = true;
        } catch (IOException e) {
            disconnect();
            throw new RuntimeException("Failed to initialize SNMP connection: " + e.getMessage(), e);
        }
    }

    public synchronized void disconnect() {
        if (!connected) {
            return;
        }
        try {
            if (snmp != null) {
                snmp.close();
            }
        } catch (IOException e) {
            // Ignore close issues
        } finally {
            connected = false;
            snmp = null;
            target = null;
        }
    }

    public synchronized String get(String oidStr) {
        if (!connected) {
            throw new IllegalStateException("SNMP Connection is not connected.");
        }
        try {
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oidStr)));
            pdu.setType(PDU.GET);

            ResponseEvent<Address> response = snmp.send(pdu, target);
            if (response != null && response.getResponse() != null) {
                PDU responsePDU = response.getResponse();
                if (responsePDU.getErrorStatus() == PDU.noError) {
                    VariableBinding vb = responsePDU.get(0);
                    if (vb != null && vb.getVariable() != null) {
                        return vb.getVariable().toString();
                    }
                } else {
                    throw new RuntimeException("SNMP GET Error Status: " + responsePDU.getErrorStatusText());
                }
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException("SNMP GET exception: " + e.getMessage(), e);
        }
    }

    public synchronized Map<String, String> walk(String rootOidStr) {
        if (!connected) {
            throw new IllegalStateException("SNMP Connection is not connected.");
        }
        Map<String, String> results = new HashMap<>();
        try {
            TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
            OID rootOid = new OID(rootOidStr);
            List<TreeEvent> events = treeUtils.getSubtree(target, rootOid);
            if (events == null || events.isEmpty()) {
                return results;
            }
            for (TreeEvent event : events) {
                if (event == null) continue;
                if (event.isError()) {
                    throw new RuntimeException("SNMP Walk Error: " + event.getErrorMessage());
                }
                VariableBinding[] vbs = event.getVariableBindings();
                if (vbs != null) {
                    for (VariableBinding vb : vbs) {
                        if (vb != null && vb.getOid() != null && vb.getVariable() != null) {
                            results.put(vb.getOid().toString(), vb.getVariable().toString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("SNMP Walk Exception: " + e.getMessage(), e);
        }
        return results;
    }

    public String execute(String oidStr) {
        return get(oidStr);
    }

    public boolean isConnected() {
        return connected;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getCommunity() {
        return community;
    }

    public static class Builder {
        private String host;
        private int port = 161; // Default SNMP port
        private String community = "public"; // Default community
        private int snmpVersion = SnmpConstants.version2c; // Default version

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder community(String community) {
            this.community = community;
            return this;
        }

        public Builder version2c() {
            this.snmpVersion = SnmpConstants.version2c;
            return this;
        }

        public Builder version1() {
            this.snmpVersion = SnmpConstants.version1;
            return this;
        }

        public SnmpConnection build() {
            if (host == null || host.isEmpty()) {
                throw new IllegalStateException("Host must be set.");
            }
            return new SnmpConnection(this);
        }
    }
}
