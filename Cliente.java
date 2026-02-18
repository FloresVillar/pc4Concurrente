
import java.util.Scanner;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.util.Random; 
import java.awt.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
// Java estándar
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FilenameFilter;

public class Cliente {
    TCPCliente tcpcliente;
    Scanner sc = new Scanner(System.in);
    Pantalla pantallaCliente;
    boolean primerMensaje =false;
    int idObjetoCliente;
    Cliente() {
        pantallaCliente = new Pantalla();
    }
    //-------------------------------------------------------------
    public static void main(String[] args) {
        Cliente obj = new Cliente();
        obj.iniciar();
    }
    //----------------------------------------------------------------
    public void iniciar() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                tcpcliente = new TCPCliente("127.0.0.1", new TCPCliente.alRecibirMensaje() {
                    @Override
                    public void mensajeRecibido(String mensaje) {
                        clienteEscuchador(mensaje);
                        clienteEscuchadorPantalla(mensaje);
                    }
                });
                tcpcliente.run();
            }
        }).start();
        String entrada = "n";
        System.out.println("Cliente: s para salir, separar etiquetas con | ");
        while (!entrada.equals("s")) {
            entrada = sc.nextLine();
            clienteEnvia(entrada);
        }
    } 
    //-------------------------------------------------------------------
    public void clienteEscuchador(String mensaje) {
        System.out.println("cliente recibe: " + mensaje);
        if(mensaje.contains(";")){
            if(mensaje.split(";")[0].equals("ESTE ES TU ID")){
                idObjetoCliente = Integer.parseInt(mensaje.split(";")[1].trim());
            }//msj = "SALDO_CONSULTADO;"+ID_CUENTA+SALDO;
            if(mensaje.split(";")[0].equals("ENTRENADOS")){
                clienteEscuchadorPantalla(mensaje);
                String idEntrenados = mensaje.split(";")[1].trim();
                String [] ids = idEntrenados.split("-");
                int[] entrenados = new int[ids.length];
                for (int i=0;i<entrenados.length;i++){
                    entrenados[i] = Integer.parseInt(ids[i].trim());
                }
                // escoger uno de los ids - nodos
            }
            if(mensaje.startsWith("PREDICCION")){  //PREDICCION;pedicho;ETIQUETA;etiqueta
                clienteEscuchadorPantalla(mensaje);
            }
        }
    }
    //---------------------------------------------------------------------
    public void clienteEscuchadorPantalla(String mensaje) {
        String[] lineas = mensaje.split("[\\n;]");
        for (String t : lineas) {
            pantallaCliente.agregarMensaje(t + "\n");
        }
    }
    //-------------------------------------------------------------------
    public void clienteEnvia(String mensaje) {
        System.out.println("clienteEnvia");
        tcpcliente.enviarMensaje(mensaje);
    }
    //--------------------------------------------------
    public void imprimir(String m){
        System.out.println(m);
    }
    //-------------------------------------------------------------------
    public String lecturaImagenes(String rutaCarpeta) {
        String mensajeImagenes = "";
        File carpeta = new File(rutaCarpeta);
        if (carpeta==null){
            System.out.println("no carpeta"); 
        }
        File[] archivos = carpeta.listFiles(new FilenameFilter(){
            @Override
            public boolean accept(File dir, String name){
                return name.toLowerCase().endsWith(".png");
            }
        }); 
        if (archivos == null) {
            System.out.println("No se encontraron imágenes en la carpeta.");
            return mensajeImagenes;
        }
        int N = archivos.length;
        for (int j =0;j<N;j++) {
            try {
            // Extraer la etiqueta (dígito) desde el nombre del archivo, ej: "5_1.png"
                String archivo = archivos[j].getName(); 
            //String[]partes = archivo.split("\\\\");
                int label = Integer.parseInt(archivo.split("_")[0].trim()); // toma el número antes del "_"
                BufferedImage imagen = ImageIO.read(archivos[j]);
                int ancho = imagen.getWidth();
                int alto = imagen.getHeight();
                String[] entrada = new String[ancho * alto];
                for (int y = 0; y < alto; y++) {
                    for (int x = 0; x < ancho; x++) {
                        int columna = x;
                        int fila = y;
                        int pixel = imagen.getRGB(columna, fila);
                        int alpha = (pixel >> 24) & 0xFF; 
                        int rojo = (pixel >> 16 ) & 0xFF;
                        int verde = (pixel >> 8) & 0xFF;
                        int azul = pixel & 0xFF;
                        entrada[y * ancho + x] = Integer.toString(alpha)+"-"+Integer.toString(rojo)+"-"+Integer.toString(verde)+"-"+Integer.toString(azul);
                }
            }
            String mensaje = "";
            int n = entrada.length;
            for (int i=0;i<n-1;i++){
                mensaje= mensaje + entrada[i] +",";
            }
            mensaje = mensaje + entrada[n-1] +" ";
            mensaje = "" + Integer.toString(label) + ";"+Integer.toString(ancho)+";"+Integer.toString(alto)+";" + mensaje + "";
            //imprimir(mensaje); // 
            if(j==N-1){
                mensajeImagenes = mensajeImagenes + mensaje +"";
            }else{
                mensajeImagenes = mensajeImagenes + mensaje +"|";
            }
        } catch (Exception e) {
            System.out.println("Error procesando archivo: " + archivos[j].getName());
            e.printStackTrace();
        }
    }
    //mensajeImagenes = mensajeImagenes +";"+idObjetoCliente; 
    String base64 = Base64.getEncoder().encodeToString(mensajeImagenes.getBytes(StandardCharsets.UTF_8));
    //imprimir(base64); //"ENTRENAR;{numero;ancho;alto;mensaje|numero;...|numero;ancho;alto;mensaje|... };idObjetoCliente" en base64 
    //mensaje : pixel,pixel,...                             {base64}
    return base64;
    }
    //-------------------------------------------------------------
    public void mostrarImagen(JFrame frame,JLabel etiqueta,File nombre){
        try{
            BufferedImage imagen = ImageIO.read(nombre);  
            etiqueta.setIcon(new ImageIcon(imagen));
            frame.repaint();
            Thread.sleep(100); 
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    //---------------------------------------------------------------
static class DataSetSimple implements Serializable {
        double[] input;
        double[] output;
        public DataSetSimple(double[] i, double[] o) {
            input = i; output = o;
        }
    }

    // ============================================================
    class Pantalla extends JFrame {
        JTextArea mensajes;
        JTextField entrada;
  
        Pantalla() {
            setTitle("CLIENTE");
            setSize(400, 300);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            mensajes = new JTextArea();//cuando recibe desde servidor
            JScrollPane scroll = new JScrollPane(mensajes);
            add(scroll, BorderLayout.CENTER);
            JPanel panelentrada =new JPanel(new BorderLayout());
            JPanel panelbotones = new JPanel(new GridLayout(2,1,5,5));
            entrada = new JTextField();
            panelentrada.add(entrada,BorderLayout.CENTER);
            ponerBoton(panelbotones, "ENTRENAR", new ActionListener() {
                public void actionPerformed(ActionEvent evento) {
                    String mensj = lecturaImagenes("datos/entrenamiento");//siguiendo la logica de los mensjaes "CONSULTAR_SALDO | ID_CUENTA | SALDO"
                    if (!mensj.isEmpty()) { 
                        mensj ="ENTRENAR;" +mensj + ";"+idObjetoCliente; 
                        imprimir(mensj);
                        clienteEnvia(mensj);
                        // entrada.setText("");
                    }
                }
            });
            ponerBoton(panelbotones, "TESTEAR", new ActionListener() {
            public void actionPerformed(ActionEvent evento) {
                String mensj = lecturaImagenes("datos/test");//siguiendo la logica de los mensjaes "CONSULTAR_SALDO | ID_CUENTA | SALDO"
                    if (!mensj.isEmpty()) { 
                        mensj ="TESTEAR;" +mensj + ";"+idObjetoCliente; 
                        imprimir(mensj);
                        clienteEnvia(mensj);
                        // entrada.setText("");
                    }
                }
            });
            panelentrada.add(panelbotones,BorderLayout.EAST);
            add(panelentrada, BorderLayout.SOUTH);
            setVisible(true);
        }

        public void agregarMensaje(String mensaje) {
            mensajes.append(mensaje + "\n");
        }

        public void ponerBoton(Container c, String nombre, ActionListener escuchador) {
            JButton boton = new JButton(nombre);
            c.add(boton,BorderLayout.EAST);
            boton.addActionListener(escuchador);
        }
    }
    // ==========================================================
}

/**
 * carpeta.listFiles(new FilenameFilter(){
 *      public void accept(String dir,String name){
 *          return name.metodo.endWith(".png")
 *      }
 *  })
 *  FilenameFilter es una interfaz funcional que declara un metodo que se implementa a futuro(en el ej ya esta )
 * pero se como interface alRecibirMensaje(){
 *  public void mensajeRecibido(String mensaje); es un unico metodo abstracto que se definira luego, esto otorga flexibilidad 
 * }
 * 
 * "\\\\" en regrex se traduce con \\ que singnifica solo \
 * cadena.split("\\\\") separamos mediante "\"
 * 
 * import javax.imageio.ImageIO;
 * ImageIO.read(imagen) devuelve un BufferedImage , que es la representacion en memoria de la imagen
 * lo quee veo es que seria la matriz imagen con ternas RGB que representan a la imagen
 * pero en realidad lo guarda como un entero de 32 bits :
 * Bits:   31 -------- 24  23 -------- 16  15 -------- 8   7 -------- 0
        [   Alpha   ]   [    Red    ]   [   Green   ]   [   Blue   ]
        pixel = (255 << 24) | (128 << 16) | (64 << 8) | 32
        255<<24 desplaza 255 24 bit a la izquierda asi los demas luego hacer un or para unir
        255 decimal  = 11111111  (8 bits)
        255 << 24    = 11111111 00000000 00000000 00000000 (32 bits)
                              ^------ Alpha en su lugar (bits 24-31)

 * imagen.getRGB(columna, fila)
 * ----------------→X
 * |ANCHO cantidad de columnas
 * |ALTO cantidad de filas
 * ↓
 * y
 * 
 * para >> :
 * ej: pixel : pixel = 11111111 01100100 10010110 11001000
                        A=255    R=100    G=150    B=200
 *  pixel >> 16
 *              11111111 01100100 10010110 11001000   (pixel original)
                >>16
                -----------------------------------
                00000000 00000000 11111111 01100100
                luego se hace el AND con 0xFF 
                00000000 00000000 11111111 01100100
                AND
                00000000 00000000 00000000 11111111
                -----------------------------------
                00000000 00000000 00000000 01100100 obteniendose el ROJO

 * 
 */