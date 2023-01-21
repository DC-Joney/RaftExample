package com.dc.raft.command.client;

import com.dc.raft.command.ResponseCode;
import com.dc.raft.command.ResponseCommand;
import lombok.*;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "create")
public class ClientResponse extends ResponseCommand {

    private String key;

    private String value;

    public static ClientResponse fail(String message) {
        ClientResponse response = new ClientResponse();
        response.setErrorCode(ResponseCode.FAIL.getCode());
        response.setMessage(message);
        return null;
    }


    public static ClientResponse ok() {
        return new ClientResponse();
    }
}
