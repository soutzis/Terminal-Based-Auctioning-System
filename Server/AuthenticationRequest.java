package Server;

import java.io.Serializable;

public class AuthenticationRequest implements Serializable {
    private String email;
    private int number;

    public AuthenticationRequest(String email, int random){

        number = random;
        this.email = email;
    }

    public String getEmail() {

        return email;
    }

    public int getNumber() {

        return number;
    }
}
