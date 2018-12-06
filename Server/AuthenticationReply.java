package Server;

import java.io.Serializable;

public class AuthenticationReply implements Serializable {

    private int solvedChallengeNumber;
    private int newChallengeNumber;

    public AuthenticationReply(int solvedChallenge, int newChallengeNumber){
        this.solvedChallengeNumber = solvedChallenge;
        this.newChallengeNumber = newChallengeNumber;
    }

    public int getSolvedChallengeNumber() {

        return solvedChallengeNumber;
    }

    public int getNewChallengeNumber() {

        return newChallengeNumber;
    }
}
