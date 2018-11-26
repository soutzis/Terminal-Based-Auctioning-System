package Server;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class AuthenticationReply implements Serializable {

    private int solvedChallengeNumber;
    private int newChallengeNumber;

    public AuthenticationReply(int solvedChallenge) throws NoSuchAlgorithmException {
        this.solvedChallengeNumber = solvedChallenge;
        this.newChallengeNumber = new Random().nextInt();
    }

    public int getSolvedChallengeNumber() {
        return solvedChallengeNumber;
    }

    public int getNewChallengeNumber() {
        return newChallengeNumber;
    }
}
