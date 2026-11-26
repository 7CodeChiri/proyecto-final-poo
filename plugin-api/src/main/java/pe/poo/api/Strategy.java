package pe.poo.api;

public interface Strategy<I, O> {
    String id();
    boolean supports(I request);
    O execute(I request);
}
