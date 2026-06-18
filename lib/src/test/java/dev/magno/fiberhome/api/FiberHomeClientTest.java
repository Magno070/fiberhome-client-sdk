package dev.magno.fiberhome.api;

import dev.magno.fiberhome.models.Onu;
import dev.magno.fiberhome.models.Capability;
import dev.magno.fiberhome.models.FiberHomeVersion;
import dev.magno.fiberhome.parser.OnuParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FiberHomeClientTest {

    @Test
    void testBuilderCreatesClientWithBothConnections() {
        FiberHomeClient client = new FiberHomeClient.Builder()
                .tl1("127.0.0.1", 3083, "admin", "password")
                .snmp("127.0.0.1", 161, "public")
                .build();

        assertNotNull(client);
        assertNotNull(client.getTl1Connection());
        assertNotNull(client.getSnmpConnection());
        assertEquals("127.0.0.1", client.getTl1Connection().getHost());
        assertEquals(3083, client.getTl1Connection().getPort());
        assertEquals("127.0.0.1", client.getSnmpConnection().getHost());
        assertEquals(161, client.getSnmpConnection().getPort());
        assertEquals("public", client.getSnmpConnection().getCommunity());
        assertEquals(FiberHomeVersion.UNKNOWN, client.getOltVersion());
    }

    @Test
    void testBuilderWithCustomOltVersion() {
        FiberHomeClient client = new FiberHomeClient.Builder()
                .tl1("127.0.0.1", 3083, "admin", "password")
                .oltVersion(FiberHomeVersion.RP1000)
                .build();

        assertNotNull(client);
        assertEquals(FiberHomeVersion.RP1000, client.getOltVersion());
    }

    @Test
    void testBuilderRequiresAtLeastOneConnection() {
        assertThrows(IllegalStateException.class, () -> {
            new FiberHomeClient.Builder().build();
        });
    }

    @Test
    void testOltVersionCapabilities() {
        // RP1000 should support both capabilities
        assertTrue(FiberHomeVersion.RP1000.supports(Capability.GET_ONUS_FIXED_INFO));
        assertTrue(FiberHomeVersion.RP1000.supports(Capability.EXECUTE_RAW_TL1));

        // RP0700 should only support GET_ONUS_FIXED_INFO
        assertTrue(FiberHomeVersion.RP0700.supports(Capability.GET_ONUS_FIXED_INFO));
        assertFalse(FiberHomeVersion.RP0700.supports(Capability.EXECUTE_RAW_TL1));

        // RP1200 and UNKNOWN should support none
        assertFalse(FiberHomeVersion.RP1200.supports(Capability.GET_ONUS_FIXED_INFO));
        assertFalse(FiberHomeVersion.UNKNOWN.supports(Capability.GET_ONUS_FIXED_INFO));
    }

    @Test
    void testOnuParserWithSampleTl1Response() {
        String sampleResponse = 
                "   OLT-1 2026-06-17 20:54:13\n" +
                "M  1 COMPLD\n" +
                "   \"OLTID=1,SLOTID=2,PONID=3,ONUID=4,SN=FHTT12345678,NAME=TestONU,STATUS=Online\"\n" +
                "   \"OLTID=1,SLOTID=2,PONID=3,ONUID=5,SN=FHTT87654321,NAME=SecondONU,STATUS=Offline\"\n" +
                ";";

        List<Onu> onus = OnuParser.parseList(sampleResponse);
        assertEquals(2, onus.size());

        Onu first = onus.get(0);
        assertEquals("1", first.getOltId());
        assertEquals(2, first.getSlotId());
        assertEquals(3, first.getPonPort());
        assertEquals(4, first.getOnuId());
        assertEquals("FHTT12345678", first.getSn());
        assertEquals("TestONU", first.getName());
        assertEquals("Online", first.getStatus());

        Onu second = onus.get(1);
        assertEquals("1", second.getOltId());
        assertEquals(2, second.getSlotId());
        assertEquals(3, second.getPonPort());
        assertEquals(5, second.getOnuId());
        assertEquals("FHTT87654321", second.getSn());
        assertEquals("SecondONU", second.getName());
        assertEquals("Offline", second.getStatus());
    }
}
