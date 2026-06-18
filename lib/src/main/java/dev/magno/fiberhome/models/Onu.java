package dev.magno.fiberhome.models;

public class Onu {
    private String oltId;
    private int slotId;
    private int ponPort;
    private int onuId;
    private String sn;
    private String name;
    private String status;

    public Onu() {}

    public Onu(String oltId, int slotId, int ponPort, int onuId, String sn, String name, String status) {
        this.oltId = oltId;
        this.slotId = slotId;
        this.ponPort = ponPort;
        this.onuId = onuId;
        this.sn = sn;
        this.name = name;
        this.status = status;
    }

    public String getOltId() { return oltId; }
    public void setOltId(String oltId) { this.oltId = oltId; }

    public int getSlotId() { return slotId; }
    public void setSlotId(int slotId) { this.slotId = slotId; }

    public int getPonPort() { return ponPort; }
    public void setPonPort(int ponPort) { this.ponPort = ponPort; }

    public int getOnuId() { return onuId; }
    public void setOnuId(int onuId) { this.onuId = onuId; }

    public String getSn() { return sn; }
    public void setSn(String sn) { this.sn = sn; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Onu{" +
                "oltId='" + oltId + '\'' +
                ", slotId=" + slotId +
                ", ponPort=" + ponPort +
                ", onuId=" + onuId +
                ", sn='" + sn + '\'' +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
