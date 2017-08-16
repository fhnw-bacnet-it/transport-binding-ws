/**
 * 
 */
package ch.fhnw.bacnetit.transportbinding.api;

/**
 * @author IMVS, FHNW
 *
 */
public interface BindingConfiguration {
    public void initializeAndStart(ConnectionFactory connectionFactoy);
    public void shutdown();
}
