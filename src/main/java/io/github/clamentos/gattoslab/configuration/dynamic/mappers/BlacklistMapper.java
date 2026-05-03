package io.github.clamentos.gattoslab.configuration.dynamic.mappers;

///
import io.github.clamentos.gattoslab.configuration.dynamic.pojos.BlacklistDynamicProperty;
import io.github.clamentos.gattoslab.configuration.dynamic.pojos.BlacklistIpEntry;
import io.github.clamentos.gattoslab.exceptions.CodecException;
import io.github.clamentos.gattoslab.persistence.EntityField;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

///..
import org.bson.BsonReader;
import org.bson.BsonType;

///
public final class BlacklistMapper implements DynamicPropertySubMapper<BlacklistDynamicProperty> {

    ///
    private static final String SOURCE_MAP = "BlacklistMapper.map";
    private static final String SOURCE_READ_IPS = "BlacklistMapper.readIps";

    ///
    @Override
    public BlacklistDynamicProperty map(final BsonReader reader) throws CodecException {

        if(reader.getCurrentBsonType().equals(BsonType.NULL)) throw new CodecException("Field 'value' cannot be null", SOURCE_MAP);

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
                    case EntityField.USER_AGENT_CONTAINS: userAgentContains = GenericUtils.readSet(reader, String.class); break;

                    default: throw new CodecException("Unknown field '" + name + "'", SOURCE_MAP);
                }
            }

            reader.readEndDocument();

            if(ipv4s == null || ipv6s == null || userAgentContains == null) {

                throw new CodecException("Fields 'ipv4s', 'ipv6s', 'userAgentContains' of object 'value' cannot be null", SOURCE_MAP);
            }

            return new BlacklistDynamicProperty(ipv4s, ipv6s, userAgentContains);
        }

        catch(final IllegalStateException exc) {

            throw new CodecException(GenericUtils.WRAPPED_EXCEPTION_MSG, SOURCE_MAP, exc);
        }
    }

    ///.
    private List<BlacklistIpEntry> readIps(final BsonReader reader) throws CodecException {

        try {

            final List<BlacklistIpEntry> ips = new ArrayList<>();

            if(reader.getCurrentBsonType().equals(BsonType.NULL)) return null;
            reader.readStartArray();

            while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

                String startAddress = null;
                String endAddress = null;

                reader.readStartDocument();

                while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

                    final String name = reader.readName();

                    switch(name) {

                        case EntityField.START: startAddress = GenericUtils.readString(reader); break;
                        case EntityField.END: endAddress = GenericUtils.readString(reader); break;

                        default: throw new CodecException("Unknown field '" + name + "'", SOURCE_READ_IPS);
                    }
                }

                reader.readEndDocument();

                final byte[] startAddressBytes = InetAddress.ofLiteral(startAddress).getAddress();
                final byte[] endAddressBytes = InetAddress.ofLiteral(endAddress).getAddress();

                if(Arrays.compareUnsigned(startAddressBytes, endAddressBytes) > 0) {

                    throw new CodecException("Start ip address cannot be greater than end ip address", SOURCE_READ_IPS);
                }

                ips.add(new BlacklistIpEntry(startAddressBytes, endAddressBytes));
            }

            reader.readEndArray();
            return ips;
        }

        catch(final IllegalArgumentException | IllegalStateException exc) {

            throw new CodecException(GenericUtils.WRAPPED_EXCEPTION_MSG, SOURCE_READ_IPS, exc);
        }
    }

    ///
}
