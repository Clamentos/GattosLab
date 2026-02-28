package io.github.clamentos.gattoslab.configuration.mappers;

///
import io.github.clamentos.gattoslab.configuration.DynamicPropertyType;
import io.github.clamentos.gattoslab.persistence.EntityField;

///..
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

///..
import org.bson.Document;

///
public final class BlacklistMapper implements DynamicPropertyMapper {

    ///
    @Override
    public DynamicPropertyType forType() {

        return DynamicPropertyType.BLACKLIST;
    }

    ///..
    @Override
    public Object map(final Document document) throws IllegalArgumentException {

        if(document == null) throw new IllegalArgumentException("Dynamic property field \"value\" cannot be null");

        final Map<String, List<?>> property = new HashMap<>();
        final List<String> userAgentContains = new ArrayList<>();

        property.put(EntityField.IPV4S.getField(), this.ipList(document, true));
        property.put(EntityField.IPV6S.getField(), this.ipList(document, false));

        for(final Object contains : document.get(EntityField.USER_AGENT_CONTAINS.getField(), List.class)) {

            userAgentContains.add((String)contains);
        }

        property.put(EntityField.USER_AGENT_CONTAINS.getField(), userAgentContains);
        return property;
    }

    ///.
    private List<Map<String, byte[]>> ipList(final Document document, final boolean isV4) throws IllegalArgumentException {

        final List<?> ranges = document.get(isV4 ? EntityField.IPV4S.getField() : EntityField.IPV6S.getField(), List.class);
        if(ranges == null) return List.of();

        final List<Map<String, byte[]>> ips = new ArrayList<>();

        for(final Object range : ranges) {

            if(range == null) throw new IllegalArgumentException("Range entries cannot be null");

            final Document rangeDocument = (Document)range;
            final String startIp = rangeDocument.getString(EntityField.START.getField());
            final String endIp = rangeDocument.getString(EntityField.END.getField());

            if(startIp == null || endIp == null) throw new IllegalArgumentException("Start and end addresses cannot be null");

            ips.add(Map.of(

                EntityField.START.getField(), InetAddress.ofLiteral(startIp).getAddress(),
                EntityField.END.getField(), InetAddress.ofLiteral(endIp).getAddress()
            ));
        }

        return ips;
    }

    ///
}
