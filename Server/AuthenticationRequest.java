package Server;

import java.io.Serializable;
import java.util.Random;

public class AuthenticationRequest implements Serializable {
    private String email;
    private int number;

    public AuthenticationRequest(String email){

        number = new Random().nextInt();
        this.email = email;
    }

    public AuthenticationRequest(String email, int num){

        this.number = num;
        this.email = email;
    }

    public String getEmail() {

        return email;
    }

    public int getNumber() {

        return number;
    }
}
