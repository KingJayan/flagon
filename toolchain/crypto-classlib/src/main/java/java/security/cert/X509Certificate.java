package java.security.cert;

import java.math.BigInteger;
import java.security.PublicKey;
import java.util.Date;

public abstract class X509Certificate extends Certificate {

    protected X509Certificate() {
        super("X.509");
    }

    public abstract void checkValidity() throws CertificateException;

    public abstract void checkValidity(Date when) throws CertificateException;

    public abstract int getVersion();

    public abstract BigInteger getSerialNumber();

    public abstract Date getNotBefore();

    public abstract Date getNotAfter();

    public abstract byte[] getSignature();

    public abstract String getSigAlgName();

    @Override
    public abstract PublicKey getPublicKey();
}
