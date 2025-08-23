 
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class TCPThread extends Thread{
    public BufferedReader in;
    public PrintWriter out;
    int id;
    String mensaje;
    TCPThread [] amigos;
    TCPServer server;
    Socket cliente;
    boolean corriendo = false;
    TCPServer.alRecibirMensaje escuchador = null;
    public TCPThread(Socket cli,TCPServer tcpsever,int ide,TCPThread[]amig){
        cliente = cli;
        server = tcpsever;
        id = ide;
        amigos = amig;
    }
    public void run(){
        corriendo = true;
        try{
            
           
            out= new PrintWriter(new BufferedWriter(new OutputStreamWriter(cliente.getOutputStream())));
            in = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            escuchador =  server.obtenerEscuchador();
            enviarMensajeACliente("ESTE ES TU ID;"+id);
            while(corriendo){
                mensaje = in.readLine();
                if(mensaje!=null && escuchador!=null){
                    System.out.println("dentro de while TCPThread IN ID hilo: ");
                    System.out.println("escuchador"+escuchador);
                    escuchador.mensajeRecibido(mensaje);
                }else{
                    System.out.println("TCPThread saliendo ");
                    break;
                }
                mensaje =null;
            }   
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public void enviarMensajeACliente(String mensaje){ //escribir en cliente , para enviarle desde servidor
        if(out!=null && !out.checkError()){
            out.println(mensaje);
            out.flush();
        }
    }

}