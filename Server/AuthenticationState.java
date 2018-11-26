package Server;

class AuthenticationState {

    private Boolean isSuccessful;
    private Integer sequenceNumber;
    private Integer challengeSent;
    private Client client;

    AuthenticationState(boolean isSuccessful, int seq, int challengeSent, Client client){
        this.isSuccessful = isSuccessful;
        this.sequenceNumber = seq;
        this. challengeSent = challengeSent;
        this.client = client;
    }

    AuthenticationState(boolean isSuccessful, int seq){
        this.isSuccessful = isSuccessful;
        this.sequenceNumber = seq;
        this. challengeSent = null;
        this.client = null;
    }

    public Boolean getSuccessful() {
        return isSuccessful;
    }

    public void setSuccessful(Boolean successful) {
        isSuccessful = successful;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public Integer getChallengeSent() {
        return challengeSent;
    }

    public void setChallengeSent(Integer challengeSent) {
        this.challengeSent = challengeSent;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
