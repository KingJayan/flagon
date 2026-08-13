package java.net;

public final class PasswordAuthentication {

    private final String user;
    private final char[] password;

    public PasswordAuthentication(String user, char[] password) {
        this.user = user;
        this.password = password.clone();
    }

    public String getUserName() {
        return user;
    }

    public char[] getPassword() {
        return password.clone();
    }
}
