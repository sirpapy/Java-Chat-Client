package trash;

import java.nio.charset.Charset;
import java.util.HashMap;

public class TestNetworkProtocol2 {

    static final HashMap<String, Integer> codeMap = new HashMap<>();
    static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    
    static {
	int i = 0;
	codeMap.put("COREQ",i++);
    }

    /* COREQ */ static class CLIENT_PUBLIC_CONNECTION_REQUEST {
	private final String code = "COREQ";
	private final int value = codeMap.get(code);
    }

    /* CORES */ static class SERVER_PUBLIC_CONNECTION_RESPONSE {

    }

    /* CODISP */ static class SERVER_PUBLIC_CONNECTION_NOTIFICATION {

    }

    /* MSG */ static class CLIENT_PUBLIC_MESSAGE {

    }

    /* MSGBC */ static class SERVER_PUBLIC_MESSAGE_BROADCAST {

    }

    /* PVCOREQ */ static class CLIENT_PRIVATE_CONNECTION_REQUEST {

    }

    /* PVCOTR */ static class SERVER_PRIVATE_CONNECTION_TRANSFER {

    }

    /* PVCOACC */ static class CLIENT_PRIVATE_CONNECTION_CONFIRM {

    }

    /* PVCORES */ static class SERVER_PRIVATE_CONNECTION_RESPONSE {

    }

    /* PVCOETA */ static class SERVER_PRIVATE_CONNECTION_ETABLISHMENT {

    }

    /* PVMSG */ static class CLIENT_PRIVATE_MESSAGE {

    }

    /* PVFILE */ static class CLIENT_PRIVATE_FILE {

    }

    /* PVDISCO */ static class CLIENT_PRIVATE_DISCONNECTION {

    }

    /* DISCO */ static class CLIENT_PUBLIC_DISCONNECTION {

    }

}
