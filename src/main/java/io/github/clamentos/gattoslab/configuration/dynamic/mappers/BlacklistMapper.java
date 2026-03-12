package io.github.clamentos.gattoslab.configuration.dynamic.mappers;

///
import io.github.clamentos.gattoslab.configuration.dynamic.pojos.BlacklistDynamicProperty;
import io.github.clamentos.gattoslab.configuration.dynamic.pojos.BlacklistIpEntry;

///..
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

///..
import org.bson.BsonReader;
import org.bson.BsonType;

///
public final class BlacklistMapper implements DynamicPropertySubMapper<BlacklistDynamicProperty> {

    ///
    @Override
    public BlacklistDynamicProperty map(final BsonReader reader) {

        reader.readStartDocument();

        final List<BlacklistIpEntry> ipv4s = this.readIps(reader, "ipv4s");
        final List<BlacklistIpEntry> ipv6s = this.readIps(reader, "ipv6s");

        reader.readName("userAgentContains");
        reader.readStartArray();

        final List<String> userAgentContains = new ArrayList<>();
        while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) userAgentContains.add(reader.readString());

        reader.readEndArray();
        reader.readEndDocument();

        return new BlacklistDynamicProperty(ipv4s, ipv6s, userAgentContains);
    }

    ///.
    private List<BlacklistIpEntry> readIps(final BsonReader reader, final String key) {

        final List<BlacklistIpEntry> entries = new ArrayList<>();

        reader.readName(key);
        reader.readStartArray();

        while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

            entries.add(new BlacklistIpEntry(

                InetAddress.ofLiteral(reader.readString("start")).getAddress(),
                InetAddress.ofLiteral(reader.readString("end")).getAddress()
            ));
        }

        reader.readEndArray();
        return entries;
    }

    ///
}
