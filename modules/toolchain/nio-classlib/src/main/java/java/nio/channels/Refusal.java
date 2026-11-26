package java.nio.channels;

final class Refusal {

    private Refusal() {
    }

    static UnsupportedOperationException refuse() {
        return new UnsupportedOperationException(
                "a page has no selectable channel: the proxy carries a stream, and nothing selects "
                        + "over it. see flagon-nu.vercel.app/docs");
    }
}
