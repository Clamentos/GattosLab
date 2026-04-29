package io.github.clamentos.gattoslab.configuration.dynamic.mappers;

///
import io.github.clamentos.gattoslab.configuration.dynamic.pojos.BlacklistDynamicProperty;
import io.github.clamentos.gattoslab.configuration.dynamic.pojos.BlacklistIpEntry;
import io.github.clamentos.gattoslab.exceptions.CodecException;
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
    public BlacklistDynamicProperty map(final BsonReader reader) throws CodecException {

        try {

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

                    default: throw new CodecException("BlacklistMapper.map~Unknown field name " + name);
                }
            }

            reader.readEndDocument();
            return new BlacklistDynamicProperty(ipv4s, ipv6s, userAgentContains);
        }

        catch(final IllegalStateException exc) {

            throw new CodecException("BlacklistMapper.map~" + exc.getMessage(), exc);
        }
    }

    ///.
    private List<BlacklistIpEntry> readIps(final BsonReader reader) throws CodecException {

        try {

            final List<BlacklistIpEntry> ips = new ArrayList<>();
            reader.readStartArray();

            while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

                reader.readStartDocument();

                String startAddress = null;
                String endAddress = null;

                while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

                    final String name = reader.readName();

                    switch(name) {

                        case EntityField.START: startAddress = reader.readString(); break;
                        case EntityField.END: endAddress = reader.readString(); break;

                        default: throw new CodecException("BlacklistMapper.readIps~Unknown field name " + name);
                    }
                }

                reader.readEndDocument();

                if(startAddress != null && startAddress.compareTo(endAddress) > 0) {

                    throw new CodecException("BlacklistMapper.readIps~Start address cannot be greater then end address: " + startAddress + " -> " + endAddress);
                }

                ips.add(new BlacklistIpEntry(InetAddress.ofLiteral(startAddress).getAddress(), InetAddress.ofLiteral(endAddress).getAddress()));
            }

            reader.readEndArray();
            return ips;
        }

        catch(final IllegalStateException exc) {

            throw new CodecException("BlacklistMapper.readIps~" + exc.getMessage(), exc);
        }
    }

    ///
}
