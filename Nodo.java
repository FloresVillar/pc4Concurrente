import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
public class Nodo {
    TCPNodo tcpnodo;
    //Pantalla pantallaNodo;
    int idObjNodo; 
    ArrayList<Data> datosEntrenamiento = new ArrayList<>();

    public Nodo() {
        //pantallaNodo = new Pantalla();
    }

    public static void main(String[] args) {
        Nodo obj = new Nodo();
        obj.iniciar();
    }

    public void iniciar() {
        new Thread(() -> {
            tcpnodo = new TCPNodo("127.0.0.1", new TCPNodo.alRecibirMensaje() {
                public void mensajeRecibido(String mensaje) {
                    nodoEscuchador(mensaje);
                    //nodoEscuchadorPantalla(mensaje);
                }
            });
            tcpnodo.run();
        }).start();
    }

    public void nodoEscuchador(String mensaje) { 
        //pantallaNodo.agregarMensaje(mensaje);
        if (mensaje.startsWith("ESTE ES TU ID")) {
            idObjNodo = Integer.parseInt(mensaje.split(";")[1].trim());
            System.out.println("ID: "+idObjNodo);
            return;
        }
        if (mensaje.startsWith("ENTRENAR")) {
            System.out.println("ENTRENAR: ");
            //String[] partes = mensaje.split(";|");
            byte[] bytes = Base64.getDecoder().decode(mensaje.split(";")[1].trim());
            if(bytes==null){
                System.out.println("bytes == null");
            }
            else{
                String numeros = new String(bytes,StandardCharsets.UTF_8);
                String[] partes = numeros.split("\\|");
                for(String numero:partes){
                    String []componentes = numero.split(";");
                    int etiqueta = Integer.parseInt(componentes[0].trim());
                    int ancho = Integer.parseInt(componentes[1].trim());
                    int largo = Integer.parseInt(componentes[2].trim());
                    System.out.println("numero:"+etiqueta+" ancho:"+ancho+" largo:"+largo);
                    String [] pixeles = componentes[3].split(",");
                    double [] entrada= new double[ancho*largo];
                    for(int i=0;i<pixeles.length;i++){
                        String[] rgba = pixeles[i].split("-");
                        int alpha = Integer.parseInt(rgba[0].trim()); 
                        int rojo = Integer.parseInt(rgba[1].trim());
                        int verde = Integer.parseInt(rgba[2].trim());
                        int azul = Integer.parseInt(rgba[3].trim());
                        entrada[i] = (0.299*rojo+0.587*verde+0.144*azul)/255.0;
                    }
                    double [] salida =new double[10];
                    for(int i = 0;i<salida.length;i++){
                        if(i==etiqueta){
                            salida[etiqueta] = 1.0;
                        }
                    }
                    Data data = new Data(entrada,salida);
                    datosEntrenamiento.add(data); 
                //redNeuronal(entrada ,salida)
                }
            } 
            //creamos el modelo..
            RedNeuronal red = new RedNeuronal(784, 64, 10);
            red.entrenar(datosEntrenamiento, 10);
            try {
                ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("modelo_nodo_" + idObjNodo + ".dat"));
                oos.writeObject(red);
                oos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            nodoEnvia("ENTRENAMIENTO_COMPLETADO;NODO_ID=" + idObjNodo);
        }

        if (mensaje.startsWith("TESTEAR")) {
            try {
                String base64 = mensaje.split(";")[1];
                byte[] datos = Base64.getDecoder().decode(base64);
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(datos));
                Data ds = (Data) ois.readObject();

                ObjectInputStream modeloStream = new ObjectInputStream(new FileInputStream("modelo_nodo_" + idObjNodo + ".dat"));
                RedNeuronal red = (RedNeuronal) modeloStream.readObject();
                modeloStream.close();

                int clase = red.predecir(ds.entrada);
                nodoEnvia("PREDICCION;CLASE=" + clase + ";NODO=" + idObjNodo);
            } catch (Exception e) {
                e.printStackTrace();
                nodoEnvia("ERROR;AL_TESTEAR;NODO=" + idObjNodo);
            }
        }
    }

    /*
    public void nodoEscuchadorPantalla(String mensaje) {
        pantallaNodo.agregarMensaje(mensaje);
    }
 */
    public void nodoEnvia(String mensaje) {
        tcpnodo.enviarMensaje(mensaje);
    }

    /*
    class Pantalla extends JFrame {
        JTextArea mensajes;
        JTextField entrada;

        Pantalla() {
            setTitle("NODO");
            setSize(300, 300);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            mensajes = new JTextArea();
            JScrollPane scroll = new JScrollPane(mensajes);
            add(scroll, BorderLayout.CENTER);
            JPanel botones = new JPanel(new BorderLayout());
            entrada = new JTextField();
            botones.add(entrada, BorderLayout.CENTER);
            ponerBoton(botones, "enviar", e -> {
                String mensj = entrada.getText();
                if (!mensj.isEmpty()) {
                    nodoEnvia(mensj);
                    entrada.setText("");
                }
            });
            add(botones, BorderLayout.SOUTH);
            setVisible(true);
        }

        public void agregarMensaje(String mensaje) {
            mensajes.append(mensaje + "\n");
        }

        public void ponerBoton(Container c, String nombre, ActionListener escuchador) {
            JButton boton = new JButton(nombre);
            c.add(boton, BorderLayout.EAST);
            boton.addActionListener(escuchador);
        }
    } */
   //----------------------------------------------------------------
    static class Data implements Serializable {
        double[] entrada;
        double[] salida;
        public Data(double[] i, double[] o) {
            entrada = i; salida = o;
        }
    }
    //----------------------------------------------------------------
    static class RedNeuronal implements Serializable {
        double[][] w1, w2;
        double[] b1, b2;
        Random rnd = new Random();
        //-------------------------------------------------
        RedNeuronal(int pixeles, int neuronasOcultas, int neuronasSalida) {
            w1 = new double[neuronasOcultas][pixeles];
            b1 = new double[neuronasOcultas];
            w2 = new double[neuronasSalida][neuronasOcultas];
            b2 = new double[neuronasSalida];
            //inicializar(w1);
            for(int i=0;i<w1.length;i++){
                for(int j=0;j<w1[0].length;j++){
                    w1[i][j] = rnd.nextGaussian()*0.01;
                }
            }
            //inicializar(w2);
            for(int i=0;i<w2.length;i++){
                for(int j=0;j<w2[0].length;j++){
                    w2[][]=rnd.nextGaussian()*0.01;
                }
            }  
        }
        //---------------------------------------- 
        double[] propagacionAdelante(double[] x) { //capa oculta
            double[]z1 = new double[b1.length];
            for(int i=0;i<w1.length;){
                for(int j=0;j<x.length;j++){
                    z1[i]+=(w1[i][j]*x[j]);
                }
                z1[i]=Math.max(0,z1[i]+b[i]);//relu
            } 
            double []z2 = new double[b2.length];
            for(int i=0;i<w2.length;i++){ //capa salida
                for(int j=0;j<z1.length;j++){
                    z2[]+=(w2[i][j]*z1[j])
                }
                z2[i]+=b2[i];
            }
            return softmax(z2); 
        }
        //----------------------------------------------------------
        double[] softmax(double[] z) {
            double max = java.util.Arrays.stream(z).max().getAsDouble();
            double sum = 0;
            double[] res = new double[z.length];
            for (int i = 0; i < z.length; i++){
                res[i] = Math.exp(z[i] - max);
                suma + = res[i];
            }
            for (int i = 0; i < res.length; i++) {
                res[i] /= sum;
            }
            return res;
        }
        //----------------------------------------------------------
        int predecir(double[] x) {
            double[] probabilidades = propagacionAdelante(x); //softmax, las probabilidades
            int indx = 0;
            for(int i=1;i<probabilidades.length;i++){
                if(probabilidades[i]>probabilidades[indx]){
                    indx = i;
                }
            }
            return idx;//el indice con la prob mas alta
        }
        //-----------------------------------------------------------
        void entrenar(java.util.List<Data> datos, int epocas) {
            double lr = 0.01;
            for (int ep = 0; ep < epocas; ep++) {
                for (Data ds : datos) {
                    double[] x = ds.entrada; //cada x[i] es un pixel de los ancho*alto pixeles
                    double[] y = ds.salida;
                    double[] z1 = new double[b1.length];
                    double[] a1 = new double[b1.length];
                    for (int i = 0; i < w1.length; i++) {
                        for (int j = 0; j < x.length; j++)
                            z1[i] += w1[i][j] * x[j];
                        a1[i] = Math.max(0, z1[i] + b1[i]);
                    }
                    double[] z2 = new double[b2.length];
                    double[] a2 = new double[b2.length];
                    double max = -Double.MAX_VALUE;
                    for (int i = 0; i < w2.length; i++) {
                        for (int j = 0; j < a1.length; j++)
                            z2[i] += w2[i][j] * a1[j];
                        z2[i] += b2[i];
                        if (z2[i] > max) {
                            max = z2[i];
                        }
                    }
                    double sum = 0;
                    for (int i = 0; i < a2.length; i++) sum += (a2[i] = Math.exp(z2[i] - max));
                    for (int i = 0; i < a2.length; i++) a2[i] /= sum;
                    double[] d2 = new double[y.length];
                    for (int i = 0; i < y.length; i++) d2[i] = a2[i] - y[i];
                    double[][] dw2 = new double[w2.length][w2[0].length];
                    for (int i = 0; i < w2.length; i++)
                        for (int j = 0; j < w2[0].length; j++)
                            dw2[i][j] = d2[i] * a1[j];
                    double[] db2 = Arrays.copyOf(d2, d2.length);
                    double[] d1 = new double[a1.length];
                    for (int i = 0; i < a1.length; i++) {
                        double sumD = 0;
                        for (int j = 0; j < d2.length; j++)
                            sumD += d2[j] * w2[j][i];
                        d1[i] = z1[i] > 0 ? sumD : 0;
                    }
                    double[][] dw1 = new double[w1.length][w1[0].length];
                    for (int i = 0; i < w1.length; i++)
                        for (int j = 0; j < x.length; j++)
                            dw1[i][j] = d1[i] * x[j];
                    double[] db1 = Arrays.copyOf(d1, d1.length);
                    for (int i = 0; i < w2.length; i++)
                        for (int j = 0; j < w2[0].length; j++)
                            w2[i][j] -= lr * dw2[i][j];
                    for (int i = 0; i < b2.length; i++) b2[i] -= lr * db2[i];
                    for (int i = 0; i < w1.length; i++)
                        for (int j = 0; j < w1[0].length; j++)
                            w1[i][j] -= lr * dw1[i][j];
                    for (int i = 0; i < b1.length; i++) b1[i] -= lr * db1[i];
                }
            }
        }
    }
}


/**
 * java.util.Arrys.stream(array).max().getAsDouble()
 *                  devuelve un wrapper    → convertir a double
 * 
 * softmax: convierte el vector de numeros reales (logits) en salidas crudas de 
 * la red softmax(zi) = exp(zi -max(zi)) / suma exp(zi -max(zi))
 * ej : si z2 = [2.0 , 1.0 , 0.1]
 * calcular las exp: e^2-2  e^1-2 e^0.1-2 suma
 * softmax(2) = e^2-2/suma *100% asi los demas    
 */