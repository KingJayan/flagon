package java.security;

/** a key, and the algorithm that gives its bytes meaning */
public interface Key extends java.io.Serializable {

    String getAlgorithm();

    String getFormat();

    byte[] getEncoded();
}
