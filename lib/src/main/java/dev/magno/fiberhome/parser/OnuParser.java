package dev.magno.fiberhome.parser;

import dev.magno.fiberhome.models.Onu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OnuParser {
    // Regex to match quoted blocks in the TL1 response
    private static final Pattern QUOTED_PATTERN = Pattern.compile("\"([^\"]+)\"");

    public static List<Onu> parseList(String rawList) {
        List<Onu> onus = new ArrayList<>();
        if (rawList == null || rawList.isEmpty()) {
            return onus;
        }

        Matcher matcher = QUOTED_PATTERN.matcher(rawList);
        while (matcher.find()) {
            String quotedContent = matcher.group(1);
            Onu onu = parseOnuLine(quotedContent);
            if (onu != null) {
                onus.add(onu);
            }
        }
        return onus;
    }

    private static Onu parseOnuLine(String content) {
        // Can be structured like: OLTID=1,SLOTID=2,PONID=3,ONUID=4,SN=FHTT12345678,NAME=Test,STATUS=Online
        String data = content;
        int colonIndex = content.indexOf(':');
        if (colonIndex != -1 && !content.substring(0, colonIndex).contains("=")) {
            // Strip any label prefix (e.g. "ONU-1:OLTID=...")
            data = content.substring(colonIndex + 1);
        }

        Map<String, String> pairs = new HashMap<>();
        String[] tokens = data.split(",");
        for (String token : tokens) {
            String[] kv = token.split("=");
            if (kv.length == 2) {
                pairs.put(kv[0].trim().toUpperCase(), kv[1].trim());
            }
        }

        if (pairs.isEmpty()) {
            return null;
        }

        Onu onu = new Onu();
        onu.setOltId(pairs.getOrDefault("OLTID", ""));
        onu.setSn(pairs.getOrDefault("SN", pairs.getOrDefault("MAC", "")));
        onu.setName(pairs.getOrDefault("NAME", ""));
        onu.setStatus(pairs.getOrDefault("STATUS", ""));

        try {
            onu.setSlotId(Integer.parseInt(pairs.getOrDefault("SLOTID", "0")));
        } catch (NumberFormatException e) {
            onu.setSlotId(0);
        }

        try {
            String ponVal = pairs.getOrDefault("PONID", pairs.getOrDefault("PONPORT", "0"));
            onu.setPonPort(Integer.parseInt(ponVal));
        } catch (NumberFormatException e) {
            onu.setPonPort(0);
        }

        try {
            onu.setOnuId(Integer.parseInt(pairs.getOrDefault("ONUID", "0")));
        } catch (NumberFormatException e) {
            onu.setOnuId(0);
        }

        return onu;
    }
}
