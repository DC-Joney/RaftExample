package com.dc.raft.command.client;

import com.dc.raft.command.RequestCommand;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@ToString
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientRequest extends RequestCommand {


    ClientType clientType;

    String key;

    String value;


    public boolean typeWith(ClientType clientType){
        return this.clientType == clientType;
    }


    public enum ClientType {
        PUT, GET, DELETE
    }


    @Override
    public String getModule() {
        return null;
    }

    private ClientRequest(Builder builder) {
        setClientType(builder.type);
        setKey(builder.key);
        setValue(builder.value);
    }

    public static final class Builder {

        private ClientType type = ClientType.GET;
        private String key;
        private String value;

        private Builder() {
        }


        public Builder type(ClientType clientType) {
            type = clientType;
            return this;
        }

        public Builder key(String val) {
            key = val;
            return this;
        }

        public Builder value(String val) {
            value = val;
            return this;
        }

        public ClientRequest build() {
            return new ClientRequest(this);
        }
    }
}
