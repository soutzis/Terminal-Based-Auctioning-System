package Server;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

 class ServerState implements Serializable {

    private ConcurrentHashMap<String, Auction> auctions = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Client> clients = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, AuthenticationState> authState = new ConcurrentHashMap<>();

     /**
      * This is used to get the saved state of the HashMap<>() containing all the clients of the server.
      * @return The stored 'clients' Map.
      */
     public ConcurrentHashMap<String, Client> getClients() {

         return this.clients;
     }

     /**
      * This is used to get the saved state of the HashMap<>() containing all the auctions of the server.
      * @return The stored 'auctions' Map.
      */
    public ConcurrentHashMap<String, Auction> getAuctions() {

        return this.auctions;
    }

     /**
      * This is used to get the saved state of the HashMap<>() containing all the authentication states of the server.
      * @return The stored 'authState' Map.
      */
     public ConcurrentHashMap<String, AuthenticationState> getAuthState() {

         return this.authState;
     }


    public void setAuctions(ConcurrentHashMap<String, Auction> auctions) {

        this.auctions = auctions;
    }

    public void setClients(ConcurrentHashMap<String, Client> clients) {

        this.clients = clients;
    }


    public void setAuthState(ConcurrentHashMap<String, AuthenticationState> authState) {

        this.authState = authState;
    }
}
