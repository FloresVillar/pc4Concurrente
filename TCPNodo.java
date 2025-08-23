 
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
//import java.net.spi.InetAddressResolver;
 
public class TCPNodo {
    String SERVERIP = "127.0.0.1";
    BufferedReader in ;
    PrintWriter out;
    int PORT = 5001;
    String mensaje;
    alRecibirMensaje escuchador;
    public TCPNodo(String ip,alRecibirMensaje listener){
        SERVERIP = ip;
        escuchador = listener;
    }
    public void run(){
        try{
            InetAddress ipAddress =InetAddress.getByName(SERVERIP);
            System.out.println("TCPNodo..");
            Socket server = new Socket(ipAddress,PORT);
            System.out.println("TCPNodo vinculo-server creado");
            try{
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(server.getOutputStream())));
                in = new BufferedReader(new InputStreamReader(server.getInputStream()));
                while(true){
                    mensaje  =in.readLine();
                    if(mensaje!=null&&escuchador!=null){
                        escuchador.mensajeRecibido(mensaje);
                    }
                }
            }catch(IOException e){
                e.printStackTrace();
            }finally{server.close();}
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    public interface  alRecibirMensaje {
        public void mensajeRecibido(String mensaje);
        
    }
    public void enviarMensaje(String mensaje){
        System.out.println("nodoEnvia-enviarMensaje");
        if(out!=null && !out.checkError()){
            System.out.println("nodoEnvia-enviarMensaje.if"+mensaje);
            out.println(mensaje);
            out.flush();
        }
    }

}
