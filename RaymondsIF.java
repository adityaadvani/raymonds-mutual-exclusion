/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//package raymondmuex;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author Aditya Advani
 */
public interface RaymondsIF extends Remote{
    
    public void getRequest(String fromNode) throws RemoteException;
    
    public void getToken(String fromNode) throws RemoteException;
    
    public void updateSharedResource(String enteringNode) throws RemoteException;
}
