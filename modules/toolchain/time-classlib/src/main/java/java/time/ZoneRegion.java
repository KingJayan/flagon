package java.time;

import java.time.zone.ZoneRules;
import java.time.zone.ZoneRulesException;

final class ZoneRegion extends ZoneId {

    private final String id;
    private final ZoneRules rules;

    ZoneRegion(String id, ZoneRules rules) {
        this.id = id;
        this.rules = rules;
    }

    static ZoneRegion ofId(String zoneId) {
        if (!zoneId.matches("[A-Za-z][A-Za-z0-9~/._+-]+")) {
            throw new DateTimeException("Invalid ID for region-based ZoneId, invalid format: "
                    + zoneId);
        }
        ZoneRules rules = ZoneRules.ofRegion(zoneId);
        if (rules == null) {
            throw new ZoneRulesException("Unknown time-zone ID: " + zoneId);
        }
        return new ZoneRegion(zoneId, rules);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ZoneRules getRules() {
        return rules;
    }
}
