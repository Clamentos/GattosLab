package io.github.clamentos.gattoslab.configuration.dynamic.mappers;

///
import io.github.clamentos.gattoslab.configuration.dynamic.pojos.BlacklistDynamicProperty;
import io.github.clamentos.gattoslab.configuration.dynamic.pojos.BlacklistIpEntry;
import io.github.clamentos.gattoslab.persistence.EntityField;

///..
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

///..
import org.bson.BsonReader;
import org.bson.BsonType;

///
public final class BlacklistMapper implements DynamicPropertySubMapper<BlacklistDynamicProperty> {

    ///
    @Override
    public BlacklistDynamicProperty map(final BsonReader reader) throws IllegalArgumentException {

        List<BlacklistIpEntry> ipv4s = null;
        List<BlacklistIpEntry> ipv6s = null;
        Set<String> userAgentContains = null;

        reader.readStartDocument();

        while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

            final String name = reader.readName();

            switch(name) {

                case EntityField.IPV4S: ipv4s = this.readIps(reader); break;
                case EntityField.IPV6S: ipv6s = this.readIps(reader); break;

                case EntityField.USER_AGENT_CONTAINS:

                    reader.readStartArray();
                    userAgentContains = new HashSet<>();
                    while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) userAgentContains.add(reader.readString());
                    reader.readEndArray();

                break;

                default: throw new IllegalArgumentException("Unknown field name " + name);
            }
        }

        reader.readEndDocument();
        return new BlacklistDynamicProperty(ipv4s, ipv6s, userAgentContains);
    }

    ///.
    private List<BlacklistIpEntry> readIps(final BsonReader reader) throws IllegalArgumentException {

        final List<BlacklistIpEntry> ips = new ArrayList<>();
        reader.readStartArray();

        while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

            String startAddress = null;
            String endAddress = null;

            final String name = reader.readName();

            switch(name) {

                case EntityField.START: startAddress = reader.readString(); break;
                case EntityField.END: endAddress = reader.readString(); break;

                default: throw new IllegalArgumentException("Unknown field name " + name);
            }

            ips.add(new BlacklistIpEntry(InetAddress.ofLiteral(startAddress).getAddress(), InetAddress.ofLiteral(endAddress).getAddress()));
        }

        reader.readEndArray();
        return ips;
    }

    ///
}
