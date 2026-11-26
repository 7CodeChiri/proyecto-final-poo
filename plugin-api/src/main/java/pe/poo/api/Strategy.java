package pe.poo.api;

/** Common Java contract for interchangeable level generators. */
public interface Strategy<I, O> {
    String id();
    boolean supports(I request);
    O execute(I request);
}
