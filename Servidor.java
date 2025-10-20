import java.util.ArrayList;
import java.util.Scanner;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Base64;

public class Servidor {
    TCPServer tcpServer;  
    Scanner sc = new Scanner(System.in);
    //Pantalla pantallaServidor;
    ArrayList<Integer> entrenados;

    Servidor() {
        //pantallaServidor = new Pantalla();
        entrenados = new ArrayList<>();
    }

    public static void main(String[] args) {
        Servidor obj = new Servidor();
        obj.iniciar();
    }

    public void iniciar() {
        new Thread(() -> {
            tcpServer = new TCPServer(
                new TCPServer.alRecibirMensaje() {
                    public void mensajeRecibido(String mensaje) {
                        servidorEscuchador(mensaje);
                    }
                },
                new TCPServer.alRecibirMensajeNodo() {
                    public void mensajeRecibidoNodo(String mensaje) {
                        servidorEscuchadorNodo(mensaje);
                    }
                }
            );
            tcpServer.run();
        }).start();

        String entrada = "n";
        System.out.println("Servidor: s para salir");
        while (!entrada.equals("s")) {
            entrada = sc.nextLine();
            servidorEnvia(entrada + ": para cliente");
            servidorEnviaNodo(entrada + ": para nodo");
        }
    }
    //---------------------------------------------------------------
    public void servidorEscuchador(String mensaje) {
    //pantallaServidor.agregarMensaje("Cliente >> " + mensaje+"\n");
        if (mensaje.startsWith("ENTRENAR;")) {
            repartirImagenesNodos(mensaje);
        }
        if (mensaje.startsWith("TESTEAR;")) {
        //"TESTEAR;" + base64 + ";" + idObjetoCliente;
            tcpServer.enviarMensajeNodo(mensaje); 
        }//"PREDICCION;CLASE=" + clase + ";NODO=" + idObjNodo
        if(mensaje.startsWith("PREDICCION;")){
            servidorEnvia(mensaje);
        }
    }
    //---------------------------------------------------------------
    public void servidorEscuchadorNodo(String mensaje) {
        //pantallaServidor.agregarMensajeNodo("Nodo >> " + mensaje);
        if (mensaje.startsWith("ENTRENAMIENTO_COMPLETADO;")) {
            try {
                int id = Integer.parseInt(mensaje.split(";")[2].trim());
                entrenados.add(id);
            } catch (Exception e) {
                System.out.println("Error al procesar ENTRENAMIENTO");
            }
        }
    }

    public void servidorEnvia(String mensaje) {
        tcpServer.enviarMensaje(mensaje);
    }

    public void servidorEnviaNodo(String mensaje) {
        tcpServer.enviarMensajeNodo(mensaje);
    }

    public void repartirImagenesNodos(String mensaje) {
        int nNodos = tcpServer.obtenerNNodos();
        for (int i = 0; i < nNodos; i++) {
            System.out.println(mensaje);
            TCPThreadNodo nodo = tcpServer.obtenerNodo(i);
            nodo.enviarMensajeANodo(mensaje);
        }
    }

    /**
     * class Pantalla extends JFrame {
        JTextArea mensajes;
        JTextArea mensajesNodo;

        Pantalla() {
            setTitle("SERVIDOR");
            setSize(500, 400);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setLayout(new BorderLayout());

            JPanel panelMensajes = new JPanel(new GridLayout(2, 1));

            mensajes = new JTextArea();
            mensajes.setBorder(BorderFactory.createTitledBorder("Mensajes de Clientes"));
            mensajes.setEditable(false);
            panelMensajes.add(new JScrollPane(mensajes));

            mensajesNodo = new JTextArea();
            mensajesNodo.setBorder(BorderFactory.createTitledBorder("Mensajes de Nodos"));
            mensajesNodo.setEditable(false);
            panelMensajes.add(new JScrollPane(mensajesNodo));

            add(panelMensajes, BorderLayout.CENTER);
            setVisible(true);
        }

        public void agregarMensaje(String mensaje) {
            SwingUtilities.invokeLater(() -> mensajes.append(mensaje + "\n"));
        }

        public void agregarMensajeNodo(String mensaje) {
            SwingUtilities.invokeLater(() -> mensajesNodo.append(mensaje + "\n"));
        }
    }
     */

    // ---------------------------------------------------------------------
    // Clase de utilidad para reemplazar DataSet
    static class DataSetSimple implements Serializable {
        double[] input;
        double[] output;

        public DataSetSimple(double[] input, double[] output) {
            this.input = input;
            this.output = output;
        }

        public byte[] toBytes() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(input.length);
            for (double v : input) 
                dos.writeDouble(v);
            dos.writeInt(output.length);
            for (double v : output) 
                dos.writeDouble(v);
            return baos.toByteArray();
        }

        public static DataSetSimple fromBytes(byte[] data) throws IOException {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DataInputStream dis = new DataInputStream(bais);
            int inLen = dis.readInt();
            double[] input = new double[inLen];
            for (int i = 0; i < inLen; i++) 
                input[i] = dis.readDouble();
            int outLen = dis.readInt();
            double[] output = new double[outLen];
            for (int i = 0; i < outLen; i++) 
                output[i] = dis.readDouble();
            return new DataSetSimple(input, output);
        }

        public String toBase64() throws IOException {
            return Base64.getEncoder().encodeToString(toBytes());
        }

        public static DataSetSimple fromBase64(String base64) throws IOException {
            return fromBytes(Base64.getDecoder().decode(base64));
        }
    }
}
